import { motion } from 'framer-motion';
import { Helmet } from 'react-helmet-async';
import { Tag } from 'lucide-react';

const container = { hidden: { opacity: 0 }, show: { opacity: 1, transition: { staggerChildren: 0.1 }}};
const item = { hidden: { opacity: 0, y: 20 }, show: { opacity: 1, y: 0 }};

const releases = [
  {
    version: 'v2.0.0',
    date: '2026-04-05',
    type: 'major',
    changes: [
      { kind: 'new', text: 'Contextd Memory Layer — typed memories (FACT, PREFERENCE, EPISODE, SKILL, CONTRADICTION) with TTL decay' },
      { kind: 'new', text: 'API Key Vault — multi-provider key storage (OpenAI, Anthropic, Groq, Gemini, OpenRouter)' },
      { kind: 'new', text: 'Intelligent Routing Engine v2 — 4-signal composite scoring with What-If budget analysis' },
      { kind: 'new', text: 'Financial Intelligence Dashboard — per-model and per-task cost tracking with savings analysis' },
      { kind: 'new', text: 'Execution History with time-window filtering (all time / 7 days / 30 days)' },
      { kind: 'new', text: 'Audit Log — every significant action persisted to SQLite' },
      { kind: 'new', text: 'Model Discovery — suitability matrix across all registered providers' },
      { kind: 'new', text: 'Admin panel — user management, model registration, decommission flows' },
      { kind: 'improved', text: 'Password hashing upgraded to salted SHA-256 (constant-time comparison)' },
      { kind: 'improved', text: 'All numeric inputs now have bounds checking and user-friendly error messages' },
      { kind: 'improved', text: 'Cost recording uses actual token count instead of arbitrary multiplier' },
      { kind: 'improved', text: 'Routing engine now filters by providers the user has API keys for' },
      { kind: 'improved', text: 'Memory prune operation is now user-scoped (no longer global)' },
      { kind: 'fixed', text: 'schema.sql now reflects all 7 database tables correctly' },
    ]
  },
  {
    version: 'v1.0.0',
    date: '2026-03-15',
    type: 'initial',
    changes: [
      { kind: 'new', text: 'Initial release — basic routing engine and user authentication' },
      { kind: 'new', text: 'SQLite local-first database via JDBC' },
      { kind: 'new', text: '`nexus start` entry point via .bat wrapper' },
    ]
  }
];

const kindStyle = {
  new:      { color: 'var(--accent)', bg: 'var(--accent-dim)', label: 'new' },
  improved: { color: 'var(--green)',  bg: 'rgba(16,185,129,0.12)', label: 'improved' },
  fixed:    { color: '#a78bfa',       bg: 'rgba(167,139,250,0.12)', label: 'fixed' },
};

export function ChangelogPage() {
  return (
    <motion.div className="page" initial="hidden" animate="show" variants={container}>
      <Helmet>
        <title>Changelog — Nexus Autopilot</title>
        <meta name="description" content="Full release history and version changelog for Nexus Autopilot." />
      </Helmet>
      <motion.div variants={item}>
        <h1>Changelog</h1>
        <p>All notable changes to Nexus Autopilot, most recent first.</p>

        {releases.map(release => (
          <div key={release.version} style={{ marginBottom: 56 }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 16, marginBottom: 24 }}>
              <div style={{
                display: 'flex', alignItems: 'center', gap: 8,
                fontFamily: 'var(--mono)', fontSize: 22, fontWeight: 700, color: 'var(--text)'
              }}>
                <Tag size={20} color="var(--accent)" />
                {release.version}
              </div>
              <span style={{ 
                fontFamily: 'var(--mono)', fontSize: 13, color: 'var(--text-muted)',
                background: 'var(--bg-2)', border: '1px solid var(--border)',
                padding: '3px 10px', borderRadius: 6
              }}>{release.date}</span>
              {release.type === 'major' && (
                <span style={{ 
                  fontSize: 11, fontWeight: 700, textTransform: 'uppercase', letterSpacing: 1,
                  color: 'var(--accent)', background: 'var(--accent-dim)',
                  padding: '3px 10px', borderRadius: 99, border: '1px solid rgba(245,158,11,0.3)'
                }}>Major Release</span>
              )}
            </div>
            <div style={{ 
              borderLeft: '2px solid var(--border)', paddingLeft: 24,
              display: 'flex', flexDirection: 'column', gap: 12
            }}>
              {release.changes.map((c, i) => {
                const style = kindStyle[c.kind] || kindStyle.new;
                return (
                  <div key={i} style={{ display: 'flex', alignItems: 'flex-start', gap: 12 }}>
                    <span style={{
                      flexShrink: 0, fontSize: 11, fontWeight: 700, textTransform: 'uppercase',
                      fontFamily: 'var(--mono)', color: style.color, background: style.bg,
                      padding: '3px 8px', borderRadius: 4, marginTop: 2
                    }}>{style.label}</span>
                    <span style={{ color: 'var(--text-dim)', fontSize: 15, lineHeight: 1.6 }}>{c.text}</span>
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
