package com.nexus.dao;

import com.nexus.domain.OutcomeMemory;
import com.nexus.domain.TaskType;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class OutcomeMemoryDao implements GenericDao<OutcomeMemory> {
    private final Connection connection;

    public OutcomeMemoryDao() {
        this.connection = DbConnectionManager.getInstance().getConnection();
    }

    @Override
    public void create(OutcomeMemory memory) {
        String sql = "INSERT INTO outcome_memories (user_id, model_id, task_type, cost, latency_ms, quality_score) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, memory.getUserId());
            pstmt.setInt(2, memory.getModelId());
            pstmt.setString(3, memory.getTaskType().name());
            pstmt.setDouble(4, memory.getCost());
            pstmt.setInt(5, memory.getLatencyMs());
            pstmt.setDouble(6, memory.getQualityScore());
            pstmt.executeUpdate();

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    memory.setId(generatedKeys.getInt(1));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error creating outcome memory: " + e.getMessage());
        }
    }

    @Override
    public Optional<OutcomeMemory> read(Integer id) {
        String sql = "SELECT * FROM outcome_memories WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToMemory(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error reading memory: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public void update(OutcomeMemory memory) {
        String sql = "UPDATE outcome_memories SET user_id = ?, model_id = ?, task_type = ?, cost = ?, latency_ms = ?, quality_score = ? WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, memory.getUserId());
            pstmt.setInt(2, memory.getModelId());
            pstmt.setString(3, memory.getTaskType().name());
            pstmt.setDouble(4, memory.getCost());
            pstmt.setInt(5, memory.getLatencyMs());
            pstmt.setDouble(6, memory.getQualityScore());
            pstmt.setInt(7, memory.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error updating memory: " + e.getMessage());
        }
    }

    @Override
    public void delete(Integer id) {
        String sql = "DELETE FROM outcome_memories WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error deleting memory: " + e.getMessage());
        }
    }

    @Override
    public List<OutcomeMemory> findAll() {
        List<OutcomeMemory> list = new ArrayList<>();
        String sql = "SELECT * FROM outcome_memories";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapResultSetToMemory(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching all memories: " + e.getMessage());
        }
        return list;
    }

    public List<OutcomeMemory> findByUserId(Integer userId) {
        List<OutcomeMemory> list = new ArrayList<>();
        String sql = "SELECT * FROM outcome_memories WHERE user_id = ? ORDER BY created_at DESC";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToMemory(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching memories by user id: " + e.getMessage());
        }
        return list;
    }

    public List<OutcomeMemory> findByTaskType(TaskType taskType) {
        List<OutcomeMemory> list = new ArrayList<>();
        String sql = "SELECT * FROM outcome_memories WHERE task_type = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, taskType.name());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToMemory(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching memories by task type: " + e.getMessage());
        }
        return list;
    }

    public List<OutcomeMemory> findByUserAndTaskType(Integer userId, TaskType taskType) {
        List<OutcomeMemory> list = new ArrayList<>();
        String sql = "SELECT * FROM outcome_memories WHERE user_id = ? AND task_type = ? ORDER BY created_at DESC";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setString(2, taskType.name());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToMemory(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching memories by user/task: " + e.getMessage());
        }
        return list;
    }

    public List<OutcomeMemory> findByModelId(Integer modelId) {
        List<OutcomeMemory> list = new ArrayList<>();
        String sql = "SELECT * FROM outcome_memories WHERE model_id = ? ORDER BY created_at DESC";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, modelId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToMemory(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching memories by model id: " + e.getMessage());
        }
        return list;
    }

    public List<OutcomeMemory> findByUserAndModelId(Integer userId, Integer modelId) {
        List<OutcomeMemory> list = new ArrayList<>();
        String sql = "SELECT * FROM outcome_memories WHERE user_id = ? AND model_id = ? ORDER BY created_at DESC";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, modelId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToMemory(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching memories by user/model: " + e.getMessage());
        }
        return list;
    }

    /**
     * Find execution records within a date window.
     * Demonstrates ArrayList collection + date-range SQL filter.
     * @param from  start of window (inclusive)
     * @param to    end of window (inclusive)
     */
    public List<OutcomeMemory> findByDateRange(java.time.LocalDateTime from, java.time.LocalDateTime to) {
        List<OutcomeMemory> list = new ArrayList<>();
        String sql = "SELECT * FROM outcome_memories WHERE created_at >= ? AND created_at <= ? ORDER BY created_at DESC";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, from.toString());
            pstmt.setString(2, to.toString());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) list.add(mapResultSetToMemory(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching by date range: " + e.getMessage());
        }
        return list;
    }

    public List<OutcomeMemory> findByUserAndDateRange(Integer userId, java.time.LocalDateTime from, java.time.LocalDateTime to) {
        List<OutcomeMemory> list = new ArrayList<>();
        String sql = "SELECT * FROM outcome_memories WHERE user_id = ? AND created_at >= ? AND created_at <= ? ORDER BY created_at DESC";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setString(2, from.toString());
            pstmt.setString(3, to.toString());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) list.add(mapResultSetToMemory(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching by user/date range: " + e.getMessage());
        }
        return list;
    }

    private OutcomeMemory mapResultSetToMemory(ResultSet rs) throws SQLException {
        LocalDateTime createdAt = null;
        if (rs.getString("created_at") != null) {
            createdAt = rs.getTimestamp("created_at").toLocalDateTime();
        }
        return new OutcomeMemory(
                rs.getInt("id"),
                rs.getInt("user_id"),
                rs.getInt("model_id"),
                TaskType.valueOf(rs.getString("task_type")),
                rs.getDouble("cost"),
                rs.getInt("latency_ms"),
                rs.getDouble("quality_score"),
                createdAt
        );
    }
}
