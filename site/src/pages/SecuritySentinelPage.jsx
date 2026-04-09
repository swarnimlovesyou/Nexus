import { motion } from 'framer-motion';
import { Helmet } from 'react-helmet-async';
import { Shield, AlertTriangle, Eye, Lock, FileSearch, Code2 } from 'lucide-react';
import { CodeBlock, Callout } from '../components/UI';

const container = { hidden: { opacity: 0 }, show: { opacity: 1, transition: { staggerChildren: 0.08 }}};
const item = { hidden: { opacity: 0, y: 12 }, show: { opacity: 1, y: 0 }};

const patterns = [
  { name: 'AWS Access Key',        regex: 'AKIA[0-9A-Z]{16}',                         severity: 'CRITICAL', color: '#ef4444', note: 'Active AWS IAM credentials. Exposure allows full account access.' },
  { name: 'OpenAI API Key',        regex: 'sk-[a-zA-Z0-9]{32,}',                      severity: 'CRITICAL', color: '#ef4444', note: 'Any sk- prefixed token. Also catches Anthropic and other providers using the same pattern.' },
  { name: 'Google API Key',        regex: 'AIza[0-9A-Za-z\\-_]{35}',                  severity: 'CRITICAL', color: '#ef4444', note: 'Google Cloud / Gemini API credentials.' },
  { name: 'Hardcoded IP Address',  regex: '\\b\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\b', severity: 'HIGH',     color: '#f59e0b', note: 'Private or public IPs in source code indicate hardcoded infrastructure dependencies.' },
  { name: 'SQL Injection Pattern', regex: "' OR '1'='1' | --",                        severity: 'HIGH',     color: '#f59e0b', note: 'Classic injection strings or comment sequences in source indicate unsafe string concatenation.' },
  { name: 'Generic Secret',        regex: 'password\\s*=\\s*["\'][^\'"]+["\']',        severity: 'MEDIUM',   color: '#a78bfa', note: 'Plaintext passwords assigned in source files.' },
];

export function SecuritySentinelPage() {
  return (
    <motion.div className="page" initial="hidden" animate="show" variants={container}>
      <Helmet>
        <title>Security Sentinel — Nexus Autopilot</title>
        <meta name="description" content="Nexus Security Sentinel: proactive workspace audit for leaked API keys, hardcoded IPs, and SQL injection patterns." />
      </Helmet>

      <motion.div variants={item}>
        <div className="badge-premium">INTELLIGENCE HUB</div>
        <h1 className="page-title">Security Sentinel</h1>
        <p className="page-description">
          A proactive, regex-based workspace auditor that scans every file in your project directory for leaked credentials, hardcoded infrastructure details, and high-risk code patterns — before they reach version control.
        </p>

        {/* ── Problem Statement ────────────────────── */}
        <section className="doc-section">
          <div className="section-header-inline">
            <AlertTriangle size={20} color="var(--accent)" />
            <h2>Why This Exists</h2>
          </div>
          <p>
            API key leakage through version control is one of the most common and costly developer security incidents. Secrets end up committed because they are written inline during development and forgotten before commit. By the time a secret is pushed to a public repository, the damage is done — the key must be rotated, and any systems that consumed it must be audited.
          </p>
          <p style={{ marginTop: '12px' }}>
            The Sentinel acts as a pre-commit gate that runs on demand, scanning the entire workspace without requiring a git hook or CI integration. It is local, instant, and generates a permanent audit record.
          </p>
        </section>

        {/* ── How It Works ─────────────────────────── */}
        <section className="doc-section">
          <div className="section-header-inline">
            <FileSearch size={20} color="var(--accent)" />
            <h2>How It Works</h2>
          </div>
          <p>On invocation from the Intelligence Hub, <code>SecuritySentinelService.performFullAudit()</code> executes the following steps:</p>
          <ol style={{ paddingLeft: '20px', display: 'flex', flexDirection: 'column', gap: '10px', marginTop: '16px' }}>
            {[
              'Walk the entire current working directory recursively using Java NIO file tree APIs.',
              'For every file, read its content and run each regex pattern in the threat library against it.',
              'Each match produces a SecurityFinding record: { type, file path, line content, severity }.',
              'All findings are returned to the CLI and displayed as a structured report with file and line references.',
              'Every scan result — clean or findings — is written to the audit_log table with action SECURITY_SCAN and user_id of the invoking user.',
            ].map((step, i) => (
              <li key={i} style={{ fontSize: '14px', color: 'var(--text-dim)', lineHeight: 1.65 }}>
                <strong style={{ color: 'var(--accent)' }}>{i + 1}.</strong> {step}
              </li>
            ))}
          </ol>
        </section>

        {/* ── Pattern Library ───────────────────────── */}
        <section className="doc-section">
          <div className="section-header-inline">
            <Eye size={20} color="var(--accent)" />
            <h2>Threat Pattern Library</h2>
          </div>
          <p>The Sentinel's detection library is a curated set of regex patterns covering the most common categories of credential exposure and code risk.</p>
          <table className="custom-table" style={{ marginTop: '24px' }}>
            <thead>
              <tr>
                <th>Threat</th>
                <th>Regex Pattern</th>
                <th>Severity</th>
                <th>Notes</th>
              </tr>
            </thead>
            <tbody>
              {patterns.map(p => (
                <tr key={p.name}>
                  <td style={{ fontWeight: 600 }}>{p.name}</td>
                  <td><code style={{ fontFamily: 'var(--mono)', fontSize: '11px', color: 'var(--text-dim)', background: 'rgba(255,255,255,0.04)', padding: '2px 6px', borderRadius: '3px' }}>{p.regex}</code></td>
                  <td><span style={{ fontSize: '11px', fontWeight: 750, color: p.color, background: `${p.color}15`, padding: '2px 8px', borderRadius: '4px', textTransform: 'uppercase', letterSpacing: '0.05em' }}>{p.severity}</span></td>
                  <td style={{ fontSize: '12.5px', color: 'var(--text-muted)', lineHeight: 1.55 }}>{p.note}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </section>

        {/* ── Audit Integration ─────────────────────── */}
        <section className="doc-section">
          <div className="section-header-inline">
            <Lock size={20} color="var(--accent)" />
            <h2>Audit Log Integration</h2>
          </div>
          <p>
            Every Security Sentinel scan, regardless of outcome, is written to the <code>audit_log</code> table with the action <code>SECURITY_SCAN</code>. This provides a historical record of when scans were run and whether findings were present.
          </p>
          <CodeBlock
            lang="java"
            code={`// SecuritySentinelService.java — performFullAudit()
List<SecurityFinding> findings = new ArrayList<>();

Files.walkFileTree(Paths.get("."), new SimpleFileVisitor<>() {
    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
        String content = Files.readString(file);
        for (ThreatPattern pattern : THREAT_PATTERNS) {
            Matcher m = pattern.regex().matcher(content);
            if (m.find()) {
                findings.add(new SecurityFinding(
                    pattern.type(), file.toString(), m.group(), pattern.severity()
                ));
            }
        }
        return FileVisitResult.CONTINUE;
    }
});

// Persist all findings to audit trail
auditLogDao.create(userId, "SECURITY_SCAN",
    "findings=" + findings.size() + ", path=" + scanRoot, "SUCCESS");`}
          />
        </section>

        {/* ── How to Run ───────────────────────────── */}
        <section className="doc-section">
          <div className="section-header-inline">
            <Code2 size={20} color="var(--accent)" />
            <h2>How to Invoke</h2>
          </div>
          <p>The Security Sentinel is available from the Intelligence Hub menu, which is accessible from the main Nexus dashboard.</p>
          <div style={{ margin: '24px 0', padding: '20px', border: '1px solid var(--border)', borderRadius: '10px', background: 'rgba(0,0,0,0.15)', fontFamily: 'var(--mono)', fontSize: '13px', lineHeight: 2 }}>
            <div style={{ color: 'var(--text-muted)' }}>nexus@local &gt; Main Dashboard</div>
            <div style={{ color: 'var(--accent)' }}>Press: <strong>I</strong>  →  Intelligence Hub</div>
            <div style={{ color: 'var(--text-muted)' }}>Press: <strong>2</strong>  →  Security Sentinel Scan</div>
            <div style={{ color: '#10b981', marginTop: '8px' }}>Output: Structured findings report + audit_log entry written</div>
          </div>
          <Callout type="warning">
            <strong>Scope:</strong> The Sentinel scans from the current working directory where the JAR is executed. Run Nexus from the root of your project to ensure full coverage.
          </Callout>
        </section>
      </motion.div>
    </motion.div>
  );
}
