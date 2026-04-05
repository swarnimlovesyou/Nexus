import { useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { Terminal, Shield, BrainCircuit, Activity, BookOpen, Key, DollarSign, Download, Zap, History, X } from 'lucide-react';

const MENU = [
  {
    title: 'GETTING STARTED',
    links: [
      { id: 'overview',    label: 'Overview',                path: '/overview',    icon: BookOpen },
      { id: 'install',     label: 'Installation',            path: '/install',     icon: Download },
      { id: 'quickstart',  label: 'Quickstart',              path: '/quickstart',  icon: Zap },
    ]
  },
  {
    title: 'FEATURES',
    links: [
      { id: 'routing',   label: 'Routing Engine',       path: '/routing',    icon: Activity },
      { id: 'memory',    label: 'Memory Vault',         path: '/memory',     icon: BrainCircuit },
      { id: 'api-vault', label: 'API Key Vault',        path: '/api-vault',  icon: Key },
      { id: 'finance',   label: 'Financial Intelligence', path: '/finance',  icon: DollarSign },
    ]
  },
  {
    title: 'REFERENCE',
    links: [
      { id: 'cli',       label: 'CLI Reference',  path: '/cli',       icon: Terminal },
      { id: 'changelog', label: 'Changelog',      path: '/changelog', icon: History },
    ]
  }
];

export function Sidebar({ open, onClose }) {
  const { pathname } = useLocation();

  return (
    <>
      {open && <div onClick={onClose} className="sidebar-overlay" />}

      <aside className={`sidebar ${open ? 'sidebar-open' : ''}`}>
        <button onClick={onClose} className="sidebar-close-btn" aria-label="Close">
          <X size={20} />
        </button>

        {MENU.map(group => (
          <div key={group.title} className="sidebar-group">
            <div className="sidebar-group-title">{group.title}</div>
            {group.links.map(link => {
              const Icon = link.icon;
              const isActive = pathname.startsWith(link.path);
              return (
                <Link
                  key={link.id}
                  to={link.path}
                  className={`sidebar-link ${isActive ? 'active' : ''}`}
                  onClick={onClose}
                >
                  <Icon size={16} strokeWidth={2} />
                  {link.label}
                </Link>
              );
            })}
          </div>
        ))}
      </aside>
    </>
  );
}
