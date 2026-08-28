import { Link } from 'react-router-dom';
import { landingFor } from '../../components/ProtectedRoute/ProtectedRoute.jsx';
import { useAuth } from '../../context/AuthContext.jsx';

/** One live figure and what it counts. */
function Figure({ value, one, many }) {
  return (
    <p className="hero__figure">
      <span className="hero__value mono">{value}</span>
      <span className="hero__label">{value > 1 ? many : one}</span>
    </p>
  );
}

/**
 * The first screen: what the application does, the two ways on, and the size of
 * what is open right now, counted from the offers themselves.
 */
export default function Hero({ offers, domains, ready }) {
  const { user, loading } = useAuth();

  return (
    <section className="pubband hero">
      <div className="pubband__inner hero__inner">
        <h1 className="hero__title">Postuler, et savoir où vous en êtes.</h1>

        <p className="hero__lead">
          Bridge publie les postes ouverts de l'entreprise, vous montre ceux dont vous remplissez
          les conditions, et suit votre candidature de sa réception à la décision.
        </p>

        <div className="hero__actions">
          <Link to="/emplois" className="hero__go">Voir les offres</Link>
          {!loading && (
            user
              ? <Link to={landingFor(user)} className="hero__alt">Ouvrir l'application</Link>
              : <Link to="/inscription" className="hero__alt">Créer un compte</Link>
          )}
        </div>

        {ready && (
          <div className="hero__figures">
            <Figure value={offers} one="poste ouvert" many="postes ouverts" />
            <Figure value={domains} one="domaine" many="domaines" />
          </div>
        )}
      </div>
    </section>
  );
}
