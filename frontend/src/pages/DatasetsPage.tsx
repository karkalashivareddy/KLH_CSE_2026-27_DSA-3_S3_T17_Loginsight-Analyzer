import { useRef, useState } from 'react';
import { useApi } from '../hooks/useApi';
import { api } from '../api/client';
import type { DatasetResult, DatasetSummary, DatasetName } from '../api/types';
import { Card, Spinner, ErrorBox, EmptyState } from '../components/ui';
import { formatNumber, formatTs } from '../components/format';

/** Dataset management: current state, demo load, bundled samples, file upload and clear. */
export default function DatasetsPage() {
  const { data: current, loading: curLoading, error: curErr, reload: curReload } = useApi<DatasetSummary | null>(async () => {
    try { return await api.currentDataset(); } catch { return null; }
  });
  const { data: names, loading: listLoading, error: listErr, reload: listReload } = useApi<DatasetName[]>(() => api.datasets());
  const [busy, setBusy] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [lastNote, setLastNote] = useState<string | null>(null);
  const fileRef = useRef<HTMLInputElement>(null);

  const run = async (label: string, fn: () => Promise<DatasetResult | unknown>) => {
    setBusy(label);
    setActionError(null);
    setLastNote(null);
    try {
      const result = await fn();
      const note = typeof result === 'object' && result !== null && 'note' in result
        ? String((result as DatasetResult).note) : null;
      setLastNote(note);
      curReload();
      listReload();
    } catch (e) {
      setActionError(e instanceof Error ? e.message : String(e));
    } finally {
      setBusy(null);
    }
  };

  const loadDemo = () => run('Loading demo dataset…', () => api.loadDemo());
  const loadSample = (name: string) => run(`Loading ${name}…`, () => api.loadDataset(name));
  const upload = () => run('Ingesting upload…', async () => {
    const file = fileRef.current?.files?.[0];
    if (!file) throw new Error('Choose a file to upload');
    return api.uploadDataset(file);
  });
  const clear = () => {
    if (!confirm('Clear the currently loaded dataset?')) return;
    run('Clearing…', () => api.clearDataset());
  };

  const loading = curLoading || listLoading || busy !== null;
  const err = curErr ?? listErr ?? (actionError ? new Error(actionError) : null);

  if (loading) return <Spinner label={busy ?? 'Loading datasets…'} />;
  if (err) return <ErrorBox error={err} retry={() => { curReload(); listReload(); }} />;

  const cur = current ?? null;
  const samples = names ?? [];

  return (
    <div className="page">
      <h2 className="page-title">Datasets</h2>

      <Card title="Current Dataset">
        {cur?.loaded ? (
          <div className="dataset-info">
            <span className="badge badge--loaded">Loaded</span>
            <strong>{cur.datasetName}</strong>
            <span>{formatNumber(cur.size ?? 0)} events</span>
            <span>{formatNumber(cur.totalLines ?? 0)} lines · {formatNumber(cur.failedLines ?? 0)} failed</span>
            {cur.loadedAt && <span className="text-muted">loaded {formatTs(cur.loadedAt)}</span>}
            <div className="quick-actions">
              <button className="btn btn-danger btn-sm" onClick={clear}>Clear</button>
            </div>
          </div>
        ) : (
          <div className="dataset-info">
            <span className="badge badge--empty">No dataset loaded</span>
            <span>Load the demo dataset or upload a file below.</span>
          </div>
        )}
        {lastNote && <div className="note-line">{lastNote}</div>}
      </Card>

      <div className="grid-2">
        <Card title="Demo Dataset" sub="deterministic 14,000-event corpus, re-anchored to the current hour">
          <p>
            One click loads the built-in demo corpus — 8 services with correlated error bursts —
            so every screen has honest data to analyse.
          </p>
          <button className="btn btn-run" onClick={loadDemo} disabled={busy !== null}>Load Demo Dataset</button>
        </Card>

        <Card title="Upload Logs" sub="JSONL or canonical text, parser auto-detected">
          <input ref={fileRef} className="input" type="file" accept=".jsonl,.log,.txt" />
          <div className="quick-actions">
            <button className="btn" onClick={upload} disabled={busy !== null}>Ingest file</button>
          </div>
        </Card>
      </div>

      <Card title="Available Samples">
        {samples.length === 0 ? (
          <EmptyState>No bundled sample files found.</EmptyState>
        ) : (
          <div className="sample-grid">
            {samples.map((name) => (
              <button
                key={name}
                className={`sample-btn${cur?.loaded && cur.datasetName === name ? ' sample-btn--active' : ''}`}
                onClick={() => loadSample(name)}
                disabled={busy !== null || (cur?.loaded && cur.datasetName === name)}
              >
                <span className="sample-name">{name}</span>
                {cur?.loaded && cur.datasetName === name && <span className="sample-check">✓</span>}
              </button>
            ))}
          </div>
        )}
      </Card>
    </div>
  );
}