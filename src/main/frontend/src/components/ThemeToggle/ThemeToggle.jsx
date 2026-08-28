import Icon from '../Icon/Icon.jsx';
import { useTheme } from '../../context/ThemeContext.jsx';
import './ThemeToggle.css';

/**
 * Flips the interface between its two themes. It writes an explicit choice, so
 * an account following its machine leaves that behind by using it.
 */
export default function ThemeToggle() {
  const { theme, setPreference } = useTheme();
  const next = theme === 'dark' ? 'light' : 'dark';
  const label = next === 'dark' ? 'Passer au thème sombre' : 'Passer au thème clair';

  return (
    <button
      type="button"
      className="themetoggle"
      onClick={() => setPreference(next)}
      aria-label={label}
      title={label}
    >
      <Icon name="contrast" />
    </button>
  );
}
