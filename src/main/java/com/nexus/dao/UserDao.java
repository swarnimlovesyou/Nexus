package com.nexus.dao;

import com.nexus.domain.AdminUser;
import com.nexus.domain.RegularUser;
import com.nexus.domain.User;
import com.nexus.exception.DaoException;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserDao implements GenericDao<User> {
    private final Connection connection;
    private final DbTimeCodec timeCodec;

    public UserDao() {
        this.connection = DbConnectionManager.getInstance().getConnection();
        this.timeCodec = new DbTimeCodec();
    }

    @Override
    public void create(User user) {
        String sql = "INSERT INTO users (username, password_hash, role) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getPasswordHash());
            pstmt.setString(3, user.getRole());
            pstmt.executeUpdate();

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    user.setId(generatedKeys.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new DaoException("Failed to create user.", e);
        }
    }

    @Override
    public Optional<User> read(Integer id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUser(rs));
                }
            }
        } catch (SQLException e) {
            throw new DaoException("Failed to read user by id.", e);
        }
        return Optional.empty();
    }

    @Override
    public void update(User user) {
        String sql = "UPDATE users SET username = ?, password_hash = ?, role = ? WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getPasswordHash());
            pstmt.setString(3, user.getRole());
            pstmt.setInt(4, user.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException("Failed to update user.", e);
        }
    }

    @Override
    public void delete(Integer id) {
        String sql = "DELETE FROM users WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException("Failed to delete user.", e);
        }
    }

    @Override
    public List<User> findAll() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }
        } catch (SQLException e) {
            throw new DaoException("Failed to fetch all users.", e);
        }
        return users;
    }

    public Optional<User> findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUser(rs));
                }
            }
        } catch (SQLException e) {
            throw new DaoException("Failed to read user by username.", e);
        }
        return Optional.empty();
    }

    public List<User> findByUsernameContaining(String query) {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users WHERE username LIKE ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, "%" + query + "%");
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    users.add(mapResultSetToUser(rs));
                }
            }
        } catch (SQLException e) {
            throw new DaoException("Failed to search users.", e);
        }
        return users;
    }

    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        LocalDateTime createdAt = timeCodec.readDateTime(rs, "created_at");
        String role = rs.getString("role");
        String username = rs.getString("username");
        String passwordHash = rs.getString("password_hash");
        Integer id = rs.getInt("id");

        if ("ADMIN".equalsIgnoreCase(role)) {
            return new AdminUser(id, username, passwordHash, createdAt);
        } else {
            return new RegularUser(id, username, passwordHash, createdAt);
        }
    }
}
