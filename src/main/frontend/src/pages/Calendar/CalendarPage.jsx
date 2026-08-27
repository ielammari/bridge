import { useMemo, useState } from 'react';
import { useLocation, useSearchParams } from 'react-router-dom';
import { calendarApi } from '../../api/calendar.js';
import ErrorState from '../../components/ErrorState/ErrorState.jsx';
import Skeleton from '../../components/Skeleton/Skeleton.jsx';
import Toggle from '../../components/Toggle/Toggle.jsx';
import {
  kindCounts, loadLabel, monthKey, monthRange, parseMonth, today,
} from '../../constants/calendar.js';
import { useAuth } from '../../context/AuthContext.jsx';
import useResource from '../../hooks/useResource.js';
import Workspace from '../Workspace/Workspace.jsx';
import DayDrawer from './DayDrawer.jsx';
import DayEntries from './DayEntries.jsx';
import MonthGrid from './MonthGrid.jsx';
import MonthNav from './MonthNav.jsx';
import './calendar.css';

// What is on screen, kept in the address: the month, the day that is open, and
// which of a recruiter's two calendars they are reading.
const MONTH = 'mois';
const DAY = 'jour';
const VIEW = 'voir';

// A recruiter runs their own interviews and arranges the exams experts run.
// The two are counted and coloured apart, so one page never mixes them.
const SIDES = [
  { value: 'rh', label: 'Mes entretiens', scope: 'MINE' },
  { value: 'technique', label: 'Examens planifiés', scope: 'PLANNED' },
];

export default function CalendarPage() {
  const { user } = useAuth();
  const location = useLocation();
  const [params, setParams] = useSearchParams();
  const [direction, setDirection] = useState(0);

  const isRecruiter = user.role === 'RH';
  const side = SIDES.find((s) => s.value === params.get(VIEW)) ?? SIDES[0];
  const scope = isRecruiter ? side.scope : 'MINE';

  const now = today();
  const current = params.get(MONTH) ?? now.slice(0, 7);
  const { year, month } = parseMonth(current);
  const selected = params.get(DAY) ?? '';

  const { from, to } = useMemo(() => monthRange(year, month), [year, month]);

  const { status, data, reload, pending, leaving } = useResource(
    () => calendarApi.range(from, to, { scope }),
    [from, to, scope],
  );

  const entries = data?.entries ?? [];
  const capacity = data?.capacity ?? 8;

  // Every day of the range that holds something, in the order the server gave.
  const byDay = useMemo(() => {
    const map = new Map();
    for (const entry of entries) {
      const list = map.get(entry.date);
      if (list) list.push(entry);
      else map.set(entry.date, [entry]);
    }
    return map;
  }, [entries]);

  // The figures name the month on screen, not the whole calendar.
  const inMonth = entries.filter((entry) => entry.date.startsWith(current));
  const outstanding = inMonth.filter((entry) => !entry.recorded).length;

  function write(changes) {
    const next = new URLSearchParams(params);
    for (const [key, value] of Object.entries(changes)) {
      if (value) next.set(key, value);
      else next.delete(key);
    }
    setParams(next, { replace: true });
  }

  /** Paging leaves the open day behind: it belongs to the month it was in. */
  function goTo(nextYear, nextMonth, step) {
    setDirection(step);
    write({ [MONTH]: monthKey(nextYear, nextMonth), [DAY]: '' });
  }

  function stepMonth(step) {
    const date = new Date(year, month + step, 1);
    goTo(date.getFullYear(), date.getMonth(), step);
  }

  function setYear(next) {
    if (next === year) return;
    goTo(next, month, next > year ? 1 : -1);
  }

  function goToday() {
    const { year: y, month: m } = parseMonth(now.slice(0, 7));
    if (y === year && m === month) return;
    goTo(y, m, y > year || (y === year && m > month) ? 1 : -1);
  }

  const open = Boolean(selected) && byDay.has(selected);
  const dayEntries = byDay.get(selected) ?? [];
  // What a recruiter arranged is someone else's day, so a card names its expert.
  const arranged = scope === 'PLANNED';

  return (
    <Workspace
      title="Calendrier"
      stats={status === 'ready' ? [
        { value: inMonth.length, label: 'ce mois-ci' },
        { value: outstanding, label: 'à venir' },
      ] : []}
      action={isRecruiter && (
        <Toggle
          name="calendar-side"
          label="Quel calendrier afficher"
          value={side.value}
          options={SIDES.map(({ value, label }) => ({ value, label }))}
          onChange={(value) => write({ [VIEW]: value, [DAY]: '' })}
        />
      )}
      toolbar={(
        <MonthNav year={year} month={month} onMonth={stepMonth} onYear={setYear}
          onToday={goToday} />
      )}
      panelOpen={open}
    >
      {status === 'error' ? (
        <ErrorState onRetry={reload}>
          Le calendrier n'a pas pu être chargé. Réessayez dans un instant.
        </ErrorState>
      ) : (
        <div className="calendar__body">
          {pending ? (
            <Skeleton variant="calendar" leaving={leaving} label="Chargement du calendrier" />
          ) : (
            <MonthGrid
              year={year}
              month={month}
              counts={byDay}
              capacity={capacity}
              direction={direction}
              selected={selected}
              onSelect={(iso) => write({ [DAY]: iso })}
              canOpen={(day, count) => count > 0}
            />
          )}
        </div>
      )}

      <DayDrawer
        open={open}
        iso={selected}
        subtitle={loadLabel(kindCounts(dayEntries))}
        onClose={() => write({ [DAY]: '' })}
      >
        <DayEntries
          entries={dayEntries}
          role={user.role}
          showEvaluator={arranged}
          from={`${location.pathname}${location.search}`}
        />
      </DayDrawer>
    </Workspace>
  );
}
