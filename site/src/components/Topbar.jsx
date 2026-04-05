import { Link } from 'react-router-dom';
import { Search, GitBranch, Menu, Terminal } from 'lucide-react';

export function Topbar({ onMenuClick }) {
  return (
    <header className="topbar">
      <button
        className="topbar-hamburger"
        onClick={onMenuClick}
        aria-label="Open navigation menu"
      >
        <Menu size={20} />
      </button>

      <Link to="/" className="topbar-logo">
        <div className="topbar-wordmark">
          <Terminal size={18} color="var(--accent)" strokeWidth={2.5} />
          <span>Nexus</span>
        </div>
      </Link>

      <div className="topbar-search">
        <Search className="search-icon" size={13} />
        <input type="text" placeholder="Search documentation..." disabled />
        <kbd className="search-kbd">⌘K</kbd>
      </div>
      
      <nav className="topbar-nav">
        <a 
          href="https://github.com/swarnimlovesyou/Nexus" 
          target="_blank" 
          rel="noreferrer" 
          className="topbar-github"
        >
          <GitBranch size={14} color="var(--accent)" strokeWidth={2.5} />
          <span>GitHub</span>
        </a>
      </nav>
    </header>
  );
}
