package com.nexus.dao;

import com.nexus.domain.AuditLog;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AuditLogDao implements GenericDao<AuditLog> {
    private final Connection connection;
    private final DbTimeCodec timeCodec;

    public AuditLogDao() {
        this.connection = DbConnectionManager.getInstance().getConnection();
        this.timeCodec = new DbTimeCodec();
    }

    @Override
    public void create(AuditLog log) {
        String sql = "INSERT INTO audit_log (user_id, action, details, outcome) VALUES (?,?,?,?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            if (log.getUserId() != null) pstmt.setInt(1, log.getUserId());
            else pstmt.setNull(1, Types.INTEGER);
            pstmt.setString(2, log.getAction());
            pstmt.setString(3, log.getDetails());
            pstmt.setString(4, log.getOutcome());
            pstmt.executeUpdate();
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) log.setId(rs.getInt(1));
            }
        } catch (SQLException e) { System.err.println("Audit log error: " + e.getMessage()); }
    }

    @Override public Optional<AuditLog> read(Integer id) { return Optional.empty(); }
    @Override public void update(AuditLog entity) {}
    @Override public void delete(Integer id) {}

    @Override
    public List<AuditLog> findAll() {
        List<AuditLog> list = new ArrayList<>();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM audit_log ORDER BY created_at DESC LIMIT 200")) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) { System.err.println(e.getMessage()); }
        return list;
    }

    public List<AuditLog> findByUserId(int userId) {
        List<AuditLog> list = new ArrayList<>();
        try (PreparedStatement pstmt = connection.prepareStatement(
                "SELECT * FROM audit_log WHERE user_id=? ORDER BY created_at DESC LIMIT 100")) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) { while (rs.next()) list.add(map(rs)); }
        } catch (SQLException e) { System.err.println(e.getMessage()); }
        return list;
    }

    private AuditLog map(ResultSet rs) throws SQLException {
        LocalDateTime createdAt = timeCodec.readDateTime(rs, "created_at");
        int userId = rs.getInt("user_id");
        return new AuditLog(
            rs.getInt("id"),
            rs.wasNull() ? null : userId,
            rs.getString("action"),
            rs.getString("details"),
            rs.getString("outcome"),
            createdAt
        );
    }
}
