import { motion } from 'framer-motion';
import { Helmet } from 'react-helmet-async';
import { Plug, ShieldCheck, Wrench, Puzzle, Layers } from 'lucide-react';
import { CodeBlock, Callout, Terminal } from '../components/UI';

const container = { hidden: { opacity: 0 }, show: { opacity: 1, transition: { staggerChildren: 0.1 } } };
const item = { hidden: { opacity: 0, y: 15 }, show: { opacity: 1, y: 0 } };

export function CompatibilityFeaturesPage() {
  return (
    <motion.div className="page" initial="hidden" animate="show" variants={container}>
      <Helmet>
        <title>Compatibility Suite - Nexus Autopilot</title>
        <meta
          name="description"
          content="Claurst-style compatibility command suite in Nexus: MCP, plugins, hooks, permissions, skills, agents, tasks, and plan commands."
        />
      </Helmet>

      <motion.div variants={item}>
        <div className="badge-premium">DASHBOARD 10</div>
        <h1 className="page-title">Compatibility Suite</h1>
        <p className="page-description">
          Nexus now ships a full compatibility command layer for teams migrating workflows that rely on MCP servers,
          plugin ecosystems, hooks, permissions, and plan-oriented automation.
        </p>

        <section className="doc-section">
          <div className="section-header-inline">
            <Plug size={20} color="var(--accent)" />
            <h2>Command Families</h2>
          </div>
          <div className="pillars-grid">
            <div className="pillar-card">
              <Layers size={18} />
              <h4>MCP + Plugins</h4>
              <p>Manage local compatibility registries for MCP and plugin entries with list, add, remove, and inspect flows.</p>
            </div>
            <div className="pillar-card">
              <Wrench size={18} />
              <h4>Hooks + Permissions</h4>
              <p>Define hook rules and enforce profile-level permission behavior without leaving command mode.</p>
            </div>
            <div className="pillar-card">
              <Puzzle size={18} />
              <h4>Skills + Agents + Tasks</h4>
              <p>Enable/disable skills and run automation-oriented agent, task, and plan commands from a unified syntax.</p>
            </div>
          </div>
        </section>

        <section className="doc-section">
          <h2>Representative Commands</h2>
          <CodeBlock
            lang="bash"
            code={`nexus mcp list
nexus mcp add --name local-fs --type stdio --command "node server.js"
nexus plugin list
nexus plugin add --name jira-sync --version 1.0.0
nexus hooks list
nexus hooks add --event pre-run --action "nexus security scan"
nexus permissions status
nexus skills list
nexus skills enable --name architecture-dna
nexus agents list
nexus tasks list
nexus plan run --goal "ship auth hardening"`}
          />
        </section>

        <section className="doc-section">
          <h2>Interactive Dashboard Access</h2>
          <Terminal
            lines={[
              '<span style="color: var(--text-muted)">[10] Compatibility Hub</span>',
              '<span style="color: var(--text-muted)">   MCP · Plugins · Hooks · Permissions · Skills · Agents · Tasks · Plan</span>',
              '<span style="color: var(--accent)">nexus@local > Select module: 10</span>',
            ]}
          />
          <Callout type="info">
            <strong>Storage:</strong> Local compatibility registries are persisted under <code>target/nexus-config</code> and
            loaded on demand by command handlers.
          </Callout>
          <Callout type="warning">
            <strong>Policy behavior:</strong> Permission and skill toggles are profile-driven. If your account uses restricted
            profile settings, some operations can be blocked by policy.
          </Callout>
        </section>

        <section className="doc-section">
          <div className="section-header-inline">
            <ShieldCheck size={20} color="var(--accent)" />
            <h2>Migration Notes</h2>
          </div>
          <p>
            The compatibility layer is intentionally alias-friendly. Slash forms and compatibility aliases normalize to
            the same internal action, so existing shell scripts can be adapted with minimal edits.
          </p>
          <CodeBlock
            lang="bash"
            code={`# Equivalent forms accepted by the dispatcher
nexus /mcp list
nexus mcp list
nexus /plan run --goal "reduce cost"`}
          />
        </section>
      </motion.div>
    </motion.div>
  );
}
