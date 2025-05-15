package org.dreamlab.TaskScheduling;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.dreamlab.Classes.*;
import org.dreamlab.Interfaces.IExecutor;
import org.dreamlab.Interfaces.IPriorityQueue;
import org.dreamlab.gRPCHandler.Ack;
import org.dreamlab.gRPCHandler.InferencerGrpc;
import org.dreamlab.gRPCHandler.JobDetails;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.HashMap;

public class TaskExecutorEdge implements Runnable, IExecutor {
    private final InferencerGrpc.InferencerBlockingStub stub;
    IPriorityQueue edgeQueue;

    static final Logger logger = Logger.getLogger(TaskExecutorEdge.class);
    static final String path = "./src/main/resources/log4j.properties";

    LinkedBlockingQueue<Task> postProcessingQueue;
    int algorithm;
    IPriorityQueue tentativeCloudQueue;
    AtomicLong executionEndTimeOnEdge;

    static final long minimumExecutionTimeForAnyModel = 132L;
    static final long maximumExecutionTimeForAnyModel = 742L;
    static long slackOnTheEdge = 0L;

    HashMap<DNNModels, Long> deadlineBufferTable;

    public TaskExecutorEdge(String host, int port, IPriorityQueue edgeQueue, LinkedBlockingQueue<Task> postProcessingQueue,
                            int algorithm, IPriorityQueue tentativeCloudQueue,
                            AtomicLong executionEndTimeOnEdge, HashMap<DNNModels, Long> deadlineBufferTable) {
        this(ManagedChannelBuilder.forAddress(host, port).usePlaintext());
        this.edgeQueue = edgeQueue;
        this.postProcessingQueue = postProcessingQueue;
        this.algorithm = algorithm;
        this.tentativeCloudQueue = tentativeCloudQueue;
        this.executionEndTimeOnEdge = executionEndTimeOnEdge;
        this.deadlineBufferTable = deadlineBufferTable;
    }

    private TaskExecutorEdge(ManagedChannelBuilder<?> builder) {
        ManagedChannel channel = builder.build();
        stub = InferencerGrpc.newBlockingStub(channel);
    }

    @Override
    public void run() {

        System.out.println("Starting edge executor thread.. ");
        PropertyConfigurator.configure(path);
        // read from edge queue and send for processing to the server
        while (true) {
            try {
                Task task = edgeQueue.peek();
                // Reset deadline violation
                if (task == null) {
                    //TODO: if edge queue is empty, prepare for work stealing. Go for sleep only if there is a task that can be stolen.
                    if (((algorithm == 31) || (algorithm == 91)) && (tentativeCloudQueue.size() > 0)) {
                        logger.info("Starting the process of scanning for work stealing");
                        Task taskFromTentativeCloudQueue = null;
                        slackOnTheEdge = maximumExecutionTimeForAnyModel;

                        long tentativeCloudQueueScanStart = System.currentTimeMillis();
                        PriorityBlockingQueue<ScannedTask> tasksSortedFromTentativeCloudQueue = tentativeCloudQueue.scanOnProfitAndReturnPriorityQueue(slackOnTheEdge);
                        long tentativeCloudQueueScanEnd = System.currentTimeMillis();
                        logger.info(System.currentTimeMillis() + " The tentative cloud queue scan took " + (tentativeCloudQueueScanEnd - tentativeCloudQueueScanStart) + " ms.");
                        logger.info("Size of tasksSortedFromTentativeCloudQueue is:  " + tasksSortedFromTentativeCloudQueue.size() + " with available time remaining = " + slackOnTheEdge + " ms.");

                        while (tasksSortedFromTentativeCloudQueue.size() > 0) {
                            taskFromTentativeCloudQueue = tasksSortedFromTentativeCloudQueue.poll().getScannedTask();
                            logger.info("Size of tasksSortedFromTentativeCloudQueue after polling is:  " + tasksSortedFromTentativeCloudQueue.size() + " with available time for scanning remaining = " + slackOnTheEdge + " ms.");
                            if ((taskFromTentativeCloudQueue.getTaskMetadata().getExpectedExecutionTimeOnEdge()) < (taskFromTentativeCloudQueue.getTaskMetadata().getDeadline() - System.currentTimeMillis())) {
                                logger.info("Deadline violation check passed, going for work stealing for task id " + taskFromTentativeCloudQueue.getTaskMetadata().getTaskId());
                                boolean flag = tentativeCloudQueue.remove(taskFromTentativeCloudQueue);
                                logger.info("Removing task from tentative cloud queue with task id: " + taskFromTentativeCloudQueue.getTaskMetadata().getTaskId() + " and the flag status is " + flag);
                                if (flag) {
                                    // log the flag as well
                                    taskFromTentativeCloudQueue.getTaskLogger().setTentativeCloudQueueRetrieveTime(System.currentTimeMillis());
                                    taskFromTentativeCloudQueue.setExecutionFlag(Deployment.WORK_STEAL_FROM_CLOUD);
                                    taskFromTentativeCloudQueue.getTaskLogger().setBeforeExecutionTime(System.currentTimeMillis());
                                    logger.info(System.currentTimeMillis() + " Task with id" + taskFromTentativeCloudQueue.getTaskMetadata().getTaskId() + " is sent for execution on edge after work stealing");
                                    executionEndTimeOnEdge.set(System.currentTimeMillis() + taskFromTentativeCloudQueue.getTaskMetadata().getExpectedExecutionTimeOnEdge());
                                    executeInferencer(taskFromTentativeCloudQueue);
                                }
                            }
                        }

                    } else {
                        Thread.sleep(5L);
                        continue;
                    }

                }

                if (task != null) {
                    if (task.isDummy()) {
                        task = edgeQueue.poll();
                        logger.info(System.currentTimeMillis() + " Dummy task is sent for execution on edge with id " + task.getTaskMetadata().getTaskId());
                        executeInferencer(task);
                    } else {
                        logger.info(System.currentTimeMillis() + " Size of the edge queue is: " + edgeQueue.size());
//                    boolean isStarving = false;
                        // check to see if there is a slack available only for the specified algorithms.
                        if (((algorithm == 31) || (algorithm == 35) || (algorithm == 91)) && (tentativeCloudQueue.size() > 0)) {
                            logger.info("Starting the process of scanning for work stealing");
                            boolean isDeadlineViolating = true;
                            Task taskFromTentativeCloudQueue = null;

                            long workStealStart = System.currentTimeMillis();
                            slackOnTheEdge = task.getTaskMetadata().getTriggerTimeForEdge() - System.currentTimeMillis();

                            if (slackOnTheEdge > minimumExecutionTimeForAnyModel) {
                                long tentativeCloudQueueScanStart = System.currentTimeMillis();
                                PriorityBlockingQueue<ScannedTask> tasksSortedFromTentativeCloudQueue = tentativeCloudQueue.scanOnProfitAndReturnPriorityQueue(slackOnTheEdge);
                                long tentativeCloudQueueScanEnd = System.currentTimeMillis();
                                logger.info(System.currentTimeMillis() + " The tentative cloud queue scan took " + (tentativeCloudQueueScanEnd - tentativeCloudQueueScanStart) + " ms.");
                                logger.info("Size of tasksSortedFromTentativeCloudQueue is:  " + tasksSortedFromTentativeCloudQueue.size() + " with available time remaining = " + slackOnTheEdge + " ms.");
                                while (tasksSortedFromTentativeCloudQueue.size() > 0 && isDeadlineViolating) {
//                                    isDeadlineViolating = false;
                                    taskFromTentativeCloudQueue = tasksSortedFromTentativeCloudQueue.poll().getScannedTask();
                                    // this is to keep updating the slack time after every iteration for real-time checking of deadline
                                    slackOnTheEdge = task.getTaskMetadata().getTriggerTimeForEdge() - System.currentTimeMillis();
                                    logger.info("Size of tasksSortedFromTentativeCloudQueue after polling is:  " + tasksSortedFromTentativeCloudQueue.size() + " with available time for scanning remaining = " + slackOnTheEdge + " ms.");
                                    // TODO: Should add the remaining processing time on edge to pulled task as well? No, nothing is being processed on the edge right now.

                                    if (slackOnTheEdge < minimumExecutionTimeForAnyModel) {
                                        logger.info(System.currentTimeMillis() + " Not enough slack time remaining, so breaking the while loop.");
                                        break;
                                    }

                                    if (taskFromTentativeCloudQueue.getTaskMetadata().getExpectedExecutionTimeOnEdge() < slackOnTheEdge) {
                                        logger.info("Starting scanning of edge queue for work stealing for task id " + taskFromTentativeCloudQueue.getTaskMetadata().getTaskId() + " with DNN Model " + taskFromTentativeCloudQueue.getTaskMetadata().getDnnModel());
                                        long edgeQueueScanForDeadlineViolationStart = System.currentTimeMillis();
                                        isDeadlineViolating = edgeQueue.scanForDeadlineViolation(taskFromTentativeCloudQueue.getTaskMetadata().getExpectedExecutionTimeOnEdge(), taskFromTentativeCloudQueue.getTaskMetadata().getTaskId());
                                        long edgeQueueScanForDeadlineViolationEnd = System.currentTimeMillis();
                                        logger.info(System.currentTimeMillis() + " The edge queue scan took " + (edgeQueueScanForDeadlineViolationEnd - edgeQueueScanForDeadlineViolationStart) + " ms.");
                                        logger.info("The task from tentative cloud queue has is deadline violation as " + isDeadlineViolating);
                                    }
//                                    else{
//                                        isDeadlineViolating = true;
//                                    }
                                }
                                long workStealEnd = System.currentTimeMillis();
                                logger.info(System.currentTimeMillis() + " The entire work steal process overhead is " + (workStealEnd - workStealStart) + " ms.");
                            }

                            if (!isDeadlineViolating && ((taskFromTentativeCloudQueue.getTaskMetadata().getExpectedExecutionTimeOnEdge()) < (taskFromTentativeCloudQueue.getTaskMetadata().getDeadline() - System.currentTimeMillis()))) {
                                logger.info("Deadline violation check passed, going for work stealing for task id " + taskFromTentativeCloudQueue.getTaskMetadata().getTaskId());
                                boolean flag = tentativeCloudQueue.remove(taskFromTentativeCloudQueue);
                                logger.info("Removing task from tentative cloud queue with task id: " + taskFromTentativeCloudQueue.getTaskMetadata().getTaskId() + " and the flag status is " + flag);
                                if (flag) {
                                    // log the flag as well
                                    taskFromTentativeCloudQueue.getTaskLogger().setTentativeCloudQueueRetrieveTime(System.currentTimeMillis());
                                    taskFromTentativeCloudQueue.setExecutionFlag(Deployment.WORK_STEAL_FROM_CLOUD);
                                    taskFromTentativeCloudQueue.getTaskLogger().setBeforeExecutionTime(System.currentTimeMillis());
                                    logger.info(System.currentTimeMillis() + " Task with id" + taskFromTentativeCloudQueue.getTaskMetadata().getTaskId() + " is sent for execution on edge after work stealing");
                                    executionEndTimeOnEdge.set(System.currentTimeMillis() + taskFromTentativeCloudQueue.getTaskMetadata().getExpectedExecutionTimeOnEdge());
                                    executeInferencer(taskFromTentativeCloudQueue);
                                }
                            } else {
                                logger.info("Since no tasks from the tentative cloud queue are eligible, polling task from the edge queue.");
                                task = edgeQueue.poll();
                                task.getTaskLogger().setEdgeQueueRetrieveTime(System.currentTimeMillis());

                                if (task.getTaskMetadata().getExpectedExecutionTimeOnEdge() < (task.getTaskMetadata().getDeadline() - System.currentTimeMillis())) {
                                    task.getTaskLogger().setBeforeExecutionTime(System.currentTimeMillis());
                                    executionEndTimeOnEdge.set(System.currentTimeMillis() + task.getTaskMetadata().getExpectedExecutionTimeOnEdge());
                                    executeInferencer(task);
                                    logger.info(System.currentTimeMillis() + " Task with id" + task.getTaskMetadata().getTaskId() + " is sent for execution on edge ");
                                } else {
                                    task.setExecutionFlag(Deployment.EXPIRED_EDGE);
                                    task.getTaskLogger().setPostProcessingPutTime(System.currentTimeMillis());
                                    postProcessingQueue.put(task);
                                }
                            }
                        }
                        // algo other than work stealing
                        else {
                            logger.info("Algorithm value is: " + algorithm + " ,going for polling from edge.");
                            task = edgeQueue.poll();
                            // added in case task has been removed from edge queue because of gatekeeper logic
                            if (task != null){
                                task.getTaskLogger().setEdgeQueueRetrieveTime(System.currentTimeMillis());

                                if (task.getTaskMetadata().getExpectedExecutionTimeOnEdge() < (task.getTaskMetadata().getDeadline() - System.currentTimeMillis())) {
                                    task.getTaskLogger().setBeforeExecutionTime(System.currentTimeMillis());
                                    executionEndTimeOnEdge.set(System.currentTimeMillis() + task.getTaskMetadata().getExpectedExecutionTimeOnEdge());
                                    executeInferencer(task);
                                    logger.info(System.currentTimeMillis() + " Task with id" + task.getTaskMetadata().getTaskId() + " is sent for execution on edge ");
                                } else {
                                    if (algorithm == 1114) {
                                    
                                        if (task.getTaskMetadata().getDnnModel() == DNNModels.BODY_POSE_ESTIMATION || task.getTaskMetadata().getDnnModel() == DNNModels.MASK_DETECTION || task.getTaskMetadata().getDnnModel() == DNNModels.DISTANCE_ESTIMATION_VIP) {

                                            // add deadline buffer to deadline
                                            TaskMetadata taskMetadata = task.getTaskMetadata();
                                            
                                            taskMetadata.setDeadline(task.getTaskMetadata().getDeadline() + deadlineBufferTable.get(task.getTaskMetadata().getDnnModel()));
                                            task.setTaskMetadata(taskMetadata);

                                            logger.info(System.currentTimeMillis() + " Algo 1114: Changed deadline of task with task id " + task.getTaskMetadata().getTaskId() + " with DNN model " + task.getTaskMetadata().getDnnModel() + " to " + task.getTaskMetadata().getDeadline());

                                            if (task.getTaskMetadata().getExpectedExecutionTimeOnEdge() < (task.getTaskMetadata().getDeadline() - System.currentTimeMillis())) {
                                                executeInferencer(task);
                                                logger.info(System.currentTimeMillis() + " Task with id" + task.getTaskMetadata().getTaskId() + " is sent for execution on edge ");
                                            }
                                            else {
                                                task.setExecutionFlag(Deployment.EXPIRED_EDGE);
                                                task.getTaskLogger().setPostProcessingPutTime(System.currentTimeMillis());
                                                postProcessingQueue.put(task);
                                            }
                                        }
                                    }
                                    else {
                                        task.setExecutionFlag(Deployment.EXPIRED_EDGE);
                                        task.getTaskLogger().setPostProcessingPutTime(System.currentTimeMillis());
                                        postProcessingQueue.put(task);
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    private void executeInferencer(Task task) {
        TaskMetadata metadata = task.getTaskMetadata();
        task.getTaskLogger().setBeforeExecutionTime(System.currentTimeMillis());

        JobDetails jobDetails = JobDetails.newBuilder()
                .setTaskId(metadata.getTaskId())
                .setBatchId(metadata.getBatchId())
                .setFilePath(metadata.getFilePath())
                .setDnnModel(metadata.getDnnModel().toString())
                .setIsCloudExec(false)
                .build();
        try {
            Ack ack = stub.submit(jobDetails);
           task.getTaskLogger().setAfterExecutionTime(System.currentTimeMillis());
           task.getTaskLogger().setPostProcessingPutTime(System.currentTimeMillis());
            task.setInferenceOutput(ack.getResult());
            postProcessingQueue.put(task);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
