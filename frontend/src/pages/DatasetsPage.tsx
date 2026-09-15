import { useCallback } from 'react';
import { useApi } from '../hooks/useApi';
import { api } from '../api/client';
import type { DatasetName, DatasetSummary } from '../api/types';
import { Card, Spinner, ErrorBox, EmptyState } from '../components/ui';
import { formatNumber, formatTs } from '../components/format';

export default function DatasetsPage() {
  const { data: available, loading: listLoading, error: listErr, reload: listReload } =
    useApi<DatasetName[]>(() => api.datasets());

  const { data: current, loading: curLoading, error: curErr, reload: curReload } =
    useApi<DatasetSummary | null>(async () => {
      try { return await api.currentDataset(); } catch { return null; }
    });

  const load = useCallback(async (name: string) => {
    try {
      await api.loadDataset(name);
      curReload();
      listReload();
    } catch (e) {
      alert((e instanceof Error ? e : new Error(String(e))).message);
    }
  }, [curReload, listReload]);

  const clear = useCallback(async () => {
    if (!confirm('Clear the currently loaded dataset?')) return;
    try {
      await api.clearDataset();
      curReload();
      listReload();
    } catch (e) {
      alert((e instanceof Error ? e : new Error(String(e))).message);
    }
  }, [curReload, listReload]);

  const loading = listLoading || curLoading;
  const err     = listErr ?? curErr;

  if (loading) return <Spinner label="Loading datasets…" />;
  if (err)     return <ErrorBox error={err} retry={() => { listReload(); curReload(); }} />;

  const names = available ?? [];
  const cur   = current ?? null;

  return (
    <div className="page">
      <h2 className="page-title">Datasets</h2>

      {/* Current dataset status */}
      <Card title="Current Dataset" className="dataset-status">
        {cur?.loaded ? (
          <div className="dataset-info">
            <span className="badge badge--loaded">Loaded</span>
            <strong>{cur.datasetName}</strong>
            <span>{formatNumber(cur.size ?? 0)} events</span>
            <span>{formatNumber(cur.totalLines ?? 0)} lines, {formatNumber(cur.failedLines ?? 0)} failed</span>
            {cur.loadedAt && <span className="text-muted">Loaded at {formatTs(cur.loadedAt)}</span>}
            <button className="btn btn-danger btn-sm" onClick={clear}>Clear</button>
          </div>
        ) : (
          <div className="dataset-info">
            <span className="badge badge--empty">No dataset loaded</span>
            <span>Select one from the available samples below.</span>
          </div>
        )}
      </Card>

      {/* Available samples */}
      <Card title="Available Samples" className="dataset-list">
        {names.length === 0 ? (
          <EmptyState>No sample files found on the server.</EmptyState>
        ) : (
          <div className="sample-grid">
            {names.map((name) => (
              <button
                key={name}
                className={`sample-btn${cur?.loaded && cur.datasetName === name ? ' sample-btn--active' : ''}`}
                onClick={() => load(name)}
                disabled={cur?.loaded && cur.datasetName === name}
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