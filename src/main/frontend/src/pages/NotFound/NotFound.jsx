import { Link } from 'react-router-dom';
import { HOME_BY_ROLE } from '../../components/ProtectedRoute/ProtectedRoute.jsx';
import { useAuth } from '../../context/AuthContext.jsx';
import useDocumentTitle from '../../hooks/useDocumentTitle.js';
import './notFound.css';

/** Names a mistyped address, instead of silently redirecting. */
export default function NotFound() {
  useDocumentTitle('Page introuvable');
  const { user } = useAuth();
  const home = user ? HOME_BY_ROLE[user.role] ?? '/' : '/connexion';

  return (
    <main className="notfound">
      <div className="notfound__inner">
        <p className="notfound__code mono">404</p>
        <h1 className="notfound__title">Cette page n'existe pas.</h1>
        <p className="notfound__body">
          L'adresse est peut-être incomplète, ou la page a été retirée depuis le lien que vous avez
          suivi.
        </p>
        <Link className="notfound__link" to={home}>
          {user ? 'Retour à mon espace' : 'Aller à la connexion'}
        </Link>
      </div>
    </main>
  );
}
