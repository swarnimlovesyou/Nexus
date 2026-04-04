-- Nexus Application Database Schema

-- Users Table
CREATE TABLE IF NOT EXISTS users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT UNIQUE NOT NULL,
    password_hash TEXT NOT NULL,
    role TEXT NOT NULL,
    created_at TEXT DEFAULT CURRENT_TIMESTAMP
);

-- LLM Models Table
CREATE TABLE IF NOT EXISTS llm_models (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    provider TEXT NOT NULL,
    cost_per_1k_tokens REAL NOT NULL,
    created_at TEXT DEFAULT CURRENT_TIMESTAMP
);

-- Explicit Model Task Suitability Table
CREATE TABLE IF NOT EXISTS model_suitability (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    model_id INTEGER,
    task_type TEXT NOT NULL,
    base_score REAL NOT NULL,
    created_at TEXT DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY(model_id) REFERENCES llm_models(id)
);

-- Historical Execution Data (Outcome Memory)
CREATE TABLE IF NOT EXISTS outcome_memories (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER,
    model_id INTEGER,
    task_type TEXT NOT NULL,
    cost REAL,
    latency_ms INTEGER,
    quality_score REAL,
    created_at TEXT DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY(user_id) REFERENCES users(id),
    FOREIGN KEY(model_id) REFERENCES llm_models(id)
);

-- ==============================================
-- Sample Data Setup
-- ==============================================

-- Seed Sample Admin User
INSERT INTO users (username, password_hash, role) 
VALUES ('admin', 'admin123', 'ADMIN');

-- Seed Sample LLM Models
INSERT INTO llm_models (name, provider, cost_per_1k_tokens) 
VALUES ('GPT-4o', 'OpenAI', 0.05);

INSERT INTO llm_models (name, provider, cost_per_1k_tokens) 
VALUES ('Claude-3.5-Sonnet', 'Anthropic', 0.03);

-- Seed Explicit Task Suitability Mappings
INSERT INTO model_suitability (model_id, task_type, base_score) 
VALUES (1, 'CODE_GENERATION', 0.90);
INSERT INTO model_suitability (model_id, task_type, base_score) 
VALUES (2, 'CODE_GENERATION', 0.85);
INSERT INTO model_suitability (model_id, task_type, base_score) 
VALUES (2, 'CREATIVE_WRITING', 0.95);
