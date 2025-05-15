package org.dreamlab.Classes;

public class PeekAndPollResponse {
    private Task peekedAndPolledTask = null;
    private boolean availableTimeNegative = false;
//    private boolean sleepRequired = false;

    public void setPeekedAndPolledTask(Task peekedAndPolledTask) {
        this.peekedAndPolledTask = peekedAndPolledTask;
    }

    public void setAvailableTimeNegative(boolean availableTimeNegative) {
        this.availableTimeNegative = availableTimeNegative;
    }

//    public void setSleepRequired(boolean sleepRequired) {
//        this.sleepRequired = sleepRequired;
//    }

    public Task getPeekedAndPolledTask(){
        return peekedAndPolledTask;
    }

    public boolean isAvailableTimeNegative() {
        return availableTimeNegative;
    }

//    public boolean isSleepRequired(){
//        return sleepRequired;
//    }
}
