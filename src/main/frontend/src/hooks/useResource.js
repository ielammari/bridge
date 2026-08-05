import { useCallback, useEffect, useRef, useState } from 'react';

// A load inside this window shows no placeholder: the blank is too short to
// read, and a placeholder that comes and goes within it reads as a fault.
const SHOW_AFTER = 300;
// Once shown, a placeholder stays this long, so a load landing just past
// SHOW_AFTER cannot leave a single stray frame of it.
const MIN_VISIBLE = 400;
// Kept in step with the exit duration in Skeleton.css.
const LEAVE = 120;

/**
 * One asynchronous load, with a retry. The loader is read through a ref so an
 * inline function does not restart the request on every render; `keys` decides
 * when to reload.
 *
 * `pending` asks whether to render a placeholder, which is not the same as
 * whether the request is outstanding: it is false through a fast load and true
 * while the placeholder leaves. `status` turns to 'ready' only once that exit
 * has finished, so a page never holds both at once.
 */
export default function useResource(loader, keys = []) {
  const [status, setStatus] = useState('loading');
  const [data, setData] = useState(null);
  const [attempt, setAttempt] = useState(0);
  const [placeholder, setPlaceholder] = useState('none'); // none, shown, leaving

  const latest = useRef(loader);
  latest.current = loader;

  useEffect(() => {
    let cancelled = false;
    let shownAt = 0;
    const timers = [];
    const later = (fn, ms) => {
      const id = setTimeout(fn, ms);
      timers.push(id);
      return id;
    };

    setStatus('loading');
    setPlaceholder('none');

    const showTimer = later(() => {
      shownAt = Date.now();
      setPlaceholder('shown');
    }, SHOW_AFTER);

    // Hands the screen over to the content, after letting the placeholder serve
    // its minimum and play its exit if one ever appeared.
    function settle(apply) {
      if (cancelled) return;
      if (shownAt === 0) {
        clearTimeout(showTimer);
        apply();
        return;
      }
      const remaining = Math.max(0, MIN_VISIBLE - (Date.now() - shownAt));
      later(() => {
        setPlaceholder('leaving');
        later(() => {
          setPlaceholder('none');
          apply();
        }, LEAVE);
      }, remaining);
    }

    latest.current()
      .then((result) => settle(() => {
        setData(result);
        setStatus('ready');
      }))
      .catch(() => settle(() => setStatus('error')));

    return () => {
      cancelled = true;
      timers.forEach(clearTimeout);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [...keys, attempt]);

  const reload = useCallback(() => setAttempt((n) => n + 1), []);

  return {
    status,
    data,
    setData,
    reload,
    pending: placeholder !== 'none',
    leaving: placeholder === 'leaving',
  };
}
