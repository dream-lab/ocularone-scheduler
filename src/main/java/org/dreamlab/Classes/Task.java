package org.dreamlab.Classes;

public class Task {    
    private long taskPriority;      // TODO:YS: Implement comparator interface. Use priority field.
    private TaskMetadata taskMetadata;
    private Deployment executionFlag;
    private TaskLogger taskLogger;
    private boolean dummy;
    private String inferenceOutput;

    public long getTaskPriority() {
        return taskPriority;
    }

    public boolean isDummy(){
        return dummy;
    }

    public TaskMetadata getTaskMetadata() {
        return taskMetadata;
    }

    public Deployment getExecutionFlag() {return executionFlag;}

    public TaskLogger getTaskLogger() {return taskLogger;}

    public void setTaskPriority(long taskPriority) {
        this.taskPriority = taskPriority;
    }

    public void setTaskMetadata(TaskMetadata taskMetadata) {
        this.taskMetadata = taskMetadata;   // TODO:YS: set priority to be equal to the deadline.
    }

    public void setInferenceOutput(String inferenceOutput) {
        this.inferenceOutput = inferenceOutput;
    }

    public void setDummy(boolean dummy){
        this.dummy = dummy;
    }

    public void setExecutionFlag(Deployment executionFlag){
        this.executionFlag = executionFlag;
    }

    public void setTaskLogger(TaskLogger taskLogger){
        this.taskLogger = taskLogger;
    }

    public String getInferenceOutput() {
        return inferenceOutput;
    }
}
