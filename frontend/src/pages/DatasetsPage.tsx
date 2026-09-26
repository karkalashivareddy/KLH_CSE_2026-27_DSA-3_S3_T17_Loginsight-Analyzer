import { useRef, useState } from 'react';
import { CloudUpload, Database, RefreshCw, Trash2 } from 'lucide-react';
import { Link } from 'react-router-dom';
import { useApi } from '../hooks/useApi';
import { api } from '../api/client';
import type { DatasetName, DatasetResult, DatasetSummary } from '../api/types';
import { Badge, Card, EmptyState, ErrorBox, PageHeader, Spinner, StatCard } from '../components/ui';
import { formatNumber, formatTs } from '../components/format';

function isLoaded(value: boolean | string | undefined): boolean {
  return value === true || value === 'true';
}

function count(value: number | undefined): string {
  return value === undefined ? '—' : formatNumber(value);
}

export default function DatasetsPage() {
  const current = useApi<DatasetSummary | null>((signal) => api.currentDataset({ signal }));
  const available = useApi<DatasetName[]>((signal) => api.datasets({ signal }));
  const [busy, setBusy] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [lastNote, setLastNote] = useState<string | null>(null);
  const [fileName, setFileName] = useState<string | null>(null);
  const fileRef = useRef<HTMLInputElement>(null);

  const run = async (label: string, action: () => Promise<DatasetResult | DatasetSummary>) => {
    setBusy(label);
    setActionError(null);
    setLastNote(null);
    try {
      const result = await action();
      if ('note' in result && typeof result.note === 'string') setLastNote(result.note);
      current.reload();
      available.reload();
    } catch (error) {
      setActionError(error instanceof Error ? error.message : String(error));
    } finally {
      setBusy(null);
    }
  };

  const upload = () => {
    const file = fileRef.current?.files?.[0];
    if (!file) {
      setActionError('Choose a file to upload.');
      return;
    }
    void run('Ingesting upload…', () => api.uploadDataset(file));
  };

  const clear = () => {
    if (!window.confirm('Clear the currently loaded dataset?')) return;
    void run('Clearing dataset…', () => api.clearDataset());
  };

  const dataset = current.data;
  const samples = available.data ?? [];
  const loaded = dataset && isLoaded(dataset.loaded);
  const initialLoading = current.loading || available.loading;
  const sourceError = current.error ?? available.error;

  return (
    <div className="page">
      <PageHeader eyebrow="Data" title="Datasets" description="Load a bundled sample or ingest a JSONL/canonical text file. Counts come from the parser response." actions={<button className="btn btn-sm" type="button" onClick={() => { current.reload(); available.reload(); }} disabled={initialLoading || busy !== null}><RefreshCw size={14} aria-hidden="true" /> Refresh</button>} />
      {busy && <Spinner label={busy} />}
      {actionError && <ErrorBox error={new Error(actionError)} />}
      {lastNote && <div className="note-line" role="status">{lastNote}</div>}
      {sourceError && <ErrorBox error={sourceError} retry={() => { current.reload(); available.reload(); }} />}
      {initialLoading ? <Card title="Loading dataset state"><Spinner label="Requesting the current dataset and available sources…" /></Card> : <>
        <Card title="Current dataset" sub="The backend owns the active dataset used by investigation views.">
          {loaded ? <>
            <div className="dataset-info"><Badge tone="good">Loaded</Badge><strong>{dataset.datasetName ?? 'Unnamed dataset'}</strong><span>{count(dataset.size)} events</span><span>{count(dataset.totalLines)} lines · {count(dataset.failedLines)} failed</span>{dataset.loadedAt && <span className="text-muted">loaded {formatTs(dataset.loadedAt)}</span>}<button className="btn btn-danger btn-sm" type="button" onClick={clear} disabled={busy !== null}><Trash2 size={13} aria-hidden="true" /> Clear</button></div>
            <div className="stat-grid stat-grid--small"><StatCard label="Events" value={count(dataset.size)} color="var(--accent)" /><StatCard label="Parsed lines" value={count(dataset.totalLines)} /><StatCard label="Failed lines" value={count(dataset.failedLines)} color={dataset.failedLines && dataset.failedLines > 0 ? 'var(--warn)' : 'var(--ok)'} /><StatCard label="Loaded at" value={dataset.loadedAt ? formatTs(dataset.loadedAt) : '—'} /></div>
            <div className="quick-actions"><Link className="btn btn-sm" to="/">Open command center</Link><Link className="btn btn-sm" to="/logs">Explore events</Link></div>
          </> : <EmptyState><Database size={23} aria-hidden="true" /><strong>No dataset loaded.</strong><span>Choose a bundled sample or upload a file to enable dataset-backed investigation.</span></EmptyState>}
        </Card>
        <div className="grid-2">
          <Card title="Bundled samples" sub="Backend-provided dataset names">
            {available.error ? <ErrorBox error={available.error} retry={available.reload} /> : samples.length === 0 ? <EmptyState>No bundled samples were returned.</EmptyState> : <div className="sample-grid">{samples.map((sample) => <button key={sample} className={`sample-btn${loaded && dataset.datasetName === sample ? ' sample-btn--active' : ''}`} type="button" onClick={() => void run(`Loading ${sample}…`, () => api.loadDataset(sample))} disabled={busy !== null}><span>{sample}</span>{loaded && dataset.datasetName === sample && <span className="sample-check">active</span>}</button>)}</div>}
          </Card>
          <Card title="Demo corpus" sub="Deterministic backend-generated sample">
            <p className="page-description">Load the bundled demo corpus to explore the product surfaces with real backend events. The browser does not generate replacement events.</p>
            <button className="btn btn-run" type="button" onClick={() => void run('Loading demo dataset…', () => api.loadDemo())} disabled={busy !== null}><Database size={14} aria-hidden="true" /> Load demo dataset</button>
          </Card>
        </div>
        <Card title="Upload logs" sub="Multipart request; the browser sets the form boundary">
          <label className="form-label" htmlFor="dataset-file">Log file</label>
          <input id="dataset-file" ref={fileRef} className="input" type="file" accept=".jsonl,.ndjson,.log,.txt,text/plain,application/x-ndjson" onChange={(event) => setFileName(event.target.files?.[0]?.name ?? null)} disabled={busy !== null} />
          <div className="dataset-info"><span>{fileName ?? 'No file selected'}</span><button className="btn btn-run" type="button" onClick={upload} disabled={busy !== null || !fileName}><CloudUpload size={14} aria-hidden="true" /> Upload and load</button></div>
          <ul className="ingest-hints"><li>JSONL and canonical text are parsed by the backend.</li><li>Parser counts and failed lines are returned after ingestion.</li></ul>
        </Card>
      </>}
    </div>
  );
}
