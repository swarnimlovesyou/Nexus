package com.nexus.domain;

import java.time.LocalDateTime;

public class RegularUser extends User {

    public RegularUser(Integer id, String username, String passwordHash, LocalDateTime createdAt) {
        super(id, username, passwordHash, "USER", createdAt);
    }

    @Override
    public String getEntityDisplayName() {
        return "Regular User: " + getUsername();
    }

    @Override
    public String toString() {
        return "RegularUser{" +
               "id=" + getId() +
               ", username='" + getUsername() + '\'' +
               ", role='USER'" +
               '}';
    }
}
