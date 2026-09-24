import { Card } from '../components/ui';

export default function DocsPage() {
  return (
    <div className="page">
      <h2 className="page-title">Documentation</h2>

      <div className="doc-grid">
        <Card title="Project Overview" className="doc-card">
          <p>
            <strong>LogInsight Analyzer</strong> is a log intelligence &amp; investigation
            workspace for the DSA-3 curriculum. It turns raw log lines into an in-memory dataset and
            powers every screen — dashboard, explorer, search, analytics, patterns, incidents and
            services — with real DSA algorithms running over that data: string matchers for search,
            edit distance for fuzzy suggestions, token normalisation for message patterns, windowed
            thresholds for incidents, and measured benchmarks for the engines themselves.
          </p>
          <p>
            Nothing is fabricated: every count, latency, pattern and incident is computed live from
            the loaded dataset, and demo data is clearly labelled.
          </p>
        </Card>

        <Card title="Product Surface" className="doc-card">
          <table className="info-table">
            <tbody>
              <tr><th>Command Center</th><td>live dashboard over the loaded dataset (GET /api/overview)</td></tr>
              <tr><th>Log Explorer</th><td>filter/paginate events and open full detail views</td></tr>
              <tr><th>Search</th><td>structured queries + DSA matchers with measured methodology</td></tr>
              <tr><th>Analytics</th><td>timeline, severity, heatmap, HTTP, hosts</td></tr>
              <tr><th>Patterns</th><td>heuristic token templates — labelled not ML</td></tr>
              <tr><th>Incidents</th><td>threshold-based error windows derived from the dataset</td></tr>
              <tr><th>Services</th><td>per-service rollups and drill-down activity</td></tr>
              <tr><th>Live Stream</th><td>labelled demo replay over SSE ("not real-time")</td></tr>
              <tr><th>Datasets / Ingestion</th><td>demo corpus, sample files and upload with honest parse results</td></tr>
              <tr><th>Algorithm Insights</th><td>the DSA engines behind the product + measured benchmarks</td></tr>
            </tbody>
          </table>
        </Card>

        <Card title="How LogInsight analyzes logs" className="doc-card">
          <table className="info-table">
            <thead><tr><th>Category</th><th>Algorithms</th></tr></thead>
            <tbody>
              <tr><td>Search</td><td>Naive, KMP, Z-Algorithm, Rabin-Karp, Aho-Corasick, Suffix Array, Fuzzy Search</td></tr>
              <tr><td>Fuzzy matching</td><td>Levenshtein, Damerau, Weighted Edit, Needleman-Wunsch, Smith-Waterman</td></tr>
              <tr><td>Repeated-substring / structure</td><td>Matrix Chain, OBST, Suffix Array + Kasai LCP</td></tr>
              <tr><td>Graph &amp; flow analysis</td><td>Ford-Fulkerson, Edmonds-Karp, Dinic, Min-Cut, Bipartite Matching, Min-Cost Flow</td></tr>
              <tr><td>Approximation</td><td>Vertex Cover 2-Approx, Incident Cover, Greedy Set Cover</td></tr>
              <tr><td>Randomized</td><td>Randomized QuickSort, Miller-Rabin Primality, Reservoir Sampling, Universal Hashing</td></tr>
              <tr><td>Aggregation</td><td>Parallel Prefix Sum, Parallel Merge-Sort, Parallel Reduce</td></tr>
            </tbody>
          </table>
        </Card>

        <Card title="Trace replay" className="doc-card">
          <p>
            Thirteen trace-instrumented algorithms record their real operations (comparisons, DP
            cells, augmentation paths, pivot choices) during an execution. Run Sessions replays these
            genuine recorded steps over SSE — nothing is animated from fabricated data.
          </p>
        </Card>

        <Card title="Architecture" className="doc-card">
          <table className="info-table">
            <tbody>
              <tr><th>Layer</th><td>React 18 + TypeScript + Vite → Spring Boot 3.5 + Java 21</td></tr>
              <tr><th>Dataset</th><td>in-memory LogEvent model; demo corpus, bundled samples and file upload</td></tr>
              <tr><th>API Contract</th><td>REST endpoints per controller, JSON wire format with @JsonInclude(NON_NULL)</td></tr>
              <tr><th>Error Handling</th><td>GlobalExceptionHandler → ApiError envelope (no stack traces on wire)</td></tr>
              <tr><th>Testing</th><td>732 backend tests · TypeScript + Vite production build clean</td></tr>
            </tbody>
          </table>
        </Card>

        <Card title="Backend Endpoints" className="doc-card card-full">
          <div className="endpoint-grid">
            <div className="endpoint-group">
              <h3>Health &amp; Dataset</h3>
              <code>GET /api/health</code>
              <code>GET /api/health/status</code>
              <code>GET /api/datasets</code>
              <code>POST /api/datasets/demo</code>
              <code>GET /api/datasets/current</code>
              <code>DELETE /api/datasets</code>
            </div>
            <div className="endpoint-group">
              <h3>Overview &amp; Logs</h3>
              <code>GET /api/overview?range=</code>
              <code>GET /api/logs?limit=&amp;offset=</code>
              <code>GET /api/logs/explore</code>
              <code>GET /api/logs/{'{id}'}</code>
              <code>GET /api/logs/stats</code>
            </div>
            <div className="endpoint-group">
              <h3>Search</h3>
              <code>POST /api/search</code>
              <code>GET /api/search/suggest</code>
              <code>POST /api/search/{'{naive|kmp|z|rabin-karp}'}</code>
              <code>POST /api/string/suffix/build|search</code>
              <code>POST /api/fuzzy/search</code>
            </div>
            <div className="endpoint-group">
              <h3>Analytics &amp; Insights</h3>
              <code>GET /api/patterns</code>
              <code>GET /api/incidents</code>
              <code>GET /api/services</code>
              <code>GET /api/analytics/{'{http|hosts|heatmap|windows|top|errors|dependencies}'}</code>
              <code>GET /api/live (SSE)</code>
            </div>
            <div className="endpoint-group">
              <h3>Dynamic Programming</h3>
              <code>POST /api/dp/{'{levenshtein|damerau|weighted-edit|global|local|matrix-chain|obst|tsp|hamiltonian|tree|rerooting|sos}'}</code>
            </div>
            <div className="endpoint-group">
              <h3>Graph &amp; Flow</h3>
              <code>POST /api/flow</code>
              <code>POST /api/flow/{'{edmonds-karp|dinic|min-cut|matching|min-cost}'}</code>
            </div>
            <div className="endpoint-group">
              <h3>Approximation</h3>
              <code>POST /api/approx/{'{vertex-cover|incident-cover|set-cover}'}</code>
            </div>
            <div className="endpoint-group">
              <h3>Randomized</h3>
              <code>POST /api/random/{'{prime|sample|hash|quicksort}'}</code>
            </div>
            <div className="endpoint-group">
              <h3>Trace replay</h3>
              <code>GET /api/trace/catalog</code>
              <code>POST /api/trace/{'{category}'}/{'{algorithm}'}</code>
              <code>POST /api/runs</code>
              <code>GET /api/runs/{'{id}'}/events</code>
            </div>
            <div className="endpoint-group">
              <h3>Algorithm Insights</h3>
              <code>GET /api/modules</code>
              <code>GET /api/algorithms</code>
              <code>GET /api/analysis/algorithms</code>
              <code>GET /api/analysis/benchmarks/search</code>
              <code>POST /api/benchmark/run</code>
            </div>
          </div>
        </Card>
      </div>
    </div>
  );
}