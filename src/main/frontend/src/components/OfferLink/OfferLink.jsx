import { Link, useLocation } from 'react-router-dom';
import '../PersonLink/PersonLink.css';

/**
 * An offer's title, wherever it appears, as the way to open it. `to` names the
 * address when the reader is not inside the application. Shares PersonLink's
 * styling: a name and a title are one kind of link.
 */
export default function OfferLink({ id, to, className, children }) {
  const location = useLocation();

  if (!id) return children;

  return (
    <Link
      className={`personlink${className ? ` ${className}` : ''}`}
      to={to ?? `/offres/${id}`}
      state={{ from: `${location.pathname}${location.search}` }}
    >
      {children}
    </Link>
  );
}
