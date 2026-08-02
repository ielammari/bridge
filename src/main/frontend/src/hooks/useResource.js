import { useCallback, useEffect, useRef, useState } from 'react';

/**
 * One asynchronous load, with a retry. The loader is read through a ref so an
 * inline function does not restart the request on every render; `keys` decides
 * when to reload.
 */
export default function useResource(loader, keys = []) {
  const [status, setStatus] = useState('loading');
  const [data, setData] = useState(null);
  const [attempt, setAttempt] = useState(0);

  const latest = useRef(loader);
  latest.current = loader;

  useEffect(() => {
    let cancelled = false;
    setStatus('loading');
    latest.current()
      .then((result) => {
        if (cancelled) return;
        setData(result);
        setStatus('ready');
      })
      .catch(() => {
        if (!cancelled) setStatus('error');
      });
    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [...keys, attempt]);

  const reload = useCallback(() => setAttempt((n) => n + 1), []);

  return { status, data, setData, reload };
}
