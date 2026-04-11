package com.nexus.dao;

import com.nexus.exception.DaoException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

public class UserProfileDao {
    public static final String GLOBAL_SCOPE = "global";

    private final Connection connection;
    private final DbTimeCodec timeCodec;

    public UserProfileDao() {
        this.connection = DbConnectionManager.getInstance().getConnection();
        this.timeCodec = new DbTimeCodec();
    }

    public void upsert(int userId, String scope, String key, String value) {
        String sql = """
            INSERT INTO user_profiles (user_id, scope, profile_key, profile_value, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?)
            ON CONFLICT(user_id, scope, profile_key)
            DO UPDATE SET profile_value=excluded.profile_value, updated_at=excluded.updated_at
            """;
        long now = timeCodec.nowEpochSeconds();

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setString(2, scope);
            pstmt.setString(3, key);
            pstmt.setString(4, value);
            pstmt.setLong(5, now);
            pstmt.setLong(6, now);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException("Failed to store profile setting.", e);
        }
    }

    public Map<String, String> findByUserAndScope(int userId, String scope) {
        Map<String, String> map = new LinkedHashMap<>();
        String sql = "SELECT profile_key, profile_value FROM user_profiles WHERE user_id=? AND scope=? ORDER BY profile_key";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setString(2, scope);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    map.put(rs.getString("profile_key"), rs.getString("profile_value"));
                }
            }
        } catch (SQLException e) {
            throw new DaoException("Failed to load profile settings.", e);
        }
        return map;
    }

    public Map<String, String> findMergedByUserAndScope(int userId, String scope) {
        Map<String, String> merged = new LinkedHashMap<>();
        merged.putAll(findByUserAndScope(userId, GLOBAL_SCOPE));
        if (!GLOBAL_SCOPE.equals(scope)) {
            merged.putAll(findByUserAndScope(userId, scope));
        }
        return merged;
    }

    public boolean deleteByUserScopeAndKey(int userId, String scope, String key) {
        String sql = "DELETE FROM user_profiles WHERE user_id=? AND scope=? AND profile_key=?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setString(2, scope);
            pstmt.setString(3, key);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DaoException("Failed to delete profile setting.", e);
        }
    }
}
