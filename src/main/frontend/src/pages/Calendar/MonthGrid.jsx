import { useEffect, useMemo, useRef, useState } from 'react';
import {
  WEEKDAYS, kindCounts, kindNoun, loadLabel, loadStep, monthDays, today,
} from '../../constants/calendar.js';
import useMediaQuery from '../../hooks/useMediaQuery.js';

// In step with --duration-slide in calendar.css.
const SLIDE = 220;

/**
 * One month of days. A cell carries its date, what sits on it and of what kind,
 * and a gauge of how much of the day that spends, so the colour never answers
 * alone. Paging slides the month it came from out the way it is going.
 */
export default function MonthGrid({
  year, month, counts, capacity, direction, selected, onSelect, canOpen,
}) {
  const days = useMemo(() => monthDays(year, month), [year, month]);
  const reduced = useMediaQuery('(prefers-reduced-motion: reduce)');
  // On a scrolling page the months are not stacked, so there is nothing to
  // slide one over the other.
  const narrow = useMediaQuery('(max-width: 64rem)');
  const [leaving, setLeaving] = useState(null);
  const previous = useRef(null);

  useEffect(() => {
    const was = previous.current;
    previous.current = { year, month, days, counts, capacity };
    if (!was || reduced || narrow || !direction) return undefined;
    if (was.year === year && was.month === month) return undefined;

    setLeaving({ ...was, direction });
    const timer = setTimeout(() => setLeaving(null), SLIDE);
    return () => clearTimeout(timer);
  }, [year, month, days, counts, capacity, direction, reduced, narrow]);

  const way = direction > 0 ? 'next' : 'prev';

  return (
    <div className="monthgrid">
      <div className="monthgrid__week" aria-hidden="true">
        {WEEKDAYS.map((name) => (
          <span key={name} className="monthgrid__weekday mono">{name}</span>
        ))}
      </div>

      <div className="monthgrid__stage">
        {leaving && (
          <div className={`monthgrid__layer monthgrid__layer--out-${leaving.direction > 0 ? 'next' : 'prev'}`}
            aria-hidden="true">
            {cells(leaving.days, leaving.counts, leaving.capacity, null, null, canOpen)}
          </div>
        )}
        <div className={`monthgrid__layer${leaving ? ` monthgrid__layer--in-${way}` : ''}`}
          key={`${year}-${month}`}>
          {cells(days, counts, capacity, selected, onSelect, canOpen)}
        </div>
      </div>
    </div>
  );
}

function cells(days, counts, capacity, selected, onSelect, canOpen) {
  const now = today();

  return (
    <div className="monthgrid__days">
      {days.map((day) => {
        // A day outside the month is drawn so the weeks keep their shape, and
        // is not a target: it belongs to the month either side.
        if (!day.inMonth) {
          return <span key={day.iso} className="day day--outside" aria-hidden="true">
            <span className="day__number">{day.number}</span>
          </span>;
        }

        const held = counts.get(day.iso) ?? [];
        const count = held.length;
        const kinds = kindCounts(held);
        const step = loadStep(count, capacity);
        const open = Boolean(onSelect) && canOpen(day, count);
        const classes = ['day', `day--load-${step}`];
        if (day.weekend) classes.push('day--weekend');
        if (day.iso < now) classes.push('day--past');
        if (day.iso === now) classes.push('day--today');
        if (day.iso === selected) classes.push('day--selected');

        return (
          <button
            key={day.iso}
            type="button"
            className={classes.join(' ')}
            disabled={!open}
            onClick={() => onSelect?.(day.iso)}
            aria-pressed={day.iso === selected}
            aria-label={`${day.number}, ${loadLabel(kinds)}`}
          >
            <span className="day__number" aria-hidden="true">{day.number}</span>
            {count > 0 && (
              <>
                <span className="day__load" aria-hidden="true">
                  {kinds.map((kind) => (
                    <span key={kind.type} className="day__kind">
                      <span className="day__count mono">{kind.count}</span>
                      <span className="day__noun">{kindNoun(kind.type, kind.count)}</span>
                    </span>
                  ))}
                </span>
                <span className="day__gauge" aria-hidden="true"
                  style={{ '--fill': Math.min(1, count / capacity) }} />
              </>
            )}
          </button>
        );
      })}
    </div>
  );
}
