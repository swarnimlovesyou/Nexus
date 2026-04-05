package com.nexus.dao;

import com.nexus.domain.ApiKey;
import com.nexus.domain.Provider;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ApiKeyDao implements GenericDao<ApiKey> {
    private final Connection connection;

    public ApiKeyDao() {
        this.connection = DbConnectionManager.getInstance().getConnection();
    }

    @Override
    public void create(ApiKey key) {
        String sql = "INSERT INTO api_keys (user_id, provider, alias, masked_key, encoded_key) VALUES (?,?,?,?,?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, key.getUserId());
            pstmt.setString(2, key.getProvider().name());
            pstmt.setString(3, key.getAlias());
            pstmt.setString(4, key.getMaskedKey());
            pstmt.setString(5, key.getEncodedKey());
            pstmt.executeUpdate();
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) key.setId(rs.getInt(1));
            }
        } catch (SQLException e) {
            System.err.println("Error creating API key: " + e.getMessage());
        }
    }

    @Override
    public Optional<ApiKey> read(Integer id) {
        String sql = "SELECT * FROM api_keys WHERE id=?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        } catch (SQLException e) { System.err.println(e.getMessage()); }
        return Optional.empty();
    }

    @Override
    public void update(ApiKey key) {
        String sql = "UPDATE api_keys SET alias=?, masked_key=?, encoded_key=? WHERE id=?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, key.getAlias());
            pstmt.setString(2, key.getMaskedKey());
            pstmt.setString(3, key.getEncodedKey());
            pstmt.setInt(4, key.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) { System.err.println(e.getMessage()); }
    }

    @Override
    public void delete(Integer id) {
        try (PreparedStatement pstmt = connection.prepareStatement("DELETE FROM api_keys WHERE id=?")) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) { System.err.println(e.getMessage()); }
    }

    @Override
    public List<ApiKey> findAll() {
        List<ApiKey> list = new ArrayList<>();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM api_keys")) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) { System.err.println(e.getMessage()); }
        return list;
    }

    public List<ApiKey> findByUserId(int userId) {
        List<ApiKey> list = new ArrayList<>();
        try (PreparedStatement pstmt = connection.prepareStatement("SELECT * FROM api_keys WHERE user_id=?")) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) { System.err.println(e.getMessage()); }
        return list;
    }

    public Optional<ApiKey> findByUserAndProvider(int userId, Provider provider) {
        try (PreparedStatement pstmt = connection.prepareStatement(
                "SELECT * FROM api_keys WHERE user_id=? AND provider=?")) {
            pstmt.setInt(1, userId);
            pstmt.setString(2, provider.name());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        } catch (SQLException e) { System.err.println(e.getMessage()); }
        return Optional.empty();
    }

    private ApiKey map(ResultSet rs) throws SQLException {
        LocalDateTime createdAt = null;
        try { createdAt = rs.getTimestamp("created_at").toLocalDateTime(); } catch (Exception ignored) {}
        return new ApiKey(
            rs.getInt("id"),
            rs.getInt("user_id"),
            Provider.valueOf(rs.getString("provider")),
            rs.getString("alias"),
            rs.getString("masked_key"),
            rs.getString("encoded_key"),
            createdAt
        );
    }
}
