import { useCallback, useState } from 'react';
import { useApi } from '../hooks/useApi';
import { api } from '../api/client';
import type { LogEvent } from '../api/types';
import { Card, Spinner, ErrorBox, LevelBadge, EmptyState } from '../components/ui';
import { formatTs } from '../components/format';

const PAGE_SIZE = 50;

export default function LogsPage() {
  const [offset, setOffset] = useState(0);

  const loadLogs = useCallback(() => api.logs(PAGE_SIZE, offset), [offset]);
  const { data, loading, error, reload } = useApi<LogEvent[]>(loadLogs, String(offset));

  const prev = () => setOffset((o) => Math.max(0, o - PAGE_SIZE));
  const next = () => setOffset((o) => o + PAGE_SIZE);

  if (loading && !data) return <Spinner label="Loading logs…" />;
  if (error) return <ErrorBox error={error} retry={reload} />;

  const rows = data ?? [];
  const hasMore = rows.length === PAGE_SIZE;

  return (
    <div className="page">
      <div className="page-header">
        <h2 className="page-title">Log Explorer</h2>
        <div className="pagination">
          <button className="btn btn-sm" onClick={prev} disabled={offset === 0}>← Prev</button>
          <span className="page-indicator">{offset + 1}–{offset + rows.length}</span>
          <button className="btn btn-sm" onClick={next} disabled={!hasMore}>Next →</button>
        </div>
      </div>

      {rows.length === 0 ? (
        <EmptyState>No log events in the current window.</EmptyState>
      ) : (
        <Card className="table-card">
          <div className="table-scroll">
            <table className="log-table">
              <thead>
                <tr>
                  <th>#</th>
                  <th>Time</th>
                  <th>Level</th>
                  <th>Service</th>
                  <th>Endpoint</th>
                  <th>Code</th>
                  <th>Resp (ms)</th>
                  <th>Message</th>
                </tr>
              </thead>
              <tbody>
                {rows.map((row) => (
                  <tr key={row.id}>
                    <td className="num">{row.id}</td>
                    <td className="ts">{formatTs(row.timestamp)}</td>
                    <td><LevelBadge level={row.level} /></td>
                    <td>{row.service}</td>
                    <td className="mono">{row.endpoint}</td>
                    <td className="num">{row.statusCode}</td>
                    <td className="num">{row.responseTime}</td>
                    <td className="msg">{row.message.length > 80 ? row.message.slice(0, 80) + '…' : row.message}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Card>
      )}
    </div>
  );
}