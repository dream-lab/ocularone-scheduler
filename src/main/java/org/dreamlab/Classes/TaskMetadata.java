package org.dreamlab.Classes;

public class TaskMetadata {

    private long deadline;
    private DNNModels dnnModel;
    private long referenceCounter;
    private long expectedExecutionTimeOnEdge;
    private long expectedExecutionTimeOnCloud;
    private long triggerTimeForCloud;
    private long triggerTimeForEdge;
    private long batchSize;
    private String taskId;
    private String batchId;
    private String filePath;
    private long batchStartTime;
    private long batchEndTime;
    private String droneId;
    private long expectedCompletionTimeOnEdge;
    private boolean stoppedCooling;
    
    public long getDeadline() {
        return deadline;
    }

    public DNNModels getDnnModel() {
        return dnnModel;
    }

    public long getReferenceCounter() {
        return referenceCounter;
    }

    public long getExpectedExecutionTimeOnEdge() {
        return expectedExecutionTimeOnEdge;
    }

    public long getExpectedExecutionTimeOnCloud() {
        return expectedExecutionTimeOnCloud;
    }

    public long getBatchSize() {
        return batchSize;
    }

    public String getTaskId() {
        return taskId;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getBatchId() {return batchId;}

    public long getBatchStartTime() {return batchStartTime;}

    public long getBatchEndTime() {return batchEndTime;}

    public String getDroneId() {return droneId;}

    public long getExpectedCompletionTimeOnEdge() {return expectedCompletionTimeOnEdge;}

    public boolean getStoppedCooling() {return stoppedCooling;}

    public void setDeadline(long deadline) {
        this.deadline = deadline;
    }

    public void setDnnModel(DNNModels dnnModel) {
        this.dnnModel = dnnModel;
    }

    public void setReferenceCounter(long referenceCounter) {
        this.referenceCounter = referenceCounter;
    }

    public void setExpectedExecutionTimeOnEdge(long expectedExecutionTimeOnEdge) {
        this.expectedExecutionTimeOnEdge = expectedExecutionTimeOnEdge;
    }

    public void setExpectedExecutionTimeOnCloud(long expectedExecutionTimeOnCloud) {
        this.expectedExecutionTimeOnCloud = expectedExecutionTimeOnCloud;
    }

    public void setBatchSize(long batchSize) {
        this.batchSize = batchSize;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public void setBatchId(String batchId) {this.batchId = batchId;}

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public void setBatchStartTime(long batchStartTime){this.batchStartTime = batchStartTime;}

    public void setBatchEndTime(long batchEndTime){this.batchEndTime = batchEndTime;}

    public void setDroneId(String droneId) {this.droneId = droneId;}

    public void setExpectedCompletionTimeOnEdge(long expectedCompletionTimeOnEdge) {this.expectedCompletionTimeOnEdge = expectedCompletionTimeOnEdge;}

    public long getTriggerTimeForCloud() {
        return triggerTimeForCloud;
    }

    public void setTriggerTimeForCloud(long triggerTimeForCloud) {
        this.triggerTimeForCloud = triggerTimeForCloud;
    }

    public long getTriggerTimeForEdge() {
        return triggerTimeForEdge;
    }

    public void setTriggerTimeForEdge(long triggerTimeForEdge) {
        this.triggerTimeForEdge = triggerTimeForEdge;
    }

    public void setStoppedCooling(boolean stoppedCooling){
        this.stoppedCooling = stoppedCooling;
    }
}
