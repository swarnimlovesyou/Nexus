package com.nexus.dao;

import com.nexus.domain.ModelSuitability;
import com.nexus.domain.TaskType;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SuitabilityDao implements GenericDao<ModelSuitability> {
    private final Connection connection;

    public SuitabilityDao() {
        this.connection = DbConnectionManager.getInstance().getConnection();
    }

    @Override
    public void create(ModelSuitability suitability) {
        String sql = "INSERT INTO model_suitability (model_id, task_type, base_score) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, suitability.getModelId());
            pstmt.setString(2, suitability.getTaskType().name());
            pstmt.setDouble(3, suitability.getBaseScore());
            pstmt.executeUpdate();

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    suitability.setId(generatedKeys.getInt(1));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error creating suitability: " + e.getMessage());
        }
    }

    @Override
    public Optional<ModelSuitability> read(Integer id) {
        String sql = "SELECT * FROM model_suitability WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToSuitability(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error reading suitability: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public void update(ModelSuitability suitability) {
        String sql = "UPDATE model_suitability SET model_id = ?, task_type = ?, base_score = ? WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, suitability.getModelId());
            pstmt.setString(2, suitability.getTaskType().name());
            pstmt.setDouble(3, suitability.getBaseScore());
            pstmt.setInt(4, suitability.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error updating suitability: " + e.getMessage());
        }
    }

    @Override
    public void delete(Integer id) {
        String sql = "DELETE FROM model_suitability WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error deleting suitability: " + e.getMessage());
        }
    }

    @Override
    public List<ModelSuitability> findAll() {
        List<ModelSuitability> list = new ArrayList<>();
        String sql = "SELECT * FROM model_suitability";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapResultSetToSuitability(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching all suitabilities: " + e.getMessage());
        }
        return list;
    }
    
    public List<ModelSuitability> findByTaskType(TaskType taskType) {
        List<ModelSuitability> list = new ArrayList<>();
        String sql = "SELECT * FROM model_suitability WHERE task_type = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, taskType.name());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToSuitability(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching suitabilities by task type: " + e.getMessage());
        }
        return list;
    }

    private ModelSuitability mapResultSetToSuitability(ResultSet rs) throws SQLException {
        LocalDateTime createdAt = null;
        if (rs.getString("created_at") != null) {
            createdAt = rs.getTimestamp("created_at").toLocalDateTime();
        }
        return new ModelSuitability(
                rs.getInt("id"),
                rs.getInt("model_id"),
                TaskType.valueOf(rs.getString("task_type")),
                rs.getDouble("base_score"),
                createdAt
        );
    }
}
