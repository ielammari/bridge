import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from 'react';
import { useLocation } from 'react-router-dom';
import { setMutationHandler } from '../api/client.js';
import { messagesApi } from '../api/messages.js';
import { useAuth } from './AuthContext.jsx';

const NotificationContext = createContext(null);

export function NotificationProvider({ children }) {
  const { user } = useAuth();
  const location = useLocation();
  const [unreadCount, setUnreadCount] = useState(0);

  const refresh = useCallback(async () => {
    if (!user) {
      setUnreadCount(0);
      return;
    }
    try {
      const { count } = await messagesApi.unreadCount();
      setUnreadCount(count);
    } catch {
      // A failed count is not worth surfacing; the badge just stays as is.
    }
  }, [user]);

  // Installed once, so it reaches the current refresh through a ref.
  const latest = useRef(refresh);
  latest.current = refresh;

  useEffect(() => {
    setMutationHandler(() => latest.current());
  }, []);

  // On every write and every navigation. Reading the count is also what
  // settles the notifications whose task is done.
  useEffect(() => {
    refresh();
  }, [refresh, location.pathname]);

  const value = useMemo(() => ({ unreadCount, refresh }), [unreadCount, refresh]);
  return <NotificationContext.Provider value={value}>{children}</NotificationContext.Provider>;
}

export function useNotifications() {
  const context = useContext(NotificationContext);
  if (!context) {
    throw new Error('useNotifications doit être utilisé à l\'intérieur d\'un NotificationProvider.');
  }
  return context;
}
