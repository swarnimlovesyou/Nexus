package com.nexus.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DbConnectionManager {
    private static DbConnectionManager instance;
    private Connection connection;
    private static final String DB_URL = "jdbc:sqlite:nexus.db";

    private DbConnectionManager() {
        try {
            // Ensure driver is loaded
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(DB_URL);
            initializeDatabase();
        } catch (ClassNotFoundException | SQLException e) {
            System.err.println("Database connection failed: " + e.getMessage());
        }
    }

    public static DbConnectionManager getInstance() {
        if (instance == null) {
            instance = new DbConnectionManager();
        }
        return instance;
    }

    public Connection getConnection() {
        return connection;
    }

    private void initializeDatabase() {
        String createUsersTable = "CREATE TABLE IF NOT EXISTS users ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "username TEXT UNIQUE NOT NULL,"
                + "password_hash TEXT NOT NULL,"
                + "role TEXT NOT NULL,"
                + "created_at TEXT DEFAULT CURRENT_TIMESTAMP"
                + ");";

        String createLlmModelsTable = "CREATE TABLE IF NOT EXISTS llm_models ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "name TEXT NOT NULL,"
                + "provider TEXT NOT NULL,"
                + "cost_per_1k_tokens REAL NOT NULL,"
                + "created_at TEXT DEFAULT CURRENT_TIMESTAMP"
                + ");";

        String createModelSuitabilityTable = "CREATE TABLE IF NOT EXISTS model_suitability ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "model_id INTEGER,"
                + "task_type TEXT NOT NULL,"
                + "base_score REAL NOT NULL,"
                + "created_at TEXT DEFAULT CURRENT_TIMESTAMP,"
                + "FOREIGN KEY(model_id) REFERENCES llm_models(id)"
                + ");";

        String createOutcomeMemoriesTable = "CREATE TABLE IF NOT EXISTS outcome_memories ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "user_id INTEGER,"
                + "model_id INTEGER,"
                + "task_type TEXT NOT NULL,"
                + "cost REAL,"
                + "latency_ms INTEGER,"
                + "quality_score REAL,"
                + "created_at TEXT DEFAULT CURRENT_TIMESTAMP,"
                + "FOREIGN KEY(user_id) REFERENCES users(id),"
                + "FOREIGN KEY(model_id) REFERENCES llm_models(id)"
                + ");";

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createUsersTable);
            stmt.execute(createLlmModelsTable);
            stmt.execute(createModelSuitabilityTable);
            stmt.execute(createOutcomeMemoriesTable);
        } catch (SQLException e) {
            System.err.println("Failed to initialize database tables: " + e.getMessage());
        }
    }
}
