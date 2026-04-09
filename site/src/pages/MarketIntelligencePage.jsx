import { motion } from 'framer-motion';
import { Helmet } from 'react-helmet-async';
import { Globe, RefreshCw, DollarSign, TrendingDown, Code2, AlertTriangle } from 'lucide-react';
import { CodeBlock, Callout } from '../components/UI';

const container = { hidden: { opacity: 0 }, show: { opacity: 1, transition: { staggerChildren: 0.08 }}};
const item = { hidden: { opacity: 0, y: 12 }, show: { opacity: 1, y: 0 }};

const samplePrices = [
  { model: 'openai/gpt-4o',                 live: '$0.0050', local: '$0.0050', status: 'current' },
  { model: 'anthropic/claude-3-5-sonnet',   live: '$0.0030', local: '$0.0035', status: 'updated' },
  { model: 'google/gemini-1.5-pro',         live: '$0.00125',local: '$0.00125',status: 'current' },
  { model: 'meta-llama/llama-3-70b-instruct', live: '$0.0008', local: '$0.0010', status: 'updated' },
  { model: 'mistralai/mistral-7b-instruct', live: '$0.0002', local: '$0.0002', status: 'current' },
];

export function MarketIntelligencePage() {
  return (
    <motion.div className="page" initial="hidden" animate="show" variants={container}>
      <Helmet>
        <title>Market Intelligence — Nexus Autopilot</title>
        <meta name="description" content="Nexus Market Intelligence: live LLM pricing sync from OpenRouter to keep routing cost decisions financially accurate." />
      </Helmet>

      <motion.div variants={item}>
        <div className="badge-premium">INTELLIGENCE HUB</div>
        <h1 className="page-title">Market Intelligence</h1>
        <p className="page-description">
          A live pricing synchronization service that fetches current model costs from the OpenRouter API and updates the local registry — ensuring every routing cost calculation is based on today's actual market rates, not stale seed data.
        </p>

        {/* ── Problem Statement ────────────────────── */}
        <section className="doc-section">
          <div className="section-header-inline">
            <AlertTriangle size={20} color="var(--accent)" />
            <h2>Why This Exists</h2>
          </div>
          <p>
            LLM pricing is not stable. OpenAI, Anthropic, Google, and Groq adjust their token prices frequently — sometimes drastically. The Routing Engine's cost sub-score is only as accurate as the <code>cost_per_1k_tokens</code> values stored in the <code>llm_models</code> table.
          </p>
          <p style={{ marginTop: '12px' }}>
            Without a sync mechanism, the registry drifts. A model seeded at <code>$0.0050/1k</code> that has since dropped to <code>$0.0015/1k</code> will be incorrectly penalized in routing decisions, causing Nexus to route away from what is now a cost-efficient option. The Market Intelligence service eliminates this drift by grounding the registry in live data.
          </p>
        </section>

        {/* ── How it Works ────────────────────────── */}
        <section className="doc-section">
          <div className="section-header-inline">
            <RefreshCw size={20} color="var(--accent)" />
            <h2>How It Works</h2>
          </div>
          <p>When invoked, <code>MarketIntelligenceService.syncMarketRates()</code> runs the following steps:</p>
          <ol style={{ paddingLeft: '20px', display: 'flex', flexDirection: 'column', gap: '10px', marginTop: '16px' }}>
            {[
              'Send a GET request to https://openrouter.ai/api/v1/models using the Java 11 HttpClient.',
              'Parse the response JSON body as a plain string (no external JSON library — uses regex extraction for resilience).',
              'For each model in the local llm_models registry, search the response for its model ID string.',
              'Extract the "prompt_price" field value using a targeted regex: /"prompt_price"\\s*:\\s*"([^"]+)"/',
              'Compare the live price to the stored cost_per_1k_tokens. If different, update the local record via LlmModelDao.update().',
              'Return the count of updated models to the UI for display.',
            ].map((step, i) => (
              <li key={i} style={{ fontSize: '14px', color: 'var(--text-dim)', lineHeight: 1.65 }}>
                <strong style={{ color: 'var(--accent)' }}>{i + 1}.</strong> {step}
              </li>
            ))}
          </ol>

          <CodeBlock
            lang="java"
            code={`// MarketIntelligenceService.java — syncMarketRates()
HttpRequest request = HttpRequest.newBuilder()
    .uri(URI.create("https://openrouter.ai/api/v1/models"))
    .header("Accept", "application/json")
    .GET().build();

HttpResponse<String> response = httpClient.send(request,
    HttpResponse.BodyHandlers.ofString());

String body = response.body();
int updatedCount = 0;

for (LlmModel model : modelDao.findAll()) {
    // Find the model block in the JSON response
    int idx = body.indexOf("\\"id\\": \\"" + model.getName() + "\\"");
    if (idx == -1) continue;

    // Extract prompt_price from the nearby JSON block
    String block = body.substring(idx, Math.min(idx + 600, body.length()));
    Matcher m = Pattern.compile("\\"prompt_price\\"\\\\s*:\\\\s*\\"([^\\"]+)\\"").matcher(block);
    if (m.find()) {
        double livePrice = Double.parseDouble(m.group(1)) * 1000; // per 1k tokens
        if (Math.abs(livePrice - model.getCostPer1kTokens()) > 0.00001) {
            model.setCostPer1kTokens(livePrice);
            modelDao.update(model);
            updatedCount++;
        }
    }
}
return updatedCount;`}
          />
        </section>

        {/* ── Sample Sync Output ───────────────────── */}
        <section className="doc-section">
          <div className="section-header-inline">
            <DollarSign size={20} color="var(--accent)" />
            <h2>Example Sync Output</h2>
          </div>
          <p>A typical sync run checks every model in the registry against the OpenRouter response and updates any price that has changed.</p>
          <table className="custom-table" style={{ marginTop: '24px' }}>
            <thead>
              <tr>
                <th>Model</th>
                <th>Live Price (per 1k)</th>
                <th>Local Before Sync</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              {samplePrices.map(p => (
                <tr key={p.model}>
                  <td><code style={{ fontFamily: 'var(--mono)', fontSize: '11.5px', color: 'var(--text-dim)' }}>{p.model}</code></td>
                  <td style={{ color: 'var(--text)', fontWeight: 600 }}>{p.live}</td>
                  <td style={{ color: p.status === 'updated' ? '#f59e0b' : 'var(--text-muted)' }}>{p.local}</td>
                  <td>
                    <span style={{
                      fontSize: '11px', fontWeight: 750, textTransform: 'uppercase', letterSpacing: '0.05em',
                      color: p.status === 'updated' ? '#10b981' : 'var(--text-muted)',
                      background: p.status === 'updated' ? 'rgba(16,185,129,0.1)' : 'rgba(255,255,255,0.04)',
                      padding: '2px 8px', borderRadius: '4px'
                    }}>
                      {p.status === 'updated' ? 'Updated' : 'No change'}
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </section>

        {/* ── Impact on Routing ────────────────────── */}
        <section className="doc-section">
          <div className="section-header-inline">
            <TrendingDown size={20} color="var(--accent)" />
            <h2>Impact on Routing Decisions</h2>
          </div>
          <p>
            The cost sub-score in the Routing Engine is derived directly from <code>cost_per_1k_tokens</code> in the <code>llm_models</code> table. After a market sync, the next routing pass immediately uses the updated values. A model that has dropped in price will have a higher cost sub-score and may be selected over previously cheaper alternatives.
          </p>
          <Callout type="info">
            <strong>Sync frequency:</strong> Market Intelligence is not run automatically on startup. It is invoked on demand from the Intelligence Hub. Run it once per week or after any major provider pricing announcement.
          </Callout>
        </section>

        {/* ── How to Invoke ─────────────────────────── */}
        <section className="doc-section">
          <div className="section-header-inline">
            <Code2 size={20} color="var(--accent)" />
            <h2>How to Invoke</h2>
          </div>
          <div style={{ margin: '24px 0', padding: '20px', border: '1px solid var(--border)', borderRadius: '10px', background: 'rgba(0,0,0,0.15)', fontFamily: 'var(--mono)', fontSize: '13px', lineHeight: 2 }}>
            <div style={{ color: 'var(--text-muted)' }}>nexus@local &gt; Main Dashboard</div>
            <div style={{ color: 'var(--accent)' }}>Press: <strong>I</strong>  →  Intelligence Hub</div>
            <div style={{ color: 'var(--text-muted)' }}>Press: <strong>3</strong>  →  Market Intelligence Reality Check</div>
            <div style={{ color: '#10b981', marginTop: '8px' }}>Output: N model prices updated in local registry</div>
          </div>
          <Callout type="warning">
            <strong>Network required:</strong> The sync requires an active internet connection. If the OpenRouter endpoint is unreachable, the existing registry values are preserved and an error is displayed without writing to the audit log.
          </Callout>
        </section>
      </motion.div>
    </motion.div>
  );
}
