package org.dreamlab.TaskScheduling;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.dreamlab.Classes.*;
import org.dreamlab.Interfaces.IExecutor;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class TaskExecutorLambda implements Runnable, IExecutor {
    static final Logger logger = Logger.getLogger(TaskExecutorLambda.class);
    static final String path = "./src/main/resources/log4j.properties";
    LinkedBlockingQueue<Task> postProcessingQueue;
    ConcurrentHashMap<String, Task> callbackMap;
    HashMap<DNNModels, String> DNNModelFunctionURLMap;
    PriorityBlockingQueue<Task> cloudQueue;
    int algorithm;
    AtomicLong concurrentExecutionsOnCloud;

    public TaskExecutorLambda(HashMap<DNNModels, String> DNNModelFunctionURLMap, PriorityBlockingQueue<Task> cloudQueue,
                              LinkedBlockingQueue<Task> postProcessingQueue, int algorithm, ConcurrentHashMap<String, Task> callbackMap, AtomicLong concurrentExecutionsOnCloud) {
        this.cloudQueue = cloudQueue;
        this.postProcessingQueue = postProcessingQueue;
        this.algorithm = algorithm;
        this.DNNModelFunctionURLMap = DNNModelFunctionURLMap;
        this.callbackMap = callbackMap;
        this.concurrentExecutionsOnCloud = concurrentExecutionsOnCloud;
    }

    @Override
    public void run() {
        PropertyConfigurator.configure(path);
        while (true) {
            try {

                if (algorithm == 2 || algorithm == 4 || algorithm == 6 || algorithm == 8 || algorithm == 10 || algorithm == 22 || algorithm == 31 || algorithm == 35 || algorithm == 91 || (algorithm >= 51 && algorithm <= 56) || (algorithm >= 61 && algorithm <= 66) || (algorithm >= 71 && algorithm <= 76) || (algorithm >= 81 && algorithm <= 86) || algorithm == 1111 || algorithm == 1112 || algorithm == 1113) {
                    Task task = cloudQueue.poll(10, TimeUnit.SECONDS);
                    if (task != null) {
                        if (task.isDummy()) {
//                            logger.info(System.currentTimeMillis() + " Size of the cloud queue is: " + cloudQueue.size());
                            logger.info(System.currentTimeMillis() + " Dummy task is sent for execution on cloud with id " + task.getTaskMetadata().getTaskId());
                            executeInferencer(task);
                        } else {
                            logger.info(System.currentTimeMillis() + " Size of the cloud queue is: " + cloudQueue.size());
                            task.getTaskLogger().setCloudQueueRetrieveTime(System.currentTimeMillis());
                            logger.info(System.currentTimeMillis() + " Task in lambda executor " + task.getTaskMetadata().getTaskId());

                            if (task.getTaskMetadata().getExpectedExecutionTimeOnCloud() < (task.getTaskMetadata().getDeadline() - System.currentTimeMillis())) {
                                logger.info(System.currentTimeMillis() + " Send for execution " + task.getTaskMetadata().getTaskId());
                                task.getTaskLogger().setBeforeExecutionTime(System.currentTimeMillis());
                                concurrentExecutionsOnCloud.getAndAdd(1L);
                                logger.info(System.currentTimeMillis() + " Number of concurrent executions on cloud are " + concurrentExecutionsOnCloud.get());
                                executeInferencer(task);
                                concurrentExecutionsOnCloud.getAndAdd(-1L);
                                logger.info(System.currentTimeMillis() + " Number of concurrent executions on cloud are " + concurrentExecutionsOnCloud.get());

                                // for testing without using cloud
                            //     task.getTaskLogger().setAfterExecutionTime(task.getTaskLogger().getBeforeExecutionTime() + task.getTaskMetadata().getExpectedExecutionTimeOnCloud());
                            //     task.setInferenceOutput("output");
                            //     postProcessingQueue.put(task);
                            //     task.getTaskLogger().setPostProcessingPutTime(System.currentTimeMillis());
                            // } else {
                            //     task.setExecutionFlag(Deployment.EXPIRED_CLOUD);
                            //     task.getTaskLogger().setPostProcessingPutTime(System.currentTimeMillis());
                            //     postProcessingQueue.put(task);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                logger.error(System.currentTimeMillis() + e.getMessage(),e);
            }
        }
    }

    private void executeInferencer(Task task) {
        int timeout = 10;
        TaskMetadata metadata = task.getTaskMetadata();
        Path batchPath = Paths.get(metadata.getFilePath());
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpPost post = new HttpPost(DNNModelFunctionURLMap.get(metadata.getDnnModel()));
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(timeout * 1000)
                .setConnectionRequestTimeout(timeout * 1000)
                .setSocketTimeout(timeout * 1000).build();
        logger.info(System.currentTimeMillis() + "Task with task id " + task.getTaskMetadata().getTaskId() + " and dnn model " + task.getTaskMetadata().getDnnModel() + " is just before try block in lamba executor.");
        try {
            logger.info(System.currentTimeMillis() + " Got task "+ metadata.getTaskId() + " in lambda execute Inferencer");
            InputStream inputStream = Files.newInputStream(batchPath);
            byte[] encodedBytes = Base64.getEncoder().encode(inputStream.readAllBytes());
            String frameInString = new String(encodedBytes, "UTF-8");
            CloudTaskPayload payload =  new CloudTaskPayload();
            payload.setTaskId(metadata.getTaskId());
            payload.setDnnModel(metadata.getDnnModel());
            payload.setFrame(frameInString);
            payload.setCloudExec(true);
            StringEntity reqStringEntity = new StringEntity(payload.payloadBuild(), ContentType.APPLICATION_JSON);
            post.setEntity(reqStringEntity);
            post.setConfig(config);

            logger.info(System.currentTimeMillis() + " Task with id "+ metadata.getTaskId() + " right before execute with post " + reqStringEntity);
            CloseableHttpResponse httpResponse = httpClient.execute(post);
	    // Thread.sleep(task.getTaskMetadata().getExpectedExecutionTimeOnCloud());
		    
            task.getTaskLogger().setAfterExecutionTime(System.currentTimeMillis());
            logger.info(System.currentTimeMillis() + " Task with id "+ metadata.getTaskId() + " right after execute with http response " + httpResponse + " with content " + httpResponse.getEntity().getContent());
            String response = new BufferedReader(new InputStreamReader(httpResponse.getEntity().getContent())).readLine();
            httpResponse.close();

	    // String response = "{\"output\": \"[]\"}";
	    logger.info("Response received:  " + response);

            String output;
	    if (task.isDummy()) {
		    output = response;
	    }
	    else {
		DNNModels model_check = task.getTaskMetadata().getDnnModel();
		if (model_check.equals(DNNModels.BODY_POSE_ESTIMATION)) {
			if(response.contains("[]"))
                		output = "[]";
			else
				output = response.substring(response.indexOf('['), response.indexOf('}') + 2);
		}
		else {
	    		String[] response_elements = response.split("\"");
		
	    		int inf_output_index = -1;
	    
	    		for (int i=0; i< response_elements.length; i++) {
            			String element = response_elements[i];
            			if (element.equals("output")) {
                			inf_output_index = i+2;
					break;
				}
			}
	    		output = response_elements[inf_output_index];
		}
	    }
            task.setInferenceOutput(output);
            postProcessingQueue.put(task);
            task.getTaskLogger().setPostProcessingPutTime(System.currentTimeMillis());
            logger.info(System.currentTimeMillis() + " Got response from " + metadata.getDnnModel() + " Task id "+ metadata.getTaskId());
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            logger.error(e.getMessage(),e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}
