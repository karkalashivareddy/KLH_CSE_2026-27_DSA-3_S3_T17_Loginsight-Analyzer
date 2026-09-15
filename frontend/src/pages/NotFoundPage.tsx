import { Link } from 'react-router-dom';
import { EmptyState } from '../components/ui';

export default function NotFoundPage() {
  return (
    <div className="page">
      <EmptyState>
        <strong>404 — page not found.</strong>
        <Link className="btn" to="/">Back to Overview</Link>
      </EmptyState>
    </div>
  );
}