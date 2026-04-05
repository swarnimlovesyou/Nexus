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
        <meta name="description" content="Nexus is a local-first Java CLI for routing, memory management, API key vaulting, session tracking, and spend analytics." />
      </Helmet>

      {/* ── Hero section ────────────────────────── */}
      <motion.div className="doc-hero" variants={item} style={{ marginBottom: '64px' }}>
        <div className="badge-premium">PLATFORM CORE</div>
        <h1 className="page-title">
          Deterministic Orchestration for <span className="text-glow">Stochastic Models.</span>
        </h1>
        <p className="page-description">
          Nexus is a research-grade local-first orchestration layer that bridges the gap between raw LLM APIs and autonomous agentic workflows.
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
          '<span className="t-prompt">nexus@local $</span> <span className="t-cmd">nexus start</span>',
          '<span style="color: var(--accent)">Nexus Engine Adjudicating suitability matrix...</span>',
          '<span style="color: var(--green)">● Nexus Core Online (v1.2.4)</span>',
          '<span style="color: var(--text-muted)">[System] Contextd Memory Vault: typed memory store online</span>',
          '<span style="color: var(--accent); font-weight: 850">★ ROUTING VERDICT: Claude-3.5-Sonnet [Score: 0.982]</span>',
          '<span style="color: var(--text-muted)">[Metadata] Latency: 1.2s. Estimated cost: $0.0042.</span>'
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
          which generates a deterministic value <code>[0.0 - 1.0]</code> for every configured provider based on suitability mappings and recorded execution outcomes.
        </p>

        <div className="algorithm-card">
          <div className="alg-header">
            <Settings size={14} />
            <span>COMPOSITE SCORING ALGORITHM</span>
          </div>
          <div className="alg-grid">
             <div className="alg-item">
               <Target size={14} />
               <span className="label">Suitability</span>
               <span className="value">35%</span>
               <div className="bar"><div className="fill" style={{ width: '35%' }}></div></div>
             </div>
             <div className="alg-item">
               <ShieldCheck size={14} />
               <span className="label">Quality</span>
               <span className="value">30%</span>
               <div className="bar"><div className="fill" style={{ width: '30%' }}></div></div>
             </div>
             <div className="alg-item">
               <Zap size={14} />
               <span className="label">Cost</span>
               <span className="value">20%</span>
               <div className="bar"><div className="fill" style={{ width: '20%' }}></div></div>
             </div>
             <div className="alg-item">
               <Activity size={14} />
               <span className="label">Latency</span>
               <span className="value">15%</span>
               <div className="bar"><div className="fill" style={{ width: '15%' }}></div></div>
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
         <p>Nexus acts as a security and performance proxy, ensuring your agents operate within a deterministic environment before stochastic LLM interaction occurs.</p>
         
         <div className="topology-grid" style={{ display: 'grid', gridTemplateColumns: '1fr 40px 1.5fr 40px 1fr', alignItems: 'center', margin: '48px 0', gap: '16px' }}>
            <div className="topo-node">
               <Cpu size={24} />
               <span>User Agent</span>
            </div>
            <ArrowRight size={24} color="var(--border)" />
            <div className="topo-node topo-nexus">
               <Layers size={24} />
               <strong>NEXUS CORE</strong>
               <small>Adjudication Layer</small>
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
          Features <strong>auto-decay</strong> and pruning to keep context relevant over time.
        </p>
      </motion.div>

      {/* ── Architectural Pillars ─────────────────── */}
      <motion.div variants={item} className="doc-section">
        <h2>Architectural Pillars</h2>
        <div className="pillars-grid">
           <div className="pillar-card">
             <ShieldCheck size={18} />
             <h4>Auditability</h4>
             <p>Every prompt and response is logged locally for RAG refinement or security auditing without leaking IP.</p>
           </div>
           <div className="pillar-card">
             <Zap size={18} />
             <h4>Cost Optimization</h4>
             <p>Automatically routes simple tasks to cost-efficient models, and complex reasoning to frontier models.</p>
           </div>
           <div className="pillar-card">
             <Clock size={18} />
             <h4>Session Context</h4>
             <p>Track full coding sessions and close them into outcomes so routing quality learns from coherent session data.</p>
           </div>
        </div>
      </motion.div>

      {/* ── Final Navigation ──────────────────────── */}
      <motion.div variants={item} className="doc-section">
        <h2>Next Logical Steps</h2>
        <div className="deep-dive-grid">
           <Link to="/routing" className="deep-dive-link">
              <span>Routing Engine &rarr;</span>
              <p>Adjudication engine deep dive.</p>
           </Link>
           <Link to="/memory" className="deep-dive-link">
              <span>Memory Layer &rarr;</span>
              <p>Understanding typed memory lifecycle.</p>
           </Link>
           <Link to="/api-vault" className="deep-dive-link">
              <span>API Vault &rarr;</span>
              <p>Zero-trust secret management.</p>
           </Link>
           <Link to="/cli" className="deep-dive-link">
              <span>CLI Reference &rarr;</span>
              <p>Interactive and command-mode usage.</p>
           </Link>
        </div>
      </motion.div>
    </motion.div>
  );
}
