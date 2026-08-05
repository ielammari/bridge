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

// Optional: the empty choice stores null, not a fourth value.
export const GENDER_OPTIONS = [
  { value: 'HOMME', label: 'Homme' },
  { value: 'FEMME', label: 'Femme' },
  { value: 'AUTRE', label: 'Autre' },
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

// The text alert shown alongside each application status, in the candidate's
// own terms.
export const APPLICATION_ALERTS = {
  NOUVELLE: 'Votre candidature a été reçue. Le recruteur va l\'examiner.',
  EN_REVUE: 'Le recruteur examine votre candidature.',
  EXAMEN_TECHNIQUE: 'Vous passez à l\'examen technique. La date vous sera communiquée.',
  ENTRETIEN_RH: 'Vous passez à l\'entretien RH. La date vous sera communiquée.',
  REFUSEE: 'Votre candidature n\'a pas été retenue cette fois.',
  EMBAUCHEE: 'Félicitations, vous êtes embauché(e).',
};

// An application whose state can no longer change. Everything else is active.
const TERMINAL = ['REFUSEE', 'EMBAUCHEE'];

export const isTerminal = (status) => TERMINAL.includes(status);

export const EVALUATION_TYPE_LABELS = {
  PRESELECTION: 'Présélection',
  TECHNIQUE: 'Examen technique',
  ENTRETIEN_RH: 'Entretien RH',
};

export const DECISION_LABELS = { VALIDEE: 'Favorable', REFUSEE: 'Défavorable' };

export const APPOINTMENT_TYPE_LABELS = { TECHNIQUE: 'Examen technique', RH: 'Entretien RH' };

export const STATUS_LABELS = {
  NOUVELLE: 'Nouvelle',
  EN_REVUE: 'En revue',
  EXAMEN_TECHNIQUE: 'Examen technique',
  ENTRETIEN_RH: 'Entretien RH',
  REFUSEE: 'Refusée',
  EMBAUCHEE: 'Embauchée',
};

// What each notification is for, so a preference toggle says what it turns off.
export const NOTIFICATION_LABELS = {
  APPLICATION_RECEIVED: 'Nouvelle candidature reçue',
  SCHEDULE_NEEDED: 'Un entretien reste à planifier',
  INTERVIEW_SCHEDULED: 'Un entretien a été fixé',
  REJECTED: 'Une candidature n\'a pas été retenue',
  HIRED: 'Une embauche est confirmée',
};

export const ROLE_OPTIONS = [
  { value: 'RH', label: 'Responsable RH' },
  { value: 'EXPERT', label: 'Expert technique' },
];
