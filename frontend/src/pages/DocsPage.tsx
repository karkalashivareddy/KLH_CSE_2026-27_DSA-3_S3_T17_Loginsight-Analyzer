import { Link } from 'react-router-dom';
import { BookOpen, Database, ExternalLink, FileSearch, Network, Search, Server, ShieldAlert, Timer, Workflow } from 'lucide-react';
import { Badge, Card, PageHeader } from '../components/ui';

const ENDPOINTS = [
  { group: 'Health and data', endpoints: ['GET /api/health', 'GET /api/health/status', 'GET /api/datasets', 'GET /api/datasets/current', 'POST /api/datasets/demo', 'POST /api/datasets/{name}', 'POST /api/datasets', 'DELETE /api/datasets', 'GET /api/ingestion/status', 'POST /api/ingestion/demo'] },
  { group: 'Logs and search', endpoints: ['GET /api/overview', 'GET /api/logs/explore', 'GET /api/logs/{id}', 'POST /api/search', 'GET /api/search/suggest'] },
  { group: 'Investigation analytics', endpoints: ['GET /api/patterns', 'GET /api/patterns/examples', 'GET /api/incidents', 'GET /api/incidents/{id}', 'GET /api/services', 'GET /api/services/{name}', 'GET /api/analytics/http', 'GET /api/analytics/hosts', 'GET /api/analytics/heatmap'] },
  { group: 'Analysis and replay', endpoints: ['GET /api/analysis/algorithms', 'GET /api/analysis/benchmarks/search', 'GET /api/runs', 'GET /api/runs/{id}', 'GET /api/runs/{id}/events', 'GET /api/live/status', 'GET /api/live'] }
] as const;

export default function DocsPage() {
  return (
    <div className="page">
      <PageHeader eyebrow="Reference" title="Documentation" description="A concise map of the real backend surface and the investigation workflows built on it." actions={<Link className="btn btn-sm" to="/system"><Server size={14} aria-hidden="true" /> System status</Link>} />
      <div className="doc-grid">
        <Card title="Workspace model" className="doc-card">
          <p><strong>LogInsight Analyzer</strong> keeps the active log dataset in the backend and computes each screen from that dataset through API calls. The browser does not synthesize events, counts, latency or incidents.</p>
          <p>Use Datasets or Ingestion to select the source. Dataset mutations invalidate open dataset-backed requests so the workspace refreshes from server state.</p>
          <div className="quick-actions"><Link className="btn btn-sm" to="/datasets"><Database size={13} aria-hidden="true" /> Manage datasets</Link><Link className="btn btn-sm" to="/"><ExternalLink size={13} aria-hidden="true" /> Open command center</Link></div>
        </Card>
        <Card title="Investigation flow" className="doc-card">
          <div className="table-scroll"><table className="info-table"><caption className="sr-only">Investigation workflow</caption><thead><tr><th>Step</th><th>Workflow</th><th>Evidence</th></tr></thead><tbody><tr><th><Database size={14} aria-hidden="true" /> Load</th><td><Link to="/datasets">Choose a bundled sample or upload a log file.</Link></td><td>Parser counts and active dataset state.</td></tr><tr><th><Search size={14} aria-hidden="true" /> Search</th><td><Link to="/search">Run a query and inspect matcher metadata.</Link></td><td>Strategy, algorithm, snippets and measured duration.</td></tr><tr><th><Network size={14} aria-hidden="true" /> Correlate</th><td><Link to="/services">Follow service, host, incident and event links.</Link></td><td>Rollups, supporting events and deep links.</td></tr><tr><th><ShieldAlert size={14} aria-hidden="true" /> Investigate</th><td><Link to="/incidents">Review heuristic elevated-error windows.</Link></td><td>Method string, primary pattern and evidence events.</td></tr><tr><th><Timer size={14} aria-hidden="true" /> Measure</th><td><Link to="/benchmarks">Run a real matcher comparison.</Link></td><td>One server-measured duration per matcher.</td></tr></tbody></table></div>
        </Card>
        <Card title="Methodology and integrity" className="doc-card">
          <p>Search timings, benchmark durations and trace execution times are nanosecond measurements. Event <code>responseTime</code> and HTTP percentile fields are milliseconds; the UI formats each using the matching unit.</p>
          <p>Patterns are heuristic token normalization, not machine learning. Incidents are elevated-error windows derived from the loaded dataset. The live route is a labelled bounded replay, not a real-time capture feed.</p>
          <div className="algo-chips"><Badge tone="info">real API DTOs</Badge><Badge tone="good">server measurements</Badge><Badge tone="warn">heuristics labelled</Badge></div>
        </Card>
        <Card title="Keyboard and accessibility" className="doc-card">
          <p>Use <kbd>Ctrl</kbd> + <kbd>K</kbd> to open workspace navigation. Search and explorer inputs submit on <kbd>Enter</kbd>; result tables expose event inspection buttons with explicit labels.</p>
          <p>Event detail drawers close with <kbd>Esc</kbd>. Every chart and table has a text or caption label, form controls have associated labels, and destructive dataset clearing requires confirmation.</p>
          <div className="quick-actions"><Link className="btn btn-sm" to="/logs"><FileSearch size={13} aria-hidden="true" /> Open explorer</Link><Link className="btn btn-sm" to="/system"><Workflow size={13} aria-hidden="true" /> Check runtime</Link></div>
        </Card>
      </div>
      <Card title="Endpoint map" sub="Endpoints used by the product surfaces in this workspace">
        <div className="endpoint-grid">{ENDPOINTS.map((group) => <section className="endpoint-group" key={group.group}><h3>{group.group}</h3>{group.endpoints.map((endpoint) => <code key={endpoint}>{endpoint}</code>)}</section>)}</div>
      </Card>
      <div className="quick-actions"><Link className="btn btn-sm" to="/analysis"><Workflow size={13} aria-hidden="true" /> Analysis hub</Link><Link className="btn btn-sm" to="/docs"><BookOpen size={13} aria-hidden="true" /> Documentation</Link></div>
    </div>
  );
}
