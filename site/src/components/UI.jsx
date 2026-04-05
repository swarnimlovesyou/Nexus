import { useState } from 'react';
import { Copy, Check, Terminal as TerminalIcon } from 'lucide-react';

export function Terminal({ lines }) {
  return (
    <div className="terminal-block" style={{ margin: '16px 0' }}>
      <div className="terminal-header">
        <div className="terminal-controls">
          <span className="dot dot-red" />
          <span className="dot dot-yellow" />
          <span className="dot dot-green" />
        </div>
        <div className="terminal-label">
          <TerminalIcon size={11} className="icon-muted" />
          <span>nexus — bash</span>
        </div>
        <div /> 
      </div>
      <div className="terminal-content">
        {lines.map((line, i) => (
          <div key={i} dangerouslySetInnerHTML={{ __html: line }} style={{ margin: '2px 0' }} />
        ))}
      </div>
    </div>
  );
}

export function CodeBlock({ code, lang = 'bash' }) {
  const [copied, setCopied] = useState(false);
  const copy = () => {
    navigator.clipboard.writeText(code);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div className="code-block-container" style={{ margin: '12px 0' }}>
      <div className="code-block-header">
        <span className="code-block-lang">{lang}</span>
        <button className={`copy-btn ${copied ? 'copied' : ''}`} onClick={copy} style={{ fontSize: '11px' }}>
          {copied ? <Check size={12} /> : <Copy size={12} />}
          <span>{copied ? 'Copied' : 'Copy'}</span>
        </button>
      </div>
      <pre className="code-block-pre" style={{ padding: '12px' }}>
        <code>{code}</code>
      </pre>
    </div>
  );
}

export function Callout({ type = 'info', children }) {
  return (
    <div className={`callout-premium callout-${type}`} style={{ margin: '16px 0' }}>
      <div className="callout-inner" style={{ padding: '12px 16px' }}>
        {children}
      </div>
    </div>
  );
}

export function InstallTabs({ tabs }) {
  const [active, setActive] = useState(0);
  return (
    <div className="install-tabs-container" style={{ margin: '24px 0' }}>
      <div className="tabs-list">
        {tabs.map((t, i) => (
          <button 
            key={i} 
            className={`tab-trigger ${active === i ? 'active' : ''}`} 
            onClick={() => setActive(i)}
            style={{ fontSize: '12px', padding: '6px 12px' }}
          >
            {t.label}
          </button>
        ))}
      </div>
      <div className="tabs-content">
        <CodeBlock code={tabs[active].code} lang="bash" />
      </div>
    </div>
  );
}

