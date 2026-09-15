import { useCallback, useEffect, useRef, useState } from 'react';

/**
 * Minimal data-fetching hook shared by every page. Exposes loading / error / data plus a
 * manual `reload`. Errors are normalised into the documented ApiError envelope when available.
 */
export interface UseApiResult<T> {
  data: T | null;
  loading: boolean;
  error: Error | null;
  reload: () => void;
}

/**
 * Call with a `loader` and an optional `key` (any serialisable value). When the key changes or
 * `reload()` is invoked the request fires automatically.
 */
export function useApi<T>(loader: () => Promise<T>, key?: string): UseApiResult<T> {
  const [data, setData] = useState<T | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<Error | null>(null);
  const loaderRef = useRef(loader);
  loaderRef.current = loader;

  const [trigger, setTrigger] = useState(0);
  const reload = useCallback(() => setTrigger((t) => t + 1), []);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);
    loaderRef.current()
      .then((value) => {
        if (!cancelled) setData(value);
      })
      .catch((reason: unknown) => {
        if (!cancelled) setError(reason instanceof Error ? reason : new Error(String(reason)));
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => { cancelled = true; };
  }, [trigger, key]);

  return { data, loading, error, reload };
}