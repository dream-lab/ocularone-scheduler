package org.dreamlab.TaskScheduling;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.dreamlab.Classes.Deployment;
import org.dreamlab.Classes.PeekAndPollResponse;
import org.dreamlab.Classes.Task;
import org.dreamlab.Interfaces.IPriorityQueue;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

public class TentativeCloudQueueWorker implements Runnable {

    static final Logger logger = Logger.getLogger(TentativeCloudQueueWorker.class);
    static final String path = "./src/main/resources/log4j.properties";

    IPriorityQueue tentativeCloudQueue;
    PriorityBlockingQueue<Task> cloudQueue;
    IPriorityQueue edgeQueue;
    LinkedBlockingQueue<Task> postProcessingQueue;
    AtomicLong monotonicCounterForCloud;

    static final long sleepTimeForTentativeCloud = 10L;

    public TentativeCloudQueueWorker(IPriorityQueue edgeQueue, IPriorityQueue tentativeCloudQueue, PriorityBlockingQueue<Task> cloudQueue, LinkedBlockingQueue<Task> postProcessingQueue, AtomicLong monotonicCounterForCloud) {
        this.edgeQueue = edgeQueue;
        this.cloudQueue = cloudQueue;
        this.postProcessingQueue = postProcessingQueue;
        this.tentativeCloudQueue = tentativeCloudQueue;
        this.monotonicCounterForCloud = monotonicCounterForCloud;
    }

    public void run() {

        logger.info("Starting thread for work stealing... ");
        PropertyConfigurator.configure(path);

        while (true) {
            try {
                // long startTime = System.currentTimeMillis();
                PeekAndPollResponse peekAndPollResponse = tentativeCloudQueue.peekAndPoll();
                // long peekAndPollTime = System.currentTimeMillis() - startTime;
                // logger.info(System.currentTimeMillis() + " " + "Peek and Poll Time is:" + " " + peekAndPollTime);

                if (peekAndPollResponse == null) {
                    // logger.info(System.currentTimeMillis() + " " + "Before sleep tenCloud");
                    Thread.sleep(sleepTimeForTentativeCloud);
                    // logger.info(System.currentTimeMillis() + " " + "After sleep tenCloud");
                    continue;
                }

                // startTime = System.currentTimeMillis();
                if (peekAndPollResponse.isAvailableTimeNegative()) {
                    logger.info(System.currentTimeMillis() +  " The available time negative is " + peekAndPollResponse.isAvailableTimeNegative());
                    Task polledTaskFromTentativeCloudQueue = peekAndPollResponse.getPeekedAndPolledTask();    // remove task
                    polledTaskFromTentativeCloudQueue.getTaskLogger().setTentativeCloudQueueRetrieveTime(System.currentTimeMillis());
                    if (polledTaskFromTentativeCloudQueue.getExecutionFlag().equals(Deployment.CLOUD)) {
                        polledTaskFromTentativeCloudQueue.setExecutionFlag(Deployment.DROP_TIME);
                    } else if (polledTaskFromTentativeCloudQueue.getExecutionFlag().equals(Deployment.RESCHEDULED_CLOUD)) {
                        polledTaskFromTentativeCloudQueue.setExecutionFlag(Deployment.RESCHEDULED_DROP_TIME);
                    }else if (polledTaskFromTentativeCloudQueue.getExecutionFlag().equals(Deployment.GATEKEEPER_RESCHEDULED)) {
                        polledTaskFromTentativeCloudQueue.setExecutionFlag(Deployment.GATEKEEPER_RESCHEDULED_DROP_TIME);
                    }
                    polledTaskFromTentativeCloudQueue.getTaskLogger().setPostProcessingPutTime(System.currentTimeMillis());
                    postProcessingQueue.put(polledTaskFromTentativeCloudQueue);
                }
                else{
                    Task polledTaskFromTentativeCloudQueue = peekAndPollResponse.getPeekedAndPolledTask();    // remove task
                    long availableTime = polledTaskFromTentativeCloudQueue.getTaskMetadata().getTriggerTimeForCloud() - System.currentTimeMillis();
                    logger.info(System.currentTimeMillis() + "Remaining time to be put from tentative cloud queue to cloud queue for task with id " + polledTaskFromTentativeCloudQueue.getTaskMetadata().getTaskId() + " is "
                            + availableTime + " and expected execution time is " + polledTaskFromTentativeCloudQueue.getTaskMetadata().getExpectedExecutionTimeOnCloud());
                    polledTaskFromTentativeCloudQueue.getTaskLogger().setTentativeCloudQueueRetrieveTime(System.currentTimeMillis());
                    // if they can be scheduled on the cloud without negative utility, then push to cloud, else drop it.
                    if (polledTaskFromTentativeCloudQueue.getExecutionFlag().equals(Deployment.RESCHEDULED_CLOUD) ||
                            polledTaskFromTentativeCloudQueue.getExecutionFlag().equals(Deployment.CLOUD) || polledTaskFromTentativeCloudQueue.getExecutionFlag().equals(Deployment.GATEKEEPER_RESCHEDULED)) {
                        polledTaskFromTentativeCloudQueue.setTaskPriority(monotonicCounterForCloud.get());
                        monotonicCounterForCloud.getAndAdd(1L);
                        polledTaskFromTentativeCloudQueue.getTaskLogger().setCloudQueuePutTime(System.currentTimeMillis());
                        cloudQueue.put(polledTaskFromTentativeCloudQueue);
                    } else {
                        polledTaskFromTentativeCloudQueue.getTaskLogger().setPostProcessingPutTime(System.currentTimeMillis());
                        postProcessingQueue.put(polledTaskFromTentativeCloudQueue);
                    }
                }
            // long tenCloudLogicTime = System.currentTimeMillis() - startTime;
            // logger.info(System.currentTimeMillis() + " " + "Tentative Cloud Queue Logic Time is:" + " " + tenCloudLogicTime);
                
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }


    }
}
