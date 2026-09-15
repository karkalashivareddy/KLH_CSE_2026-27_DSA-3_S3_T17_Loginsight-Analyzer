import { EmptyState } from '../components/ui';

export default function BenchmarksPage() {
  return (
    <div className="page">
      <h2 className="page-title">Benchmarks</h2>
      <EmptyState>
        The benchmark workspace (Phase 15) will run measured sequential-vs-parallel sweeps with the
        real engine. It is not available in this build yet.
      </EmptyState>
    </div>
  );
}