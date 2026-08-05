import Icon from '../../components/Icon/Icon.jsx';
import { useTheme } from '../../context/ThemeContext.jsx';

const CHOICES = [
  { value: 'system', label: 'Système', hint: 'Suit le réglage de votre appareil' },
  { value: 'light', label: 'Clair', hint: 'Fond clair en permanence' },
  { value: 'dark', label: 'Sombre', hint: 'Fond sombre en permanence' },
];

/**
 * The theme, kept on this device rather than on the account: it answers where
 * someone is reading, not who they are.
 */
export default function AppearanceSection() {
  const { preference, setPreference } = useTheme();

  return (
    <section className="card" aria-labelledby="appearance-title">
      <div className="card__head">
        <h2 id="appearance-title" className="card__title">Apparence</h2>
        <p className="card__subtitle">Ce réglage ne vaut que pour cet appareil.</p>
      </div>
      <div className="card__body">
        <fieldset className="theme">
          <legend className="visually-hidden">Thème de l'interface</legend>
          {CHOICES.map((choice) => (
            <label
              key={choice.value}
              className={`theme__choice${preference === choice.value ? ' theme__choice--on' : ''}`}
            >
              <input
                type="radio"
                name="theme"
                className="visually-hidden"
                value={choice.value}
                checked={preference === choice.value}
                onChange={() => setPreference(choice.value)}
              />
              {/* A swatch of the theme itself, so the choice is shown rather
                  than only named. */}
              <span className={`theme__swatch theme__swatch--${choice.value}`} aria-hidden="true">
                <span className="theme__swatch-bar" />
                <span className="theme__swatch-bar theme__swatch-bar--short" />
              </span>
              <span className="theme__label">{choice.label}</span>
              <span className="theme__hint">{choice.hint}</span>
              <Icon name="check" className="theme__check" />
            </label>
          ))}
        </fieldset>
      </div>
    </section>
  );
}
