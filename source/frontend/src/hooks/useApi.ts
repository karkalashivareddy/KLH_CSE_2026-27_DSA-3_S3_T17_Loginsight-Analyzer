import { useCallback, useEffect, useRef, useState } from 'react';
import { subscribeDatasetInvalidation } from '../api/client';

export interface UseApiResult<T> {
  data: T | null;
  loading: boolean;
  refreshing: boolean;
  error: Error | null;
  reload: () => void;
  lastUpdated: number | null;
}

export type ApiLoader<T> = (signal: AbortSignal) => Promise<T>;

function asError(value: unknown): Error {
  if (value instanceof Error) return value;
  return new Error(String(value));
}

export function useApi<T>(loader: ApiLoader<T>, key?: string): UseApiResult<T> {
  const [data, setData] = useState<T | null>(null);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState<Error | null>(null);
  const [lastUpdated, setLastUpdated] = useState<number | null>(null);
  const [trigger, setTrigger] = useState(0);
  const loaderRef = useRef(loader);
  const generationRef = useRef(0);
  const dataRef = useRef<T | null>(null);
  const keyRef = useRef(key);
  loaderRef.current = loader;

  const reload = useCallback(() => setTrigger((value) => value + 1), []);

  useEffect(() => subscribeDatasetInvalidation(reload), [reload]);

  useEffect(() => {
    const generation = ++generationRef.current;
    const controller = new AbortController();
    const changedKey = keyRef.current !== key;
    keyRef.current = key;
    if (changedKey) {
      dataRef.current = null;
      setData(null);
      setLastUpdated(null);
    }
    setLoading(true);
    setRefreshing(!changedKey && dataRef.current !== null);
    setError(null);

    Promise.resolve()
      .then(() => loaderRef.current(controller.signal))
      .then((value) => {
        if (generation !== generationRef.current || controller.signal.aborted) return;
        dataRef.current = value;
        setData(value);
        setLastUpdated(Date.now());
      })
      .catch((reason: unknown) => {
        if (generation !== generationRef.current || controller.signal.aborted) return;
        setError(asError(reason));
      })
      .finally(() => {
        if (generation !== generationRef.current || controller.signal.aborted) return;
        setLoading(false);
        setRefreshing(false);
      });

    return () => controller.abort();
  }, [key, trigger]);

  return { data, loading, refreshing, error, reload, lastUpdated };
}
