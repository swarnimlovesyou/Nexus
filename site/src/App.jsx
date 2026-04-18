import { useState } from 'react';
import { HashRouter, Routes, Route, Navigate } from 'react-router-dom';
import { HelmetProvider } from 'react-helmet-async';
import './index.css';

import { Topbar } from './components/Topbar';
import { Sidebar } from './components/Sidebar';

import { LandingPage } from './pages/LandingPage';
import { OverviewPage } from './pages/OverviewPage';
import { InstallPage } from './pages/InstallPage';
import { QuickstartPage } from './pages/QuickstartPage';
import { RoutingEnginePage } from './pages/RoutingEnginePage';
import { MemoryLayerPage } from './pages/MemoryLayerPage';
import { ApiKeyVaultPage } from './pages/ApiKeyVaultPage';
import { FinancialPage } from './pages/FinancialPage';
import { CliReferencePage } from './pages/CliReferencePage';
import { ChangelogPage } from './pages/ChangelogPage';
import { SovereignDocsPage } from './pages/SovereignDocsPage';
import { SecuritySentinelPage } from './pages/SecuritySentinelPage';
import { ArchitectureDnaPage } from './pages/ArchitectureDnaPage';
import { MarketIntelligencePage } from './pages/MarketIntelligencePage';
import { CompatibilityFeaturesPage } from './pages/CompatibilityFeaturesPage';
import { SessionPowerToolsPage } from './pages/SessionPowerToolsPage';

function DocsLayout({ sidebarOpen, setSidebarOpen }) {
  return (
    <div className="app">
      <Topbar onMenuClick={() => setSidebarOpen(true)} />
      <div className="app-body">
        <Sidebar open={sidebarOpen} onClose={() => setSidebarOpen(false)} />
        <main className="main">
          <Routes>
            <Route index element={<Navigate to="/overview" replace />} />
            <Route path="overview" element={<OverviewPage />} />
            <Route path="install" element={<InstallPage />} />
            <Route path="quickstart" element={<QuickstartPage />} />
            <Route path="architecture" element={<SovereignDocsPage />} />
            <Route path="routing" element={<RoutingEnginePage />} />
            <Route path="memory" element={<MemoryLayerPage />} />
            <Route path="api-vault" element={<ApiKeyVaultPage />} />
            <Route path="finance" element={<FinancialPage />} />
            <Route path="compatibility" element={<CompatibilityFeaturesPage />} />
            <Route path="session-tools" element={<SessionPowerToolsPage />} />
            <Route path="security" element={<SecuritySentinelPage />} />
            <Route path="architecture-dna" element={<ArchitectureDnaPage />} />
            <Route path="market-intel" element={<MarketIntelligencePage />} />
            <Route path="cli" element={<CliReferencePage />} />
            <Route path="changelog" element={<ChangelogPage />} />
            <Route path="*" element={<Navigate to="/overview" replace />} />
          </Routes>
        </main>
        <aside className="toc-sidebar">
          <div className="toc-title">On this page</div>
          <ul className="toc-list">
            <li className="toc-item"><a href="#introduction" className="toc-link">Introduction</a></li>
            <li className="toc-item"><a href="#core-concepts" className="toc-link">Core Concepts</a></li>
            <li className="toc-item"><a href="#architecture" className="toc-link">Architecture</a></li>
            <li className="toc-item"><a href="#deep-dive" className="toc-link">Deep Dive</a></li>
            <li className="toc-item"><a href="#best-practices" className="toc-link">Best Practices</a></li>
          </ul>
        </aside>
      </div>
    </div>
  );
}

export default function App() {
  const [sidebarOpen, setSidebarOpen] = useState(false);

  return (
    <HelmetProvider>
      <HashRouter>
        <Routes>
          <Route path="/" element={<LandingPage />} />
          <Route path="/*" element={<DocsLayout sidebarOpen={sidebarOpen} setSidebarOpen={setSidebarOpen} />} />
        </Routes>
      </HashRouter>
    </HelmetProvider>
  );
}
