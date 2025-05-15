package org.dreamlab.TaskScheduling;

import io.grpc.stub.StreamObserver;
import org.dreamlab.gRPCHandler.Ack;

import java.util.concurrent.atomic.AtomicInteger;

public class testCloudExecCallback implements StreamObserver<Ack> {
    AtomicInteger c;
    public testCloudExecCallback(AtomicInteger c) {
        this.c = c;
    }

    @Override
    public void onNext(Ack ack) {
        System.out.println("Task id: " + ack.getTaskId()+ " Received-Timestamp:" + System.currentTimeMillis());
        c.decrementAndGet();
    }

    @Override
    public void onError(Throwable throwable) {
        throwable.printStackTrace();

    }

    @Override
    public void onCompleted() {

    }
}
