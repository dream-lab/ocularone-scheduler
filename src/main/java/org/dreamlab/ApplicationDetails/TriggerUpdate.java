package org.dreamlab.ApplicationDetails;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.dreamlab.Classes.Task;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicLong;

public class TriggerUpdate implements Runnable{

    static final Logger logger = Logger.getLogger(TriggerUpdate.class);
    static final String path = "./src/main/resources/log4j.properties";

    Object circularBufferLock;
    AtomicLong[][] circularBuffer;
    int dnnCount;
    int bufferCapacity;
    long[] averageLatency;
    long[] globalAverageLatency;
    long THRESHOLD;
    PriorityBlockingQueue<Task> cloudQueue;
    LinkedBlockingQueue<Task> postProcessingQueue;
    AtomicIntegerArray bufferCount;

    public TriggerUpdate(Object circularBufferLock, AtomicLong[][] circularBuffer, int dnnCount, int bufferCapacity, long[] averageLatency,
                         long[] globalAverageLatency, long THRESHOLD, PriorityBlockingQueue<Task> cloudQueue, LinkedBlockingQueue<Task> postProcessingQueue, AtomicIntegerArray bufferCount) {
        this.circularBufferLock = circularBufferLock;
        this.circularBuffer = circularBuffer;
        this.dnnCount = dnnCount;
        this.bufferCapacity = bufferCapacity;
        this.averageLatency = averageLatency;
        this.globalAverageLatency = globalAverageLatency;
        this.THRESHOLD = THRESHOLD;
        this.cloudQueue = cloudQueue;
        this.postProcessingQueue = postProcessingQueue;
        this.bufferCount = bufferCount;
    }

    public void run() {

        System.out.println("Trigger update Thread started... ");
        PropertyConfigurator.configure(path);

        while (true){
            // wait to be notified of a change in global avg latency for a DNN
            synchronized (circularBufferLock){
                try {
                    circularBufferLock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            for (int i = 0; i<dnnCount ; i++){
                logger.info("notification of network dynamism lock has been received.");
                // TODO: buffer count should be equal to buffer capacity, which has been hardcoded to 10 here.
                if((Math.abs(globalAverageLatency[i] - averageLatency[i]) > THRESHOLD) && (bufferCount.get(i) == 10)){
                    // handle DNN global change
                    logger.info(System.currentTimeMillis() + " The value of i is: " + i);
                    long diffAverageGlobalLatency = globalAverageLatency[i] - averageLatency[i];
                    logger.info("Threshold for updating latency value crossed, hence updating the global variable. ");
                    logger.info("Previous average latency values for cloud are "+ globalAverageLatency[i]);
                    logger.info("New average latency values for cloud are "+ averageLatency[i]);
                    logger.info("Difference in the average global latency is " + diffAverageGlobalLatency);
                    globalAverageLatency[i] = averageLatency[i];
                }
            }
        }
    }
}
