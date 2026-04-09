import { motion } from 'framer-motion';
import { Helmet } from 'react-helmet-async';
import { Tag } from 'lucide-react';

const container = { hidden: { opacity: 0 }, show: { opacity: 1, transition: { staggerChildren: 0.1 }}};
const item = { hidden: { opacity: 0, y: 20 }, show: { opacity: 1, y: 0 }};

const releases = [
  {
    version: 'v2.1.0',
    date: '2026-04-09',
    type: 'major',
    changes: [
      { kind: 'new', text: 'Intelligence Hub — dedicated menu entry (I) housing Architecture DNA, Security Sentinel, and Market Intelligence in one place' },
      { kind: 'new', text: 'Architecture DNA Engine (ArchitectureService) — scans the workspace, maps all class import dependencies, and persists them as FACT memories in the vault for architectural querying' },
      { kind: 'new', text: 'Security Sentinel (SecuritySentinelService) — regex-based workspace auditor detecting AWS/OpenAI key leaks, hardcoded IPs, and SQL injection patterns across all project files' },
      { kind: 'new', text: 'Market Intelligence (MarketIntelligenceService) — live sync with OpenRouter API to keep local cost_per_1k_tokens values accurate and routing decisions financially sound' },
      { kind: 'new', text: 'Autonomous Recalibration — RoutingEngine.recalibrateScores() adjusts suitability weights using outcome telemetry after every execution cycle' },
      { kind: 'new', text: 'Rate Limit Detection — LlmCallService now detects HTTP 429 (rate limit) and HTTP 400 (context overflow) and surfaces provider-specific actionable messages' },
      { kind: 'new', text: 'TaskType enum expanded with UNIT_TESTING, REASONING, and GENERAL_KNOWLEDGE for finer-grained agentic task decomposition' },
      { kind: 'improved', text: 'Composite scoring weights finalized: Suitability 45%, Historical Quality 25%, Inverse Latency 20%, Inverse Cost 10%' },
      { kind: 'improved', text: 'All dashboard switch statements now call .toUpperCase() enabling fully case-insensitive navigation throughout the CLI' },
      { kind: 'improved', text: 'NexusApp fully modularized — IntelligenceMenu injected as first-class singleton alongside all other menu instances' },
    ]
  },
  {
    version: 'v2.0.0',
    date: '2026-04-05',
    type: 'major',
    changes: [
      { kind: 'new', text: 'Contextd Memory Layer — typed memories (FACT, PREFERENCE, EPISODE, SKILL, CONTRADICTION) with TTL decay and automatic contradiction detection' },
      { kind: 'new', text: 'API Key Vault — multi-provider key storage (OpenAI, Anthropic, Groq, Gemini, OpenRouter). Keys are Base64-encoded, never stored in plain text' },
      { kind: 'new', text: 'Intelligent Routing Engine v2 — 4-signal composite scoring with telemetry feedback and API-key-based actionability filtering' },
      { kind: 'new', text: 'Agentic Task Decomposition — TaskPlannerService splits compound prompts into typed sub-tasks each routed independently for cost efficiency' },
      { kind: 'new', text: 'Financial Intelligence Dashboard — per-model and per-task cost tracking with savings projections' },
      { kind: 'new', text: 'Execution History with task/model/date filters and user-scoped ownership controls' },
      { kind: 'new', text: 'Session Context — open and close coding sessions, auto-write outcome records on session close' },
      { kind: 'new', text: 'Audit Log — every significant system event persisted to SQLite (8 tables total in schema)' },
      { kind: 'new', text: 'Model Discovery — full suitability matrix across all registered providers and task types' },
      { kind: 'new', text: 'Admin panel — user management, model registration, registry overrides, and decommission flows' },
      { kind: 'improved', text: 'Password hashing upgraded to salted SHA-256 with constant-time comparison' },
      { kind: 'improved', text: 'Routing engine now filters models by providers the user actually has stored API keys for' },
      { kind: 'fixed', text: 'Memory prune operation is now user-scoped and no longer purges data belonging to other users' },
    ]
  },
  {
    version: 'v1.0.0',
    date: '2026-03-15',
    type: 'initial',
    changes: [
      { kind: 'new', text: 'Initial release — basic routing engine and user authentication' },
      { kind: 'new', text: 'SQLite local-first database via JDBC' },
      { kind: 'new', text: 'nexus start entry point via .bat wrapper' },
    ]
  }
];

const kindStyle = {
  new:      { color: 'var(--accent)', bg: 'var(--accent-dim)', label: 'new' },
  improved: { color: '#10b981',  bg: 'rgba(16,185,129,0.12)', label: 'improved' },
  fixed:    { color: '#a78bfa',  bg: 'rgba(167,139,250,0.12)', label: 'fixed' },
};

export function ChangelogPage() {
  return (
    <motion.div className="page" initial="hidden" animate="show" variants={container}>
      <Helmet>
        <title>Changelog — Nexus Autopilot</title>
        <meta name="description" content="Full release history and version changelog for Nexus Autopilot." />
      </Helmet>
      <motion.div variants={item}>
        <div className="badge-premium" style={{ marginBottom: '16px' }}>Release History</div>
        <h1 className="page-title">Changelog</h1>
        <p style={{ fontSize: '16px', color: 'var(--text-muted)', marginBottom: '64px' }}>
          All notable changes to Nexus Autopilot, most recent first. Every entry maps directly to a commit or feature branch.
        </p>

        {releases.map(release => (
          <div key={release.version} style={{ marginBottom: 64 }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 16, marginBottom: 28, paddingBottom: 20, borderBottom: '1px solid var(--border)' }}>
              <div style={{
                display: 'flex', alignItems: 'center', gap: 8,
                fontFamily: 'var(--mono)', fontSize: 22, fontWeight: 700, color: 'var(--text)'
              }}>
                <Tag size={20} color="var(--accent)" />
                {release.version}
              </div>
              <span style={{
                fontFamily: 'var(--mono)', fontSize: 13, color: 'var(--text-muted)',
                background: 'rgba(255,255,255,0.04)', border: '1px solid var(--border)',
                padding: '3px 10px', borderRadius: 6
              }}>{release.date}</span>
              {release.type === 'major' && (
                <span style={{
                  fontSize: 11, fontWeight: 750, textTransform: 'uppercase', letterSpacing: '0.1em',
                  color: 'var(--accent)', background: 'var(--accent-dim)',
                  padding: '3px 10px', borderRadius: 99, border: '1px solid rgba(232,116,92,0.2)'
                }}>Major Release</span>
              )}
            </div>
            <div style={{
              borderLeft: '2px solid var(--border)', paddingLeft: 24,
              display: 'flex', flexDirection: 'column', gap: 14
            }}>
              {release.changes.map((c, i) => {
                const style = kindStyle[c.kind] || kindStyle.new;
                return (
                  <div key={i} style={{ display: 'flex', alignItems: 'flex-start', gap: 12 }}>
                    <span style={{
                      flexShrink: 0, fontSize: 10, fontWeight: 750, textTransform: 'uppercase',
                      fontFamily: 'var(--mono)', color: style.color, background: style.bg,
                      padding: '3px 8px', borderRadius: 4, marginTop: 2
                    }}>{style.label}</span>
                    <span style={{ color: 'var(--text-dim)', fontSize: 14, lineHeight: 1.65 }}>{c.text}</span>
                  </div>
                );
              })}
            </div>
          </div>
        ))}
      </motion.div>
    </motion.div>
  );
}
