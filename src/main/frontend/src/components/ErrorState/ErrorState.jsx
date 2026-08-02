import Button from '../Button/Button.jsx';
import Icon from '../Icon/Icon.jsx';
import './ErrorState.css';

/** A failed load, with a retry so recovering does not mean reloading the page. */
export default function ErrorState({
  title = 'Ces informations n\'ont pas pu être chargées.',
  children = 'La connexion au serveur a échoué. Réessayez dans un instant.',
  onRetry,
  retrying = false,
}) {
  return (
    <div className="errorstate" role="alert">
      <p className="errorstate__title">
        <Icon name="warning" /> {title}
      </p>
      <p className="errorstate__body">{children}</p>
      {onRetry && (
        <Button variant="secondary" onClick={onRetry} loading={retrying}>
          <Icon name="retry" /> Réessayer
        </Button>
      )}
    </div>
  );
}
