package com.nexus.domain;

import java.time.LocalDateTime;

/**
 * Represents one end-to-end agent work session.
 * Sessions are the primary unit for tracking practical developer outcomes.
 */
public class AgentSession extends BaseEntity implements Auditable {
    private Integer userId;
    private TaskType taskType;
    private Integer modelId;
    private String status; // ACTIVE | CLOSED
    private Integer inputTokens;
    private Integer outputTokens;
    private Double totalCost;
    private Double qualityScore;
    private String notes;
    private LocalDateTime endedAt;

    public AgentSession() {
        super();
    }

    public AgentSession(Integer id, Integer userId, TaskType taskType, Integer modelId,
                        String status, Integer inputTokens, Integer outputTokens,
                        Double totalCost, Double qualityScore, String notes,
                        LocalDateTime endedAt, LocalDateTime createdAt) {
        super(id, createdAt);
        this.userId = userId;
        this.taskType = taskType;
        this.modelId = modelId;
        this.status = status;
        this.inputTokens = inputTokens;
        this.outputTokens = outputTokens;
        this.totalCost = totalCost;
        this.qualityScore = qualityScore;
        this.notes = notes;
        this.endedAt = endedAt;
    }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public TaskType getTaskType() { return taskType; }
    public void setTaskType(TaskType taskType) { this.taskType = taskType; }

    public Integer getModelId() { return modelId; }
    public void setModelId(Integer modelId) { this.modelId = modelId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getInputTokens() { return inputTokens; }
    public void setInputTokens(Integer inputTokens) { this.inputTokens = inputTokens; }

    public Integer getOutputTokens() { return outputTokens; }
    public void setOutputTokens(Integer outputTokens) { this.outputTokens = outputTokens; }

    public Double getTotalCost() { return totalCost; }
    public void setTotalCost(Double totalCost) { this.totalCost = totalCost; }

    public Double getQualityScore() { return qualityScore; }
    public void setQualityScore(Double qualityScore) { this.qualityScore = qualityScore; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getEndedAt() { return endedAt; }
    public void setEndedAt(LocalDateTime endedAt) { this.endedAt = endedAt; }

    public boolean isActive() {
        return "ACTIVE".equalsIgnoreCase(status);
    }

    @Override
    public String getEntityDisplayName() {
        return "Session #" + getId() + " (" + taskType + ", " + status + ")";
    }

    @Override
    public String getAuditSummary() {
        return "Session user=" + userId + " task=" + taskType + " model=" + modelId + " status=" + status;
    }

    @Override
    public LocalDateTime getTimestamp() {
        return getCreatedAt();
    }
}
