import { useState } from 'react';
import { HashRouter, Routes, Route, Navigate, useLocation } from 'react-router-dom';
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

function DocsLayout({ sidebarOpen, setSidebarOpen }) {
  return (
    <div className="app">
      <Topbar onMenuClick={() => setSidebarOpen(true)} />
      <div className="app-body">
        <Sidebar open={sidebarOpen} onClose={() => setSidebarOpen(false)} />
        <main className="main">
          <Routes>
            <Route index element={<Navigate to="overview" replace />} />
            <Route path="overview" element={<OverviewPage />} />
            <Route path="install" element={<InstallPage />} />
            <Route path="quickstart" element={<QuickstartPage />} />
            <Route path="routing" element={<RoutingEnginePage />} />
            <Route path="memory" element={<MemoryLayerPage />} />
            <Route path="api-vault" element={<ApiKeyVaultPage />} />
            <Route path="finance" element={<FinancialPage />} />
            <Route path="cli" element={<CliReferencePage />} />
            <Route path="changelog" element={<ChangelogPage />} />
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
          <Route path="/overview" element={<DocsLayout sidebarOpen={sidebarOpen} setSidebarOpen={setSidebarOpen} />} />
          <Route path="/install" element={<DocsLayout sidebarOpen={sidebarOpen} setSidebarOpen={setSidebarOpen} />} />
          <Route path="/quickstart" element={<DocsLayout sidebarOpen={sidebarOpen} setSidebarOpen={setSidebarOpen} />} />
          <Route path="/routing" element={<DocsLayout sidebarOpen={sidebarOpen} setSidebarOpen={setSidebarOpen} />} />
          <Route path="/memory" element={<DocsLayout sidebarOpen={sidebarOpen} setSidebarOpen={setSidebarOpen} />} />
          <Route path="/api-vault" element={<DocsLayout sidebarOpen={sidebarOpen} setSidebarOpen={setSidebarOpen} />} />
          <Route path="/finance" element={<DocsLayout sidebarOpen={sidebarOpen} setSidebarOpen={setSidebarOpen} />} />
          <Route path="/cli" element={<DocsLayout sidebarOpen={sidebarOpen} setSidebarOpen={setSidebarOpen} />} />
          <Route path="/changelog" element={<DocsLayout sidebarOpen={sidebarOpen} setSidebarOpen={setSidebarOpen} />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </HashRouter>
    </HelmetProvider>
  );
}
