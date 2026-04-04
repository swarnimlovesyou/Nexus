package com.nexus.service;

import com.nexus.dao.UserDao;
import com.nexus.domain.User;

import java.util.List;
import java.util.Optional;

public class UserService {
    private final UserDao userDao;

    public UserService() {
        this.userDao = new UserDao();
    }

    public User registerUser(String username, String password, String role) {
        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Username and password cannot be empty");
        }
        
        Optional<User> existing = userDao.findByUsername(username);
        if (existing.isPresent()) {
            throw new IllegalArgumentException("Username already exists");
        }

        User newUser = new User();
        newUser.setUsername(username);
        // In a real system, you'd hash the password securely
        newUser.setPasswordHash(password); 
        newUser.setRole(role);
        
        userDao.create(newUser);
        return newUser;
    }

    public User authenticate(String username, String password) {
        Optional<User> userOpt = userDao.findByUsername(username);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (user.getPasswordHash().equals(password)) {
                return user;
            }
        }
        return null;
    }

    public List<User> getAllUsers() {
        return userDao.findAll();
    }
    
    public void deleteUser(Integer id) {
        userDao.delete(id);
    }
}
