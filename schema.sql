-- Nexus Autopilot — Complete Database Schema
-- This file reflects the exact schema created at runtime by DbConnectionManager.java

-- ── Users ─────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT UNIQUE NOT NULL,
    password_hash TEXT NOT NULL,       -- Format: Base64(salt):Base64(SHA-256(salt+password))
    role TEXT NOT NULL,                -- 'ADMIN' | 'USER'
    created_at TEXT DEFAULT CURRENT_TIMESTAMP
);

-- ── LLM Models ────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS llm_models (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    provider TEXT NOT NULL,
    cost_per_1k_tokens REAL NOT NULL,
    created_at TEXT DEFAULT CURRENT_TIMESTAMP
);

-- ── Model Task Suitability ────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS model_suitability (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    model_id INTEGER,
    task_type TEXT NOT NULL,           -- e.g. CODE_GENERATION, CREATIVE_WRITING, SUMMARIZATION...
    base_score REAL NOT NULL,          -- 0.0 - 1.0 expert-seeded weight
    created_at TEXT DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY(model_id) REFERENCES llm_models(id)
);

-- ── Execution Outcome Memories ────────────────────────────────────────────────
-- Feeds the routing engine quality/latency signals.
CREATE TABLE IF NOT EXISTS outcome_memories (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER,
    model_id INTEGER,
    task_type TEXT NOT NULL,
    cost REAL,                         -- Actual cost: model.cost_per_1k * (tokens / 1000)
    latency_ms INTEGER,                -- Wall-clock latency in milliseconds
    quality_score REAL,                -- User-rated 0.0 - 1.0
    created_at TEXT DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY(user_id) REFERENCES users(id),
    FOREIGN KEY(model_id) REFERENCES llm_models(id)
);

-- ── Agent Sessions (new) ─────────────────────────────────────────────────────
-- Captures end-to-end coding sessions so outcomes are logged as coherent units.
CREATE TABLE IF NOT EXISTS agent_sessions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    task_type TEXT NOT NULL,
    model_id INTEGER NOT NULL,
    status TEXT NOT NULL DEFAULT 'ACTIVE', -- ACTIVE | CLOSED
    input_tokens INTEGER,
    output_tokens INTEGER,
    total_cost REAL,
    quality_score REAL,
    notes TEXT,
    ended_at TEXT,
    created_at TEXT DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY(user_id) REFERENCES users(id),
    FOREIGN KEY(model_id) REFERENCES llm_models(id)
);

-- ── API Key Vault ─────────────────────────────────────────────────────────────
-- Keys are XOR + Base64 encoded. Not plaintext.
CREATE TABLE IF NOT EXISTS api_keys (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    provider TEXT NOT NULL,            -- Maps to Provider enum (OPENAI, ANTHROPIC, etc.)
    alias TEXT NOT NULL,               -- User-defined label (e.g. "work-key")
    masked_key TEXT NOT NULL,          -- e.g. sk-abcd...ef12
    encoded_key TEXT NOT NULL,         -- XOR(rawKey, 0x4E) then Base64 encoded
    created_at TEXT DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY(user_id) REFERENCES users(id)
);

-- ── Contextd Memory Layer ─────────────────────────────────────────────────────
-- Typed long-term memory with TTL and confidence decay.
CREATE TABLE IF NOT EXISTS memories (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    agent_id TEXT DEFAULT 'default',
    content TEXT NOT NULL,
    tags TEXT,
    type TEXT NOT NULL,                -- FACT | PREFERENCE | EPISODE | SKILL | CONTRADICTION
    confidence REAL DEFAULT 1.0,       -- Decays 5% per week if not accessed
    access_count INTEGER DEFAULT 0,
    last_accessed_at TEXT,
    expires_at TEXT,                   -- NULL = no expiry; calculated from MemoryType.getDefaultTtlDays()
    created_at TEXT DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY(user_id) REFERENCES users(id)
);

-- ── Audit Log ─────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS audit_log (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER,
    action TEXT NOT NULL,              -- e.g. ROUTING_DECISION, MEMORY_STORE, API_KEY_ADD
    details TEXT,
    outcome TEXT DEFAULT 'SUCCESS',    -- SUCCESS | FAILURE
    created_at TEXT DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY(user_id) REFERENCES users(id)
);
