import { useState } from 'react';
import { applicationsApi } from '../../api/applications.js';
import { scheduleApi } from '../../api/schedule.js';
import Select from '../Select/Select.jsx';
import { clockTime, localDate } from '../../constants/format.js';
import useResource from '../../hooks/useResource.js';
import './Scheduler.css';

/** Whether an hour on a given day has already gone by. */
function isPast(date, time) {
  if (date !== localDate()) return false;
  const now = new Date();
  return Number(time.slice(0, 2)) <= now.getHours();
}

const KIND = { TECHNIQUE: 'examen technique', RH: 'entretien RH' };

/** Who a booked slot belongs to, named fully enough to tell two people apart. */
function occupant(slot) {
  return `${KIND[slot.type] ?? 'Entretien'} de ${slot.candidateName} pour l'offre « ${slot.offerTitle} »`;
}

/** An expert, with what they already hold that week. */
function expertLabel(expert) {
  const load = expert.load === 0 ? 'aucun entretien'
    : `${expert.load} entretien${expert.load > 1 ? 's' : ''}`;
  return `${expert.firstName} ${expert.lastName} (${load} cette semaine)`;
}

/**
 * HR books an interview on an hourly grid. The calendar belongs to whoever runs
 * it, so an exam names its expert first and then shows that expert's day, with
 * a taken hour disabled and named. The final interview is the recruiter's own
 * and opens straight on the grid.
 */
export default function Scheduler({ applicationId, kind, current, onScheduled }) {
  const needsExpert = kind === 'TECHNIQUE';
  const [date, setDate] = useState(current?.date ?? localDate(1));
  const [expertId, setExpertId] = useState(current?.evaluatorId ? String(current.evaluatorId) : '');
  const [busy, setBusy] = useState(null);
  const [error, setError] = useState(null);

  const experts = useResource(
    () => (needsExpert ? scheduleApi.experts(date) : Promise.resolve([])),
    [needsExpert, date],
  );

  // Without an expert there is no day to show: the hours belong to them.
  const ready = !needsExpert || Boolean(expertId);
  const { status, data, reload } = useResource(
    () => (ready ? scheduleApi.day(date, expertId || undefined) : Promise.resolve(null)),
    [date, expertId, ready],
  );

  async function pick(time) {
    setError(null);
    setBusy(time);
    try {
      onScheduled(await applicationsApi.schedule(applicationId, {
        date,
        time,
        expertId: expertId ? Number(expertId) : null,
      }));
    } catch (apiError) {
      setError(apiError.message);
    } finally {
      setBusy(null);
    }
  }

  return (
    <div className="scheduler">
      <div className="scheduler__who">
        {needsExpert && (
          <Select
            label="Expert technique"
            value={expertId}
            onChange={(event) => setExpertId(event.target.value)}
            options={(experts.data ?? []).map((expert) => ({
              value: String(expert.id),
              label: expertLabel(expert),
            }))}
            placeholder="Choisir un expert"
            hint={experts.status === 'ready' && (experts.data ?? []).length === 0
              ? 'Aucun expert : créez un compte expert dans Paramètres.'
              : undefined}
          />
        )}

        <label className="scheduler__date">
          <span>Date</span>
          <input type="date" value={date} min={localDate()}
            onChange={(event) => setDate(event.target.value)} />
        </label>
      </div>

      {error && <p className="scheduler__error" role="alert">{error}</p>}

      {!ready && (
        <p className="scheduler__muted">
          Choisissez l'expert pour voir les créneaux dont il dispose.
        </p>
      )}

      {ready && status === 'loading' && <p className="scheduler__muted">Chargement des créneaux...</p>}

      {ready && status === 'error' && (
        <p className="scheduler__error" role="alert">
          La journée n'a pas pu être chargée.{' '}
          <button type="button" className="scheduler__retry" onClick={reload}>Réessayer</button>
        </p>
      )}

      {ready && status === 'ready' && data && (
        <div className="scheduler__grid" role="group" aria-label="Créneaux de la journée">
          {data.slots.map((slot) => {
            const mine = slot.taken && slot.applicationId === applicationId;
            const takenByOther = slot.taken && !mine;
            const past = isPast(date, slot.time);
            const blocked = takenByOther || past;
            return (
              <button
                key={slot.time}
                type="button"
                className={`slot${mine ? ' slot--current' : ''}${blocked ? ' slot--taken' : ''}`}
                disabled={blocked || busy === slot.time}
                onClick={() => pick(slot.time)}
                title={takenByOther ? occupant(slot) : undefined}
                aria-label={takenByOther
                  ? `${clockTime(slot.time)}, pris : ${occupant(slot)}`
                  : undefined}
              >
                <span className="slot__time mono">{clockTime(slot.time)}</span>
                {mine && <span className="slot__note">Actuel</span>}
                {takenByOther && <span className="slot__note">{slot.candidateName}</span>}
                {past && !takenByOther && <span className="slot__note">Passé</span>}
              </button>
            );
          })}
        </div>
      )}
    </div>
  );
}
