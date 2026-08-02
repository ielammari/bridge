import { Link } from 'react-router-dom';
import Button from '../Button/Button.jsx';
import './EmptyState.css';

/** The one empty state in the application. Names the next action, not just the
 *  absence of content. */
export default function EmptyState({ title, children, actionLabel, actionTo, onAction }) {
  return (
    <div className="empty">
      <p className="empty__title">{title}</p>
      {children && <p className="empty__body">{children}</p>}

      {actionLabel && actionTo && (
        <Link className="empty__action" to={actionTo}>{actionLabel}</Link>
      )}
      {actionLabel && !actionTo && onAction && (
        <Button onClick={onAction}>{actionLabel}</Button>
      )}
    </div>
  );
}
