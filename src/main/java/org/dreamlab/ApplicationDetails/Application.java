package org.dreamlab.ApplicationDetails;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.dreamlab.Classes.PriorityQueue;
import org.dreamlab.Classes.*;
import org.dreamlab.Interfaces.IPriorityQueue;
import org.dreamlab.TaskScheduling.*;
import org.dreamlab.gRPCHandler.IncomingBatchHandler;
import org.dreamlab.gRPCHandler.PostInferencerGrpc;
import org.javatuples.Pair;
import org.javatuples.Triplet;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;

public class Application {

    static final Logger logger = Logger.getLogger(Application.class);
    static final String path = "./src/main/resources/log4j.properties";
    static final String path_config = "./src/main/resources/config.properties";

    static HashMap<UserMode, List<DNNModelsWithDeadline>> modelsLookupTable = new HashMap<>();
    static HashMap<DNNModels, DNNPerfModel> expectedExecutionTime = new HashMap<>();
    // format is benefit: based on criticality, cost on cloud: based on execution time, cost on edge
    public static HashMap<DNNModels, Triplet<Integer, Double, Double>> utilityLookupTable = new HashMap<>();

    // for d3 + kalmia variant baseline
    public static HashMap<DNNModels, Long> deadlineBufferTable = new HashMap<>();

    static HashMap <DNNModels,String> DNNModelFunctionURLMap = new HashMap<>();
    static AtomicLong monotonicCounterForCloud = new AtomicLong(0L);
    static AtomicLong executionEndTimeOnEdge = new AtomicLong(System.currentTimeMillis());
    static AtomicLong concurrentExecutionsOnCloud = new AtomicLong(0L);
    static HashMap<DNNModels, PostInferencerGrpc.PostInferencerStub> dnnModelsInferencerStubHashMap = new HashMap<>();

//    public static Hashtable<String,ApplicationRequest> userApplicationMap = new Hashtable<>();
    static UserMode userMode;
    static int algorithm;

    static AlgorithmType algorithmType;

    // for network dynamism
    // index 0: Hazard, index 1: Object, index 2: hand pose, index 3: body pose, index 4: mask, index 5: distance
    static int dnnCount = 6;
    static int bufferCapacity = 10;  // FIXME: load this from properties file
    static long[] averageLatency = new long[dnnCount];

    static long[] initialization = new long[]{-1L,-1L,-1L,-1L,-1L,-1L};
    static int[] boolInitialization = new int[]{0,0,0,0,0,0};
    static AtomicIntegerArray isCooling = new AtomicIntegerArray(boolInitialization);
    static AtomicLongArray startCoolingTimeInMs = new AtomicLongArray(initialization);
    static long coolingDuration = 10*1000L;

    static AtomicLong[][] circularBuffer = new AtomicLong[dnnCount][10];
    static AtomicIntegerArray bufferCount = new AtomicIntegerArray(boolInitialization);
    static long[] globalAverageLatency;
    static long THRESHOLD = 10L;  // FIXME: load this from properties file

    // for tpds2020 baseline
    static AtomicLong totalCompletionTimeOnEdge = new AtomicLong(0L);
    static AtomicLong totalTasksCompletedOnEdge = new AtomicLong(0L);

    // queues
    static LinkedBlockingQueue<ReceivedBatchMetadata> batchMetadataQueue = new LinkedBlockingQueue<>();
    static LinkedBlockingQueue<Task> postProcessingQueue = new LinkedBlockingQueue<>();
    static LinkedBlockingQueue<Task> taskQueue = new LinkedBlockingQueue<>();
    static PriorityBlockingQueue<Task> cloudQueue = new PriorityBlockingQueue<Task>(2, Comparator.comparingLong(Task::getTaskPriority));
    static IPriorityQueue tentativeCloudQueue = new PriorityQueue();

    static IPriorityQueue edgeQueue = new PriorityQueue();

    // list of tasks removed from the edge queue
    static LinkedBlockingQueue<Task> taskRemovedFromEdgeQueue = new LinkedBlockingQueue<>();

    // gatekeeper logic related variables
    static long windowDuration = 20000L;
    static boolean slidingFlag = false;
    static double[] requiredFrameFractionArray = new double[]{0.9,0.9,0.9,0.9,0.9,0.9};
    static long[] bonusUtility = new long[]{100L,120L,130L,140L,150L,160L};
    static AtomicLong[][] bufferForGateKeeper = new AtomicLong[dnnCount][2];
    static AtomicLong gateKeeperWindowStartTime = new AtomicLong(System.currentTimeMillis());
    static AtomicLong gateKeeperWindowEndTime = new AtomicLong(System.currentTimeMillis() + windowDuration);
    static AtomicBoolean resetGateKeeperBuffer = new AtomicBoolean(false);

    public static void main(String[] args) {

        PropertyConfigurator.configure(path);
        System.out.println("Starting the application... ");

        algorithm = Integer.parseInt(args[0]);
        userMode = UserMode.valueOf(args[1]);
        logger.info("User mode is: " + userMode);
        logger.info("Algorithm to run is: " + algorithm);

        if (algorithm == 1111){
            algorithmType = AlgorithmType.BASELINE1;
        }

        Object circularBufferLock = new Object();
        Object gateKeeperBufferLock = new Object();

        File configFile = new File(path_config);
        FileReader reader;
        Properties props;
        {
            try {
                reader = new FileReader(configFile);
                props = new Properties();
                props.load(reader);

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        boolean useLambda = Boolean.parseBoolean(props.getProperty("useLambda"));
        boolean useSyncEIA =  Boolean.parseBoolean(props.getProperty("useSyncEIA"));
        boolean useElasticInference = Boolean.parseBoolean(props.getProperty("useElasticInference"));
        boolean usePrivateCloud = Boolean.parseBoolean(props.getProperty("usePrivateCloud"));
        String HOST = props.getProperty("localhost");
        //Replace localhost with the IP of remote cloud server you are using
        String CLOUD_HOST = props.getProperty("localhost");
        int INCOMING_BATCH_DETAILS_PORT = Integer.parseInt(props.getProperty("INCOMING_BATCH_DETAILS_PORT"));
        int INFERENCING_PROCESS_GRPC_PORT_EDGE = Integer.parseInt(props.getProperty("INFERENCING_PROCESS_GRPC_PORT_EDGE"));
        int INFERENCING_PROCESS_GRPC_PORT_CLOUD = Integer.parseInt(props.getProperty("INFERENCING_PROCESS_GRPC_PORT_CLOUD"));
        int USER_APPLICATION_GRPC_PORT = Integer.parseInt(props.getProperty("USER_APPLICATION_GRPC_PORT"));
        int POST_PROCESS_GRPC_PORT_HV = Integer.parseInt(props.getProperty("POST_PROCESS_GRPC_PORT_HV"));
        int POST_PROCESS_GRPC_PORT_DE_VIP = Integer.parseInt(props.getProperty("POST_PROCESS_GRPC_PORT_DE_VIP"));
        int POST_PROCESS_GRPC_PORT_MD = Integer.parseInt(props.getProperty("POST_PROCESS_GRPC_PORT_MD"));
        int POST_PROCESS_GRPC_PORT_CD = Integer.parseInt(props.getProperty("POST_PROCESS_GRPC_PORT_CD"));
        int POST_PROCESS_GRPC_PORT_DE_OBJ = Integer.parseInt(props.getProperty("POST_PROCESS_GRPC_PORT_DE_OBJ"));
        int POST_PROCESS_GRPC_PORT_BP = Integer.parseInt(props.getProperty("POST_PROCESS_GRPC_PORT_BP"));
        boolean edgeOnly = false;

        if(algorithm == 1 || algorithm == 3 || algorithm == 5 || algorithm == 7 || algorithm == 9 || algorithm == 21){
            edgeOnly = true;
        }

        setActiveApplicationTable();
        setModelsServerPortForPostProcessingTable(HOST,POST_PROCESS_GRPC_PORT_HV,POST_PROCESS_GRPC_PORT_DE_VIP,POST_PROCESS_GRPC_PORT_MD,POST_PROCESS_GRPC_PORT_CD,POST_PROCESS_GRPC_PORT_BP,POST_PROCESS_GRPC_PORT_DE_OBJ);
        logger.info(System.currentTimeMillis() + " Application table has been initialised.");

        if(usePrivateCloud){
            setExpectedExecutionTimeForPrivateCloud();
            logger.info(System.currentTimeMillis() + " Expected execution time for private cloud has been initialised.");
            setUtilityTableForPrivateCloud();
            logger.info(System.currentTimeMillis() + " Utility table for private cloud has been initialised.");
        }

        if (useElasticInference || useSyncEIA){
            globalAverageLatency = new long[]{336L,1000L,216L,214L,219L,336L};
            setExpectedExecutionTimeForElasticInference();
            logger.info(System.currentTimeMillis() + " Expected execution time for elastic inference has been initialised.");
            setUtilityTableForElasticInference();
            logger.info(System.currentTimeMillis() + " Utility table for elastic inference has been initialised.");
        }

        if (useLambda){
            globalAverageLatency = new long[]{398L,429L,589L,878L,542L,832L};
            setExpectedExecutionTimeForLambda();
            logger.info(System.currentTimeMillis() + " Expected execution time for lambda has been initialised.");
            setUtilityTableForLambda();
//            setUtilityTableForLambdaBenchmarking();
            logger.info(System.currentTimeMillis() + " Utility table for lambda has been initialised.");
            setLambdaUrls();
            logger.info(System.currentTimeMillis() + " Lambda URLs have been initialised.");
        }

        Thread incomingBatchHandler = new Thread(new IncomingBatchHandler(INCOMING_BATCH_DETAILS_PORT,batchMetadataQueue));
        incomingBatchHandler.start();

        Thread taskCreation = new Thread(new TaskCreation(batchMetadataQueue, modelsLookupTable, expectedExecutionTime, taskQueue, userMode, algorithm, utilityLookupTable, globalAverageLatency));
        taskCreation.start();

        if (algorithm == 1113 || algorithm == 1114){
            setDeadlineBufferTable();
        }

        Thread scheduler = new Thread(new Scheduler(taskQueue, edgeQueue, cloudQueue, algorithm, postProcessingQueue, utilityLookupTable, taskRemovedFromEdgeQueue, monotonicCounterForCloud, tentativeCloudQueue, startCoolingTimeInMs, isCooling,coolingDuration, circularBuffer, bufferCount, expectedExecutionTime, executionEndTimeOnEdge, totalCompletionTimeOnEdge, totalTasksCompletedOnEdge, deadlineBufferTable));

        scheduler.start();

        Thread rescheduler = new Thread(new Rescheduler(cloudQueue, postProcessingQueue, utilityLookupTable, taskRemovedFromEdgeQueue, monotonicCounterForCloud, algorithm, tentativeCloudQueue, startCoolingTimeInMs, isCooling,coolingDuration, circularBuffer, bufferCount, expectedExecutionTime, executionEndTimeOnEdge));
        rescheduler.start();

        Thread taskExecutorEdge = new Thread(new TaskExecutorEdge(HOST, INFERENCING_PROCESS_GRPC_PORT_EDGE, edgeQueue, postProcessingQueue, algorithm, tentativeCloudQueue, executionEndTimeOnEdge, deadlineBufferTable));
        taskExecutorEdge.start();

        Thread taskExecutorCloud = null;
        if (!useLambda && !edgeOnly) {
            if (useSyncEIA){
                ManagedChannel channel = ManagedChannelBuilder.forAddress(CLOUD_HOST, INFERENCING_PROCESS_GRPC_PORT_CLOUD).usePlaintext().build();
                int nThreads_eia_executor = Integer.parseInt(props.getProperty("nEIACloudExecutorThreads"));
                ConcurrentHashMap<String, Task> callbackMap = new ConcurrentHashMap<>();
                logger.info(System.currentTimeMillis() + "Starting "+ nThreads_eia_executor + " eia executor threads.");
                ExecutorService lambdaCloudExecutorService = Executors.newFixedThreadPool(nThreads_eia_executor);
                for (int i = 0; i < nThreads_eia_executor; i++) {
                    Runnable eiaRunnable = new TaskExecutorEiaSync(CLOUD_HOST, INFERENCING_PROCESS_GRPC_PORT_CLOUD, cloudQueue, postProcessingQueue, algorithm, channel);
                     lambdaCloudExecutorService.execute(eiaRunnable);
                }
            }
            else {
                // check for private cloud later
                taskExecutorCloud = new Thread(new TaskExecutorCloud(CLOUD_HOST, INFERENCING_PROCESS_GRPC_PORT_CLOUD, cloudQueue, postProcessingQueue, algorithm));
                taskExecutorCloud.start();
            }
        }
        else if (useLambda && !edgeOnly){
            int nThreads_lambda_executor = Integer.parseInt(props.getProperty("nLambdaExecutorThreads"));
            ConcurrentHashMap<String, Task> callbackMap = new ConcurrentHashMap<>();
            logger.info(System.currentTimeMillis() + "Starting "+ nThreads_lambda_executor + " lambda executor threads.");
            ExecutorService lambdaCloudExecutorService = Executors.newFixedThreadPool(nThreads_lambda_executor);
            for (int i = 0; i < nThreads_lambda_executor; i++) {
                Runnable lambdaRunnable = new TaskExecutorLambda(DNNModelFunctionURLMap,cloudQueue, postProcessingQueue, algorithm, callbackMap, concurrentExecutionsOnCloud);
                lambdaCloudExecutorService.execute(lambdaRunnable);
            }
        }

        Thread postProcessing = new Thread(new PostProcessing(postProcessingQueue, utilityLookupTable, circularBufferLock, circularBuffer, dnnCount, bufferCapacity, averageLatency, globalAverageLatency, THRESHOLD, algorithm, bufferCount, startCoolingTimeInMs, isCooling, dnnModelsInferencerStubHashMap, bufferForGateKeeper, gateKeeperBufferLock, gateKeeperWindowStartTime, gateKeeperWindowEndTime, resetGateKeeperBuffer, totalCompletionTimeOnEdge, totalTasksCompletedOnEdge));
        postProcessing.start();

        if(algorithm == 35) {
            for(int i = 0; i < dnnCount; i ++ ){
                for (int j = 0; j < 2; j ++ ){
                    bufferForGateKeeper[i][j] = new AtomicLong(0L);
                }
            }
            Thread gateKeeper = new Thread(new GateKeeper(gateKeeperWindowStartTime, gateKeeperWindowEndTime, windowDuration, slidingFlag, requiredFrameFractionArray, bonusUtility, bufferForGateKeeper, dnnCount, edgeQueue, tentativeCloudQueue, gateKeeperBufferLock, resetGateKeeperBuffer, utilityLookupTable, cloudQueue, monotonicCounterForCloud));
            gateKeeper.start();
        }

        if(algorithm >= 71 && algorithm != 1111 && algorithm != 1112 && algorithm != 1113 && algorithm != 1114) {
            for(int i = 0; i < dnnCount; i ++ ){
                for (int j = 0; j < bufferCapacity; j ++ ){
                    circularBuffer[i][j] = new AtomicLong(0L);
                }
            }
            Thread triggerUpdate = new Thread(new TriggerUpdate(circularBufferLock, circularBuffer, dnnCount, bufferCapacity, averageLatency, globalAverageLatency, THRESHOLD, cloudQueue, postProcessingQueue, bufferCount));
            triggerUpdate.start();
        }

        if(algorithm == 31 || algorithm == 35 ||algorithm == 91) {
            ExecutorService tentativeCloudExecutorService = Executors.newFixedThreadPool(2);
            for (int j = 0; j < 2; j++) {
                Runnable tentativeCloudRunnable = new TentativeCloudQueueWorker(edgeQueue, tentativeCloudQueue, cloudQueue, postProcessingQueue, monotonicCounterForCloud);
                tentativeCloudExecutorService.execute(tentativeCloudRunnable);
            }
        }

        logger.info(System.currentTimeMillis() + " All threads have started.");
    }

    public static void setLambdaUrls(){
        DNNModelFunctionURLMap = new HashMap<>() {
            {
                put(DNNModels.HAZARD_VEST, "https://xyz.aws/");
                put(DNNModels.BODY_POSE_ESTIMATION, "https://xyz.aws/");
                put(DNNModels.DISTANCE_ESTIMATION_OBJECT, "https://xyz.aws/");
                put(DNNModels.CROWD_DENSITY, "https://xyz.aws/");
                put(DNNModels.MASK_DETECTION, "https://xyz.aws/");
                put(DNNModels.DISTANCE_ESTIMATION_VIP, "https://xyz.aws/");
            }
        };
    }

    public static void setActiveApplicationTable(){
        // TODO: fill the appropriate deadlines
        List<DNNModelsWithDeadline> priorityForUserModeTesting = new ArrayList<>();
        priorityForUserModeTesting.add(new DNNModelsWithDeadline(DNNModels.DISTANCE_ESTIMATION_OBJECT, 50000L));
        modelsLookupTable.put(UserMode.BENCHMARK, priorityForUserModeTesting);

        // for experiments
        List<DNNModelsWithDeadline> priorityForUserModeLight = new ArrayList<>();
        priorityForUserModeLight.add(new DNNModelsWithDeadline(DNNModels.HAZARD_VEST, 650L));
        priorityForUserModeLight.add(new DNNModelsWithDeadline(DNNModels.MASK_DETECTION, 750L));
        modelsLookupTable.put(UserMode.LIGHT, priorityForUserModeLight);

        List<DNNModelsWithDeadline> priorityForUserModeMedium = new ArrayList<>();
        priorityForUserModeMedium.add(new DNNModelsWithDeadline(DNNModels.HAZARD_VEST, 650L));
        priorityForUserModeMedium.add(new DNNModelsWithDeadline(DNNModels.MASK_DETECTION, 850L));
        priorityForUserModeMedium.add(new DNNModelsWithDeadline(DNNModels.BODY_POSE_ESTIMATION, 900L));
        priorityForUserModeMedium.add(new DNNModelsWithDeadline(DNNModels.DISTANCE_ESTIMATION_VIP, 750L));

        modelsLookupTable.put(UserMode.MEDIUM, priorityForUserModeMedium);

        List<DNNModelsWithDeadline> priorityForUserModeHeavy = new ArrayList<>();
        priorityForUserModeHeavy.add(new DNNModelsWithDeadline(DNNModels.HAZARD_VEST, 650L));
        priorityForUserModeHeavy.add(new DNNModelsWithDeadline(DNNModels.CROWD_DENSITY, 1000L));
        priorityForUserModeHeavy.add(new DNNModelsWithDeadline(DNNModels.DISTANCE_ESTIMATION_VIP, 750L));
        priorityForUserModeHeavy.add(new DNNModelsWithDeadline(DNNModels.BODY_POSE_ESTIMATION, 900L));
        priorityForUserModeHeavy.add(new DNNModelsWithDeadline(DNNModels.MASK_DETECTION, 850L));
        priorityForUserModeHeavy.add(new DNNModelsWithDeadline(DNNModels.DISTANCE_ESTIMATION_OBJECT, 950L));
        modelsLookupTable.put(UserMode.HEAVY, priorityForUserModeHeavy);
    }

    public static void setExpectedExecutionTimeForPrivateCloud(){
        HashMap<Integer, Pair<Long, Long>> expectedExecutionTimeForModel1 = new HashMap<>();
        expectedExecutionTimeForModel1.put(1, new Pair<>(217L,75L));
        expectedExecutionTime.put(DNNModels.HAZARD_VEST, new DNNPerfModel(expectedExecutionTimeForModel1));

        HashMap<Integer, Pair<Long, Long>> expectedExecutionTimeForModel2 = new HashMap<>();
        expectedExecutionTimeForModel2.put(1, new Pair<>(241L,305L));
        expectedExecutionTime.put(DNNModels.CROWD_DENSITY, new DNNPerfModel(expectedExecutionTimeForModel2));

        HashMap<Integer, Pair<Long, Long>> expectedExecutionTimeForModel3 = new HashMap<>();
        expectedExecutionTimeForModel3.put(1, new Pair<>(223L,287L));
        expectedExecutionTime.put(DNNModels.HAND_POSE_ESTIMATION, new DNNPerfModel(expectedExecutionTimeForModel3));

        HashMap<Integer, Pair<Long, Long>> expectedExecutionTimeForModel4 = new HashMap<>();
        expectedExecutionTimeForModel4.put(1, new Pair<>(219L,77L));
        expectedExecutionTime.put(DNNModels.BODY_POSE_ESTIMATION, new DNNPerfModel(expectedExecutionTimeForModel4));

        HashMap<Integer, Pair<Long, Long>> expectedExecutionTimeForModel5 = new HashMap<>();
        expectedExecutionTimeForModel5.put(1, new Pair<>(55L,119L));
        expectedExecutionTime.put(DNNModels.MASK_DETECTION, new DNNPerfModel(expectedExecutionTimeForModel5));

        HashMap<Integer, Pair<Long, Long>> expectedExecutionTimeForModel6= new HashMap<>();
        expectedExecutionTimeForModel6.put(1, new Pair<>(215L,75L));
        expectedExecutionTime.put(DNNModels.DISTANCE_ESTIMATION,  new DNNPerfModel(expectedExecutionTimeForModel6));
    }

    public static void setExpectedExecutionTimeForElasticInference(){
        HashMap<Integer, Pair<Long, Long>> expectedExecutionTimeForModel1 = new HashMap<>();
        expectedExecutionTimeForModel1.put(1, new Pair<>(217L,336L));
        expectedExecutionTime.put(DNNModels.HAZARD_VEST, new DNNPerfModel(expectedExecutionTimeForModel1));

        HashMap<Integer, Pair<Long, Long>> expectedExecutionTimeForModel2 = new HashMap<>();
        // keeping same value for crowd density as gpu as we don't have benchmark value
        expectedExecutionTimeForModel2.put(1, new Pair<>(241L,1000L));
        expectedExecutionTime.put(DNNModels.CROWD_DENSITY, new DNNPerfModel(expectedExecutionTimeForModel2));

        HashMap<Integer, Pair<Long, Long>> expectedExecutionTimeForModel3 = new HashMap<>();
        expectedExecutionTimeForModel3.put(1, new Pair<>(223L,216L));
        expectedExecutionTime.put(DNNModels.HAND_POSE_ESTIMATION, new DNNPerfModel(expectedExecutionTimeForModel3));

        HashMap<Integer, Pair<Long, Long>> expectedExecutionTimeForModel4 = new HashMap<>();
        expectedExecutionTimeForModel4.put(1, new Pair<>(219L,214L));
        expectedExecutionTime.put(DNNModels.BODY_POSE_ESTIMATION, new DNNPerfModel(expectedExecutionTimeForModel4));

        HashMap<Integer, Pair<Long, Long>> expectedExecutionTimeForModel5 = new HashMap<>();
        expectedExecutionTimeForModel5.put(1, new Pair<>(55L,219L));
        expectedExecutionTime.put(DNNModels.MASK_DETECTION, new DNNPerfModel(expectedExecutionTimeForModel5));

        HashMap<Integer, Pair<Long, Long>> expectedExecutionTimeForModel6= new HashMap<>();
        expectedExecutionTimeForModel6.put(1, new Pair<>(215L,336L));
        expectedExecutionTime.put(DNNModels.DISTANCE_ESTIMATION,  new DNNPerfModel(expectedExecutionTimeForModel6));
    }

    public static void setExpectedExecutionTimeForLambda(){
        HashMap<Integer, Pair<Long, Long>> expectedExecutionTimeForModel1 = new HashMap<>();
        expectedExecutionTimeForModel1.put(1, new Pair<>(174L,398L));
        expectedExecutionTime.put(DNNModels.HAZARD_VEST, new DNNPerfModel(expectedExecutionTimeForModel1));

        HashMap<Integer, Pair<Long, Long>> expectedExecutionTimeForModel2 = new HashMap<>();
        // keeping same value for crowd density as gpu as we don't have benchmark value
        expectedExecutionTimeForModel2.put(1, new Pair<>(563L,878L));
        expectedExecutionTime.put(DNNModels.CROWD_DENSITY, new DNNPerfModel(expectedExecutionTimeForModel2));

        HashMap<Integer, Pair<Long, Long>> expectedExecutionTimeForModel3 = new HashMap<>();
        expectedExecutionTimeForModel3.put(1, new Pair<>(739L,832L));
        expectedExecutionTime.put(DNNModels.DISTANCE_ESTIMATION_OBJECT, new DNNPerfModel(expectedExecutionTimeForModel3));

        HashMap<Integer, Pair<Long, Long>> expectedExecutionTimeForModel4 = new HashMap<>();
        expectedExecutionTimeForModel4.put(1, new Pair<>(244L,542L));
        expectedExecutionTime.put(DNNModels.BODY_POSE_ESTIMATION, new DNNPerfModel(expectedExecutionTimeForModel4));

        HashMap<Integer, Pair<Long, Long>> expectedExecutionTimeForModel5 = new HashMap<>();
        expectedExecutionTimeForModel5.put(1, new Pair<>(142L,589L));
        expectedExecutionTime.put(DNNModels.MASK_DETECTION, new DNNPerfModel(expectedExecutionTimeForModel5));

        HashMap<Integer, Pair<Long, Long>> expectedExecutionTimeForModel6= new HashMap<>();
        expectedExecutionTimeForModel6.put(1, new Pair<>(172L,429L));
        expectedExecutionTime.put(DNNModels.DISTANCE_ESTIMATION_VIP,  new DNNPerfModel(expectedExecutionTimeForModel6));
    }

    // for edge and 3080 GPU scenario
    public static void setUtilityTableForPrivateCloud(){
        // in the form beta, kappa
        utilityLookupTable.put(DNNModels.HAZARD_VEST, new Triplet<>(100,30.0,4.0));
        utilityLookupTable.put(DNNModels.BODY_POSE_ESTIMATION, new Triplet<>(75,30.0,4.0));
        utilityLookupTable.put(DNNModels.MASK_DETECTION, new Triplet<>(70,50.0,1.0));
        utilityLookupTable.put(DNNModels.HAND_POSE_ESTIMATION, new Triplet<>(65,120.0,4.0));
        utilityLookupTable.put(DNNModels.DISTANCE_ESTIMATION, new Triplet<>(85,30.0,4.0));
        utilityLookupTable.put(DNNModels.CROWD_DENSITY, new Triplet<>(95,130.0,4.5));
    }

    // for edge and AWS EI medium scenario
    public static void setUtilityTableForElasticInference(){
        // in the form beta, kappa
        utilityLookupTable.put(DNNModels.HAZARD_VEST, new Triplet<>(100,130.0,4.0));
        // keeping same value for crowd density as gpu to get negative utility, as we don't have benchmark value
        utilityLookupTable.put(DNNModels.BODY_POSE_ESTIMATION, new Triplet<>(75,54.0,4.0));
        utilityLookupTable.put(DNNModels.MASK_DETECTION, new Triplet<>(70,12.0,1.0));
        utilityLookupTable.put(DNNModels.HAND_POSE_ESTIMATION, new Triplet<>(65,59.0,4.0));
        utilityLookupTable.put(DNNModels.DISTANCE_ESTIMATION, new Triplet<>(85,130.0,4.0));
        utilityLookupTable.put(DNNModels.CROWD_DENSITY, new Triplet<>(95,130.0,4.5));
    }

    // for edge and AWS Lambda medium scenario
    public static void setUtilityTableForLambda(){
        // in the form beta, kappa
        utilityLookupTable.put(DNNModels.HAZARD_VEST, new Triplet<>(125,25.0,1.0));
        utilityLookupTable.put(DNNModels.BODY_POSE_ESTIMATION, new Triplet<>(40,43.0,2.0));
        utilityLookupTable.put(DNNModels.MASK_DETECTION, new Triplet<>(75,15.0,1.0));
        utilityLookupTable.put(DNNModels.DISTANCE_ESTIMATION_VIP, new Triplet<>(100,26.0,1.0));
        utilityLookupTable.put(DNNModels.DISTANCE_ESTIMATION_OBJECT, new Triplet<>(250,210.0,6.0));
        utilityLookupTable.put(DNNModels.CROWD_DENSITY, new Triplet<>(175,152.0,4.0));
    }

    // for benchmarking
    public static void setUtilityTableForLambdaBenchmarking(){
        // in the form beta, kappa
        utilityLookupTable.put(DNNModels.HAZARD_VEST, new Triplet<>(10000,78.0,4.0));
        utilityLookupTable.put(DNNModels.BODY_POSE_ESTIMATION, new Triplet<>(7500,99.0,4.0));
        utilityLookupTable.put(DNNModels.MASK_DETECTION, new Triplet<>(7000,11.0,1.0));
        utilityLookupTable.put(DNNModels.DISTANCE_ESTIMATION_OBJECT, new Triplet<>(6500,100.0,4.0));
        utilityLookupTable.put(DNNModels.DISTANCE_ESTIMATION_VIP, new Triplet<>(8500,75.0,4.0));
        utilityLookupTable.put(DNNModels.CROWD_DENSITY, new Triplet<>(9500,90.0,4.5));
    }

    public static double getUtilityDiff(DNNModels dnnModel){
        double utilityOnEdge = utilityLookupTable.get(dnnModel).getValue0() - utilityLookupTable.get(dnnModel).getValue2();
        double utilityOnCloud = utilityLookupTable.get(dnnModel).getValue0() - utilityLookupTable.get(dnnModel).getValue1();
        double utilityDiff = utilityOnEdge - utilityOnCloud;
        return (utilityDiff*1000/expectedExecutionTime.get(dnnModel).getEdgeTime(1));
    }

    public static boolean hasNegativeUtilityOnCloud(DNNModels dnnModel){
        double utilityOnCloud = utilityLookupTable.get(dnnModel).getValue0() - utilityLookupTable.get(dnnModel).getValue1();
        return utilityOnCloud < 0;
    }

    public static long calculateScore(Task task){
        DNNModels dnnModel = task.getTaskMetadata().getDnnModel();
        double utilityOnEdge = utilityLookupTable.get(dnnModel).getValue0() - utilityLookupTable.get(dnnModel).getValue2();
        double utilityOnCloud = utilityLookupTable.get(dnnModel).getValue0() - utilityLookupTable.get(dnnModel).getValue1();
        long score = 0L;

        if((task.getTaskMetadata().getExpectedExecutionTimeOnCloud() < (task.getTaskMetadata().getDeadline() - System.currentTimeMillis())) && (!Application.hasNegativeUtilityOnCloud(dnnModel))){
            score = (long)Math.abs(utilityOnCloud - utilityOnEdge);
        }
        else if ((task.getTaskMetadata().getExpectedExecutionTimeOnCloud() > (task.getTaskMetadata().getDeadline() - System.currentTimeMillis())) || Application.hasNegativeUtilityOnCloud(dnnModel)){
            score = (long)Math.abs(utilityOnEdge);
        }
        return score;
    }

    public static void setModelsServerPortForPostProcessingTable(String host, int hv_port, int de_vip_port, int md_port, int cd_port, int bp_port, int de_obj_port){
        PostInferencerGrpc.PostInferencerStub channel_hv = PostInferencerGrpc.newStub(ManagedChannelBuilder.forAddress(host, hv_port).usePlaintext().build());
        PostInferencerGrpc.PostInferencerStub channel_de_vip = PostInferencerGrpc.newStub(ManagedChannelBuilder.forAddress(host, de_vip_port).usePlaintext().build());
        PostInferencerGrpc.PostInferencerStub channel_md = PostInferencerGrpc.newStub(ManagedChannelBuilder.forAddress(host, md_port).usePlaintext().build());
        PostInferencerGrpc.PostInferencerStub channel_cd = PostInferencerGrpc.newStub(ManagedChannelBuilder.forAddress(host, cd_port).usePlaintext().build());
        PostInferencerGrpc.PostInferencerStub channel_bp = PostInferencerGrpc.newStub(ManagedChannelBuilder.forAddress(host, bp_port).usePlaintext().build());
        PostInferencerGrpc.PostInferencerStub channel_de_obj = PostInferencerGrpc.newStub(ManagedChannelBuilder.forAddress(host, de_obj_port).usePlaintext().build());
        dnnModelsInferencerStubHashMap.put(DNNModels.HAZARD_VEST,channel_hv);
        dnnModelsInferencerStubHashMap.put(DNNModels.DISTANCE_ESTIMATION_VIP,channel_de_vip);
        dnnModelsInferencerStubHashMap.put(DNNModels.MASK_DETECTION,channel_md);
        dnnModelsInferencerStubHashMap.put(DNNModels.CROWD_DENSITY,channel_cd);
        dnnModelsInferencerStubHashMap.put(DNNModels.BODY_POSE_ESTIMATION,channel_bp);
        dnnModelsInferencerStubHashMap.put(DNNModels.DISTANCE_ESTIMATION_OBJECT,channel_de_obj);
    }

    public static void setDeadlineBufferTable(){
        // deadlineBufferTable.put(DNNModels.HAZARD_VEST, 1000L);
        deadlineBufferTable.put(DNNModels.DISTANCE_ESTIMATION_VIP, 75L);
        deadlineBufferTable.put(DNNModels.MASK_DETECTION, 85L);
        // deadlineBufferTable.put(DNNModels.CROWD_DENSITY, 1000L);
        deadlineBufferTable.put(DNNModels.BODY_POSE_ESTIMATION, 90L);
        // deadlineBufferTable.put(DNNModels.DISTANCE_ESTIMATION_OBJECT, 1000L);
    }
}
