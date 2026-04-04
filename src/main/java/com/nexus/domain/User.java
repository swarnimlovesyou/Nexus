package com.nexus.domain;

import java.time.LocalDateTime;

public abstract class User extends BaseEntity implements Auditable {
    private String username;
    private String passwordHash;
    private String role;

    public User() {
        super();
    }
    
    public User(Integer id, String username, String passwordHash, String role, LocalDateTime createdAt) {
        super(id, createdAt);
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    @Override
    public LocalDateTime getTimestamp() {
        return getCreatedAt();
    }

    @Override
    public String getAuditSummary() {
        return "User: " + username + " with role: " + role;
    }
}
