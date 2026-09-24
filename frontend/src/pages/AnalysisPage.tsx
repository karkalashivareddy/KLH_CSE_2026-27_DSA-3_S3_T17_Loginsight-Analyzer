import { Link } from 'react-router-dom';
import { Card } from '../components/ui';

/** Analysis hub: the DSA engines behind the product, and their measured benchmark. */
export default function AnalysisPage() {
  return (
    <div className="page">
      <h2 className="page-title">Analysis</h2>

      <div className="analysis-grid">
        <Link className="analysis-card" to="/analysis/algorithms">
          <h3>▤ Algorithm Catalogue</h3>
          <p>
            Every exposed algorithm behind LogInsight — string search matchers, dynamic
            programming, graph &amp; flow, approximation, randomized and parallel engines, with
            their canonical endpoints and complexity bounds.
          </p>
          <span className="btn-link">Browse algorithms ›</span>
        </Link>
        <Link className="analysis-card" to="/analysis/benchmarks">
          <h3>⏱ Search Benchmarks</h3>
          <p>
            The four single-pattern matchers run over the exact same dataset haystack with your
            pattern. Every time is measured on that run — the winner is simply the fastest measured.
          </p>
          <span className="btn-link">Run a benchmark ›</span>
        </Link>
        <Link className="analysis-card" to="/runs">
          <h3>↻ Run Sessions</h3>
          <p>
            Recorded algorithm executions with step-by-step SSE replay, kept alongside the product
            screens as evidence of the engine traces behind the analysis.
          </p>
          <span className="btn-link">Open run sessions ›</span>
        </Link>
      </div>

      <Card title="Relationship to the product">
        <p>
          LogInsight's search and analytics are powered by the DSA-3 engines: product search
          executes the user pattern with KMP/Naive/Rabin-Karp/Z over the rendered dataset haystack,
          fuzzy "did you mean" uses Levenshtein edit distance, patterns are discovered by the
          heuristic token normaliser, and incidents are detected with a windowed threshold method.
          The methodology of every operation is surfaced in the UI — nothing is presented as ML.
        </p>
      </Card>
    </div>
  );
}