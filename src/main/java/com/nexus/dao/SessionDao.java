package com.nexus.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.nexus.domain.AgentSession;
import com.nexus.domain.TaskType;

public class SessionDao implements GenericDao<AgentSession> {
    private final Connection connection;

    public SessionDao() {
        this.connection = DbConnectionManager.getInstance().getConnection();
    }

    @Override
    public void create(AgentSession session) {
        String sql = "INSERT INTO agent_sessions (user_id, task_type, model_id, status, notes) VALUES (?,?,?,?,?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, session.getUserId());
            pstmt.setString(2, session.getTaskType().name());
            pstmt.setInt(3, session.getModelId());
            pstmt.setString(4, session.getStatus());
            pstmt.setString(5, session.getNotes());
            pstmt.executeUpdate();
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) session.setId(rs.getInt(1));
            }
        } catch (SQLException e) {
            System.err.println("Error creating session: " + e.getMessage());
        }
    }

    @Override
    public Optional<AgentSession> read(Integer id) {
        String sql = "SELECT * FROM agent_sessions WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error reading session: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public void update(AgentSession session) {
        String sql = "UPDATE agent_sessions SET user_id=?, task_type=?, model_id=?, status=?, input_tokens=?, output_tokens=?, total_cost=?, quality_score=?, notes=?, ended_at=? WHERE id=?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, session.getUserId());
            pstmt.setString(2, session.getTaskType().name());
            pstmt.setInt(3, session.getModelId());
            pstmt.setString(4, session.getStatus());
            if (session.getInputTokens() == null) pstmt.setNull(5, Types.INTEGER); else pstmt.setInt(5, session.getInputTokens());
            if (session.getOutputTokens() == null) pstmt.setNull(6, Types.INTEGER); else pstmt.setInt(6, session.getOutputTokens());
            if (session.getTotalCost() == null) pstmt.setNull(7, Types.REAL); else pstmt.setDouble(7, session.getTotalCost());
            if (session.getQualityScore() == null) pstmt.setNull(8, Types.REAL); else pstmt.setDouble(8, session.getQualityScore());
            pstmt.setString(9, session.getNotes());
            if (session.getEndedAt() == null) pstmt.setNull(10, Types.VARCHAR); else pstmt.setString(10, session.getEndedAt().toString());
            pstmt.setInt(11, session.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error updating session: " + e.getMessage());
        }
    }

    @Override
    public void delete(Integer id) {
        String sql = "DELETE FROM agent_sessions WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error deleting session: " + e.getMessage());
        }
    }

    @Override
    public List<AgentSession> findAll() {
        List<AgentSession> list = new ArrayList<>();
        String sql = "SELECT * FROM agent_sessions ORDER BY created_at DESC";
        try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            System.err.println("Error listing sessions: " + e.getMessage());
        }
        return list;
    }

    public List<AgentSession> findByUserId(int userId) {
        List<AgentSession> list = new ArrayList<>();
        String sql = "SELECT * FROM agent_sessions WHERE user_id = ? ORDER BY created_at DESC";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error listing user sessions: " + e.getMessage());
        }
        return list;
    }

    public List<AgentSession> findActiveByUserId(int userId) {
        List<AgentSession> list = new ArrayList<>();
        String sql = "SELECT * FROM agent_sessions WHERE user_id = ? AND status = 'ACTIVE' ORDER BY created_at DESC";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error listing active sessions: " + e.getMessage());
        }
        return list;
    }

    private AgentSession map(ResultSet rs) throws SQLException {
        LocalDateTime createdAt = rs.getTimestamp("created_at").toLocalDateTime();
        LocalDateTime endedAt = null;
        String ended = rs.getString("ended_at");
        if (ended != null && !ended.isBlank()) {
            endedAt = LocalDateTime.parse(ended.replace(" ", "T").substring(0, 19));
        }

        Integer inputTokens = rs.getObject("input_tokens") == null ? null : rs.getInt("input_tokens");
        Integer outputTokens = rs.getObject("output_tokens") == null ? null : rs.getInt("output_tokens");
        Double totalCost = rs.getObject("total_cost") == null ? null : rs.getDouble("total_cost");
        Double qualityScore = rs.getObject("quality_score") == null ? null : rs.getDouble("quality_score");

        return new AgentSession(
            rs.getInt("id"),
            rs.getInt("user_id"),
            TaskType.valueOf(rs.getString("task_type")),
            rs.getInt("model_id"),
            rs.getString("status"),
            inputTokens,
            outputTokens,
            totalCost,
            qualityScore,
            rs.getString("notes"),
            endedAt,
            createdAt
        );
    }
}
