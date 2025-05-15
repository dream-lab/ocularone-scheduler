package org.dreamlab.Classes;

public class DNNModelsWithDeadline {

    private DNNModels dnnModel;
    private long deadline;

    public DNNModelsWithDeadline(DNNModels dnnModel, long deadline){
        this.dnnModel = dnnModel;
        this.deadline = deadline;
    }

    public DNNModels getDnnModel() {
        return dnnModel;
    }

    public long getDeadline() {
        return deadline;
    }
}
