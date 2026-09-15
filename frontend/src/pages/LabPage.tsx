import { EmptyState } from '../components/ui';

export default function LabPage() {
  return (
    <div className="page">
      <h2 className="page-title">Algorithm Laboratory</h2>
      <EmptyState>
        The Algorithm Laboratory (Phase 14) will let you replay real algorithm traces step by step.
        It is not available in this build yet.
      </EmptyState>
    </div>
  );
}