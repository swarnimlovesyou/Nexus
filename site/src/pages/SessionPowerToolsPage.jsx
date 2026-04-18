import { motion } from 'framer-motion';
import { Helmet } from 'react-helmet-async';
import { GitFork, PlayCircle, PenSquare, RotateCcw, FileOutput } from 'lucide-react';
import { CodeBlock, Terminal, Callout } from '../components/UI';

const container = { hidden: { opacity: 0 }, show: { opacity: 1, transition: { staggerChildren: 0.1 } } };
const item = { hidden: { opacity: 0, y: 15 }, show: { opacity: 1, y: 0 } };

export function SessionPowerToolsPage() {
  return (
    <motion.div className="page" initial="hidden" animate="show" variants={container}>
      <Helmet>
        <title>Session Power Tools - Nexus Autopilot</title>
        <meta
          name="description"
          content="Advanced Nexus session lifecycle controls: resume, rename, fork, rewind, and export for deterministic multi-turn workflows."
        />
      </Helmet>

      <motion.div variants={item}>
        <div className="badge-premium">DASHBOARD 11</div>
        <h1 className="page-title">Session Power Tools</h1>
        <p className="page-description">
          Multi-turn work no longer needs brittle shell bookkeeping. Session Power Tools add lifecycle controls for
          continuation, branching, and transcript portability.
        </p>

        <section className="doc-section">
          <div className="section-header-inline">
            <GitFork size={20} color="var(--accent)" />
            <h2>Lifecycle Actions</h2>
          </div>
          <div className="pillars-grid">
            <div className="pillar-card">
              <PlayCircle size={18} />
              <h4>Resume</h4>
              <p>Continue an existing session by id, preserving context and continuity metadata.</p>
            </div>
            <div className="pillar-card">
              <PenSquare size={18} />
              <h4>Rename</h4>
              <p>Apply meaningful titles to active threads so teams can discover relevant transcripts quickly.</p>
            </div>
            <div className="pillar-card">
              <GitFork size={18} />
              <h4>Fork</h4>
              <p>Branch from a source session for parallel experimentation without polluting the original thread.</p>
            </div>
            <div className="pillar-card">
              <RotateCcw size={18} />
              <h4>Rewind</h4>
              <p>Roll back to earlier state points when an execution path needs to be re-tried safely.</p>
            </div>
            <div className="pillar-card">
              <FileOutput size={18} />
              <h4>Export</h4>
              <p>Export transcripts for handoff, archive, or external review in CI and project workflows.</p>
            </div>
          </div>
        </section>

        <section className="doc-section">
          <h2>Command Examples</h2>
          <CodeBlock
            lang="bash"
            code={`nexus session list --user admin
nexus session resume --id 42
nexus session rename --id 42 --title "auth hardening regression pass"
nexus session fork --id 42 --title "alt-prompt branch"
nexus session rewind --id 42 --to 5
nexus session export --id 42 --format markdown`}
          />
        </section>

        <section className="doc-section">
          <h2>Dashboard Flow</h2>
          <Terminal
            lines={[
              '<span style="color: var(--text-muted)">[11] Session Power Tools</span>',
              '<span style="color: var(--text-muted)">   Resume · Rename · Fork · Rewind · Export</span>',
              '<span style="color: var(--accent)">nexus@local > Select module: 11</span>',
            ]}
          />
          <Callout type="info">
            <strong>Operator pattern:</strong> use <code>resume</code> for continuity, <code>fork</code> for exploration, then
            <code>export</code> to package the chosen branch.
          </Callout>
          <Callout type="warning">
            <strong>Rewind impact:</strong> rewinding moves session state backward by design. Export before destructive
            rewinds when you need a checkpoint artifact.
          </Callout>
        </section>
      </motion.div>
    </motion.div>
  );
}
