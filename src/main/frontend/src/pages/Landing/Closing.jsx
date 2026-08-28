import { Link } from 'react-router-dom';
import { landingFor } from '../../components/ProtectedRoute/ProtectedRoute.jsx';
import { useAuth } from '../../context/AuthContext.jsx';

/**
 * The foot of the argument. Reading the offers needed nothing; applying needs
 * an account, which is what this asks for.
 */
export default function Closing() {
  const { user, loading } = useAuth();

  return (
    <section className="pubband closing">
      <div className="pubband__inner closing__inner">
        <div className="closing__words">
          <h2 className="closing__title">Prêt à postuler ?</h2>
          <p className="closing__lead">
            Lire les offres ne demande rien. Postuler demande un compte, un profil de traits et un
            CV.
          </p>
        </div>

        <div className="closing__actions">
          {!loading && (
            user
              ? <Link to={landingFor(user)} className="hero__go">Ouvrir l'application</Link>
              : <Link to="/inscription" className="hero__go">Créer un compte</Link>
          )}
          <Link to="/emplois" className="closing__alt">Voir les offres</Link>
        </div>
      </div>
    </section>
  );
}
