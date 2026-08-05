import { Link, useLocation } from 'react-router-dom';
import './PersonLink.css';

/**
 * A person's name, wherever it appears, as the way to open them.
 *
 * Carries the current view along so the profile can come back to it, and falls
 * back to plain text when there is no id to link to, so a caller never has to
 * decide whether a name is clickable this time.
 */
export default function PersonLink({ id, className, children }) {
  const location = useLocation();

  if (!id) return children;

  return (
    <Link
      className={`personlink${className ? ` ${className}` : ''}`}
      to={`/personnes/${id}`}
      state={{ from: `${location.pathname}${location.search}` }}
    >
      {children}
    </Link>
  );
}
