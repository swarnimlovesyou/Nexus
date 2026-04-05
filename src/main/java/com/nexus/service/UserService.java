package com.nexus.service;

import com.nexus.dao.UserDao;
import com.nexus.domain.AdminUser;
import com.nexus.domain.RegularUser;
import com.nexus.domain.User;
import com.nexus.exception.AuthenticationException;
import com.nexus.exception.ResourceNotFoundException;
import com.nexus.exception.ValidationException;
import com.nexus.util.SecurityUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class UserService {
    private final UserDao userDao;

    public UserService() {
        this.userDao = new UserDao();
    }

    public User registerUser(String username, String password, String role) {
        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            throw new ValidationException("Username and password cannot be empty");
        }
        
        Optional<User> existing = userDao.findByUsername(username);
        if (existing.isPresent()) {
            throw new ValidationException("Username already exists");
        }

        String hashedPassword = SecurityUtils.hashPassword(password);
        User newUser;
        if ("ADMIN".equalsIgnoreCase(role)) {
            newUser = new AdminUser(null, username, hashedPassword, LocalDateTime.now());
        } else {
            newUser = new RegularUser(null, username, hashedPassword, LocalDateTime.now());
        }
        
        userDao.create(newUser);
        return newUser;
    }

    public User authenticate(String username, String password) {
        Optional<User> userOpt = userDao.findByUsername(username);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (SecurityUtils.verifyPassword(password, user.getPasswordHash())) {
                return user;
            }
        }
        throw new AuthenticationException("Invalid username or password");
    }

    public List<User> searchUsers(String query) {
        return userDao.findByUsernameContaining(query);
    }

    public List<User> getAllUsers() {
        return userDao.findAll();
    }
    
    public void deleteUser(Integer id) {
        userDao.delete(id);
    }

    public User updateUser(Integer id, String newUsername, String newPassword) {
        Optional<User> opt = userDao.read(id);
        if (opt.isEmpty()) throw new ResourceNotFoundException("User not found");
        
        User user = opt.get();
        if (newUsername != null && !newUsername.trim().isEmpty()) {
            // Check for duplicate
            Optional<User> existing = userDao.findByUsername(newUsername);
            if (existing.isPresent() && !existing.get().getId().equals(id)) {
                throw new ValidationException("Username already exists");
            }
            user.setUsername(newUsername.trim());
        }
        
        if (newPassword != null && !newPassword.trim().isEmpty()) {
            user.setPasswordHash(SecurityUtils.hashPassword(newPassword.trim()));
        }
        
        userDao.update(user);
        return user;
    }
}
