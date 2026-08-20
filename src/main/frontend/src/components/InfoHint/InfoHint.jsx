import { useCallback, useEffect, useRef } from 'react';
import Icon from '../Icon/Icon.jsx';
import './InfoHint.css';

// Clearance kept between the bubble and both the mark and the viewport edge.
const GAP = 8;
const MARGIN = 12;

/**
 * Explains a control without spending page space on prose. The bubble is a
 * popover, so nothing can clip it, and its position is measured: above the mark
 * where there is room, below where there is not, held inside the viewport.
 */
export default function InfoHint({ label, children }) {
  const trigger = useRef(null);
  const bubble = useRef(null);

  const place = useCallback(() => {
    const mark = trigger.current;
    const tip = bubble.current;
    if (!mark || !tip || !tip.matches(':popover-open')) return;

    const from = mark.getBoundingClientRect();
    const size = tip.getBoundingClientRect();

    const above = from.top - size.height - GAP;
    const below = from.bottom + GAP;
    const flipped = above < MARGIN && below + size.height <= window.innerHeight - MARGIN;

    const left = Math.min(
      Math.max(from.left + from.width / 2 - size.width / 2, MARGIN),
      window.innerWidth - size.width - MARGIN,
    );

    tip.classList.toggle('infohint__bubble--below', flipped);
    tip.style.top = `${Math.max(flipped ? below : above, MARGIN)}px`;
    tip.style.left = `${Math.max(left, MARGIN)}px`;
  }, []);

  const show = useCallback(() => {
    const tip = bubble.current;
    if (!tip || tip.matches(':popover-open')) return;
    tip.showPopover();
    place();
    // Placed while still transparent, so the first painted frame is already in
    // the right spot and the bubble grows from there.
    requestAnimationFrame(() => tip.classList.add('infohint__bubble--open'));
  }, [place]);

  const hide = useCallback(() => {
    const tip = bubble.current;
    if (!tip || !tip.matches(':popover-open')) return;
    tip.classList.remove('infohint__bubble--open');
    tip.hidePopover();
  }, []);

  useEffect(() => {
    const onScroll = () => place();
    window.addEventListener('scroll', onScroll, true);
    window.addEventListener('resize', onScroll);
    return () => {
      window.removeEventListener('scroll', onScroll, true);
      window.removeEventListener('resize', onScroll);
    };
  }, [place]);

  return (
    <span className="infohint">
      <button
        type="button"
        className="infohint__trigger"
        ref={trigger}
        aria-label={label}
        onPointerEnter={show}
        onPointerLeave={hide}
        onFocus={show}
        onBlur={hide}
        onKeyDown={(event) => event.key === 'Escape' && hide()}
      >
        <Icon name="info" />
      </button>
      <span className="infohint__bubble" ref={bubble} popover="manual" role="tooltip">
        {children}
      </span>
    </span>
  );
}
