import { useRef, useState } from 'react';
import { CheckCircle2, CloudUpload, FileUp, RefreshCw, ServerCog } from 'lucide-react';
import { Link } from 'react-router-dom';
import { useApi } from '../hooks/useApi';
import { api } from '../api/client';
import type { IngestionStatus } from '../api/types';
import { Badge, Card, EmptyState, ErrorBox, PageHeader, Spinner, StatCard } from '../components/ui';
import { formatNumber, formatTs } from '../components/format';

function count(value: number | undefined): string {
  return value === undefined ? '—' : formatNumber(value);
}

export default function IngestionPage() {
  const status = useApi<IngestionStatus>((signal) => api.ingestionStatus({ signal }));
  const [busy, setBusy] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [note, setNote] = useState<string | null>(null);
  const [fileName, setFileName] = useState<string | null>(null);
  const fileRef = useRef<HTMLInputElement>(null);

  const run = async (label: string, action: () => Promise<unknown>) => {
    setBusy(label);
    setActionError(null);
    setNote(null);
    try {
      const result = await action();
      if (result && typeof result === 'object' && 'note' in result && typeof (result as { note?: unknown }).note === 'string') setNote((result as { note: string }).note);
      status.reload();
    } catch (error) {
      setActionError(error instanceof Error ? error.message : String(error));
    } finally {
      setBusy(null);
    }
  };

  const upload = () => {
    const file = fileRef.current?.files?.[0];
    if (!file) {
      setActionError('Choose a log file first.');
      return;
    }
    void run('Ingesting file…', () => api.uploadDataset(file));
  };

  if (status.loading) return <div className="page"><PageHeader eyebrow="Data" title="Ingestion" description="Inspect parser capabilities and ingest a file through the backend multipart endpoint." /><Card title="Loading ingestion status"><Spinner label="Requesting parser and dataset state…" /></Card></div>;
  if (status.error) return <div className="page"><PageHeader eyebrow="Data" title="Ingestion" description="Inspect parser capabilities and ingest a file through the backend multipart endpoint." /><ErrorBox error={status.error} retry={status.reload} /></div>;
  if (!status.data) return <div className="page"><PageHeader eyebrow="Data" title="Ingestion" description="Inspect parser capabilities and ingest a file through the backend multipart endpoint." /><EmptyState><ServerCog size={23} aria-hidden="true" /><strong>Ingestion status is unavailable.</strong><span>The backend did not return a status payload.</span></EmptyState></div>;

  const data = status.data;
  const dataset = data.dataset;
  const loaded = dataset.loaded;

  return (
    <div className="page">
      <PageHeader eyebrow="Data" title="Ingestion" description="Inspect parser capabilities and ingest a file through the backend multipart endpoint." actions={<button className="btn btn-sm" type="button" onClick={status.reload} disabled={status.refreshing || busy !== null}><RefreshCw size={14} aria-hidden="true" /> Refresh status</button>} />
      {busy && <Spinner label={busy} />}
      {actionError && <ErrorBox error={new Error(actionError)} />}
      {note && <div className="note-line" role="status">{note}</div>}
      <Card title="Dataset status" sub="Current state reported by the ingestion endpoint.">
        {loaded ? <><div className="dataset-info"><Badge tone="good"><CheckCircle2 size={12} aria-hidden="true" /> Loaded</Badge><strong>{dataset.datasetName ?? 'Unnamed dataset'}</strong><span>{count(dataset.size)} events</span><span>{count(dataset.totalLines)} lines · {count(dataset.failedLines)} failed</span>{dataset.loadedAt && <span className="text-muted">ingested {formatTs(dataset.loadedAt)}</span>}</div><div className="stat-grid stat-grid--small"><StatCard label="Events" value={count(dataset.size)} color="var(--accent)" /><StatCard label="Parsed lines" value={count(dataset.totalLines)} /><StatCard label="Failed lines" value={count(dataset.failedLines)} color={dataset.failedLines && dataset.failedLines > 0 ? 'var(--warn)' : 'var(--ok)'} /><StatCard label="Loaded at" value={dataset.loadedAt ? formatTs(dataset.loadedAt) : '—'} /></div></> : <EmptyState><ServerCog size={22} aria-hidden="true" /><strong>No dataset loaded.</strong><span>Load a source to enable the ingestion pipeline and investigation views.</span></EmptyState>}
      </Card>
      <div className="grid-2">
        <Card title="Parser" sub="Capabilities reported by the backend">
          <div className="table-scroll"><table className="info-table"><caption className="sr-only">Parser capabilities</caption><tbody><tr><th>Parser</th><td>{data.parser}</td></tr><tr><th>Streaming</th><td>{data.streaming}</td></tr><tr><th>Bundled samples</th><td>{formatNumber(data.sampleCount)} file{data.sampleCount === 1 ? '' : 's'}</td></tr></tbody></table></div>
          <div className="quick-actions"><button className="btn btn-run" type="button" onClick={() => void run('Loading demo dataset…', () => api.ingestionDemo())} disabled={busy !== null}><FileUp size={14} aria-hidden="true" /> Load demo corpus</button><Link className="btn" to="/datasets">Manage datasets</Link></div>
        </Card>
        <Card title="Available samples" sub="Load a named sample through the dataset API.">
          {data.availableSamples.length === 0 ? <EmptyState>No bundled samples were returned.</EmptyState> : <div className="sample-grid">{data.availableSamples.map((sample) => <button key={sample} className="sample-btn" type="button" onClick={() => void run(`Loading ${sample}…`, () => api.loadDataset(sample))} disabled={busy !== null}><span>{sample}</span></button>)}</div>}
        </Card>
      </div>
      <Card title="Upload a log file" sub="JSONL or canonical text; parser auto-detected">
        <label className="form-label" htmlFor="ingestion-file">Log file</label>
        <input id="ingestion-file" ref={fileRef} className="input" type="file" accept=".jsonl,.ndjson,.log,.txt,text/plain,application/x-ndjson" onChange={(event) => setFileName(event.target.files?.[0]?.name ?? null)} disabled={busy !== null} />
        <div className="dataset-info"><span>{fileName ?? 'No file selected'}</span><button className="btn btn-run" type="button" onClick={upload} disabled={busy !== null || !fileName}><CloudUpload size={14} aria-hidden="true" /> Upload and ingest</button></div>
        <ul className="ingest-hints"><li>The multipart request is sent to the real dataset endpoint.</li><li>Returned line and failure counts are displayed after parsing.</li></ul>
      </Card>
    </div>
  );
}
