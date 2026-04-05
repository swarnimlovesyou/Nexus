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
          A local-first typed memory layer on SQLite for operational knowledge captured during AI-agent workflows.
        </p>

        {/* ── Architecture Overview ─────────────────── */}
        <section className="doc-section">
           <div className="section-header-inline">
              <Layers size={20} color="var(--accent)" />
              <h2>Typed Persistence Architecture</h2>
           </div>
           <p>
             Contextd stores explicit memory objects (`FACT`, `PREFERENCE`, `EPISODE`, `SKILL`, `CONTRADICTION`) in SQLite and exposes CRUD + recall
             through service/DAO layers. It is designed for practical, queryable workflow memory, not opaque chat transcripts.
           </p>

           <div className="algorithm-card">
              <div className="alg-header">
                <DbIcon size={14} />
                <span>MEMORY LIFECYCLE RULES</span>
              </div>
              <div className="alg-grid">
                 <div className="alg-item">
                   <span className="label">Store Types</span>
                   <span className="value">FACT/PREFERENCE/EPISODE/SKILL/CONTRADICTION</span>
                   <div className="bar"><div className="fill" style={{ width: '100%' }}></div></div>
                 </div>
                 <div className="alg-item">
                   <span className="label">Recall Ranking</span>
                   <span className="value">confidence*0.7 + recency*0.3</span>
                   <div className="bar"><div className="fill" style={{ width: '70%' }}></div></div>
                 </div>
                 <div className="alg-item">
                   <span className="label">Decay Rate</span>
                   <span className="value">-5% after 7 days stale</span>
                   <div className="bar"><div className="fill" style={{ width: '55%' }}></div></div>
                 </div>
                 <div className="alg-item">
                   <span className="label">Prune Threshold</span>
                   <span className="value">confidence &lt; 0.10 or expired TTL</span>
                   <div className="bar"><div className="fill" style={{ width: '35%' }}></div></div>
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
             Not all memories stay relevant forever. The decay pass reduces confidence by 5% for entries that have not been accessed in 7+ days.
             Pruning removes entries once confidence drops below <code>0.10</code> or TTL expires.
           </p>
           
           <CodeBlock 
             lang="python" 
             code={`# Contextd decay calculation logic
      def run_decay(memory):
        if days_since(memory.last_accessed_or_created) > 7:
          memory.confidence = max(0.0, memory.confidence - 0.05)

      def should_prune(memory):
        return memory.confidence < 0.10 or memory.is_ttl_expired()`}
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
               <p>SQLite-backed recall avoids network round-trips and keeps retrieval local to your machine.</p>
             </div>
             <div className="pillar-card">
               <Activity size={18} fill="rgba(232,116,92,0.1)" />
               <h4>Predictive Injection</h4>
               <p>Nexus ranks recall results by confidence and recency so you can inject the most relevant memories first.</p>
             </div>
          </div>
        </section>

        {/* ── Best Practices ──────────────────────────── */}
        <section className="doc-section">
          <h2>Memory Management</h2>
          <div className="best-practices-grid">
            <Callout type="info">
              <strong>Storage Path:</strong> Memory is persisted in local SQLite tables inside <code>nexus.db</code>.
            </Callout>
            <Callout type="warning">
              <strong>Operational Habit:</strong> Run decay/prune periodically, or at least review stale memories weekly to keep recall high-signal.
            </Callout>
          </div>
        </section>
      </motion.div>
    </motion.div>
  );
}
