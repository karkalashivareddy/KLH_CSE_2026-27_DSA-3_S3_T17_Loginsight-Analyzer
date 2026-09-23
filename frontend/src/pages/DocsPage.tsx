import { Card } from '../components/ui';

export default function DocsPage() {
  return (
    <div className="page">
      <h2 className="page-title">Documentation</h2>

      <div className="doc-grid">
        <Card title="Project Overview" className="doc-card">
          <p>
            <strong>TextHack</strong> is the DSA-3 Advanced Algorithmic Log Intelligence and
            Text Analytics laboratory built for the KLH CSE 2026-27 curriculum. It applies core DSA
            algorithms to real log analysis problems using a Spring Boot + React architecture with
            no fabricated data — every step shown in a replay was recorded by the real engine.
          </p>
        </Card>

        <Card title="Modules &amp; TextHack (Phases 2–3)" className="doc-card">
          <table className="info-table">
            <tbody>
              <tr><th>Framing</th><td>6 modules (strings, dp, flow, approximation, randomized, parallel) exposed via the catalogue</td></tr>
              <tr><th>TextHack Console</th><td>Pattern Search, Fuzzy Match, Document Similarity, Dependency Flow, Project Scheduling, Prime Testing</td></tr>
              <tr><th>Run Sessions</th><td>Every trace-instrumented execution is recorded and replayed over SSE (meta / step / complete)</td></tr>
            </tbody>
          </table>
        </Card>

        <Card title="Algorithm Categories" className="doc-card">
          <table className="info-table">
            <thead><tr><th>Category</th><th>Algorithms</th></tr></thead>
            <tbody>
              <tr><td>String Search</td><td>Naive, KMP, Z-Algorithm, Rabin-Karp, Aho-Corasick, Suffix Array, Fuzzy Search</td></tr>
              <tr><td>Dynamic Programming</td><td>Levenshtein, Damerau, Weighted Edit, Needleman-Wunsch, Smith-Waterman, Matrix Chain, OBST, Bitmask TSP, Hamiltonian, Tree DP, Rerooting, SOS DP</td></tr>
              <tr><td>Graph &amp; Flow</td><td>Ford-Fulkerson, Edmonds-Karp, Dinic, Min-Cut, Bipartite Matching, Min-Cost Flow</td></tr>
              <tr><td>Approximation</td><td>Vertex Cover 2-Approx, Incident Cover, Greedy Set Cover</td></tr>
              <tr><td>Randomized</td><td>Randomized QuickSort, Miller-Rabin Primality, Reservoir Sampling, Universal Hashing</td></tr>
              <tr><td>Parallel</td><td>Parallel Prefix Sum, Parallel Merge-Sort, Parallel Reduce</td></tr>
            </tbody>
          </table>
        </Card>

        <Card title="Trace / Algorithm Laboratory" className="doc-card">
          <p>
            The Algorithm Laboratory provides step-by-step replay of 13 instrumented algorithms. Each
            trace endpoint runs the real algorithm and records every operation (comparisons, DP cells,
            augmentation paths, partition choices). The frontend replays these genuine execution steps
            — nothing is animated from fabricated data.
          </p>
          <ul>
            <li><strong>String traces:</strong> Naive, KMP, Z, Rabin-Karp — character comparisons, LPS, Z-values</li>
            <li><strong>DP traces:</strong> Levenshtein (matrix), Matrix Chain (split schedule)</li>
            <li><strong>Flow traces:</strong> Ford-Fulkerson, Edmonds-Karp, Dinic — augmenting paths, bottleneck values, residual edges</li>
            <li><strong>Approximation:</strong> Vertex Cover — matched edges, bound tracking</li>
            <li><strong>Randomized:</strong> QuickSort (pivot choices), Miller-Rabin (witnesses), Reservoir (fill/replace decisions)</li>
          </ul>
        </Card>

        <Card title="Architecture" className="doc-card">
          <table className="info-table">
            <tbody>
              <tr><th>Layer</th><td>React 18 + TypeScript + Vite → Spring Boot 3.5 + Java 21</td></tr>
              <tr><th>Dataset</th><td>In-memory LogEvent model, loaded from bundled .jsonl sample files</td></tr>
              <tr><th>API Contract</th><td>REST endpoints per controller, JSON wire format with @JsonInclude(NON_NULL)</td></tr>
              <tr><th>Error Handling</th><td>GlobalExceptionHandler → ErrorResponseDto (no stack traces on wire)</td></tr>
              <tr><th>Testing</th><td>669 tests · JaCoCo 92% instruction coverage · Cross-check (traced == untraced)</td></tr>
              <tr><th>Coverage</th><td>Backend: algorithm packages ≥ 95% · Services ≥ 90% · No fabricated data</td></tr>
            </tbody>
          </table>
        </Card>

        <Card title="Backend Endpoints" className="doc-card card-full">
          <div className="endpoint-grid">
            <div className="endpoint-group">
              <h3>Health &amp; Dataset</h3>
              <code>GET /api/health</code>
              <code>GET /api/health/status</code>
              <code>GET /api/health/dataset</code>
              <code>GET /api/datasets</code>
              <code>POST /api/datasets/{'{name}'}</code>
              <code>DELETE /api/datasets</code>
              <code>GET /api/datasets/current</code>
            </div>
            <div className="endpoint-group">
              <h3>Logs &amp; Analytics</h3>
              <code>GET /api/logs?limit=&amp;offset=</code>
              <code>GET /api/logs/stats</code>
              <code>GET /api/analytics/dependencies</code>
              <code>GET /api/analytics/errors?limit=</code>
              <code>GET /api/analytics/top?dimension=&amp;limit=</code>
              <code>GET /api/analytics/windows?buckets=</code>
            </div>
            <div className="endpoint-group">
              <h3>String Search</h3>
              <code>POST /api/search/{'{naive|kmp|z|rabin-karp}'}</code>
              <code>POST /api/search/multi</code>
              <code>POST /api/string/suffix/build</code>
              <code>POST /api/string/suffix/search</code>
              <code>POST /api/fuzzy/search</code>
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
              <h3>Trace / Laboratory</h3>
              <code>GET /api/trace/catalog</code>
              <code>POST /api/trace/{'{search/dp/flow/approx/random}'}/{'{algorithm}'}</code>
            </div>
            <div className="endpoint-group">
              <h3>Benchmark &amp; Parallel</h3>
              <code>POST /api/benchmark/run</code>
              <code>POST /api/parallel/{'{reduce|scan|sort}'}</code>
            </div>
            <div className="endpoint-group">
              <h3>Catalog &amp; TextHack</h3>
              <code>GET /api/modules</code>
              <code>GET /api/modules/{'{id}'}</code>
              <code>GET /api/algorithms</code>
              <code>GET /api/algorithms/{'{key}'}</code>
              <code>POST /api/text-hack/query</code>
            </div>
            <div className="endpoint-group">
              <h3>Run Sessions &amp; SSE Replay</h3>
              <code>POST /api/runs</code>
              <code>GET /api/runs</code>
              <code>GET /api/runs/{'{id}'}</code>
              <code>GET /api/runs/{'{id}'}/events</code>
              <code>GET /api/runs/{'{id}'}/result</code>
            </div>
          </div>
        </Card>
      </div>
    </div>
  );
}