import { motion } from 'framer-motion';
import { Helmet } from 'react-helmet-async';
import { Brain, Database, Activity, Target, Zap, ArrowRight, Clock, ShieldCheck, Layers, Cpu, Database as DbIcon, Settings } from 'lucide-react';
import { CodeBlock, Callout } from '../components/UI';

const container = { hidden: { opacity: 0 }, show: { opacity: 1, transition: { staggerChildren: 0.1 }}};
const item = { hidden: { opacity: 0, y: 15 }, show: { opacity: 1, y: 0 }};

export function MemoryLayerPage() {
  return (
    <motion.div className="page" initial="hidden" animate="show" variants={container}>
      <Helmet>
        <title>Memory Vault — Nexus Autopilot</title>
      </Helmet>

      <motion.div variants={item}>
        <div className="badge-premium">PERSISTENCE LAYER</div>
        <h1 className="page-title">Contextd: The Memory Vault</h1>
        <p className="page-description">
          A local-first SQLite sharding engine designed for high-density agentic memory without the overhead of cloud vector stores.
        </p>

        {/* ── Architecture Overview ─────────────────── */}
        <section className="doc-section">
           <div className="section-header-inline">
              <Layers size={20} color="var(--accent)" />
              <h2>Sharded Persistence Architecture</h2>
           </div>
           <p>
             Contextd isn't just a database; it is a <strong>temporal sharding engine</strong>. It anchors stochastic LLM outputs into 
             stateful, deterministic local memory shards. This allows your agents to maintain long-term context across multiple execution cycles without re-prompting.
           </p>

           <div className="algorithm-card">
              <div className="alg-header">
                <DbIcon size={14} />
                <span>ACTIVE SHARD TELEMETRY</span>
              </div>
              <div className="alg-grid">
                 <div className="alg-item">
                   <span className="label">Total Shards</span>
                   <span className="value">42,810</span>
                   <div className="bar"><div className="fill" style={{ width: '85%' }}></div></div>
                 </div>
                 <div className="alg-item">
                   <span className="label">Access Latency</span>
                   <span className="value">1.4ms</span>
                   <div className="bar"><div className="fill" style={{ width: '15%' }}></div></div>
                 </div>
                 <div className="alg-item">
                   <span className="label">Decay Rate</span>
                   <span className="value">0.05 / day</span>
                   <div className="bar"><div className="fill" style={{ width: '40%' }}></div></div>
                 </div>
                 <div className="alg-item">
                   <span className="label">Relevance Index</span>
                   <span className="value">0.96 P99</span>
                   <div className="bar"><div className="fill" style={{ width: '96%' }}></div></div>
                 </div>
              </div>
           </div>
        </section>

        {/* ── Temporal Decay ───────────────────────── */}
        <section className="doc-section">
           <div className="section-header-inline">
              <Clock size={20} color="var(--accent)" />
              <h2>The Temporal Decay Algorithm</h2>
           </div>
           <p>
             Not all memories are useful forever. Contextd implements a <strong>decay-based pruning logic</strong>. Every shard starts with a vitality score of 1.0. 
             If a shard remains un-queried, its vitality score decays geometrically. Once it hits the <code>0.05</code> threshold, it is archived to local cold storage.
           </p>
           
           <CodeBlock 
             lang="python" 
             code={`# Contextd decay calculation logic
def calculate_shard_vitality(last_access_ts, base_vitality=1.0):
    delta_days = (now() - last_access_ts).days
    decay_factor = 0.95 ** delta_days
    current_vitality = base_vitality * decay_factor
    
    if current_vitality < 0.05:
        trigger_cold_storage_archive()
    return current_vitality`}
           />
        </section>

        {/* ── Architectural Pillars ─────────────────── */}
        <section className="doc-section">
          <h2>Why Local-First Memory?</h2>
          <div className="pillars-grid">
             <div className="pillar-card">
               <ShieldCheck size={18} fill="rgba(232,116,92,0.1)" />
               <h4>Zero Data Leak</h4>
               <p>Your agent's most sensitive project memories never leave your machine.</p>
             </div>
             <div className="pillar-card">
               <Zap size={18} fill="rgba(232,116,92,0.1)" />
               <h4>Sub-ms Recall</h4>
               <p>SQLite sharding ensures memory recall is faster than any cloud vector database.</p>
             </div>
             <div className="pillar-card">
               <Activity size={18} fill="rgba(232,116,92,0.1)" />
               <h4>Predictive Injection</h4>
               <p>Nexus only injects the top 5% most relevant shards, keeping token costs low.</p>
             </div>
          </div>
        </section>

        {/* ── Best Practices ──────────────────────────── */}
        <section className="doc-section">
          <h2>Memory Management</h2>
          <div className="best-practices-grid">
            <Callout type="info">
              <strong>Storage Path:</strong> Memory is persisted in <code>~/.nexus/contextd.db</code>. This file is encrypted using your local vault secret.
            </Callout>
            <Callout type="warning">
              <strong>Archive Hygiene:</strong> We recommend pruning your cold storage every quarter to reclaim disk space.
            </Callout>
          </div>
        </section>
      </motion.div>
    </motion.div>
  );
}
