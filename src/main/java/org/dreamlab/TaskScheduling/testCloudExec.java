package org.dreamlab.TaskScheduling;

import com.google.common.io.ByteStreams;
import com.google.protobuf.ByteString;
import io.grpc.ManagedChannelBuilder;
import org.dreamlab.gRPCHandler.InferencerGrpc;
import org.dreamlab.gRPCHandler.JobDetails;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class testCloudExec {
    public static void main(String[] args) {
        final InferencerGrpc.InferencerBlockingStub stub = InferencerGrpc.newBlockingStub(ManagedChannelBuilder.forAddress("10.24.24.31", 6001).usePlaintext().build());
        final InferencerGrpc.InferencerStub asyncStub = InferencerGrpc.newStub(ManagedChannelBuilder.forAddress("10.24.24.31", 6001).usePlaintext().build());
        String model = args[0];
        final CountDownLatch finishLatch = new CountDownLatch(1);
        AtomicInteger c = new AtomicInteger(0);
        try (BufferedReader br = new BufferedReader(new FileReader("/tmp/ramdrive/1frame/metadata"))) {
            String line;
            while (br.ready()) {
//                while (c.get() != 0) ;
                // process the line.
//                for (int i = 0; i < Integer.parseInt(args[1]); i++) {
                    try {
                        if ((line = br.readLine()) != null) {
                            Path batchPath = Paths.get("/tmp/ramdrive/1frame/" + line);
                            InputStream inputStream = Files.newInputStream(batchPath);
                            byte[] bytes = ByteStreams.toByteArray(inputStream);
                            String task_id = UUID.randomUUID().toString();
                            JobDetails jobDetails = JobDetails.newBuilder().setFrame(ByteString.copyFrom(bytes))
                                    .setIsCloudExec(true).setTaskId(task_id)
                                    .setDnnModel(String.valueOf(model))
                                    .build();
                            asyncStub.submit(jobDetails, new testCloudExecCallback(c));
                            System.out.println("Task id: " + task_id + " Sent-Timestamp:" + System.currentTimeMillis());
                            c.incrementAndGet();
                            /*
                            Path batchPath = Paths.get("/tmp/ramdrive/1frame/" + line);
                            InputStream inputStream = Files.newInputStream(batchPath);
                            byte[] bytes = ByteStreams.toByteArray(inputStream);
                            String task_id = UUID.randomUUID().toString();
                            long start = System.currentTimeMillis();
                            JobDetails jobDetails = JobDetails.newBuilder().setFrame(ByteString.copyFrom(bytes))
                                    .setIsCloudExec(true).setTaskId(task_id)
                                    .setDnnModel(String.valueOf(model))
                                    .build();
                            //asyncStub.submit(jobDetails, new testCloudExecCallback(c));
                            stub.submit(jobDetails);
                            //System.out.println("Task id: " + task_id + " Sent-Timestamp:" + System.currentTimeMillis());
                            System.out.println("Timestamp" + System.currentTimeMillis() + "e2e: "+ (System.currentTimeMillis() - start));
                            //c.incrementAndGet();
                             */
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
//                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
