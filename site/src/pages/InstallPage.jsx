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
      label: 'Recommended (Windows)', 
      code: '# 1. Build project with Maven\nmvn clean package\n\n# 2. Start Nexus via the launcher script\nnexus start' 
    },
    { 
      label: 'Maven + Java Direct', 
      code: '# 1. Build JAR with all dependencies\nmvn clean package\n\n# 2. Run the JAR directly\njava -jar target/nexus-autopilot-2.1.0-jar-with-dependencies.jar' 
    },
    { 
      label: 'Linux / MacOS', 
      code: '# 1. Build project\nmvn clean package\n\n# 2. Add as alias or run jar directly\njava -jar target/nexus-autopilot-2.1.0-jar-with-dependencies.jar' 
    },
    { 
      label: 'Update Only', 
      code: '# 1. Rebuild JAR from latest source\nmvn clean package\n\n# 2. Nexus automatically handles schema migrations on start' 
    }
  ];

  return (
    <motion.div className="page" initial="hidden" animate="show" variants={container}>
      <Helmet>
        <title>Installation — Nexus Autopilot</title>
        <meta name="description" content="Set up Nexus Autopilot locally using Maven and the Windows launcher. Learn how to build the v2.1.0 Agentic OS from source." />
      </Helmet>

      <motion.div variants={item}>
        <div className="badge-premium">DEPLOYMENT</div>
        <h1 className="page-title">Installation Guide</h1>
        <p className="page-description">
          Nexus is a local-first agentic operating system. Since it runs as a standalone JAR, installation is as simple as building from source and launching the script.
        </p>

        {/* ── Selection Tabs ────────────────────────── */}
        <div className="doc-section">
           <InstallTabs tabs={installOptions} />
        </div>

        {/* ── Prerequisites ─────────────────────────── */}
        <section className="doc-section" style={{ marginTop: '64px' }}>
          <div className="section-header-inline">
             <Settings size={20} color="var(--accent)" />
             <h2>System Prerequisites</h2>
          </div>
          <p>
            Before running Nexus, ensure your environment has the required runtime components.
          </p>
          
          <div className="pillars-grid" style={{ marginTop: '32px' }}>
            <div className="pillar-card">
              <Cpu size={18} color="var(--accent)" />
              <h4>JDK 17 Runtime</h4>
              <p>The core Nexus engine is built in Java 17. Ensure <code>java -version</code> reports 17 or higher in your terminal.</p>
            </div>
            <div className="pillar-card">
              <Database size={18} color="var(--accent)" />
              <h4>Maven Build System</h4>
              <p>Used to resolve dependencies and package the JAR. Verify with <code>mvn -v</code> before building.</p>
            </div>
            <div className="pillar-card">
              <ShieldCheck size={18} color="var(--accent)" />
              <h4>Workspace Permissions</h4>
              <p>Nexus needs write access to its current directory to create <code>nexus.db</code> and index your code's Architecture DNA.</p>
            </div>
          </div>
        </section>

        {/* ── Verification ──────────────────────────── */}
        <section className="doc-section">
           <div className="section-header-inline">
              <Binary size={20} color="var(--accent)" />
              <h2>Verify Deployment</h2>
           </div>
            <p>Once the build completes and the script is launched, the Nexus splash screen will appear. If you see the version <code>v2.1.0</code>, your installation is successful.</p>
            <CodeBlock code="nexus start" lang="bash" />
        </section>

        {/* ── Next Steps ────────────────────────────── */}
        <div className="next-steps-banner">
           <h3>Nexus Successfully Indexed</h3>
           <p>
             You've deployed the core engine. Now it's time to register your first user and configure your provider API keys.
           </p>
           <div style={{ marginTop: '32px' }} className="deep-dive-grid">
              <Link to="/quickstart" className="deep-dive-link" style={{ textAlign: 'center' }}>
                 <span>Quickstart Guide &rarr;</span>
                 <p>Go from empty vault to your first autonomous execution in under 2 minutes.</p>
              </Link>
           </div>
        </div>
      </motion.div>
    </motion.div>
  );
}
