import { Navigate, useLocation } from 'react-router-dom';
import Skeleton from '../Skeleton/Skeleton.jsx';
import { useAuth } from '../../context/AuthContext.jsx';
import './ProtectedRoute.css';

export const HOME_BY_ROLE = {
  CANDIDAT: '/offres',
  RH: '/candidatures',
  EXPERT: '/evaluations',
};

/**
 * Gates a route on authentication and, optionally, on role.
 * The attempted location is carried along so the user lands where they were
 * headed after signing in, rather than on a generic home page.
 */
export default function ProtectedRoute({ roles, children }) {
  const { user, loading, expired } = useAuth();
  const location = useLocation();

  if (loading) {
    return (
      <div className="booting">
        <Skeleton count={2} label="Vérification de votre session" />
      </div>
    );
  }

  if (!user) {
    return <Navigate to="/connexion" state={{ from: location, expired }} replace />;
  }

  if (roles && !roles.includes(user.role)) {
    return <Navigate to={HOME_BY_ROLE[user.role] ?? '/'} replace />;
  }

  return children;
}
