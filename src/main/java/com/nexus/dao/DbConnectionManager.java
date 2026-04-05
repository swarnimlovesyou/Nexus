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
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(DB_URL);
            // Enable foreign keys
            try (Statement fk = connection.createStatement()) {
                fk.execute("PRAGMA foreign_keys = ON");
            }
            initializeDatabase();
            migrateLegacyTimestamps();
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

    public Connection getConnection() { return connection; }

    private void initializeDatabase() {
        try (Statement stmt = connection.createStatement()) {
            // ── Users ───────────────────────────────────────────────────────
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    username TEXT UNIQUE NOT NULL,
                    password_hash TEXT NOT NULL,
                    role TEXT NOT NULL,
                    created_at INTEGER DEFAULT (strftime('%s','now'))
                )""");

            // ── LLM Models ──────────────────────────────────────────────────
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS llm_models (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    provider TEXT NOT NULL,
                    cost_per_1k_tokens REAL NOT NULL,
                    created_at INTEGER DEFAULT (strftime('%s','now'))
                )""");

            // ── Model Suitability ───────────────────────────────────────────
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS model_suitability (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    model_id INTEGER,
                    task_type TEXT NOT NULL,
                    base_score REAL NOT NULL,
                    created_at INTEGER DEFAULT (strftime('%s','now')),
                    FOREIGN KEY(model_id) REFERENCES llm_models(id)
                )""");

            // ── Outcome Memories ────────────────────────────────────────────
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS outcome_memories (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER,
                    model_id INTEGER,
                    task_type TEXT NOT NULL,
                    cost REAL,
                    latency_ms INTEGER,
                    quality_score REAL,
                    created_at INTEGER DEFAULT (strftime('%s','now')),
                    FOREIGN KEY(user_id) REFERENCES users(id),
                    FOREIGN KEY(model_id) REFERENCES llm_models(id)
                )""");

            // ── Agent Sessions ──────────────────────────────────────────────
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS agent_sessions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER NOT NULL,
                    task_type TEXT NOT NULL,
                    model_id INTEGER NOT NULL,
                    status TEXT NOT NULL DEFAULT 'ACTIVE',
                    input_tokens INTEGER,
                    output_tokens INTEGER,
                    total_cost REAL,
                    quality_score REAL,
                    notes TEXT,
                    ended_at INTEGER,
                    created_at INTEGER DEFAULT (strftime('%s','now')),
                    FOREIGN KEY(user_id) REFERENCES users(id),
                    FOREIGN KEY(model_id) REFERENCES llm_models(id)
                )""");

            // ── API Keys ────────────────────────────────────────────────────
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS api_keys (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER NOT NULL,
                    provider TEXT NOT NULL,
                    alias TEXT NOT NULL,
                    masked_key TEXT NOT NULL,
                    encoded_key TEXT NOT NULL,
                    created_at INTEGER DEFAULT (strftime('%s','now')),
                    FOREIGN KEY(user_id) REFERENCES users(id)
                )""");

            // ── Contextd Memories ───────────────────────────────────────────
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS memories (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER NOT NULL,
                    agent_id TEXT DEFAULT 'default',
                    content TEXT NOT NULL,
                    tags TEXT,
                    type TEXT NOT NULL,
                    confidence REAL DEFAULT 1.0,
                    access_count INTEGER DEFAULT 0,
                    last_accessed_at INTEGER,
                    expires_at INTEGER,
                    created_at INTEGER DEFAULT (strftime('%s','now')),
                    FOREIGN KEY(user_id) REFERENCES users(id)
                )""");

            // ── Audit Log ───────────────────────────────────────────────────
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS audit_log (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER,
                    action TEXT NOT NULL,
                    details TEXT,
                    outcome TEXT DEFAULT 'SUCCESS',
                    created_at INTEGER DEFAULT (strftime('%s','now')),
                    FOREIGN KEY(user_id) REFERENCES users(id)
                )""");

        } catch (SQLException e) {
            System.err.println("Failed to initialize DB: " + e.getMessage());
        }
    }

    private void migrateLegacyTimestamps() {
        try (Statement stmt = connection.createStatement()) {
            migrateColumn(stmt, "users", "created_at");
            migrateColumn(stmt, "llm_models", "created_at");
            migrateColumn(stmt, "model_suitability", "created_at");
            migrateColumn(stmt, "outcome_memories", "created_at");
            migrateColumn(stmt, "agent_sessions", "created_at");
            migrateColumn(stmt, "agent_sessions", "ended_at");
            migrateColumn(stmt, "api_keys", "created_at");
            migrateColumn(stmt, "memories", "created_at");
            migrateColumn(stmt, "memories", "last_accessed_at");
            migrateColumn(stmt, "memories", "expires_at");
            migrateColumn(stmt, "audit_log", "created_at");
        } catch (SQLException e) {
            System.err.println("Failed to migrate legacy timestamps: " + e.getMessage());
        }
    }

    private void migrateColumn(Statement stmt, String table, String column) throws SQLException {
        String sql = String.format(
            "UPDATE %s SET %s = CAST(strftime('%%s', replace(substr(%s,1,19), 'T', ' ')) AS INTEGER) " +
            "WHERE %s IS NOT NULL AND %s LIKE '____-__-__%%'",
            table, column, column, column, column
        );
        stmt.executeUpdate(sql);
    }
}
