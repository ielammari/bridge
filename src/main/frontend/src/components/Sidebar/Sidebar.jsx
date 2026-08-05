import { NavLink } from 'react-router-dom';
import Icon from '../Icon/Icon.jsx';
import { useAuth } from '../../context/AuthContext.jsx';
import { useNotifications } from '../../context/NotificationContext.jsx';
import './Sidebar.css';

// Active work first, then the record of what is finished.
const NAV_BY_ROLE = {
  CANDIDAT: [
    { to: '/offres', label: 'Offres', icon: 'briefcase' },
    { to: '/mes-candidatures', label: 'Mes candidatures', icon: 'stack' },
    { to: '/historique/candidatures', label: 'Historique', icon: 'archive' },
  ],
  RH: [
    { to: '/offres', label: 'Offres', icon: 'briefcase' },
    { to: '/candidatures', label: 'Candidatures', icon: 'stack' },
    { to: '/historique/candidatures', label: 'Historique', icon: 'archive' },
  ],
  EXPERT: [
    { to: '/evaluations', label: 'Évaluations', icon: 'star' },
    { to: '/historique/evaluations', label: 'Historique', icon: 'archive' },
  ],
};

const ROLE_LABELS = { CANDIDAT: 'Candidat', RH: 'RH', EXPERT: 'Expert' };

/**
 * Persistent primary navigation. Routes at the top, the account and its
 * settings at the bottom, and the two groups never mix.
 *
 * Every row shares one geometry, with the icon at a fixed offset that is also
 * its centre once the panel is collapsed. Collapsing then moves nothing but the
 * panel edge: the labels only fade.
 */
export default function Sidebar({ collapsed, onToggle, onNavigate }) {
  const { user, logout } = useAuth();
  const { unreadCount } = useNotifications();

  const routes = NAV_BY_ROLE[user.role] ?? [];
  const isCandidate = user.role === 'CANDIDAT';

  // With the labels hidden there is nothing left to name the row.
  const hint = (label) => (collapsed ? label : undefined);

  function row({ to, label, icon, badge }) {
    return (
      <NavLink
        key={to}
        to={to}
        className={({ isActive }) => `sidebar__row${isActive ? ' sidebar__row--active' : ''}`}
        title={hint(label)}
        aria-label={hint(label)}
        onClick={onNavigate}
      >
        <Icon name={icon} className="sidebar__icon" />
        <span className="sidebar__label">{label}</span>
        {badge > 0 && (
          <span className="sidebar__badge" aria-label={`${badge} non lus`}>
            {badge > 9 ? '9+' : badge}
          </span>
        )}
      </NavLink>
    );
  }

  const identity = (
    <>
      <Icon name="user" className="sidebar__icon" />
      <span className="sidebar__label sidebar__identity">
        <span className="sidebar__name">{user.firstName} {user.lastName}</span>
        <span className="sidebar__role">{ROLE_LABELS[user.role] ?? user.role}</span>
      </span>
    </>
  );

  return (
    <aside className={`sidebar${collapsed ? ' sidebar--collapsed' : ''}`}>
      <div className="sidebar__top">
        {/* The wordmark and the mark share one cell, so collapsing reads as the
            word losing its tail rather than one logo swapping for another. */}
        <span className="sidebar__logo">
          <span className="sidebar__logo-full">Bridge</span>
          <span className="sidebar__logo-mark" aria-hidden="true">B</span>
        </span>
      </div>

      <button
        type="button"
        className="sidebar__toggle"
        onClick={onToggle}
        aria-expanded={!collapsed}
        aria-label={collapsed ? 'Déplier le menu' : 'Replier le menu'}
        title={collapsed ? 'Déplier le menu' : 'Replier le menu'}
      >
        <Icon name="chevron" className="sidebar__toggle-icon" />
      </button>

      <nav className="sidebar__nav" aria-label="Navigation principale">
        {routes.map(row)}
        {row({ to: '/messages', label: 'Messages', icon: 'bell', badge: unreadCount })}
      </nav>

      <div className="sidebar__foot">
        {/* The account row is the identity and, for a candidate, the way into
            their profile. */}
        {isCandidate ? (
          <NavLink
            to="/profil"
            className={({ isActive }) => `sidebar__row${isActive ? ' sidebar__row--active' : ''}`}
            title={hint('Mon profil')}
            aria-label={hint('Mon profil')}
            onClick={onNavigate}
          >
            {identity}
          </NavLink>
        ) : (
          <div className="sidebar__row sidebar__row--static">{identity}</div>
        )}

        {row({ to: '/parametres', label: 'Paramètres', icon: 'settings' })}

        <button
          type="button"
          className="sidebar__row sidebar__row--danger"
          onClick={logout}
          title={hint('Se déconnecter')}
          aria-label={hint('Se déconnecter')}
        >
          <Icon name="logout" className="sidebar__icon" />
          <span className="sidebar__label">Se déconnecter</span>
        </button>
      </div>
    </aside>
  );
}
