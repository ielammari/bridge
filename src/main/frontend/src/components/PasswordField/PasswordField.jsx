import { useId, useState } from 'react';
import Icon from '../Icon/Icon.jsx';
import '../Field/Field.css';
import './PasswordField.css';

/**
 * Password entry with a reveal toggle and a Caps Lock warning. `rulesId` points
 * at a PasswordRules list rendered elsewhere in the form.
 */
export default function PasswordField({
  label,
  value,
  onChange,
  onBlur,
  error,
  name,
  autoComplete,
  required = false,
  rulesId,
  ...rest
}) {
  const id = useId();
  const errorId = `${id}-error`;

  const [revealed, setRevealed] = useState(false);
  const [capsLock, setCapsLock] = useState(false);

  const describedBy = [rulesId, error ? errorId : null].filter(Boolean).join(' ');

  function trackCapsLock(event) {
    setCapsLock(event.getModifierState?.('CapsLock') ?? false);
  }

  return (
    <div className="field">
      <label className="field__label" htmlFor={id}>
        {label}
        {required && <span className="field__required" aria-hidden="true">*</span>}
      </label>

      <div className="password__control">
        <input
          id={id}
          name={name}
          type={revealed ? 'text' : 'password'}
          className={`field__input password__input${error ? ' field__input--error' : ''}`}
          value={value}
          onChange={onChange}
          onBlur={(event) => {
            setCapsLock(false);
            onBlur?.(event);
          }}
          onKeyUp={trackCapsLock}
          onKeyDown={trackCapsLock}
          autoComplete={autoComplete}
          required={required}
          aria-invalid={error ? true : undefined}
          aria-describedby={describedBy || undefined}
          {...rest}
        />
        <button
          type="button"
          className="password__reveal"
          onClick={() => setRevealed((shown) => !shown)}
          aria-pressed={revealed}
          aria-label={revealed ? 'Masquer le mot de passe' : 'Afficher le mot de passe'}
        >
          <Icon name={revealed ? 'eye-off' : 'eye'} />
        </button>
      </div>

      {capsLock && (
        <p className="password__caps" role="status">
          <Icon name="warning" /> La touche Verr. Maj est activée.
        </p>
      )}

      {error && <p className="field__error" id={errorId}>{error}</p>}
    </div>
  );
}
