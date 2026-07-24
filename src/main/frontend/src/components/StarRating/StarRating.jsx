import './StarRating.css';

// Scores are half star units from 0 to 10; the widget shows them as 5 stars.
const POSITIONS = [0, 1, 2, 3, 4];

function Star({ className }) {
  return (
    <svg className={className} viewBox="0 0 24 24" width="1em" height="1em" aria-hidden="true" focusable="false">
      <path d="M12 2.5l2.9 5.9 6.5.9-4.7 4.6 1.1 6.5L12 17.9 6.2 20.9l1.1-6.5L2.6 9.3l6.5-.9z"
        fill="currentColor" />
    </svg>
  );
}

/**
 * Half star rating. The value is in half star units (0 to 10) but it is
 * announced and shown in stars (0 to 5), and never conveyed by fill alone: a
 * numeric readout sits beside it.
 */
export default function StarRating({ value = 0, onChange, readOnly = false, label = 'Note' }) {
  const stars = value / 2;
  const editable = !readOnly && typeof onChange === 'function';

  function set(units) {
    if (!editable) return;
    // Clicking the current value clears it back to zero.
    onChange(units === value ? 0 : units);
  }

  function onKeyDown(event) {
    if (!editable) return;
    let next = value;
    switch (event.key) {
      case 'ArrowRight':
      case 'ArrowUp':
        next = Math.min(10, value + 1);
        break;
      case 'ArrowLeft':
      case 'ArrowDown':
        next = Math.max(0, value - 1);
        break;
      case 'Home':
        next = 0;
        break;
      case 'End':
        next = 10;
        break;
      default:
        return;
    }
    event.preventDefault();
    onChange(next);
  }

  return (
    <span className="stars">
      <span
        className={`stars__row${editable ? ' stars__row--editable' : ''}`}
        role={editable ? 'slider' : 'img'}
        tabIndex={editable ? 0 : undefined}
        aria-label={`${label} : ${format(stars)} sur 5`}
        aria-valuemin={editable ? 0 : undefined}
        aria-valuemax={editable ? 5 : undefined}
        aria-valuenow={editable ? stars : undefined}
        aria-valuetext={editable ? `${format(stars)} sur 5` : undefined}
        onKeyDown={onKeyDown}
      >
        {POSITIONS.map((i) => {
          const fill = Math.max(0, Math.min(1, stars - i));
          return (
            <span key={i} className="stars__star">
              <Star className="stars__bg" />
              <span className="stars__fill" style={{ width: `${fill * 100}%` }}>
                <Star className="stars__fg" />
              </span>
              {editable && (
                <>
                  <button type="button" tabIndex={-1} aria-hidden="true"
                    className="stars__hit stars__hit--left" onClick={() => set(i * 2 + 1)} />
                  <button type="button" tabIndex={-1} aria-hidden="true"
                    className="stars__hit stars__hit--right" onClick={() => set(i * 2 + 2)} />
                </>
              )}
            </span>
          );
        })}
      </span>
      <span className="stars__value mono">{format(stars)}</span>
    </span>
  );
}

function format(stars) {
  return stars.toString().replace('.', ',');
}
