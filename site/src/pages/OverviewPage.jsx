import { Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import { Helmet } from 'react-helmet-async';
import { ShieldCheck, Activity, Brain, Zap, Cpu, Lock, ArrowRight, GitBranch, Binary, Database, Layers, Target, Settings, Clock, Network, Globe } from 'lucide-react';
import { Terminal, Callout } from '../components/UI';

const container = {
  hidden: { opacity: 0 },
  show: { opacity: 1, transition: { staggerChildren: 0.1 } }
};

const item = {
  hidden: { opacity: 0, y: 15 },
  show: { opacity: 1, y: 0 }
};

export function OverviewPage() {
  return (
    <motion.div className="page" initial="hidden" animate="show" variants={container}>
      <Helmet>
        <title>Overview — Nexus Autopilot</title>
        <meta name="description" content="Nexus is a local-first agentic CLI for intelligent LLM routing, semantic memory, security auditing, and financial intelligence." />
      </Helmet>

      {/* ── Hero section ────────────────────────── */}
      <motion.div className="doc-hero" variants={item} style={{ marginBottom: '64px' }}>
        <div className="badge-premium">PLATFORM CORE</div>
        <h1 className="page-title">
          Deterministic Orchestration for <span className="text-glow">Stochastic Models.</span>
        </h1>
        <p className="page-description">
          Nexus is a local-first agentic operating system that routes every prompt to the optimal model, tracks spend at the token level, and proactively defends your workspace — all without a cloud service.
        </p>
        <div className="hero-actions" style={{ display: 'flex', gap: '16px', marginTop: '24px' }}>
          <Link to="/install" className="btn-glow">
            Get Started <ArrowRight size={16} />
          </Link>
          <a href="https://github.com/swarnimlovesyou/Nexus" target="_blank" rel="noreferrer" className="btn-outline">
            <GitBranch size={16} /> View Source
          </a>
        </div>
      </motion.div>

      {/* ── System Status ────────────────────────── */}
      <motion.div variants={item} style={{ marginBottom: '64px' }}>
        <Terminal lines={[
          '<span class="t-prompt">nexus@local $</span> <span class="t-cmd">nexus start</span>',
          '<span style="color: var(--accent)">Nexus v2.1.0 — Agentic OS loading...</span>',
          '<span style="color: var(--green)">● Core Online · Memory Vault: 24 memories · Security: CLEAN</span>',
          '<span style="color: var(--text-muted)">[Intelligence] Architecture DNA: 56 classes indexed</span>',
          '<span style="color: var(--text-muted)">[Market] Last sync: 2026-04-09 · gpt-4o: $0.005/1k</span>',
          '<span style="color: var(--accent); font-weight: 850">ROUTING VERDICT: claude-3-5-sonnet [Score: 0.967 · Cost: $0.0036]</span>'
        ]} />
      </motion.div>

      {/* ── Core Concepts: Routing ────────────────── */}
      <motion.div variants={item} className="doc-section">
        <div className="section-header-inline">
           <Activity size={20} color="var(--accent)" />
           <h2>The Intelligent Routing Signal</h2>
        </div>
        <p>
          Nexus doesn't just proxy calls; it <strong>adjudicates</strong> them. Every request is analyzed by the Composite Scoring Engine,
          which generates a deterministic value <code>[0.0 - 1.0]</code> for every configured provider based on suitability mappings, recorded execution outcomes, latency history, and live market costs.
        </p>

        <div className="algorithm-card">
          <div className="alg-header">
            <Settings size={14} />
            <span>COMPOSITE SCORING ALGORITHM — v2.1.0</span>
          </div>
          <div className="alg-grid">
             <div className="alg-item">
               <Target size={14} />
               <span className="label">Suitability</span>
               <span className="value">45%</span>
               <div className="bar"><div className="fill" style={{ width: '45%' }}></div></div>
             </div>
             <div className="alg-item">
               <ShieldCheck size={14} />
               <span className="label">Quality</span>
               <span className="value">25%</span>
               <div className="bar"><div className="fill" style={{ width: '25%' }}></div></div>
             </div>
             <div className="alg-item">
               <Zap size={14} />
               <span className="label">Latency</span>
               <span className="value">20%</span>
               <div className="bar"><div className="fill" style={{ width: '20%' }}></div></div>
             </div>
             <div className="alg-item">
               <Activity size={14} />
               <span className="label">Cost</span>
               <span className="value">10%</span>
               <div className="bar"><div className="fill" style={{ width: '10%' }}></div></div>
             </div>
          </div>
        </div>
      </motion.div>

      {/* ── System Topology ───────────────────────── */}
      <motion.div variants={item} className="doc-section">
         <div className="section-header-inline">
            <Network size={20} color="var(--accent)" />
            <h2>System Topology</h2>
         </div>
         <p>Nexus acts as a performance and security proxy, ensuring every agent call is adjudicated, cost-tracked, and audited before and after reaching a provider.</p>

         <div className="topology-grid" style={{ display: 'grid', gridTemplateColumns: '1fr 40px 1.5fr 40px 1fr', alignItems: 'center', margin: '48px 0', gap: '16px' }}>
            <div className="topo-node">
               <Cpu size={24} />
               <span>User Agent</span>
            </div>
            <ArrowRight size={24} color="var(--border)" />
            <div className="topo-node topo-nexus">
               <Layers size={24} />
               <strong>NEXUS CORE</strong>
               <small>Route · Audit · Learn</small>
            </div>
            <ArrowRight size={24} color="var(--border)" />
            <div className="topo-node">
               <Globe size={24} />
               <span>LLM Providers</span>
            </div>
         </div>
      </motion.div>

      {/* ── Memory Vault ──────────────────────────── */}
      <motion.div variants={item} className="doc-section">
        <div className="section-header-inline">
           <Database size={20} color="var(--accent)" />
           <h2>Contextd: The Memory Vault</h2>
        </div>
        <p>
          A local-first SQLite persistence layer for typed operational memories.
          Supports 5 memory types (FACT, PREFERENCE, EPISODE, SKILL, CONTRADICTION), each with configurable TTL, confidence scores, and automatic decay on every login.
        </p>
      </motion.div>

      {/* ── Architectural Pillars ─────────────────── */}
      <motion.div variants={item} className="doc-section">
        <h2>Architectural Pillars</h2>
        <div className="pillars-grid" style={{ gridTemplateColumns: 'repeat(3, 1fr)' }}>
           <div className="pillar-card">
             <ShieldCheck size={18} />
             <h4>Auditability</h4>
             <p>Every action — login, memory write, key add, LLM call — is logged to an append-only SQLite audit table for full traceability.</p>
           </div>
           <div className="pillar-card">
             <Zap size={18} />
             <h4>Cost Optimization</h4>
             <p>Routes simple tasks to cost-efficient models and complex reasoning to frontier models. Tracks spend at the token level per session.</p>
           </div>
           <div className="pillar-card">
             <Clock size={18} />
             <h4>Session Intelligence</h4>
             <p>Opens and closes coded agent sessions. Outcome data auto-feeds the routing recalibration engine after every close.</p>
           </div>
           <div className="pillar-card">
             <Lock size={18} />
             <h4>Security Sentinel</h4>
             <p>Proactively scans the local workspace for leaked API keys, hardcoded IPs, and SQL injection risks before they reach version control.</p>
           </div>
           <div className="pillar-card">
             <Binary size={18} />
             <h4>Architecture DNA</h4>
             <p>Maps every class and its dependency graph into the Memory Vault, creating a queryable architectural knowledge base of your project.</p>
           </div>
           <div className="pillar-card">
             <Globe size={18} />
             <h4>Market Grounding</h4>
             <p>Syncs live pricing from OpenRouter so routing cost calculations always reflect current real-world token rates — never stale data.</p>
           </div>
        </div>
      </motion.div>

      {/* ── Final Navigation ──────────────────────── */}
      <motion.div variants={item} className="doc-section">
        <h2>Next Logical Steps</h2>
        <div className="deep-dive-grid">
           <Link to="/routing" className="deep-dive-link">
              <span>Routing Engine &rarr;</span>
              <p>4-signal composite scoring and autonomous recalibration.</p>
           </Link>
           <Link to="/memory" className="deep-dive-link">
              <span>Memory Layer &rarr;</span>
              <p>Typed memory lifecycle with decay and contradiction detection.</p>
           </Link>
           <Link to="/architecture" className="deep-dive-link">
              <span>Sovereign Architecture &rarr;</span>
              <p>Full system brief — every file, table, and agentic flow documented.</p>
           </Link>
           <Link to="/cli" className="deep-dive-link">
              <span>CLI Reference &rarr;</span>
              <p>Interactive and headless command-mode usage.</p>
           </Link>
        </div>
      </motion.div>
    </motion.div>
  );
}
