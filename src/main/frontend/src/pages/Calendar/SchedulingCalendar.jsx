import { useMemo, useState } from 'react';
import { useLocation, useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { applicationsApi } from '../../api/applications.js';
import { calendarApi } from '../../api/calendar.js';
import { scheduleApi } from '../../api/schedule.js';
import ConfirmDialog from '../../components/ConfirmDialog/ConfirmDialog.jsx';
import EmptyState from '../../components/EmptyState/EmptyState.jsx';
import ErrorState from '../../components/ErrorState/ErrorState.jsx';
import SlotGrid from '../../components/Scheduler/SlotGrid.jsx';
import Skeleton from '../../components/Skeleton/Skeleton.jsx';
import { useToast } from '../../components/Toast/ToastContext.jsx';
import {
  fullDay, kindCounts, loadLabel, monthKey, monthRange, parseMonth, today,
} from '../../constants/calendar.js';
import { clockTime } from '../../constants/format.js';
import useResource from '../../hooks/useResource.js';
import Workspace from '../Workspace/Workspace.jsx';
import DayDrawer from './DayDrawer.jsx';
import MonthGrid from './MonthGrid.jsx';
import MonthNav from './MonthNav.jsx';
import './calendar.css';
import '../../components/Scheduler/Scheduler.css';

const MONTH = 'mois';
const DAY = 'jour';
const EXPERT = 'expert';

const KIND = { EXAMEN_TECHNIQUE: 'Examen technique', ENTRETIEN_RH: 'Entretien RH' };

/**
 * The month an interview is booked in. The hours belong to whoever runs it, so
 * this shows that evaluator's own load and opens their day to pick from. The
 * booking itself is the same one the panel makes.
 */
export default function SchedulingCalendar() {
  const { id } = useParams();
  const applicationId = Number(id);
  const navigate = useNavigate();
  const location = useLocation();
  const toast = useToast();
  const [params, setParams] = useSearchParams();
  const [direction, setDirection] = useState(0);
  const [confirming, setConfirming] = useState(null);
  const [busy, setBusy] = useState(false);

  const expertId = params.get(EXPERT) ?? '';
  const now = today();
  const current = params.get(MONTH) ?? now.slice(0, 7);
  const { year, month } = parseMonth(current);
  const day = params.get(DAY) ?? '';

  const { from, to } = useMemo(() => monthRange(year, month), [year, month]);

  const app = useResource(() => applicationsApi.get(applicationId), [applicationId]);
  const experts = useResource(
    () => (expertId ? scheduleApi.experts(now) : Promise.resolve([])),
    [expertId],
  );
  const monthRes = useResource(
    () => calendarApi.range(from, to,
      expertId ? { scope: 'EVALUATOR', evaluatorId: expertId } : {}),
    [from, to, expertId],
  );
  const dayRes = useResource(
    () => (day ? scheduleApi.day(day, expertId || undefined) : Promise.resolve(null)),
    [day, expertId],
  );

  const entries = monthRes.data?.entries ?? [];
  const capacity = monthRes.data?.capacity ?? 8;

  const byDay = useMemo(() => {
    const map = new Map();
    for (const entry of entries) {
      const list = map.get(entry.date);
      if (list) list.push(entry);
      else map.set(entry.date, [entry]);
    }
    return map;
  }, [entries]);

  const expert = (experts.data ?? []).find((one) => String(one.id) === expertId);
  const who = expert ? `${expert.firstName} ${expert.lastName}` : null;

  const back = {
    to: location.state?.from
      ?? `/candidatures?candidature=${applicationId}&action=planification`,
    label: 'Retour à la candidature',
  };

  function write(changes) {
    const next = new URLSearchParams(params);
    for (const [key, value] of Object.entries(changes)) {
      if (value) next.set(key, value);
      else next.delete(key);
    }
    setParams(next, { replace: true });
  }

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

  /** Booking here settles the same appointment the panel would have set. */
  async function book() {
    setBusy(true);
    try {
      await applicationsApi.schedule(applicationId, {
        date: day,
        time: confirming,
        expertId: expertId ? Number(expertId) : null,
      });
      toast.success('Entretien planifié. Le candidat a été prévenu.');
      navigate(back.to, { replace: true });
    } catch (apiError) {
      toast.error(apiError.message);
      setConfirming(null);
    } finally {
      setBusy(false);
    }
  }

  if (app.status === 'loading') {
    return (
      <Workspace title="Planifier" back={back}>
        {app.pending && <Skeleton variant="page" leaving={app.leaving}
          label="Chargement de la candidature" />}
      </Workspace>
    );
  }

  if (app.status === 'error') {
    return (
      <Workspace title="Planifier" back={back}>
        <ErrorState onRetry={app.reload}>
          Cette candidature n'a pas pu être chargée. Réessayez dans un instant.
        </ErrorState>
      </Workspace>
    );
  }

  const kind = KIND[app.data.status];
  if (!kind) {
    return (
      <Workspace title="Planifier" back={back}>
        <EmptyState title="Cette candidature n'attend pas de rendez-vous."
          actionLabel={back.label} actionTo={back.to}>
          Un entretien se planifie une fois la candidature arrivée à l'examen technique ou à
          l'entretien RH.
        </EmptyState>
      </Workspace>
    );
  }

  const candidate = `${app.data.candidateFirstName} ${app.data.candidateLastName}`;
  const dayLoad = loadLabel(kindCounts(byDay.get(day) ?? []));

  return (
    <Workspace
      title="Planifier"
      subtitle={`${kind} de ${candidate}${who ? ` avec ${who}` : ''}`}
      back={back}
      toolbar={(
        <MonthNav year={year} month={month} onMonth={stepMonth} onYear={setYear}
          onToday={goToday} />
      )}
      panelOpen={Boolean(day)}
    >
      {monthRes.status === 'error' ? (
        <ErrorState onRetry={monthRes.reload}>
          Le calendrier n'a pas pu être chargé. Réessayez dans un instant.
        </ErrorState>
      ) : (
        <div className="calendar__body">
          {monthRes.pending ? (
            <Skeleton variant="calendar" leaving={monthRes.leaving}
              label="Chargement du calendrier" />
          ) : (
            <MonthGrid
              year={year}
              month={month}
              counts={byDay}
              capacity={capacity}
              direction={direction}
              selected={day}
              onSelect={(iso) => write({ [DAY]: iso })}
              canOpen={(cell) => cell.iso >= now}
            />
          )}
        </div>
      )}

      <DayDrawer
        open={Boolean(day)}
        iso={day}
        subtitle={dayLoad}
        onClose={() => write({ [DAY]: '' })}
      >
        {dayRes.status === 'loading' && (
          <p className="dayfeed__none">Chargement des créneaux...</p>
        )}
        {dayRes.status === 'error' && (
          <p className="scheduler__error" role="alert">
            La journée n'a pas pu être chargée.{' '}
            <button type="button" className="scheduler__retry" onClick={dayRes.reload}>
              Réessayer
            </button>
          </p>
        )}
        {dayRes.status === 'ready' && dayRes.data && (
          <SlotGrid slots={dayRes.data.slots} date={day} applicationId={applicationId}
            busy={busy ? confirming : null} onPick={setConfirming} />
        )}
      </DayDrawer>

      {confirming && (
        <ConfirmDialog
          open
          title="Fixer cet entretien ?"
          confirmLabel={`Planifier à ${clockTime(confirming)}`}
          busy={busy}
          onConfirm={book}
          onCancel={() => setConfirming(null)}
        >
          <strong>{kind} de {candidate}</strong>
          {who ? <> avec <strong>{who}</strong></> : null}, le {fullDay(day)} à{' '}
          <strong>{clockTime(confirming)}</strong>. Le candidat en est informé, et vous revenez
          à la candidature.
        </ConfirmDialog>
      )}
    </Workspace>
  );
}
