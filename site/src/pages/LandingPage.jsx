import { motion, AnimatePresence } from 'framer-motion';
import { Link } from 'react-router-dom';
import { useState, useRef } from 'react';
import { BookOpen, Terminal, Shield, Workflow, Database, Zap, Lock, CreditCard, ChevronRight, SquareCode, User } from 'lucide-react';

const FEATURES = [
  { 
    id: 'privacy',
    title: "Local-First Privacy", 
    desc: "Nexus keeps runtime state local in SQLite. API keys are masked and XOR-obfuscated, and all routing/memory/audit data stays on your machine by default.",
    icon: <Shield size={24}/>,
    label: "XOR VAULT",
    specs: { KeyStorage: 'XOR + Base64', Database: 'SQLite', Scope: 'Local-first' }
  },
  { 
    id: 'routing',
    title: "Agentic Routing", 
    desc: "Stop guessing which LLM to use. Nexus scores models by suitability, historical quality, cost efficiency, and latency, then explains the ranking before you choose.",
    icon: <Workflow size={24}/>,
    label: "VERITY ENGINE",
    specs: { Weights: '35/30/20/15', ExplainMode: 'Built-in', WhatIf: 'Budget tiers' }
  },
  { 
    id: 'memory',
    title: "Contextd Memory", 
    desc: "Capture durable workflow knowledge as typed memories (FACT, PREFERENCE, EPISODE, SKILL). Decay and prune tools keep memory relevant over time.",
    icon: <Database size={24}/>,
    label: "TYPED MEMORY",
    specs: { Types: '5 memory classes', Decay: '-5% after 7d stale', Prune: 'Expired or <0.10 conf' }
  },
  { 
    id: 'finance',
    title: "Financial Intel", 
    desc: "Every token has a price. Nexus compares actual spend versus estimated optimal spend and breaks down costs by model and task.",
    icon: <CreditCard size={24}/>,
    label: "TOKEN ECONOMY",
    specs: { Views: 'Lifetime/7d/30d', Breakdown: 'Model + Task', Insight: 'Avoidable spend' }
  },
  { 
    id: 'vault',
    title: "Zero-Trust Vault", 
    desc: "The API Key Vault centralizes provider keys and integrates directly with routing so recommendations stay actionable for your available providers.",
    icon: <Lock size={24}/>,
    label: "KEY MANAGEMENT",
    specs: { MaskedView: 'Enabled', OwnershipChecks: 'Enabled', ProviderAware: 'Enabled' }
  },
  { 
    id: 'latency',
    title: "Predictive Latency", 
    desc: "Latency from recorded outcomes is fed back into routing scores, helping recommendations adapt to your recent execution behavior.",
    icon: <Zap size={24}/>,
    label: "TELEMETRY",
    specs: { Signal: 'Latency score', Source: 'Outcome history', Scope: 'Per task/model' }
  }
];

export function LandingPage() {
  const [activeFeature, setActiveFeature] = useState(FEATURES[0]);

  return (
    <div className="landing-wrap">
      <div className="landing-grid" />
      <div className="bg-blob blob-1" />
      <div className="bg-blob blob-2" />
      <div className="bg-blob blob-3" />
      
      <header className="landing-header">
        <div className="topbar-wordmark" style={{ fontSize: '18px' }}>
          <Terminal size={22} color="var(--accent)" strokeWidth={2.5} />
          <span>Nexus</span>
        </div>
        <div className="landing-nav">
          <Link to="/overview" className="nav-link">Documentation</Link>
          <a href="https://github.com/swarnimlovesyou/Nexus" target="_blank" rel="noreferrer" className="nav-link">GitHub</a>
        </div>
      </header>

      <main className="landing-hero">
        <motion.div 
          initial={{ opacity: 0, y: 15 }}
          animate={{ opacity: 1, y: 0 }}
        >
          <div className="badge-premium">v2.0.0 — THE AGENTIC UPDATE</div>
          <h1 className="hero-title">
            The intelligent center for<br />
            <span className="text-glow">Autonomous Orchestration.</span>
          </h1>
          <p className="hero-subtitle">
            Nexus is a local-first architectural core that bridge the gap between stochastic LLM outputs and deterministic agentic workflows.
          </p>
          
          <div className="hero-ctas">
            <Link to="/overview" className="cta-primary">
              Read Documentation <BookOpen size={18} />
            </Link>
          </div>
        </motion.div>

        <motion.div 
          className="hero-preview"
          initial={{ opacity: 0, scale: 0.98 }}
          animate={{ opacity: 1, scale: 1 }}
          transition={{ delay: 0.2 }}
        >
          <div className="preview-window">
             <div className="window-header">
               <div className="dot dot-red"></div>
               <div className="dot dot-yellow"></div>
               <div className="dot dot-green"></div>
               <span className="window-title">nexus start</span>
             </div>
             <div className="window-content">
               <div className="p-line"><span className="p-prompt">nexus@local $</span> Initializing Contextd typed memory store... [DONE]</div>
               <div className="p-line"><span className="p-prompt">nexus@local $</span> Evaluating suitability matrix for task "Build Landing Page"...</div>
               <div className="p-line t-verdict">★ VERDICT: Claude-3.5-Sonnet routed via Verity Optimization [Score: 0.985]</div>
               <div className="p-line">Latency: 1.2s | Cost: $0.002 | Quality Delta: +14%</div>
             </div>
          </div>
        </motion.div>
      </main>

      <section className="engine-architecture">
        <div className="section-header">
          <div className="badge-premium">ORCHESTRATION CORE</div>
          <h2>The Integrated Foundation</h2>
        </div>

        <div className="feature-selector">
          <div className="selector-list">
            {FEATURES.map((f) => (
              <button 
                key={f.id}
                className={`selector-item ${activeFeature.id === f.id ? 'active' : ''}`}
                onClick={() => setActiveFeature(f)}
              >
                <div className="selector-icon">{f.icon}</div>
                <div className="selector-info">
                  <span className="selector-label">{f.label}</span>
                  <span className="selector-title">{f.title}</span>
                </div>
                <ChevronRight className="selector-arrow" size={16} />
              </button>
            ))}
          </div>

          <div className="selector-display">
             <AnimatePresence mode="wait">
               <motion.div 
                 key={activeFeature.id}
                 initial={{ opacity: 0, x: 20 }}
                 animate={{ opacity: 1, x: 0 }}
                 exit={{ opacity: 0, x: -20 }}
                 className="display-card"
               >
                 <div className="display-header">
                   <div className="display-icon">{activeFeature.icon}</div>
                   <div className="display-meta">
                      <span className="display-label">{activeFeature.label}</span>
                      <h3>{activeFeature.title}</h3>
                   </div>
                 </div>
                 <p className="display-desc">{activeFeature.desc}</p>
                 
                 <div className="display-specs">
                   {Object.entries(activeFeature.specs).map(([k, v]) => (
                     <div key={k} className="spec-item">
                       <span className="spec-label">{k}</span>
                       <span className="spec-val">{v}</span>
                     </div>
                   ))}
                 </div>

                 <div className="display-actions">
                   <Link to="/overview" className="btn-text">Explore technical spec &rarr;</Link>
                 </div>
               </motion.div>
             </AnimatePresence>
          </div>
        </div>
      </section>

      <section className="integrations-section">
        <div className="section-header">
          <div className="badge-premium">ECOSYSTEM</div>
          <h2>Unified Intelligence</h2>
        </div>
        <div className="marquee">
          <div className="marquee-content">
            {['OpenAI', 'Anthropic', 'Google Gemini', 'Groq', 'OpenRouter', 'Custom Provider'].map((p, i) => (
              <div key={i} className="marquee-item">{p}</div>
            ))}
            {/* Duplicate for infinite loop */}
            {['OpenAI', 'Anthropic', 'Google Gemini', 'Groq', 'OpenRouter', 'Custom Provider'].map((p, i) => (
              <div key={`dup-${i}`} className="marquee-item">{p}</div>
            ))}
          </div>
        </div>
      </section>

      <section className="pillars-metrics">
        <div className="metrics-grid">
           <div className="metric-box">
             <span className="m-val">100%</span>
             <span className="m-lbl">Local-First Privacy</span>
           </div>
           <div className="metric-box">
             <span className="m-val">XOR</span>
             <span className="m-lbl">Vault Security</span>
           </div>
           <div className="metric-box">
             <span className="m-val">0.98</span>
             <span className="m-lbl">Verity Routing Score</span>
           </div>
           <div className="metric-box">
             <span className="m-val">∞</span>
             <span className="m-lbl">Context Continuity</span>
           </div>
        </div>
      </section>

      <section className="process-flow">
        <div className="section-header">
           <div className="badge-premium">HOW IT WORKS</div>
           <h2>The Orchestration Loop</h2>
        </div>
        <div className="flow-grid">
           {[
             { title: 'Input Capture', desc: 'Agent submits task. Nexus calculates fingerprint and context requirements.', icon: <SquareCode size={22}/> },
             { title: 'Adjudication', desc: 'Verity engine evaluates the provider matrix for optimal cost-to-quality ratio.', icon: <Workflow size={22}/> },
             { title: 'Execution', desc: 'Request is dispatched to the chosen provider (or simulated when live calls are unavailable).', icon: <Terminal size={22}/> },
             { title: 'Synthesis', desc: 'Result is captured, persisted to Contextd, and returned to the workflow.', icon: <Database size={22}/> },
           ].map((step, i) => (
             <div key={i} className="flow-step">
               <div className="step-icon-wrap">
                  <div className="step-icon">{step.icon}</div>
                  <div className="step-num">{i + 1}</div>
               </div>
               <div className="step-content">
                  <h4>{step.title}</h4>
                  <p>{step.desc}</p>
               </div>
             </div>
           ))}
        </div>
      </section>

      <footer className="landing-footer">
         <div className="footer-brand">
            <div className="topbar-wordmark" style={{ marginBottom: '16px' }}>
              <Terminal size={18} color="var(--accent)" strokeWidth={2.5} />
              <span>Nexus</span>
            </div>
            <p>© 2026 Nexus AI Platform. Built for the era of autonomy.</p>
         </div>
         <div className="footer-links">
            <div className="f-col">
              <span>Platform</span>
              <Link to="/overview">Documentation</Link>
              <Link to="/changelog">Changelog</Link>
            </div>
            <div className="f-col">
              <span>Community</span>
              <a href="https://github.com/swarnimlovesyou/Nexus" target="_blank" rel="noreferrer">GitHub</a>
              <a href="#">Discord</a>
            </div>
            <div className="f-col">
              <span>Contact Us</span>
              <a href="https://www.linkedin.com/in/parthparmar04/" target="_blank" rel="noreferrer" className="contact-link">
                <User size={14} /> Parth Parmar
              </a>
              <a href="https://www.linkedin.com/in/swarnim-jambhrunkar/" target="_blank" rel="noreferrer" className="contact-link">
                <User size={14} /> Swarnim Jambhrunkar
              </a>
              <a href="https://www.linkedin.com/in/saket-sarvaiya/" target="_blank" rel="noreferrer" className="contact-link">
                <User size={14} /> Saket Sarvaiya
              </a>
            </div>
         </div>
      </footer>
    </div>
  );
}
