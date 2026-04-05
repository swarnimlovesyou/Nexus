import { motion } from 'framer-motion';
import { Helmet } from 'react-helmet-async';
import { Wallet, BarChart3, TrendingDown, Target, ArrowRight, Activity, PieChart, TrendingUp, ShieldCheck } from 'lucide-react';
import { CodeBlock, Callout } from '../components/UI';

const container = { hidden: { opacity: 0 }, show: { opacity: 1, transition: { staggerChildren: 0.1 }}};
const item = { hidden: { opacity: 0, y: 15 }, show: { opacity: 1, y: 0 }};

export function FinancialPage() {
  return (
    <motion.div className="page" initial="hidden" animate="show" variants={container}>
      <Helmet>
        <title>Financial Intel — Nexus Autopilot</title>
      </Helmet>

      <motion.div variants={item}>
        <div className="badge-premium">ECONOMY ANALYTICS</div>
        <h1 className="page-title">Financial Intelligence</h1>
        <p className="page-description">
          Analyze recorded spend, compare it with lower-cost viable routing options, and break down costs by model and task.
        </p>

        {/* ── Cost Efficiency ─────────────────────── */}
        <section className="doc-section">
           <div className="section-header-inline">
              <PieChart size={20} color="var(--accent)" />
              <h2>Token Economy & Real-Time Intel</h2>
           </div>
           <p>
             Every token has a price. Nexus provides granular visibility into your AI expenditure using the same USD values stored in execution outcomes.
           </p>

           <div className="algorithm-card">
              <div className="alg-header">
                <Activity size={14} />
                <span>LOCAL ECONOMY TELEMETRY (MONTHLY)</span>
              </div>
              <div className="alg-grid">
                 <div className="alg-item">
                   <span className="label">Total Expenditure</span>
                   <span className="value">$154.22</span>
                   <div className="bar"><div className="fill" style={{ width: '65%' }}></div></div>
                 </div>
                 <div className="alg-item">
                   <span className="label">Optimal Savings</span>
                   <span className="value">$43.81</span>
                   <div className="bar"><div className="fill" style={{ width: '85%', background: 'var(--green)' }}></div></div>
                 </div>
                 <div className="alg-item">
                   <span className="label">Avg. Call Cost</span>
                   <span className="value">$0.019</span>
                   <div className="bar"><div className="fill" style={{ width: '25%' }}></div></div>
                 </div>
                 <div className="alg-item">
                   <span className="label">Budget Utilization</span>
                   <span className="value">72%</span>
                   <div className="bar"><div className="fill" style={{ width: '72%' }}></div></div>
                 </div>
              </div>
           </div>
        </section>

        {/* ── Model Analytics ──────────────────────── */}
        <section className="doc-section">
           <h2>Model Pricing Adjudication</h2>
            <p>Nexus compares model usage from recorded outcomes and highlights where lower-cost options could have satisfied quality constraints.</p>
           
           <div className="table-responsive">
              <table className="custom-table" style={{ width: '100%', marginTop: '16px', borderCollapse: 'collapse' }}>
                <thead>
                  <tr style={{ textAlign: 'left', borderBottom: '1px solid var(--border)' }}>
                    <th style={{ padding: '12px', fontSize: '11px', color: 'var(--text-muted)' }}>MODEL PROVIDER</th>
                    <th style={{ padding: '12px', fontSize: '11px', color: 'var(--text-muted)' }}>CALLS</th>
                    <th style={{ padding: '12px', fontSize: '11px', color: 'var(--text-muted)' }}>AVG. COST (1k)</th>
                    <th style={{ padding: '12px', fontSize: '11px', color: 'var(--text-muted)' }}>SAVINGS (VS DEFAULT)</th>
                  </tr>
                </thead>
                <tbody>
                  {[
                    { provider: 'OpenAI GPT-4o', calls: '241', cost: '$0.0050', savings: '$19.44' },
                    { provider: 'Anthropic Claude-3.5', calls: '110', cost: '$0.0030', savings: '$11.28' },
                    { provider: 'Groq Llama-3-70b', calls: '421', cost: '$0.0008', savings: '$25.60' },
                    { provider: 'Gemini 1.5 Pro', calls: '89', cost: '$0.00125', savings: '$4.33' },
                  ].map((row, i) => (
                    <tr key={i} style={{ borderBottom: '1px solid var(--border)' }}>
                      <td style={{ padding: '12px', color: 'var(--text)', fontWeight: '850', fontSize: '14px' }}>{row.provider}</td>
                      <td style={{ padding: '12px', color: 'var(--text-dim)', fontSize: '13.5px' }}>{row.calls}</td>
                      <td style={{ padding: '12px', color: 'var(--text-dim)', fontSize: '13.5px' }}>{row.cost}</td>
                      <td style={{ padding: '12px', color: 'var(--green)', fontWeight: '850', fontSize: '13.5px' }}>+{row.savings}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
           </div>
        </section>

        {/* ── Predict Budgeting ─────────────────────── */}
        <section className="doc-section">
           <div className="section-header-inline">
              <TrendingDown size={20} color="var(--accent)" />
              <h2>Cost Analysis Logic</h2>
           </div>
           <p>
             Financial analysis is computed from stored execution outcomes. Nexus reports actual spend, estimated optimal spend, and avoidable spend.
           </p>
           
           <CodeBlock 
             lang="java" 
             code={`// Spend analysis from recorded outcomes
double actualSpend = history.stream().mapToDouble(OutcomeMemory::getCost).sum();
double optimalSpend = estimateOptimalSpend(history, qualityThreshold);
double avoidableSpend = Math.max(0.0, actualSpend - optimalSpend);

System.out.printf("Actual: $%.6f, Optimal: $%.6f, Avoidable: $%.6f", 
    actualSpend, optimalSpend, avoidableSpend);`}
           />
        </section>

        {/* ── Architectural Pillars ─────────────────── */}
        <section className="doc-section">
          <h2>Cost Insights</h2>
          <div className="pillars-grid">
             <div className="pillar-card">
               <ShieldCheck size={18} fill="rgba(232,116,92,0.1)" />
               <h4>Savings Visibility</h4>
               <p>See exactly how much spend could have been avoided given your historical task and model distribution.</p>
             </div>
             <div className="pillar-card">
               <TrendingUp size={18} fill="rgba(232,116,92,0.1)" />
               <h4>Range-Based Views</h4>
               <p>Review lifetime, 7-day, or 30-day windows to spot current trends without losing long-term context.</p>
             </div>
             <div className="pillar-card">
               <LayoutGrid size={18} fill="rgba(232,116,92,0.1)" />
               <h4>Granular Billing</h4>
               <p>Analyze economy metrics per-user, per-model, and per-task type using local persisted outcomes.</p>
             </div>
          </div>
        </section>

        {/* ── Best Practices ──────────────────────────── */}
        <section className="doc-section">
          <h2>Financial Constraints</h2>
          <div className="best-practices-grid">
            <Callout type="info">
              <strong>Currency Setup:</strong> Runtime analytics use USD values persisted from outcome records.
            </Callout>
            <Callout type="warning">
              <strong>Hard Limits:</strong> Set a practical monthly cap in your workflow and run the dashboard weekly to keep spend intentional.
            </Callout>
          </div>
        </section>
      </motion.div>
    </motion.div>
  );
}

const LayoutGrid = ({ size, fill }) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill={fill} stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><rect x="3" y="3" width="7" height="7"></rect><rect x="14" y="3" width="7" height="7"></rect><rect x="14" y="14" width="7" height="7"></rect><rect x="3" y="14" width="7" height="7"></rect></svg>
);
