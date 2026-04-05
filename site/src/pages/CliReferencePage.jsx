import { motion } from 'framer-motion';
import { Helmet } from 'react-helmet-async';
import { Terminal as TerminalIcon, Search, HelpCircle, Code, Settings2, Command, Terminal as CliIcon, Play, RefreshCw, Archive, Activity } from 'lucide-react';
import { CodeBlock, Callout, Terminal } from '../components/UI';

const container = { hidden: { opacity: 0 }, show: { opacity: 1, transition: { staggerChildren: 0.1 }}};
const item = { hidden: { opacity: 0, y: 15 }, show: { opacity: 1, y: 0 }};

export function CliReferencePage() {
  return (
    <motion.div className="page" initial="hidden" animate="show" variants={container}>
      <Helmet>
        <title>CLI Reference — Nexus Autopilot</title>
      </Helmet>

      <motion.div variants={item}>
        <div className="badge-premium">COMMAND REFERENCE</div>
        <h1 className="page-title">CLI Reference</h1>
        <p className="page-description">
          Nexus is built for engineers who work in the terminal. A single binary for all your agentic orchestration needs.
        </p>

        {/* ── Start Command ───────────────────────── */}
        <section className="doc-section">
           <div className="section-header-inline">
              <Play size={20} color="var(--accent)" />
              <h2>Initialization & Runtime</h2>
           </div>
           <p>The <code>start</code> command initializes the local Nexus server and enters the interactive orchestration loop.</p>
           <CodeBlock lang="bash" code="nexus start --engine=verity --port=5173 --decay=auto" />
           
           <div className="table-responsive" style={{ marginTop: '24px' }}>
              <table className="custom-table" style={{ width: '100%', borderCollapse: 'collapse' }}>
                <thead>
                  <tr style={{ textAlign: 'left', borderBottom: '1px solid var(--border)' }}>
                    <th style={{ padding: '12px', fontSize: '11px', color: 'var(--text-muted)' }}>FLAG</th>
                    <th style={{ padding: '12px', fontSize: '11px', color: 'var(--text-muted)' }}>DEFAULT</th>
                    <th style={{ padding: '12px', fontSize: '11px', color: 'var(--text-muted)' }}>DESCRIPTION</th>
                  </tr>
                </thead>
                <tbody>
                  {[
                    { flag: '--engine', def: 'verity', desc: 'Sets the adjudication engine (verity | legacy).' },
                    { flag: '--port', def: '5173', desc: 'Local listener port for the Nexus core.' },
                    { flag: '--decay', def: 'auto', desc: 'Contextd sharding decay strategy (auto | manual).' },
                    { flag: '--debug', def: 'false', desc: 'Enable verbose wire logs for all LLM calls.' },
                  ].map((row, i) => (
                    <tr key={i} style={{ borderBottom: '1px solid var(--border)' }}>
                      <td style={{ padding: '12px', color: 'var(--accent)', fontWeight: '850', fontFamily: 'var(--mono)', fontSize: '13px' }}>{row.flag}</td>
                      <td style={{ padding: '12px', color: 'var(--text-muted)', fontSize: '13px' }}>{row.def}</td>
                      <td style={{ padding: '12px', color: 'var(--text-dim)', fontSize: '13px' }}>{row.desc}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
           </div>
        </section>

        {/* ── Command Modules ─────────────────────── */}
        <section className="doc-section">
          <h2>Core Command Modules</h2>
          <div className="pillars-grid">
             <div className="pillar-card">
               <Activity size={18} fill="rgba(232,116,92,0.1)" />
               <h4>nexus status</h4>
               <p>Analyzes local health, memory shards, and active API provider telemetry.</p>
             </div>
             <div className="pillar-card">
               <Archive size={18} fill="rgba(232,116,92,0.1)" />
               <h4>nexus audit</h4>
               <p>Dumps the last 50 local interaction events for security debugging.</p>
             </div>
             <div className="pillar-card">
               <RefreshCw size={18} fill="rgba(232,116,92,0.1)" />
               <h4>nexus update</h4>
               <p>Syncs latest model definitions without updating the core binary.</p>
             </div>
          </div>
        </section>

        {/* ── Interactive Loop ─────────────────────── */}
        <section className="doc-section">
           <div className="section-header-inline">
              <Command size={20} color="var(--accent)" />
              <h2>The Interactive Loop</h2>
           </div>
           <p>Once inside the Nexus loop, navigate using numeric keyboard indices to switch between orchestration pillars.</p>
           
           <Terminal lines={[
             '<span style="color: var(--text-muted)">[1] Routing Engine    - Adjudicate prompts.</span>',
             '<span style="color: var(--text-muted)">[2] Contextd Storage  - Manage memory shards.</span>',
             '<span style="color: var(--text-muted)">[3] API Key Vault     - Manage encrypted keys.</span>',
             '<span style="color: var(--text-muted)">[5] Financial Intel   - Observe token economy.</span>',
             '<span style="color: var(--accent)">nexus@local > Select module: </span>'
           ]} />
        </section>

        {/* ── Environment ──────────────────────────── */}
        <section className="doc-section">
          <h2>Shell Environment</h2>
          <div className="best-practices-grid">
            <Callout type="info">
              <strong>NEXUS_DB_PATH:</strong> Explicitly set the location of your <code>.db</code> and <code>.vault</code> files.
            </Callout>
            <Callout type="warning">
              <strong>NEXUS_LOG_LEVEL:</strong> Defaults to <code>INFO</code>. Use <code>DEBUG</code> only during development as it may leak decrypted tokens to stdout.
            </Callout>
          </div>
        </section>
      </motion.div>
    </motion.div>
  );
}
