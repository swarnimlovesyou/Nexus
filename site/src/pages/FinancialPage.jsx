import { motion } from 'framer-motion';
import { Helmet } from 'react-helmet-async';
import { Wallet, TrendingDown, Target, ArrowRight, Activity, PieChart, TrendingUp, ShieldCheck, Globe, Zap, Search } from 'lucide-react';
import { CodeBlock, Callout } from '../components/UI';
import { Link } from 'react-router-dom';

const container = { hidden: { opacity: 0 }, show: { opacity: 1, transition: { staggerChildren: 0.1 }}};
const item = { hidden: { opacity: 0, y: 15 }, show: { opacity: 1, y: 0 }};

export function FinancialPage() {
  return (
    <motion.div className="page" initial="hidden" animate="show" variants={container}>
      <Helmet>
        <title>Financial Intel — Nexus Autopilot</title>
        <meta name="description" content="Nexus Financial Intelligence: track spend, analyze cost savings, and sync live market prices from OpenRouter." />
      </Helmet>

      <motion.div variants={item}>
        <div className="badge-premium">ECONOMY ANALYTICS</div>
        <h1 className="page-title">Financial Intelligence</h1>
        <p className="page-description">
          Analyze recorded spend, compare it with lower-cost viable routing options, and ground your decisions in real-time market pricing — all from your local machine.
        </p>

        {/* ── Cost Efficiency ─────────────────────── */}
        <section className="doc-section">
           <div className="section-header-inline">
              <PieChart size={20} color="var(--accent)" />
              <h2>Token Economy & Real-Time Intel</h2>
           </div>
           <p>
             Every token has a price. Nexus provides granular visibility into your AI expenditure by aggregating the USD costs persisted in every <code>outcome_memory</code> record. This isn't just a total; it's a diagnostic of your routing efficiency.
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
                   <span className="label">Potential Savings</span>
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
           <div className="section-header-inline">
              <TrendingUp size={20} color="var(--accent)" />
              <h2>Model Pricing Adjudication</h2>
           </div>
            <p>Nexus compares actual model usage from recorded outcomes and highlights where "What-If" routing — choosing the cheapest viable model instead of the highest score — would have impacted your bottom line.</p>
           
           <div className="table-responsive">
              <table className="custom-table" style={{ width: '100%', marginTop: '16px' }}>
                <thead>
                  <tr>
                    <th>MODEL PROVIDER</th>
                    <th>TOTAL CALLS</th>
                    <th>AVG. COST (1k)</th>
                    <th>TOTAL SAVINGS</th>
                  </tr>
                </thead>
                <tbody>
                  {[
                    { provider: 'Anthropic Claude-3.5 Sonnet', calls: '241', cost: '$0.0030', savings: '$24.44' },
                    { provider: 'OpenAI GPT-4o', calls: '110', cost: '$0.0050', savings: '$11.28' },
                    { provider: 'Groq Llama-3-70b', calls: '421', cost: '$0.0008', savings: '$45.60' },
                    { provider: 'Gemini 1.5 Pro', calls: '89', cost: '$0.00125', savings: '$9.33' },
                  ].map((row, i) => (
                    <tr key={i}>
                      <td style={{ fontWeight: '850' }}>{row.provider}</td>
                      <td>{row.calls}</td>
                      <td>{row.cost}</td>
                      <td style={{ color: 'var(--green)', fontWeight: '850' }}>+{row.savings}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
           </div>
        </section>

        {/* ── Market Grounding ─────────────────────── */}
        <section className="doc-section">
           <div className="section-header-inline">
              <Globe size={20} color="var(--accent)" />
              <h2>Market Intelligence Grounding</h2>
           </div>
           <p>
             Cost metrics are only as good as the pricing data behind them. The <Link to="/market-intel" style={{ color: 'var(--accent)', textDecoration: 'underline' }}>Market Intelligence</Link> service keeps your local registry synced with live OpenRouter pricing, ensuring that "Savings" calculations are grounded in current market reality.
           </p>
           <Callout type="info">
             <strong>Feature Link:</strong> Run the Market Reality Check from the Intelligence Hub to update your <code>llm_models</code> registry prices before generating a financial report.
           </Callout>
        </section>

        {/* ── Analysis Logic ───────────────────────── */}
        <section className="doc-section">
           <div className="section-header-inline">
              <TrendingDown size={20} color="var(--accent)" />
              <h2>Cost Analysis Logic</h2>
           </div>
           <p>
             The financial dashboard computes metrics by joining the <code>agent_sessions</code> and <code>memories</code> tables (specifically <code>outcome_memory</code> records). It calculates actual vs. optimal spend per session.
           </p>
           
           <CodeBlock 
             lang="java" 
             code={`// FinanceService.java — generateReport()
double totalActual = outcomes.stream().mapToDouble(OutcomeMemory::getCost).sum();

// Optimal spend: what we would have paid if we routed to the 
// cheapest model that still met the QUALITY threshold of 0.8.
double totalOptimal = outcomes.stream()
    .mapToDouble(o -> getCheapestViablePrice(o.getTaskType(), 0.8))
    .sum();

double savingsUnrealized = totalActual - totalOptimal;
System.out.printf("Efficiency: %.1f%% | Potential Savings: $%.2f", 
    (totalOptimal / totalActual) * 100, savingsUnrealized);`}
           />
        </section>

        {/* ── Architectural Pillars ─────────────────── */}
        <section className="doc-section">
          <h2>Financial Pillars</h2>
          <div className="pillars-grid">
             <div className="pillar-card">
               <ShieldCheck size={18} />
               <h4>Zero Hidden Spend</h4>
               <p>Every token generated by an agent is cost-calculated locally. No mystery bills at the end of the month.</p>
             </div>
             <div className="pillar-card">
               <Zap size={18} />
               <h4>Arbitrage Discovery</h4>
               <p>Identify which task types (e.g. SUMMARIZATION) are consistently routing to over-expensive models and adjust registry weights.</p>
             </div>
             <div className="pillar-card">
               <Search size={18} />
               <h4>Per-User Accounting</h4>
               <p>Analyze spend and savings across different developers to identify high-efficiency workflows and budget waste.</p>
             </div>
          </div>
        </section>

        {/* ── Best Practices ──────────────────────────── */}
        <section className="doc-section">
          <div className="best-practices-grid">
            <Callout type="info">
              <strong>Reporting Range:</strong> Generate reports for the last 24h, 7 days, or 30 days via the CLI to track immediate impact of routing changes.
            </Callout>
            <Callout type="warning">
              <strong>Cost Ceiling:</strong> Nexus does not block calls based on budget yet. Use the dashboard to manually monitor spend against your personal threshold.
            </Callout>
          </div>
        </section>
      </motion.div>
    </motion.div>
  );
}
