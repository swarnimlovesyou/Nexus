import React from 'react';

/**
 * DB Schema Visual — ERD-style card grid
 */
export const DbSchemaVisual = () => {
  const tables = [
    {
      name: 'users',
      color: '#E8745C',
      fields: [
        { name: 'id', type: 'PK · INTEGER' },
        { name: 'username', type: 'TEXT · UNIQUE' },
        { name: 'password_hash', type: 'TEXT' },
        { name: 'role', type: 'TEXT · ADMIN | DEV' },
        { name: 'created_at', type: 'INTEGER · epoch' },
      ]
    },
    {
      name: 'llm_models',
      color: '#3b82f6',
      fields: [
        { name: 'id', type: 'PK · INTEGER' },
        { name: 'name', type: 'TEXT' },
        { name: 'provider', type: 'TEXT' },
        { name: 'cost_per_1k_tokens', type: 'REAL' },
        { name: 'created_at', type: 'INTEGER · epoch' },
      ]
    },
    {
      name: 'memories',
      color: '#a855f7',
      fields: [
        { name: 'id', type: 'PK · INTEGER' },
        { name: 'user_id', type: 'FK → users' },
        { name: 'content', type: 'TEXT · full-text' },
        { name: 'tags', type: 'TEXT' },
        { name: 'type', type: 'TEXT · enum' },
        { name: 'confidence', type: 'REAL · 0.0–1.0' },
        { name: 'expires_at', type: 'INTEGER · epoch' },
      ]
    },
    {
      name: 'agent_sessions',
      color: '#10b981',
      fields: [
        { name: 'id', type: 'PK · INTEGER' },
        { name: 'user_id', type: 'FK → users' },
        { name: 'model_id', type: 'FK → llm_models' },
        { name: 'task_type', type: 'TEXT · TaskType enum' },
        { name: 'input_tokens', type: 'INTEGER' },
        { name: 'output_tokens', type: 'INTEGER' },
        { name: 'total_cost', type: 'REAL' },
        { name: 'quality_score', type: 'REAL · 0.0–1.0' },
      ]
    },
    {
      name: 'outcome_memories',
      color: '#f59e0b',
      fields: [
        { name: 'id', type: 'PK · INTEGER' },
        { name: 'user_id', type: 'FK → users' },
        { name: 'model_id', type: 'FK → llm_models' },
        { name: 'task_type', type: 'TEXT' },
        { name: 'cost', type: 'REAL' },
        { name: 'latency_ms', type: 'INTEGER' },
        { name: 'quality_score', type: 'REAL' },
      ]
    },
    {
      name: 'api_keys',
      color: '#ef4444',
      fields: [
        { name: 'id', type: 'PK · INTEGER' },
        { name: 'user_id', type: 'FK → users' },
        { name: 'provider', type: 'TEXT' },
        { name: 'alias', type: 'TEXT' },
        { name: 'masked_key', type: 'TEXT · display only' },
        { name: 'encoded_key', type: 'TEXT · Base64' },
      ]
    },
    {
      name: 'model_suitability',
      color: '#6366f1',
      fields: [
        { name: 'id', type: 'PK · INTEGER' },
        { name: 'model_id', type: 'FK → llm_models' },
        { name: 'task_type', type: 'TEXT · TaskType enum' },
        { name: 'base_score', type: 'REAL · 0.0–1.0' },
      ]
    },
    {
      name: 'audit_log',
      color: '#64748b',
      fields: [
        { name: 'id', type: 'PK · INTEGER' },
        { name: 'user_id', type: 'FK → users (nullable)' },
        { name: 'action', type: 'TEXT · event type' },
        { name: 'details', type: 'TEXT · key=value pairs' },
        { name: 'outcome', type: 'TEXT · SUCCESS | FAILURE' },
      ]
    },
  ];

  return (
    <div style={{
      display: 'grid',
      gridTemplateColumns: 'repeat(auto-fill, minmax(240px, 1fr))',
      gap: '16px',
      padding: '8px 0 32px',
    }}>
      {tables.map(table => (
        <div key={table.name} style={{
          background: 'var(--bg)',
          border: `1px solid ${table.color}30`,
          borderTop: `2px solid ${table.color}`,
          borderRadius: '10px',
          overflow: 'hidden',
          transition: '.18s',
        }}>
          <div style={{
            padding: '10px 16px',
            background: `${table.color}08`,
            borderBottom: `1px solid ${table.color}20`,
            display: 'flex',
            alignItems: 'center',
            gap: '8px',
          }}>
            <div style={{ width: '7px', height: '7px', background: table.color, borderRadius: '2px', flexShrink: 0 }} />
            <span style={{ fontFamily: 'var(--mono)', fontSize: '11.5px', fontWeight: 800, color: 'var(--text)', textTransform: 'uppercase', letterSpacing: '0.06em' }}>
              {table.name}
            </span>
          </div>
          <div style={{ padding: '4px 0' }}>
            {table.fields.map(f => (
              <div key={f.name} style={{
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                padding: '6px 16px',
                borderBottom: '1px solid rgba(255,255,255,0.025)',
                gap: '8px',
              }}>
                <span style={{ fontFamily: 'var(--mono)', fontSize: '12px', color: 'var(--text)', fontWeight: 600, flexShrink: 0 }}>
                  {f.name}
                </span>
                <span style={{ fontFamily: 'var(--mono)', fontSize: '10px', color: 'var(--text-muted)', textAlign: 'right' }}>
                  {f.type}
                </span>
              </div>
            ))}
          </div>
        </div>
      ))}
    </div>
  );
};

/**
 * FK Relationship Map — text-based visual
 */
export const RelationshipMap = () => {
  const relations = [
    { from: 'memories.user_id', to: 'users.id' },
    { from: 'agent_sessions.user_id', to: 'users.id' },
    { from: 'agent_sessions.model_id', to: 'llm_models.id' },
    { from: 'outcome_memories.user_id', to: 'users.id' },
    { from: 'outcome_memories.model_id', to: 'llm_models.id' },
    { from: 'api_keys.user_id', to: 'users.id' },
    { from: 'model_suitability.model_id', to: 'llm_models.id' },
    { from: 'audit_log.user_id', to: 'users.id (nullable)' },
  ];

  return (
    <div style={{
      background: 'rgba(0,0,0,0.2)',
      border: '1px solid var(--border)',
      borderRadius: '10px',
      overflow: 'hidden',
      margin: '24px 0',
    }}>
      <div style={{ padding: '10px 20px', borderBottom: '1px solid var(--border)', background: 'var(--bg-surface)' }}>
        <span style={{ fontSize: '10px', fontWeight: 850, letterSpacing: '0.18em', color: 'var(--accent)', textTransform: 'uppercase' }}>
          Foreign Key Relationships
        </span>
      </div>
      {relations.map((r, i) => (
        <div key={i} style={{
          display: 'flex', alignItems: 'center', gap: '12px',
          padding: '9px 20px',
          borderBottom: i < relations.length - 1 ? '1px solid rgba(255,255,255,0.03)' : 'none',
          fontFamily: 'var(--mono)', fontSize: '12px',
        }}>
          <span style={{ color: 'var(--accent)', minWidth: '260px' }}>{r.from}</span>
          <span style={{ color: 'var(--border)' }}>→</span>
          <span style={{ color: 'var(--text-dim)' }}>{r.to}</span>
        </div>
      ))}
    </div>
  );
};

/**
 * 4-Layer Architecture Stack
 */
export const ArchitectureStack = () => {
  const layers = [
    {
      title: 'Presentation Layer',
      subtitle: 'com.nexus.presentation',
      detail: 'All CLI menus, ANSI-256 rendering, and user interaction. No business logic lives here.',
      color: '#E8745C',
      files: ['NexusApp', 'RoutingMenu', 'IntelligenceMenu', 'MemoryMenu', 'AdminMenu', '+6 more']
    },
    {
      title: 'Service Intelligence Layer',
      subtitle: 'com.nexus.service',
      detail: 'All autonomous intelligence: routing arbitrage, agentic planning, memory lifecycle, security audits, and market sync.',
      color: '#3b82f6',
      files: ['RoutingEngine', 'TaskPlannerService', 'LlmCallService', 'SecuritySentinelService', 'ArchitectureService', '+5 more']
    },
    {
      title: 'Domain Model Layer',
      subtitle: 'com.nexus.domain',
      detail: 'Pure data models with zero dependencies on infrastructure or I/O. Enums define valid states.',
      color: '#a855f7',
      files: ['LlmModel', 'Memory', 'AgentSession', 'User', 'TaskType', 'Provider', 'MemoryType']
    },
    {
      title: 'Data Access Layer',
      subtitle: 'com.nexus.dao',
      detail: 'All SQLite CRUD operations. GenericDao<T> provides type-safe base. DbConnectionManager owns the schema lifecycle.',
      color: '#10b981',
      files: ['DbConnectionManager', 'GenericDao<T>', 'LlmModelDao', 'MemoryDao', 'OutcomeMemoryDao', '+5 more']
    },
  ];

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '10px', margin: '24px 0 32px' }}>
      {layers.map((layer, i) => (
        <div key={layer.title} style={{
          display: 'grid',
          gridTemplateColumns: '3fr 1fr',
          gap: '0',
          border: `1px solid ${layer.color}25`,
          borderLeft: `3px solid ${layer.color}`,
          borderRadius: '10px',
          overflow: 'hidden',
          background: `${layer.color}05`,
        }}>
          <div style={{ padding: '18px 24px' }}>
            <div style={{ display: 'flex', alignItems: 'baseline', gap: '12px', marginBottom: '6px' }}>
              <span style={{ fontSize: '14px', fontWeight: 850, color: 'var(--text)' }}>{layer.title}</span>
              <code style={{ fontFamily: 'var(--mono)', fontSize: '11px', color: layer.color, background: `${layer.color}15`, padding: '1px 8px', borderRadius: '4px' }}>
                {layer.subtitle}
              </code>
            </div>
            <p style={{ fontSize: '13px', color: 'var(--text-muted)', lineHeight: 1.6, margin: 0 }}>{layer.detail}</p>
          </div>
          <div style={{ padding: '18px 20px', borderLeft: `1px solid ${layer.color}15`, display: 'flex', flexWrap: 'wrap', gap: '4px', alignContent: 'flex-start' }}>
            {layer.files.map(f => (
              <span key={f} style={{ fontFamily: 'var(--mono)', fontSize: '10px', color: 'var(--text-muted)', background: 'rgba(255,255,255,0.04)', border: '1px solid var(--border)', padding: '2px 7px', borderRadius: '3px' }}>
                {f}
              </span>
            ))}
          </div>
        </div>
      ))}
    </div>
  );
};

/**
 * Agentic Execution Flow — 5-step horizontal
 */
export const ExecutionFlow = () => {
  const steps = [
    { step: '01', label: 'Ingest', file: 'RoutingMenu', detail: 'Raw prompt received from terminal input' },
    { step: '02', label: 'Plan', file: 'TaskPlannerService', detail: 'Decomposed into typed sub-tasks via keyword heuristics' },
    { step: '03', label: 'Route', file: 'RoutingEngine', detail: 'Composite score calculated for all accessible models' },
    { step: '04', label: 'Execute', file: 'LlmCallService', detail: 'HTTP call dispatched to winning provider' },
    { step: '05', label: 'Learn', file: 'OutcomeMemoryDao', detail: 'Telemetry written; scores recalibrated for next run' },
  ];

  return (
    <div style={{ margin: '24px 0 32px', position: 'relative' }}>
      <div style={{
        position: 'absolute', top: '28px', left: '10%', right: '10%',
        height: '1px',
        background: 'linear-gradient(90deg, transparent, var(--border) 20%, var(--border) 80%, transparent)',
        zIndex: 0,
      }} />
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(5, 1fr)', gap: '8px', position: 'relative', zIndex: 1 }}>
        {steps.map((s) => (
          <div key={s.step} style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', textAlign: 'center', padding: '0 8px' }}>
            <div style={{
              width: '48px', height: '48px', borderRadius: '50%',
              background: 'var(--bg-surface)',
              border: '1.5px solid var(--accent)',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              fontSize: '11px', fontWeight: 850, color: 'var(--accent)',
              marginBottom: '14px',
              boxShadow: '0 0 16px rgba(232,116,92,0.12)',
              fontFamily: 'var(--mono)',
            }}>
              {s.step}
            </div>
            <div style={{ fontSize: '13px', fontWeight: 850, color: 'var(--text)', marginBottom: '4px' }}>{s.label}</div>
            <code style={{ fontFamily: 'var(--mono)', fontSize: '10px', color: 'var(--accent)', background: 'var(--accent-dim)', padding: '2px 6px', borderRadius: '3px', marginBottom: '6px' }}>
              {s.file}
            </code>
            <div style={{ fontSize: '11px', color: 'var(--text-muted)', lineHeight: 1.5 }}>{s.detail}</div>
          </div>
        ))}
      </div>
    </div>
  );
};

/**
 * Routing Score Formula Visual
 */
export const ScoringFormula = () => {
  const factors = [
    { label: 'Suitability', weight: 45, color: '#E8745C', note: 'Expert-curated task-model fit score. Stored in model_suitability.' },
    { label: 'Quality', weight: 25, color: '#3b82f6', note: 'Aggregated from outcome_memories. Your real-world data drives this.' },
    { label: 'Inverse Latency', weight: 20, color: '#a855f7', note: 'Models with faster historical average responses rank higher.' },
    { label: 'Inverse Cost', weight: 10, color: '#10b981', note: 'Lower cost_per_1k_tokens yields a higher sub-score.' },
  ];

  return (
    <div style={{ border: '1px solid var(--border)', borderRadius: '12px', overflow: 'hidden', margin: '24px 0' }}>
      <div style={{ padding: '12px 20px', background: 'var(--bg-surface)', borderBottom: '1px solid var(--border)', display: 'flex', alignItems: 'center', gap: '10px' }}>
        <span style={{ fontFamily: 'var(--mono)', fontSize: '12px', color: 'var(--accent)', fontWeight: 750 }}>CompositeScore</span>
        <span style={{ fontSize: '12px', color: 'var(--text-muted)' }}>=</span>
        <span style={{ fontFamily: 'var(--mono)', fontSize: '12px', color: 'var(--text-dim)' }}>
          (S × 0.45) + (Q × 0.25) + (L<sub>inv</sub> × 0.20) + (C<sub>inv</sub> × 0.10)
        </span>
      </div>
      {factors.map((f, i) => (
        <div key={f.label} style={{
          display: 'grid', gridTemplateColumns: '120px 50px 1fr 40px',
          gap: '16px', alignItems: 'center', padding: '14px 20px',
          borderBottom: i < factors.length - 1 ? '1px solid rgba(255,255,255,0.04)' : 'none',
          background: 'var(--bg)',
        }}>
          <span style={{ fontSize: '13px', fontWeight: 700, color: 'var(--text)' }}>{f.label}</span>
          <div style={{ height: '4px', background: 'rgba(255,255,255,0.06)', borderRadius: '2px', position: 'relative' }}>
            <div style={{ position: 'absolute', left: 0, top: 0, height: '100%', width: `${f.weight / 45 * 100}%`, background: f.color, borderRadius: '2px' }} />
          </div>
          <span style={{ fontSize: '12px', color: 'var(--text-muted)', lineHeight: 1.5 }}>{f.note}</span>
          <span style={{ fontSize: '13px', fontWeight: 850, color: f.color, textAlign: 'right', fontFamily: 'var(--mono)' }}>{f.weight}%</span>
        </div>
      ))}
    </div>
  );
};
