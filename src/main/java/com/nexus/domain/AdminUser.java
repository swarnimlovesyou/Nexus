package com.nexus.domain;

import java.time.LocalDateTime;

public class AdminUser extends User {
    
    public AdminUser(Integer id, String username, String passwordHash, LocalDateTime createdAt) {
        super(id, username, passwordHash, "ADMIN", createdAt);
    }

    @Override
    public String getEntityDisplayName() {
        return "Administrator: " + getUsername();
    }

    @Override
    public String toString() {
        return "AdminUser{" +
               "id=" + getId() +
               ", username='" + getUsername() + '\'' +
               ", role='ADMIN'" +
               '}';
    }
}
