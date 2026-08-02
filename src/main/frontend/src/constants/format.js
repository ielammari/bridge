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

/** A date in the browser's own timezone. toISOString answers in UTC, which is
 *  a day out either side of midnight. */
export function localDate(offsetDays = 0) {
  const date = new Date();
  date.setDate(date.getDate() + offsetDays);
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${date.getFullYear()}-${month}-${day}`;
}
