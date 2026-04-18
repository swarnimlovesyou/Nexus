import { motion } from 'framer-motion';
import { Helmet } from 'react-helmet-async';
import { Command, Terminal as CliIcon, Play, RefreshCw, Archive, Activity, Layers, GitFork } from 'lucide-react';
import { CodeBlock, Callout, Terminal } from '../components/UI';

const container = { hidden: { opacity: 0 }, show: { opacity: 1, transition: { staggerChildren: 0.1 }}};
const item = { hidden: { opacity: 0, y: 15 }, show: { opacity: 1, y: 0 }};

export function CliReferencePage() {
  return (
    <motion.div className="page" initial="hidden" animate="show" variants={container}>
      <Helmet>
        <title>CLI Reference - Nexus Autopilot</title>
      </Helmet>

      <motion.div variants={item}>
        <div className="badge-premium">COMMAND REFERENCE</div>
        <h1 className="page-title">CLI Reference</h1>
        <p className="page-description">
          Nexus is built for engineers who work in the terminal. A single binary for all your agentic orchestration needs.
        </p>

        <section className="doc-section">
           <div className="section-header-inline">
              <Play size={20} color="var(--accent)" />
              <h2>Initialization & Runtime</h2>
           </div>
           <p>The CLI entrypoint launches the interactive dashboard.</p>
           <CodeBlock lang="bash" code="nexus start" />

           <div className="table-responsive" style={{ marginTop: '24px' }}>
              <table className="custom-table" style={{ width: '100%', borderCollapse: 'collapse' }}>
                <thead>
                  <tr style={{ textAlign: 'left', borderBottom: '1px solid var(--border)' }}>
                    <th style={{ padding: '12px', fontSize: '11px', color: 'var(--text-muted)' }}>ENTRYPOINT</th>
                    <th style={{ padding: '12px', fontSize: '11px', color: 'var(--text-muted)' }}>SUPPORTED</th>
                    <th style={{ padding: '12px', fontSize: '11px', color: 'var(--text-muted)' }}>DESCRIPTION</th>
                  </tr>
                </thead>
                <tbody>
                  {[
                    { flag: 'nexus start', def: 'yes', desc: 'Launches the full interactive dashboard with all menu modules.' },
                    { flag: 'java -jar nexus.jar', def: 'yes', desc: 'Runs Nexus directly without wrapper scripts. Equivalent to nexus start.' },
                    { flag: 'nexus session ...', def: 'yes', desc: 'Run session workflows from command mode, including resume/rename/fork/rewind/export.' },
                    { flag: 'nexus finance report ...', def: 'yes', desc: 'Generate spend analytics from command mode with date-range filters (for example --range 30d).' },
                    { flag: 'nexus mcp|plugin|hooks ...', def: 'yes', desc: 'Operate compatibility registries and operational integrations from command mode.' },
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

        <section className="doc-section">
          <div className="section-header-inline">
            <CliIcon size={20} color="var(--accent)" />
            <h2>Command Mode Examples</h2>
          </div>
          <p>Use these commands when you want fast terminal-first flows without entering the menu loop.</p>
          <CodeBlock
            lang="bash"
            code={`nexus session list --user admin
nexus session start --user admin --task CODE_GENERATION --note "pairing on refactor"
nexus session close --user admin --id 42 --input 1200 --output 580 --quality 0.89
nexus session fork --id 42 --title "experiment branch"
nexus mcp list
nexus plugin list
nexus hooks list
nexus finance report --user admin --range 30d`}
          />
        </section>

        <section className="doc-section">
          <div className="section-header-inline">
            <Layers size={20} color="var(--accent)" />
            <h2>Compatibility Command Suite</h2>
          </div>
          <p>
            Compatibility aliases are normalized by the dispatcher. Both slash commands and plain command forms map to
            the same underlying handlers.
          </p>
          <CodeBlock
            lang="bash"
            code={`nexus /mcp list
nexus /plugin list
nexus permissions status
nexus skills list
nexus agents list
nexus tasks list
nexus /plan run --goal "reduce model spend"`}
          />
        </section>

        <section className="doc-section">
          <div className="section-header-inline">
            <GitFork size={20} color="var(--accent)" />
            <h2>Session Power Tools</h2>
          </div>
          <p>Advanced lifecycle controls let you continue, branch, and export sessions from either menus or command mode.</p>
          <CodeBlock
            lang="bash"
            code={`nexus session resume --id 42
nexus session rename --id 42 --title "post-merge polish"
nexus session rewind --id 42 --to 4
nexus session export --id 42 --format markdown`}
          />
        </section>

        <section className="doc-section">
          <h2>Core Command Modules</h2>
          <div className="pillars-grid">
             <div className="pillar-card">
               <Activity size={18} fill="rgba(232,116,92,0.1)" />
               <h4>Routing & Sessions</h4>
               <p>Route tasks, run explain and what-if, test provider calls, and run interactive coding sessions.</p>
             </div>
             <div className="pillar-card">
               <Archive size={18} fill="rgba(232,116,92,0.1)" />
               <h4>Memory & Audit</h4>
               <p>Capture typed memories, run decay/prune, and review persistent audit events.</p>
             </div>
             <div className="pillar-card">
               <RefreshCw size={18} fill="rgba(232,116,92,0.1)" />
               <h4>History & Finance</h4>
               <p>Track outcomes, filter by task/model/date, and inspect spend versus optimization opportunities.</p>
             </div>
          </div>
        </section>

        <section className="doc-section">
           <div className="section-header-inline">
              <Command size={20} color="var(--accent)" />
              <h2>The Interactive Loop</h2>
           </div>
           <p>Once inside the Nexus loop, navigate using numeric keyboard indices to switch between orchestration pillars.</p>

           <Terminal lines={[
             '<span style="color: var(--text-muted)">[1] Intelligent Routing Engine</span>',
             '<span style="color: var(--text-muted)">[2] Memory Vault</span>',
             '<span style="color: var(--text-muted)">[3] API Key Vault</span>',
             '<span style="color: var(--text-muted)">[4] Model Discovery</span>',
             '<span style="color: var(--text-muted)">[5] Financial Intelligence</span>',
             '<span style="color: var(--text-muted)">[6] Execution History</span>',
             '<span style="color: var(--text-muted)">[7] Audit Log</span>',
             '<span style="color: var(--text-muted)">[8] Account Settings</span>',
             '<span style="color: var(--text-muted)">[I] Intelligence Hub  (Architecture DNA · Security · Market)</span>',
             '<span style="color: var(--text-muted)">[10] Compatibility Hub  (MCP · Plugins · Hooks · Permissions · Skills · Agents · Tasks · Plan)</span>',
             '<span style="color: var(--text-muted)">[11] Session Power Tools  (Resume · Rename · Fork · Rewind · Export)</span>',
             '<span style="color: var(--text-muted)">[9] System Administration  (ADMIN only)</span>',
             '<span style="color: var(--accent)">nexus@local > Select module: </span>'
           ]} />
        </section>

        <section className="doc-section">
          <h2>Notes</h2>
          <div className="best-practices-grid">
            <Callout type="info">
              <strong>Database:</strong> Nexus persists all data in local SQLite (nexus.db) through JDBC DAOs.
            </Callout>
            <Callout type="warning">
              <strong>Provider calls:</strong> Live call mode depends on network and valid API keys. If unavailable, Nexus falls back to simulation mode and labels it explicitly.
            </Callout>
          </div>
        </section>
      </motion.div>
    </motion.div>
  );
}
