import { ArrowRight, Braces, GitBranch, History, Search, Share2, Timer, Workflow } from 'lucide-react';
import { Link } from 'react-router-dom';
import { api } from '../api/client';
import type { AlgorithmGroup } from '../api/types';
import { useApi } from '../hooks/useApi';
import { Card, EmptyState, ErrorBox, PageHeader, Spinner } from '../components/ui';
import { formatNumber } from '../components/format';

const ICONS = [Workflow, Braces, GitBranch, Share2];

export default function AnalysisPage() {
  const catalogue = useApi<AlgorithmGroup[]>((signal) => api.algorithmGroups({ signal }));
  const groups = catalogue.data ?? [];
  const algorithms = groups.flatMap((group) => group.algorithms);
  const traceable = algorithms.filter((algorithm) => algorithm.tracked).length;
  const exposed = algorithms.filter((algorithm) => Boolean(algorithm.canonicalEndpoint)).length;

  return (
    <div className="page algorithm-lab-page">
      <PageHeader eyebrow="ALGORITHM LAB / DSA-3" title="Algorithm Lab" description="Explore the registered algorithms, run real backend executions, and follow their operation traces." actions={<><Link className="btn btn-sm" to="/benchmarks"><Timer size={14} aria-hidden="true" /> Benchmarks</Link><Link className="btn btn-sm btn-primary" to="/algorithms">Open catalogue <ArrowRight size={14} aria-hidden="true" /></Link></>} />

      {catalogue.loading ? <Card title="Loading algorithm modules"><Spinner label="Requesting the backend catalogue" /></Card> : catalogue.error ? <ErrorBox error={catalogue.error} retry={catalogue.reload} /> : !catalogue.data ? <Card title="Algorithm catalogue"><EmptyState>The backend did not return an algorithm catalogue.</EmptyState></Card> : <>
        <section className="lab-overview-strip" aria-label="Algorithm catalogue coverage">
          <div><span>MODULES</span><strong>{formatNumber(groups.length)}</strong></div>
          <div><span>REGISTERED ALGORITHMS</span><strong>{formatNumber(algorithms.length)}</strong></div>
          <div><span>TRACEABLE</span><strong>{formatNumber(traceable)}</strong></div>
          <div><span>API EXPOSED</span><strong>{formatNumber(exposed)}</strong></div>
          <p>Counts come from the Spring Boot algorithm catalogue and its exposure metadata.</p>
        </section>

        <section className="lab-workspace-intro">
          <div className="lab-orbit" aria-hidden="true"><span /><span /><span /><Workflow size={28} /></div>
          <div><span className="eyebrow">FROM INPUT TO EVIDENCE</span><h2>See the computation behind the signal.</h2><p>Search uses string matching. Similarity uses edit distance. Graph and optimization algorithms remain available for direct runs and trace inspection.</p></div>
          <Link to="/search"><Search size={15} aria-hidden="true" /> Open algorithmic search</Link>
        </section>

        <div className="lab-module-heading"><div><span className="eyebrow">REGISTERED MODULES</span><h2>Choose a field of study</h2></div><span>{formatNumber(groups.length)} modules returned</span></div>
        <div className="lab-module-grid" aria-label="Algorithm modules">
          {groups.map((group, index) => {
            const Icon = ICONS[index % ICONS.length];
            const groupTraceable = group.algorithms.filter((algorithm) => algorithm.tracked).length;
            const groupExposed = group.algorithms.filter((algorithm) => Boolean(algorithm.canonicalEndpoint)).length;
            return <Link key={group.module} className={`lab-module-card lab-module-card--${index % 4}`} to={`/algorithms?module=${encodeURIComponent(group.module)}`}>
              <span className="lab-module-icon"><Icon size={19} aria-hidden="true" /></span>
              <span className="lab-module-index">{String(index + 1).padStart(2, '0')}</span>
              <strong>{group.module}</strong>
              <span className="lab-module-count">{formatNumber(group.algorithms.length)} registered algorithms</span>
              <span className="lab-module-meta"><span>{formatNumber(groupTraceable)} traces</span><i aria-hidden="true" /><span>{formatNumber(groupExposed)} API endpoints</span></span>
              <span className="lab-module-link">Explore module <ArrowRight size={14} aria-hidden="true" /></span>
            </Link>;
          })}
        </div>

        <section className="lab-next-steps" aria-label="More algorithm tools">
          <Link to="/algorithms"><Workflow size={17} aria-hidden="true" /><span><strong>Algorithm catalogue</strong><small>Complexity, input contract, endpoint and trace availability.</small></span><ArrowRight size={15} aria-hidden="true" /></Link>
          <Link to="/benchmarks"><Timer size={17} aria-hidden="true" /><span><strong>Search benchmarks</strong><small>Compare real matcher executions on the same dataset query.</small></span><ArrowRight size={15} aria-hidden="true" /></Link>
          <Link to="/runs"><History size={17} aria-hidden="true" /><span><strong>Run sessions</strong><small>Inspect recorded executions and operation traces.</small></span><ArrowRight size={15} aria-hidden="true" /></Link>
        </section>
      </>}
    </div>
  );
}
