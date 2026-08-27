import { Navigate, useLocation } from 'react-router-dom';
import Skeleton from '../Skeleton/Skeleton.jsx';
import { useAuth } from '../../context/AuthContext.jsx';
import './ProtectedRoute.css';

export const HOME_BY_ROLE = {
  CANDIDAT: '/offres',
  RH: '/candidatures',
  EXPERT: '/evaluations',
};

export const CHANGE_PASSWORD = '/mot-de-passe';

/**
 * Gates a route on authentication and, optionally, on role. The attempted
 * location travels with the redirect, so signing in lands where it was headed.
 * An account still owing its first password change reaches nothing but that
 * change.
 */
export default function ProtectedRoute({ roles, children }) {
  const { user, loading, expired } = useAuth();
  const location = useLocation();

  if (loading) {
    return (
      <div className="booting">
        <Skeleton variant="page" label="Vérification de votre session" />
      </div>
    );
  }

  if (!user) {
    return <Navigate to="/connexion" state={{ from: location, expired }} replace />;
  }

  if (user.mustChangePassword && location.pathname !== CHANGE_PASSWORD) {
    return <Navigate to={CHANGE_PASSWORD} replace />;
  }

  if (roles && !roles.includes(user.role)) {
    return <Navigate to={HOME_BY_ROLE[user.role] ?? '/'} replace />;
  }

  return children;
}
