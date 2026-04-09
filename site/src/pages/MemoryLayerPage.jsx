import { motion } from 'framer-motion';
import { Helmet } from 'react-helmet-async';
import { Brain, Layers, Clock, Zap, ShieldCheck, Activity, Database as DbIcon } from 'lucide-react';
import { CodeBlock, Callout } from '../components/UI';

const container = { hidden: { opacity: 0 }, show: { opacity: 1, transition: { staggerChildren: 0.1 }}};
const item = { hidden: { opacity: 0, y: 15 }, show: { opacity: 1, y: 0 }};

const memoryTypes = [
  { type: 'FACT',          ttl: '365 days',  usage: 'Architectural decisions, stable project rules, tech constraints. High-confidence. Long-lived.' },
  { type: 'PREFERENCE',   ttl: '90 days',   usage: 'Coding style, naming conventions, formatting preferences. Medium-term relevance.' },
  { type: 'EPISODE',      ttl: '30 days',   usage: 'What happened in a specific session. Short-term reference. Expires fast.' },
  { type: 'SKILL',        ttl: '180 days',  usage: 'Learned task patterns, prompt templates, reusable strategies.' },
  { type: 'CONTRADICTION',ttl: '7 days',    usage: 'Conflicting instructions flagged for user resolution. Expires quickly to prevent clutter.' },
];

export function MemoryLayerPage() {
  return (
    <motion.div className="page" initial="hidden" animate="show" variants={container}>
      <Helmet>
        <title>Memory Vault — Nexus Autopilot</title>
        <meta name="description" content="Nexus Memory Vault: local-first typed semantic memory with TTL decay, contradiction detection, and sub-millisecond SQLite recall." />
      </Helmet>

      <motion.div variants={item}>
        <div className="badge-premium">PERSISTENCE LAYER</div>
        <h1 className="page-title">Contextd: The Memory Vault</h1>
        <p className="page-description">
          A local-first typed memory layer on SQLite. Nexus remembers your project's architecture, preferences, and session history across every run — without a cloud service, embedding model, or vector database.
        </p>

        {/* ── Why Memory Matters ───────────────────── */}
        <section className="doc-section">
          <div className="section-header-inline">
            <Brain size={20} color="var(--accent)" />
            <h2>The Problem It Solves</h2>
          </div>
          <p>
            Every LLM session starts cold. Without persistent memory, you repeat the same context in every prompt — your tech stack, your conventions, your architectural constraints. Nexus eliminates this by storing typed memories that survive across sessions and are surfaced as context on recall.
          </p>
          <p style={{ marginTop: '12px' }}>
            Memory is stored in the <code>memories</code> table in <code>nexus.db</code>. Every record has a type, a confidence score, optional tags for indexing, an expiry timestamp, and an access counter. This gives the system all the signals it needs to rank, decay, and prune intelligently.
          </p>
        </section>

        {/* ── Memory Types ─────────────────────────── */}
        <section className="doc-section">
          <div className="section-header-inline">
            <Layers size={20} color="var(--accent)" />
            <h2>Memory Types</h2>
          </div>
          <p>Each memory type has a different TTL and semantic role. The MemoryType enum enforces this contract at the service layer.</p>
          <table className="custom-table" style={{ marginTop: '24px' }}>
            <thead>
              <tr>
                <th>Type</th>
                <th>Default TTL</th>
                <th>Semantic Role</th>
              </tr>
            </thead>
            <tbody>
              {memoryTypes.map(m => (
                <tr key={m.type}>
                  <td><code style={{ fontFamily: 'var(--mono)', fontSize: '12px', color: 'var(--accent)', background: 'var(--accent-dim)', padding: '2px 6px', borderRadius: '4px' }}>{m.type}</code></td>
                  <td style={{ color: 'var(--text-muted)', fontSize: '13px' }}>{m.ttl}</td>
                  <td style={{ fontSize: '13.5px', lineHeight: 1.55 }}>{m.usage}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </section>

        {/* ── Decay Algorithm ──────────────────────── */}
        <section className="doc-section">
          <div className="section-header-inline">
            <Clock size={20} color="var(--accent)" />
            <h2>Temporal Decay Algorithm</h2>
          </div>
          <p>
            Not all memories stay relevant. A decay pass runs on every login. Any memory not accessed in 7+ days has its confidence score reduced by 0.05 per elapsed week. Once confidence drops below <code>0.10</code> or the TTL expires, the memory is eligible for pruning.
          </p>
          <CodeBlock
            lang="java"
            code={`// MemoryService.java — runDecayPass()
for (Memory m : memoryDao.findAllByUser(userId)) {
    long weeksSinceAccess = daysSince(m.getLastAccessedAt()) / 7;
    if (weeksSinceAccess > 0) {
        double newConf = Math.max(0.0, m.getConfidence() - (0.05 * weeksSinceAccess));
        m.setConfidence(newConf);
        memoryDao.update(m);
    }
}

// Prune: confidence < 0.10 OR past expires_at
memoryDao.pruneStaleForUser(userId, 0.10, Instant.now().getEpochSecond());`}
          />
        </section>

        {/* ── Contradiction Detection ───────────────── */}
        <section className="doc-section">
          <div className="section-header-inline">
            <ShieldCheck size={20} color="var(--accent)" />
            <h2>Contradiction Detection</h2>
          </div>
          <p>
            Before storing a new <code>FACT</code> memory, the service checks for existing <code>FACT</code> memories with overlapping tags. If a conflict is found — for example, "Use Tabs" when "Use Spaces" is already stored — the new entry is stored as a <code>CONTRADICTION</code> type with a 7-day TTL, flagging it for user resolution.
          </p>
          <Callout type="warning">
            <strong>Operational note:</strong> Contradictions are surfaced in the Memory Vault menu under a dedicated filter. Review and resolve them before they expire — the older memory will remain dominant until manually updated.
          </Callout>
        </section>

        {/* ── Recall Ranking ───────────────────────── */}
        <section className="doc-section">
          <div className="section-header-inline">
            <Activity size={20} color="var(--accent)" />
            <h2>Recall Ranking</h2>
          </div>
          <p>
            When searching memories, results are ranked using a hybrid score that weights confidence and recency. Higher-confidence, recently accessed memories always surface first.
          </p>
          <div className="algorithm-card">
            <div className="alg-header">
              <DbIcon size={14} />
              <span>RECALL SCORING FORMULA</span>
            </div>
            <div className="alg-grid">
              <div className="alg-item">
                <span className="label">Confidence Weight</span>
                <span className="value">70%</span>
                <div className="bar"><div className="fill" style={{ width: '70%' }}></div></div>
              </div>
              <div className="alg-item">
                <span className="label">Recency Weight</span>
                <span className="value">30%</span>
                <div className="bar"><div className="fill" style={{ width: '30%' }}></div></div>
              </div>
              <div className="alg-item">
                <span className="label">Decay Rate</span>
                <span className="value">-5% per 7 days inactive</span>
                <div className="bar"><div className="fill" style={{ width: '50%' }}></div></div>
              </div>
              <div className="alg-item">
                <span className="label">Prune Threshold</span>
                <span className="value">confidence &lt; 0.10 or TTL expired</span>
                <div className="bar"><div className="fill" style={{ width: '35%' }}></div></div>
              </div>
            </div>
          </div>
        </section>

        {/* ── Pillars ──────────────────────────────── */}
        <section className="doc-section">
          <h2>Design Principles</h2>
          <div className="pillars-grid">
            <div className="pillar-card">
              <ShieldCheck size={18} />
              <h4>Zero Data Leak</h4>
              <p>All memories are stored in the local SQLite file. No cloud sync. No embedding APIs. No external dependency.</p>
            </div>
            <div className="pillar-card">
              <Zap size={18} />
              <h4>Sub-ms Recall</h4>
              <p>SQLite-backed recall avoids network round-trips. Tag-indexed queries return results in under a millisecond on local hardware.</p>
            </div>
            <div className="pillar-card">
              <Activity size={18} />
              <h4>Architecture DNA Store</h4>
              <p>The Architecture DNA Engine uses the Memory Vault to store FACT-type class dependency maps, making the project knowledge base a first-class citizen of the memory system.</p>
            </div>
          </div>
        </section>

        <section className="doc-section">
          <div className="best-practices-grid">
            <Callout type="info">
              <strong>Storage location:</strong> All memories are persisted in the <code>memories</code> table inside <code>nexus.db</code> — the same file as every other Nexus data record.
            </Callout>
            <Callout type="warning">
              <strong>Operational habit:</strong> Run the decay and prune pass weekly via Memory Vault to keep your context high-signal. Stale memories reduce recall quality without being removed automatically until pruned.
            </Callout>
          </div>
        </section>
      </motion.div>
    </motion.div>
  );
}
