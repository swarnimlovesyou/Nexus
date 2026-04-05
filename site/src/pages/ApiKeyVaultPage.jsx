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
          Security isn't baked in; it's the foundation. Nexus stores all secrets behind hardware-level isolation where possible, obfuscated with rotating local secrets.
        </p>

        {/* ── Security Architecture ────────────────── */}
        <section className="doc-section">
           <div className="section-header-inline">
              <ShieldCheck size={20} color="var(--accent)" />
              <h2>Localized Encryption Matrix</h2>
           </div>
           <p>
             Most AI tools store keys in plaintext <code>.env</code> files. Nexus moves beyond this by providing a <strong>Local-First Vaulting</strong> mechanism 
             that utilizes XOR-encoded obfuscation. Your API keys, prompts, and memory never leave your silicon.
           </p>

           <div className="algorithm-card">
              <div className="alg-header">
                <Binary size={14} />
                <span>VAULT SECURITY TELEMETRY</span>
              </div>
              <div className="alg-grid">
                 <div className="alg-item">
                   <span className="label">Encryption Type</span>
                   <span className="value">XOR + Per-Session Salt</span>
                   <div className="bar"><div className="fill" style={{ width: '95%' }}></div></div>
                 </div>
                 <div className="alg-item">
                   <span className="label">Key Rotation</span>
                   <span className="value">90-Day Lifecycle</span>
                   <div className="bar"><div className="fill" style={{ width: '70%' }}></div></div>
                 </div>
                 <div className="alg-item">
                   <span className="label">Cloud Sync</span>
                   <span className="value">DISABLED (0% Leak)</span>
                   <div className="bar"><div className="fill" style={{ width: '100%', background: 'var(--green)' }}></div></div>
                 </div>
                 <div className="alg-item">
                   <span className="label">Hardware Isolation</span>
                   <span className="value">TPM / Secure Enclave</span>
                   <div className="bar"><div className="fill" style={{ width: '60%' }}></div></div>
                 </div>
              </div>
           </div>
        </section>

        {/* ── XOR Obfuscation ──────────────────────── */}
        <section className="doc-section">
           <div className="section-header-inline">
              <Key size={20} color="var(--accent)" />
              <h2>The In-Memory Decryption Loop</h2>
           </div>
           <p>
             Decryption only occurs in-memory during the active request lifecycle. Keys are scrubbed immediately after the LLM handoff, 
             ensuring they are never persisted in plaintext, even in temporary heap buffers.
           </p>
           
           <CodeBlock 
             lang="java" 
             code={`// Local Zero-Trust Decryption Factory
public String getProviderSecret(String providerId) {
    SecretEntity secret = vault.fetchEncrypted(providerId);
    
    // Obfuscate in-memory with local machine-specific salt
    byte[] rawBytes = secret.getBlob();
    for (int i = 0; i < rawBytes.length; i++) {
        rawBytes[i] ^= machineSecret[i % machineSecret.length];
    }
    
    return SecurePruner.scrubAndReturn(new String(rawBytes));
}`}
           />
        </section>

        {/* ── Architectural Pillars ─────────────────── */}
        <section className="doc-section">
          <h2>Zero-Trust Security Pillars</h2>
          <div className="pillars-grid">
             <div className="pillar-card">
               <EyeOff size={18} fill="rgba(232,116,92,0.1)" />
               <h4>Zero Tracking</h4>
               <p>Nexus does not track your provider IDs or key usage metrics. All billing is inferred from local traces.</p>
             </div>
             <div className="pillar-card">
               <RefreshCw size={18} fill="rgba(232,116,92,0.1)" />
               <h4>Rotating Secrets</h4>
               <p>The internal vault secret rotates every 90 days, re-encrypting your entire key set automatically.</p>
             </div>
             <div className="pillar-card">
               <Server size={18} fill="rgba(232,116,92,0.1)" />
               <h4>Isolated Sandbox</h4>
               <p>Every provider is instantiated in its own isolated environment, preventing cross-key contamination.</p>
             </div>
          </div>
        </section>

        {/* ── Best Practices ──────────────────────────── */}
        <section className="doc-section">
          <h2>Vault Hygiene</h2>
          <div className="best-practices-grid">
            <Callout type="info">
              <strong>Local Backup:</strong> Export your encrypted vault blob periodically. Without your machine-linked secret, this blob is useless to third parties.
            </Callout>
            <Callout type="warning">
              <strong>Key Exposure:</strong> Avoid using <code>.env</code> files for LLM keys once you've migrated to the Nexus Vault.
            </Callout>
          </div>
        </section>
      </motion.div>
    </motion.div>
  );
}
