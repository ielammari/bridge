import { useState } from 'react';
import { applicationsApi } from '../../api/applications.js';
import { scheduleApi } from '../../api/schedule.js';
import { clockTime, localDate } from '../../constants/format.js';
import useResource from '../../hooks/useResource.js';
import './Scheduler.css';

/** Whether an hour on a given day has already gone by. */
function isPast(date, time) {
  if (date !== localDate()) return false;
  const now = new Date();
  return Number(time.slice(0, 2)) <= now.getHours();
}

/**
 * HR books an interview from an hourly grid. A slot taken by another interview
 * is disabled and names its occupant; this application's own slot is marked.
 */
export default function Scheduler({ applicationId, current, onScheduled }) {
  const [date, setDate] = useState(current?.date ?? localDate(1));
  const [busy, setBusy] = useState(null);
  const [error, setError] = useState(null);

  const { status, data, reload } = useResource(() => scheduleApi.day(date), [date]);

  async function pick(time) {
    setError(null);
    setBusy(time);
    try {
      onScheduled(await applicationsApi.schedule(applicationId, { date, time }));
    } catch (apiError) {
      setError(apiError.message);
    } finally {
      setBusy(null);
    }
  }

  return (
    <div className="scheduler">
      <label className="scheduler__date">
        <span>Date</span>
        <input type="date" value={date} min={localDate()}
          onChange={(event) => setDate(event.target.value)} />
      </label>

      {error && <p className="scheduler__error" role="alert">{error}</p>}

      {status === 'loading' && <p className="scheduler__muted">Chargement des créneaux...</p>}

      {status === 'error' && (
        <p className="scheduler__error" role="alert">
          La journée n'a pas pu être chargée.{' '}
          <button type="button" className="scheduler__retry" onClick={reload}>Réessayer</button>
        </p>
      )}

      {status === 'ready' && (
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
                title={takenByOther ? `Pris par ${slot.candidateName}` : undefined}
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
