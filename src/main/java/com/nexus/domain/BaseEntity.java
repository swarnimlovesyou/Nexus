package com.nexus.domain;

import java.time.LocalDateTime;

/**
 * Common abstract class to demonstrate Inheritance and Abstraction.
 */
public abstract class BaseEntity {
    private Integer id;
    private LocalDateTime createdAt;
    
    public BaseEntity() {
        this.createdAt = LocalDateTime.now();
    }
    
    public BaseEntity(Integer id, LocalDateTime createdAt) {
        this.id = id;
        this.createdAt = createdAt;
    }

    // Encapsulation with Getters and Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    /**
     * Abstract method to enforce implementation down the hierarchy
     */
    public abstract String getEntityDisplayName();
    
    // Demonstrating method overriding in subclasses
    @Override
    public String toString() {
        return "BaseEntity{id=" + id + ", createdAt=" + createdAt + "}";
    }
}
