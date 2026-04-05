import { motion } from 'framer-motion';
import { Helmet } from 'react-helmet-async';
import { Terminal as TerminalIcon, UserPlus, Key, Zap, ArrowRight, ShieldCheck, Database, LayoutGrid, Settings, BookOpen, Clock, Activity, Target } from 'lucide-react';
import { CodeBlock, Callout } from '../components/UI';
import { Link } from 'react-router-dom';

const container = { hidden: { opacity: 0 }, show: { opacity: 1, transition: { staggerChildren: 0.1 }}};
const item = { hidden: { opacity: 0, y: 15 }, show: { opacity: 1, y: 0 }};

export function QuickstartPage() {
  return (
    <motion.div className="page" initial="hidden" animate="show" variants={container}>
      <Helmet>
        <title>Quickstart — Nexus Autopilot</title>
        <meta name="description" content="Go from zero to your first routed AI interaction in under 2 minutes with Nexus." />
      </Helmet>

      <motion.div variants={item}>
        <div className="badge-premium">GETTING STARTED</div>
        <h1 className="page-title">Quickstart Guide</h1>
        <p className="page-description">
          Go from zero to your first autonomously routed prompt in under 2 minutes.
        </p>

        {/* ── Intelligent Routing Signal ───────────────── */}
        <section className="doc-section">
          <div className="section-header-inline">
             <Activity size={20} color="var(--accent)" />
             <h2>The Intelligent Routing Signal</h2>
          </div>
          <p>
            Nexus doesn't just proxy calls; it <strong>adjudicates</strong> them. Every request is analyzed by the Composite Scoring Engine, 
            which generates a deterministic value <code>[0.0 - 1.0]</code> for every configured provider based on suitability mappings and execution outcomes.
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
        </section>

        {/* ── Steps ───────────────────────────────────── */}
        <div className="steps-container">
           <div className="step-box">
             <h3>01. Initialize Nexus</h3>
             <p>Deploy the platform core with a single command.</p>
             <CodeBlock code="nexus start" lang="bash" />
           </div>
           
           <div className="step-box">
             <h3>02. Local Registration</h3>
             <p>Create your local account (Option 2) so memories, sessions, keys, and outcomes are associated with your user.</p>
           </div>
           
           <div className="step-box">
             <h3>03. API Key Vault</h3>
             <p>Navigate to <strong>Option 3</strong> and add keys for OpenAI or Groq. Keys are XOR-encoded immediately.</p>
           </div>
        </div>

        {/* ── Contextd ────────────────────────────────── */}
        <section className="doc-section">
          <div className="section-header-inline">
             <Database size={20} color="var(--accent)" />
             <h2>Contextd: The Memory Vault</h2>
          </div>
          <p>
            A local-first SQLite memory layer for typed workflow knowledge.
            Features <strong>auto-decay</strong> and prune operations to keep recall quality high.
          </p>
        </section>

        {/* ── Architectural Pillars ───────────────────── */}
        <section className="doc-section">
          <h2>Architectural Pillars</h2>
          <div className="pillars-grid">
             <div className="pillar-card">
               <ShieldCheck size={18} />
               <h4>Auditability</h4>
               <p>Every interaction is logged locally for security auditing without leaking IP to third parties.</p>
             </div>
             <div className="pillar-card">
               <Zap size={18} />
               <h4>Cost Optimization</h4>
               <p>Automatically routes to cost-efficient models for simple tasks, saving frontier models for complex reasoning.</p>
             </div>
             <div className="pillar-card">
               <Clock size={18} />
               <h4>Session Context</h4>
               <p>Use start/close session flows to track end-to-end coding runs and auto-log outcomes consistently.</p>
             </div>
          </div>
        </section>

        {/* ── Deep Dives ──────────────────────────────── */}
        <section className="doc-section">
           <h2>Logical Deep Dives</h2>
           <div className="deep-dive-grid">
              <Link to="/routing" className="deep-dive-link">
                 <span>Routing Logic &rarr;</span>
                 <p>Algorithm score matrix</p>
              </Link>
              <Link to="/memory" className="deep-dive-link">
                 <span>Contextd Storage &rarr;</span>
                 <p>Memory decay algorithm</p>
              </Link>
              <Link to="/api-vault" className="deep-dive-link">
                 <span>API Key Vault &rarr;</span>
                 <p>Encrypted local storage</p>
              </Link>
              <Link to="/cli" className="deep-dive-link">
                 <span>CLI Reference &rarr;</span>
                  <p>Interactive and command-mode commands</p>
              </Link>
           </div>
        </section>

        {/* ── Best Practices ──────────────────────────── */}
        <section className="doc-section">
          <h2>Best Practices</h2>
          <div className="best-practices-grid">
             <Callout type="info">
               <strong>Prompt Engineering:</strong> Keep tasks concise. Nexus routes better when intent is clearly defined at the start of the prompt.
             </Callout>
             <Callout type="warning">
               <strong>Key Management:</strong> Keys are XOR-obfuscated for local convenience; use dedicated secret management for high-security environments.
             </Callout>
          </div>
        </section>

        {/* ── Next Steps ──────────────────────────────── */}
        <div className="next-steps-banner">
           <h3>Next Steps</h3>
           <p>Ready to initialize your local environment? Go from zero to your first routed prompt in under 2 minutes.</p>
           <Link to="/install" className="btn-glow">Proceed to Installation</Link>
        </div>
      </motion.div>
    </motion.div>
  );
}
