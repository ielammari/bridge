import { useTheme } from '../../context/ThemeContext.jsx';
import word from '../../assets/logo.png';
import wordDark from '../../assets/logo-dark.png';
import letter from '../../assets/logo-mark.png';

/**
 * The wordmark, or the B alone. Each file is trimmed to its ink, so a rule under
 * the box sits under the word. `on="page"` takes the dark cut for a ground that
 * follows the theme; the rail and the auth panel are dark in both.
 */
export default function Wordmark({ on = 'rail', mark = false, className, ...rest }) {
  const { theme } = useTheme();
  const src = mark ? letter : (on === 'page' && theme === 'light' ? wordDark : word);
  const size = mark ? { width: 160, height: 271 } : { width: 900, height: 271 };

  return (
    <img
      src={src}
      alt="Bridge"
      className={className}
      {...size}
      {...rest}
    />
  );
}
