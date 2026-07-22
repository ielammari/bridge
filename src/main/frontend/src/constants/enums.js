// French display labels for the backend enums. Identifiers stay English; only
// what the user reads is translated.

export const DEGREE_OPTIONS = [
  { value: 'BAC', label: 'Baccalauréat' },
  { value: 'BAC_2', label: 'Bac +2' },
  { value: 'BAC_3', label: 'Bac +3 (Licence)' },
  { value: 'BAC_5', label: 'Bac +5 (Master)' },
  { value: 'DOCTORAT', label: 'Doctorat' },
];

export const CONTRACT_OPTIONS = [
  { value: 'PERMANENT', label: 'CDI' },
  { value: 'FIXED_TERM', label: 'CDD' },
  { value: 'FREELANCE', label: 'Freelance' },
  { value: 'INTERNSHIP', label: 'Stage' },
];

export const REMOTE_OPTIONS = [
  { value: 'ON_SITE', label: 'Sur site' },
  { value: 'HYBRID', label: 'Hybride' },
  { value: 'FULL_REMOTE', label: 'Télétravail complet' },
];

function labelMap(options) {
  return Object.fromEntries(options.map((o) => [o.value, o.label]));
}

export const DEGREE_LABELS = labelMap(DEGREE_OPTIONS);
export const CONTRACT_LABELS = labelMap(CONTRACT_OPTIONS);
export const REMOTE_LABELS = labelMap(REMOTE_OPTIONS);
