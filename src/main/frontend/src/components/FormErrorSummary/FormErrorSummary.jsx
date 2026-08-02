import './FormErrorSummary.css';

/** The errors from a failed submit, gathered at the top with a link to each
 *  field, which on a long form may be off screen. */
export default function FormErrorSummary({ errors, rules }) {
  const entries = Object.entries(errors);
  if (entries.length === 0) return null;

  return (
    <div className="summary" role="alert">
      <p className="summary__title">
        {entries.length === 1
          ? 'Un champ demande votre attention avant l\'envoi.'
          : `${entries.length} champs demandent votre attention avant l'envoi.`}
      </p>
      <ul className="summary__list">
        {entries.map(([key, message]) => (
          <li key={key}>
            <button
              type="button"
              className="summary__link"
              onClick={() => document.querySelector(`[name="${key}"]`)?.focus()}
            >
              {rules[key]?.label ?? key}
            </button>
            {' : '}
            {message}
          </li>
        ))}
      </ul>
    </div>
  );
}
