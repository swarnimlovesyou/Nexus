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

### 8) Interactive Coding Sessions
- Start a multi-turn interactive chat session in the terminal with the routed optimal model
- Interactive slash commands: `/read`, `/write`, `/ls`, `/cost`, `/clear`, `/exit`
- Inject entire files into the conversation context (governed by `context.max_injection_tokens` profile policy)
- Safely extract generated code blocks and save them directly to disk (governed by `policy.allow_file_write` profile policy)
- Automatically tracks accumulated tokens + cost across the multi-turn exchange
- Auto-persists `outcome_memories` and a semantic `EPISODE` memory upon closing the session

### 9) Live LLM Test Calls
- Real HTTP call path for OpenAI-compatible providers (`OpenAI`, `Groq`, `OpenRouter`), plus Anthropic and Gemini paths
- If provider call fails (network, key, endpoint, lab restrictions), Nexus falls back to simulation mode and labels output clearly

### 10) Claurst-Style Compatibility Command Suite
- Slash-prefixed command compatibility in command mode (for example: `/status`, `/mcp`, `/skills`, `/plan`)
- New command groups for compatibility workflows:
	- `mcp`: list/add/remove/restart/connect/disconnect
	- `plugin`: list/install/remove/enable/disable/reload/info
	- `hooks`: list/add/remove
	- `permissions`: list/allow/deny/reset
	- `skills`: list/enable/disable/reload
	- `agents`, `tasks`, `plan`, `ultraplan`, `status`, `stats`, `version`, `update`
- Local registries for compatibility state are stored under `target/nexus-config`:
	- `mcp.db`, `plugins.db`, `hooks.db`

### 11) Session Power Tools
- Added command-mode session lifecycle tools:
	- `session resume`: show latest or selected transcript tail
	- `session rename`: set a human title for session listing
	- `session fork`: create a new session from prior context
	- `session rewind`: checkpoint transcript at a selected turn
	- `session export`: export transcript as markdown/text/json
- Session listing includes title metadata when available

### 12) Dashboard Integration for New Features
- The interactive dashboard now includes two new menu entries:
	- `10` Compatibility Features Hub
	- `11` Session Power Tools
- Each hub includes plain-language guidance so users can understand what each action does before executing it
- Hub actions call the same command-mode implementations, so behavior is consistent between command mode and interactive mode

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

- Command mode accepts both plain and slash-prefixed forms. These are equivalent:
	- `nexus status --user admin`
	- `nexus /status --user admin`

### Nexus Chat Quick Guide
1. Start fresh: nexus chat --user admin
2. Pick task in prompt, or pass --task GENERAL_CHAT
3. Pin provider/model if needed: --provider GROQ --model llama-3.1-8b-instant
4. Type normal messages after You>
5. Use /reset to clear only current in-memory turns
6. Use /exit to finish and save summary + extracted memories
7. Continue from latest summary: nexus chat --user admin --parent-chat latest
8. Continue by id: nexus chat --user admin --parent-chat nx-1234abcd
9. Interactive parent picker: nexus chat --user admin --continue
10. Child sessions save lineage with parent_chat:<id> tag

```bash
# list your sessions
nexus session list --user admin

# start a session with routed model recommendation
nexus session start --user admin --task CODE_GENERATION --note "building auth flow"

# close a session and persist outcome telemetry
nexus session close --user admin --id 12 --input 1400 --output 620 --quality 0.91 --note "shipped MVP"

# analyze spend for last 30 days
nexus finance report --user admin --range 30d

# store long-term project memory (pinned)
nexus memory store --user admin --scope project --content "Use repository pattern for DAOs" --type SKILL --pin

# store global user profile memory (shared across projects)
nexus memory store --user admin --scope global --content "Prefers concise CLI responses" --type PREFERENCE --ttl 3650 --pin

# recall memory from current project scope + global profile fallback
nexus memory recall --user admin --scope project --query "dao pattern"

# persist profile instructions used during LLM calls
nexus profile set --user admin --scope global --key response_tone --value "Concise, technical, no fluff"
nexus profile set --user admin --scope project --key code_style --value "Prefer small pure methods and clear variable names"

# inspect or delete profile settings
nexus profile list --user admin --scope project
nexus profile delete --user admin --scope project --key code_style

# generate code directly into file
nexus codegen run --user admin --task CODE_GENERATION --prompt "Create a Java DAO for project settings" --output src/main/java/com/nexus/dao/ProjectSettingsDao.java

# strict CLI-first codegen mode (prevents prose/fenced markdown writes)
nexus codegen run --user admin --task CODE_GENERATION --prompt "Create ConfigService class" --output src/main/java/com/nexus/service/ConfigService.java --strict-code true --format java

# strict-code is ON by default (disable only if explicitly needed)
nexus codegen run --user admin --task CODE_GENERATION --prompt "Generate quick prototype" --output target/tmp-prototype.txt --strict-code false --format markdown

# pin workspace intent and require confirmation if output drifts
nexus codegen run --user admin --task CODE_GENERATION --prompt "Build React landing page" --output site/src/pages/LandingPage.jsx --pin-intent true --format react-jsx
nexus codegen run --user admin --task CODE_GENERATION --prompt "Generate Rust backend handler" --output src/main/rust/handler.rs --confirm-intent true --format rust

# run a reusable multi-step automation recipe
nexus recipe run --user admin --file recipes/example.recipe

# validate recipes before execution
nexus recipe validate --user admin --file recipes/example.recipe

# optional policy controls (defaults allow)
nexus profile set --user admin --scope global --key policy.allow_file_write --value true
nexus profile set --user admin --scope global --key policy.allow_recipe_run --value true
nexus profile set --user admin --scope global --key policy.allow_external_write --value false

# context budget guardrails for memory/profile injection
nexus profile set --user admin --scope project --key context.max_injection_tokens --value 900
nexus profile set --user admin --scope project --key context.max_memories --value 6

# intent drift controls
nexus profile set --user admin --scope project --key intent.require_confirmation_on_drift --value true
nexus profile set --user admin --scope project --key intent.min_alignment_percent --value 12

# apply policy presets for CLI operating mode
nexus profile preset --user admin --name safe --scope project
nexus profile preset --user admin --name balanced --scope project
nexus profile preset --user admin --name power-user --scope project

# guided profile UX
nexus profile wizard --user admin --scope project --mode balanced
nexus profile doctor --user admin --scope project

# one-command onboarding (wizard + doctor + provider check + smoke + readiness score)
nexus onboard --user admin --scope project --mode balanced --provider GROQ

# one-command onboarding with provider key bootstrap from env
nexus onboard --user admin --scope project --mode balanced --provider GROQ --from-env true

# export onboarding report artifacts (JSON + badge markdown under target/onboard)
nexus onboard --user admin --scope project --mode balanced --provider GROQ

# trust envelope for any planned command
nexus trust evaluate --user admin --command "codegen run --output src/main/java/com/nexus/Foo.java"

# replay timeline with traceability of actions/outcomes
nexus storyboard show --user admin --limit 25

# workflow macros (ship/hotfix/release-notes)
nexus workflow list --user admin
nexus workflow run --user admin --name ship --provider GROQ

# smart command suggestions from history + profile policy
nexus suggest --user admin --prefix code

# policy-aware PR prep with risk and suggested tests
nexus pr prep --user admin --output target/pr/prep.md

# memory timeline with why-used trace hints
nexus memory timeline --user admin --query "architecture" --limit 20

# curated recipe marketplace
nexus recipe marketplace --user admin --action list
nexus recipe marketplace --user admin --action install --name backend-rust

# list and run built-in tools (MCP-style adapters)
nexus tool list --user admin
nexus tool run --user admin --name fs.read --path README.md --maxchars 1500
nexus tool run --user admin --name shell.exec --command "git status --short" --timeoutseconds 10

# compatibility command suite (slash-compatible forms)
nexus /mcp list --user admin --password admin123
nexus /mcp add --user admin --password admin123 --name filesystem --command npx --args "-y @modelcontextprotocol/server-filesystem ."
nexus /plugin list --user admin --password admin123
nexus /plugin install --user admin --password admin123 --name demo/plugin
nexus /hooks list --user admin --password admin123
nexus /permissions list --user admin --password admin123
nexus /skills list --user admin --password admin123
nexus /agents --user admin --password admin123
nexus /tasks list --user admin --password admin123
nexus /plan --user admin --password admin123
nexus /plan off --user admin --password admin123
nexus /status --user admin --password admin123
nexus /stats --user admin --password admin123
nexus /version
nexus /update

# session power tools
nexus session resume --user admin --latest true --tail 10
nexus session rename --user admin --id 12 --title "Payment Retry Investigation"
nexus session fork --user admin --id latest
nexus session rewind --user admin --id 12 --turn 8
nexus session export --user admin --latest true --format markdown

# provider readiness checks (key + live health)
nexus provider list --user admin
nexus provider setup --user admin --provider GROQ --from-env true
nexus provider check --user admin --provider GROQ

# dry-run command policy before executing for real
nexus policy simulate --user admin --command "codegen run --output src/main/java/com/nexus/Foo.java"

# one-command end-to-end CLI smoke test (provider + call + codegen + tool + recipe)
nexus smoke run --user admin --provider GROQ

# interactive dashboard now supports section-style command words (route, memory, keys, models, finance, history, audit, profile, intel)
# interactive dashboard also exposes advanced hubs:
#   10 = Compatibility Features Hub (mcp/plugin/hooks/permissions/skills/agents/tasks/plan/status)
#   11 = Session Power Tools (resume/rename/fork/rewind/export)

# smoke report artifacts are saved to target/smoke/*.json and *.md for CI/history tracking

# command-prefix compatibility (both forms are valid)
nexus onboard --user admin --provider GROQ
nexus command onboard --user admin --provider GROQ

# tool policies
nexus profile set --user admin --scope global --key policy.allow_tool_fs_read --value true
nexus profile set --user admin --scope global --key policy.allow_tool_fs_write --value true
nexus profile set --user admin --scope global --key policy.allow_tool_shell --value false

# fallback tuning for real provider execution
nexus profile set --user admin --scope global --key policy.enable_provider_fallback --value true
nexus profile set --user admin --scope global --key routing.max_fallback_candidates --value 4
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
