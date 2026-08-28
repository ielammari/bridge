import { Link } from 'react-router-dom';

/** One kind of trait a real offer asks for, named so the chips are not read as
 *  a list of equals. */
function Row({ label, traits, kind }) {
  if (traits.length === 0) return null;
  return (
    <div className="feature__row">
      <p className="feature__row-label">{label}</p>
      <ul className="feature__tags">
        {traits.map((trait) => (
          <li key={trait.traitId} className={`tag tag--${kind}`}>{trait.label}</li>
        ))}
      </ul>
    </div>
  );
}

/**
 * What the application does for the person applying. Each block states one rule
 * the funnel actually enforces, and the first shows it on a real offer.
 */
export default function Features({ sample }) {
  const required = sample?.requirements.filter((requirement) => requirement.mandatory) ?? [];
  const plus = sample?.requirements.filter((requirement) => !requirement.mandatory) ?? [];

  return (
    <section className="pubband features" id="fonctionnalites">
      <div className="pubband__inner">
        <div className="section__head">
          <p className="section__eyebrow">Fonctionnalités</p>
          <h2 className="section__title">Ce que fait Bridge</h2>
        </div>

        <div className="features__list">
          <article className="feature">
            <h3 className="feature__title">Le matching par traits</h3>
            <p className="feature__body">
              Une offre nomme les traits qu'elle cherche, chacun obligatoire ou simple atout. Elle
              entre dans votre fil quand vous détenez tous ses traits obligatoires et le niveau
              d'études demandé. Les atouts comptent en votre faveur sans rien conditionner.
            </p>

            {sample && (
              <div className="feature__sample">
                <p className="feature__sample-head">
                  Ce que demande <Link to={`/emplois/${sample.id}`}>{sample.title}</Link>
                </p>
                <Row label="Obligatoires" traits={required} kind="required" />
                <Row label="Atouts" traits={plus} kind="plus" />
              </div>
            )}
          </article>

          <article className="feature">
            <h3 className="feature__title">Une candidature, un dossier</h3>
            <p className="feature__body">
              Vous gardez vos CV dans votre profil et choisissez celui que vous envoyez à chaque
              offre. La copie reçue par le recruteur est celle que vous avez jointe ce jour là,
              même si vous remplacez le document ensuite.
            </p>
          </article>

          <article className="feature">
            <h3 className="feature__title">Le suivi, du dépôt à la décision</h3>
            <p className="feature__body">
              Chaque candidature porte un état, visible à tout moment avec ce qu'il signifie pour
              vous. Rien n'est effacé : une candidature refusée garde son dossier complet, et vous
              pouvez repostuler à l'offre.
            </p>
          </article>
        </div>
      </div>
    </section>
  );
}
