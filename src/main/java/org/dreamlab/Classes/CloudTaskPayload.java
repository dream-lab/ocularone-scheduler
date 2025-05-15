package org.dreamlab.Classes;

import org.json.JSONObject;

public class CloudTaskPayload {
    private boolean isCloudExec = true;

    private String Frame;

    public String getFrame() {
        return Frame;
    }

    public void setFrame(String frame) {
        Frame = frame;
    }

    private String TaskId;

    public String getTaskId() {
        return TaskId;
    }
    private DNNModels dnnModel;

    public DNNModels getDnnModel() {
        return dnnModel;
    }

    public void setDnnModel(DNNModels dnnModel) {
        this.dnnModel = dnnModel;
    }

    private String BatchId;

    public String getBatchId() {
        return BatchId;
    }

    public void setBatchId(String batchId) {
        BatchId = batchId;
    }

    public String payloadBuild(){
        return new JSONObject(){
            {
                put("frame", getFrame());
                put("task_id", getTaskId());
                put("dnn_model", getDnnModel().toString());
                put("is_cloud_exec", isCloudExec());
            }
        }.toString();
    }

    public boolean isCloudExec() {
        return isCloudExec;
    }

    public void setCloudExec(boolean cloudExec) {
        isCloudExec = cloudExec;
    }

    public void setTaskId(String taskId) {
        TaskId = taskId;
    }
}
