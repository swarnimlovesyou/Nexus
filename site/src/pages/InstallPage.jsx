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
      label: 'Maven Build', 
      code: '# Build from repository root\nmvn clean package\n\n# Run jar\njava -jar target/nexus-autopilot-1.0-SNAPSHOT-jar-with-dependencies.jar' 
    },
    { 
      label: 'Windows Helper', 
      code: '# Windows convenience wrapper\nnexus.bat start' 
    },
    { 
      label: 'Jar Direct Run', 
      code: 'java -jar target/nexus-autopilot-1.0-SNAPSHOT-jar-with-dependencies.jar' 
    },
    { 
      label: 'NPM Local Link', 
      code: '# Optional local CLI wrapper\nnpm link\n\n# Then run\nnexus start' 
    }
  ];

  return (
    <motion.div className="page" initial="hidden" animate="show" variants={container}>
      <Helmet>
        <title>Installation — Nexus Autopilot</title>
        <meta name="description" content="Set up Nexus Autopilot locally using Maven, the Windows launcher, direct jar execution, or npm link." />
      </Helmet>

      <motion.div variants={item}>
        <div className="badge-premium">DEPLOYMENT</div>
        <h1 className="page-title">Installation Guide</h1>
        <p className="page-description">
          Nexus is designed to be local-first and lightweight. Build once, then run via jar, launcher script, or npm-linked command.
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
            Before running Nexus, ensure your environment has the required runtime and terminal support.
          </p>
          
          <div className="pillars-grid" style={{ marginTop: '32px' }}>
            <div className="pillar-card">
              <Cpu size={18} color="var(--accent)" />
              <h4>Runtime Engine</h4>
              <p>Java 17+ is required. Node.js is optional and only needed if you want the npm CLI wrapper.</p>
            </div>
            <div className="pillar-card">
              <Database size={18} color="var(--accent)" />
              <h4>Local Storage</h4>
              <p>Embedded SQLite (`nexus.db`) is created automatically on first run. No external DB setup required.</p>
            </div>
            <div className="pillar-card">
              <ShieldCheck size={18} color="var(--accent)" />
              <h4>Terminal Access</h4>
              <p>Use a terminal that supports ANSI output for the best CLI rendering experience.</p>
            </div>
          </div>
        </section>

        {/* ── Troubleshooting ───────────────────────── */}
        <section className="doc-section">
           <div className="section-header-inline">
              <Binary size={20} color="var(--accent)" />
              <h2>Verify Deployment</h2>
           </div>
            <p>If the dashboard loads and prompts for login/register, installation is working.</p>
            <CodeBlock code="nexus start" lang="bash" />
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
