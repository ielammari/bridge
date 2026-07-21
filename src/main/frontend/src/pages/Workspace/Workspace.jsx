import { NavLink } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext.jsx';
import Button from '../../components/Button/Button.jsx';
import './workspace.css';

const NAV_BY_ROLE = {
  CANDIDAT: [
    { to: '/offres', label: 'Offres' },
    { to: '/profil', label: 'Mon profil' },
  ],
  RH: [{ to: '/candidatures', label: 'Candidatures' }],
  EXPERT: [{ to: '/evaluations', label: 'Évaluations' }],
};

/**
 * Signed in shell shared by the three workspaces: brand, primary navigation for
 * the current role, and the account controls.
 */
export default function Workspace({ title, children }) {
  const { user, logout } = useAuth();
  const nav = NAV_BY_ROLE[user.role] ?? [];

  return (
    <div className="workspace">
      <header className="workspace__bar">
        <div className="workspace__lead">
          <span className="workspace__logo">Bridge</span>
          {nav.length > 0 && (
            <nav className="workspace__nav" aria-label="Navigation principale">
              {nav.map((item) => (
                <NavLink
                  key={item.to}
                  to={item.to}
                  className={({ isActive }) =>
                    `workspace__link${isActive ? ' workspace__link--active' : ''}`
                  }
                >
                  {item.label}
                </NavLink>
              ))}
            </nav>
          )}
        </div>

        <div className="workspace__account">
          <span className="workspace__identity">
            {user.firstName} {user.lastName}
            <span className="workspace__role">{user.role}</span>
          </span>
          <Button variant="text" onClick={logout}>
            Se déconnecter
          </Button>
        </div>
      </header>

      <main className="workspace__content">
        <h1>{title}</h1>
        {children}
      </main>
    </div>
  );
}
