import { messagesApi } from '../../api/messages.js';
import Button from '../../components/Button/Button.jsx';
import EmptyState from '../../components/EmptyState/EmptyState.jsx';
import ErrorState from '../../components/ErrorState/ErrorState.jsx';
import Icon from '../../components/Icon/Icon.jsx';
import Skeleton from '../../components/Skeleton/Skeleton.jsx';
import { useToast } from '../../components/Toast/ToastContext.jsx';
import { dateTime } from '../../constants/format.js';
import useResource from '../../hooks/useResource.js';
import Workspace from '../Workspace/Workspace.jsx';
import './messages.css';

// Each notification type carries a token colour key, so the accent tells the
// reader what kind of news it is without relying on colour alone (the text does).
const TONE = {
  APPLICATION_RECEIVED: 'en-revue',
  SCHEDULE_NEEDED: 'entretien-rh',
  INTERVIEW_SCHEDULED: 'examen-technique',
  REJECTED: 'refusee',
  HIRED: 'embauchee',
};

export default function Messages() {
  const toast = useToast();
  const { status, data, setData, reload } = useResource(() => messagesApi.inbox());

  const messages = data ?? [];
  const unread = messages.filter((m) => !m.read).length;

  // The nav badge refreshes itself: the API client reports every write.
  async function readOne(id) {
    try {
      await messagesApi.markRead(id);
      setData((list) => list.map((m) => (m.id === id ? { ...m, read: true } : m)));
    } catch (apiError) {
      toast.error(apiError.message);
    }
  }

  async function readAll() {
    try {
      await messagesApi.markAllRead();
      setData((list) => list.map((m) => ({ ...m, read: true })));
    } catch (apiError) {
      toast.error(apiError.message);
    }
  }

  return (
    <Workspace title="Messages">
      {status === 'loading' && <Skeleton count={4} label="Chargement de vos messages" />}

      {status === 'error' && (
        <ErrorState onRetry={reload}>
          Vos messages n'ont pas pu être chargés. Réessayez dans un instant.
        </ErrorState>
      )}

      {status === 'ready' && (
        <>
          <div className="msg__bar">
            <p className="msg__count">
              {unread > 0 ? `${unread} non lu${unread > 1 ? 's' : ''}` : 'Tout est lu'}
            </p>
            {unread > 0 && <Button variant="text" onClick={readAll}>Tout marquer comme lu</Button>}
          </div>

          {messages.length === 0 ? (
            <EmptyState title="Aucun message pour le moment.">
              Vous serez prévenu ici à chaque étape de vos candidatures : réception, entretien
              planifié, décision.
            </EmptyState>
          ) : (
            <ul className="msg__list">
              {messages.map((m) => (
                <li key={m.id}
                  className={`msg${m.read ? '' : ' msg--unread'}`}
                  style={{ '--tone': `var(--status-${TONE[m.type] ?? 'nouvelle'})` }}>
                  <span className="msg__icon"><Icon name="bell" /></span>
                  <div className="msg__body">
                    <p className="msg__text">{m.content}</p>
                    <p className="msg__time">{dateTime(m.sentAt)}</p>
                  </div>
                  {!m.read && (
                    <Button variant="text" onClick={() => readOne(m.id)}>
                      Marquer comme lu
                    </Button>
                  )}
                </li>
              ))}
            </ul>
          )}
        </>
      )}
    </Workspace>
  );
}
