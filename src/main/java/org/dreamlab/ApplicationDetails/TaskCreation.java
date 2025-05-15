package org.dreamlab.ApplicationDetails;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.dreamlab.Classes.*;
import org.javatuples.Triplet;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class TaskCreation implements Runnable {

    static final Logger logger = Logger.getLogger(TaskCreation.class);
    static final String path = "./src/main/resources/log4j.properties";

     LinkedBlockingQueue<ReceivedBatchMetadata> batchMetadataQueue;
     HashMap<UserMode, List<DNNModelsWithDeadline>> modelsLookupTable;
     HashMap<DNNModels, Triplet<Integer, Double, Double>> utilityLookupTable;
     HashMap<DNNModels, DNNPerfModel> expectedExecutionTime;
     LinkedBlockingQueue<Task> taskQueue;
     UserMode userMode;

    long[] globalAverageLatency;

     int size = 0;
     int frameCount = 0;
     int algorithm;
     int monotonicCounter = 0;
     long edgeBuffer = 10L;
     long cloudBuffer = 10L;

    public TaskCreation(LinkedBlockingQueue<ReceivedBatchMetadata> batchMetadataQueue, HashMap<UserMode, List<DNNModelsWithDeadline>> modelsLookupTable,
                        HashMap<DNNModels, DNNPerfModel> expectedExecutionTime, LinkedBlockingQueue taskQueue, UserMode userMode, int algorithm,
                        HashMap<DNNModels, Triplet<Integer, Double, Double>> utilityLookupTable, long[] globalAverageLatency){
        this.batchMetadataQueue = batchMetadataQueue;
        this.modelsLookupTable = modelsLookupTable;
        this.expectedExecutionTime = expectedExecutionTime;
        this.taskQueue = taskQueue;
        this.userMode = userMode;
        this.algorithm = algorithm;
        this.utilityLookupTable = utilityLookupTable;
        this.globalAverageLatency = globalAverageLatency;
    }


    public void run() {
        PropertyConfigurator.configure(path);

        System.out.println("Inside task creation.... ");

        List<DNNModelsWithDeadline> dnnModelsWithDeadlineList = modelsLookupTable.get(userMode);
        size = dnnModelsWithDeadlineList.size();

        while(true) {
            try {
                ReceivedBatchMetadata batchMetadata = batchMetadataQueue.poll(10, TimeUnit.SECONDS);
                if(batchMetadata != null){
                    logger.info(System.currentTimeMillis() + " batch to be processed " + batchMetadata.getBatchId());

                    frameCount = (batchMetadata.getBatchSize() * batchMetadata.getBatchDuration());
                    logger.info(System.currentTimeMillis() + "frame count is " + frameCount);
                    Collections.shuffle(dnnModelsWithDeadlineList);

                    for (DNNModelsWithDeadline dnnModelsWithDeadline : dnnModelsWithDeadlineList) {

                        Task task = new Task();

                        TaskMetadata taskMetadata = new TaskMetadata();
                        TaskLogger taskLogger = new TaskLogger();

                        // deadline is in absolute wall clock time
                        taskMetadata.setDeadline(batchMetadata.getReceivedTime() + dnnModelsWithDeadline.getDeadline());
                        taskMetadata.setDnnModel(dnnModelsWithDeadline.getDnnModel());
                        taskMetadata.setReferenceCounter(size);
                        taskMetadata.setExpectedExecutionTimeOnEdge(expectedExecutionTime.get(dnnModelsWithDeadline.getDnnModel()).getEdgeTime(frameCount));
                        taskMetadata.setBatchSize(batchMetadata.getBatchSize());
                        taskMetadata.setTaskId(UUID.randomUUID().toString());
                        taskMetadata.setBatchId(batchMetadata.getBatchId());
                        taskMetadata.setFilePath(batchMetadata.getFilePath());
                        taskMetadata.setDroneId(batchMetadata.getDroneId());
                        taskMetadata.setBatchStartTime(batchMetadata.getStartTime());
                        taskMetadata.setBatchEndTime(batchMetadata.getEndTime());

                        task.setDummy(batchMetadata.isDummyData());

                        // new for network dynamism algo
                        if(algorithm >= 71 && algorithm != 1111 && algorithm != 1112){ 
                            int i = 0;
                            switch (dnnModelsWithDeadline.getDnnModel()){
                                case HAZARD_VEST:
                                    i = 0;
                                    break;
                                case DISTANCE_ESTIMATION_VIP:
                                    i = 1;
                                    break;
                                case MASK_DETECTION:
                                    i = 2;
                                    break;
                                case CROWD_DENSITY:
                                    i = 3;
                                    break;
                                case BODY_POSE_ESTIMATION:
                                    i = 4;
                                    break;
                                case DISTANCE_ESTIMATION_OBJECT:
                                    i = 5;
                                    break;
                            }
//                            taskMetadata.setTriggerTime(batchMetadata.getReceivedTime() + dnnModelsWithDeadline.getDeadline() - globalAverageLatency[i] - buffer);
                            logger.info(System.currentTimeMillis() + " Setting execution time for DNN model: " + dnnModelsWithDeadline.getDnnModel() + " is "+ globalAverageLatency[i]);
                            taskMetadata.setExpectedExecutionTimeOnCloud(globalAverageLatency[i]);
                            taskMetadata.setTriggerTimeForCloud(batchMetadata.getReceivedTime() + dnnModelsWithDeadline.getDeadline() - globalAverageLatency[i] - cloudBuffer);
                            taskMetadata.setTriggerTimeForEdge(batchMetadata.getReceivedTime() + dnnModelsWithDeadline.getDeadline() - expectedExecutionTime.get(dnnModelsWithDeadline.getDnnModel()).getEdgeTime(frameCount) - edgeBuffer);
                        }
                        else{
                            taskMetadata.setExpectedExecutionTimeOnCloud(expectedExecutionTime.get(dnnModelsWithDeadline.getDnnModel()).getCloudTime(frameCount));
                            taskMetadata.setTriggerTimeForCloud(batchMetadata.getReceivedTime() + dnnModelsWithDeadline.getDeadline() - expectedExecutionTime.get(dnnModelsWithDeadline.getDnnModel()).getCloudTime(frameCount) - cloudBuffer);
                            taskMetadata.setTriggerTimeForEdge(batchMetadata.getReceivedTime() + dnnModelsWithDeadline.getDeadline() - expectedExecutionTime.get(dnnModelsWithDeadline.getDnnModel()).getEdgeTime(frameCount) - edgeBuffer);
                        }

                        taskLogger.setBatchReceivedTime(batchMetadata.getReceivedTime());


                        task.setTaskMetadata(taskMetadata);
                        task.setTaskLogger(taskLogger);
                        // FIFO
                        if(algorithm == 1 || algorithm == 2 || algorithm == 51 || algorithm == 61 || algorithm == 71 || algorithm == 81  || algorithm == 1113 || algorithm == 1114){
                            task.setTaskPriority(monotonicCounter);
                            monotonicCounter++;
                        }
                        // EDF and deadline aware with JIT
                        else if(algorithm == 3 || algorithm == 4 || algorithm == 31 || algorithm == 35 || algorithm == 32 || algorithm == 42 || algorithm == 52 || algorithm == 62 || algorithm == 72 || algorithm == 82 || algorithm == 91 || algorithm == 1111){ 
                            task.setTaskPriority(dnnModelsWithDeadline.getDeadline());
                        }
                        // SJF
                        else if(algorithm == 5 || algorithm == 6 || algorithm == 53 || algorithm == 63 || algorithm == 73 || algorithm == 83 || algorithm == 1112){
                            task.setTaskPriority(expectedExecutionTime.get(dnnModelsWithDeadline.getDnnModel()).getEdgeTime(frameCount));
                        }
                        // HBF
                        else if(algorithm == 7 || algorithm == 8 || algorithm == 54 || algorithm == 64 || algorithm == 74 || algorithm == 84){
                            task.setTaskPriority(Integer.MAX_VALUE - utilityLookupTable.get(dnnModelsWithDeadline.getDnnModel()).getValue0());
                        }
                        // HUF
                        else if(algorithm == 9 || algorithm == 10 || algorithm == 55 || algorithm == 65 || algorithm == 75 || algorithm == 85){
                            Triplet<Integer, Double, Double> triplet = utilityLookupTable.get(task.getTaskMetadata().getDnnModel());
                            int utilityCheck = (int) (triplet.getValue0() - triplet.getValue2());
                            logger.info(System.currentTimeMillis() + " The value of utility is: " + utilityCheck);
                            task.setTaskPriority(Integer.MAX_VALUE - utilityCheck);
                        }
                        // HUF/time
                        else if(algorithm == 21 || algorithm == 22 || algorithm == 56 || algorithm == 66 || algorithm == 76 || algorithm == 86){
                            Triplet<Integer, Double, Double> triplet = utilityLookupTable.get(task.getTaskMetadata().getDnnModel());
                            double utilityCheck = (triplet.getValue0() - triplet.getValue2());
                            double utilityPerTime = (utilityCheck/(expectedExecutionTime.get(dnnModelsWithDeadline.getDnnModel()).getEdgeTime(frameCount)))*100;
                            task.setTaskPriority(Integer.MAX_VALUE - (int) utilityPerTime);
                        }
                        task.getTaskLogger().setTaskQueuePutTime(System.currentTimeMillis());
                        taskQueue.put(task);
                        logger.info(System.currentTimeMillis() + "Task with id " + taskMetadata.getTaskId() +
                                " with DNN Model " + taskMetadata.getDnnModel() + " is set priority value " + task.getTaskPriority());
                        logger.info(System.currentTimeMillis() + " Task with id " + taskMetadata.getTaskId() + " has been created and put on the queue.");    // TODO:YS: Log the task metadata as CSV
                    }
                    logger.info(System.currentTimeMillis() + " Size of the task queue is: " + taskQueue.size());
                }
            } catch (Exception e) {
                e.printStackTrace();
                logger.error("Metadata queue not ready yet!!");
            }
        }

    }
}
