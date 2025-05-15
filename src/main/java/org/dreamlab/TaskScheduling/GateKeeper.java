package org.dreamlab.TaskScheduling;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.dreamlab.Classes.DNNModels;
import org.dreamlab.Classes.Deployment;
import org.dreamlab.Classes.Task;
import org.dreamlab.Interfaces.IPriorityQueue;
import org.javatuples.Triplet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class GateKeeper implements Runnable {

    static final Logger logger = Logger.getLogger(GateKeeper.class);
    static final String path = "./src/main/resources/log4j.properties";

    AtomicLong windowStartTime;
    long windowDuration;
    AtomicLong windowEndTime;
    boolean slidingFlag;
    double[] requiredFrameFractionArray;
    long[] bonusUtility;
    AtomicLong[][] bufferForGateKeeper;
    int dnnCount;
    double[] fractionTillNow = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
    IPriorityQueue edgeQueue;
    IPriorityQueue tentativeCloudQueue;
    PriorityBlockingQueue<Task> cloudQueue;
    boolean[] dnnModelsToBeSentToCloud = new boolean[]{false, false, false, false, false, false};
    ArrayList<DNNModels> dnnModelsArrayList = new ArrayList<>();
    DNNModels[] dnnModels = new DNNModels[]{DNNModels.HAZARD_VEST, DNNModels.DISTANCE_ESTIMATION_VIP, DNNModels.MASK_DETECTION, DNNModels.CROWD_DENSITY, DNNModels.BODY_POSE_ESTIMATION, DNNModels.DISTANCE_ESTIMATION_OBJECT};
    Object gateKeeperBufferLock;
    AtomicBoolean resetGateKeeperBuffer;
    HashMap<DNNModels, Triplet<Integer, Double, Double>> utilityLookupTable;
    AtomicLong monotonicCounterForCloud;

    public GateKeeper(AtomicLong windowStartTime, AtomicLong windowEndTime, long windowDuration, boolean slidingFlag, double[] requiredFrameFractionArray, long[] bonusUtility, AtomicLong[][] bufferForGateKeeper,
                      int dnnCount, IPriorityQueue edgeQueue, IPriorityQueue tentativeCloudQueue, Object gateKeeperBufferLock, AtomicBoolean resetGateKeeperBuffer, HashMap<DNNModels, Triplet<Integer, Double, Double>> utilityLookupTable,
                      PriorityBlockingQueue<Task> cloudQueue, AtomicLong monotonicCounterForCloud) {
        this.windowStartTime = windowStartTime;
        this.windowEndTime = windowEndTime;
        this.windowDuration = windowDuration;
        this.slidingFlag = slidingFlag;
        this.requiredFrameFractionArray = requiredFrameFractionArray;
        this.bonusUtility = bonusUtility;
        this.bufferForGateKeeper = bufferForGateKeeper;
        this.dnnCount = dnnCount;
        this.edgeQueue = edgeQueue;
        this.tentativeCloudQueue = tentativeCloudQueue;
        this.gateKeeperBufferLock = gateKeeperBufferLock;
        this.resetGateKeeperBuffer = resetGateKeeperBuffer;
        this.utilityLookupTable = utilityLookupTable;
        this.cloudQueue = cloudQueue;
        this.monotonicCounterForCloud = monotonicCounterForCloud;
    }

    public void run() {
        System.out.println("Gatekeeper Thread started... ");
        PropertyConfigurator.configure(path);

        while (true) {
            synchronized (gateKeeperBufferLock) {
                try {
                    gateKeeperBufferLock.wait();
                    logger.info("lock for gatekeeper has been received 1.");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            // to ensure that we don't send all tasks to cloud in the start itself
            logger.info("lock for gatekeeper has been received 2.");
            logger.info("CurrentTime: " + System.currentTimeMillis() + "  WindowStartTime: " + windowStartTime + " WindowEndTime: " + windowEndTime.get());
            if ((System.currentTimeMillis() < windowEndTime.get()) && (System.currentTimeMillis() > (windowStartTime.get() + 1L))) {
                for (int i = 0; i < dnnCount; i++) {
                    fractionTillNow[i] = (double) bufferForGateKeeper[i][0].get() / bufferForGateKeeper[i][1].get();
                    logger.info(System.currentTimeMillis() + " Outside the if loop: Current fraction rate is " + fractionTillNow[i] + " and required fraction rate is " + requiredFrameFractionArray[i] + " for DNN Model with index " + i);
                    if (fractionTillNow[i] < requiredFrameFractionArray[i]) {
                        logger.info(System.currentTimeMillis() + " Inside the if loop: Current fraction rate is " + fractionTillNow[i] + " and required fraction rate is " + requiredFrameFractionArray[i] + " for DNN Model with index " + i);
                        dnnModelsToBeSentToCloud[i] = true;
                        dnnModelsArrayList.add(dnnModels[i]);
                    } else {
                        dnnModelsToBeSentToCloud[i] = false;
                    }
                }
                // version 0 -- send all tasks of the DNN model from edge queue to cloud queue
                if (!dnnModelsArrayList.isEmpty()) {
                    long gateKeeperScanStart = System.currentTimeMillis();
                    List<Task> taskList = edgeQueue.gatekeeperV0Scan(dnnModelsArrayList);
                    long gateKeeperScanEnd = System.currentTimeMillis();
                    logger.info(System.currentTimeMillis() + " gatekeeper scan took " + (gateKeeperScanEnd - gateKeeperScanStart) + " ms and size of the array list for gatekeeper is " + taskList.size());
                    for (Task task : taskList) {
                        // deadline violation check is taken care of by scheduler/rescheduler, if a task is in edge queue, means it is not violating any deadline in the current state
                        Triplet<Integer, Double, Double> triplet = utilityLookupTable.get(task.getTaskMetadata().getDnnModel());
                        if ((triplet.getValue0() - triplet.getValue1()) > 0) {
                            if (task.getTaskMetadata().getExpectedExecutionTimeOnCloud() < (task.getTaskMetadata().getDeadline() - System.currentTimeMillis())) {
                                edgeQueue.remove(task);
                                task.getTaskLogger().setEdgeQueueRetrieveTime(System.currentTimeMillis());
                                task.setExecutionFlag(Deployment.GATEKEEPER_RESCHEDULED);
                                task.getTaskLogger().setCloudQueuePutTime(System.currentTimeMillis());
//                                task.setTaskPriority(task.getTaskMetadata().getTriggerTimeForCloud());
//                                tentativeCloudQueue.add(task, false, false, false, new AtomicLong(0L));
                                task.setTaskPriority(monotonicCounterForCloud.get());
                                monotonicCounterForCloud.getAndAdd(1L);
                                cloudQueue.put(task);
                                logger.info(System.currentTimeMillis() + " Task with task id " + task.getTaskMetadata().getTaskId() + " has been removed from the edge queue and put in the cloud queue with DNN Model using gatekeeper logic " + task.getTaskMetadata().getDnnModel());
                            }
                        } else {
                            // TODO: put logic for V1
                        }
                    }
                    dnnModelsArrayList = new ArrayList<>();
                }
            } else {
                logger.info(System.currentTimeMillis() + " Window time for gatekeeper logic is expired, hence resetting all variables.");
                windowStartTime.set(System.currentTimeMillis());
                windowEndTime.set(System.currentTimeMillis() + windowDuration);
                dnnModelsArrayList = new ArrayList<>();
                dnnModelsToBeSentToCloud = new boolean[]{false, false, false, false, false, false};
                resetGateKeeperBuffer.set(true);
            }
        }
    }
}
