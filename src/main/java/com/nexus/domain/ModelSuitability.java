package com.nexus.domain;

import java.time.LocalDateTime;

public class ModelSuitability extends BaseEntity {
    private Integer modelId;
    private TaskType taskType;
    private double baseScore;

    public ModelSuitability() {
        super();
    }

    public ModelSuitability(Integer id, Integer modelId, TaskType taskType, double baseScore, LocalDateTime createdAt) {
        super(id, createdAt);
        this.modelId = modelId;
        this.taskType = taskType;
        this.baseScore = baseScore;
    }

    public Integer getModelId() { return modelId; }
    public void setModelId(Integer modelId) { this.modelId = modelId; }

    public TaskType getTaskType() { return taskType; }
    public void setTaskType(TaskType taskType) { this.taskType = taskType; }

    public double getBaseScore() { return baseScore; }
    public void setBaseScore(double baseScore) { this.baseScore = baseScore; }

    @Override
    public String getEntityDisplayName() {
        return "Suitability: Model " + modelId + " for " + taskType;
    }

    @Override
    public String toString() {
        return "ModelSuitability{" +
               "id=" + getId() +
               ", modelId=" + modelId +
               ", taskType=" + taskType +
               ", baseScore=" + baseScore +
               '}';
    }
}
