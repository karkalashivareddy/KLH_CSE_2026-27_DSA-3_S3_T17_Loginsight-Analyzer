import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Link, NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom';
import {
  Activity,
  BarChart3,
  BookOpen,
  ChevronRight,
  CircleGauge,
  Command,
  Database,
  FileSearch,
  FolderKanban,
  History,
  LayoutDashboard,
  Menu,
  Network,
  PanelLeftClose,
  PanelLeftOpen,
  Radio,
  Search,
  Server,
  ShieldAlert,
  Timer,
  Workflow,
  X,
  type LucideIcon
} from 'lucide-react';
import { api } from '../api/client';
import type { LiveStatus, SystemStatus } from '../api/types';
import { formatNumber } from './format';
import { PROJECT } from '../types/project';
import { useApi } from '../hooks/useApi';

type Tone = 'good' | 'muted' | 'warn' | 'danger' | 'info';

interface NavEntry {
  to: string;
  label: string;
  icon: LucideIcon;
  end?: boolean;
  keywords?: string;
  aliases?: string[];
}

interface NavGroup {
  label: string;
  entries: NavEntry[];
}

const NAV_GROUPS: NavGroup[] = [
  {
    label: 'Command Center',
    entries: [
      { to: '/', label: 'Command Center', icon: LayoutDashboard, end: true, aliases: ['/command-center'], keywords: 'overview dashboard home' }
    ]
  },
  {
    label: 'Investigate',
    entries: [
      { to: '/logs', label: 'Log Explorer', icon: FileSearch, keywords: 'events explorer query' },
      { to: '/search', label: 'Search', icon: Search, keywords: 'query matcher fuzzy' },
      { to: '/patterns', label: 'Patterns', icon: Workflow, keywords: 'templates recurrence' },
      { to: '/incidents', label: 'Incidents', icon: ShieldAlert, aliases: ['/investigate'], keywords: 'alerts investigation evidence' },
      { to: '/live', label: 'Live Replay', icon: Radio, keywords: 'stream sse replay' }
    ]
  },
  {
    label: 'Analyze',
    entries: [
      { to: '/analytics', label: 'Analytics', icon: BarChart3, aliases: ['/analyze'], keywords: 'charts traffic severity http hosts' },
      { to: '/services', label: 'Services', icon: Network, keywords: 'fleet dependencies hosts' }
    ]
  },
  {
    label: 'Algorithm Lab',
    entries: [
      { to: '/analysis', label: 'Lab overview', icon: CircleGauge, end: true, aliases: ['/lab', '/algorithm-lab'], keywords: 'engines laboratory' },
      { to: '/algorithms', label: 'Algorithms', icon: Workflow, aliases: ['/analysis/algorithms'], keywords: 'catalogue dsa engines' },
      { to: '/benchmarks', label: 'Benchmarks', icon: Timer, aliases: ['/analysis/benchmarks'], keywords: 'search measured performance' },
      { to: '/runs', label: 'Run Sessions', icon: History, keywords: 'trace replay sessions' }
    ]
  },
  {
    label: 'Data',
    entries: [
      { to: '/datasets', label: 'Datasets', icon: Database, end: true, aliases: ['/data'], keywords: 'load dataset current' },
      { to: '/ingestion', label: 'Ingestion', icon: FolderKanban, keywords: 'parser upload files' }
    ]
  },
  {
    label: 'System',
    entries: [
      { to: '/system', label: 'System', icon: Server, aliases: ['/system/status'], keywords: 'health backend runtime modules' },
      { to: '/docs', label: 'Documentation', icon: BookOpen, keywords: 'guide api reference' }
    ]
  }
];

const ALL_ENTRIES = NAV_GROUPS.flatMap((group) => group.entries);

function normalizePath(pathname: string): string {
  if (pathname === '/') return pathname;
  return pathname.replace(/\/+$/, '') || '/';
}

function entryPaths(entry: NavEntry): string[] {
  return [entry.to, ...(entry.aliases ?? [])].map(normalizePath);
}

function entryIsActive(entry: NavEntry, pathname: string): boolean {
  const path = normalizePath(pathname);
  return entryPaths(entry).some((target) => {
    if (entry.end || target === '/') return path === target;
    return path === target || path.startsWith(`${target}/`);
  });
}

function currentEntry(pathname: string): NavEntry | undefined {
  return [...ALL_ENTRIES]
    .sort((a, b) => Math.max(...entryPaths(b).map((value) => value.length)) - Math.max(...entryPaths(a).map((value) => value.length)))
    .find((entry) => entryIsActive(entry, pathname));
}

function readableSegment(value: string): string {
  try {
    return decodeURIComponent(value);
  } catch {
    return value;
  }
}

function pageCrumbs(pathname: string): Array<{ label: string; to?: string }> {
  const path = normalizePath(pathname);
  const entry = currentEntry(path);
  const crumbs: Array<{ label: string; to?: string }> = [{ label: 'Workspace', to: '/' }];
  if (!entry) return [{ label: 'Workspace' }, { label: 'Not found' }];

  const group = NAV_GROUPS.find((candidate) => candidate.entries.includes(entry));
  if (group) {
    const groupTarget = group.entries[0]?.to;
    crumbs.push({ label: group.label, to: groupTarget });
    if (entry.label !== group.label) crumbs.push({ label: entry.label, to: entry.to });
  }

  const segments = path.split('/').filter(Boolean);
  if (segments.length > 1 && ['logs', 'incidents', 'services', 'runs'].includes(segments[0])) {
    const detail = segments[segments.length - 1];
    const base = segments[0];
    const label = base === 'logs'
      ? `Event #${readableSegment(detail)}`
      : base === 'incidents'
        ? `Incident #${readableSegment(detail)}`
        : base === 'services'
          ? `Service ${readableSegment(detail)}`
          : `Run ${readableSegment(detail)}`;
    crumbs.push({ label });
  }

  return crumbs;
}

function StatusDot({ tone, label }: { tone: Tone; label: string }) {
  return <span className={`status-dot status-dot--${tone}`} role="img" aria-label={label} title={label} />;
}

function StatusChip({ icon: Icon, label, value, detail, tone, to }: {
  icon: LucideIcon;
  label: string;
  value: string;
  detail?: string;
  tone: Tone;
  to?: string;
}) {
  const content = (
    <>
      <Icon size={14} aria-hidden="true" />
      <span className="status-chip-copy">
        <span className="status-chip-label">{label}</span>
        <span className="status-chip-value">{value}</span>
      </span>
      {detail && <span className="status-chip-detail">{detail}</span>}
      <StatusDot tone={tone} label={`${label}: ${value}`} />
    </>
  );
  const className = 'header-status-chip';
  if (to) {
    return <NavLink className={className} to={to} aria-label={`${label}: ${value}`} title={`${label}: ${value}`}>{content}</NavLink>;
  }
  return <div className={className} role="status" aria-label={`${label}: ${value}`} title={`${label}: ${value}`}>{content}</div>;
}

interface PaletteItem {
  id: string;
  label: string;
  description: string;
  group: 'Navigation' | 'Actions';
  icon: LucideIcon;
  keywords: string;
  run: () => void;
}

function CommandPalette({ open, onClose, onToggleSidebar, sidebarCollapsed }: {
  open: boolean;
  onClose: () => void;
  onToggleSidebar: () => void;
  sidebarCollapsed: boolean;
}) {
  const navigate = useNavigate();
  const inputRef = useRef<HTMLInputElement>(null);
  const dialogRef = useRef<HTMLElement>(null);
  const previousFocusRef = useRef<HTMLElement | null>(null);
  const [query, setQuery] = useState('');
  const [activeIndex, setActiveIndex] = useState(0);

  const navigationItems = useMemo<PaletteItem[]>(() => ALL_ENTRIES.map((entry) => {
    const group = NAV_GROUPS.find((candidate) => candidate.entries.includes(entry));
    return {
      id: `nav:${entry.to}`,
      label: entry.label,
      description: `${group?.label ?? 'Workspace'} · ${entry.to}`,
      group: 'Navigation',
      icon: entry.icon,
      keywords: `${entry.label} ${entry.keywords ?? ''} ${group?.label ?? ''} ${entry.to}`,
      run: () => navigate(entry.to)
    };
  }), [navigate]);

  const actionItems = useMemo<PaletteItem[]>(() => [
    {
      id: 'action:sidebar',
      label: sidebarCollapsed ? 'Expand sidebar' : 'Collapse sidebar',
      description: 'Toggle compact navigation',
      group: 'Actions',
      icon: sidebarCollapsed ? PanelLeftOpen : PanelLeftClose,
      keywords: 'sidebar navigation compact expand collapse',
      run: onToggleSidebar
    },
    {
      id: 'action:live',
      label: 'Open live replay',
      description: 'Start a bounded dataset replay',
      group: 'Actions',
      icon: Radio,
      keywords: 'live replay stream sse',
      run: () => navigate('/live')
    },
    {
      id: 'action:dataset',
      label: 'Manage active dataset',
      description: 'Load or replace the current dataset',
      group: 'Actions',
      icon: Database,
      keywords: 'dataset load replace data',
      run: () => navigate('/datasets')
    },
    {
      id: 'action:health',
      label: 'Inspect backend health',
      description: 'Review runtime and module status',
      group: 'Actions',
      icon: Server,
      keywords: 'backend health runtime system',
      run: () => navigate('/system')
    }
  ], [navigate, onToggleSidebar, sidebarCollapsed]);

  const items = useMemo(() => [...navigationItems, ...actionItems], [actionItems, navigationItems]);
  const results = useMemo(() => {
    const normalized = query.trim().toLowerCase();
    if (!normalized) return items;
    return items.filter((item) => item.keywords.toLowerCase().includes(normalized) || item.label.toLowerCase().includes(normalized));
  }, [items, query]);

  useEffect(() => {
    if (!open) {
      setQuery('');
      setActiveIndex(0);
      return;
    }
    previousFocusRef.current = document.activeElement instanceof HTMLElement ? document.activeElement : null;
    const timer = window.setTimeout(() => inputRef.current?.focus(), 0);
    return () => window.clearTimeout(timer);
  }, [open]);

  useEffect(() => {
    setActiveIndex(0);
  }, [query]);

  useEffect(() => {
    if (!open) return;
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        event.preventDefault();
        onClose();
        return;
      }
      if (event.key === 'Tab' && dialogRef.current) {
        const focusable = [...dialogRef.current.querySelectorAll<HTMLElement>('button, input, [href], [tabindex]:not([tabindex="-1"])')].filter((element) => !element.hasAttribute('disabled'));
        if (focusable.length === 0) return;
        const first = focusable[0];
        const last = focusable[focusable.length - 1];
        if (event.shiftKey && document.activeElement === first) {
          event.preventDefault();
          last.focus();
        } else if (!event.shiftKey && document.activeElement === last) {
          event.preventDefault();
          first.focus();
        }
      }
    };
    document.addEventListener('keydown', onKeyDown, true);
    return () => document.removeEventListener('keydown', onKeyDown, true);
  }, [onClose, open]);

  useEffect(() => {
    if (open) return;
    previousFocusRef.current?.focus();
    previousFocusRef.current = null;
  }, [open]);

  if (!open) return null;

  const choose = (item: PaletteItem | undefined) => {
    if (!item) return;
    item.run();
    onClose();
  };

  const move = (direction: 1 | -1) => {
    if (results.length === 0) return;
    setActiveIndex((index) => (index + direction + results.length) % results.length);
  };

  const renderGroup = (group: PaletteItem['group']) => {
    const groupItems = results.filter((item) => item.group === group);
    if (groupItems.length === 0) return null;
    return (
      <div className="command-group" key={group}>
        <div className="command-heading">{group}</div>
        <div className="command-results" role="listbox" aria-label={`${group} commands`}>
          {groupItems.map((item) => {
            const Icon = item.icon;
            const resultIndex = results.indexOf(item);
            return (
              <button
                key={item.id}
                id={`command-item-${item.id.replace(/[^a-z0-9]/gi, '-')}`}
                className={`command-result${resultIndex === activeIndex ? ' command-result--active' : ''}`}
                type="button"
                role="option"
                aria-selected={resultIndex === activeIndex}
                onMouseEnter={() => setActiveIndex(resultIndex)}
                onClick={() => choose(item)}
              >
                <Icon size={17} aria-hidden="true" />
                <span className="command-result-copy">
                  <span className="command-result-label">{item.label}</span>
                  <span className="command-result-description">{item.description}</span>
                </span>
                <span className="command-result-path">{item.group === 'Navigation' ? item.description.split(' · ')[1] : 'Action'}</span>
              </button>
            );
          })}
        </div>
      </div>
    );
  };

  return (
    <div className="command-overlay" role="presentation" onMouseDown={(event) => event.target === event.currentTarget && onClose()}>
      <section ref={dialogRef} className="command-dialog" role="dialog" aria-modal="true" aria-labelledby="command-title" aria-describedby="command-description">
        <div className="command-dialog-head">
          <div>
            <div className="command-dialog-kicker"><Command size={14} aria-hidden="true" /> Command palette</div>
            <h2 id="command-title">Move through LogInsight</h2>
            <p id="command-description">Open a workspace or run a quick workspace action.</p>
          </div>
          <button className="icon-btn" type="button" onClick={onClose} aria-label="Close command palette" title="Close command palette">
            <X size={17} aria-hidden="true" />
          </button>
        </div>
        <div className="command-search-row">
          <Search size={17} aria-hidden="true" />
          <input
            ref={inputRef}
            className="command-input"
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            onKeyDown={(event) => {
              if (event.key === 'ArrowDown') {
                event.preventDefault();
                move(1);
              } else if (event.key === 'ArrowUp') {
                event.preventDefault();
                move(-1);
              } else if (event.key === 'Home') {
                event.preventDefault();
                setActiveIndex(0);
              } else if (event.key === 'End') {
                event.preventDefault();
                setActiveIndex(Math.max(0, results.length - 1));
              } else if (event.key === 'Enter') {
                event.preventDefault();
                choose(results[activeIndex]);
              }
            }}
            placeholder="Search navigation and actions…"
            aria-label="Search navigation and actions"
            aria-controls="command-results"
            aria-activedescendant={results[activeIndex] ? `command-item-${results[activeIndex].id.replace(/[^a-z0-9]/gi, '-')}` : undefined}
          />
          <kbd>Esc</kbd>
        </div>
        <div id="command-results" className="command-content">
          {results.length === 0 ? <div className="command-empty">No matching workspace or action.</div> : <>{renderGroup('Navigation')}{renderGroup('Actions')}</>}
        </div>
        <div className="command-footer">
          <span>Navigate with arrow keys</span>
          <kbd>Enter</kbd>
          <span>Close</span>
          <kbd>Esc</kbd>
        </div>
      </section>
    </div>
  );
}

export default function Layout() {
  const location = useLocation();
  const [collapsed, setCollapsed] = useState(() => {
    if (typeof window === 'undefined') return false;
    try {
      return window.localStorage.getItem('loginsight-sidebar-collapsed') === 'true';
    } catch {
      return false;
    }
  });
  const [mobileOpen, setMobileOpen] = useState(false);
  const [paletteOpen, setPaletteOpen] = useState(false);
  const sidebarRef = useRef<HTMLElement>(null);
  const menuButtonRef = useRef<HTMLButtonElement>(null);
  const system = useApi<SystemStatus>((signal) => api.systemStatus({ signal }));
  const live = useApi<LiveStatus>((signal) => api.liveStatus({ signal }));
  const crumbs = useMemo(() => pageCrumbs(location.pathname), [location.pathname]);

  const closeMobile = useCallback(() => {
    setMobileOpen(false);
    window.setTimeout(() => menuButtonRef.current?.focus(), 0);
  }, []);

  const toggleCollapsed = useCallback(() => {
    setCollapsed((value) => {
      const next = !value;
      try {
        window.localStorage.setItem('loginsight-sidebar-collapsed', String(next));
      } catch {
        return next;
      }
      return next;
    });
  }, []);

  const closePalette = useCallback(() => setPaletteOpen(false), []);

  useEffect(() => {
    setMobileOpen(false);
  }, [location.pathname, location.search]);

  useEffect(() => {
    const onKeyDown = (event: KeyboardEvent) => {
      if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === 'k') {
        event.preventDefault();
        setPaletteOpen((value) => !value);
      }
    };
    window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
  }, []);

  useEffect(() => {
    document.body.classList.toggle('nav-open', mobileOpen);
    return () => document.body.classList.remove('nav-open');
  }, [mobileOpen]);

  useEffect(() => {
    if (!mobileOpen) return;
    const timer = window.setTimeout(() => sidebarRef.current?.querySelector<HTMLElement>('.nav-item')?.focus(), 0);
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        event.preventDefault();
        closeMobile();
        return;
      }
      if (event.key !== 'Tab' || !sidebarRef.current) return;
      const focusable = [...sidebarRef.current.querySelectorAll<HTMLElement>('button, a, [tabindex]:not([tabindex="-1"])')].filter((element) => !element.hasAttribute('disabled'));
      if (focusable.length === 0) return;
      const first = focusable[0];
      const last = focusable[focusable.length - 1];
      if (event.shiftKey && document.activeElement === first) {
        event.preventDefault();
        last.focus();
      } else if (!event.shiftKey && document.activeElement === last) {
        event.preventDefault();
        first.focus();
      }
    };
    document.addEventListener('keydown', onKeyDown);
    return () => {
      window.clearTimeout(timer);
      document.removeEventListener('keydown', onKeyDown);
    };
  }, [closeMobile, mobileOpen]);

  const systemTone: Tone = system.error ? 'danger' : system.data?.status === 'UP' ? 'good' : system.loading ? 'muted' : 'warn';
  const systemLabel = system.error ? 'Unavailable' : system.data?.status === 'UP' ? 'Connected' : system.data?.status ?? 'Checking';
  const datasetLoaded = Boolean(system.data?.datasetLoaded);
  const datasetLabel = system.error ? 'Unavailable' : system.loading && !system.data ? 'Checking' : datasetLoaded ? system.data?.datasetName ?? 'Loaded dataset' : 'No dataset';
  const datasetTone: Tone = system.error ? 'danger' : datasetLoaded ? 'good' : 'muted';
  const liveTone: Tone = live.error ? 'danger' : live.data?.enabled ? 'good' : 'muted';
  const liveLabel = live.error ? 'Unavailable' : live.loading && !live.data ? 'Checking' : live.data?.enabled ? 'Ready' : 'Standby';
  const datasetDetail = system.error ? 'Unavailable' : system.loading && !system.data ? 'Checking status' : datasetLoaded ? `${formatNumber(system.data?.datasetSize ?? 0)} events` : 'Load a source';
  const liveDetail = live.error ? 'Unavailable' : live.loading && !live.data ? 'Checking status' : live.data?.dataset ?? 'No replay source';

  return (
    <>
      <a className="skip-link" href="#main-content">Skip to main content</a>
      <div className={`app-shell${collapsed ? ' app-shell--collapsed' : ''}${mobileOpen ? ' app-shell--mobile-open' : ''}`}>
        <aside ref={sidebarRef} id="primary-navigation" className="sidebar" aria-label="Primary navigation">
          <div className="sidebar-brand">
            <Link className="brand-link" to="/" aria-label="LogInsight Analyzer home">
              <span className="brand-mark" aria-hidden="true"><Activity size={19} strokeWidth={2.2} /></span>
              <span className="brand-copy">
                <span className="brand-text">LogInsight</span>
                <span className="brand-tagline">Observability workspace</span>
              </span>
            </Link>
            <button className="icon-btn mobile-close-btn" type="button" onClick={closeMobile} aria-label="Close navigation" title="Close navigation">
              <X size={18} aria-hidden="true" />
            </button>
          </div>

          <div className="sidebar-workspace">
            <span className="workspace-kicker">Workspace</span>
            <span className="workspace-name">Log intelligence</span>
            <span className="workspace-version">Build {PROJECT.version}</span>
          </div>

          <div className="sidebar-controls">
            <span className="sidebar-section-label">Navigation</span>
            <button className="icon-btn sidebar-collapse" type="button" onClick={toggleCollapsed} aria-label={collapsed ? 'Expand navigation' : 'Collapse navigation'} title={collapsed ? 'Expand navigation' : 'Collapse navigation'}>
              {collapsed ? <PanelLeftOpen size={17} aria-hidden="true" /> : <PanelLeftClose size={17} aria-hidden="true" />}
            </button>
          </div>

          <nav className="sidebar-nav" aria-label="Workspace sections">
            {NAV_GROUPS.map((group) => (
              <div className="nav-group" key={group.label}>
                <div className="nav-group-label"><span>{group.label}</span><span className="nav-group-rule" aria-hidden="true" /></div>
                {group.entries.map((entry) => {
                  const Icon = entry.icon;
                  const active = entryIsActive(entry, location.pathname);
                  return (
                    <NavLink
                      key={entry.to}
                      to={entry.to}
                      end={entry.end}
                      className={`nav-item${active ? ' nav-item--active' : ''}`}
                      aria-label={entry.label}
                      aria-current={active ? 'page' : undefined}
                      title={entry.label}
                    >
                      <span className="nav-item-indicator" aria-hidden="true" />
                      <Icon className="nav-icon" size={17} strokeWidth={1.8} aria-hidden="true" />
                      <span className="nav-label">{entry.label}</span>
                    </NavLink>
                  );
                })}
              </div>
            ))}
          </nav>

          <div className="sidebar-footer">
            <div className="sidebar-health">
              <StatusDot tone={systemTone} label={`Backend ${systemLabel}`} />
              <span>API {systemLabel.toLowerCase()}</span>
            </div>
            <div className="sidebar-footer-meta"><span>Local workspace</span><span>v{PROJECT.version}</span></div>
          </div>
        </aside>

        {mobileOpen && <button className="mobile-scrim" type="button" onClick={closeMobile} aria-label="Close navigation" />}

        <div className="main-area">
          <header className="app-header" aria-label="Application toolbar">
            <div className="header-leading">
              <button ref={menuButtonRef} className="icon-btn mobile-menu-btn" type="button" onClick={() => setMobileOpen(true)} aria-label="Open navigation" aria-controls="primary-navigation" aria-expanded={mobileOpen}>
                <Menu size={19} aria-hidden="true" />
              </button>
              <div className="header-context">
                <nav className="breadcrumbs" aria-label="Breadcrumb">
                  {crumbs.map((crumb, index) => (
                    <span className="breadcrumb-piece" key={`${crumb.label}-${index}`}>
                      {index > 0 && <ChevronRight size={13} aria-hidden="true" />}
                      {crumb.to && index < crumbs.length - 1 ? <Link to={crumb.to}>{crumb.label}</Link> : <span aria-current={index === crumbs.length - 1 ? 'page' : undefined}>{crumb.label}</span>}
                    </span>
                  ))}
                </nav>
                <div className="connection-indicator" role="status" aria-live="polite">
                  <StatusDot tone={systemTone} label={`Backend ${systemLabel}`} />
                  <span>Backend {systemLabel.toLowerCase()}</span>
                  <span className="connection-divider" aria-hidden="true" />
                  <span>Demo replay channel {liveLabel.toLowerCase()}</span>
                </div>
              </div>
            </div>
            <div className="header-actions">
              <div className="header-status-group" aria-label="Runtime status">
                <StatusChip icon={Database} label="Dataset" value={datasetLabel} detail={datasetDetail} tone={datasetTone} to="/datasets" />
                <StatusChip icon={Radio} label="Demo replay" value={liveLabel} detail={liveDetail} tone={liveTone} to="/live" />
                <StatusChip icon={Server} label="Backend" value={systemLabel} detail={system.data?.engines ? `${system.data.engines} engines` : 'Runtime'} tone={systemTone} to="/system" />
              </div>
              <button className="command-trigger" type="button" onClick={() => setPaletteOpen(true)} aria-label="Open command palette" title="Open command palette">
                <Command size={15} aria-hidden="true" />
                <span>Command</span>
                <kbd>Ctrl K</kbd>
              </button>
            </div>
          </header>

          <main className="app-main" id="main-content" tabIndex={-1}>
            <Outlet />
          </main>
        </div>
      </div>
      <CommandPalette open={paletteOpen} onClose={closePalette} onToggleSidebar={toggleCollapsed} sidebarCollapsed={collapsed} />
    </>
  );
}
