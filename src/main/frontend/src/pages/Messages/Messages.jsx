import { useEffect, useState } from 'react';
import { messagesApi } from '../../api/messages.js';
import Alert from '../../components/Alert/Alert.jsx';
import Button from '../../components/Button/Button.jsx';
import Icon from '../../components/Icon/Icon.jsx';
import { useNotifications } from '../../context/NotificationContext.jsx';
import Workspace from '../Workspace/Workspace.jsx';
import './messages.css';

const when = new Intl.DateTimeFormat('fr-FR', {
  day: 'numeric', month: 'long', hour: '2-digit', minute: '2-digit',
});

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
  const { refresh } = useNotifications();
  const [status, setStatus] = useState('loading');
  const [messages, setMessages] = useState([]);

  function load() {
    return messagesApi.inbox().then(setMessages);
  }

  useEffect(() => {
    let cancelled = false;
    load()
      .then(() => !cancelled && setStatus('ready'))
      .catch(() => !cancelled && setStatus('error'));
    return () => {
      cancelled = true;
    };
  }, []);

  async function readOne(id) {
    await messagesApi.markRead(id);
    setMessages((list) => list.map((m) => (m.id === id ? { ...m, read: true } : m)));
    refresh();
  }

  async function readAll() {
    await messagesApi.markAllRead();
    setMessages((list) => list.map((m) => ({ ...m, read: true })));
    refresh();
  }

  const unread = messages.filter((m) => !m.read).length;

  if (status === 'loading') {
    return <Workspace title="Messages"><p className="msg__muted">Chargement...</p></Workspace>;
  }
  if (status === 'error') {
    return <Workspace title="Messages"><Alert>Vos messages n'ont pas pu être chargés.</Alert></Workspace>;
  }

  return (
    <Workspace title="Messages">
      <div className="msg__bar">
        <p className="msg__count">
          {unread > 0 ? `${unread} non lu${unread > 1 ? 's' : ''}` : 'Tout est lu'}
        </p>
        {unread > 0 && <Button variant="text" onClick={readAll}>Tout marquer comme lu</Button>}
      </div>

      {messages.length === 0 ? (
        <div className="msg__empty"><p>Vous n'avez aucun message pour le moment.</p></div>
      ) : (
        <ul className="msg__list">
          {messages.map((m) => (
            <li key={m.id}
              className={`msg${m.read ? '' : ' msg--unread'}`}
              style={{ '--tone': `var(--status-${TONE[m.type] ?? 'nouvelle'})` }}>
              <span className="msg__icon"><Icon name="bell" /></span>
              <div className="msg__body">
                <p className="msg__text">{m.content}</p>
                <p className="msg__time">{when.format(new Date(m.sentAt))}</p>
              </div>
              {!m.read && (
                <button type="button" className="msg__read" onClick={() => readOne(m.id)}>
                  Marquer comme lu
                </button>
              )}
            </li>
          ))}
        </ul>
      )}
    </Workspace>
  );
}
