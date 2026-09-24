import { NavLink, Outlet } from 'react-router-dom';
import { useApi } from '../hooks/useApi';
import { api } from '../api/client';
import type { SystemStatus } from '../api/types';
import { formatNumber } from './format';

/**
 * LogInsight Analyzer shell: sidebar navigation + header + routed content. Every screen drives the
 * real backend API; the header reflects the currently loaded dataset live.
 */

interface NavEntry {
  to: string;
  icon: string;
  label: string;
  end?: boolean;
  badge?: string;
}

const NAV: NavEntry[] = [
  { to: '/',                  icon: '◈', label: 'Command Center', end: true },
  { to: '/logs',              icon: '☰', label: 'Log Explorer' },
  { to: '/search',            icon: '⌕', label: 'Search' },
  { to: '/analytics',         icon: '◆', label: 'Analytics' },
  { to: '/patterns',          icon: '≈', label: 'Patterns' },
  { to: '/incidents',         icon: '!', label: 'Incidents' },
  { to: '/services',          icon: '⚑', label: 'Services' },
  { to: '/live',              icon: '➼', label: 'Live Stream' },
  { to: '/datasets',          icon: '◉', label: 'Datasets' },
  { to: '/ingestion',         icon: '↑', label: 'Ingestion' },
  { to: '/analysis',          icon: '⌬', label: 'Analysis' },
  { to: '/analysis/algorithms', icon: '▤', label: 'Algorithms' },
  { to: '/analysis/benchmarks', icon: '⏱', label: 'Search Benchmarks' },
  { to: '/runs',              icon: '↻', label: 'Run Sessions' },
  { to: '/system',            icon: '☺', label: 'System' },
  { to: '/docs',              icon: '✎', label: 'Docs' }
];

export default function Layout() {
  const { data: sys } = useApi<SystemStatus>(() => api.systemStatus());

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="sidebar-brand">
          <span className="brand-mark">LI</span>
          <span className="brand-text">LogInsight</span>
        </div>

        <nav className="sidebar-nav">
          {NAV.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.end}
              className={({ isActive }) => `nav-item${isActive ? ' nav-item--active' : ''}`}
            >
              <span className="nav-icon" aria-hidden="true">{item.icon}</span>
              <span className="nav-label">{item.label}</span>
            </NavLink>
          ))}
        </nav>

        <div className="sidebar-footer">
          <span className="version">v3.0 — LogInsight Analyzer</span>
        </div>
      </aside>

      <div className="main-area">
        <header className="app-header">
          <h1>LogInsight Analyzer — Log Intelligence &amp; Investigation</h1>
          <span className="header-sub">
            {sys?.datasetLoaded
              ? `${sys.datasetName} · ${formatNumber(sys.datasetSize)} events`
              : 'No dataset loaded'}
          </span>
        </header>

        <main className="app-main">
          <Outlet />
        </main>
      </div>
    </div>
  );
}