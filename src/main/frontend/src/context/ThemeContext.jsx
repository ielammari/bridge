import { createContext, useCallback, useContext, useEffect, useState } from 'react';

const KEY = 'bridge.theme';
const PREFERENCES = ['system', 'light', 'dark'];

const systemQuery = () => window.matchMedia('(prefers-color-scheme: dark)');

/** The stored choice, or following the system when nothing has been chosen. */
export function readPreference() {
  const stored = localStorage.getItem(KEY);
  return PREFERENCES.includes(stored) ? stored : 'system';
}

/** The theme a preference actually resolves to right now. */
export function resolve(preference) {
  if (preference === 'system') {
    return systemQuery().matches ? 'dark' : 'light';
  }
  return preference;
}

/**
 * Writes the resolved theme where the stylesheet reads it. The same function
 * runs before the first paint from a script in the page head, so the document
 * is never painted in one theme and corrected into the other.
 */
export function applyTheme(preference) {
  document.documentElement.dataset.theme = resolve(preference);
}

const ThemeContext = createContext(null);

export function ThemeProvider({ children }) {
  const [preference, setStored] = useState(readPreference);

  useEffect(() => {
    applyTheme(preference);
    // Only while following the system: an explicit choice is not overridden by
    // the machine changing its mind.
    if (preference !== 'system') return undefined;

    const query = systemQuery();
    const follow = () => applyTheme('system');
    query.addEventListener('change', follow);
    return () => query.removeEventListener('change', follow);
  }, [preference]);

  const setPreference = useCallback((next) => {
    localStorage.setItem(KEY, next);
    setStored(next);
  }, []);

  return (
    <ThemeContext.Provider value={{ preference, setPreference, theme: resolve(preference) }}>
      {children}
    </ThemeContext.Provider>
  );
}

export function useTheme() {
  const context = useContext(ThemeContext);
  if (!context) {
    throw new Error('useTheme doit être utilisé dans un ThemeProvider.');
  }
  return context;
}
