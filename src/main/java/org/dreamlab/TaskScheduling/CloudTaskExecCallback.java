package org.dreamlab.TaskScheduling;

import io.grpc.stub.StreamObserver;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.dreamlab.Classes.Task;
import org.dreamlab.gRPCHandler.Ack;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

public class CloudTaskExecCallback implements StreamObserver<Ack> {

    static final Logger logger = Logger.getLogger(CloudTaskExecCallback.class);
    static final String path = "./src/main/resources/log4j.properties";

    LinkedBlockingQueue<Task> postProcessingQueue;
    ConcurrentHashMap<String, Task> callbackMap;

    public CloudTaskExecCallback(LinkedBlockingQueue<Task> postProcessingQueue, ConcurrentHashMap<String, Task> callbackMap) {
        PropertyConfigurator.configure(path);
        this.postProcessingQueue = postProcessingQueue;
        this.callbackMap = callbackMap;
    }

    @Override
    public void onNext(Ack ack) {

        System.out.println(ack);
        String task_id = ack.getTaskId();
        //get task details from from hash
        try {
            Task task = callbackMap.get(task_id);
            task.getTaskLogger().setAfterExecutionTime(System.currentTimeMillis());
            task.getTaskLogger().setPostProcessingPutTime(System.currentTimeMillis());

            postProcessingQueue.put(task);
            
            logger.info(System.currentTimeMillis() + " Task received from async server " + task_id);
            callbackMap.remove(task_id);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // put into post processing queue
    }

    @Override
    public void onError(Throwable throwable) {
        System.out.println("Error "+throwable);
    }

    @Override
    public void onCompleted() {
        System.out.println("Result returned .. ");
    }
}
