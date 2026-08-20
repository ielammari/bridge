// Shared formatters, so a date reads the same on every page and each Intl
// instance is built once.

const LONG_DATE = new Intl.DateTimeFormat('fr-FR', {
  day: 'numeric',
  month: 'long',
  year: 'numeric',
});

const SHORT_DATE = new Intl.DateTimeFormat('fr-FR', { day: 'numeric', month: 'long' });

const DATE_TIME = new Intl.DateTimeFormat('fr-FR', {
  day: 'numeric',
  month: 'long',
  hour: '2-digit',
  minute: '2-digit',
});

export const longDate = (iso) => LONG_DATE.format(new Date(iso));
export const shortDate = (iso) => SHORT_DATE.format(new Date(iso));
export const dateTime = (iso) => DATE_TIME.format(new Date(iso));

/** "14:00:00" from the API becomes "14:00". */
export const clockTime = (value) => (value ? value.slice(0, 5) : '');

export const euros = (amount) => `${Number(amount).toLocaleString('fr-FR')} €`;

/** A salary range in whichever of its two bounds an offer states. */
export const salaryText = (min, max) => {
  if (min && max) return `${euros(min)} à ${euros(max)}`;
  if (min) return `À partir de ${euros(min)}`;
  if (max) return `Jusqu'à ${euros(max)}`;
  return null;
};

/** A date in the browser's own timezone. toISOString answers in UTC, which is
 *  a day out either side of midnight. */
export function localDate(offsetDays = 0) {
  const date = new Date();
  date.setDate(date.getDate() + offsetDays);
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${date.getFullYear()}-${month}-${day}`;
}

const MONTH = new Intl.DateTimeFormat('fr-FR', { month: 'long', year: 'numeric' });

/** "2026-08" from any ISO date or instant, for grouping by period. */
export const monthOf = (iso) => String(iso).slice(0, 7);

export const monthLabel = (key) => {
  const label = MONTH.format(new Date(`${key}-01T00:00:00`));
  return label.charAt(0).toUpperCase() + label.slice(1);
};

/**
 * Which dated group an instant belongs to. Coarser the further back it goes,
 * since older news is scanned by period and recent news by day.
 */
export const timeGroup = (iso) => {
  const then = new Date(iso);
  const midnight = new Date();
  midnight.setHours(0, 0, 0, 0);
  const days = Math.floor((midnight - then) / 86400000);

  if (days < 0) return 'today';
  if (days === 0) return 'yesterday';
  if (days < 7) return 'week';
  if (days < 30) return 'month';
  return 'earlier';
};

export const TIME_GROUP_LABELS = {
  today: "Aujourd'hui",
  yesterday: 'Hier',
  week: 'Cette semaine',
  month: 'Ce mois-ci',
  earlier: 'Plus tôt',
};

export const TIME_GROUP_ORDER = ['today', 'yesterday', 'week', 'month', 'earlier'];
