import { useEffect, useRef, useState } from 'react';
import Icon from '../../components/Icon/Icon.jsx';
import { monthName } from '../../constants/calendar.js';

// How far either side of this year the calendar may be taken.
const BACK = 1;
const FORWARD = 5;

/**
 * Which month is on screen, and the way through them. The year is set in the
 * figure face because it is a value: opening it makes it an input, and the
 * month name beside it stays prose.
 */
export default function MonthNav({ year, month, onMonth, onYear, onToday }) {
  const [editing, setEditing] = useState(false);
  const [draft, setDraft] = useState(String(year));
  const field = useRef(null);

  const now = new Date().getFullYear();
  const min = now - BACK;
  const max = now + FORWARD;

  useEffect(() => {
    if (!editing) return;
    field.current?.focus();
    field.current?.select();
  }, [editing]);

  function open() {
    setDraft(String(year));
    setEditing(true);
  }

  function commit() {
    const parsed = Number(draft);
    setEditing(false);
    if (!Number.isInteger(parsed)) return;
    onYear(Math.min(max, Math.max(min, parsed)));
  }

  return (
    <div className="monthnav">
      <div className="monthnav__steps">
        <button type="button" className="monthnav__step" onClick={() => onMonth(-1)}
          aria-label="Mois précédent" title="Mois précédent">
          <Icon name="chevron" className="monthnav__prev" />
        </button>

        <p className="monthnav__label">
          <span className="monthnav__month">{monthName(year, month)}</span>
          {editing ? (
            <input
              ref={field}
              className="monthnav__year monthnav__year--editing mono"
              type="number"
              inputMode="numeric"
              min={min}
              max={max}
              value={draft}
              aria-label="Année"
              onChange={(event) => setDraft(event.target.value)}
              onBlur={commit}
              onKeyDown={(event) => {
                if (event.key === 'Enter') commit();
                if (event.key === 'Escape') setEditing(false);
              }}
            />
          ) : (
            <button type="button" className="monthnav__year mono" onClick={open}
              onDoubleClick={open} title="Changer l'année">
              {year}
            </button>
          )}
        </p>

        <button type="button" className="monthnav__step" onClick={() => onMonth(1)}
          aria-label="Mois suivant" title="Mois suivant">
          <Icon name="chevron" className="monthnav__next" />
        </button>
      </div>

      <button type="button" className="monthnav__today" onClick={onToday}>
        Aujourd'hui
      </button>
    </div>
  );
}
