/**
 * The way back to wherever a card was opened from. Pages carry their own
 * address in `state.from`, so the link returns to the exact view, filters and
 * open panel included.
 */
export function returnLink(state) {
  const from = state?.from;
  if (!from) return null;
  return {
    to: from,
    label: from.startsWith('/calendrier') ? 'Retour au calendrier' : 'Retour',
  };
}

/** Whether a page was reached from the calendar, which keeps its own way back. */
export const fromCalendar = (state) => Boolean(state?.from?.startsWith('/calendrier'));

/** The interview a record is opened at, named in the address. */
export const INTERVIEW = 'entretien';
