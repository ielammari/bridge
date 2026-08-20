import './CardGrid.css';

/**
 * Responsive listing of item cards. `size` sets how much room a card needs
 * before the grid drops a column: 'default', 'wide', or 'full' for a card
 * carrying a funnel rail.
 */
export default function CardGrid({ size = 'default', label, children }) {
  return (
    <ul className={`cardgrid cardgrid--${size}`} aria-label={label}>
      {children}
    </ul>
  );
}
