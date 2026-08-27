import { useEffect, useRef } from 'react';
import Icon from '../../components/Icon/Icon.jsx';
import { fullDay } from '../../constants/calendar.js';

/**
 * The day a cell opens, in a panel at the right edge. It carries no scrim: the
 * month behind it stays readable and operable, so it is dismissed rather than
 * escaped. It enters and leaves along the same edge, and returns focus to
 * whatever opened it.
 */
export default function DayDrawer({ open, iso, subtitle, onClose, children }) {
  const panel = useRef(null);
  const heading = useRef(null);
  const opener = useRef(null);

  useEffect(() => {
    if (open) {
      opener.current = document.activeElement;
      heading.current?.focus();
      return undefined;
    }
    // Focus goes back only when it is still inside the panel that is closing.
    if (opener.current && panel.current?.contains(document.activeElement)) {
      opener.current.focus();
    }
    return undefined;
  }, [open]);

  useEffect(() => {
    if (!open) return undefined;
    function onKeyDown(event) {
      if (event.key === 'Escape') onClose();
    }
    window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
  }, [open, onClose]);

  return (
    <aside
      ref={panel}
      className={`daydrawer${open ? ' daydrawer--open' : ''}`}
      aria-label="Journée"
      inert={!open}
    >
      <header className="daydrawer__head">
        <div className="daydrawer__title">
          <h2 className="daydrawer__day" ref={heading} tabIndex={-1}>
            {iso ? fullDay(iso) : ''}
          </h2>
          {subtitle && <p className="daydrawer__count mono">{subtitle}</p>}
        </div>
        <button type="button" className="daydrawer__close" onClick={onClose}
          aria-label="Fermer" title="Fermer">
          <Icon name="close" />
        </button>
      </header>

      <div className="daydrawer__body">{children}</div>
    </aside>
  );
}
