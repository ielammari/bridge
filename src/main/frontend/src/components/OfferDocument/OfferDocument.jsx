import InfoHint from '../InfoHint/InfoHint.jsx';
import './OfferDocument.css';

/** One group of traits, named and listed, or the fact that it holds none. */
function Group({ title, traits, empty }) {
  return (
    <div className="offerdoc__group">
      <h3 className="offerdoc__group-title">{title}</h3>
      {traits.length === 0 ? (
        <p className="offerdoc__none">{empty}</p>
      ) : (
        <ul className="offerdoc__tags">
          {traits.map((trait) => (
            <li key={trait.traitId} className={`tag tag--${trait.mandatory ? 'required' : 'plus'}`}>
              {trait.label}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

/**
 * An offer as it reads: the post, then the profile it looks for. The document
 * does not change with who opens it, so a visitor and a candidate are given the
 * same one.
 */
export default function OfferDocument({ description, requirements }) {
  return (
    <>
      <section className="doc__section">
        <h2 className="doc__heading">Le poste</h2>
        <p className="offerdoc__desc">{description}</p>
      </section>

      <section className="doc__section">
        <h2 className="doc__heading">
          Profil recherché
          <InfoHint label="Obligatoire ou atout">
            Les traits obligatoires conditionnent la candidature. Les autres comptent en votre
            faveur sans être exigés.
          </InfoHint>
        </h2>

        <Group
          title="Obligatoires"
          empty="Aucun trait obligatoire."
          traits={requirements.filter((requirement) => requirement.mandatory)}
        />
        <Group
          title="Atouts"
          empty="Aucun atout déclaré."
          traits={requirements.filter((requirement) => !requirement.mandatory)}
        />
      </section>
    </>
  );
}
