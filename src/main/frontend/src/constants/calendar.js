// The month grid and what colours it. A month always draws six weeks starting
// on a Monday, so the grid never changes height as the reader pages through.

export const WEEKDAYS = ['LUN', 'MAR', 'MER', 'JEU', 'VEN', 'SAM', 'DIM'];
export const WEEKS = 6;
const DAYS = WEEKS * 7;

/** How far a load ramp reaches: five steps, plus nothing at all. */
export const LOAD_STEPS = 5;

const ISO = (date) => {
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${date.getFullYear()}-${month}-${day}`;
};

/** "2026-08" for the month a date belongs to. */
export const monthKey = (year, month) => `${year}-${String(month + 1).padStart(2, '0')}`;

/** The year and zero based month a "2026-08" key names. */
export function parseMonth(key) {
  const [year, month] = String(key).split('-');
  return { year: Number(year), month: Number(month) - 1 };
}

/** The Monday on or before a date. */
function mondayOf(date) {
  const start = new Date(date);
  // getDay is Sunday first; the week here starts on Monday.
  start.setDate(start.getDate() - ((start.getDay() + 6) % 7));
  return start;
}

/**
 * The forty two days a month's grid draws, each carrying its ISO date, its
 * number, and whether it belongs to the month itself.
 */
export function monthDays(year, month) {
  const first = mondayOf(new Date(year, month, 1));
  return Array.from({ length: DAYS }, (_, index) => {
    const date = new Date(first);
    date.setDate(date.getDate() + index);
    return {
      iso: ISO(date),
      number: date.getDate(),
      weekend: date.getDay() === 0 || date.getDay() === 6,
      inMonth: date.getMonth() === month,
    };
  });
}

/** The first and last day the grid draws, which is what the server is asked for. */
export function monthRange(year, month) {
  const days = monthDays(year, month);
  return { from: days[0].iso, to: days[days.length - 1].iso };
}

/**
 * Which step of the ramp a day sits on: nothing at all is 0, and a day with no
 * hour left is the last step. Capacity is the organisation's bookable hours, so
 * the colour follows the setting rather than a number written here.
 */
export function loadStep(count, capacity) {
  if (count <= 0) return 0;
  if (count >= capacity) return LOAD_STEPS;
  return Math.min(LOAD_STEPS, Math.ceil((count / capacity) * LOAD_STEPS));
}

// The kinds of interview a day can hold, in the order a cell lists them.
const KIND_ORDER = ['TECHNIQUE', 'RH'];
const KIND_NOUN = {
  TECHNIQUE: ['examen', 'examens'],
  RH: ['entretien', 'entretiens'],
};

/** A day's interviews tallied by kind, in a fixed order so a cell never reorders. */
export function kindCounts(entries) {
  const tally = new Map();
  for (const entry of entries) {
    tally.set(entry.type, (tally.get(entry.type) ?? 0) + 1);
  }
  return KIND_ORDER
    .filter((type) => tally.has(type))
    .map((type) => ({ type, count: tally.get(type) }));
}

/** What a kind is called at that many. */
export const kindNoun = (type, count) => (KIND_NOUN[type] ?? KIND_NOUN.RH)[count > 1 ? 1 : 0];

/** A day's load in words, for the readers its figures do not reach. */
export const loadLabel = (kinds) => (kinds.length === 0
  ? 'aucun entretien'
  : kinds.map(({ type, count }) => `${count} ${kindNoun(type, count)}`).join(', '));

const MONTH_NAME = new Intl.DateTimeFormat('fr-FR', { month: 'long' });
const FULL_DAY = new Intl.DateTimeFormat('fr-FR', {
  weekday: 'long', day: 'numeric', month: 'long', year: 'numeric',
});

export function monthName(year, month) {
  const label = MONTH_NAME.format(new Date(year, month, 1));
  return label.charAt(0).toUpperCase() + label.slice(1);
}

/** "Vendredi 14 août 2026", for the day a drawer is showing. */
export function fullDay(iso) {
  const label = FULL_DAY.format(new Date(`${iso}T00:00:00`));
  return label.charAt(0).toUpperCase() + label.slice(1);
}

export const today = () => ISO(new Date());
