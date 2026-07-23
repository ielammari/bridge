import Icon from '../Icon/Icon.jsx';
import './FunnelRail.css';

// The application lifecycle is a fixed sequence of four stages plus a terminal
// decision. Order carries real meaning here, which is what makes the rail
// content rather than decoration.
const STAGES = [
  { key: 'NOUVELLE', label: 'Reçue' },
  { key: 'EN_REVUE', label: 'Présélection' },
  { key: 'EXAMEN_TECHNIQUE', label: 'Examen technique' },
  { key: 'ENTRETIEN_RH', label: 'Entretien RH' },
  { key: 'DECISION', label: 'Décision' },
];

const POSITION = {
  NOUVELLE: 0,
  EN_REVUE: 1,
  EXAMEN_TECHNIQUE: 2,
  ENTRETIEN_RH: 3,
  EMBAUCHEE: 4,
  REFUSEE: 4,
};

// Rejection can happen at several points and the status alone does not say
// which, so a refused application shows the stages as inactive and only the
// terminal node carries the outcome, rather than claiming a stage it may not
// have reached.
function stateFor(index, status) {
  const isTerminal = index === STAGES.length - 1;

  if (status === 'REFUSEE') {
    return isTerminal ? 'refused' : 'inactive';
  }
  if (isTerminal) {
    return status === 'EMBAUCHEE' ? 'hired' : 'upcoming';
  }

  const position = POSITION[status];
  if (index < position) return 'done';
  if (index === position) return 'current';
  return 'upcoming';
}

function terminalLabel(status) {
  if (status === 'EMBAUCHEE') return 'Embauche';
  if (status === 'REFUSEE') return 'Refusée';
  return 'Décision';
}

/** The signature progress rail for an application's position in the funnel. */
export default function FunnelRail({ status }) {
  return (
    <ol className="rail" aria-label="Progression de la candidature">
      {STAGES.map((stage, index) => {
        const state = stateFor(index, status);
        const label = index === STAGES.length - 1 ? terminalLabel(status) : stage.label;
        return (
          <li key={stage.key} className={`rail__step rail__step--${state}`} aria-current={state === 'current' ? 'step' : undefined}>
            <span className="rail__node">
              {(state === 'done' || state === 'hired') && <Icon name="check" />}
              {state === 'refused' && <Icon name="close" />}
            </span>
            <span className="rail__label">{label}</span>
          </li>
        );
      })}
    </ol>
  );
}
