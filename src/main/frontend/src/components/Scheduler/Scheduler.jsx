import { useEffect, useState } from 'react';
import { applicationsApi } from '../../api/applications.js';
import { scheduleApi } from '../../api/schedule.js';
import './Scheduler.css';

function tomorrow() {
  const d = new Date();
  d.setDate(d.getDate() + 1);
  return d.toISOString().slice(0, 10);
}

/**
 * HR books an interview from an hourly grid. A slot already taken by another
 * interview is disabled and names its occupant; the slot currently held by this
 * application is marked so HR can keep or move it.
 */
export default function Scheduler({ applicationId, current, onScheduled }) {
  const [date, setDate] = useState(current?.date ?? tomorrow());
  const [grid, setGrid] = useState(null);
  const [busy, setBusy] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    let cancelled = false;
    setGrid(null);
    scheduleApi.day(date)
      .then((data) => {
        if (!cancelled) setGrid(data);
      })
      .catch(() => {
        if (!cancelled) setError('La journée n\'a pas pu être chargée.');
      });
    return () => {
      cancelled = true;
    };
  }, [date]);

  async function pick(time) {
    setError(null);
    setBusy(time);
    try {
      const updated = await applicationsApi.schedule(applicationId, { date, time });
      onScheduled(updated);
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
        <input type="date" value={date} min={new Date().toISOString().slice(0, 10)}
          onChange={(e) => setDate(e.target.value)} />
      </label>

      {error && <p className="scheduler__error" role="alert">{error}</p>}

      {!grid ? (
        <p className="scheduler__muted">Chargement des créneaux...</p>
      ) : (
        <div className="scheduler__grid" role="group" aria-label="Créneaux de la journée">
          {grid.slots.map((slot) => {
            const mine = slot.taken && slot.applicationId === applicationId;
            const takenByOther = slot.taken && !mine;
            return (
              <button
                key={slot.time}
                type="button"
                className={`slot${mine ? ' slot--current' : ''}${takenByOther ? ' slot--taken' : ''}`}
                disabled={takenByOther || busy === slot.time}
                onClick={() => pick(slot.time)}
                title={takenByOther ? `Pris par ${slot.candidateName}` : undefined}
              >
                <span className="slot__time mono">{slot.time}</span>
                {mine && <span className="slot__note">Actuel</span>}
                {takenByOther && <span className="slot__note">{slot.candidateName}</span>}
              </button>
            );
          })}
        </div>
      )}
    </div>
  );
}
