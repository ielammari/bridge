import { useEffect, useRef } from 'react';
import { Link, NavLink, useLocation } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext.jsx';
import { useNotifications } from '../../context/NotificationContext.jsx';
import Button from '../../components/Button/Button.jsx';
import Icon from '../../components/Icon/Icon.jsx';
import useDocumentTitle from '../../hooks/useDocumentTitle.js';
import './workspace.css';

const NAV_BY_ROLE = {
  CANDIDAT: [
    { to: '/offres', label: 'Offres' },
    { to: '/mes-candidatures', label: 'Mes candidatures' },
    { to: '/profil', label: 'Mon profil' },
  ],
  RH: [
    { to: '/offres', label: 'Offres' },
    { to: '/candidatures', label: 'Candidatures' },
  ],
  EXPERT: [{ to: '/evaluations', label: 'Évaluations' }],
};

const ROLE_LABELS = { CANDIDAT: 'Candidat', RH: 'RH', EXPERT: 'Expert' };

/**
 * Signed in shell shared by the three workspaces: brand, navigation for the
 * current role, and the account controls. Also names the page in the tab and
 * moves focus on navigation, which a single page application must do by hand.
 */
export default function Workspace({ title, subtitle, back, children }) {
  const { user, logout } = useAuth();
  const { unreadCount } = useNotifications();
  const location = useLocation();
  const heading = useRef(null);

  useDocumentTitle(title);

  // Focus moves to the new page's heading, so the next Tab starts from its
  // content rather than the link that was clicked.
  useEffect(() => {
    heading.current?.focus();
  }, [location.pathname]);

  const nav = NAV_BY_ROLE[user.role] ?? [];

  return (
    <div className="workspace">
      <a className="workspace__skip" href="#contenu">Aller au contenu</a>

      <header className="workspace__bar">
        <div className="workspace__lead">
          <span className="workspace__logo">Bridge</span>
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
            <NavLink
              to="/messages"
              className={({ isActive }) =>
                `workspace__link${isActive ? ' workspace__link--active' : ''}`
              }
            >
              Messages
              {unreadCount > 0 && (
                <span className="workspace__badge" aria-label={`${unreadCount} non lus`}>
                  {unreadCount > 9 ? '9+' : unreadCount}
                </span>
              )}
            </NavLink>
          </nav>
        </div>

        <div className="workspace__account">
          <span className="workspace__identity">
            {user.firstName} {user.lastName}
            <span className="workspace__role">{ROLE_LABELS[user.role] ?? user.role}</span>
          </span>
          <Button variant="text" onClick={logout}>
            Se déconnecter
          </Button>
        </div>
      </header>

      <main className="workspace__content" id="contenu">
        <div className="workspace__head">
          {back && (
            <Link className="workspace__back" to={back.to}>
              <Icon name="chevron" className="workspace__back-icon" /> {back.label}
            </Link>
          )}
          <h1 className="workspace__title" ref={heading} tabIndex={-1}>{title}</h1>
          {subtitle && <p className="workspace__subtitle">{subtitle}</p>}
        </div>

        {children}
      </main>
    </div>
  );
}
