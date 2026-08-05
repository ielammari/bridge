import { useEffect, useRef, useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import Icon from '../../components/Icon/Icon.jsx';
import Sidebar from '../../components/Sidebar/Sidebar.jsx';
import { useNotifications } from '../../context/NotificationContext.jsx';
import useDocumentTitle from '../../hooks/useDocumentTitle.js';
import useMediaQuery from '../../hooks/useMediaQuery.js';
import './workspace.css';

const COLLAPSED_KEY = 'bridge.sidebar.collapsed';

/**
 * Signed in shell: the sidebar, and the page it frames. Also names the page in
 * the tab and moves focus on navigation, which a single page application must
 * do by hand.
 *
 * `width` is 'wide' for listings, which want room for a card grid, and 'narrow'
 * for reading and forms, which want a readable line length instead.
 */
export default function Workspace({ title, subtitle, back, width = 'wide', children }) {
  const { unreadCount } = useNotifications();
  const location = useLocation();
  const heading = useRef(null);
  const rail = useRef(null);
  const menuButton = useRef(null);

  // Below this width the rail is a drawer rather than a fixture, which changes
  // whether it may hold focus at all.
  const isDrawer = useMediaQuery('(max-width: 64rem)');

  const [collapsed, setCollapsed] = useState(
    () => localStorage.getItem(COLLAPSED_KEY) === 'true',
  );
  const [drawerOpen, setDrawerOpen] = useState(false);

  useDocumentTitle(title);

  useEffect(() => {
    heading.current?.focus();
  }, [location.pathname]);

  // An open drawer covers the page, so Tab has to stay inside it, and closing
  // returns focus to the control that opened it.
  useEffect(() => {
    if (!drawerOpen) return undefined;

    const focusable = () =>
      [...(rail.current?.querySelectorAll('a[href], button:not([disabled])') ?? [])];
    focusable()[0]?.focus();

    function onKeyDown(event) {
      if (event.key === 'Escape') {
        setDrawerOpen(false);
        return;
      }
      if (event.key !== 'Tab') return;

      const items = focusable();
      if (items.length === 0) return;
      const first = items[0];
      const last = items[items.length - 1];

      if (event.shiftKey && document.activeElement === first) {
        event.preventDefault();
        last.focus();
      } else if (!event.shiftKey && document.activeElement === last) {
        event.preventDefault();
        first.focus();
      }
    }

    window.addEventListener('keydown', onKeyDown);
    return () => {
      window.removeEventListener('keydown', onKeyDown);
      menuButton.current?.focus();
    };
  }, [drawerOpen]);

  function toggleCollapsed() {
    setCollapsed((current) => {
      localStorage.setItem(COLLAPSED_KEY, String(!current));
      return !current;
    });
  }

  return (
    <div className={`workspace workspace--${width}${collapsed ? ' workspace--collapsed' : ''}`}>
      <a className="workspace__skip" href="#contenu">Aller au contenu</a>

      {/* Below the sidebar breakpoint the navigation becomes a drawer, so the
          wordmark and the unread count need somewhere else to live. */}
      <header className="workspace__mobilebar">
        <button
          type="button"
          className="workspace__menu"
          ref={menuButton}
          onClick={() => setDrawerOpen(true)}
          aria-expanded={drawerOpen}
          aria-label="Ouvrir le menu"
        >
          <Icon name="menu" />
          {unreadCount > 0 && <span className="workspace__menu-dot" aria-hidden="true" />}
        </button>
        <span className="workspace__logo">Bridge</span>
      </header>

      {/* A drawer that is merely translated off screen still holds its links in
          the tab order, so it is made inert while it is closed. */}
      <div
        className={`workspace__rail${drawerOpen ? ' workspace__rail--open' : ''}`}
        ref={rail}
        inert={isDrawer && !drawerOpen}
      >
        <Sidebar
          collapsed={collapsed}
          onToggle={toggleCollapsed}
          onNavigate={() => setDrawerOpen(false)}
        />
      </div>

      {drawerOpen && (
        <button
          type="button"
          className="workspace__scrim"
          onClick={() => setDrawerOpen(false)}
          aria-label="Fermer le menu"
        />
      )}

      <main className="workspace__content" id="contenu">
        <div className="workspace__inner">
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
        </div>
      </main>
    </div>
  );
}
