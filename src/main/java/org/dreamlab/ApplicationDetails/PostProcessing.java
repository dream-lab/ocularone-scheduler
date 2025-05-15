package org.dreamlab.ApplicationDetails;

import io.grpc.stub.StreamObserver;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.dreamlab.Classes.DNNModels;
import org.dreamlab.Classes.Deployment;
import org.dreamlab.Classes.Task;
import org.dreamlab.gRPCHandler.Empty;
import org.dreamlab.gRPCHandler.OutputDetails;
import org.dreamlab.gRPCHandler.PostInferencerGrpc;
import org.javatuples.Triplet;

import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;

public class PostProcessing implements Runnable {

    static final Logger logger = Logger.getLogger(PostProcessing.class);
    static final String path = "./src/main/resources/log4j.properties";

    private final HashMap<DNNModels, PostInferencerGrpc.PostInferencerStub> dnnModelsInferencerStubHashMap;

    LinkedBlockingQueue<Task> postProcessingQueue;
    HashMap<DNNModels, Triplet<Integer, Double, Double>> utilityLookupTable;
    Object circularBufferLock;
    // index 0: Hazard, index 1: Object, index 2: hand pose, index 3: body pose, index 4: mask, index 5: distance
//    long[][] circularBuffer;
    AtomicLong[][] circularBuffer;
    int dnnCount = 6;
    int bufferCapacity;
    long THRESHOLD;

//    int[] bufferIndex = new int[]{0,0,0,0,0,0};
    int[] bufferIndex = new int[]{-1, -1, -1, -1, -1, -1};
    long[] bufferSum = new long[dnnCount];
//    int[] bufferCount = new int[dnnCount];
    AtomicIntegerArray bufferCount;
    long[] averageLatency;
    long[] globalAverageLatency;

    int processedTasksOnEdge = 0;
    int processedTaskOnCloud = 0;
    int droppedTasksForTime = 0;
    int droppedTasksForCost = 0;
    int expiredTasksFromEdgeQueue = 0;
    int expiredTasksFromCloudQueue = 0;
    int missedDeadlineTasksOnEdge = 0;
    int missedDeadlineTasksOnCloud = 0;

    double totalUtility = 0L;

    int algorithm;

    // AlgorithmType algorithmType;

    AtomicLongArray startCoolingTimeInMs;
    AtomicIntegerArray isCooling;

    AtomicLong[][] bufferForGateKeeper;
    Object gateKeeperBufferLock;
    AtomicLong gateKeeperWindowStartTime;
    AtomicLong gateKeeperWindowEndTime;
    AtomicBoolean resetGateKeeperBuffer;

    // for tpds2020 baseline
    AtomicLong totalCompletionTimeOnEdge;
    AtomicLong totalTasksCompletedOnEdge;

    public PostProcessing(LinkedBlockingQueue<Task> postProcessingQueue, HashMap<DNNModels, Triplet<Integer, Double, Double>> utilityLookupTable,
                          Object circularBufferLock, AtomicLong[][] circularBuffer, int dnnCount, int bufferCapacity, long[] averageLatency,
                          long[] globalAverageLatency, long THRESHOLD, int algorithm, AtomicIntegerArray bufferCount, AtomicLongArray startCoolingTimeInMs,
                          AtomicIntegerArray isCooling, HashMap<DNNModels, PostInferencerGrpc.PostInferencerStub> dnnModelsInferencerStubHashMap, AtomicLong[][] bufferForGateKeeper,
                          Object gateKeeperBufferLock, AtomicLong gateKeeperWindowStartTime, AtomicLong gateKeeperWindowEndTime, AtomicBoolean resetGateKeeperBuffer, AtomicLong totalCompletionTimeOnEdge, AtomicLong totalTasksCompletedOnEdge) {

        this.postProcessingQueue = postProcessingQueue;
        this.utilityLookupTable = utilityLookupTable;
        this.circularBufferLock = circularBufferLock;
        this.circularBuffer = circularBuffer;
        this.dnnCount = dnnCount;
        this.bufferCapacity = bufferCapacity;
        this.averageLatency = averageLatency;
        this.globalAverageLatency = globalAverageLatency;
        this.THRESHOLD = THRESHOLD;
        this.algorithm = algorithm;
        // this.algorithmType = algorithmType;
        this.bufferCount = bufferCount;
        this.startCoolingTimeInMs = startCoolingTimeInMs;
        this.isCooling = isCooling;
        this.dnnModelsInferencerStubHashMap = dnnModelsInferencerStubHashMap;
        this.bufferForGateKeeper = bufferForGateKeeper;
        this.gateKeeperBufferLock = gateKeeperBufferLock;
        this.gateKeeperWindowStartTime = gateKeeperWindowStartTime;
        this.gateKeeperWindowEndTime = gateKeeperWindowEndTime;
        this.resetGateKeeperBuffer = resetGateKeeperBuffer;

        // for tpds2020 baseline
        this.totalCompletionTimeOnEdge = totalCompletionTimeOnEdge;
        this.totalTasksCompletedOnEdge = totalTasksCompletedOnEdge;
    }

    public void run() {

        System.out.println("Post Processing Thread started... ");
        PropertyConfigurator.configure(path);

        while (true) {
            try {
                Task processedTask = postProcessingQueue.poll(10, TimeUnit.SECONDS);
                if (processedTask != null) {
                    if (processedTask.isDummy()) {
                        System.out.println(" Dummy task with id " + processedTask.getTaskMetadata().getTaskId() + " has been processed.");
                    } else {
                        double costOnEdge = 0;
                        double costOnCloud = 0;
                        int benefit = 0;
                        double utility = 0;
                        int modelNumber = 0;

                        processedTask.getTaskLogger().setPostProcessingRetrieveTime(System.currentTimeMillis());
                        DNNModels dnnModelUsedForProcessing = processedTask.getTaskMetadata().getDnnModel();
                        Deployment executionState = processedTask.getExecutionFlag();
                        Long actualExecutionTime = processedTask.getTaskLogger().getAfterExecutionTime() - processedTask.getTaskLogger().getBeforeExecutionTime();
                        Triplet<Integer, Double, Double> triplet = utilityLookupTable.get(processedTask.getTaskMetadata().getDnnModel());


                        if ((executionState.equals(Deployment.EDGE)) || (executionState.equals(Deployment.WORK_STEAL_FROM_CLOUD))) {
                            if (processedTask.getTaskLogger().getAfterExecutionTime() <= processedTask.getTaskMetadata().getDeadline()) {
                                benefit = triplet.getValue0();
                                processedTasksOnEdge++;
                                OutputDetails outputDetails = OutputDetails.newBuilder()
                                        .setDnnModel(processedTask.getTaskMetadata().getDnnModel().toString())
                                        .setTaskId(processedTask.getTaskMetadata().getTaskId())
                                        .setResult(processedTask.getInferenceOutput())
                                        .build();
                                System.out.println(" Result: " + outputDetails.getResult() + "at Task Id: " + outputDetails.getTaskId());
                                System.out.println(" Inference Output: " + processedTask.getInferenceOutput() + "at Task Id: " + processedTask.getTaskMetadata().getTaskId());
                                // call async grpc for post processing
                                try {
                                    dnnModelsInferencerStubHashMap.get(dnnModelUsedForProcessing).submit(outputDetails, new StreamObserver<>() {
                                        @Override
                                        public void onNext(Empty empty) {
                                        }

                                        @Override
                                        public void onError(Throwable throwable) {
                                        }

                                        @Override
                                        public void onCompleted() {
                                        }
                                    });
                                }
                                catch (Exception e){
                                    e.printStackTrace();
                                }

                            if (algorithm == 1111 || algorithm == 1112) {
                                totalCompletionTimeOnEdge.set(totalCompletionTimeOnEdge.get() + actualExecutionTime);
                                totalTasksCompletedOnEdge.set(processedTasksOnEdge);
                            }

                            } else {
                                processedTask.setExecutionFlag(Deployment.MISSED_EDGE);
                                executionState = Deployment.MISSED_EDGE;
                                missedDeadlineTasksOnEdge++;
                            }
                            costOnEdge = triplet.getValue2();
                        } else if (executionState.equals(Deployment.CLOUD) || executionState.equals(Deployment.RESCHEDULED_CLOUD) || executionState.equals(Deployment.GATEKEEPER_RESCHEDULED)) {
                            if (processedTask.getTaskLogger().getAfterExecutionTime() <= processedTask.getTaskMetadata().getDeadline()) {
                                benefit = triplet.getValue0();
                                processedTaskOnCloud++;
                                OutputDetails outputDetails = OutputDetails.newBuilder()
                                        .setDnnModel(processedTask.getTaskMetadata().getDnnModel().toString())
                                        .setTaskId(processedTask.getTaskMetadata().getTaskId())
                                        .setResult(processedTask.getInferenceOutput())
                                        .build();
                                System.out.println(" Result: " + outputDetails.getResult() + "at Task Id: " + outputDetails.getTaskId());
                                System.out.println(" Inference Output: " + processedTask.getInferenceOutput() + "at Task Id: " + processedTask.getTaskMetadata().getTaskId());
                                try {
                                    dnnModelsInferencerStubHashMap.get(dnnModelUsedForProcessing).submit(outputDetails, new StreamObserver<>() {
                                        @Override
                                        public void onNext(Empty empty) {
                                        }

                                        @Override
                                        public void onError(Throwable throwable) {
                                        }

                                        @Override
                                        public void onCompleted() {
                                        }
                                    });
                                }
                                catch (Exception e){
                                    e.printStackTrace();
                                }
                            } else {
                                processedTask.setExecutionFlag(Deployment.MISSED_CLOUD);
                                executionState = Deployment.MISSED_CLOUD;
                                missedDeadlineTasksOnCloud++;
                            }
                            costOnCloud = triplet.getValue1();
                        } else if (executionState.equals(Deployment.DROP_TIME) || executionState.equals(Deployment.RESCHEDULED_DROP_TIME) || executionState.equals(Deployment.GATEKEEPER_RESCHEDULED_DROP_TIME)) {
                            droppedTasksForTime++;
                        } else if (executionState.equals(Deployment.DROP_COST) || executionState.equals(Deployment.RESCHEDULED_DROP_COST)) {
                            droppedTasksForCost++;
                        } else if (executionState.equals(Deployment.EXPIRED_EDGE)) {
                            expiredTasksFromEdgeQueue++;
                        } else if (executionState.equals(Deployment.EXPIRED_CLOUD)) {
                            expiredTasksFromCloudQueue++;
                        }

                        utility = benefit - costOnCloud - costOnEdge;
                        totalUtility += utility;

                        // gatekeeper array insertion
                        if (algorithm == 35){
                            DNNModels dnnModel = processedTask.getTaskMetadata().getDnnModel();
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
                            synchronized (gateKeeperBufferLock) {
                                logger.info(System.currentTimeMillis() + " Current value of reset gatekeeper buffer is set to " + resetGateKeeperBuffer.get());
                                if(!(resetGateKeeperBuffer.get()))
                                {
                                    logger.info(System.currentTimeMillis() + "Task with id " + processedTask.getTaskMetadata().getTaskId() + " with DNN Model " + processedTask.getTaskMetadata().getDnnModel() + " has execution flag " + processedTask.getExecutionFlag());
                                    if (executionState.equals(Deployment.EDGE) || executionState.equals(Deployment.WORK_STEAL_FROM_CLOUD)
                                            || executionState.equals(Deployment.CLOUD) || executionState.equals(Deployment.RESCHEDULED_CLOUD) || executionState.equals(Deployment.GATEKEEPER_RESCHEDULED)) {
                                        bufferForGateKeeper[modelNumber][0].getAndAdd(1L);
                                    }
                                    bufferForGateKeeper[modelNumber][1].getAndAdd(1L);
                                }
                                else{
                                    for (int i = 0; i < dnnCount ; i++) {
                                        bufferForGateKeeper[i][0].set(0L);
                                        bufferForGateKeeper[i][1].set(0L);
                                    }
                                    logger.info("gatekeeper buffer has been re-initialized.");
                                    resetGateKeeperBuffer.set(false);
                                }
                                gateKeeperBufferLock.notify();
                                logger.info(System.currentTimeMillis() + "lock for gatekeeper has been notified.");
                            }
                        }

                        logger.info(System.currentTimeMillis() + " " + ", Task id: " + processedTask.getTaskMetadata().getTaskId() + ", drone id: " + processedTask.getTaskMetadata().getDroneId() +
                                ", execution Device: " + processedTask.getExecutionFlag() +
                                processedTask.getTaskLogger().toString() + ", DNN Model: " + processedTask.getTaskMetadata().getDnnModel() + ", deadline: " + processedTask.getTaskMetadata().getDeadline() + ", trigger time on cloud: " + processedTask.getTaskMetadata().getTriggerTimeForCloud());

                        logger.info(System.currentTimeMillis() + " Edge processed tasks count is " + processedTasksOnEdge);
                        logger.info(System.currentTimeMillis() + " Cloud processed tasks count is " + processedTaskOnCloud);
                        logger.info(System.currentTimeMillis() + " Dropped tasks due to time count is " + droppedTasksForTime);
                        logger.info(System.currentTimeMillis() + " Dropped tasks due to cost count is " + droppedTasksForCost);

                        logger.info(System.currentTimeMillis() + " Edge processed tasks count but missed deadline is " + missedDeadlineTasksOnEdge);
                        logger.info(System.currentTimeMillis() + " Cloud processed tasks count but missed deadline is " + missedDeadlineTasksOnCloud);
                        logger.info(System.currentTimeMillis() + " Expired tasks on edge queue count is " + expiredTasksFromEdgeQueue);
                        logger.info(System.currentTimeMillis() + " Expired tasks on cloud queue count is " + expiredTasksFromCloudQueue);

                        logger.info(System.currentTimeMillis() + " Size of the post processing queue is " + postProcessingQueue.size());
                        logger.info(System.currentTimeMillis() + " Utility is: " + utility);
                        logger.info(System.currentTimeMillis() + " Total utility is: " + totalUtility);

                        logger.info(System.currentTimeMillis() + " Global average latency is: " + globalAverageLatency[0] + ", " + globalAverageLatency[1] + ", " + globalAverageLatency[2] + ", " + globalAverageLatency[3] + ", " + globalAverageLatency[4] + ", " + globalAverageLatency[5]);
                        logger.info(System.currentTimeMillis() + " Local average latency is: " + averageLatency[0] + ", " + averageLatency[1] + ", " + averageLatency[2] + ", " + averageLatency[3] + ", " + averageLatency[4] + ", " + averageLatency[5]);

                        if (algorithm >= 71 && (executionState.equals(Deployment.CLOUD) || executionState.equals(Deployment.RESCHEDULED_CLOUD) || executionState.equals(Deployment.MISSED_CLOUD)) && algorithm != 1111 & algorithm != 1112 && algorithm != 1113 && algorithm != 1114) {
                            DNNModels dnnModel = processedTask.getTaskMetadata().getDnnModel();
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
                            logger.info("Value of model number is " + modelNumber);
                            logger.info("Value of dnn count is " + dnnCount);

                            if ((executionState.equals(Deployment.CLOUD) || executionState.equals(Deployment.RESCHEDULED_CLOUD)) && (isCooling.get(modelNumber) == 1)) {
                                startCoolingTimeInMs.set(modelNumber, -1L);
                                isCooling.set(modelNumber, 0);
                                logger.info(System.currentTimeMillis() + " The task has been successfully processed so stopping cooling " + processedTask.getTaskMetadata().getTaskId());
                            } else if ((executionState.equals(Deployment.CLOUD) || executionState.equals(Deployment.RESCHEDULED_CLOUD)) && (isCooling.get(modelNumber) == 0) && (processedTask.getTaskMetadata().getStoppedCooling())) {
                                // reset circular buffer
                                bufferSum[modelNumber] = 0;
                                for (int j = 0; j < 10; j++) {
                                    circularBuffer[modelNumber][j] = new AtomicLong(actualExecutionTime);
                                    bufferSum[modelNumber] += actualExecutionTime;
                                }
                                logger.info(System.currentTimeMillis() + " Buffer has been reset by task with id " + processedTask.getTaskMetadata().getTaskId() + " with value " + actualExecutionTime);
                                logger.info(System.currentTimeMillis() + " Current buffer for model " + modelNumber + " is " + circularBuffer[modelNumber][0] + " , " + circularBuffer[modelNumber][1] + " , " + circularBuffer[modelNumber][2] + " , "
                                        + circularBuffer[modelNumber][3] + " , " + circularBuffer[modelNumber][4] + " , " + circularBuffer[modelNumber][5] + circularBuffer[modelNumber][6] + " , " + circularBuffer[modelNumber][7] + " , "
                                        + circularBuffer[modelNumber][8] + " , " + circularBuffer[modelNumber][9]);
                            }

                            synchronized (circularBufferLock) {
                                // increment sliding window sum for modelNumber with latest exec time and decrement oldest exec time
                                logger.info("entered synchronised region.... ");

                                logger.info("before addition, buffer sum for model number " + modelNumber + " is " + bufferSum[modelNumber]);
                                logger.info("before addition, buffer index for model number " + modelNumber + " is " + bufferIndex[modelNumber]);
                                // edits
                                if (bufferCount.get(modelNumber) < bufferCapacity) {
                                    // increment index for buffer
                                    bufferIndex[modelNumber] = (bufferIndex[modelNumber] + 1) % bufferCapacity;
                                    bufferSum[modelNumber] = bufferSum[modelNumber] + actualExecutionTime;
                                    // replace oldest exec time for modelNumber with latest exec time
                                    circularBuffer[modelNumber][bufferIndex[modelNumber]] = new AtomicLong(actualExecutionTime);
                                    // update the count till the count reaches the maximum capacity
                                    bufferCount.getAndAdd(modelNumber, 1);
                                    logger.info("after addition buffer sum for model number " + modelNumber + " is " + bufferSum[modelNumber]);
                                    logger.info("after addition buffer index for model number " + modelNumber + " is " + bufferIndex[modelNumber]);
                                    logger.info("Current buffer count for model number " + modelNumber + " is " + bufferCount.get(modelNumber));

                                    // at the edge case when buffer capacity is filled up
                                    if(bufferCount.get(modelNumber) == bufferCapacity){
                                        logger.info("Current buffer count for model number " + modelNumber + " is " + bufferCount.get(modelNumber) + " hence going for averaging.");
                                        // update local avg exec time for model
                                        averageLatency[modelNumber] = (bufferSum[modelNumber] / bufferCount.get(modelNumber));
                                        logger.info(System.currentTimeMillis() + " Calculated current average latency for model number " + modelNumber + " is " + averageLatency[modelNumber]);
                                        // has local avg exceeded global avg for this model by threshold?
                                        if (Math.abs(globalAverageLatency[modelNumber] - averageLatency[modelNumber]) > THRESHOLD) {
                                            circularBufferLock.notify();
                                            logger.info("lock for network dynamism has been notified.");
                                        }
                                    }
                                }
                                else if (bufferCount.get(modelNumber) == bufferCapacity){
                                    logger.info("Current buffer count for model number " + modelNumber + " is " + bufferCount.get(modelNumber));
                                    // increment index for buffer
                                    bufferIndex[modelNumber] = (bufferIndex[modelNumber] + 1) % bufferCapacity;
                                    bufferSum[modelNumber] = bufferSum[modelNumber] - circularBuffer[modelNumber][bufferIndex[modelNumber]].get() + actualExecutionTime;
                                    // replace oldest exec time for modelNumber with latest exec time
                                    circularBuffer[modelNumber][bufferIndex[modelNumber]] = new AtomicLong(actualExecutionTime);
                                    // update local avg exec time for model
                                    averageLatency[modelNumber] = (bufferSum[modelNumber] / bufferCount.get(modelNumber));
                                    logger.info(System.currentTimeMillis() + " Calculated current average latency for model number " + modelNumber + " is " + averageLatency[modelNumber]);
                                    // has local avg exceeded global avg for this model by threshold?
                                    if (Math.abs(globalAverageLatency[modelNumber] - averageLatency[modelNumber]) > THRESHOLD) {
                                        circularBufferLock.notify();
                                        logger.info("lock for network dynamism has been notified.");
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
}
