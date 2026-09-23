import { useState } from 'react';
import { NavLink, Outlet } from 'react-router-dom';
import { useApi } from '../hooks/useApi';
import { api } from '../api/client';
import type { ModuleInfo } from '../api/types';
import { moduleAccent } from './format';

/**
 * Application shell: sidebar navigation + header + routed content. The sidebar collapses to an
 * icon rail on narrow viewports via CSS media queries. Module entries are fetched from the real
 * catalogue when available so the lab nav always reflects the backend.
 */

const NAV = [
  { to: '/',           icon: '◈', label: 'Command Center', end: true },
  { to: '/text-hack',  icon: '⌗', label: 'TextHack',       end: false },
  { to: '/labs',       icon: '⚙', label: 'Laboratory',     end: false },
  { to: '/runs',       icon: '↻', label: 'Run Sessions',   end: false },
  { to: '/course-map', icon: '≈', label: 'Course Map',     end: false },
  { to: '/analytics',  icon: '◆', label: 'Log Analytics',  end: false },
  { to: '/logs',       icon: '☰', label: 'Logs',           end: false },
  { to: '/benchmarks', icon: '▤', label: 'Benchmarks',     end: false },
  { to: '/datasets',   icon: '◉', label: 'Datasets',       end: false },
  { to: '/system',     icon: '☺', label: 'System',         end: false },
  { to: '/docs',       icon: '✎', label: 'Docs',           end: false }
];

export default function Layout() {
  const { data: modules } = useApi<ModuleInfo[]>(() => api.modules());
  const [modsOpen, setModsOpen] = useState(false);

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="sidebar-brand">
          <span className="brand-mark">TH</span>
          <span className="brand-text">TextHack</span>
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

          {modules && modules.length > 0 && (
            <div style={{ marginTop: '0.4rem' }}>
              <button
                type="button"
                className="nav-item"
                style={{ width: '100%', background: 'none', border: 'none', color: 'var(--text-muted)', cursor: 'pointer' }}
                onClick={() => setModsOpen((o) => !o)}
                aria-expanded={modsOpen}
              >
                <span className="nav-icon" aria-hidden="true">◆</span>
                <span className="nav-label">Per-module labs {modsOpen ? '▾' : '▸'}</span>
              </button>
              {modsOpen && (
                <div style={{ paddingLeft: '1rem', display: 'flex', flexDirection: 'column', gap: '2px' }}>
                  {modules.map((mod) => (
                    <NavLink
                      key={mod.id}
                      to={`/labs/${mod.id}`}
                      className={({ isActive }) => `nav-item${isActive ? ' nav-item--active' : ''}`}
                      style={{ fontSize: '0.8rem' }}
                    >
                      <span className="nav-icon" aria-hidden="true" style={{ color: moduleAccent(mod.id) }}>▪</span>
                      <span className="nav-label">{mod.title}</span>
                    </NavLink>
                  ))}
                </div>
              )}
            </div>
          )}
        </nav>

        <div className="sidebar-footer">
          <span className="version">v2.0 — DSA-3 TextHack</span>
        </div>
      </aside>

      <div className="main-area">
        <header className="app-header">
          <h1>TextHack — Advanced Algorithms Laboratory</h1>
          <span className="header-sub">Real algorithms · real traces · DSA-3 Modules 1–6</span>
        </header>

        <main className="app-main">
          <Outlet />
        </main>
      </div>
    </div>
  );
}