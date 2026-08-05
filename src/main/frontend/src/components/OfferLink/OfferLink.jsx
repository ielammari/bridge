import { Link, useLocation } from 'react-router-dom';
import '../PersonLink/PersonLink.css';

/**
 * An offer's title, wherever it appears, as the way to open it.
 *
 * Shares PersonLink's styling: a name and a title behave the same way in a
 * listing, so they should not look like two kinds of link.
 */
export default function OfferLink({ id, className, children }) {
  const location = useLocation();

  if (!id) return children;

  return (
    <Link
      className={`personlink${className ? ` ${className}` : ''}`}
      to={`/offres/${id}`}
      state={{ from: `${location.pathname}${location.search}` }}
    >
      {children}
    </Link>
  );
}
