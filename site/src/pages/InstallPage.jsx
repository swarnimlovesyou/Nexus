import { motion } from 'framer-motion';
import { Helmet } from 'react-helmet-async';
import { Link } from 'react-router-dom';
import { DownloadCloud, Terminal as TerminalIcon, ShieldCheck, Cpu, Database, Server, ArrowRight, Zap, Settings, Binary } from 'lucide-react';
import { CodeBlock, Callout, InstallTabs } from '../components/UI';

const container = { hidden: { opacity: 0 }, show: { opacity: 1, transition: { staggerChildren: 0.1 } }};
const item = { hidden: { opacity: 0, y: 15 }, show: { opacity: 1, y: 0 }};

export function InstallPage() {
  const installOptions = [
    { 
      label: 'NPM (Global)', 
      code: '# Recommended for most users\nnpm install -g nexus-autopilot\n\n# Verify installation\nnexus --version' 
    },
    { 
      label: 'Homebrew (macOS)', 
      code: 'brew tap nexus-autopilot/nexus\nbrew install nexus\n\n# Start nexus\nnexus start' 
    },
    { 
      label: 'PowerShell (Win)', 
      code: 'iwr https://nexus.ai/install.ps1 -useb | iex\n\n# Verify\nnexus start' 
    },
    { 
      label: 'From Source', 
      code: 'git clone https://github.com/swarnimlovesyou/Nexus.git\ncd Nexus\nmvn clean package\njava -jar target/nexus-1.0.jar start' 
    }
  ];

  return (
    <motion.div className="page" initial="hidden" animate="show" variants={container}>
      <Helmet>
        <title>Installation — Nexus Autopilot</title>
        <meta name="description" content="Set up Nexus Autopilot on your local machine using NPM, Homebrew, or binary installation." />
      </Helmet>

      <motion.div variants={item}>
        <div className="badge-premium">DEPLOYMENT</div>
        <h1 className="page-title">Installation Guide</h1>
        <p className="page-description">
          Nexus is designed to be cross-platform and lightweight. Deploy the orchestrator core to your local environment in seconds.
        </p>

        {/* ── Selection Tabs ────────────────────────── */}
        <div className="doc-section">
           <InstallTabs tabs={installOptions} />
        </div>

        {/* ── Prerequisites ─────────────────────────── */}
        <section className="doc-section">
          <div className="section-header-inline">
             <Settings size={20} color="var(--accent)" />
             <h2>System Prerequisites</h2>
          </div>
          <p>
            Before deploying, ensure your local environment meets these minimum high-fidelity requirements for secure LLM orchestration.
          </p>
          
          <div className="pillars-grid" style={{ marginTop: '32px' }}>
            <div className="pillar-card">
              <Cpu size={18} color="var(--accent)" />
              <h4>Runtime Engine</h4>
              <p>Java 17+ (LTS) and Node.js 18+ are required for core execution and CLI management.</p>
            </div>
            <div className="pillar-card">
              <Database size={18} color="var(--accent)" />
              <h4>Local Storage</h4>
              <p>Embedded SQLite engine. No external databases or cloud sync required for local state.</p>
            </div>
            <div className="pillar-card">
              <ShieldCheck size={18} color="var(--accent)" />
              <h4>Write Access</h4>
              <p>Nexus requires elevated local permissions to initialize the <strong>XOR-encoded</strong> key vault.</p>
            </div>
          </div>
        </section>

        {/* ── Troubleshooting ───────────────────────── */}
        <section className="doc-section">
           <div className="section-header-inline">
              <Binary size={20} color="var(--accent)" />
              <h2>Verify Deployment</h2>
           </div>
           <p>Post-installation, bridge the connection to your local core using the status flag.</p>
           <CodeBlock code="nexus status --ping" lang="bash" />
        </section>

        {/* ── Next Steps ────────────────────────────── */}
        <div className="next-steps-banner">
           <h3>Next Steps</h3>
           <p>
             Once installed, you're ready to initialize your local environment and configure your first model provider.
           </p>
           <div style={{ marginTop: '24px', display: 'flex', justifyContent: 'center' }}>
              <Link to="/quickstart" className="btn-glow">
                 Proceed to Quickstart <ArrowRight size={16} />
              </Link>
           </div>
           <div style={{ marginTop: '32px' }} className="deep-dive-grid">
              <Link to="/quickstart" className="deep-dive-link" style={{ textAlign: 'center' }}>
                 <span>Quickstart Guide &rarr;</span>
                 <p>Go from zero to your first routed prompt in under 2 minutes.</p>
              </Link>
           </div>
        </div>
      </motion.div>
    </motion.div>
  );
}
