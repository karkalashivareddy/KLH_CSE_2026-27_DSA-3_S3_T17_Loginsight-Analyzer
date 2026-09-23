import { useMemo } from 'react';
import { Link } from 'react-router-dom';
import { useApi } from '../hooks/useApi';
import { api } from '../api/client';
import type { AlgorithmInfo, ModuleInfo } from '../api/types';
import { Card, StatCard, Spinner, ErrorBox } from '../components/ui';
import { moduleAccent } from '../components/format';

export default function CourseMapPage() {
  const modulesApi = useApi<ModuleInfo[]>(() => api.modules());
  const algosApi = useApi<AlgorithmInfo[]>(() => api.algorithms());

  const { data: modules } = modulesApi;
  const { data: algorithms } = algosApi;

  const counts = useMemo(() => {
    const list = algorithms ?? [];
    const total = list.length;
    const tracked = list.filter((a) => a.tracked).length;
    const exposed = list.filter((a) => a.exposed).length;
    const libraryOnly = list.filter((a) => !a.exposed).length;
    return { total, tracked, exposed, libraryOnly };
  }, [algorithms]);

  if (modulesApi.loading || algosApi.loading) return <Spinner label="Building course map…" />;
  const error = modulesApi.error ?? algosApi.error;
  if (error) return <ErrorBox error={error} retry={() => { modulesApi.reload(); algosApi.reload(); }} />;

  const modulesList = modules ?? [];

  return (
    <div className="page">
      <h2 className="page-title">Course Map</h2>
      <p className="lab-intro">
        The full DSA-3 algorithm catalogue mapped to the six TextHack modules. <strong>trace</strong> = live step
        replay, <strong>lib</strong> = implemented library algorithm (no step recorder), no badge = exposed API only.
      </p>

      <div className="stat-grid">
        <StatCard label="Algorithms" value={counts.total} color="var(--accent)" />
        <StatCard label="Traceable" value={counts.tracked} color="var(--ok)" sub="live step replay" />
        <StatCard label="Exposed APIs" value={counts.exposed} color="#38c172" />
        <StatCard label="Library-only" value={counts.libraryOnly} color="var(--mod-random)" sub="no recorder" />
        <StatCard label="Modules" value={modulesList.length} color="var(--mod-dp)" />
      </div>

      <div className="course-matrix">
        <table>
          <thead>
            <tr>
              <th>Module</th>
              <th>Algorithm</th>
              <th>Problem</th>
              <th>Complexity</th>
              <th>Status</th>
            </tr>
          </thead>
          <tbody>
            {modulesList.map((mod) => {
              const rows = mod.algorithms.length > 0
                ? mod.algorithms
                : (algorithms ?? []).filter((a) => a.moduleId === mod.id);
              return rows.map((entry, i) => (
                <tr
                  key={`${mod.id}-${entry.key}`}
                  style={{ boxShadow: 'none' }}
                >
                  {i === 0 && (
                    <td rowSpan={rows.length} className="matrix-hl" style={{ color: moduleAccent(mod.id), verticalAlign: 'top' }}>
                      {mod.title}
                      <div className="lab-desc" style={{ fontSize: '0.72rem' }}>
                        {mod.trackableCount} traceable · {mod.exposedCount} exposed
                      </div>
                    </td>
                  )}
                  <td>
                    <Link to={`/labs/${entry.key}`}>{entry.name}</Link>
                  </td>
                  <td className="lab-desc" style={{ margin: 0 }}>{entry.problem}</td>
                  <td className="text-muted" style={{ whiteSpace: 'nowrap', fontSize: '0.78rem' }}>{entry.timeComplexity}</td>
                  <td>
                    <span className="run-item-meta" style={{ marginTop: 0 }}>
                      {entry.tracked && <span className="status-dot status-dot--trace" aria-label="traceable" />}
                      {!entry.exposed
                        ? <span className="badge">lib</span>
                        : entry.tracked
                          ? <span className="badge" style={{ color: 'var(--ok)', borderColor: 'rgba(56,193,114,.4)' }}>trace</span>
                          : <span className="badge">api</span>}
                    </span>
                  </td>
                </tr>
              ));
            })}
          </tbody>
        </table>
      </div>

      <div className="chart-grid" style={{ marginTop: '1.2rem' }}>
        <Card title="Module 5 — NP-Completeness reductions" className="doc-card">
          <p>
            Bounded Vertex Cover, Vertex Cover kernelization and the Independent Set reduction are
            implemented as library algorithms:
          </p>
          <ul>
            <li><strong>Vertex Cover ⇄ Independent Set</strong> — VC(G, k) ≡ IS(Ḡ, |V| − k) on the complement graph.</li>
            <li><strong>Kernelization</strong> — high-degree vertices are forced into the cover; low-degree leaves are resolved locally.</li>
            <li><strong>FPTAS</strong> — Knapsack under an ε-precision schedule (O(n³/ε), not a heuristic).</li>
          </ul>
        </Card>
        <Card title="Module 6 — Parallel algorithms" className="doc-card">
          <p>Parallel prefix sum, merge-sort and reduce use Java's fork/join-style parallel streams on real inputs:</p>
          <ul>
            <li><strong>Speedup</strong> — measured sequential vs parallel wall time in <strong>Benchmarks</strong>.</li>
            <li><strong>Work / Span</strong> — per-algorithm recorded in the benchmark table, never estimated on the fly.</li>
          </ul>
        </Card>
      </div>
    </div>
  );
}