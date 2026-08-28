import { Link, NavLink } from 'react-router-dom';
import ThemeToggle from '../ThemeToggle/ThemeToggle.jsx';
import { landingFor } from '../ProtectedRoute/ProtectedRoute.jsx';
import { useAuth } from '../../context/AuthContext.jsx';
import './PublicChrome.css';

/**
 * The public site's navigation, on the same dark slab as the application's own
 * rail. One control carries the way in, and what it says depends on whether
 * there is already a session: a visitor signs in, an account opens its
 * workspace at whatever step it owes.
 */
export default function PublicHeader() {
  const { user, loading } = useAuth();

  return (
    <header className="pubhead">
      <div className="pubhead__inner">
        <Link to="/" className="pubhead__brand">Bridge</Link>

        <nav className="pubhead__nav" aria-label="Navigation du site">
          <Link to="/#fonctionnalites" className="pubhead__link">Fonctionnalités</Link>
          <NavLink
            to="/emplois"
            className={({ isActive }) => `pubhead__link${isActive ? ' pubhead__link--current' : ''}`}
          >
            Offres
          </NavLink>
        </nav>

        <div className="pubhead__actions">
          <ThemeToggle />
          {!loading && (
            user
              ? <Link to={landingFor(user)} className="pubhead__enter">Ouvrir l'application</Link>
              : <Link to="/connexion" className="pubhead__enter">Se connecter</Link>
          )}
        </div>
      </div>
    </header>
  );
}
