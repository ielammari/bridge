import { createContext, useCallback, useContext, useMemo, useRef, useState } from 'react';
import Icon from '../Icon/Icon.jsx';
import './Toast.css';

const ToastContext = createContext(null);

const LIFETIME = 5000;
const EXIT = 160;

const ICONS = { success: 'check', error: 'warning', info: 'bell' };

/**
 * Transient confirmations, dismissed after five seconds or on click. Anything
 * the reader must act on stays inline instead.
 */
export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([]);
  const timers = useRef(new Map());
  const nextId = useRef(0);

  const remove = useCallback((id) => {
    clearTimeout(timers.current.get(id));
    timers.current.delete(id);
    setToasts((list) => list.map((t) => (t.id === id ? { ...t, leaving: true } : t)));
    setTimeout(() => setToasts((list) => list.filter((t) => t.id !== id)), EXIT);
  }, []);

  const arm = useCallback(
    (id) => {
      clearTimeout(timers.current.get(id));
      timers.current.set(id, setTimeout(() => remove(id), LIFETIME));
    },
    [remove],
  );

  const push = useCallback(
    (tone, message) => {
      const id = nextId.current++;
      setToasts((list) => [...list, { id, tone, message, leaving: false }]);
      arm(id);
    },
    [arm],
  );

  const value = useMemo(
    () => ({
      success: (message) => push('success', message),
      error: (message) => push('error', message),
      info: (message) => push('info', message),
    }),
    [push],
  );

  return (
    <ToastContext.Provider value={value}>
      {children}

      <div className="toasts" aria-live="polite" aria-atomic="false">
        {toasts.map((toast) => (
          <div
            key={toast.id}
            className={`toast toast--${toast.tone}${toast.leaving ? ' toast--leaving' : ''}`}
            onMouseEnter={() => clearTimeout(timers.current.get(toast.id))}
            onMouseLeave={() => arm(toast.id)}
          >
            <span className="toast__icon"><Icon name={ICONS[toast.tone]} /></span>
            <p className="toast__text">{toast.message}</p>
            <button
              type="button"
              className="toast__close"
              onClick={() => remove(toast.id)}
              aria-label="Fermer la notification"
            >
              <Icon name="close" />
            </button>
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  );
}

export function useToast() {
  const context = useContext(ToastContext);
  if (!context) {
    throw new Error('useToast doit être utilisé à l\'intérieur d\'un ToastProvider.');
  }
  return context;
}
