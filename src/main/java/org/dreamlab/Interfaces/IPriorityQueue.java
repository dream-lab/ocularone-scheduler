package org.dreamlab.Interfaces;

import org.dreamlab.Classes.DNNModels;
import org.dreamlab.Classes.PeekAndPollResponse;
import org.dreamlab.Classes.ScannedTask;
import org.dreamlab.Classes.Task;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

public interface IPriorityQueue {
    // insert the task in the queue depending on the priority value, low being the front and high is rear
    // if inserted successfully, return True else False
    List<Task> add(Task task, boolean checkTaskDeadline, boolean removeDeadlineViolations, boolean checkDeadlineViolationFromTail, AtomicLong executionEndTimeOnEdge);

    // for tpds2020 baseline
    List<Task> addAvgCompletionTime(Task task, boolean checkTaskDeadline, boolean removeDeadlineViolations, boolean checkDeadlineViolationFromTail, AtomicLong executionEndTimeOnEdge, AtomicLong totalCompletionTimeOnEdge, AtomicLong totalTasksCompletedOnEdge, int algorithm);

    // remove the task from the front of the queue to be sent to the executor
    Task poll();
    // removes and returns a specific task from the queue
    boolean remove(Task task);
    // returns the head of the queue
    Task peek();
    PeekAndPollResponse peekAndPoll();
    // returns the size of the queue
    int size();
    //returns a task or null based on if there is a task that has execution time less than slack
    PriorityBlockingQueue<ScannedTask> scanOnProfitAndReturnPriorityQueue(long slack);

    // returns true if the scan in the edge queue doesn't lead to a violation after task stealing
    boolean scanForDeadlineViolation(long executionTimeOfStolenTask, String taskId);
//    void printContents();

    // returns a list of tasks for the specific DNN models to be sent to cloud
    List<Task> gatekeeperV0Scan(ArrayList<DNNModels> dnnModelsArrayList);
}
