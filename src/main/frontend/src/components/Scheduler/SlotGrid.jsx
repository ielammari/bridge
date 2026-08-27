import { clockTime, localDate } from '../../constants/format.js';

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

/**
 * One evaluator's hours for a day. A slot they already hold is closed and
 * named, and the hour this application is on reads as the current one.
 */
export default function SlotGrid({ slots, date, applicationId, busy, onPick }) {
  return (
    <div className="scheduler__grid" role="group" aria-label="Créneaux de la journée">
      {slots.map((slot) => {
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
            onClick={() => onPick(slot.time)}
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
  );
}
