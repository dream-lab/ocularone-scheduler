package org.dreamlab.gRPCHandler;


import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.dreamlab.ApplicationDetails.TaskCreation;
import org.dreamlab.Classes.ReceivedBatchMetadata;

import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

public class IncomingBatchHandler extends JavaConnGrpc.JavaConnImplBase implements Runnable {

    static final Logger logger = Logger.getLogger(IncomingBatchHandler.class);
    static final String path = "./src/main/resources/log4j.properties";

    private final int port;
    private LinkedBlockingQueue<ReceivedBatchMetadata> batchMetaDataQueue;

    public IncomingBatchHandler(int port, LinkedBlockingQueue<ReceivedBatchMetadata> batchMetadataQueue){
        batchMetaDataQueue = batchMetadataQueue;
        this.port = port;
    }
    @Override
    public void run() {

        PropertyConfigurator.configure(path);
//        ExecutorService executorService = Executors.newFixedThreadPool(5);
        Server server = ServerBuilder.forPort(port).addService(this).build();
        try {
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Starting Server at " + server.getPort());
        try {
            server.awaitTermination();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void newBatch(BatchMetadata request, StreamObserver<Ack> responseObserver) {
        logger.info("Received new batch " + request.getDroneId());
        responseObserver.onNext(Ack.newBuilder().setMessage("Got the batch..").build());
        responseObserver.onCompleted();

        long start = System.currentTimeMillis();
        ReceivedBatchMetadata metadata = new ReceivedBatchMetadata();
        metadata.setReceivedTime(System.currentTimeMillis());
        metadata.setBatchId(request.getBatchId());
        metadata.setDroneId(request.getDroneId());
        metadata.setStartTime(request.getStartTime());
        metadata.setEndTime(request.getEndTime());
        metadata.setBatchSize(request.getBatchSize());
        metadata.setBatchDuration(request.getBatchDuration());
        metadata.setFilePath(request.getFilePath());
        metadata.setDummyData(request.getIsDummyData());
        batchMetaDataQueue.add(metadata);
        long end = System.currentTimeMillis();
        logger.info(System.currentTimeMillis() + " Time taken (in ms) to put data in metadata queue: " + (end - start));
    }
}