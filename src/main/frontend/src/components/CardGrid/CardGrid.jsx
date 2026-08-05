import './CardGrid.css';

/**
 * Responsive listing of item cards.
 *
 * `size` names how much room a card needs before the grid drops a column:
 * 'default' for an ordinary item, 'wide' for one carrying extra detail, and
 * 'full' for one carrying a funnel rail.
 */
export default function CardGrid({ size = 'default', label, children }) {
  return (
    <ul className={`cardgrid cardgrid--${size}`} aria-label={label}>
      {children}
    </ul>
  );
}
