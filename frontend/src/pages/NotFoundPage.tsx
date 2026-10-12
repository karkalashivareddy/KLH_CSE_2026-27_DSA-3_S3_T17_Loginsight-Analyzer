import { Link } from 'react-router-dom';
import { ArrowLeft, SearchX } from 'lucide-react';
import { EmptyState, PageHeader } from '../components/ui';

export default function NotFoundPage() {
  return <div className="page"><PageHeader eyebrow="Reference" title="Page not found" description="The requested workspace route does not exist." /><EmptyState><SearchX size={24} aria-hidden="true" /><strong>404</strong><span>Check the address or return to the command center.</span><Link className="btn btn-sm" to="/"><ArrowLeft size={14} aria-hidden="true" /> Command center</Link></EmptyState></div>;
}
