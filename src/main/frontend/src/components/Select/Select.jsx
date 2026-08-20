import { useId } from 'react';
import Icon from '../Icon/Icon.jsx';
import '../Field/Field.css';
import './Select.css';

/**
 * Labelled select matching the Field primitive: the label stays visible and the
 * chevron is decorative, so the native control keeps its keyboard behaviour.
 * The label, hint and error come from Field, so the two cannot drift apart.
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
  labelHidden = false,
  ...rest
}) {
  const id = useId();
  const hintId = `${id}-hint`;
  const errorId = `${id}-error`;
  const describedBy = [hint ? hintId : null, error ? errorId : null].filter(Boolean).join(' ');

  return (
    <div className="select">
      {/* For a control its surroundings already name. The label still exists,
          so the control keeps its accessible name; the head goes only once
          there is no hint left in it. */}
      {labelHidden && !hint ? (
        <label className="visually-hidden" htmlFor={id}>{label}</label>
      ) : (
        <div className="field__head">
          <label className={`field__label${labelHidden ? ' visually-hidden' : ''}`} htmlFor={id}>
            {label}
            {required && (
              <span className="field__required" aria-hidden="true">
                *
              </span>
            )}
          </label>
          {hint && (
            <span className="field__hint" id={hintId} title={hint}>
              {hint}
            </span>
          )}
        </div>
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
        <p className="field__error" id={errorId}>
          {error}
        </p>
      )}
    </div>
  );
}
