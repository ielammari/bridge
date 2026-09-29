import { Link } from 'react-router-dom';
import art from '../../assets/hero.webp';
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
 * what is open right now, counted from the offers themselves. The bridge stands
 * beside it, drifting.
 */
export default function Hero({ offers, domains, ready }) {
  const { user, loading } = useAuth();

  return (
    <section className="pubband hero">
      <div className="pubband__inner hero__inner">
        <div className="hero__words">
          <h1 className="hero__title">Postulez là où votre profil compte.</h1>

          <p className="hero__lead">
            La plateforme de recrutement de l'entreprise, de l'offre à l'embauche.
          </p>

          <div className="hero__actions">
            <Link to="/emplois" className="hero__go">Voir les offres</Link>
            {!loading && (
              user
                ? <Link to={landingFor(user)} className="hero__alt">Ouvrir l'application</Link>
                : <Link to="/inscription" className="hero__alt">Créer un compte</Link>
            )}
          </div>
        </div>

        <img src={art} alt="" className="hero__art" width={1029} height={675} />

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
