package com.nexus.domain;

import java.time.LocalDateTime;

public class Memory extends BaseEntity implements Auditable {
    private Integer userId;
    private String agentId;
    private String content;
    private String tags;
    private MemoryType type;
    private double confidence;
    private int accessCount;
    private LocalDateTime lastAccessedAt;
    private LocalDateTime expiresAt;

    public Memory() { super(); }

    public Memory(Integer id, Integer userId, String agentId, String content, String tags,
                  MemoryType type, double confidence, int accessCount,
                  LocalDateTime lastAccessedAt, LocalDateTime expiresAt, LocalDateTime createdAt) {
        super(id, createdAt);
        this.userId = userId;
        this.agentId = agentId;
        this.content = content;
        this.tags = tags;
        this.type = type;
        this.confidence = confidence;
        this.accessCount = accessCount;
        this.lastAccessedAt = lastAccessedAt;
        this.expiresAt = expiresAt;
    }

    // --- Getters & Setters ---
    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }
    public String getAgentId() { return agentId; }
    public void setAgentId(String agentId) { this.agentId = agentId; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    public MemoryType getType() { return type; }
    public void setType(MemoryType type) { this.type = type; }
    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }
    public int getAccessCount() { return accessCount; }
    public void setAccessCount(int accessCount) { this.accessCount = accessCount; }
    public LocalDateTime getLastAccessedAt() { return lastAccessedAt; }
    public void setLastAccessedAt(LocalDateTime lastAccessedAt) { this.lastAccessedAt = lastAccessedAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    /** Returns days until expiry — negative means already expired */
    public long daysUntilExpiry() {
        if (expiresAt == null) return Long.MAX_VALUE;
        return java.time.temporal.ChronoUnit.DAYS.between(LocalDateTime.now(), expiresAt);
    }

    /** True if confidence has degraded below pruning threshold */
    public boolean shouldPrune() {
        return confidence < 0.10 || (expiresAt != null && LocalDateTime.now().isAfter(expiresAt));
    }

    @Override
    public String getEntityDisplayName() {
        return "[" + type + "] " + (content.length() > 60 ? content.substring(0, 57) + "..." : content);
    }

    // --- Auditable interface ---
    @Override
    public String getAuditSummary() {
        return "Memory#" + getId() + " [" + type + "] conf=" + String.format("%.2f", confidence) + " tags=" + tags;
    }

    @Override
    public LocalDateTime getTimestamp() {
        return getCreatedAt();
    }

    @Override
    public String toString() {
        return "Memory{id=" + getId() + ", type=" + type + ", confidence=" + confidence + ", content='" + content + "'}";
    }
}
