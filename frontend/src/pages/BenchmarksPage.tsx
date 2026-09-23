import { useCallback, useState } from 'react';
import { api } from '../api/client';
import type { BenchmarkRequest, BenchmarkRow } from '../api/types';
import { Card, Spinner, ErrorBox, EmptyState, BarChart } from '../components/ui';
import { formatNanos, formatNumber } from '../components/format';

type FormState = {
  scenario: string;
  sizes: string;
  repetitions: string;
  parallelism: string;
};

const DEFAULTS: FormState = {
  scenario: 'dataset',
  sizes: '1000,10000,100000',
  repetitions: '3',
  parallelism: '4'
};

export default function BenchmarksPage() {
  const [form, setForm] = useState<FormState>(DEFAULTS);
  const [rows, setRows] = useState<BenchmarkRow[] | null>(null);
  const [running, setRunning] = useState(false);
  const [error, setError] = useState<Error | null>(null);

  const run = useCallback(async () => {
    const sizes = form.sizes.split(/[,;\s]+/).map(Number).filter((n) => n > 0);
    const reps  = parseInt(form.repetitions, 10) || 3;
    const para  = parseInt(form.parallelism, 10) || 4;
    const payload: BenchmarkRequest = { scenario: form.scenario, sizes, repetitions: reps, parallelism: para };
    setRunning(true);
    setError(null);
    setRows(null);
    try {
      const result = await api.benchmark(payload);
      setRows(result);
    } catch (e) {
      setError(e instanceof Error ? e : new Error(String(e)));
    } finally {
      setRunning(false);
    }
  }, [form]);

  const update = (field: keyof FormState, value: string) =>
    setForm((f) => ({ ...f, [field]: value }));

  // Derive chart data: group by algorithm, then each size as a bar
  const chartData = rows ? deriveChart(rows) : null;

  return (
    <div className="page">
      <h2 className="page-title">Benchmarks</h2>
      <p className="lab-intro">
        Sequential and parallel algorithms are executed for real and wall-clock timed. Ratios are
        sequential ÷ parallel time over identical inputs; speedups are measured on this machine
        (JVM 21 · {navigator.hardwareConcurrency ?? '?'} logical cores) and vary with host and load.
      </p>

      <Card title="Configuration" className="bench-form-card">
        <div className="bench-form">
          <label className="form-field">
            <span className="form-label">Scenario</span>
            <select className="select-sm form-input" value={form.scenario} onChange={(e) => update('scenario', e.target.value)}>
              <option value="dataset">Dataset</option>
              <option value="synthetic">Synthetic</option>
              <option value="worst">Worst-case</option>
            </select>
          </label>
          <label className="form-field">
            <span className="form-label">Input sizes (comma-separated)</span>
            <input className="form-input" value={form.sizes} onChange={(e) => update('sizes', e.target.value)} />
          </label>
          <label className="form-field">
            <span className="form-label">Repetitions</span>
            <input className="form-input" type="number" min={1} max={20} value={form.repetitions} onChange={(e) => update('repetitions', e.target.value)} />
          </label>
          <label className="form-field">
            <span className="form-label">Parallelism (threads)</span>
            <input className="form-input" type="number" min={1} max={64} value={form.parallelism} onChange={(e) => update('parallelism', e.target.value)} />
          </label>
          <button className="btn btn-run" onClick={run} disabled={running} style={{ alignSelf: 'flex-end' }}>
            {running ? 'Running…' : 'Run Benchmarks'}
          </button>
        </div>
      </Card>

      {error && <ErrorBox error={error} />}

      {running && <Spinner label="Running benchmark sweep…" />}

      {chartData && chartData.length > 0 && (
        <div className="bench-charts">
          {chartData.map((group) => (
            <Card key={group.algorithm} title={`${group.algorithm} — Time by Input Size`}>
              <BarChart
                data={group.rows.map((r) => ({
                  label: formatNumber(r.inputSize),
                  value: r.executionTimeNanos
                }))}
                height={150}
                label={`${group.rows.length} sizes`}
              />
            </Card>
          ))}
        </div>
      )}

      {rows && rows.some((r) => r.speedup != null) && (
        <div className="bench-charts">
          <Card title="Parallel Speedup — Sequential ÷ Parallel wall time" className="card-wide">
            <div className="chart-grid">
              {deriveChart(rows).filter((g) => g.rows.some((r) => r.speedup != null)).map((group) => (
                <Card key={group.algorithm} title={group.algorithm}>
                  <BarChart
                    data={group.rows.map((r) => ({
                      label: formatNumber(r.inputSize),
                      value: r.speedup ?? 0
                    }))}
                    height={140}
                    label="speedup × (1 = no gain, >1 = faster)"
                  />
                </Card>
              ))}
            </div>
          </Card>
        </div>
      )}

      {rows && rows.length > 0 && (
        <Card title="Results" className="table-card">
          <div className="table-scroll">
            <table className="log-table">
              <thead>
                <tr>
                  <th>Algorithm</th>
                  <th>Input Size</th>
                  <th>Time</th>
                  <th>Throughput</th>
                  <th>Result Size</th>
                  <th>Sequential</th>
                  <th>Parallel</th>
                  <th>Speedup</th>
                  <th>Work / Span</th>
                  <th>Parallelism</th>
                </tr>
              </thead>
              <tbody>
                {rows.map((row, i) => (
                  <tr key={i}>
                    <td>{row.algorithm}</td>
                    <td className="num">{formatNumber(row.inputSize)}</td>
                    <td className="num">{formatNanos(row.executionTimeNanos)}</td>
                    <td className="num">{formatNumber(Math.round(row.throughputPerSec))}/s</td>
                    <td className="num">{formatNumber(row.resultSize)}</td>
                    <td className="num">{row.sequentialNanos != null ? formatNanos(row.sequentialNanos) : '—'}</td>
                    <td className="num">{row.parallelNanos   != null ? formatNanos(row.parallelNanos)   : '—'}</td>
                    <td className="num">{row.speedup         != null ? `${row.speedup.toFixed(2)}×`     : '—'}</td>
                    <td className="num">{row.work != null && row.span != null ? `${formatNumber(row.work)} / ${formatNumber(row.span)}` : '—'}</td>
                    <td className="num">{row.parallelism != null ? row.parallelism : '—'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Card>
      )}

      {rows && rows.length === 0 && !running && (
        <EmptyState>The benchmark returned no rows for the selected configuration.</EmptyState>
      )}

      {!rows && !running && !error && (
        <EmptyState>
          Configure a sweep above and click <strong>Run Benchmarks</strong> to measure real sequential vs. parallel execution times.
        </EmptyState>
      )}
    </div>
  );
}

interface ChartGroup {
  algorithm: string;
  rows: BenchmarkRow[];
}

function deriveChart(rows: BenchmarkRow[]): ChartGroup[] {
  const map = new Map<string, BenchmarkRow[]>();
  for (const row of rows) {
    const list = map.get(row.algorithm) ?? [];
    list.push(row);
    map.set(row.algorithm, list);
  }
  return Array.from(map.entries()).map(([algorithm, group]) => ({
    algorithm,
    rows: group.sort((a, b) => a.inputSize - b.inputSize)
  }));
}