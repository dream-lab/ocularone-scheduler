package org.dreamlab.Interfaces;

import org.dreamlab.Classes.DNNModels;
import org.dreamlab.Classes.Task;
import org.javatuples.Triplet;

import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

public interface IAlgorithms {
     Task scheduleTasks(Task task, LinkedBlockingQueue taskQueue, AtomicLong sumOfExecutionTimesOnEdge, AtomicLong sumOfExecutionTimesOnCloud,
                        HashMap<DNNModels, Triplet<Integer, Double, Double>> utilityLookupTable);
}
