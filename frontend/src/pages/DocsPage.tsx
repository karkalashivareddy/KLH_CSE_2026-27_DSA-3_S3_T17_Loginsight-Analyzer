import { Link } from 'react-router-dom';
import { BookOpen, Database, ExternalLink, FileSearch, Network, Search, Server, ShieldAlert, Timer, Workflow } from 'lucide-react';
import { Badge, Card, PageHeader } from '../components/ui';

const SIMULATION_ENDPOINTS = [
  'GET /api/scenarios',
  'GET /api/scenarios/{id}',
  'GET /api/scenarios/default',
  'GET /api/simulation/status',
  'GET /api/simulation/stream',
  'GET /api/simulation/sample',
  'GET /api/simulation/incidents',
  'POST /api/simulation/incidents/{id}/transition',
  'GET /api/simulation/lifecycle'
] as const;

const ALGORITHM_ROLES = [
  {
    algorithm: 'Aho-Corasick',
    role: 'Algorithm engine',
    where: 'Error/security signature scan on every simulation frame, and dataset pattern analysis',
    complexity: 'O(n + m)',
    note: 'Scans the whole rolling window each frame, so several signatures are matched in one pass.'
  },
  {
    algorithm: 'KMP',
    role: 'Algorithm engine',
    where: 'Confirms the strongest matched signature; also the known-pattern search mode',
    complexity: 'O(n + m)',
    note: 'Used to confirm rather than discover, so a single candidate is verified without rescanning for prefixes.'
  },
  {
    algorithm: 'Sliding-window aggregation',
    role: 'Product feature',
    where: 'Error rate, throughput and latency percentile for the live window',
    complexity: 'O(window)',
    note: 'This is measurement, not a DSA demonstration; it is what every other signal depends on.'
  },
  {
    algorithm: 'BFS over the dependency graph',
    role: 'Analytical capability',
    where: 'Blast radius of a failing service, walking callers upstream',
    complexity: 'O(V + E)',
    note: 'Plain breadth-first traversal on the declared dependency graph. It reports reachability, never a probability.'
  },
  {
    algorithm: 'Naive, Z, Rabin-Karp, Suffix Array + Kasai',
    role: 'Academic lab',
    where: 'Algorithm Lab and matcher benchmarks',
    complexity: 'varies per matcher',
    note: 'Kept as reference implementations and comparison baselines; never presented as the production detector.'
  },
  {
    algorithm: 'Levenshtein and other DP engines',
    role: 'Algorithm engine / academic lab',
    where: 'Fuzzy message grouping in search; the rest are executed in the lab only',
    complexity: 'O(nm)',
    note: 'Only the edit-distance family has a measured log-analysis use case in this repository.'
  },
  {
    algorithm: 'Flow, approximation, randomized, parallel',
    role: 'Academic lab',
    where: 'Algorithm Lab and Benchmarks',
    complexity: 'per module',
    note: 'Present because the DSA-3 syllabus requires them, not because incident detection needs them. No such claim is made anywhere in the product.'
  }
] as const;

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
          <p>Patterns are heuristic token normalization, not machine learning. Detector incidents are elevated-error windows derived from the loaded dataset. <code>/replay</code> is a labelled bounded dataset replay and <code>/live</code> is labelled generated traffic; neither claims to be a real-time capture feed.</p>
          <div className="algo-chips"><Badge tone="info">real API DTOs</Badge><Badge tone="good">server measurements</Badge><Badge tone="warn">heuristics labelled</Badge></div>
        </Card>
        <Card title="Keyboard and accessibility" className="doc-card">
          <p>Use <kbd>Ctrl</kbd> + <kbd>K</kbd> to open workspace navigation. Search and explorer inputs submit on <kbd>Enter</kbd>; result tables expose event inspection buttons with explicit labels.</p>
          <p>Event detail drawers close with <kbd>Esc</kbd>. Every chart and table has a text or caption label, form controls have associated labels, and destructive dataset clearing requires confirmation.</p>
          <div className="quick-actions"><Link className="btn btn-sm" to="/logs"><FileSearch size={13} aria-hidden="true" /> Open explorer</Link><Link className="btn btn-sm" to="/system"><Workflow size={13} aria-hidden="true" /> Check runtime</Link></div>
        </Card>
      </div>
      <Card title="Deterministic simulation" sub="A second, clearly separated data source">
        <p>
          <strong>Live Monitor</strong> and <strong>Scenario Lab</strong> read generated traffic from{' '}
          <code>/api/simulation</code>. This is not a capture feed and is never merged with the dataset: every event it
          emits is labelled <code>source: live-simulation</code> and <code>generated &middot; not captured</code> in the
          interface.
        </p>
        <div className="table-scroll">
          <table className="info-table">
            <caption className="sr-only">Simulation determinism model</caption>
            <thead>
              <tr>
                <th>Property</th>
                <th>Behaviour</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <th>Determinism</th>
                <td>
                  Event content is a pure function of <code>(scenario, seed, tick)</code> on a{' '}
                  <code>250&nbsp;ms</code> logical tick. Two runs with the same pair emit the same events in the same
                  order.
                </td>
              </tr>
              <tr>
                <th>Speed</th>
                <td>
                  The <code>speed</code> control changes wall-clock delivery pace only. It never changes content, so a
                  0.25&times; run and an 8&times; run of the same seed are equivalent.
                </td>
              </tr>
              <tr>
                <th>Measured runtime</th>
                <td>
                  <code>runtimeNanos</code> and <code>runtimeMicros</code> on each evidence entry are real timings of
                  that invocation and therefore differ between runs. They are excluded from determinism comparisons.
                </td>
              </tr>
              <tr>
                <th>Phases</th>
                <td>
                  <code>healthy → onset → degrading → peak</code>, derived from the scenario intensity curve, so the
                  label can never disagree with the measurements on the same frame.
                </td>
              </tr>
              <tr>
                <th>Incidents</th>
                <td>
                  Candidates open only when measured thresholds are crossed, and move through{' '}
                  <code>DETECTED → INVESTIGATING → ACKNOWLEDGED → MITIGATED → RESOLVED</code>. Status changes are
                  validated by the server and stored for the session only.
                </td>
              </tr>
              <tr>
                <th>Boundaries</th>
                <td>
                  Streams, worker threads, queue depth, frame budget, session retention and incident count are all
                  bounded, and a preview frame is retained so operator actions still address a real session.
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <div className="quick-actions">
          <Link className="btn btn-sm" to="/scenario-lab">
            <Server size={13} aria-hidden="true" /> Scenario Lab
          </Link>
          <Link className="btn btn-sm" to="/live">
            <Network size={13} aria-hidden="true" /> Live Monitor
          </Link>
          <Link className="btn btn-sm" to="/incidents/workbench">
            <ShieldAlert size={13} aria-hidden="true" /> Incident workbench
          </Link>
        </div>
      </Card>

      <Card title="Algorithm roles" sub="Every algorithm is classified, and nothing is claimed to be a detector when it is not">
        <p>
          A syllabus algorithm is only allowed in this product if it also has a real log-analysis use case. Where that
          is not true, the algorithm stays in the Algorithm Lab and is labelled as a demonstration.
        </p>
        <div className="table-scroll">
          <table className="info-table">
            <caption className="sr-only">Algorithm classification</caption>
            <thead>
              <tr>
                <th>Algorithm</th>
                <th>Classification</th>
                <th>Where it runs</th>
                <th>Complexity</th>
                <th>Honest note</th>
              </tr>
            </thead>
            <tbody>
              {ALGORITHM_ROLES.map((row) => (
                <tr key={row.algorithm}>
                  <th>{row.algorithm}</th>
                  <td>
                    <Badge tone={row.role === 'Product feature' ? 'good' : row.role === 'Academic lab' ? 'warn' : 'info'}>
                      {row.role}
                    </Badge>
                  </td>
                  <td>{row.where}</td>
                  <td className="mono">{row.complexity}</td>
                  <td>{row.note}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </Card>

      <Card title="Endpoint map" sub="Endpoints used by the product surfaces in this workspace">
        <div className="endpoint-grid">{ENDPOINTS.map((group) => <section className="endpoint-group" key={group.group}><h3>{group.group}</h3>{group.endpoints.map((endpoint) => <code key={endpoint}>{endpoint}</code>)}</section>)}<section className="endpoint-group"><h3>Deterministic simulation</h3>{SIMULATION_ENDPOINTS.map((endpoint) => <code key={endpoint}>{endpoint}</code>)}</section></div>
      </Card>
      <div className="quick-actions"><Link className="btn btn-sm" to="/analysis"><Workflow size={13} aria-hidden="true" /> Analysis hub</Link><Link className="btn btn-sm" to="/docs"><BookOpen size={13} aria-hidden="true" /> Documentation</Link></div>
    </div>
  );
}
