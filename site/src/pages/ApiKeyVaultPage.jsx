import { motion } from 'framer-motion';
import { Helmet } from 'react-helmet-async';
import { ShieldCheck, Lock, ShieldAlert, Binary, ArrowRight, Shield, Key, RefreshCw, EyeOff, Server, Cpu } from 'lucide-react';
import { CodeBlock, Callout } from '../components/UI';

const container = { hidden: { opacity: 0 }, show: { opacity: 1, transition: { staggerChildren: 0.1 }}};
const item = { hidden: { opacity: 0, y: 15 }, show: { opacity: 1, y: 0 }};

export function ApiKeyVaultPage() {
  return (
    <motion.div className="page" initial="hidden" animate="show" variants={container}>
      <Helmet>
        <title>API Vault — Nexus Autopilot</title>
      </Helmet>

      <motion.div variants={item}>
        <div className="badge-premium">SECURITY ARCHITECTURE</div>
        <h1 className="page-title">Zero-Trust API Key Vault</h1>
        <p className="page-description">
          Nexus stores provider keys locally and masks them in UI. Keys are XOR-obfuscated in SQLite for convenience and local workflow safety.
        </p>

        {/* ── Security Architecture ────────────────── */}
        <section className="doc-section">
           <div className="section-header-inline">
              <ShieldCheck size={20} color="var(--accent)" />
              <h2>Local Key Storage Model</h2>
           </div>
           <p>
             Nexus avoids scattering provider keys across shells and project files by storing them centrally in SQLite.
             Keys are masked in UI and XOR-obfuscated at rest. This is practical local key management, not a replacement for enterprise KMS.
           </p>

           <div className="algorithm-card">
              <div className="alg-header">
                <Binary size={14} />
                <span>VAULT SECURITY TELEMETRY</span>
              </div>
              <div className="alg-grid">
                 <div className="alg-item">
                   <span className="label">Encryption Type</span>
                   <span className="value">XOR + Base64 obfuscation</span>
                   <div className="bar"><div className="fill" style={{ width: '55%' }}></div></div>
                 </div>
                 <div className="alg-item">
                   <span className="label">Display</span>
                   <span className="value">Masked key view (e.g. sk-...)</span>
                   <div className="bar"><div className="fill" style={{ width: '80%' }}></div></div>
                 </div>
                 <div className="alg-item">
                   <span className="label">Cloud Sync</span>
                   <span className="value">Not implemented</span>
                   <div className="bar"><div className="fill" style={{ width: '100%', background: 'var(--green)' }}></div></div>
                 </div>
                 <div className="alg-item">
                   <span className="label">Routing Integration</span>
                   <span className="value">Provider-key aware recommendations</span>
                   <div className="bar"><div className="fill" style={{ width: '85%' }}></div></div>
                 </div>
              </div>
           </div>
        </section>

        {/* ── XOR Obfuscation ──────────────────────── */}
        <section className="doc-section">
           <div className="section-header-inline">
              <Key size={20} color="var(--accent)" />
              <h2>Encode/Decode Flow</h2>
           </div>
           <p>
             When executing a live provider call, Nexus decodes the selected provider key in memory for request authorization.
             The persisted DB value remains obfuscated.
           </p>
           
           <CodeBlock 
             lang="java" 
             code={`// Local Zero-Trust Decryption Factory
      public String xorDecode(String encodedKey) {
        byte[] bytes = Base64.getDecoder().decode(encodedKey);
        for (int i = 0; i < bytes.length; i++) {
          bytes[i] ^= XOR_KEY;
        }
        return new String(bytes);
}`}
           />
        </section>

        {/* ── Architectural Pillars ─────────────────── */}
        <section className="doc-section">
          <h2>Zero-Trust Security Pillars</h2>
          <div className="pillars-grid">
             <div className="pillar-card">
               <EyeOff size={18} fill="rgba(232,116,92,0.1)" />
               <h4>Centralized Local Vault</h4>
               <p>Add each provider key once and reuse it across routing/live-call features in the CLI.</p>
             </div>
             <div className="pillar-card">
               <RefreshCw size={18} fill="rgba(232,116,92,0.1)" />
               <h4>Ownership Checks</h4>
               <p>Delete operations enforce user ownership so one user cannot remove another user's key.</p>
             </div>
             <div className="pillar-card">
               <Server size={18} fill="rgba(232,116,92,0.1)" />
               <h4>Actionable Routing</h4>
               <p>Recommendations can be filtered by key availability so selected models are actually callable.</p>
             </div>
          </div>
        </section>

        {/* ── Best Practices ──────────────────────────── */}
        <section className="doc-section">
          <h2>Vault Hygiene</h2>
          <div className="best-practices-grid">
            <Callout type="info">
              <strong>Practical note:</strong> XOR obfuscation is lightweight. For high-security environments, integrate a dedicated secret manager.
            </Callout>
            <Callout type="warning">
              <strong>Operational hygiene:</strong> Prefer vault-managed keys over scattered per-project secrets to reduce key confusion.
            </Callout>
          </div>
        </section>
      </motion.div>
    </motion.div>
  );
}
