package com.nexus.dao;

import com.nexus.domain.Memory;
import com.nexus.domain.MemoryType;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MemoryDao implements GenericDao<Memory> {
    private final Connection connection;

    public MemoryDao() {
        this.connection = DbConnectionManager.getInstance().getConnection();
    }

    @Override
    public void create(Memory mem) {
        String sql = "INSERT INTO memories (user_id, agent_id, content, tags, type, confidence, expires_at) VALUES (?,?,?,?,?,?,?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, mem.getUserId());
            pstmt.setString(2, mem.getAgentId() != null ? mem.getAgentId() : "default");
            pstmt.setString(3, mem.getContent());
            pstmt.setString(4, mem.getTags());
            pstmt.setString(5, mem.getType().name());
            pstmt.setDouble(6, mem.getConfidence());
            pstmt.setString(7, mem.getExpiresAt() != null ? mem.getExpiresAt().toString() : null);
            pstmt.executeUpdate();
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) mem.setId(rs.getInt(1));
            }
        } catch (SQLException e) { System.err.println("Error storing memory: " + e.getMessage()); }
    }

    @Override
    public Optional<Memory> read(Integer id) {
        try (PreparedStatement pstmt = connection.prepareStatement("SELECT * FROM memories WHERE id=?")) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        } catch (SQLException e) { System.err.println(e.getMessage()); }
        return Optional.empty();
    }

    @Override
    public void update(Memory mem) {
        String sql = "UPDATE memories SET content=?, tags=?, type=?, confidence=?, expires_at=? WHERE id=?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, mem.getContent());
            pstmt.setString(2, mem.getTags());
            pstmt.setString(3, mem.getType().name());
            pstmt.setDouble(4, mem.getConfidence());
            pstmt.setString(5, mem.getExpiresAt() != null ? mem.getExpiresAt().toString() : null);
            pstmt.setInt(6, mem.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) { System.err.println(e.getMessage()); }
    }

    @Override
    public void delete(Integer id) {
        try (PreparedStatement pstmt = connection.prepareStatement("DELETE FROM memories WHERE id=?")) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) { System.err.println(e.getMessage()); }
    }

    public boolean deleteByIdAndUserId(int id, int userId) {
        String sql = "DELETE FROM memories WHERE id=? AND user_id=?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.setInt(2, userId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println(e.getMessage());
            return false;
        }
    }

    @Override
    public List<Memory> findAll() {
        List<Memory> list = new ArrayList<>();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM memories ORDER BY confidence DESC")) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) { System.err.println(e.getMessage()); }
        return list;
    }

    public List<Memory> findByUserId(int userId) {
        List<Memory> list = new ArrayList<>();
        try (PreparedStatement pstmt = connection.prepareStatement(
                "SELECT * FROM memories WHERE user_id=? ORDER BY confidence DESC")) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) { while (rs.next()) list.add(map(rs)); }
        } catch (SQLException e) { System.err.println(e.getMessage()); }
        return list;
    }

    public List<Memory> searchContent(int userId, String query) {
        List<Memory> list = new ArrayList<>();
        try (PreparedStatement pstmt = connection.prepareStatement(
                "SELECT * FROM memories WHERE user_id=? AND (content LIKE ? OR tags LIKE ?) ORDER BY confidence DESC")) {
            String like = "%" + query + "%";
            pstmt.setInt(1, userId); pstmt.setString(2, like); pstmt.setString(3, like);
            try (ResultSet rs = pstmt.executeQuery()) { while (rs.next()) list.add(map(rs)); }
        } catch (SQLException e) { System.err.println(e.getMessage()); }
        return list;
    }

    public List<Memory> findByType(int userId, MemoryType type) {
        List<Memory> list = new ArrayList<>();
        try (PreparedStatement pstmt = connection.prepareStatement(
                "SELECT * FROM memories WHERE user_id=? AND type=? ORDER BY confidence DESC")) {
            pstmt.setInt(1, userId); pstmt.setString(2, type.name());
            try (ResultSet rs = pstmt.executeQuery()) { while (rs.next()) list.add(map(rs)); }
        } catch (SQLException e) { System.err.println(e.getMessage()); }
        return list;
    }

    public void updateConfidenceAndAccess(int id, double newConf) {
        try (PreparedStatement pstmt = connection.prepareStatement(
                "UPDATE memories SET confidence=?, access_count=access_count+1, last_accessed_at=CURRENT_TIMESTAMP WHERE id=?")) {
            pstmt.setDouble(1, newConf);
            pstmt.setInt(2, id);
            pstmt.executeUpdate();
        } catch (SQLException e) { System.err.println(e.getMessage()); }
    }

    public int pruneStale(int userId) {
        try (PreparedStatement pstmt = connection.prepareStatement(
                "DELETE FROM memories WHERE user_id=? AND (confidence < 0.10 OR (expires_at IS NOT NULL AND expires_at < CURRENT_TIMESTAMP))")) {
            pstmt.setInt(1, userId);
            return pstmt.executeUpdate();
        } catch (SQLException e) { System.err.println(e.getMessage()); return 0; }
    }

    public List<Memory> findContradictions(int userId) {
        return findByType(userId, MemoryType.CONTRADICTION);
    }

    private Memory map(ResultSet rs) throws SQLException {
        LocalDateTime createdAt = null, lastAccessed = null, expiresAt = null;
        try { createdAt = rs.getTimestamp("created_at").toLocalDateTime(); } catch (Exception ignored) {}
        try { if (rs.getString("last_accessed_at") != null) lastAccessed = rs.getTimestamp("last_accessed_at").toLocalDateTime(); } catch (Exception ignored) {}
        try { if (rs.getString("expires_at") != null) expiresAt = LocalDateTime.parse(rs.getString("expires_at").replace(" ", "T").substring(0, 19)); } catch (Exception ignored) {}

        return new Memory(
            rs.getInt("id"),
            rs.getInt("user_id"),
            rs.getString("agent_id"),
            rs.getString("content"),
            rs.getString("tags"),
            MemoryType.valueOf(rs.getString("type")),
            rs.getDouble("confidence"),
            rs.getInt("access_count"),
            lastAccessed,
            expiresAt,
            createdAt
        );
    }
}
