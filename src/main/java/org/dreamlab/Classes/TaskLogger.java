package org.dreamlab.Classes;

public class TaskLogger {

    private long batchReceivedTime;
    private long taskQueuePutTime;
    private long taskQueueRetrieveTime;
    private long edgeQueuePutTime;
    private long edgeQueueRetrieveTime;
    private long tentativeCloudQueuePutTime;
    private long tentativeCloudQueueRetrieveTime;
    private long cloudQueuePutTime;
    private long cloudQueueRetrieveTime;
    private long beforeExecutionTime;
    private long afterExecutionTime;
    private long postProcessingPutTime;
    private long postProcessingRetrieveTime;

    public TaskLogger() {
    }

    public void setBatchReceivedTime(long batchReceivedTime) {
        this.batchReceivedTime = batchReceivedTime;
    }

    public void setTaskQueuePutTime(long taskQueuePutTime) {
        this.taskQueuePutTime = taskQueuePutTime;
    }

    public void setTaskQueueRetrieveTime(long taskQueueRetrieveTime) {
        this.taskQueueRetrieveTime = taskQueueRetrieveTime;
    }

    public void setEdgeQueuePutTime(long edgeQueuePutTime) {
        this.edgeQueuePutTime = edgeQueuePutTime;
    }

    public void setEdgeQueueRetrieveTime(long edgeQueueRetrieveTime) {
        this.edgeQueueRetrieveTime = edgeQueueRetrieveTime;
    }

    public void setTentativeCloudQueuePutTime(long tentativeCloudQueuePutTime){this.tentativeCloudQueuePutTime = tentativeCloudQueuePutTime;}

    public long getTentativeCloudQueuePutTime() {
        return tentativeCloudQueuePutTime;
    }

    public void setTentativeCloudQueueRetrieveTime(long tentativeCloudQueueRetrieveTime){this.tentativeCloudQueueRetrieveTime = tentativeCloudQueueRetrieveTime;}

    public long getTentativeCloudQueueRetrieveTime() {
        return tentativeCloudQueueRetrieveTime;
    }

    public void setCloudQueuePutTime(long cloudQueuePutTime) {
        this.cloudQueuePutTime = cloudQueuePutTime;
    }

    public void setCloudQueueRetrieveTime(long cloudQueueRetrieveTime) {
        this.cloudQueueRetrieveTime = cloudQueueRetrieveTime;
    }

    public void setBeforeExecutionTime(long beforeExecutionTime) {
        this.beforeExecutionTime = beforeExecutionTime;
    }

    public void setAfterExecutionTime(long afterExecutionTime) {
        this.afterExecutionTime = afterExecutionTime;
    }

    public void setPostProcessingPutTime(long postProcessingPutTime) {
        this.postProcessingPutTime = postProcessingPutTime;
    }

    public void setPostProcessingRetrieveTime(long postProcessingRetrieveTime) {
        this.postProcessingRetrieveTime = postProcessingRetrieveTime;
    }


    public long getBatchReceivedTime() {
        return batchReceivedTime;
    }


    public long getTaskQueuePutTime() {
        return taskQueuePutTime;
    }

    public long getTaskQueueRetrieveTime() {
        return taskQueueRetrieveTime;
    }

    public long getEdgeQueuePutTime() {
        return edgeQueuePutTime;
    }

    public long getEdgeQueueRetrieveTime() {
        return edgeQueueRetrieveTime;
    }

    public long getCloudQueuePutTime() {
        return cloudQueuePutTime;
    }

    public long getCloudQueueRetrieveTime() {
        return cloudQueueRetrieveTime;
    }

    public long getBeforeExecutionTime() {
        return beforeExecutionTime;
    }

    public long getAfterExecutionTime() {
        return afterExecutionTime;
    }

    public long getPostProcessingPutTime() {
        return postProcessingPutTime;
    }

    public long getPostProcessingRetrieveTime() {
        return postProcessingRetrieveTime;
    }

    public String toString(){
        return ", batchReceivedTime: " + batchReceivedTime + ", taskQueuePutTime: " + taskQueuePutTime + ", taskQueueRetrieveTime: " + taskQueueRetrieveTime + ", edgeQueuePutTime: " + edgeQueuePutTime +
                ", edgeQueueRetrieveTime: " + edgeQueueRetrieveTime + ", cloudQueuePutTime: " + cloudQueuePutTime + ", cloudQueueRetrieveTime: " + cloudQueueRetrieveTime + ", tentativeCloudQueuePutTime: " + tentativeCloudQueuePutTime
                + ", tentativeCloudQueueRetrieveTime: " + tentativeCloudQueueRetrieveTime + ", beforeExecutionTime: "+ beforeExecutionTime +
                ", afterExecutionTime: " + afterExecutionTime + ", postProcessingPutTime: " + postProcessingPutTime + ", postProcessingRetrieveTime: " + postProcessingRetrieveTime;

    }
}
