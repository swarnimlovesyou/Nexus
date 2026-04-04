package com.nexus.domain;

import java.time.LocalDateTime;

public class OutcomeMemory extends BaseEntity implements Auditable {
    private Integer userId;
    private Integer modelId;
    private TaskType taskType;
    private double cost;
    private int latencyMs;
    private double qualityScore;

    public OutcomeMemory() {
        super();
    }

    public OutcomeMemory(Integer id, Integer userId, Integer modelId, TaskType taskType, double cost, int latencyMs, double qualityScore, LocalDateTime createdAt) {
        super(id, createdAt);
        this.userId = userId;
        this.modelId = modelId;
        this.taskType = taskType;
        this.cost = cost;
        this.latencyMs = latencyMs;
        this.qualityScore = qualityScore;
    }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public Integer getModelId() { return modelId; }
    public void setModelId(Integer modelId) { this.modelId = modelId; }

    public TaskType getTaskType() { return taskType; }
    public void setTaskType(TaskType taskType) { this.taskType = taskType; }

    public double getCost() { return cost; }
    public void setCost(double cost) { this.cost = cost; }

    public int getLatencyMs() { return latencyMs; }
    public void setLatencyMs(int latencyMs) { this.latencyMs = latencyMs; }

    public double getQualityScore() { return qualityScore; }
    public void setQualityScore(double qualityScore) { this.qualityScore = qualityScore; }

    @Override
    public String getEntityDisplayName() {
        return "Trace: " + taskType + " execute " + modelId;
    }

    @Override
    public String getAuditSummary() {
        return "Execution Log: User " + userId + " invoked model " + modelId + " for " + taskType;
    }

    @Override
    public LocalDateTime getTimestamp() {
        return getCreatedAt();
    }

    @Override
    public String toString() {
        return "OutcomeMemory{" +
               "id=" + getId() +
               ", userId=" + userId +
               ", modelId=" + modelId +
               ", taskType=" + taskType +
               ", qualityScore=" + qualityScore +
               '}';
    }
}
