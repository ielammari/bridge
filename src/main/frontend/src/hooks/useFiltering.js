import { useMemo } from 'react';
import { useSearchParams } from 'react-router-dom';

// The filter lives in the address, so a filtered listing can be returned to,
// refreshed, and shared as the thing the user was actually looking at.
const DIMENSION = 'filtre';
const VALUE = 'valeur';

/**
 * The filtering and grouping behind a history listing.
 *
 * A dimension is one way of cutting the list: `of` reads the value to match on,
 * `groupOf` the value to gather sections by when it differs (a date filters on
 * the day but groups by month), and `labelOf` names a section.
 *
 * Choosing a dimension without a value groups by it; adding a value narrows to
 * that value; choosing nothing groups by `fallback`. A listing is therefore
 * never a flat undifferentiated run.
 */
export default function useFiltering(items, dimensions, fallback) {
  const [params, setParams] = useSearchParams();
  const dimension = params.get(DIMENSION) ?? '';
  const value = params.get(VALUE) ?? '';

  /**
   * Both keys are written together, since changing the dimension clears the
   * value and two separate writes would each start from the same snapshot.
   * Replaces rather than pushes, so back leaves the listing instead of
   * retracing every keystroke.
   */
  function write(nextDimension, nextValue) {
    const updated = new URLSearchParams(params);
    for (const [key, val] of [[DIMENSION, nextDimension], [VALUE, nextValue]]) {
      if (val) {
        updated.set(key, val);
      } else {
        updated.delete(key);
      }
    }
    setParams(updated, { replace: true });
  }

  const active = dimensions.find((d) => d.key === dimension);

  const kept = useMemo(() => {
    if (!active || !value.trim()) return items;
    const needle = value.trim().toLowerCase();
    return items.filter((item) => {
      const raw = active.of(item);
      if (raw === null || raw === undefined) return false;
      return active.type === 'search'
        ? String(raw).toLowerCase().includes(needle)
        : String(raw) === value;
    });
  }, [items, active, value]);

  const sections = useMemo(() => {
    const by = active ?? fallback;
    const read = by.groupOf ?? by.of;

    const buckets = new Map();
    for (const item of kept) {
      const key = String(read(item) ?? '');
      if (!buckets.has(key)) buckets.set(key, []);
      buckets.get(key).push(item);
    }

    const compare = by.compare ?? ((a, b) => a.label.localeCompare(b.label, 'fr'));
    return [...buckets.entries()]
      .map(([key, group]) => ({ key, label: by.labelOf(key), items: group }))
      .sort(compare);
  }, [kept, active, fallback]);

  return {
    dimension,
    value,
    kept,
    sections,
    setDimension: (key) => write(key, ''),
    setValue: (next) => write(dimension, next),
  };
}
