package org.dreamlab.TaskScheduling;

import com.google.common.io.ByteStreams;
import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.dreamlab.Classes.Deployment;
import org.dreamlab.Classes.Task;
import org.dreamlab.Classes.TaskMetadata;
import org.dreamlab.Interfaces.IExecutor;
import org.dreamlab.gRPCHandler.InferencerGrpc;
import org.dreamlab.gRPCHandler.JobDetails;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class TaskExecutorCloud implements Runnable, IExecutor {

    static final Logger logger = Logger.getLogger(TaskExecutorCloud.class);
    static final String path = "./src/main/resources/log4j.properties";

    private final InferencerGrpc.InferencerBlockingStub stub;
    private final InferencerGrpc.InferencerStub asyncStub;

    LinkedBlockingQueue<Task> postProcessingQueue;
    ConcurrentHashMap<String, Task> callbackMap = new ConcurrentHashMap<>();

    PriorityBlockingQueue<Task> cloudQueue;
    int algorithm;

    public TaskExecutorCloud(String host, int port, PriorityBlockingQueue<Task> cloudQueue, LinkedBlockingQueue<Task> postProcessingQueue, int algorithm) {
        this(ManagedChannelBuilder.forAddress(host, port).usePlaintext());
        this.cloudQueue = cloudQueue;
        this.postProcessingQueue = postProcessingQueue;
        this.algorithm = algorithm;
    }

    private TaskExecutorCloud(ManagedChannelBuilder<?> builder) {
        ManagedChannel channel = builder.build();
        stub = InferencerGrpc.newBlockingStub(channel);
        asyncStub = InferencerGrpc.newStub(channel);
    }

    @Override
    public void run() {
        // read from cloud queue
        PropertyConfigurator.configure(path);
        while (true) {
            try {
                if ( algorithm == 2 || algorithm == 4 || algorithm == 6 || algorithm == 8 || algorithm == 10 || algorithm == 22 || algorithm == 31 || algorithm == 35 || algorithm == 91 || (algorithm >= 51 && algorithm <= 56 ) || (algorithm >= 61 && algorithm <= 66 ) || (algorithm >= 71 && algorithm <= 76) || (algorithm >= 81 && algorithm <= 86)) {
                    Task task = cloudQueue.poll(10, TimeUnit.SECONDS);
                    if (task != null) {
                        if (task.isDummy()) {
                            logger.info(System.currentTimeMillis() + " Dummy task is sent for execution on cloud with id " + task.getTaskMetadata().getTaskId());
                            executeInferencer(task);
                        } else {
                            task.getTaskLogger().setCloudQueueRetrieveTime(System.currentTimeMillis());
                            logger.info(System.currentTimeMillis() + " Task in cloud executor " + task.getTaskMetadata().getTaskId());

                            if (task.getTaskMetadata().getExpectedExecutionTimeOnCloud() < (task.getTaskMetadata().getDeadline() - System.currentTimeMillis())) {
                                task.getTaskLogger().setBeforeExecutionTime(System.currentTimeMillis());
                                callbackMap.put(task.getTaskMetadata().getTaskId(), task);
                                logger.info(System.currentTimeMillis() + " Task with id" + task.getTaskMetadata().getTaskId() + " for execution on cloud with available time = " + (task.getTaskMetadata().getDeadline() - System.currentTimeMillis()));
                                executeInferencer(task);
                            } else {
                                task.setExecutionFlag(Deployment.EXPIRED_CLOUD);
                                task.getTaskLogger().setPostProcessingPutTime(System.currentTimeMillis());
                                postProcessingQueue.put(task);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void executeInferencer(Task task){
        TaskMetadata metadata = task.getTaskMetadata();
        Path batchPath = Paths.get(metadata.getFilePath());
        try {
            // read batch file into input param
            InputStream inputStream = Files.newInputStream(batchPath);
            byte[] bytes = ByteStreams.toByteArray(inputStream);

            // call async cloud grpc
            JobDetails jobDetails = JobDetails.newBuilder().setFrame(ByteString.copyFrom(bytes))
                    .setIsCloudExec(true)
                    .setTaskId(metadata.getTaskId())
                    .setDnnModel(String.valueOf(metadata.getDnnModel()))
                    .setBatchId(metadata.getBatchId())
                    .build();

            asyncStub.submit(jobDetails, new CloudTaskExecCallback(postProcessingQueue,callbackMap));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
