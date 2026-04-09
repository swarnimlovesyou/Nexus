import { motion } from 'framer-motion';
import { Helmet } from 'react-helmet-async';
import { Cpu, GitBranch, Search, Database, Code2, ArrowRight } from 'lucide-react';
import { CodeBlock, Callout } from '../components/UI';
import { Link } from 'react-router-dom';

const container = { hidden: { opacity: 0 }, show: { opacity: 1, transition: { staggerChildren: 0.08 }}};
const item = { hidden: { opacity: 0, y: 12 }, show: { opacity: 1, y: 0 }};

export function ArchitectureDnaPage() {
  return (
    <motion.div className="page" initial="hidden" animate="show" variants={container}>
      <Helmet>
        <title>Architecture DNA — Nexus Autopilot</title>
        <meta name="description" content="Nexus Architecture DNA Engine: recursive Java class scanner that maps dependencies into a queryable Memory Vault knowledge base." />
      </Helmet>

      <motion.div variants={item}>
        <div className="badge-premium">INTELLIGENCE HUB</div>
        <h1 className="page-title">Architecture DNA Engine</h1>
        <p className="page-description">
          A recursive Java source scanner that builds a queryable dependency graph of your entire codebase and persists it as structured FACT memories in the Memory Vault — giving Nexus a living knowledge base of your project's architecture.
        </p>

        {/* ── Problem ──────────────────────────────── */}
        <section className="doc-section">
          <div className="section-header-inline">
            <Cpu size={20} color="var(--accent)" />
            <h2>Why This Exists</h2>
          </div>
          <p>
            As a project grows, its dependency graph becomes invisible. Developers forget which services depend on which DAOs, which domain objects are shared across multiple layers, and which files are safe to refactor without cascading breaks. This knowledge exists in the code, but is never surfaced until something breaks.
          </p>
          <p style={{ marginTop: '12px' }}>
            The Architecture DNA Engine makes this graph explicit and persistent. By scanning the source tree and mapping every class's import dependencies, it creates a structured knowledge base that Nexus can query, and that you can search through the Memory Vault. Every entry is timestamped, tagged, and confidence-scored like any other memory.
          </p>
        </section>

        {/* ── How It Works ─────────────────────────── */}
        <section className="doc-section">
          <div className="section-header-inline">
            <GitBranch size={20} color="var(--accent)" />
            <h2>How It Works</h2>
          </div>
          <p>When invoked, <code>ArchitectureService.buildContextGraph()</code> runs the following pipeline:</p>
          <ol style={{ paddingLeft: '20px', display: 'flex', flexDirection: 'column', gap: '10px', marginTop: '16px' }}>
            {[
              'Walk the target directory recursively, collecting every .java file path using NIO file tree APIs.',
              'For each file: extract the declared class name from the first "class " or "interface " declaration found.',
              'Extract the import list — every "import " statement — as the class\'s dependency set.',
              'Construct a structured summary string: "Class: X | File: path | Dependencies: A, B, C".',
              'Store this summary in the Memory Vault as a FACT type memory with tags [ARCH_MAP, ClassName] and a 365-day TTL.',
              'After all classes are processed, store a global summary: total class count and the scan timestamp.',
            ].map((step, i) => (
              <li key={i} style={{ fontSize: '14px', color: 'var(--text-dim)', lineHeight: 1.65 }}>
                <strong style={{ color: 'var(--accent)' }}>{i + 1}.</strong> {step}
              </li>
            ))}
          </ol>

          <CodeBlock
            lang="java"
            code={`// ArchitectureService.java — buildContextGraph()
List<Path> javaFiles = new ArrayList<>();
Files.walkFileTree(Paths.get(scanRoot), new SimpleFileVisitor<>() {
    public FileVisitResult visitFile(Path file, BasicFileAttributes a) {
        if (file.toString().endsWith(".java")) javaFiles.add(file);
        return FileVisitResult.CONTINUE;
    }
});

for (Path path : javaFiles) {
    String content = Files.readString(path);
    String className = extractClassName(content);     // scans for "class " / "interface "
    List<String> deps = extractImports(content);       // scans for "import " lines

    String summary = "Class: " + className
        + " | File: " + path.getFileName()
        + " | Dependencies: " + String.join(", ", deps);

    // Persist to Memory Vault as a FACT with 365-day TTL
    memoryService.store(userId, summary,
        "ARCH_MAP," + className, MemoryType.FACT, 365);
}`}
          />
        </section>

        {/* ── The Output ───────────────────────────── */}
        <section className="doc-section">
          <div className="section-header-inline">
            <Database size={20} color="var(--accent)" />
            <h2>What Gets Stored</h2>
          </div>
          <p>After a DNA scan of the Nexus project itself, the Memory Vault contains entries structured like this:</p>
          <div style={{ background: 'rgba(0,0,0,0.2)', border: '1px solid var(--border)', borderRadius: '10px', overflow: 'hidden', margin: '24px 0' }}>
            <div style={{ padding: '10px 20px', borderBottom: '1px solid var(--border)', background: 'var(--bg-surface)' }}>
              <span style={{ fontFamily: 'var(--mono)', fontSize: '11px', color: 'var(--accent)', fontWeight: 750 }}>memories table — sample entries after DNA scan</span>
            </div>
            {[
              { tag: 'ARCH_MAP,RoutingEngine', content: 'Class: RoutingEngine | File: RoutingEngine.java | Dependencies: LlmModelDao, SuitabilityDao, OutcomeMemoryDao, ApiKeyDao, LlmModel, TaskType' },
              { tag: 'ARCH_MAP,MemoryService', content: 'Class: MemoryService | File: MemoryService.java | Dependencies: MemoryDao, Memory, MemoryType, User' },
              { tag: 'ARCH_MAP,NexusApp', content: 'Class: NexusApp | File: NexusApp.java | Dependencies: RoutingMenu, MemoryMenu, IntelligenceMenu, MenuContext, AdminMenu' },
              { tag: 'GLOBAL_DNA,GRAPH', content: 'Architecture scan complete. 56 classes indexed. Scan run: 2026-04-09T09:15' },
            ].map((entry, i) => (
              <div key={i} style={{ padding: '12px 20px', borderBottom: '1px solid rgba(255,255,255,0.04)' }}>
                <div style={{ fontFamily: 'var(--mono)', fontSize: '11px', color: 'var(--accent)', marginBottom: '4px' }}>{entry.tag}</div>
                <div style={{ fontFamily: 'var(--mono)', fontSize: '12px', color: 'var(--text-muted)', lineHeight: 1.5 }}>{entry.content}</div>
              </div>
            ))}
          </div>
        </section>

        {/* ── Search and Query ─────────────────────── */}
        <section className="doc-section">
          <div className="section-header-inline">
            <Search size={20} color="var(--accent)" />
            <h2>Querying the Architecture Graph</h2>
          </div>
          <p>
            Once the DNA scan is complete, the full dependency graph is queryable through the Memory Vault's search function. Search for any class name to retrieve its dependency list. Search for a DAO name to find every class that depends on it.
          </p>
          <div style={{ margin: '24px 0', padding: '20px', border: '1px solid var(--border)', borderRadius: '10px', background: 'rgba(0,0,0,0.15)', fontFamily: 'var(--mono)', fontSize: '13px', lineHeight: 2 }}>
            <div style={{ color: 'var(--text-muted)' }}>Memory Vault &gt; Search</div>
            <div style={{ color: 'var(--accent)' }}>Query: "RoutingEngine"</div>
            <div style={{ color: 'var(--text-dim)', marginTop: '4px' }}>Result: Class: RoutingEngine | Dependencies: LlmModelDao, SuitabilityDao, OutcomeMemoryDao...</div>
            <div style={{ color: 'var(--accent)', marginTop: '8px' }}>Query: "MemoryDao"</div>
            <div style={{ color: 'var(--text-dim)', marginTop: '4px' }}>Results: MemoryService, MemoryMenu, NexusApp (via MenuContext) — 3 dependent classes</div>
          </div>
          <Callout type="info">
            <strong>Re-scan on change:</strong> The DNA graph is a point-in-time snapshot. After refactoring classes or adding new files, re-run the scan from the Intelligence Hub to update the dependency map.
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
            <div style={{ color: 'var(--text-muted)' }}>Press: <strong>1</strong>  →  Build Architectural Context Graph</div>
            <div style={{ color: '#10b981', marginTop: '8px' }}>Output: N classes indexed and stored in Memory Vault as FACT type</div>
          </div>
          <p style={{ marginTop: '16px' }}>
            After the scan completes, search for any class name in the Memory Vault to retrieve its dependency data. The full graph summary is stored under the tag <code>GLOBAL_DNA,GRAPH</code>.
          </p>
        </section>
      </motion.div>
    </motion.div>
  );
}
