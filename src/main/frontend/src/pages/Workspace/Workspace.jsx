import { useEffect, useRef, useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import Icon from '../../components/Icon/Icon.jsx';
import InfoHint from '../../components/InfoHint/InfoHint.jsx';
import Sidebar from '../../components/Sidebar/Sidebar.jsx';
import '../../components/PersonLink/PersonLink.css';
import { useNotifications } from '../../context/NotificationContext.jsx';
import useDocumentTitle from '../../hooks/useDocumentTitle.js';
import useMediaQuery from '../../hooks/useMediaQuery.js';
import './workspace.css';

const COLLAPSED_KEY = 'bridge.sidebar.collapsed';

// The stretch of the page the header's change of shape spans: full shape until
// the first mark, fully condensed at the second.
const CONDENSE_FROM = 45;
const CONDENSE_TO = 200;
// How closely the shape follows the scroll: the time it takes to cover most of
// the distance to where the page is.
const FOLLOW = 90;

// Near the top on the way up, the page finishes the climb itself: the band it
// does so from, how long the scroll must have been still first, and how far
// down the page must go before it may take over again.
const CLIMB_BAND = 30;
const CLIMB_REST = 235;
const CLIMB_REARM = 100;
// Scroll positions are fractional on a scaled display, so the band's edge is
// compared with a pixel's tolerance.
const EDGE = 0.5;

/**
 * Signed in shell: the sidebar, and the page it frames. Names the page in the
 * tab and moves focus on navigation.
 *
 * `width` is 'wide' for listings and 'narrow' for reading and forms, `info`
 * carries what the page cannot say for itself, and `stats` and `action` fill
 * the header band. `returnTo` is the way back out of a page reached from
 * elsewhere, and holds its shape while the header condenses, since it is how
 * the reader leaves. `titleTo` makes the title the way into what it names.
 * `toolbar` holds what steers the content (tabs, filters, a picker) and stays
 * with the header, which holds the top of the screen and condenses on scroll.
 * `panelOpen` says a side panel is out, so the page gives up its width to it.
 */
export default function Workspace({
  title, titleTo, subtitle, info, back, stats = [], action, returnTo, toolbar,
  width = 'wide', panelOpen = false, children,
}) {
  const { unreadCount } = useNotifications();
  const location = useLocation();
  const heading = useRef(null);
  const rail = useRef(null);
  const menuButton = useRef(null);
  const header = useRef(null);
  const shell = useRef(null);

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

  // The header's shape answers the scroll, and near the top the page finishes
  // the climb itself. Both read the same movement.
  useEffect(() => {
    const root = shell.current;
    const reduced = window.matchMedia('(prefers-reduced-motion: reduce)');

    function shapeAt(y) {
      return Math.min(1, Math.max(0, (y - CONDENSE_FROM) / (CONDENSE_TO - CONDENSE_FROM)));
    }

    let shape = shapeAt(window.scrollY);
    let previous = window.scrollY;
    let armed = window.scrollY > CLIMB_REARM;
    let frame = 0;
    let last = 0;
    let rest = 0;
    let aimed = 0;

    function paint() {
      root.style.setProperty('--condense', shape.toFixed(3));
    }

    // Each frame closes the same fraction of the distance left between the
    // shape and the scroll, so a fast scroll lands smoothly.
    function step(now) {
      const goal = shapeAt(window.scrollY);
      shape += (goal - shape) * (1 - Math.exp(-Math.min(64, now - last) / FOLLOW));
      last = now;

      if (Math.abs(goal - shape) < 0.001) {
        shape = goal;
        frame = 0;
      } else {
        frame = requestAnimationFrame(step);
      }
      paint();
    }

    function follow() {
      if (reduced.matches) {
        shape = shapeAt(window.scrollY) >= 0.5 ? 1 : 0;
        paint();
        return;
      }
      if (frame) return;
      last = performance.now();
      frame = requestAnimationFrame(step);
    }

    function climb() {
      armed = false;
      window.scrollTo({ top: 0, behavior: reduced.matches ? 'auto' : 'smooth' });
    }

    function onScroll() {
      const y = window.scrollY;
      const up = y < previous - EDGE;
      previous = y;
      follow();

      // The climb takes over once per approach; going back down restores it.
      if (y > CLIMB_REARM) armed = true;

      clearTimeout(rest);
      if (armed && up && y > 0 && y <= CLIMB_BAND + EDGE && Date.now() >= aimed) {
        rest = window.setTimeout(climb, CLIMB_REST);
      }
    }

    // A link to a section moves the page on purpose, so the climb leaves it
    // where it aimed.
    function onLink(event) {
      if (event.target.closest?.('a[href^="#"]')) aimed = Date.now() + 600;
    }

    paint();
    window.addEventListener('scroll', onScroll, { passive: true });
    document.addEventListener('click', onLink, true);
    return () => {
      cancelAnimationFrame(frame);
      clearTimeout(rest);
      window.removeEventListener('scroll', onScroll);
      document.removeEventListener('click', onLink, true);
    };
  }, []);

  // What the header occupies, published so the section rail and anchored
  // sections can clear it rather than sliding under it.
  useEffect(() => {
    const node = header.current;
    if (!node) return undefined;

    const publish = () =>
      node.closest('.workspace')?.style.setProperty('--header-h', `${node.offsetHeight}px`);
    publish();

    const observer = new ResizeObserver(publish);
    observer.observe(node);
    return () => observer.disconnect();
    // The observer covers a reflow; the deps cover the header's own content
    // arriving.
  }, [toolbar, title, subtitle, stats.length, action, returnTo, back]);

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
    <div
      className={`workspace workspace--${width}${collapsed ? ' workspace--collapsed' : ''}`
        + `${panelOpen ? ' workspace--panelled' : ''}`}
      ref={shell}
    >
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
        <Link to="/" className="workspace__logo">Bridge</Link>
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
          <div className="workspace__header" ref={header}>
            {back && (
              <Link className="workspace__back" to={back.to}>
                <Icon name="chevron" className="workspace__back-icon" /> {back.label}
              </Link>
            )}

            <header className="workspace__band">
              <div className="workspace__head">
                <h1 className="workspace__title" ref={heading} tabIndex={-1}>
                  {titleTo
                    ? (
                      <Link className="personlink" to={titleTo}
                        state={{ from: `${location.pathname}${location.search}` }}>
                        {title}
                      </Link>
                    )
                    : title}
                  {info && <InfoHint label={`À propos de ${title}`}>{info}</InfoHint>}
                </h1>
                {subtitle && (
                  <div className="workspace__fold">
                    <p className="workspace__subtitle">{subtitle}</p>
                  </div>
                )}
              </div>

              {(stats.length > 0 || action || returnTo) && (
                <div className="workspace__aside">
                  {stats.length > 0 && (
                    <div className="workspace__fold workspace__fold--inline">
                      <dl className="workspace__stats">
                        {stats.map((stat) => (
                          <div key={stat.label} className="workspace__stat">
                            <dt className="workspace__stat-value">{stat.value}</dt>
                            <dd className="workspace__stat-label">{stat.label}</dd>
                          </div>
                        ))}
                      </dl>
                    </div>
                  )}
                  {action}
                  {returnTo && (
                    <Link className="workspace__return" to={returnTo.to}>
                      <Icon name="chevron" className="workspace__back-icon" /> {returnTo.label}
                    </Link>
                  )}
                </div>
              )}
            </header>

            {toolbar && <div className="workspace__toolbar">{toolbar}</div>}
          </div>

          {children}
        </div>
      </main>
    </div>
  );
}
