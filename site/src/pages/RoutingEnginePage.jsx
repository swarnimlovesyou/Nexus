import { motion } from 'framer-motion';
import { Helmet } from 'react-helmet-async';
import { Map, Clock, DollarSign, Target, ArrowRight } from 'lucide-react';
import { CodeBlock, Callout } from '../components/UI';

const container = { hidden: { opacity: 0 }, show: { opacity: 1, transition: { staggerChildren: 0.05 }}};
const item = { hidden: { opacity: 0, y: 10 }, show: { opacity: 1, y: 0 }};

export function RoutingEnginePage() {
  return (
    <motion.div className="page" initial="hidden" animate="show" variants={container}>
      <Helmet>
        <title>Routing Engine — Nexus</title>
      </Helmet>

      <motion.div variants={item}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '8px' }}>
          <Map size={24} color="var(--accent)" />
          <h1 style={{ margin: 0 }}>The Routing Signal</h1>
        </div>
        
        <p className="hero-sub" style={{ fontSize: '16px', color: 'var(--text-muted)' }}>
          Think of Nexus as a <strong>GPS for your prompts</strong>. Instead of sending every request down the same expensive highway, Nexus finds the fastest, cheapest, and most accurate route for every specific task.
        </p>

        <h2 style={{ marginTop: '40px' }}>How it Works (The Layman Guide)</h2>
        <p>
          When you ask an AI a question, it's like sending a package across the world. Some packages are urgent and important (like a legal contract review), while others are simple and casual (like "summarize this email"). 
        </p>
        
        <div className="feature-grid" style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))', gap: '16px', marginTop: '24px' }}>
          <div className="callout-premium">
            <Target size={16} color="var(--accent)" />
            <div style={{ marginTop: '6px' }}>
              <strong style={{ display: 'block', fontSize: '14px', marginBottom: '2px' }}>Suitability</strong>
              <p style={{ fontSize: '12.5px', color: 'var(--text-muted)', margin: 0 }}>Is the model smart enough for this? We don't use a bulldozer to plant a flower.</p>
            </div>
          </div>
          <div className="callout-premium">
            <Clock size={16} color="var(--accent)" />
            <div style={{ marginTop: '6px' }}>
              <strong style={{ display: 'block', fontSize: '14px', marginBottom: '2px' }}>Latency</strong>
              <p style={{ fontSize: '12.5px', color: 'var(--text-muted)', margin: 0 }}>Is the model having "traffic" issues? We route away from slow providers in real-time.</p>
            </div>
          </div>
          <div className="callout-premium">
            <DollarSign size={16} color="var(--accent)" />
            <div style={{ marginTop: '6px' }}>
              <strong style={{ display: 'block', fontSize: '14px', marginBottom: '2px' }}>Cost Efficiency</strong>
              <p style={{ fontSize: '12.5px', color: 'var(--text-muted)', margin: 0 }}>Why pay for a first-class ticket when a bike ride arrives at the same time? Nexus picks the best price-to-performance ratio.</p>
            </div>
          </div>
        </div>

        <h2 style={{ marginTop: '48px' }}>The Composite Score Math</h2>
        <p>
          For the technically inclined, Nexus uses a normalized arithmetic mean across four primary telemetry signals. This creates a "Verity Score" from 0.0 to 1.0.
        </p>
        
        <CodeBlock 
          lang="python" 
          code={`# How Nexus determines the 'winner'
def calculate_verity_score(model):
    s = model.suitability * 0.40 # IQ weight
    q = model.historical_quality * 0.30 # Reliability weight
    l = (1 / model.latency) * 0.20 # Speed weight
    c = (1 / model.cost) * 0.10 # Price weight
    
    return s + q + l + c # Final Composite Score`} 
        />

        <Callout type="info">
          <strong>Proactive Load Balancing:</strong> If multiple models have identical scores, Nexus will load-balance your requests locally to avoid rate-limiting from a single provider.
        </Callout>

        <div style={{ marginTop: '40px', padding: '24px', background: 'var(--bg-surface)', border: '1px solid var(--border)', borderRadius: '8px' }}>
          <h3>Example: "A Simple Greeting"</h3>
          <p style={{ fontSize: '13.5px', color: 'var(--text-dim)' }}>
            <strong>Task:</strong> "Say hello to the user."<br/>
            <strong>Nexus Logic:</strong> Complexity is near zero. IQ requirements are low. <br/>
            <strong>Verdict:</strong> Route to <strong>Groq/Llama-3</strong> (Instant & Near-Free) instead of <strong>GPT-4o</strong> (Slow & Expensive).
          </p>
          <div style={{ color: 'var(--green)', fontSize: '12px', fontWeight: '650', marginTop: '8px' }}>
            Result: 85% Cost Reduction · 0.1s Result
          </div>
        </div>
      </motion.div>
    </motion.div>
  );
}
