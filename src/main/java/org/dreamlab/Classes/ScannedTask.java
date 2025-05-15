package org.dreamlab.Classes;

public class ScannedTask {

    private long scannedTaskPriority;
    private Task scannedTask;

    public ScannedTask(Task scannedTask, long scannedTaskPriority){
        this.scannedTask = scannedTask;
        this.scannedTaskPriority = scannedTaskPriority;
    }

    public long getScannedTaskPriority(){
        return scannedTaskPriority;
    }

    public Task getScannedTask(){
        return scannedTask;
    }
}
