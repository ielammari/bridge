import { useId } from 'react';
import Icon from '../Icon/Icon.jsx';
import './Select.css';

/**
 * Labelled select matching the Field primitive. The label is always visible and
 * the chevron is decorative, so the native control keeps its keyboard behaviour.
 */
export default function Select({
  label,
  value,
  onChange,
  options,
  placeholder,
  hint,
  error,
  required = false,
  ...rest
}) {
  const id = useId();
  const hintId = `${id}-hint`;
  const errorId = `${id}-error`;
  const describedBy = [hint ? hintId : null, error ? errorId : null].filter(Boolean).join(' ');

  return (
    <div className="select">
      <label className="select__label" htmlFor={id}>
        {label}
        {required && (
          <span className="select__required" aria-hidden="true">
            *
          </span>
        )}
      </label>

      {hint && (
        <p className="select__hint" id={hintId}>
          {hint}
        </p>
      )}

      <div className="select__control">
        <select
          id={id}
          className={`select__native${error ? ' select__native--error' : ''}`}
          value={value ?? ''}
          onChange={onChange}
          aria-invalid={error ? true : undefined}
          aria-describedby={describedBy || undefined}
          {...rest}
        >
          {placeholder && <option value="">{placeholder}</option>}
          {options.map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </select>
        <Icon name="chevron" className="select__chevron" />
      </div>

      {error && (
        <p className="select__error" id={errorId}>
          {error}
        </p>
      )}
    </div>
  );
}
