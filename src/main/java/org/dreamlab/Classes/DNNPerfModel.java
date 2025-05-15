package org.dreamlab.Classes;

import org.javatuples.Pair;

import java.util.HashMap;

public class DNNPerfModel {
    private HashMap<Integer, Pair<Long, Long>> map;

    public DNNPerfModel(HashMap<Integer, Pair<Long, Long>> entry){
        this.map = entry;
    }

    public long getEdgeTime(int frameCount){
        return map.get(frameCount).getValue0();
    }

    public long getCloudTime(int frameCount){
        return map.get(frameCount).getValue1();
    }


}
