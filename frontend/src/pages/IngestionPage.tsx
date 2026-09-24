import { useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { useApi } from '../hooks/useApi';
import { api } from '../api/client';
import type { IngestionStatus } from '../api/types';
import { Card, Spinner, ErrorBox, EmptyState } from '../components/ui';
import { formatNumber, formatTs } from '../components/format';

/** Ingestion (docs/API.md §4): parser/streaming status, demo load and file upload. */
export default function IngestionPage() {
  const { data, loading, error, reload } = useApi<IngestionStatus>(() => api.ingestionStatus());
  const [busy, setBusy] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [note, setNote] = useState<string | null>(null);
  const fileRef = useRef<HTMLInputElement>(null);

  if (loading) return <Spinner label="Loading ingestion status…" />;
  if (error) return <ErrorBox error={error} retry={reload} />;
  if (!data) return <EmptyState>Backend unreachable.</EmptyState>;

  const ds = data.dataset;

  const run = async (label: string, fn: () => Promise<unknown>) => {
    setBusy(label);
    setActionError(null);
    setNote(null);
    try {
      const result = await fn();
      const raw = result as { note?: string };
      setNote(typeof raw?.note === 'string' ? raw.note : null);
      reload();
    } catch (e) {
      setActionError(e instanceof Error ? e.message : String(e));
    } finally {
      setBusy(null);
    }
  };

  const upload = () => run('Ingesting…', async () => {
    const file = fileRef.current?.files?.[0];
    if (!file) throw new Error('Choose a file first');
    return api.uploadDataset(file);
  });

  return (
    <div className="page">
      <h2 className="page-title">Ingestion</h2>

      {busy && <Spinner label={busy} />}
      {actionError && <div className="error-box"><div className="error-msg">{actionError}</div></div>}
      {note && <div className="note-line">{note}</div>}

      <Card title="Dataset Status">
        <div className="dataset-info">
          {ds.loaded ? (
            <>
              <span className="badge badge--loaded">Loaded</span>
              <strong>{ds.datasetName}</strong>
              <span>{formatNumber(ds.size ?? 0)} events</span>
              <span>{formatNumber(ds.totalLines ?? 0)} lines · {formatNumber(ds.failedLines ?? 0)} failed</span>
              {ds.loadedAt && <span className="text-muted">ingested {formatTs(ds.loadedAt)}</span>}
            </>
          ) : (
            <>
              <span className="badge badge--empty">No dataset loaded</span>
              <span>Load the demo dataset to get started.</span>
            </>
          )}
        </div>
      </Card>

      <div className="grid-2">
        <Card title="Parser" sub="what the ingest pipeline does">
          <table className="info-table">
            <tbody>
              <tr><th>Parser</th><td>{data.parser}</td></tr>
              <tr><th>Streaming</th><td>{data.streaming}</td></tr>
              <tr><th>Bundled samples</th><td>{data.sampleCount} file{data.sampleCount === 1 ? '' : 's'}</td></tr>
            </tbody>
          </table>
          <div className="quick-actions" style={{ marginTop: '0.7rem' }}>
            <button className="btn btn-run" onClick={() => run('Loading demo dataset…', () => api.ingestionDemo())} disabled={busy !== null}>
              Load Demo Dataset
            </button>
            <Link className="btn" to="/datasets">Manage datasets ›</Link>
          </div>
        </Card>

        <Card title="Upload a Log File" sub="JSONL or canonical text — auto-detected">
          <input ref={fileRef} className="input" type="file" accept=".jsonl,.log,.txt" />
          <div className="quick-actions">
            <button className="btn" onClick={upload} disabled={busy !== null}>Ingest file</button>
          </div>
          <ul className="ingest-hints">
            <li>Lines without a parseable timestamp are recorded as failed, not dropped silently.</li>
            <li>The summary reflects the real parse outcome — counts are never guessed.</li>
          </ul>
        </Card>
      </div>
    </div>
  );
}