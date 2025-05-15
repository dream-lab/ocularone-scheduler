package org.dreamlab.TaskScheduling;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.dreamlab.Classes.DNNModels;
import org.dreamlab.Classes.DNNPerfModel;
import org.dreamlab.Classes.Deployment;
import org.dreamlab.Classes.Task;
import org.dreamlab.Interfaces.IPriorityQueue;
import org.javatuples.Triplet;

import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;

public class Rescheduler implements Runnable {

    static final Logger logger = Logger.getLogger(Rescheduler.class);
    static final String path = "./src/main/resources/log4j.properties";

    LinkedBlockingQueue<Task> postProcessingQueue;
    HashMap<DNNModels, Triplet<Integer, Double, Double>> utilityLookupTable;

    PriorityBlockingQueue<Task> cloudQueue;
    IPriorityQueue tentativeCloudQueue;

    LinkedBlockingQueue<Task> taskRemovedFromEdgeQueue;
    AtomicLong monotonicCounterForCloud;
    int algorithm;
    AtomicLong executionEndTimeOnEdge;

    AtomicLongArray startCoolingTimeInMs;
    AtomicIntegerArray isCooling;
    long coolingDuration;
    AtomicLong[][] circularBuffer;
    AtomicIntegerArray bufferCount;
    HashMap<DNNModels, DNNPerfModel> expectedExecutionTime;

    public Rescheduler(PriorityBlockingQueue<Task> cloudQueue, LinkedBlockingQueue<Task> postProcessingQueue,
                       HashMap<DNNModels, Triplet<Integer, Double, Double>> utilityLookupTable, LinkedBlockingQueue<Task> taskRemovedFromEdgeQueue, AtomicLong monotonicCounterForCloud, int algorithm, IPriorityQueue tentativeCloudQueue,
                       AtomicLongArray startCoolingTimeInMs, AtomicIntegerArray isCooling, long coolingDuration, AtomicLong[][] circularBuffer, AtomicIntegerArray bufferCount,
                       HashMap<DNNModels, DNNPerfModel> expectedExecutionTime, AtomicLong executionEndTimeOnEdge) {
        this.cloudQueue = cloudQueue;
        this.postProcessingQueue = postProcessingQueue;
        this.utilityLookupTable = utilityLookupTable;
        this.taskRemovedFromEdgeQueue = taskRemovedFromEdgeQueue;
        this.monotonicCounterForCloud = monotonicCounterForCloud;
        this.algorithm = algorithm;
        this.tentativeCloudQueue = tentativeCloudQueue;
        this.startCoolingTimeInMs = startCoolingTimeInMs;
        this.isCooling = isCooling;
        this.coolingDuration = coolingDuration;
        this.circularBuffer = circularBuffer;
        this.bufferCount = bufferCount;
        this.expectedExecutionTime =expectedExecutionTime;
        this.executionEndTimeOnEdge = executionEndTimeOnEdge;
    }

    public void run() {

        logger.info("Starting thread for rescheduler");
        PropertyConfigurator.configure(path);

        while (true) {
            try {
                Task task = taskRemovedFromEdgeQueue.poll(10, TimeUnit.SECONDS);
                if(task != null) {
                    logger.info(System.currentTimeMillis() + " Task to be re-scheduled. " + task.getTaskMetadata().getTaskId());
                    Triplet<Integer, Double, Double> triplet = utilityLookupTable.get(task.getTaskMetadata().getDnnModel());
                    if((triplet.getValue0() - triplet.getValue1()) > 0 || algorithm == 1112 || algorithm == 1113 || algorithm == 1114) {
                        int modelNumber = 0;
                        DNNModels dnnModel = task.getTaskMetadata().getDnnModel();
                        switch (dnnModel){
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
                        if (task.getTaskMetadata().getExpectedExecutionTimeOnCloud() > (task.getTaskMetadata().getDeadline() - System.currentTimeMillis()) && algorithm != 1111 && algorithm != 1112 && algorithm != 1113 && algorithm != 1114) {
                            logger.info(System.currentTimeMillis() + " Checking if task has available time to be re-scheduled to the cloud for task with id " + task.getTaskMetadata().getTaskId());
                            if(algorithm >=71){
                                if (isCooling.get(modelNumber) == 0) {
                                    startCoolingTimeInMs.set(modelNumber, System.currentTimeMillis());
                                    isCooling.set(modelNumber, 1);
                                    task.setExecutionFlag(Deployment.RESCHEDULED_DROP_TIME);
                                    logger.info(System.currentTimeMillis() + " Cooling has been started by task with id " + task.getTaskMetadata().getTaskId());
                                } else {
                                    if (System.currentTimeMillis() - startCoolingTimeInMs.get(modelNumber) > coolingDuration) {
                                        startCoolingTimeInMs.set(modelNumber, -1L);
                                        isCooling.set(modelNumber, 0);
                                        task.getTaskMetadata().setStoppedCooling(true);
                                        task.getTaskMetadata().setExpectedExecutionTimeOnCloud(expectedExecutionTime.get(dnnModel).getCloudTime(1));
                                        task.getTaskMetadata().setTriggerTimeForCloud(task.getTaskMetadata().getDeadline() - expectedExecutionTime.get(dnnModel).getCloudTime(1) - 10L);
                                        logger.info(System.currentTimeMillis() + " Expected execution time on cloud is set to default for task with id " + task.getTaskMetadata().getTaskId() + " and value is " + task.getTaskMetadata().getExpectedExecutionTimeOnCloud());
                                        // recheck
                                        if (System.currentTimeMillis() + task.getTaskMetadata().getExpectedExecutionTimeOnCloud() <= task.getTaskMetadata().getDeadline()) {
                                            logger.info(System.currentTimeMillis() + " Rechecking of task is successful for task with id " + task.getTaskMetadata().getTaskId());
                                            task.setExecutionFlag(Deployment.RESCHEDULED_CLOUD);
                                        }else{
                                            task.setExecutionFlag(Deployment.RESCHEDULED_DROP_TIME);
                                            logger.info(System.currentTimeMillis() + " Rechecking task timing check failed. ");
                                        }
                                    } else {
                                        task.setExecutionFlag(Deployment.RESCHEDULED_DROP_TIME);
                                        logger.info(System.currentTimeMillis() + " Cooling period going on...  ");
                                    }
                                }
                            }
                            else{
                                task.setExecutionFlag(Deployment.RESCHEDULED_DROP_TIME);
                                logger.info(System.currentTimeMillis() + "Dropping due to time constraints " + task.getTaskMetadata().getTaskId());
                            }
                        }
                        else{
                            task.setExecutionFlag(Deployment.RESCHEDULED_CLOUD);
                            logger.info(System.currentTimeMillis() + " The task has available time to be scheduled to the cloud for task with id " + task.getTaskMetadata().getTaskId());
                        }
                    }
                    else{
                        task.setExecutionFlag(Deployment.RESCHEDULED_DROP_COST);
                        logger.info(System.currentTimeMillis() + "Incurring negative utility, hence dropping due to cost with model " + task.getTaskMetadata().getDnnModel());

                    }

                    if (task.getExecutionFlag().equals(Deployment.RESCHEDULED_CLOUD)) {
                        if(algorithm == 31 || algorithm == 91){
                            logger.info(System.currentTimeMillis() + " Task with id: " + task.getTaskMetadata().getTaskId() + " has been put in the tentative cloud queue.");
                            task.setTaskPriority(task.getTaskMetadata().getTriggerTimeForCloud());
                            tentativeCloudQueue.add(task, false, false, false, new AtomicLong(0L));
                            task.getTaskLogger().setTentativeCloudQueuePutTime(System.currentTimeMillis());
                        }
                        else {
                            // Cloud is a FIFO for all the baselines
                            task.setTaskPriority(monotonicCounterForCloud.get());
                            monotonicCounterForCloud.getAndAdd(1L);
                            task.getTaskLogger().setCloudQueuePutTime(System.currentTimeMillis());
                            cloudQueue.put(task);
                        }
                    } else if (task.getExecutionFlag().equals(Deployment.RESCHEDULED_DROP_TIME) || task.getExecutionFlag().equals(Deployment.RESCHEDULED_DROP_COST)) {
                        if((task.getExecutionFlag().equals(Deployment.RESCHEDULED_DROP_COST)) && (algorithm == 31 || algorithm == 35 || algorithm == 91) ){
                            logger.info(System.currentTimeMillis() + " Task with id: " + task.getTaskMetadata().getTaskId() + " has been put in the tentative cloud queue.");
                            task.setTaskPriority(task.getTaskMetadata().getTriggerTimeForEdge());
                            tentativeCloudQueue.add(task, false, false, false, new AtomicLong(0L));
                            task.getTaskLogger().setTentativeCloudQueuePutTime(System.currentTimeMillis());
                        }
                        else {
                            task.getTaskLogger().setPostProcessingPutTime(System.currentTimeMillis());
                            postProcessingQueue.put(task);
                        }
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
