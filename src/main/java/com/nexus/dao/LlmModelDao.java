package com.nexus.dao;

import com.nexus.domain.LlmModel;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class LlmModelDao implements GenericDao<LlmModel> {
    private final Connection connection;
    private final DbTimeCodec timeCodec;

    public LlmModelDao() {
        this.connection = DbConnectionManager.getInstance().getConnection();
        this.timeCodec = new DbTimeCodec();
    }

    @Override
    public void create(LlmModel model) {
        String sql = "INSERT INTO llm_models (name, provider, cost_per_1k_tokens) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, model.getName());
            pstmt.setString(2, model.getProvider());
            pstmt.setDouble(3, model.getCostPer1kTokens());
            pstmt.executeUpdate();

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    model.setId(generatedKeys.getInt(1));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error creating model: " + e.getMessage());
        }
    }

    @Override
    public Optional<LlmModel> read(Integer id) {
        String sql = "SELECT * FROM llm_models WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToModel(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error reading model: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public void update(LlmModel model) {
        String sql = "UPDATE llm_models SET name = ?, provider = ?, cost_per_1k_tokens = ? WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, model.getName());
            pstmt.setString(2, model.getProvider());
            pstmt.setDouble(3, model.getCostPer1kTokens());
            pstmt.setInt(4, model.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error updating model: " + e.getMessage());
        }
    }

    @Override
    public void delete(Integer id) {
        String sql = "DELETE FROM llm_models WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error deleting model: " + e.getMessage());
        }
    }

    @Override
    public List<LlmModel> findAll() {
        List<LlmModel> models = new ArrayList<>();
        String sql = "SELECT * FROM llm_models";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                models.add(mapResultSetToModel(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching all models: " + e.getMessage());
        }
        return models;
    }

    public List<LlmModel> findByProvider(String provider) {
        List<LlmModel> models = new ArrayList<>();
        String sql = "SELECT * FROM llm_models WHERE provider LIKE ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, "%" + provider + "%");
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    models.add(mapResultSetToModel(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error filtering models by provider: " + e.getMessage());
        }
        return models;
    }

    private LlmModel mapResultSetToModel(ResultSet rs) throws SQLException {
        LocalDateTime createdAt = timeCodec.readDateTime(rs, "created_at");
        return new LlmModel(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("provider"),
                rs.getDouble("cost_per_1k_tokens"),
                createdAt
        );
    }
}
