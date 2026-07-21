import { useId } from 'react';
import './Field.css';

/**
 * Labelled input with helper text and inline error.
 * The label is always visible: a placeholder disappears the moment someone
 * starts typing, which is when they most need to know what the field is.
 */
export default function Field({
  label,
  type = 'text',
  value,
  onChange,
  onBlur,
  error,
  hint,
  required = false,
  autoComplete,
  ...rest
}) {
  const id = useId();
  const hintId = `${id}-hint`;
  const errorId = `${id}-error`;

  const describedBy = [hint ? hintId : null, error ? errorId : null].filter(Boolean).join(' ');

  return (
    <div className="field">
      <label className="field__label" htmlFor={id}>
        {label}
        {required && (
          <span className="field__required" aria-hidden="true">
            *
          </span>
        )}
      </label>

      {hint && (
        <p className="field__hint" id={hintId}>
          {hint}
        </p>
      )}

      <input
        id={id}
        type={type}
        className={`field__input${error ? ' field__input--error' : ''}`}
        value={value}
        onChange={onChange}
        onBlur={onBlur}
        required={required}
        autoComplete={autoComplete}
        aria-invalid={error ? true : undefined}
        aria-describedby={describedBy || undefined}
        {...rest}
      />

      {error && (
        <p className="field__error" id={errorId}>
          {error}
        </p>
      )}
    </div>
  );
}
