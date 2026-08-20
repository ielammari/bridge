import { useNavigate } from 'react-router-dom';
import { messagesApi } from '../../api/messages.js';
import Button from '../../components/Button/Button.jsx';
import EmptyState from '../../components/EmptyState/EmptyState.jsx';
import ErrorState from '../../components/ErrorState/ErrorState.jsx';
import Icon from '../../components/Icon/Icon.jsx';
import Skeleton from '../../components/Skeleton/Skeleton.jsx';
import { useToast } from '../../components/Toast/ToastContext.jsx';
import {
  clockTime, dateTime, TIME_GROUP_LABELS, TIME_GROUP_ORDER, timeGroup,
} from '../../constants/format.js';
import { useAuth } from '../../context/AuthContext.jsx';
import useResource from '../../hooks/useResource.js';
import Workspace from '../Workspace/Workspace.jsx';
import './messages.css';

// Each notification type carries a token colour key, so the accent tells the
// reader what kind of news it is without relying on colour alone (the text does).
const TONE = {
  APPLICATION_RECEIVED: 'en-revue',
  APPLICATION_SUBMITTED: 'nouvelle',
  SCHEDULE_NEEDED: 'entretien-rh',
  INTERVIEW_SCHEDULED: 'examen-technique',
  EXAM_UNASSIGNED: 'cloturee',
  EXAM_OVERDUE: 'refusee',
  REJECTED: 'refusee',
  HIRED: 'embauchee',
};

/**
 * Where a notice leads: a recruiter to the application with the action it calls
 * for already open, an expert to the exam to run, a candidate to their
 * applications, or to the record once that application has closed.
 */
function destination(message, role) {
  const app = message.applicationId;
  if (!app) return null;

  if (role === 'CANDIDAT') {
    return message.type === 'REJECTED' || message.type === 'HIRED'
      ? `/historique/candidatures/${app}`
      : `/mes-candidatures?candidature=${app}`;
  }

  if (role === 'EXPERT') {
    return message.type === 'INTERVIEW_SCHEDULED' ? `/evaluations/${app}` : '/evaluations';
  }

  switch (message.type) {
    case 'APPLICATION_RECEIVED':
      return `/candidatures?candidature=${app}&action=preselection`;
    case 'SCHEDULE_NEEDED':
    case 'EXAM_OVERDUE':
      return `/candidatures?candidature=${app}&action=planification`;
    default:
      return `/candidatures?candidature=${app}`;
  }
}

export default function Messages() {
  const toast = useToast();
  const navigate = useNavigate();
  const { user } = useAuth();
  const { status, data, setData, reload, pending, leaving } = useResource(() => messagesApi.inbox());

  const messages = data ?? [];
  const unread = messages.filter((m) => !m.read).length;

  // Grouped by when they arrived, so the inbox reads as a record of the funnel
  // rather than one long undifferentiated run.
  const groups = TIME_GROUP_ORDER
    .map((key) => ({
      key,
      label: TIME_GROUP_LABELS[key],
      items: messages.filter((m) => timeGroup(m.sentAt) === key),
    }))
    .filter((group) => group.items.length > 0);

  // The nav badge refreshes itself: the API client reports every write.
  async function readOne(id) {
    try {
      await messagesApi.markRead(id);
      setData((list) => list.map((m) => (m.id === id ? { ...m, read: true } : m)));
    } catch (apiError) {
      toast.error(apiError.message);
    }
  }

  /** Reading a notice settles it and takes the reader to what it is about. */
  function open(message, to) {
    if (!message.read) readOne(message.id);
    if (to) navigate(to);
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
    <Workspace
      width="narrow"
      title="Messages"
      stats={status === 'ready' ? [
        { value: unread, label: unread > 1 ? 'non lus' : 'non lu' },
        { value: messages.length, label: 'au total' },
      ] : []}
      action={unread > 0 && (
        <Button variant="secondary" onClick={readAll}>Tout marquer comme lu</Button>
      )}
    >
      {pending && (
        <Skeleton variant="rows" count={4} leaving={leaving}
          label="Chargement de vos messages" />
      )}

      {status === 'error' && (
        <ErrorState onRetry={reload}>
          Vos messages n'ont pas pu être chargés. Réessayez dans un instant.
        </ErrorState>
      )}

      {status === 'ready' && messages.length === 0 && (
        <EmptyState title="Aucun message pour le moment.">
          Vous serez prévenu ici à chaque étape de vos candidatures : réception, entretien
          planifié, décision.
        </EmptyState>
      )}

      {status === 'ready' && groups.map((group) => (
        <section key={group.key} className="msg__group">
          <h2 className="msg__groupname">
            {group.label}
            <span className="msg__groupcount mono">{group.items.length}</span>
          </h2>

          <ul className="msg__list">
            {group.items.map((m) => {
              const to = destination(m, user.role);
              return (
                <li key={m.id}
                  className={`msg${m.read ? '' : ' msg--unread'}${to ? ' msg--openable' : ''}`}
                  style={{ '--tone': `var(--status-${TONE[m.type] ?? 'nouvelle'})` }}
                  role={to ? 'link' : undefined}
                  tabIndex={to ? 0 : undefined}
                  onClick={() => open(m, to)}
                  onKeyDown={(event) => {
                    if (to && (event.key === 'Enter' || event.key === ' ')) {
                      event.preventDefault();
                      open(m, to);
                    }
                  }}>
                  <span className="msg__icon"><Icon name="bell" /></span>
                  <div className="msg__body">
                    <p className="msg__text">{m.content}</p>
                    <p className="msg__time">
                      {group.key === 'today' ? clockTime(new Date(m.sentAt).toTimeString())
                        : dateTime(m.sentAt)}
                    </p>
                  </div>
                  {!m.read && (
                    <Button variant="text" onClick={(event) => {
                      event.stopPropagation();
                      readOne(m.id);
                    }}>
                      Marquer comme lu
                    </Button>
                  )}
                </li>
              );
            })}
          </ul>
        </section>
      ))}
    </Workspace>
  );
}
