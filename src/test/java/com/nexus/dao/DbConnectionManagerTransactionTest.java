package com.nexus.dao;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DbConnectionManagerTransactionTest {

    @Test
    public void withTransactionShouldRollbackOnFailure() throws Exception {
        DbConnectionManager manager = DbConnectionManager.getInstance();
        Connection connection = manager.getConnection();

        try (Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS tx_test (id INTEGER PRIMARY KEY AUTOINCREMENT, value INTEGER)");
            stmt.execute("DELETE FROM tx_test");
        }

        manager.withTransaction(() -> {
            insertValue(connection, 1);
            return null;
        });

        assertEquals(1, countRows(connection));

        assertThrows(RuntimeException.class, () ->
            manager.withTransaction(() -> {
                insertValue(connection, 2);
                throw new RuntimeException("force rollback");
            })
        );

        assertEquals(1, countRows(connection));
    }

    private void insertValue(Connection connection, int value) {
        try (PreparedStatement pstmt = connection.prepareStatement("INSERT INTO tx_test (value) VALUES (?)")) {
            pstmt.setInt(1, value);
            pstmt.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private int countRows(Connection connection) {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM tx_test")) {
            rs.next();
            return rs.getInt(1);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
