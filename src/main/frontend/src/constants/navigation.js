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

/**
 * Where an action interrupted by signing in resumes. It travels in the address
 * rather than in router state, so it survives a refresh and a shared link, and
 * only a path inside the application is accepted.
 */
export const SUITE = 'suite';

export function readSuite(search) {
  const value = new URLSearchParams(search).get(SUITE);
  return value && value.startsWith('/') && !value.startsWith('//') ? value : null;
}

export const withSuite = (path, suite) => `${path}?${SUITE}=${encodeURIComponent(suite)}`;
