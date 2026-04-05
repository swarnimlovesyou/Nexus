# Nexus Autopilot

Nexus is a local-first Java CLI that helps developers run AI-agent workflows deliberately.
It provides model routing recommendations, memory management, session tracking, API key handling,
execution history, cost analytics, and auditable event logs on top of SQLite.

## What Is Implemented

### 1) Intelligent Routing Engine
- Composite scoring per task type: `suitability * 0.35 + quality * 0.30 + cost * 0.20 + latency * 0.15`
- User-aware provider filtering: only models with available user API keys are prioritized
- Explain mode with ranked signal breakdown per candidate model
- What-if budget analysis across multiple price caps

### 2) Memory Vault (Contextd Layer)
- Typed memory storage: `FACT`, `PREFERENCE`, `EPISODE`, `SKILL`, `CONTRADICTION`
- CRUD operations with search, type filters, contradiction view
- Confidence decay pass and stale-memory pruning
- Recall ranking using confidence + recency hybrid scoring

### 3) API Key Vault
- Provider-scoped keys stored in SQLite
- Local XOR + Base64 obfuscation (convenience obfuscation, not enterprise encryption)
- Masked key display in UI
- Ownership checks enforced on destructive operations

### 4) Financial Intelligence
- Actual spend vs optimal spend analysis from recorded outcomes
- Spend breakdown by model
- Savings opportunity estimate based on suitable lower-cost alternatives

### 5) Execution History
- Outcome record create/read/update/delete
- Filters by task type, model id, and date range
- User ownership enforced for update/delete actions

### 6) Audit Log
- Persists key user/system actions (`LOGIN`, `ROUTING_DECISION`, `MEMORY_*`, `API_KEY_*`, `SESSION_*`)
- Role-aware visibility: admins can inspect global logs, users view their own logs

### 7) Model Discovery + Administration
- Model catalog and provider filtering
- Suitability matrix view by task type
- Admin CRUD operations for users/models and suitability score management

### 8) Session Context (DB-backed)
- Start an active coding session for a task with routed model recommendation
- Close session with aggregate tokens + quality
- Auto-persists one `outcome_memories` record at session close
- Session close also writes a memory episode entry for future recall

### 9) Live LLM Test Calls
- Real HTTP call path for OpenAI-compatible providers (`OpenAI`, `Groq`, `OpenRouter`), plus Anthropic and Gemini paths
- If provider call fails (network, key, endpoint, lab restrictions), Nexus falls back to simulation mode and labels output clearly

## Technical Architecture

Layered design (assignment aligned):
1. Presentation Layer (`com.nexus.presentation`)
2. Service Layer (`com.nexus.service`)
3. Data Access Layer / DAO (`com.nexus.dao`)
4. Domain Layer (`com.nexus.domain`)

Core OOP elements include:
- abstract classes (`BaseEntity`, `User`)
- inheritance (`User -> AdminUser/RegularUser`)
- interfaces (`Auditable`, `GenericDao<T>`)
- method overloading (notably in `MemoryService`, `RoutingEngine`)

## Database

SQLite database file: `nexus.db`

Primary tables:
- `users`
- `llm_models`
- `model_suitability`
- `outcome_memories`
- `agent_sessions`
- `api_keys`
- `memories`
- `audit_log`

Schema reference: `schema.sql`

## Build and Run

### Requirements
- Java 17+
- Maven

### Build
```bash
mvn clean package
```

### Enable `nexus` Command (Recommended)
```bash
npm link
```
This installs the local CLI globally on your machine so you can run:
```bash
nexus start
```

### Command Mode (Non-Interactive)
You can run session and finance workflows directly from the terminal:

```bash
# list your sessions
nexus session list --user admin

# start a session with routed model recommendation
nexus session start --user admin --task CODE_GENERATION --note "building auth flow"

# close a session and persist outcome telemetry
nexus session close --user admin --id 12 --input 1400 --output 620 --quality 0.91 --note "shipped MVP"

# analyze spend for last 30 days
nexus finance report --user admin --range 30d
```

If `--password` is not passed, Nexus prompts securely in terminal when supported.

### Run (direct)
```bash
java -jar target/nexus-autopilot-1.0-SNAPSHOT-jar-with-dependencies.jar
```

### Run (Windows helper)
```bash
nexus.bat start
```

### NPM launcher (local wrapper)
The repo includes a Node launcher at `bin/nexus.js` that forwards args to the Java jar.

## Test
```bash
mvn test
```

## Default Credentials
- Admin: `admin / admin123` (seeded on first run when no users exist)

## Current Limitations
- API key storage is obfuscation-oriented (XOR), not hardened secret management
- Real provider calls depend on outbound network and valid provider API keys
- Documentation site includes examples; the Java CLI remains the source of runtime truth
