package com.nexus.domain;

import java.time.LocalDateTime;

public class AuditLog extends BaseEntity {
    private Integer userId;
    private String action;
    private String details;
    private String outcome; // SUCCESS / FAILURE

    public AuditLog() { super(); }

    public AuditLog(Integer id, Integer userId, String action, String details, String outcome, LocalDateTime createdAt) {
        super(id, createdAt);
        this.userId = userId;
        this.action = action;
        this.details = details;
        this.outcome = outcome;
    }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
    public String getOutcome() { return outcome; }
    public void setOutcome(String outcome) { this.outcome = outcome; }

    @Override
    public String getEntityDisplayName() {
        return "[" + outcome + "] " + action + " — " + details;
    }

    @Override
    public String toString() {
        return getCreatedAt() + " | " + outcome + " | " + action + " | " + details;
    }
}
