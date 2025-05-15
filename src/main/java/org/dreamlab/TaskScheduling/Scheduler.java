package org.dreamlab.TaskScheduling;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.dreamlab.Classes.DNNModels;
import org.dreamlab.Classes.DNNPerfModel;
import org.dreamlab.Classes.Deployment;
import org.dreamlab.Classes.Task;
import org.dreamlab.Interfaces.IAlgorithms;
import org.dreamlab.Interfaces.IPriorityQueue;
import org.javatuples.Triplet;

import org.dreamlab.Classes.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;

public class Scheduler implements Runnable {

    static final Logger logger = Logger.getLogger(Scheduler.class);
    static final String path = "./src/main/resources/log4j.properties";

    LinkedBlockingQueue<Task> taskQueue;
    LinkedBlockingQueue<Task> postProcessingQueue;
    HashMap<DNNModels, Triplet<Integer, Double, Double>> utilityLookupTable;

    PriorityBlockingQueue<Task> cloudQueue;

    IPriorityQueue edgeQueue;

    int algorithm;
    LinkedBlockingQueue<Task> taskRemovedFromEdgeQueue;
    AtomicLong monotonicCounterForCloud;
    IPriorityQueue tentativeCloudQueue;
    AtomicLong executionEndTimeOnEdge;

    AtomicLongArray startCoolingTimeInMs;
    AtomicIntegerArray isCooling;
    long coolingDuration;
    AtomicLong[][] circularBuffer;
    AtomicIntegerArray bufferCount;
    HashMap<DNNModels, DNNPerfModel> expectedExecutionTime;

    // for tpds2020 baseline
    AtomicLong totalCompletionTimeOnEdge;
    AtomicLong totalTasksCompletedOnEdge;

    // for d3 + kalmia variant baseline
    HashMap<DNNModels, Long> deadlineBufferTable;

    public Scheduler(LinkedBlockingQueue<Task> taskQueue, IPriorityQueue edgeQueue, PriorityBlockingQueue<Task> cloudQueue,
                     int algorithm, LinkedBlockingQueue<Task> postProcessingQueue,
                     HashMap<DNNModels, Triplet<Integer, Double, Double>> utilityLookupTable, LinkedBlockingQueue<Task> taskRemovedFromEdgeQueue, AtomicLong monotonicCounterForCloud, IPriorityQueue tentativeCloudQueue,
                     AtomicLongArray startCoolingTimeInMs, AtomicIntegerArray isCooling, long coolingDuration, AtomicLong[][] circularBuffer, AtomicIntegerArray bufferCount,
                     HashMap<DNNModels, DNNPerfModel> expectedExecutionTime, AtomicLong executionEndTimeOnEdge,

                     // for tpds2020 baseline
                     AtomicLong totalCompletionTimeOnEdge, AtomicLong totalTasksCompletedOnEdge,
                     
                     // for d3 + kalmia variant baseline
                     HashMap<DNNModels, Long> deadlineBufferTable
                     ) {

        this.edgeQueue = edgeQueue;
        this.cloudQueue = cloudQueue;
        this.taskQueue = taskQueue;
        this.algorithm = algorithm;
        this.postProcessingQueue = postProcessingQueue;
        this.utilityLookupTable = utilityLookupTable;
        this.taskRemovedFromEdgeQueue = taskRemovedFromEdgeQueue;
        this.monotonicCounterForCloud = monotonicCounterForCloud;
        this.tentativeCloudQueue = tentativeCloudQueue;
        this.startCoolingTimeInMs = startCoolingTimeInMs;
        this.isCooling = isCooling;
        this.coolingDuration = coolingDuration;
        this.circularBuffer = circularBuffer;
        this.bufferCount = bufferCount;
        this.expectedExecutionTime = expectedExecutionTime;
        this.executionEndTimeOnEdge = executionEndTimeOnEdge;

        // for tpds2020 baseline
        this.totalCompletionTimeOnEdge = totalCompletionTimeOnEdge;
        this.totalTasksCompletedOnEdge = totalTasksCompletedOnEdge;

        //for d3kalmia variant baseline
        this.deadlineBufferTable = deadlineBufferTable;
    }

    public void run() {

        logger.info("Starting thread for scheduler");
        PropertyConfigurator.configure(path);

        IAlgorithms algorithms = null;
        Task taskToBeExecuted = null;

        while (true) {
            try {
                Task task = taskQueue.poll(10, TimeUnit.SECONDS);
                if (task != null) {

                    if (task.isDummy()) {
                        logger.info(System.currentTimeMillis() + " this is just a warm up task with id " + task.getTaskMetadata().getTaskId());
                        task.setExecutionFlag(Deployment.EDGE);
                        edgeQueue.add(task,false,false,false,new AtomicLong(0L));
                        task.setExecutionFlag(Deployment.CLOUD);
                        cloudQueue.put(task);
                    } else {
                        logger.info(System.currentTimeMillis() + " Task to be scheduled. " + task.getTaskMetadata().getTaskId());
                        task.getTaskLogger().setTaskQueueRetrieveTime(System.currentTimeMillis());
                        try {
                            // only edge baselines
                            if (algorithm == 1 || algorithm == 3 || algorithm == 5 || algorithm == 7 || algorithm == 9 || algorithm == 21 || algorithm == 1114) {
                                taskToBeExecuted = task;
                                taskToBeExecuted.setExecutionFlag(Deployment.EDGE);
                            }
                            else if (algorithm == 2 || algorithm == 4 || algorithm == 6 || algorithm == 8 || algorithm == 10 || algorithm == 22){
                                taskToBeExecuted = task;
                                Triplet<Integer, Double, Double> triplet = utilityLookupTable.get(taskToBeExecuted.getTaskMetadata().getDnnModel());
                                if ((triplet.getValue0() - triplet.getValue1()) > 0) {
                                    taskToBeExecuted.setExecutionFlag(Deployment.CLOUD);
                                }
                                else{
                                    taskToBeExecuted.setExecutionFlag(Deployment.DROP_COST);
                                }
                            }
                            else if (algorithm == 31 || algorithm == 35 || algorithm == 91 || algorithm == 32 || algorithm == 42 || (algorithm >= 51 && algorithm <= 56) || (algorithm >= 61 && algorithm <= 66) || (algorithm >= 71 && algorithm <= 76) || (algorithm >= 81 && algorithm <= 86)) {
                                taskToBeExecuted = task;
                                taskToBeExecuted.setExecutionFlag(Deployment.EDGE);

                                List<Task> taskList = new ArrayList<>();
                                // cloud with migration
                                if (algorithm == 31 || algorithm == 35 ||algorithm == 91 || algorithm == 32 || algorithm == 42 || (algorithm >= 51 && algorithm <= 56) || (algorithm >= 81 && algorithm <= 86)) {
                                    taskList = edgeQueue.add(taskToBeExecuted, true, true, false, executionEndTimeOnEdge);
                                }
                                // cloud with retention
                                else if ((algorithm >= 61 && algorithm <= 66) || (algorithm >= 71 && algorithm <= 76)) {
                                    taskList = edgeQueue.add(taskToBeExecuted, true, false, false, executionEndTimeOnEdge);
                                }
                                logger.info(" Received task list " + taskList + "for task with  id " + taskToBeExecuted.getTaskMetadata().getTaskId());
                                // Edge+Cloud with deadline check and migration of removed tasks to cloud
                                // removedTasks = priorityQueue.add(task, checkTaskDeadline=true, removeDeadlineViolations=true, checkDeadlineViolationFromTail=false)
                                // Case 1: removedTasks=null ==> New task not inserted in edge.
                                //      cloudQueue.offer(task) // check if we will violate deadline on cloud  and/or if profit on cloud is positivve
                                // Case 2: removedTasks=list(0) ==> New task inserted in edge queue and no old tasks removed. Nothing more to do.
                                // Case 2: removedTasks=list(n) ==> New task inserted in edge queue and "n" old tasks removed.
                                //  while(rtask in removedTasks)
                                //      cloudQueue.offer(rtask) // check if we will violate deadline on cloud  and/or if profit on cloud is positive

                                if (taskList == null) {
                                    logger.info(System.currentTimeMillis() + "Task list received is null for " + taskToBeExecuted.getTaskMetadata().getTaskId() + " so, checking for cloud conditions.");

                                    Triplet<Integer, Double, Double> triplet = utilityLookupTable.get(taskToBeExecuted.getTaskMetadata().getDnnModel());
                                    if ((triplet.getValue0() - triplet.getValue1()) > 0) {
                                        int modelNumber = 0;
                                        DNNModels dnnModel = taskToBeExecuted.getTaskMetadata().getDnnModel();
                                        switch (dnnModel) {
                                            case HAZARD_VEST:
                                                modelNumber = 0;
                                                break;
                                            case DISTANCE_ESTIMATION_VIP:
                                                modelNumber = 1;
                                                break;
                                            case MASK_DETECTION:
                                                modelNumber = 2;
                                                break;
                                            case CROWD_DENSITY:
                                                modelNumber = 3;
                                                break;
                                            case BODY_POSE_ESTIMATION:
                                                modelNumber = 4;
                                                break;
                                            case DISTANCE_ESTIMATION_OBJECT:
                                                modelNumber = 5;
                                                break;
                                        }
                                        if (taskToBeExecuted.getTaskMetadata().getExpectedExecutionTimeOnCloud() > (taskToBeExecuted.getTaskMetadata().getDeadline() - System.currentTimeMillis())) {
                                            logger.info(System.currentTimeMillis() + " Checking if task has available time to be scheduled to the cloud for task with id " + taskToBeExecuted.getTaskMetadata().getTaskId());
                                            if (algorithm >= 71) {
                                                if (isCooling.get(modelNumber) == 0) {
                                                    startCoolingTimeInMs.set(modelNumber, System.currentTimeMillis());
                                                    isCooling.set(modelNumber, 1);
                                                    taskToBeExecuted.setExecutionFlag(Deployment.DROP_TIME);
                                                    logger.info(System.currentTimeMillis() + " Cooling has been started by task with id " + taskToBeExecuted.getTaskMetadata().getTaskId());
                                                } else {
                                                    if (System.currentTimeMillis() - startCoolingTimeInMs.get(modelNumber) > coolingDuration) {
                                                        logger.info(System.currentTimeMillis() + " Cooling period has exceed the cooling duration, hence stopping by task with id " + taskToBeExecuted.getTaskMetadata().getTaskId());
                                                        startCoolingTimeInMs.set(modelNumber, -1L);
                                                        isCooling.set(modelNumber, 0);
                                                        taskToBeExecuted.getTaskMetadata().setStoppedCooling(true);
                                                        taskToBeExecuted.getTaskMetadata().setExpectedExecutionTimeOnCloud(expectedExecutionTime.get(dnnModel).getCloudTime(1));
                                                        taskToBeExecuted.getTaskMetadata().setTriggerTimeForCloud(taskToBeExecuted.getTaskMetadata().getDeadline() - expectedExecutionTime.get(dnnModel).getCloudTime(1) - 10L);
                                                        logger.info(System.currentTimeMillis() + " Expected execution time on cloud is set to default for task with id " + taskToBeExecuted.getTaskMetadata().getTaskId() +
                                                                " and value is " + taskToBeExecuted.getTaskMetadata().getExpectedExecutionTimeOnCloud());
                                                        // recheck
                                                        if (System.currentTimeMillis() + taskToBeExecuted.getTaskMetadata().getExpectedExecutionTimeOnCloud() <= taskToBeExecuted.getTaskMetadata().getDeadline()) {
                                                            logger.info(System.currentTimeMillis() + " Rechecking of task is successful for task with id " + taskToBeExecuted.getTaskMetadata().getTaskId());
                                                            taskToBeExecuted.setExecutionFlag(Deployment.CLOUD);
                                                        } else {
                                                            taskToBeExecuted.setExecutionFlag(Deployment.DROP_TIME);
                                                            logger.info(System.currentTimeMillis() + " Rechecking task timing check failed. ");
                                                        }
                                                    } else {
                                                        taskToBeExecuted.setExecutionFlag(Deployment.DROP_TIME);
                                                        logger.info(System.currentTimeMillis() + " Cooling period going on for task with id " + taskToBeExecuted.getTaskMetadata().getTaskId());
                                                    }
                                                }
                                            } else {
                                                taskToBeExecuted.setExecutionFlag(Deployment.DROP_TIME);
                                                logger.info(System.currentTimeMillis() + "Dropping due to time constraints " + taskToBeExecuted.getTaskMetadata().getTaskId());
                                            }
                                        } else {
                                            taskToBeExecuted.setExecutionFlag(Deployment.CLOUD);
                                            logger.info(System.currentTimeMillis() + " The task has available time to be scheduled to the cloud for task with id " + taskToBeExecuted.getTaskMetadata().getTaskId());
                                        }
                                    } else {
                                        taskToBeExecuted.setExecutionFlag(Deployment.DROP_COST);
                                        logger.info(System.currentTimeMillis() + "Incurring negative utility, hence dropping due to cost with model " + taskToBeExecuted.getTaskMetadata().getDnnModel());

                                    }
                                } else if ((taskList != null) && (taskList.size() > 0)) {
                                    logger.info(System.currentTimeMillis() + " Removed a total of " + taskRemovedFromEdgeQueue.size() + " because of inserting task with id " + taskToBeExecuted.getTaskMetadata().getTaskId());
                                    for (Task removedTask : taskList) {
                                        removedTask.getTaskLogger().setEdgeQueueRetrieveTime(System.currentTimeMillis());
                                        logger.info(System.currentTimeMillis() + " Removing task with id " + removedTask.getTaskMetadata().getTaskId() + " and DNN model " +
                                                removedTask.getTaskMetadata().getDnnModel() + " because of task with id " + taskToBeExecuted.getTaskMetadata().getTaskId() + " and DNN model " + taskToBeExecuted.getTaskMetadata().getDnnModel());
                                        edgeQueue.remove(removedTask);
                                    }
                                    taskRemovedFromEdgeQueue.addAll(taskList);
                                }
                            }

                            // for tpds2020 baseline
                            else if (algorithm == 1111){
                                taskToBeExecuted = task;
                                List<Task> taskList = new ArrayList<>();

                                // trying to insert at a suitable position/replace with a task in edge queue
                                logger.info(System.currentTimeMillis() + " Algo 1111: Trying to insert task with id " + taskToBeExecuted.getTaskMetadata().getTaskId());

                                taskToBeExecuted.setExecutionFlag(Deployment.EDGE);
                                taskList = edgeQueue.addAvgCompletionTime(taskToBeExecuted, true, true, false, executionEndTimeOnEdge, totalCompletionTimeOnEdge, totalTasksCompletedOnEdge, algorithm);
                                // schedule all tasks in taskList to cloud, if taskList is not empty. If task could not be inserted in edge queue, it is returned in taskList, so scheduled on cloud

                                if (taskList == null){
                                    logger.info(System.currentTimeMillis() + "Task list received is null for " + taskToBeExecuted.getTaskMetadata().getTaskId() + " so, checking for cloud conditions.");

                                    Triplet<Integer, Double, Double> triplet = utilityLookupTable.get(taskToBeExecuted.getTaskMetadata().getDnnModel());
                                    if ((triplet.getValue0() - triplet.getValue1()) > 0) {
                                        int modelNumber = 0;
                                        DNNModels dnnModel = taskToBeExecuted.getTaskMetadata().getDnnModel();
                                        switch (dnnModel) {
                                            case HAZARD_VEST:
                                                modelNumber = 0;
                                                break;
                                            case DISTANCE_ESTIMATION_VIP:
                                                modelNumber = 1;
                                                break;
                                            case MASK_DETECTION:
                                                modelNumber = 2;
                                                break;
                                            case CROWD_DENSITY:
                                                modelNumber = 3;
                                                break;
                                            case BODY_POSE_ESTIMATION:
                                                modelNumber = 4;
                                                break;
                                            case DISTANCE_ESTIMATION_OBJECT:
                                                modelNumber = 5;
                                                break;
                                        }
                                        // logger.info(System.currentTimeMillis() + " Checking if task has available time to be scheduled to the cloud for task with id " + taskToBeExecuted.getTaskMetadata().getTaskId());

                                        // if (taskToBeExecuted.getTaskMetadata().getExpectedExecutionTimeOnCloud() < (taskToBeExecuted.getTaskMetadata().getDeadline() - System.currentTimeMillis())) {
                                        taskToBeExecuted.setExecutionFlag(Deployment.CLOUD);
                                        logger.info(System.currentTimeMillis() + " The task has available time to be scheduled to the cloud for task with id " + taskToBeExecuted.getTaskMetadata().getTaskId());
                                        // } else {
                                        //         taskToBeExecuted.setExecutionFlag(Deployment.DROP_TIME);
                                        //         logger.info(System.currentTimeMillis() + "Dropping due to time constraints " + taskToBeExecuted.getTaskMetadata().getTaskId());
                                        // }
                                    } else {
                                        taskToBeExecuted.setExecutionFlag(Deployment.DROP_COST);
                                        logger.info(System.currentTimeMillis() + "Incurring negative utility, hence dropping due to cost with model " + taskToBeExecuted.getTaskMetadata().getDnnModel());

                                    }
                                }
                                if ((taskList != null) && (taskList.size() > 0)) {
                                    logger.info(System.currentTimeMillis() + " Removed a total of " + taskRemovedFromEdgeQueue.size() + " because of inserting task with id " + taskToBeExecuted.getTaskMetadata().getTaskId());
                                    for (Task removedTask : taskList) {
                                        removedTask.getTaskLogger().setEdgeQueueRetrieveTime(System.currentTimeMillis());
                                        logger.info(System.currentTimeMillis() + " Removing task with id " + removedTask.getTaskMetadata().getTaskId() + " and DNN model " +
                                                removedTask.getTaskMetadata().getDnnModel() + " because of task with id " + taskToBeExecuted.getTaskMetadata().getTaskId() + " and DNN model " + taskToBeExecuted.getTaskMetadata().getDnnModel());
                                        edgeQueue.remove(removedTask);
                                    }
                                    taskRemovedFromEdgeQueue.addAll(taskList);
                                }
                            }

                            // for tpds2020 baseline -- v1
                            else if (algorithm == 1112){
                                taskToBeExecuted = task;
                                List<Task> taskList = new ArrayList<>();

                                // trying to insert at a suitable position/replace with a task in edge queue
                                logger.info(System.currentTimeMillis() + " Algo 1112: Trying to insert task with id " + taskToBeExecuted.getTaskMetadata().getTaskId());

                                taskToBeExecuted.setExecutionFlag(Deployment.EDGE);
                                taskList = edgeQueue.addAvgCompletionTime(taskToBeExecuted, true, true, false, executionEndTimeOnEdge, totalCompletionTimeOnEdge, totalTasksCompletedOnEdge, algorithm);
                                // schedule all tasks in taskList to cloud, if taskList is not empty. If task could not be inserted in edge queue, it is returned in taskList, so scheduled on cloud

                                if (taskList == null){
                                    logger.info(System.currentTimeMillis() + "Task list received is null for " + taskToBeExecuted.getTaskMetadata().getTaskId() + " so, checking for cloud conditions.");
                                    taskToBeExecuted.setExecutionFlag(Deployment.CLOUD);
                                    logger.info(System.currentTimeMillis() + " The task has available time to be scheduled to the cloud for task with id " + taskToBeExecuted.getTaskMetadata().getTaskId());
                                }
                                if ((taskList != null) && (taskList.size() > 0)) {
                                    logger.info(System.currentTimeMillis() + " Removed a total of " + taskRemovedFromEdgeQueue.size() + " because of inserting task with id " + taskToBeExecuted.getTaskMetadata().getTaskId());
                                    for (Task removedTask : taskList) {
                                        removedTask.getTaskLogger().setEdgeQueueRetrieveTime(System.currentTimeMillis());
                                        logger.info(System.currentTimeMillis() + " Removing task with id " + removedTask.getTaskMetadata().getTaskId() + " and DNN model " +
                                                removedTask.getTaskMetadata().getDnnModel() + " because of task with id " + taskToBeExecuted.getTaskMetadata().getTaskId() + " and DNN model " + taskToBeExecuted.getTaskMetadata().getDnnModel());
                                        edgeQueue.remove(removedTask);
                                    }
                                    taskRemovedFromEdgeQueue.addAll(taskList);
                                }
                            }

                            // for d3 + kalmia variant baseline
                            else if (algorithm == 1113){
                                taskToBeExecuted = task;
                                List<Task> taskList = new ArrayList<>();

                                // trying to insert at a suitable position/replace with a task in edge queue
                                logger.info(System.currentTimeMillis() + " Algo 1113: Trying to insert task with id " + taskToBeExecuted.getTaskMetadata().getTaskId());

                                taskToBeExecuted.setExecutionFlag(Deployment.EDGE);
                                taskList = edgeQueue.add(taskToBeExecuted, true, false, false, executionEndTimeOnEdge);

                                if (taskList == null){
                                    logger.info(System.currentTimeMillis() + "Task list received is null for " + taskToBeExecuted.getTaskMetadata().getTaskId() + " so, checking for cloud conditions.");

                                        if (taskToBeExecuted.getTaskMetadata().getExpectedExecutionTimeOnCloud() < (taskToBeExecuted.getTaskMetadata().getDeadline() - System.currentTimeMillis())) {
                                            taskToBeExecuted.setExecutionFlag(Deployment.CLOUD);
                                            logger.info(System.currentTimeMillis() + " The task has available time to be scheduled to the cloud for task with id " + taskToBeExecuted.getTaskMetadata().getTaskId());

                                        // check for non-urgent tasks
                                        } else if (taskToBeExecuted.getTaskMetadata().getDnnModel() == DNNModels.BODY_POSE_ESTIMATION || taskToBeExecuted.getTaskMetadata().getDnnModel() == DNNModels.MASK_DETECTION || taskToBeExecuted.getTaskMetadata().getDnnModel() == DNNModels.DISTANCE_ESTIMATION_VIP) {

                                            // add deadline buffer to deadline
                                            TaskMetadata taskMetadata = taskToBeExecuted.getTaskMetadata();
                                        
                                            taskMetadata.setDeadline(taskToBeExecuted.getTaskMetadata().getDeadline() + deadlineBufferTable.get(taskToBeExecuted.getTaskMetadata().getDnnModel()));
                                            taskToBeExecuted.setTaskMetadata(taskMetadata);
                                            taskList = edgeQueue.add(taskToBeExecuted, true, false, false, executionEndTimeOnEdge);

                                            logger.info(System.currentTimeMillis() + " Algo 1113: Changed deadline of task with task id " + taskToBeExecuted.getTaskMetadata().getTaskId() + " with DNN model " + taskToBeExecuted.getTaskMetadata().getDnnModel() + " to " + taskToBeExecuted.getTaskMetadata().getDeadline());

                                            taskList = edgeQueue.add(taskToBeExecuted, true, false, false, executionEndTimeOnEdge);

                                            if (taskList == null){

                                                // TODO: why not check the deadline here?
                                                taskToBeExecuted.setExecutionFlag(Deployment.CLOUD);
                                                logger.info(System.currentTimeMillis() + " The task has available time to be scheduled to the cloud for task with id " + taskToBeExecuted.getTaskMetadata().getTaskId());
                                            }
                                        }
                                        else {
                                            taskToBeExecuted.setExecutionFlag(Deployment.DROP_TIME);
                                            logger.info(System.currentTimeMillis() + "Dropping due to time constraints " + taskToBeExecuted.getTaskMetadata().getTaskId());
                                        }
                                }
                                if ((taskList != null) && (taskList.size() > 0)) {
                                    logger.info(System.currentTimeMillis() + " Removed a total of " + taskRemovedFromEdgeQueue.size() + " because of inserting task with id " + taskToBeExecuted.getTaskMetadata().getTaskId());
                                    for (Task removedTask : taskList) {
                                        removedTask.getTaskLogger().setEdgeQueueRetrieveTime(System.currentTimeMillis());
                                        logger.info(System.currentTimeMillis() + " Removing task with id " + removedTask.getTaskMetadata().getTaskId() + " and DNN model " +
                                                removedTask.getTaskMetadata().getDnnModel() + " because of task with id " + taskToBeExecuted.getTaskMetadata().getTaskId() + " and DNN model " + taskToBeExecuted.getTaskMetadata().getDnnModel());
                                        edgeQueue.remove(removedTask);
                                    }
                                    taskRemovedFromEdgeQueue.addAll(taskList);
                                }
                            }

                                // make a copy of existing tasks in edge queue
                                // try to insert task from the back and look for deadline violations
                                // if found, add the task to task queue and set the edge task queue to this copied queue
                                // if not found, try to replace tasks from the back, and calc avg completion time
                                // if lesser avg completion time, replace task with new task and send older task to cloud



                            if (taskToBeExecuted.getExecutionFlag().equals(Deployment.EDGE)) {
                                if ((algorithm >= 1 && algorithm <= 10) || algorithm == 21 || algorithm == 22 || algorithm == 1114) {
                                    edgeQueue.add(taskToBeExecuted, false, false, false, executionEndTimeOnEdge);
                                    taskToBeExecuted.getTaskLogger().setEdgeQueuePutTime(System.currentTimeMillis());
                                }
                            } else if (taskToBeExecuted.getExecutionFlag().equals(Deployment.CLOUD)) {
                                // work stealing, send to tentative cloud queue
                                if (algorithm == 42 || algorithm == 32 || algorithm == 31 || algorithm == 35 || algorithm == 91) {
                                    logger.info(System.currentTimeMillis() + " Task with id: " + taskToBeExecuted.getTaskMetadata().getTaskId() + " has been put in the tentative cloud queue.");
                                    taskToBeExecuted.setTaskPriority(taskToBeExecuted.getTaskMetadata().getTriggerTimeForCloud());
                                    tentativeCloudQueue.add(taskToBeExecuted, false, false, false, new AtomicLong(0L));
                                    taskToBeExecuted.getTaskLogger().setTentativeCloudQueuePutTime(System.currentTimeMillis());
                                }
                                // Cloud is a FIFO for all the baselines
                                else {
                                    taskToBeExecuted.setTaskPriority(monotonicCounterForCloud.get());
                                    monotonicCounterForCloud.getAndAdd(1L);
                                    taskToBeExecuted.getTaskLogger().setCloudQueuePutTime(System.currentTimeMillis());
                                    cloudQueue.put(taskToBeExecuted);
                                }
                            } else if (taskToBeExecuted.getExecutionFlag().equals(Deployment.DROP_TIME) || taskToBeExecuted.getExecutionFlag().equals(Deployment.DROP_COST)) {
                                if ((taskToBeExecuted.getExecutionFlag().equals(Deployment.DROP_COST)) && (algorithm == 31 || algorithm == 91)) {
                                    logger.info(System.currentTimeMillis() + " Task with id: " + taskToBeExecuted.getTaskMetadata().getTaskId() + " has been put in the tentative cloud queue.");
                                    taskToBeExecuted.setTaskPriority(taskToBeExecuted.getTaskMetadata().getTriggerTimeForEdge());
                                    tentativeCloudQueue.add(taskToBeExecuted, false, false, false, new AtomicLong(0L));
                                    taskToBeExecuted.getTaskLogger().setTentativeCloudQueuePutTime(System.currentTimeMillis());
                                } else {
                                    taskToBeExecuted.getTaskLogger().setPostProcessingPutTime(System.currentTimeMillis());
                                    postProcessingQueue.put(taskToBeExecuted);
                                }
                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}
