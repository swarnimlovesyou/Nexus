import { motion } from 'framer-motion';
import { Helmet } from 'react-helmet-async';
import { Terminal as TerminalIcon, UserPlus, Key, Zap, ArrowRight, ShieldCheck, Database, Layers, Settings, BookOpen, Clock, Activity, Target, Binary, ShieldAlert } from 'lucide-react';
import { CodeBlock, Callout } from '../components/UI';
import { Link } from 'react-router-dom';

const container = { hidden: { opacity: 0 }, show: { opacity: 1, transition: { staggerChildren: 0.1 }}};
const item = { hidden: { opacity: 0, y: 15 }, show: { opacity: 1, y: 0 }};

export function QuickstartPage() {
  return (
    <motion.div className="page" initial="hidden" animate="show" variants={container}>
      <Helmet>
        <title>Quickstart — Nexus Autopilot</title>
        <meta name="description" content="Go from zero to your first autonomously routed and security-audited AI interaction in under 2 minutes with Nexus v2.1.0." />
      </Helmet>

      <motion.div variants={item}>
        <div className="badge-premium">GETTING STARTED</div>
        <h1 className="page-title">Quickstart Guide</h1>
        <p className="page-description">
          Follow the 4-stage pipeline to initialize your local Agentic OS and run your first intelligently routed prompt.
        </p>

        {/* ── Scoring Engine ─────────────────────────── */}
        <section className="doc-section">
          <div className="section-header-inline">
             <Activity size={20} color="var(--accent)" />
             <h2>Stage 0: The Scoring Heartbeat</h2>
          </div>
          <p>
            Before you start, understand that Nexus is automated. Every prompt is scored across 4 signals before dispatched to a provider.
          </p>
          
          <div className="algorithm-card">
            <div className="alg-header">
              <Settings size={14} />
              <span>COMPOSITE SCORING WEIGHTS — v2.1.0</span>
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
        </section>

        {/* ── Setup Steps ────────────────────────────── */}
        <div className="steps-container">
           <div className="step-box">
             <div className="step-badge">01</div>
             <h3>Initialize Core</h3>
             <p>Launch the platform. On first run, Nexus creates <code>nexus.db</code> and boots the local service mesh.</p>
             <CodeBlock code="nexus start" lang="bash" />
           </div>
           
           <div className="step-box">
             <div className="step-badge">02</div>
             <h3>Identity and Vault</h3>
             <p>Register a local account, then head to <strong>Option 3</strong> to store your OpenAI/Groq keys in the Zero-Trust Vault.</p>
           </div>
           
           <div className="step-box">
             <div className="step-badge">03</div>
             <h3>DNA Indexing</h3>
             <p>Go to the <strong>Intelligence Hub (I)</strong> and run the <strong>Architecture DNA Scan</strong>. This maps your project dependencies into memory.</p>
           </div>

           <div className="step-box">
             <div className="step-badge">04</div>
             <h3>Routed Dispatch</h3>
             <p>Navigate to <strong>Option 1</strong> and enter a complex coding prompt. Nexus will plan, route, and execute autonomously.</p>
           </div>
        </div>

        {/* ── Core Systems ────────────────────────────── */}
        <div className="pillars-grid" style={{ marginTop: '64px' }}>
           <div className="pillar-card">
             <Database size={18} />
             <h4>Memory Vault</h4>
             <p>FACT memories from your DNA scan are automatically injected into the routed context for better LLM grounding.</p>
           </div>
           <div className="pillar-card">
             <ShieldAlert size={18} />
             <h4>Security Sentinel</h4>
             <p>Before executing, use the Sentinel to ensure no keys or hardcoded IPs are sitting in your active buffers.</p>
           </div>
           <div className="pillar-card">
             <Clock size={18} />
             <h4>Outcome Learning</h4>
             <p>Close your session after completion. The routing engine recalibrates its scores based on your reported session quality.</p>
           </div>
        </div>

        {/* ── Navigation ──────────────────────────────── */}
        <section className="doc-section">
           <h2>Detailed Intelligence Deep Dives</h2>
           <div className="deep-dive-grid">
              <Link to="/routing" className="deep-dive-link">
                 <span>Routing Engine &rarr;</span>
                 <p>Autonomous adjudication logic</p>
              </Link>
              <Link to="/memory" className="deep-dive-link">
                 <span>Memory Layer &rarr;</span>
                 <p>Typed storage & decay pass</p>
              </Link>
              <Link to="/architecture-dna" className="deep-dive-link">
                 <span>Architecture DNA &rarr;</span>
                 <p>Dependency mapping internals</p>
              </Link>
              <Link to="/security" className="deep-dive-link">
                 <span>Security Sentinel &rarr;</span>
                 <p>Workspace audit regex library</p>
              </Link>
           </div>
        </section>

        {/* ── Best Practices ──────────────────────────── */}
        <section className="doc-section">
          <h2>Standard Operating Procedures</h2>
          <div className="best-practices-grid">
             <Callout type="info">
               <strong>Rescan DNA:</strong> After big refactors, run the DNA scan again (Intelligence Hub &gt; 1) to keep the Memory Vault's map accurate.
             </Callout>
             <Callout type="warning">
               <strong>Audit Weekly:</strong> Run the Security Sentinel scan before every major push to ensure zero secret leakage.
             </Callout>
          </div>
        </section>

        {/* ── Installation ────────────────────────────── */}
        <div className="next-steps-banner">
           <h3>Ready to start building?</h3>
           <p>Follow the installation guide to build the platform core from source.</p>
           <Link to="/install" className="btn-glow">View Installation Guide</Link>
        </div>
      </motion.div>
    </motion.div>
  );
}
