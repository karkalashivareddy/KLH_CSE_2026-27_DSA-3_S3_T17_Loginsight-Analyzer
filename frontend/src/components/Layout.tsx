import { NavLink, Outlet } from 'react-router-dom';

/**
 * Application shell: sidebar navigation + header + routed content. The sidebar collapses to an
 * icon rail on narrow viewports via CSS media queries.
 */

const NAV = [
  { to: '/',           icon: '◈', label: 'Overview',   end: true },
  { to: '/logs',       icon: '☰', label: 'Logs',        end: false },
  { to: '/analytics',  icon: '◆', label: 'Analytics',   end: false },
  { to: '/datasets',   icon: '◉', label: 'Datasets',    end: false },
  { to: '/lab',        icon: '⚙', label: 'Lab',         end: false },
  { to: '/benchmarks', icon: '▤', label: 'Benchmarks',  end: false },
  { to: '/system',     icon: '☺', label: 'System',      end: false },
  { to: '/docs',       icon: '✎', label: 'Docs',        end: false }
];

export default function Layout() {
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
          <span className="version">v0.1.0</span>
        </div>
      </aside>

      <div className="main-area">
        <header className="app-header">
          <h1>LogInsight Analyzer</h1>
          <span className="header-sub">DSA-3 Algorithmic Log Intelligence</span>
        </header>

        <main className="app-main">
          <Outlet />
        </main>
      </div>
    </div>
  );
}