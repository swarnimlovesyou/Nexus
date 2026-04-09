import { motion } from 'framer-motion';
import { Helmet } from 'react-helmet-async';
import { DbSchemaVisual, RelationshipMap, ArchitectureStack, ExecutionFlow, ScoringFormula } from '../components/visuals/ArchitectureVisuals';

const container = { hidden: { opacity: 0 }, show: { opacity: 1, transition: { staggerChildren: 0.06 } } };
const item = { hidden: { opacity: 0, y: 12 }, show: { opacity: 1, y: 0, transition: { duration: 0.3 } } };

const SectionHeader = ({ label, title, sub }) => (
  <div style={{ marginBottom: '36px' }}>
    {label && (
      <div style={{
        fontSize: '10px', fontWeight: 850, letterSpacing: '0.2em',
        color: 'var(--accent)', textTransform: 'uppercase', marginBottom: '12px'
      }}>
        {label}
      </div>
    )}
    <h2 style={{ fontSize: '28px', fontWeight: 850, letterSpacing: '-0.03em', color: 'var(--text)', marginBottom: '12px' }}>
      {title}
    </h2>
    {sub && <p style={{ fontSize: '16px', color: 'var(--text-muted)', lineHeight: 1.65, maxWidth: '720px' }}>{sub}</p>}
  </div>
);

const ProseBlock = ({ children }) => (
  <div style={{ color: 'var(--text-dim)', fontSize: '15px', lineHeight: 1.75, maxWidth: '760px' }}>
    {children}
  </div>
);

const Divider = () => (
  <div style={{ borderTop: '1px solid var(--border)', margin: '72px 0' }} />
);

const FeatureRow = ({ title, why, how, result }) => (
  <div style={{
    display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '1px',
    background: 'var(--border)', border: '1px solid var(--border)',
    borderRadius: '12px', overflow: 'hidden', marginBottom: '16px'
  }}>
    {[
      { tag: 'FEATURE', val: title },
      { tag: 'WHY IT EXISTS', val: why },
      { tag: 'HOW IT WORKS', val: how },
    ].map(col => (
      <div key={col.tag} style={{ background: 'var(--bg)', padding: '20px 24px' }}>
        <div style={{ fontSize: '9px', fontWeight: 850, letterSpacing: '0.18em', color: 'var(--accent)', textTransform: 'uppercase', marginBottom: '8px' }}>
          {col.tag}
        </div>
        <div style={{ fontSize: '13.5px', lineHeight: 1.65, color: 'var(--text-dim)' }}>
          {col.tag === 'FEATURE'
            ? <strong style={{ color: 'var(--text)', fontWeight: 750 }}>{col.val}</strong>
            : col.val}
        </div>
      </div>
    ))}
  </div>
);

const InfoTable = ({ rows }) => (
  <table className="custom-table" style={{ marginTop: '24px' }}>
    <thead>
      <tr>{Object.keys(rows[0]).map(h => <th key={h}>{h}</th>)}</tr>
    </thead>
    <tbody>
      {rows.map((row, i) => (
        <tr key={i}>{Object.values(row).map((v, j) => <td key={j}>{v}</td>)}</tr>
      ))}
    </tbody>
  </table>
);

const InlineCode = ({ children }) => (
  <code style={{
    fontFamily: 'var(--mono)', fontSize: '12px',
    background: 'rgba(255,255,255,0.06)', color: 'var(--text)',
    padding: '2px 7px', borderRadius: '4px', border: '1px solid var(--border)'
  }}>
    {children}
  </code>
);

export function SovereignDocsPage() {
  return (
    <motion.div className="page" initial="hidden" animate="show" variants={container}>
      <Helmet>
        <title>System Architecture — Nexus</title>
      </Helmet>

      {/* ─── Hero ─────────────────────────────────────────────────────────── */}
      <motion.div variants={item}>
        <div style={{ display: 'inline-flex', alignItems: 'center', gap: '6px', fontSize: '11px', fontWeight: 750, letterSpacing: '0.1em', textTransform: 'uppercase', color: 'var(--accent)', background: 'var(--accent-dim)', border: '1px solid rgba(232,116,92,0.12)', padding: '2px 12px', borderRadius: '99px', marginBottom: '20px' }}>
          Full System Architecture
        </div>
        <h1 style={{ fontSize: '46px', fontWeight: 850, letterSpacing: '-0.04em', color: 'var(--text)', lineHeight: 1.05, marginBottom: '20px' }}>
          Nexus Enterprise Autopilot<br />
          <span style={{ color: 'var(--accent)', textShadow: '0 0 40px rgba(232,116,92,0.25)' }}>Architecture Brief</span>
        </h1>
        <p style={{ fontSize: '17px', color: 'var(--text-muted)', lineHeight: 1.65, maxWidth: '700px', marginBottom: '40px' }}>
          This document is the ground truth for every architectural decision, design principle, and feature
          implementation inside the Nexus platform. It covers what every component does, why it was built,
          how it executes at runtime, and how it fits into the larger agentic system.
        </p>
      </motion.div>

      {/* ─── 1. What is Nexus ─────────────────────────────────────────────── */}
      <motion.div variants={item}>
        <Divider />
        <SectionHeader
          label="Section 01"
          title="What Nexus Actually Is"
          sub="Nexus is not a chatbot wrapper. It is a local-first, agentic command-line operating system that unifies access to every major LLM provider under a single, self-optimizing intelligence layer."
        />
        <ProseBlock>
          <p style={{ marginBottom: '16px' }}>
            The core problem Nexus solves is <strong>decision fatigue and financial waste</strong>. Developers
            who work with multiple AI providers — OpenAI, Anthropic, Groq, Google Gemini — must manually
            decide which model to use for each task, track costs themselves, and rotate API keys by hand.
            This creates cognitive overhead and consistently leads to overspending on models that are
            overqualified for simple tasks.
          </p>
          <p style={{ marginBottom: '16px' }}>
            Nexus replaces that manual decision loop with an autonomous routing engine that scores every
            available model in real time based on suitability, historical quality, response latency, and
            cost per token. It then executes the optimal call — automatically.
          </p>
          <p>
            Beyond routing, Nexus maintains a persistent, user-scoped memory vault that survives across
            sessions. It tracks financial spend per session, maintains an audit trail of every action, and
            runs proactive security scans on the local file system. The sum of these parts is an OS, not
            just a tool.
          </p>
        </ProseBlock>
      </motion.div>

      {/* ─── 2. Architecture ──────────────────────────────────────────────── */}
      <motion.div variants={item}>
        <Divider />
        <SectionHeader
          label="Section 02"
          title="The Layered Architecture"
          sub="Nexus is structured in four strict, decoupled layers. Each layer has a single responsibility. No layer reaches down through another to access state directly."
        />
        <ArchitectureStack />
        <InfoTable rows={[
          { Layer: 'Presentation', Package: 'com.nexus.presentation', Role: 'All user-facing menus, prompts, and output rendering. Zero business logic.' },
          { Layer: 'Service', Package: 'com.nexus.service', Role: 'All intelligence: routing decisions, memory management, security audits, market sync.' },
          { Layer: 'Domain', Package: 'com.nexus.domain', Role: 'Pure data models (LlmModel, Memory, AgentSession). No dependencies on DB or HTTP.' },
          { Layer: 'Data Access', Package: 'com.nexus.dao', Role: 'All SQLite reads/writes. Uses GenericDao<T> for type-safe, reusable CRUD patterns.' },
        ]} />
        <div style={{ marginTop: '32px' }}>
          <ProseBlock>
            <p>
              The central dependency container is <InlineCode>MenuContext</InlineCode>, a lightweight context
              object passed into every menu class. This avoids God-Class anti-patterns and makes every
              component independently testable. Any menu or service can be instantiated in isolation.
            </p>
          </ProseBlock>
        </div>
      </motion.div>

      {/* ─── 3. File Structure ────────────────────────────────────────────── */}
      <motion.div variants={item}>
        <Divider />
        <SectionHeader
          label="Section 03"
          title="Complete File Structure"
          sub="Every file in the Java source tree has a deliberate role. The following maps every class to its function."
        />
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
          {[
            {
              title: 'presentation/',
              files: [
                { name: 'NexusApp.java', role: 'Entry point. Manages auth loop, seeds DB with default models on first run, routes to all menus.' },
                { name: 'NexusCommandRunner.java', role: 'Parses CLI flags (nexus session, nexus finance, nexus health) for non-interactive use.' },
                { name: 'RoutingMenu.java', role: 'The primary work surface. Handles prompt entry, task decomposition, and LLM execution.' },
                { name: 'IntelligenceMenu.java', role: 'God-Mode hub. Exposes Architecture DNA scan, Security Sentinel, and Market Rate sync.' },
                { name: 'MemoryMenu.java', role: 'CRUD UI for the memory vault. Supports search, tag filtering, contradiction review, and prune.' },
                { name: 'AdminMenu.java', role: 'System-level access for ADMIN role. User management, model registry, and audit log.' },
                { name: 'FinanceMenu.java', role: 'Financial dashboard. Per-user spend, per-model breakdown, ROI heatmap.' },
                { name: 'ApiKeyMenu.java', role: 'Secure key storage. Keys are Base64-encoded with masked display. Never stored in plain text.' },
                { name: 'AuditMenu.java', role: 'Audit log browser. Streams every system event for compliance review.' },
                { name: 'HistoryMenu.java', role: 'Session history viewer. Lists all past agent sessions with token count and cost summaries.' },
                { name: 'ModelMenu.java', role: 'Model registry browser. CRUD for LLM models and their suitability scores.' },
                { name: 'MenuContext.java', role: 'Shared context container. All service and DAO instances are injected here once.' },
              ]
            },
            {
              title: 'service/',
              files: [
                { name: 'RoutingEngine.java', role: 'Composite scoring engine. Weighs Suitability (45%), Quality (25%), Latency (20%), Cost (10%) per model.' },
                { name: 'TaskPlannerService.java', role: 'Keyword-heuristic decomposer. Splits compound prompts into typed atomic sub-tasks.' },
                { name: 'LlmCallService.java', role: 'HTTP protocol layer. Handles provider-specific API formats, detects 429 rate limits and 400 context errors.' },
                { name: 'MemoryService.java', role: 'Semantic memory lifecycle: store, recall, decay, prune, and contradiction detection.' },
                { name: 'ArchitectureService.java', role: 'Java file scanner. Maps class dependencies and persists them as FACT memories in the vault.' },
                { name: 'SecuritySentinelService.java', role: 'Regex-based auditor. Scans for AWS/OpenAI key leaks, hardcoded IPs, and SQL injection patterns.' },
                { name: 'MarketIntelligenceService.java', role: 'Live sync layer. Fetches current model pricing from OpenRouter to keep the registry accurate.' },
                { name: 'ApiKeyService.java', role: 'Vault manager. Encodes/decodes keys and enforces per-user, per-provider scoping.' },
                { name: 'UserService.java', role: 'Auth and user management. Bcrypt-equivalent hashing, role enforcement, and account updates.' },
                { name: 'SessionService.java', role: 'Session lifecycle. Opens, closes, and calculates the final cost of each agent session.' },
                { name: 'ExportService.java', role: 'Data export. Serializes session and memory data to structured output formats.' },
              ]
            },
            {
              title: 'dao/',
              files: [
                { name: 'DbConnectionManager.java', role: 'SQLite singleton. Initialises all 8 schema tables, runs migration pass on startup. Provides thread-safe withTransaction().' },
                { name: 'GenericDao.java', role: 'Abstract CRUD base. All DAOs extend this to get type-safe create/read/update/delete for free.' },
                { name: 'LlmModelDao.java', role: 'Reads/writes model metadata: name, provider, cost. Primary source for the routing engine.' },
                { name: 'MemoryDao.java', role: 'Memory persistence with full-text search on content and tags, TTL expiry, and access tracking.' },
                { name: 'OutcomeMemoryDao.java', role: 'Telemetry vault. Stores post-execution quality score, latency, and cost for each model call.' },
                { name: 'SessionDao.java', role: 'Agent session records including token counts, total cost, status, and model reference.' },
                { name: 'SuitabilityDao.java', role: 'Per-model suitability scores by TaskType. The foundation of the routing engine\'s scoring.' },
                { name: 'ApiKeyDao.java', role: 'Encrypted key storage. Keys are masked on retrieval, decoded only during HTTP execution.' },
                { name: 'AuditLogDao.java', role: 'Append-only audit log. Every system action is written here for security and compliance.' },
                { name: 'UserDao.java', role: 'User records with hashed passwords and role assignments.' },
                { name: 'DbTimeCodec.java', role: 'Handles Unix epoch to LocalDateTime conversion for legacy data migration.' },
              ]
            },
            {
              title: 'domain/ & exception/',
              files: [
                { name: 'LlmModel.java', role: 'Represents a single AI model record: id, name, provider, cost_per_1k_tokens.' },
                { name: 'Memory.java', role: 'Context unit: content, tags, type, confidence (0.0–1.0), expiry, and access count.' },
                { name: 'AgentSession.java', role: 'A single AI task lifecycle: input/output tokens, total cost, model used, quality score.' },
                { name: 'User.java / AdminUser.java / RegularUser.java', role: 'Role-polymorphic user model. Admin gets system menu access; Regular gets dev tools only.' },
                { name: 'TaskType.java', role: 'Enum defining all valid task categories: CODE_GENERATION, SUMMARIZATION, REASONING, etc.' },
                { name: 'Provider.java', role: 'Enum mapping provider names to their base API URLs and display identifiers.' },
                { name: 'MemoryType.java', role: 'Enum defining memory lifecycle: FACT (365d), PREFERENCE (90d), EPISODE (30d), SKILL (180d), CONTRADICTION (7d).' },
                { name: 'NexusException.java', role: 'Base unchecked exception for all domain and service errors.' },
                { name: 'DaoException.java', role: 'Wraps all SQLite errors. Ensures database failures are handled gracefully at the service layer.' },
                { name: 'ValidationException.java', role: 'Thrown for invalid inputs. Surfaced to users as readable error messages.' },
              ]
            },
          ].map(group => (
            <div key={group.title} style={{ background: 'rgba(255,255,255,0.015)', border: '1px solid var(--border)', borderRadius: '12px', overflow: 'hidden' }}>
              <div style={{ background: 'var(--bg-surface)', padding: '12px 20px', borderBottom: '1px solid var(--border)' }}>
                <span style={{ fontFamily: 'var(--mono)', fontSize: '12px', fontWeight: 750, color: 'var(--accent)' }}>{group.title}</span>
              </div>
              <div style={{ padding: '4px 0' }}>
                {group.files.map(f => (
                  <div key={f.name} style={{ padding: '12px 20px', borderBottom: '1px solid rgba(255,255,255,0.03)', display: 'flex', flexDirection: 'column', gap: '4px' }}>
                    <span style={{ fontFamily: 'var(--mono)', fontSize: '12px', color: 'var(--text)', fontWeight: 650 }}>{f.name}</span>
                    <span style={{ fontSize: '12.5px', color: 'var(--text-muted)', lineHeight: 1.5 }}>{f.role}</span>
                  </div>
                ))}
              </div>
            </div>
          ))}
        </div>
      </motion.div>

      {/* ─── 4. Database Schema ───────────────────────────────────────────── */}
      <motion.div variants={item}>
        <Divider />
        <SectionHeader
          label="Section 04"
          title="Database Schema"
          sub="All data lives in a local SQLite file (nexus.db). The schema is initialized on first boot and migrated automatically on upgrade. Eight tables cover the full system state."
        />
        <DbSchemaVisual />
        <RelationshipMap />
        <InfoTable rows={[
          { Table: 'users', 'Primary Keys': 'id', 'Foreign Relations': 'Referenced by all other tables via user_id', Purpose: 'Stores credentials (hashed), role, and creation timestamp.' },
          { Table: 'llm_models', 'Primary Keys': 'id', 'Foreign Relations': 'Referenced by agent_sessions, model_suitability, outcome_memories', Purpose: 'The global registry of all known AI models with their provider and cost.' },
          { Table: 'model_suitability', 'Primary Keys': 'id', 'Foreign Relations': 'model_id -> llm_models', Purpose: 'Maps each model to a task type with a base suitability score (0.0–1.0).' },
          { Table: 'memories', 'Primary Keys': 'id', 'Foreign Relations': 'user_id -> users', Purpose: 'Long-term user context. Includes confidence decay, TTL expiry, and full-text searchable content.' },
          { Table: 'agent_sessions', 'Primary Keys': 'id', 'Foreign Relations': 'user_id, model_id', Purpose: 'Full audit of every LLM interaction: tokens, cost, quality score, task type, duration.' },
          { Table: 'outcome_memories', 'Primary Keys': 'id', 'Foreign Relations': 'user_id, model_id', Purpose: 'Telemetry records written after each call. Feeds the autonomous recalibration engine.' },
          { Table: 'api_keys', 'Primary Keys': 'id', 'Foreign Relations': 'user_id -> users', Purpose: 'Per-user, per-provider encrypted key storage. Plain-text keys are never persisted.' },
          { Table: 'audit_log', 'Primary Keys': 'id', 'Foreign Relations': 'user_id -> users', Purpose: 'Append-only log of every system event: logins, memory writes, security findings, errors.' },
        ]} />
      </motion.div>

      {/* ─── 5. The Agentic Flow ─────────────────────────────────────────── */}
      <motion.div variants={item}>
        <Divider />
        <SectionHeader
          label="Section 05"
          title="The Agentic Execution Loop"
          sub="When a user submits a prompt, it is not simply forwarded to an API. It moves through a five-stage autonomous pipeline that plans, routes, executes, audits, and learns."
        />
        <ExecutionFlow />
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr 1fr 1fr', gap: '1px', background: 'var(--border)', border: '1px solid var(--border)', borderRadius: '12px', overflow: 'hidden', marginTop: '32px' }}>
          {[
            { step: '01', label: 'Ingest', detail: 'RoutingMenu receives the raw prompt string from the user terminal input.' },
            { step: '02', label: 'Plan', detail: 'TaskPlannerService decomposes the prompt into one or more typed TaskType sub-tasks using keyword heuristics.' },
            { step: '03', label: 'Route', detail: 'RoutingEngine scores all models with accessible API keys. The highest composite score wins.' },
            { step: '04', label: 'Execute', detail: 'LlmCallService sends the formatted request to the provider. Detects 429 rate limits and 400 context errors.' },
            { step: '05', label: 'Learn', detail: 'OutcomeMemoryDao writes latency, cost, and quality data. recalibrateScores() adjusts weights for next time.' },
          ].map(s => (
            <div key={s.step} style={{ background: 'var(--bg)', padding: '20px' }}>
              <div style={{ fontSize: '10px', fontWeight: 850, color: 'var(--accent)', letterSpacing: '0.15em', marginBottom: '6px' }}>STEP {s.step}</div>
              <div style={{ fontSize: '14px', fontWeight: 850, color: 'var(--text)', marginBottom: '8px' }}>{s.label}</div>
              <div style={{ fontSize: '12px', color: 'var(--text-muted)', lineHeight: 1.6 }}>{s.detail}</div>
            </div>
          ))}
        </div>
      </motion.div>

      {/* ─── 6. Feature Deep Dives ────────────────────────────────────────── */}
      <motion.div variants={item}>
        <Divider />
        <SectionHeader
          label="Section 06"
          title="Feature Deep Dives"
          sub="Every major feature is documented here — what it does, why it was built, how it is implemented, and what the expected outcome is."
        />

        {/* Sub-sections */}
        {[
          {
            id: 'routing',
            title: '6.1  Intelligent Routing Engine',
            why: 'Developers waste money and time picking models manually. A prompt that needs only summarization should never be sent to GPT-4o at $5/1M tokens when Llama-3 on Groq costs $0.10 and returns in 300ms.',
            how: [
              'The engine loads all models from the llm_models table that have a matching API key in api_keys.',
              'For each model, it retrieves base suitability scores from model_suitability for the current TaskType.',
              'It queries outcome_memories for the user\'s historical quality scores and latency data for that model.',
              'A composite score is calculated: Suitability × 0.45 + Quality × 0.25 + InverseLatency × 0.20 + InverseCost × 0.10.',
              'The model with the highest composite score is selected. If no model has telemetry data, suitability alone decides.',
            ],
            result: 'The optimal model for the task type is returned, with a reasoning breakdown available on request.',
            extra: <ScoringFormula />
          },
          {
            id: 'memory',
            title: '6.2  Memory Vault',
            why: 'LLM context windows are ephemeral. Every session starts cold with no memory of your project conventions, coding preferences, or past decisions. The Memory Vault gives Nexus persistent, user-scoped long-term memory that survives across all sessions.',
            how: [
              'Memories are stored in the memories table with a type (FACT, PREFERENCE, EPISODE, SKILL, CONTRADICTION), tags for indexing, confidence score (0.0–1.0), and a TTL expiry date.',
              'Recall uses a hybrid scoring model: keyword match on content and tags, weighted by confidence × recency factor.',
              'On every login, a decay pass runs automatically. Any memory not accessed in 7+ days has its confidence reduced by 0.05 per week.',
              'Before storing a new FACT, the engine checks for existing FACTs with the same tag. If a conflict exists, the new entry is stored as a CONTRADICTION type for user resolution.',
              'Expired memories (past their TTL date) and those below 0.1 confidence are pruned via the prune command.',
            ],
            result: 'Nexus accumulates project context over time, reducing prompt size overhead and ensuring consistent, context-aware responses.',
            extra: (
              <InfoTable rows={[
                { Type: 'FACT', 'Default TTL': '365 days', 'Use Case': 'Architectural decisions, stable project rules.' },
                { Type: 'PREFERENCE', 'Default TTL': '90 days', 'Use Case': 'Coding style, language preferences.' },
                { Type: 'EPISODE', 'Default TTL': '30 days', 'Use Case': 'What happened in a specific session.' },
                { Type: 'SKILL', 'Default TTL': '180 days', 'Use Case': 'Learned task patterns and prompt templates.' },
                { Type: 'CONTRADICTION', 'Default TTL': '7 days', 'Use Case': 'Conflicting instructions that need resolution.' },
              ]} />
            )
          },
          {
            id: 'security',
            title: '6.3  Security Sentinel',
            why: 'Developers frequently commit API keys, hardcoded IPs, and SQL injection patterns to version control by accident. By the time a secret is pushed, the damage is done. The Sentinel acts before the commit.',
            how: [
              'On demand (via Intelligence Hub), SecuritySentinelService walks the entire current directory tree recursively.',
              'Every file is scanned against a library of regex patterns covering AWS keys (AKIA prefix), OpenAI keys (sk- prefix), Google keys, generic secrets ("password=" patterns), hardcoded IPs, and SQL injection signatures.',
              'Findings are structured as SecurityFinding records (type, file, line, severity) and returned to the UI for display.',
              'Every scan result is written to the audit_log table with the SECURITY_SCAN action and CRITICAL severity for key leaks.',
            ],
            result: 'A full report of all security risks in the local workspace, categorized by severity and logged permanently for compliance review.',
            extra: (
              <InfoTable rows={[
                { Pattern: 'AWS Access Key', Regex: 'AKIA[0-9A-Z]{16}', Severity: 'CRITICAL' },
                { Pattern: 'OpenAI API Key', Regex: 'sk-[a-zA-Z0-9]{32,}', Severity: 'CRITICAL' },
                { Pattern: 'Hardcoded IP', Regex: '\\b\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\b', Severity: 'HIGH' },
                { Pattern: 'SQL Injection Risk', Regex: "' OR '1'='1' | --", Severity: 'HIGH' },
                { Pattern: 'Generic Secret', Regex: 'password\\s*=\\s*["\'][^\'"]+["\']', Severity: 'MEDIUM' },
              ]} />
            )
          },
          {
            id: 'architecture-dna',
            title: '6.4  Architecture DNA Engine',
            why: 'As projects grow, developers lose track of which classes depend on what. Refactoring a DAO unexpectedly breaks a service that imported it without anyone noticing until runtime. The DNA engine creates a persistent, queryable dependency graph.',
            how: [
              'ArchitectureService.buildContextGraph() walks the target directory recursively for all .java files.',
              'Each file is parsed for its declared class name and its import statements.',
              'A summary string is formed: "Class: X | Location: file.java | Dependencies: A, B, C".',
              'This summary is stored as a FACT memory in the vault with the tag ARCH_MAP,ClassName.',
              'After all files are processed, a global graph summary is stored as a GLOBAL_DNA,GRAPH memory.',
            ],
            result: 'The full project dependency graph is live-queryable from the Memory Vault. Developers can search "what depends on UserDao" and get an immediate answer from the vault.'
          },
          {
            id: 'market',
            title: '6.5  Market Intelligence (Web Grounding)',
            why: 'LLM pricing changes frequently. A routing decision made with 3-month-old cost data is potentially wrong and leads to overspending. The Market Intelligence service keeps the local registry anchored to live global data.',
            how: [
              'MarketIntelligenceService.syncMarketRates() sends a GET request to https://openrouter.ai/api/v1/models.',
              'The response JSON is scanned for each model in the local llm_models registry.',
              'A regex extractor parses the prompt price field from the JSON blob for each matching model ID.',
              'If the live price differs from the local record by more than a threshold, the local cost_per_1k_tokens is updated via modelDao.update().',
              'The count of updated models is returned to the UI for display.',
            ],
            result: 'The routing engine\'s cost calculations always reflect current market rates, preventing routing decisions based on stale data.'
          },
          {
            id: 'sessions',
            title: '6.6  Session Tracker & Financial Intelligence',
            why: 'AI spend is invisible without instrumentation. Developers have no idea how much a month of LLM usage actually costs until they receive a bill. Nexus provides line-item financial visibility at the session level.',
            how: [
              'When a task executes, SessionService opens an AgentSession record in agent_sessions.',
              'Token counts (input and output) are passed back from LlmCallService after each completion.',
              'Cost is calculated as (input_tokens + output_tokens) / 1000 × cost_per_1k_tokens for the selected model.',
              'The session is closed with total_cost, quality_score, and ended_at written to the record.',
              'FinanceMenu aggregates these records by user and model, producing per-day, per-model, and per-task-type spend breakdowns.',
            ],
            result: 'A full financial intelligence dashboard showing ROI per model, spend trends over time, and projected monthly costs based on current usage patterns.'
          },
          {
            id: 'task-decomposition',
            title: '6.7  Agentic Task Decomposition',
            why: 'Complex requests like "Build a User entity, create its DAO, write tests, and document it" contain four distinct tasks that are optimally served by different models. Treating them as one call wastes money and reduces quality.',
            how: [
              'TaskPlannerService.plan() receives the raw user prompt.',
              'It scans the prompt for keywords associated with each TaskType: "test" maps to UNIT_TESTING, "explain" maps to SUMMARIZATION, "build"/"create" maps to CODE_GENERATION, etc.',
              'The method returns a List<TaskType> representing the ordered execution steps.',
              'RoutingMenu\'s decompose-and-execute loop iterates through this list, calling the routing engine separately for each TaskType.',
              'Each sub-task produces its own session record, enabling granular cost tracking.',
            ],
            result: 'A complex multi-step prompt is automatically broken into specialized calls, each routed to the model best suited for that specific sub-task.'
          },
          {
            id: 'audit',
            title: '6.8  Audit Log',
            why: 'In any system handling credentials, API keys, and external service calls, a tamper-evident audit trail is a non-negotiable security requirement. The audit log provides full traceability of every system event.',
            how: [
              'AuditLogDao.create() is called at every security-relevant event: LOGIN, LOGIN_FAIL, MEMORY_STORE, MEMORY_FORGET, KEY_ADD, KEY_DELETE, SECURITY_SCAN, SESSION_OPEN, SESSION_CLOSE.',
              'Each record is append-only and includes user_id, action, details (key-value string), outcome (SUCCESS/FAILURE), and created_at timestamp.',
              'AuditMenu provides a paginated browser for reviewing these records, with filtering by action type.',
              'Failed login attempts are logged even without a valid user_id, ensuring that pre-authentication attacks are captured.',
            ],
            result: 'A complete, chronological record of all system events that can be used for security review, debugging, and compliance documentation.'
          },
        ].map(feat => (
          <div key={feat.id} style={{ marginBottom: '64px' }}>
            <h3 style={{ fontSize: '22px', fontWeight: 850, color: 'var(--text)', letterSpacing: '-0.02em', marginBottom: '8px', paddingBottom: '16px', borderBottom: '1px solid var(--border)' }}>
              {feat.title}
            </h3>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px', marginTop: '24px' }}>
              <div>
                <div style={{ fontSize: '10px', fontWeight: 850, letterSpacing: '0.18em', color: 'var(--accent)', marginBottom: '8px' }}>WHY IT EXISTS</div>
                <p style={{ fontSize: '14px', color: 'var(--text-dim)', lineHeight: 1.7 }}>{feat.why}</p>
              </div>
              <div>
                <div style={{ fontSize: '10px', fontWeight: 850, letterSpacing: '0.18em', color: 'var(--accent)', marginBottom: '8px' }}>HOW IT IS IMPLEMENTED</div>
                <ol style={{ paddingLeft: '16px', display: 'flex', flexDirection: 'column', gap: '6px' }}>
                  {feat.how.map((step, i) => (
                    <li key={i} style={{ fontSize: '13.5px', color: 'var(--text-muted)', lineHeight: 1.6 }}>{step}</li>
                  ))}
                </ol>
              </div>
            </div>
            <div style={{ marginTop: '16px', padding: '14px 18px', background: 'rgba(232,116,92,0.05)', border: '1px solid rgba(232,116,92,0.15)', borderRadius: '8px' }}>
              <span style={{ fontSize: '10px', fontWeight: 850, letterSpacing: '0.15em', color: 'var(--accent)', textTransform: 'uppercase' }}>Result: </span>
              <span style={{ fontSize: '13.5px', color: 'var(--text-dim)' }}>{feat.result}</span>
            </div>
            {feat.extra && feat.extra}
          </div>
        ))}
      </motion.div>

      {/* ─── 7. Tech Stack ────────────────────────────────────────────────── */}
      <motion.div variants={item}>
        <Divider />
        <SectionHeader
          label="Section 07"
          title="Technology Stack"
          sub="Every technology choice in Nexus was made deliberately. This section documents the rationale behind each dependency."
        />
        <InfoTable rows={[
          { Technology: 'Java 17 LTS', Layer: 'Runtime', 'Why Chosen': 'Long-term support, sealed classes for domain safety, records for concise DTOs, pattern matching for cleaner routing logic.' },
          { Technology: 'SQLite 3.45 (xerial)', Layer: 'Persistence', 'Why Chosen': 'Zero-configuration local database. No network, no service, no credentials required. Compatible with read-heavy, single-user workloads.' },
          { Technology: 'Maven 3.9', Layer: 'Build', 'Why Chosen': 'Deterministic dependency resolution. maven-assembly-plugin produces a shaded fat JAR with all dependencies embedded for single-command distribution.' },
          { Technology: 'Java HttpClient (JDK 11+)', Layer: 'HTTP', 'Why Chosen': 'Included in the JDK. No additional dependency. Supports async calls, HTTP/2, and proper timeout management.' },
          { Technology: 'ANSI-256 (TerminalUtils)', Layer: 'UI', 'Why Chosen': 'Custom-built rendering layer for box-drawing, tables, progress bars, and spinners. No framework dependency, full control over terminal output.' },
          { Technology: 'React + Vite', Layer: 'Frontend Docs', 'Why Chosen': 'Fast hot-reload development experience. Minimal config. Outputs static assets deployable to any CDN or GitHub Pages.' },
          { Technology: 'JUnit 5', Layer: 'Testing', 'Why Chosen': 'Modern parameterized test support. Nested test classes for organized coverage of scoring logic and DAO operations.' },
        ]} />
      </motion.div>

      {/* ─── 8. Role System ──────────────────────────────────────────────── */}
      <motion.div variants={item}>
        <Divider />
        <SectionHeader
          label="Section 08"
          title="Role-Based Access Control"
          sub="Nexus implements a two-tier access model. Role determines which menus are visible and which operations are permitted."
        />
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
          {[
            {
              role: 'DEVELOPER', color: '#3b82f6',
              access: ['Routing Engine — full prompt and model execution', 'Memory Vault — store, recall, update, delete own memories', 'API Key Vault — manage own provider keys', 'Financial Dashboard — view own session spend', 'Execution History — view own session records', 'Account Settings — change own password', 'Intelligence Hub — architecture scan, security audit, market sync']
            },
            {
              role: 'ADMIN', color: 'var(--accent)',
              access: ['All DEVELOPER capabilities', 'Admin Control Panel — user management (create, disable, promote)', 'Model Registry — add, edit, and delete LLM models and suitability scores', 'Audit Log — view all user events across the system', 'System Diagnostics — connectivity health checks across all providers']
            }
          ].map(r => (
            <div key={r.role} style={{ background: 'rgba(255,255,255,0.015)', border: `1px solid ${r.color}40`, borderRadius: '12px', overflow: 'hidden' }}>
              <div style={{ background: `${r.color}10`, padding: '14px 20px', borderBottom: `1px solid ${r.color}30` }}>
                <span style={{ fontSize: '12px', fontWeight: 850, color: r.color, letterSpacing: '0.12em', textTransform: 'uppercase' }}>{r.role}</span>
              </div>
              <ul style={{ listStyle: 'none', padding: '16px 0' }}>
                {r.access.map((a, i) => (
                  <li key={i} style={{ padding: '8px 20px', fontSize: '13px', color: 'var(--text-dim)', display: 'flex', gap: '10px', borderBottom: '1px solid rgba(255,255,255,0.03)' }}>
                    <span style={{ color: r.color, flexShrink: 0 }}>—</span>
                    {a}
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </div>
      </motion.div>

      {/* ─── 9. CLI Commands ───────────────────────────────────────────────── */}
      <motion.div variants={item}>
        <Divider />
        <SectionHeader
          label="Section 09"
          title="CLI Command Reference"
          sub="Nexus supports both interactive mode and headless command-line flags for CI/CD and scripted workflows."
        />
        <InfoTable rows={[
          { Command: 'java -jar nexus.jar', Mode: 'Interactive', Description: 'Launches the full ANSI CLI with authentication and all menus.' },
          { Command: 'java -jar nexus.jar session list --user admin', Mode: 'Headless', Description: 'Lists all agent sessions for the specified user without launching the interactive loop.' },
          { Command: 'java -jar nexus.jar session start --user admin --task CODE_GENERATION', Mode: 'Headless', Description: 'Opens a new agent session for the given task type and routes to the optimal model.' },
          { Command: 'java -jar nexus.jar finance report --user admin --range 30d', Mode: 'Headless', Description: 'Outputs a structured spend report for the past 30 days to stdout.' },
          { Command: 'java -jar nexus.jar health', Mode: 'Headless', Description: 'Pings all configured API endpoints and reports latency and status for each provider.' },
        ]} />
      </motion.div>

      {/* ─── Footer CTA ─────────────────────────────────────────────────────── */}
      <motion.div variants={item}>
        <Divider />
        <div style={{ textAlign: 'center', padding: '48px 0 24px' }}>
          <h3 style={{ fontSize: '22px', fontWeight: 850, color: 'var(--text)', marginBottom: '12px' }}>
            This is the complete picture.
          </h3>
          <p style={{ color: 'var(--text-muted)', fontSize: '15px', marginBottom: '32px' }}>
            Every system, every class, every decision documented in one place.
          </p>
          <div style={{ display: 'flex', gap: '16px', justifyContent: 'center' }}>
            <a href="#/routing" className="btn-glow" style={{ padding: '12px 28px' }}>Explore the Routing Engine</a>
            <a href="#/cli" className="btn-outline">View CLI Reference</a>
          </div>
        </div>
      </motion.div>

    </motion.div>
  );
}
