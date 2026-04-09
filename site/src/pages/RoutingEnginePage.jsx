import { motion } from 'framer-motion';
import { Helmet } from 'react-helmet-async';
import { Map, Activity, Clock, DollarSign, Target, RotateCcw, Zap } from 'lucide-react';
import { CodeBlock, Callout } from '../components/UI';

const container = { hidden: { opacity: 0 }, show: { opacity: 1, transition: { staggerChildren: 0.05 }}};
const item = { hidden: { opacity: 0, y: 10 }, show: { opacity: 1, y: 0 }};

const scoringFactors = [
  { label: 'Suitability',       weight: 45, color: '#E8745C', note: 'Expert-curated task-model fit. Set once per model per TaskType in model_suitability. Reflects domain strengths (e.g. Claude excels at CODE_GENERATION; Llama-3 at SUMMARIZATION).' },
  { label: 'Historical Quality', weight: 25, color: '#3b82f6', note: 'Aggregated quality_score from past outcome_memories for this user and model pair. Zero telemetry means this sub-score defaults to the suitability score.' },
  { label: 'Inverse Latency',   weight: 20, color: '#a855f7', note: 'Faster average response time from outcome_memories yields a higher sub-score. A model averaging 300ms scores higher than one averaging 4s.' },
  { label: 'Inverse Cost',      weight: 10, color: '#10b981', note: 'Lower cost_per_1k_tokens yields a higher sub-score. Derived from the llm_models registry, updated by MarketIntelligenceService.' },
];

const taskTypes = [
  { type: 'CODE_GENERATION',     bestFor: 'Claude 3.5 Sonnet, GPT-4o',   reason: 'High suitability for structured output, docstrings, and complex logic chains.' },
  { type: 'SUMMARIZATION',       bestFor: 'Llama-3 70b (Groq), Gemini Flash', reason: 'Fast and cheap for text compression tasks. Frontier models are overqualified.' },
  { type: 'REASONING',           bestFor: 'GPT-4o, Claude 3 Opus',        reason: 'Complex multi-step inference requires high token capacity and reasoning depth.' },
  { type: 'UNIT_TESTING',        bestFor: 'Claude 3.5 Sonnet',            reason: 'Structured test generation benefits from models with strong coding+documentation overlap.' },
  { type: 'GENERAL_KNOWLEDGE',   bestFor: 'Groq Llama-3, Gemini Flash',   reason: 'General Q&A is cheaply solvable. Using frontier models here is the primary source of budget waste.' },
  { type: 'DATA_EXTRACTION',     bestFor: 'GPT-4o, Gemini Pro',           reason: 'JSON-structured output from unstructured data requires strong instruction-following.' },
];

export function RoutingEnginePage() {
  return (
    <motion.div className="page" initial="hidden" animate="show" variants={container}>
      <Helmet>
        <title>Routing Engine — Nexus Autopilot</title>
        <meta name="description" content="Nexus Routing Engine: autonomous composite scoring for optimal LLM selection based on suitability, quality, latency, and cost." />
      </Helmet>

      <motion.div variants={item}>
        <div className="badge-premium">INTELLIGENCE CORE</div>
        <h1 className="page-title">The Routing Engine</h1>
        <p className="page-description">
          Nexus does not forward your prompt to a fixed model. It runs an autonomous arbitrage pass across every model you have an API key for, calculates a composite score, and dispatches to the best option — every time.
        </p>

        {/* ── The Problem ─────────────────────────── */}
        <section className="doc-section">
          <div className="section-header-inline">
            <Map size={20} color="var(--accent)" />
            <h2>The Problem It Solves</h2>
          </div>
          <p>
            Developers default to one model for everything — usually GPT-4o or Claude — because choosing manually is cognitive overhead. This leads to two failure modes: using expensive frontier models for trivial tasks (financial waste), and using cheap models for complex tasks (quality degradation).
          </p>
          <p style={{ marginTop: '12px' }}>
            The Routing Engine eliminates this by making the selection decision deterministically, using a weighted scoring function that accounts for the specific task type, the model's historical performance on your machine, and its current market cost.
          </p>
        </section>

        {/* ── Composite Score ──────────────────────── */}
        <section className="doc-section">
          <div className="section-header-inline">
            <Target size={20} color="var(--accent)" />
            <h2>The Composite Scoring Formula</h2>
          </div>
          <p>Every accessible model receives a normalized composite score between 0.0 and 1.0. The model with the highest score wins the dispatch.</p>

          <div style={{ border: '1px solid var(--border)', borderRadius: '12px', overflow: 'hidden', margin: '24px 0' }}>
            <div style={{ padding: '12px 20px', background: 'var(--bg-surface)', borderBottom: '1px solid var(--border)' }}>
              <code style={{ fontFamily: 'var(--mono)', fontSize: '13px', color: 'var(--text-dim)' }}>
                Score = (S × 0.45) + (Q × 0.25) + (L<sub>inv</sub> × 0.20) + (C<sub>inv</sub> × 0.10)
              </code>
            </div>
            {scoringFactors.map((f, i) => (
              <div key={f.label} style={{
                display: 'grid', gridTemplateColumns: '160px 50px 1fr 50px',
                gap: '20px', alignItems: 'center', padding: '16px 20px',
                background: 'var(--bg)',
                borderBottom: i < scoringFactors.length - 1 ? '1px solid rgba(255,255,255,0.04)' : 'none'
              }}>
                <span style={{ fontSize: '14px', fontWeight: 750, color: 'var(--text)' }}>{f.label}</span>
                <div style={{ height: '4px', background: 'rgba(255,255,255,0.06)', borderRadius: '2px', position: 'relative' }}>
                  <div style={{ position: 'absolute', top: 0, left: 0, height: '100%', width: `${f.weight / 45 * 100}%`, background: f.color, borderRadius: '2px' }} />
                </div>
                <span style={{ fontSize: '13px', color: 'var(--text-muted)', lineHeight: 1.6 }}>{f.note}</span>
                <span style={{ fontSize: '14px', fontWeight: 850, color: f.color, textAlign: 'right', fontFamily: 'var(--mono)' }}>{f.weight}%</span>
              </div>
            ))}
          </div>

          <Callout type="info">
            <strong>Actionability filter:</strong> Only models with a matching API key in the user's vault are scored. This ensures every recommendation is actually executable — no phantom options.
          </Callout>
        </section>

        {/* ── TaskType → Model Map ─────────────────── */}
        <section className="doc-section">
          <div className="section-header-inline">
            <Zap size={20} color="var(--accent)" />
            <h2>TaskType to Model Mapping</h2>
          </div>
          <p>
            The suitability scores in the registry are pre-seeded based on each model's documented strengths. The following shows the typical routing verdicts for each task type with a well-populated registry.
          </p>
          <table className="custom-table" style={{ marginTop: '24px' }}>
            <thead>
              <tr>
                <th>TaskType</th>
                <th>Typically Routes To</th>
                <th>Rationale</th>
              </tr>
            </thead>
            <tbody>
              {taskTypes.map(t => (
                <tr key={t.type}>
                  <td><code style={{ fontFamily: 'var(--mono)', fontSize: '12px', color: 'var(--accent)', background: 'var(--accent-dim)', padding: '2px 6px', borderRadius: '4px' }}>{t.type}</code></td>
                  <td style={{ fontSize: '13px', color: 'var(--text)', fontWeight: 600 }}>{t.bestFor}</td>
                  <td style={{ fontSize: '13px', lineHeight: 1.55 }}>{t.reason}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </section>

        {/* ── Autonomous Recalibration ─────────────── */}
        <section className="doc-section">
          <div className="section-header-inline">
            <RotateCcw size={20} color="var(--accent)" />
            <h2>Autonomous Recalibration</h2>
          </div>
          <p>
            The routing engine is not static. After each successful agent session closes, <code>recalibrateScores()</code> reads all recent <code>outcome_memories</code> and adjusts each model's effective suitability score in the registry based on real performance data. Models that consistently produce high-quality results rise. Models with deteriorating performance fall.
          </p>
          <CodeBlock
            lang="java"
            code={`// RoutingEngine.java — recalibrateScores()
Map<Integer, List<OutcomeMemory>> byModel = outcomes.stream()
    .collect(Collectors.groupingBy(OutcomeMemory::getModelId));

for (var entry : byModel.entrySet()) {
    double avgQuality = entry.getValue().stream()
        .mapToDouble(OutcomeMemory::getQualityScore).average().orElse(0.5);
    double avgLatency = entry.getValue().stream()
        .mapToLong(OutcomeMemory::getLatencyMs).average().orElse(2000);

    // Pull down poor performers, push up strong performers
    double delta = (avgQuality - 0.5) * 0.1;
    suitabilityDao.adjustBaseScore(entry.getKey(), currentTask, delta);
}`}
          />
        </section>

        {/* ── Agentic Decomposition ────────────────── */}
        <section className="doc-section">
          <div className="section-header-inline">
            <Activity size={20} color="var(--accent)" />
            <h2>Agentic Task Decomposition</h2>
          </div>
          <p>
            When the routing menu detects a compound prompt — one containing multiple distinct intent signals — it invokes <code>TaskPlannerService.plan()</code>, which decomposes the prompt into an ordered list of <code>TaskType</code> values. The routing engine then runs independently for each sub-task, potentially dispatching to different models in the same workflow.
          </p>
          <p style={{ marginTop: '12px', fontSize: '14px', color: 'var(--text-muted)' }}>
            Example: <em>"Build the UserDao class, then write unit tests for it"</em> decomposes to <code>[CODE_GENERATION, UNIT_TESTING]</code>. Claude handles generation; Claude also handles testing (but with a separate suitability scoring pass). Each produces its own session record with independent cost tracking.
          </p>
        </section>

        {/* ── Pillars ──────────────────────────────── */}
        <section className="doc-section">
          <h2>Design Principles</h2>
          <div className="pillars-grid">
            <div className="pillar-card">
              <DollarSign size={18} />
              <h4>Cost Intelligence</h4>
              <p>Routing decisions factor in live market pricing (synced from OpenRouter) so the cost component is never based on stale data.</p>
            </div>
            <div className="pillar-card">
              <Clock size={18} />
              <h4>Latency Awareness</h4>
              <p>Historical latency from outcome records is factored in, so consistently slow providers are deprioritized over time — automatically.</p>
            </div>
            <div className="pillar-card">
              <Activity size={18} />
              <h4>Self-Calibrating</h4>
              <p>The recalibration pass means the engine improves with every session closed. No configuration changes required — it learns from usage.</p>
            </div>
          </div>
        </section>
      </motion.div>
    </motion.div>
  );
}
