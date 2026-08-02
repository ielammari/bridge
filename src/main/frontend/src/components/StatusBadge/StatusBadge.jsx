import './StatusBadge.css';

// Every offer and application status maps to a label and a token colour key.
// The keys are unique across both sets, so one table serves both.
const STATUS = {
  // Offer
  BROUILLON: { label: 'Brouillon', key: 'brouillon' },
  PUBLIEE: { label: 'Publiée', key: 'publiee' },
  CLOTUREE: { label: 'Clôturée', key: 'cloturee' },
  // Application
  NOUVELLE: { label: 'Nouvelle', key: 'nouvelle' },
  EN_REVUE: { label: 'En revue', key: 'en-revue' },
  EXAMEN_TECHNIQUE: { label: 'Examen technique', key: 'examen-technique' },
  ENTRETIEN_RH: { label: 'Entretien RH', key: 'entretien-rh' },
  REFUSEE: { label: 'Refusée', key: 'refusee' },
  EMBAUCHEE: { label: 'Embauchée', key: 'embauchee' },
};

/**
 * The single status indicator used across the app. The label is always spelled
 * out, so status is never conveyed by colour alone.
 */
export default function StatusBadge({ status }) {
  const entry = STATUS[status];
  if (!entry) return null;

  const style = {
    color: `var(--status-${entry.key})`,
    background: `var(--status-${entry.key}-bg)`,
  };

  return (
    <span className="status" style={style}>
      {entry.label}
    </span>
  );
}
