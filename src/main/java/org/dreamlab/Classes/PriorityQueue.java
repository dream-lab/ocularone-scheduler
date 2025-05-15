package org.dreamlab.Classes;

import org.apache.log4j.Logger;
import org.dreamlab.ApplicationDetails.Application;
import org.dreamlab.Interfaces.IPriorityQueue;

import java.util.*;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

public class PriorityQueue implements IPriorityQueue {

    static final Logger logger = Logger.getLogger(PriorityQueue.class);
    static final String path = "./src/main/resources/log4j.properties";
    static final long bufferForTentativeCloud = 50L;

    private LinkedList<Task> queue = new LinkedList<>();

    public synchronized Task peek() {
        return queue.peek();
    }

    public synchronized int size() {
        return queue.size();
    }

    public synchronized Task poll() {
        return queue.poll();
    }

    public synchronized void printContents(){
        ListIterator<Task> taskListIterator = queue.listIterator();
        while(taskListIterator.hasNext()){
            Task task = taskListIterator.next();
            logger.info(" Task id: " + task.getTaskMetadata().getTaskId() + " , dnn model: " + task.getTaskMetadata().getDnnModel()+ ", trigger time: " + task.getTaskMetadata().getTriggerTimeForCloud());
        }
    }

    public synchronized PeekAndPollResponse peekAndPoll() {

        Task peekedTask = queue.peek();

        if (peekedTask == null) {
            return null;
        }
        long availableTime = 0L;

        if(Application.hasNegativeUtilityOnCloud(peekedTask.getTaskMetadata().getDnnModel())){
            availableTime =  peekedTask.getTaskMetadata().getTriggerTimeForEdge() - System.currentTimeMillis();
        }
        else{
            availableTime =  peekedTask.getTaskMetadata().getTriggerTimeForCloud() - System.currentTimeMillis();
        }

        logger.info(System.currentTimeMillis() + " The peeked task with task id " + peekedTask.getTaskMetadata().getTaskId() + " has available time as " + availableTime);

        if (availableTime >= -10L && availableTime <= bufferForTentativeCloud) {
            PeekAndPollResponse peekAndPollResponse = new PeekAndPollResponse();
            queue.remove(peekedTask);
            peekAndPollResponse.setPeekedAndPolledTask(peekedTask);
            peekAndPollResponse.setAvailableTimeNegative(false);
            return peekAndPollResponse;
        }
        if (availableTime < -10L) {
            PeekAndPollResponse peekAndPollResponse = new PeekAndPollResponse();
            queue.remove(peekedTask);
            peekAndPollResponse.setPeekedAndPolledTask(peekedTask);
            peekAndPollResponse.setAvailableTimeNegative(true);
            return peekAndPollResponse;
        }
        return null;
    }

    public synchronized List<Task> gatekeeperV0Scan(ArrayList<DNNModels> dnnModelsArrayList){
        List<Task> taskListForGateKeeperV0Scan = new ArrayList<>();
        ListIterator<Task> taskListIterator = queue.listIterator();
        while(taskListIterator.hasNext()){
            Task taskToAddForGatekeeperV0Scan = taskListIterator.next();
            if(dnnModelsArrayList.contains(taskToAddForGatekeeperV0Scan.getTaskMetadata().getDnnModel())){
                taskListForGateKeeperV0Scan.add(taskToAddForGatekeeperV0Scan);
                logger.info(System.currentTimeMillis() + " Task with id " + taskToAddForGatekeeperV0Scan.getTaskMetadata().getTaskId() + " and size of the array list for gatekeeper is " + taskListForGateKeeperV0Scan.size() + " with DNN Model " + taskToAddForGatekeeperV0Scan.getTaskMetadata().getDnnModel());
            }
        }
        return taskListForGateKeeperV0Scan;
    }

    public synchronized boolean remove(Task task) {
        return queue.remove(task);
    }

    public synchronized boolean scanForDeadlineViolation(long executionTimeOfStolenTask, String taskId) {
        ListIterator<Task> taskListIterator = queue.listIterator();
        long partialSumPrior = executionTimeOfStolenTask;
        boolean isDeadlineViolating = false;

        while (taskListIterator.hasNext()) {
            Task task = taskListIterator.next();
            if ((task.getTaskMetadata().getExpectedExecutionTimeOnEdge() + partialSumPrior) > (task.getTaskMetadata().getDeadline() - System.currentTimeMillis())) {
                isDeadlineViolating = true;
                break;
            } else {
                partialSumPrior += task.getTaskMetadata().getExpectedExecutionTimeOnEdge();
            }
        }
        return isDeadlineViolating;
    }

    public synchronized boolean scanForStarvation(long thresholdTime) {
        boolean isStarving = false;
        long partialSumPrior = 0L;
        ListIterator<Task> taskListIterator = queue.listIterator();
        while (taskListIterator.hasNext() && !isStarving) {
            Task task = taskListIterator.next();
            if ((task.getTaskMetadata().getExpectedExecutionTimeOnEdge() + partialSumPrior + System.currentTimeMillis()) < (task.getTaskMetadata().getDeadline() - thresholdTime)) {
                partialSumPrior += task.getTaskMetadata().getExpectedExecutionTimeOnEdge();
            } else {
                isStarving = true;
            }
        }
        return isStarving;
    }

    public synchronized PriorityBlockingQueue<ScannedTask> scanOnProfitAndReturnPriorityQueue(long slack) {
        ListIterator<Task> taskIterator = queue.listIterator();
        long utilityDiff = 0L;
        PriorityBlockingQueue<ScannedTask> scannedTasksPriorityQueue = new PriorityBlockingQueue<ScannedTask>(2, Comparator.comparingLong(ScannedTask::getScannedTaskPriority));
        while (taskIterator.hasNext()) {
            Task iteratorTask = taskIterator.next();
            if ((iteratorTask.getTaskMetadata().getExpectedExecutionTimeOnEdge()) <= slack) {
                DNNModels dnnModel = iteratorTask.getTaskMetadata().getDnnModel();
                if (Application.hasNegativeUtilityOnCloud(dnnModel)) {
                    utilityDiff = (long) Application.getUtilityDiff(dnnModel) + 1000L;
                } else {
                    utilityDiff = (long) Application.getUtilityDiff(dnnModel);
                }
            }
            scannedTasksPriorityQueue.put(new ScannedTask(iteratorTask, (Integer.MAX_VALUE - utilityDiff)));
        }
        return scannedTasksPriorityQueue;
    }


    // checkTaskDeadline false -- sum of execution times of all tasks in the queue should be considered
    // checkTaskDeadline true -- sum of execution times of tasks ahead of the queue should be considered -- yes
    // removeDeadlineViolations false -- do not remove tasks after the new tasks which will violate their deadline
    // removeDeadlineViolations true -- remove tasks after the new tasks which will violate their deadline -- yes

    public synchronized List<Task> add(Task task, boolean checkTaskDeadline, boolean removeDeadlineViolations, boolean checkDeadlineViolationFromTail,
                                       AtomicLong executionEndTimeOnEdge) {
        // cases : (1) incoming task has priority less than the first task -- index changes to 0
        // (2) incoming task has priority more than the last task -- index changes to n
        // (3) incoming task has priority somewhere in middle -- index changes to the index of the last task which has priority less than new task

        ListIterator<Task> taskIterator = queue.listIterator();
        int index = 0;
        long remainingEdgeProcessingTime = executionEndTimeOnEdge.get() - System.currentTimeMillis();
        if (remainingEdgeProcessingTime <= 0L) {
            remainingEdgeProcessingTime = 0L;
        }
        logger.info(System.currentTimeMillis() + " The remaining edge processing time is " + remainingEdgeProcessingTime + " ms for task with id " + task.getTaskMetadata().getTaskId());
        logger.info(System.currentTimeMillis() + "The task has available time to be scheduled to the edge for task with id " + task.getTaskMetadata().getTaskId() + " of " + (task.getTaskMetadata().getDeadline() - System.currentTimeMillis()) + " ms.");
        long partialSumOfExecutionTimesOnEdge = remainingEdgeProcessingTime;
        List<Task> removedTasks = new ArrayList<>();
        List<Task> tentativeRemovedTasks = new ArrayList<>();
        long scoreForExistingTasks = 0L;

        while (taskIterator.hasNext()) {
            Task iteratorTask = taskIterator.next();
            // incoming task has priority greater than at least the first task
            if (iteratorTask.getTaskPriority() <= task.getTaskPriority()) { // lower number means higher priority
                partialSumOfExecutionTimesOnEdge += iteratorTask.getTaskMetadata().getExpectedExecutionTimeOnEdge();
                index++;
            } else {
                // we have found a location to insert
                if (checkTaskDeadline &&
                        ((task.getTaskMetadata().getDeadline() - System.currentTimeMillis()) > (partialSumOfExecutionTimesOnEdge + task.getTaskMetadata().getExpectedExecutionTimeOnEdge()))) {
                    // we will be performing an insertion
                    if (removeDeadlineViolations && (!checkDeadlineViolationFromTail)) {
                        // check for deadline violations for tasks after current task insertion point
                        long partialSumPosterior = partialSumOfExecutionTimesOnEdge;
                        partialSumPosterior += task.getTaskMetadata().getExpectedExecutionTimeOnEdge();
                        while (taskIterator.hasNext()) {
                            Task postTask = taskIterator.next(); // "A B [X] C D"
                            if ((postTask.getTaskMetadata().getDeadline() - System.currentTimeMillis()) < (partialSumPosterior + postTask.getTaskMetadata().getExpectedExecutionTimeOnEdge())) { // Post task will miss deadline. Need to remove.
                                logger.info("remove deadline violations is true, and partial sum deadline condition satisfies, so checking for deadline violations for tasks behind the inserted task");
                                scoreForExistingTasks += Application.calculateScore(postTask);
                                tentativeRemovedTasks.add(postTask);
                            } else {
                                partialSumPosterior += postTask.getTaskMetadata().getExpectedExecutionTimeOnEdge();
                            }
                        }

                        long scoreForIncomingTask = Application.calculateScore(task);
                        logger.info(System.currentTimeMillis() + " Score for incoming tasks " + scoreForIncomingTask + " with task id " + task.getTaskMetadata().getTaskId() + " and score for existing tasks " + scoreForExistingTasks);
                        if (scoreForExistingTasks < scoreForIncomingTask) {
                            removedTasks = tentativeRemovedTasks;
                            logger.info("The utility lost from tasks on edge is less than incoming task for task id " + task.getTaskMetadata().getTaskId());
                        } else {
                            // If utility lost is greater than incoming task, send the incoming task to cloud
                            logger.info("The utility lost from tasks on edge is greater than incoming task for task id " + task.getTaskMetadata().getTaskId());
                            return null;
                        }
                    } else if (removeDeadlineViolations && checkDeadlineViolationFromTail) {
                        throw new UnsupportedOperationException(); // TODO: support checkDeadlineViolationFromTail = true
                    } else {
                        break;
                    }
                } else {
                    break;
                }
            }
        }
        // if deadline is passed, check against deadline
        if (checkTaskDeadline &&
                ((task.getTaskMetadata().getDeadline() - System.currentTimeMillis()) > (partialSumOfExecutionTimesOnEdge + task.getTaskMetadata().getExpectedExecutionTimeOnEdge()))) {
            queue.add(index, task);
            task.getTaskLogger().setEdgeQueuePutTime(System.currentTimeMillis());
            logger.info("Task with task id " + task.getTaskMetadata().getTaskId() + " inserted at index " + index + " and has DNN model " + task.getTaskMetadata().getDnnModel());
            return removedTasks;
        }
        // just insert and return the empty array
        else if (!checkTaskDeadline) {
            queue.add(index, task);
            logger.info("Task with task id " + task.getTaskMetadata().getTaskId() + " inserted at index " + index + " and has DNN model " + task.getTaskMetadata().getDnnModel());
            return removedTasks;
        } else {
            return null;
        }
    }

    // for tpds2020 baseline
    public synchronized List<Task> addAvgCompletionTime(Task task, boolean checkTaskDeadline, boolean removeDeadlineViolations, boolean checkDeadlineViolationFromTail, AtomicLong executionEndTimeOnEdge, AtomicLong totalCompletionTimeOnEdge, AtomicLong totalTasksCompletedOnEdge, int algorithm) {
        // cases : (1) incoming task has priority less than the first task -- index changes to 0
        // (2) incoming task has priority more than the last task -- index changes to n
        // (3) incoming task has priority somewhere in middle -- index changes to the index of the last task which has priority less than new task

        ListIterator<Task> taskIterator = queue.listIterator();
        int index = 0;
        long remainingEdgeProcessingTime = executionEndTimeOnEdge.get() - System.currentTimeMillis();
        if (remainingEdgeProcessingTime <= 0L) {
            remainingEdgeProcessingTime = 0L;
        }
        logger.info(System.currentTimeMillis() + " The remaining edge processing time is " + remainingEdgeProcessingTime + " ms for task with id " + task.getTaskMetadata().getTaskId());
        logger.info(System.currentTimeMillis() + "The task has available time to be scheduled to the edge for task with id " + task.getTaskMetadata().getTaskId() + " of " + (task.getTaskMetadata().getDeadline() - System.currentTimeMillis()) + " ms.");
        long partialSumOfExecutionTimesOnEdge = remainingEdgeProcessingTime;
        List<Task> removedTasks = new ArrayList<>();
        List<Task> tentativeRemovedTasks = new ArrayList<>();
        long scoreForExistingTasks = 0L;

        long expectedExecTimeForTentativeRemovedTasks = 0L;
        double avgCompletionTimeWithIncomingTask = 0.0;
        double avgCompletionTimeWithTentativeRemovedTasks = 0.0;

        while (taskIterator.hasNext()) {
            Task iteratorTask = taskIterator.next();
            // incoming task has priority greater than at least the first task
            if (iteratorTask.getTaskPriority() <= task.getTaskPriority()) { // lower number means higher priority
                partialSumOfExecutionTimesOnEdge += iteratorTask.getTaskMetadata().getExpectedExecutionTimeOnEdge();
                index++;
            } else {
                // we have found a location to insert
                if (checkTaskDeadline &&
                        ((task.getTaskMetadata().getDeadline() - System.currentTimeMillis()) > (partialSumOfExecutionTimesOnEdge + task.getTaskMetadata().getExpectedExecutionTimeOnEdge()))) {
                    // we will be performing an insertion
                    if (removeDeadlineViolations && (!checkDeadlineViolationFromTail) ) {
                        // check for deadline violations for tasks after current task insertion point
                        long partialSumPosterior = partialSumOfExecutionTimesOnEdge;
                        partialSumPosterior += task.getTaskMetadata().getExpectedExecutionTimeOnEdge();

                        // float avgCompletionTimeForExistingTasks = 0.0f;
                        while (taskIterator.hasNext()) {
                            Task postTask = taskIterator.next(); // "A B [X] C D"
                            if ((postTask.getTaskMetadata().getDeadline() - System.currentTimeMillis()) < (partialSumPosterior + postTask.getTaskMetadata().getExpectedExecutionTimeOnEdge())) { // Post task will miss deadline, will have to remove and send to cloud
                                logger.info("remove deadline violations is true, and partial sum deadline condition satisfies, so checking for deadline violations for tasks behind the inserted task");

                                // TODO: if improves expected avg completion time, add the task and remove others, else continue
                                expectedExecTimeForTentativeRemovedTasks += postTask.getTaskMetadata().getExpectedExecutionTimeOnEdge();
                                tentativeRemovedTasks.add(postTask);

                            } else {
                                partialSumPosterior += postTask.getTaskMetadata().getExpectedExecutionTimeOnEdge();
                            }
                        }

                        if (algorithm == 1112 && tentativeRemovedTasks.size() > 1){
                            // If utility lost is greater than incoming task, send the incoming task to cloud
                            logger.info("algo 1112: Not Removing Tasks: The average completion time for tasks on edge is lesser than if inserted incoming task with task id " + task.getTaskMetadata().getTaskId());
                            return null;
                        }

                        // long expectedExecTimeForIncomingTask = task.getTaskMetadata().getExpectedExecutionTimeOnEdge;
                        // logger.info(System.currentTimeMillis() + " Expected execution time for incoming task " + expectedExecTimeForIncomingTask + " with task id " + task.getTaskMetadata().getTaskId() + " and expected execution time for existing tasks " + expectedExecTimeForExistingTasks);

                        avgCompletionTimeWithIncomingTask = (double)(totalCompletionTimeOnEdge.longValue() + task.getTaskMetadata().getExpectedExecutionTimeOnEdge()) / (totalTasksCompletedOnEdge.longValue() + 1);

                        // expectedExecTimeForExistingTasks += postTask.getTaskMetadata().getExpectedExecutionTimeOnEdge();
                        if (totalTasksCompletedOnEdge.longValue() > 0) {
                            avgCompletionTimeWithTentativeRemovedTasks = (double)(totalCompletionTimeOnEdge.longValue() + expectedExecTimeForTentativeRemovedTasks) / (totalTasksCompletedOnEdge.longValue() + tentativeRemovedTasks.size());
                        }

                        logger.info(System.currentTimeMillis() + " Average completion time for tasks on edge with incoming task " + avgCompletionTimeWithIncomingTask + " and with tentative removed tasks " + avgCompletionTimeWithTentativeRemovedTasks);
                        
                        if (avgCompletionTimeWithTentativeRemovedTasks > avgCompletionTimeWithIncomingTask) {
                            // logger.info("algo 1111 debug: Size of edge queue is" + queue.size());
                            removedTasks = tentativeRemovedTasks;
                            logger.info("Removing Tasks; The average completion time for tasks on edge is greater than if inserted incoming task with task id " + task.getTaskMetadata().getTaskId());

                            // for (Task removedTask : removedTasks) {
                                // logger.info("algo 1111 debug: Removing task with task id " + removedTask.getTaskMetadata().getTaskId());                           
                            // }
                        } else {
                            // If utility lost is greater than incoming task, send the incoming task to cloud
                            logger.info("Not Removing Tasks: The average completion time for tasks on edge is lesser than if inserted incoming task with task id " + task.getTaskMetadata().getTaskId());
                            return null;
                        }
                    } else if (removeDeadlineViolations && checkDeadlineViolationFromTail) {
                        throw new UnsupportedOperationException(); // TODO: support checkDeadlineViolationFromTail = true
                    } else {
                        break;
                    }
                } else {
                    break;
                }
            }
        }
        // if deadline is passed, check against deadline
        if (checkTaskDeadline &&
                ((task.getTaskMetadata().getDeadline() - System.currentTimeMillis()) > (partialSumOfExecutionTimesOnEdge + task.getTaskMetadata().getExpectedExecutionTimeOnEdge()))) {
            queue.add(index, task);
            task.getTaskLogger().setEdgeQueuePutTime(System.currentTimeMillis());
            logger.info("Task with task id " + task.getTaskMetadata().getTaskId() + " inserted at index " + index + " and has DNN model " + task.getTaskMetadata().getDnnModel());
            return removedTasks;
        }
        // just insert and return the empty array
        else if (!checkTaskDeadline) {
            queue.add(index, task);
            logger.info("Task with task id " + task.getTaskMetadata().getTaskId() + " inserted at index " + index + " and has DNN model " + task.getTaskMetadata().getDnnModel());
            return removedTasks;
        } else {
            return null;
        }
    }
}
