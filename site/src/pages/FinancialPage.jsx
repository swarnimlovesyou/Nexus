import { motion } from 'framer-motion';
import { Helmet } from 'react-helmet-async';
import { Wallet, BarChart3, TrendingDown, Target, ArrowRight, IndianRupee, Activity, PieChart, TrendingUp, ShieldCheck } from 'lucide-react';
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
          Real-time economy analytics visualizing cost-vectors and providing predictive budgeting for high-scale agent deployments.
        </p>

        {/* ── Cost Efficiency ─────────────────────── */}
        <section className="doc-section">
           <div className="section-header-inline">
              <PieChart size={20} color="var(--accent)" />
              <h2>Token Economy & Real-Time Intel</h2>
           </div>
           <p>
             Every token has a price. Nexus provides granular visibility into your AI expenditure, adjudicating model pricing in <strong>Indian Rupees (₹)</strong> to ensure 
             localized financial clarity for your agentive operations.
           </p>

           <div className="algorithm-card">
              <div className="alg-header">
                <Activity size={14} />
                <span>LOCAL ECONOMY TELEMETRY (MONTHLY)</span>
              </div>
              <div className="alg-grid">
                 <div className="alg-item">
                   <span className="label">Total Expenditure</span>
                   <span className="value">₹12,402.00</span>
                   <div className="bar"><div className="fill" style={{ width: '65%' }}></div></div>
                 </div>
                 <div className="alg-item">
                   <span className="label">Optimal Savings</span>
                   <span className="value">₹4,281.50</span>
                   <div className="bar"><div className="fill" style={{ width: '85%', background: 'var(--green)' }}></div></div>
                 </div>
                 <div className="alg-item">
                   <span className="label">Avg. Call Cost</span>
                   <span className="value">₹0.42</span>
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
           <p>Nexus analyzes the <strong>Cost-to-Quality Ratio (CQR)</strong> for every configured provider, routing tasks to the most economical high-performance model.</p>
           
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
                    { provider: 'OpenAI GPT-4o', calls: '2,401', cost: '₹0.42', savings: '₹10,290' },
                    { provider: 'Anthropic Claude-3.5', calls: '1,102', cost: '₹0.25', savings: '₹3,528' },
                    { provider: 'Groq Llama-3 (vLLM)', calls: '8,421', cost: '₹0.008', savings: '₹26,104' },
                    { provider: 'Mistral-Large', calls: '402', cost: '₹0.67', savings: '₹998' },
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
              <h2>Predictive Budgeting</h2>
           </div>
           <p>
             The verity engine calculates your <strong>Cost Epochs</strong>, predicting when you might exceed your monthly threshold based on current agentive velocity.
           </p>
           
           <CodeBlock 
             lang="java" 
             code={`// Predictive cost epoch analysis
double daily_velocity = currentEpoch.getTokens() / daysActive;
double estimated_eom_bill = daily_velocity * 30 * avgTokenRateINR;

if (estimated_eom_bill > userBudgetINR) {
    nexus.throttle(ThrottlingStrategy.CONSERVATIVE_SAVE);
    System.out.println("ALERT: Predictive budget overflow detected.");
}`}
           />
        </section>

        {/* ── Architectural Pillars ─────────────────── */}
        <section className="doc-section">
          <h2>Predictive Intelligence</h2>
          <div className="pillars-grid">
             <div className="pillar-card">
               <ShieldCheck size={18} fill="rgba(232,116,92,0.1)" />
               <h4>Zero Debt Policy</h4>
               <p>Nexus pauses all higher-cost models instantly if budgets are breached, forcing a hard fallback.</p>
             </div>
             <div className="pillar-card">
               <TrendingUp size={18} fill="rgba(232,116,92,0.1)" />
               <h4>Margin Optimization</h4>
               <p>Maximize your agent's ROI by ensuring every token is bought at the best possible market rate.</p>
             </div>
             <div className="pillar-card">
               <LayoutGrid size={18} fill="rgba(232,116,92,0.1)" />
               <h4>Granular Billing</h4>
               <p>Analyze economy metrics per-project, per-user, or per-agent shard with zero cloud leak.</p>
             </div>
          </div>
        </section>

        {/* ── Best Practices ──────────────────────────── */}
        <section className="doc-section">
          <h2>Financial Constraints</h2>
          <div className="best-practices-grid">
            <Callout type="info">
              <strong>Currency Setup:</strong> All metrics are normalized to <code>INR (₹)</code> using a daily updated exchange rate buffer.
            </Callout>
            <Callout type="warning">
              <strong>Hard Limits:</strong> We recommend setting your <code>monthly_cap_inr</code> to at least 15% above your expected usage to prevent service interruption.
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
