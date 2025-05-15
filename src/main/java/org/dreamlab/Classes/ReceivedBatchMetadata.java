package org.dreamlab.Classes;

public class ReceivedBatchMetadata {
    private  String droneId;
    private String batchId;
    private long startTime;
    private long endTime;
    private int batchSize;
    private int batchDuration;
    private String filePath;
    private long receivedTime;

    private boolean isDummyData;


    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public String getBatchId() {
        return batchId;
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }

    public String getDroneId() {
        return droneId;
    }

    public String getFilePath() {
        return filePath;
    }

    public long getReceivedTime() {return receivedTime;}

    public void setDroneId(String droneId) {
        this.droneId = droneId;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public int getBatchDuration() {
        return batchDuration;
    }

    public void setBatchDuration(int batchDuration) {
        this.batchDuration = batchDuration;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public void setReceivedTime(Long receivedTime) {this.receivedTime = receivedTime;}

    public boolean isDummyData() {
        return isDummyData;
    }

    public void setDummyData(boolean dummyData) {
        isDummyData = dummyData;
    }
}
