import StatusBadge from '../../components/StatusBadge/StatusBadge.jsx';

// The four stages an application passes through, then the two ways it ends.
const STAGES = [
  { status: 'NOUVELLE', what: "Votre candidature est reçue, avec le CV que vous y avez joint." },
  { status: 'EN_REVUE', what: 'Un recruteur la lit et enregistre sa présélection, décision et commentaire.' },
  { status: 'EXAMEN_TECHNIQUE', what: "Un expert technique nommé vous note sur les traits de l'offre." },
  { status: 'ENTRETIEN_RH', what: "Le recruteur de l'offre vous reçoit à la date et l'heure fixées." },
];

const OUTCOMES = [
  { status: 'EMBAUCHEE', what: "Vos conditions d'embauche sont enregistrées." },
  { status: 'REFUSEE', what: 'La décision est enregistrée et reste consultable.' },
];

/** A mark on the spine, in the status's own pair of tokens. */
function Node({ status }) {
  const key = status.toLowerCase().replace('_', '-');
  return (
    <span
      className="funnel__node"
      style={{ background: `var(--status-${key}-bg)`, borderColor: `var(--status-${key})` }}
      aria-hidden="true"
    />
  );
}

/**
 * The shape of the process, named by the states an application really carries.
 * It runs on one line through the four stages and splits at the end, because a
 * procedure that can only end two ways is worth saying out loud.
 */
export default function Funnel() {
  return (
    <section className="pubband funnel">
      <div className="pubband__inner">
        <div className="section__head">
          <p className="section__eyebrow">Le parcours</p>
          <h2 className="section__title">De la candidature à la décision</h2>
        </div>

        <div className="funnel__track">
          <ol className="funnel__stages">
            {STAGES.map(({ status, what }) => (
              <li key={status} className="funnel__stage">
                <Node status={status} />
                <StatusBadge status={status} />
                <p className="funnel__what">{what}</p>
              </li>
            ))}
          </ol>

          <div className="funnel__fork">
            <ul className="funnel__outcomes">
              {OUTCOMES.map(({ status, what }) => (
                <li key={status} className="funnel__outcome">
                  <Node status={status} />
                  <StatusBadge status={status} />
                  <p className="funnel__what">{what}</p>
                </li>
              ))}
            </ul>
          </div>
        </div>
      </div>
    </section>
  );
}
