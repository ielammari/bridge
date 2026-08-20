import Icon from '../Icon/Icon.jsx';
import './Checkbox.css';

/**
 * A checkbox drawn from the design tokens: the native input keeps its
 * semantics, focus and keyboard behaviour and is only made invisible. `hint`
 * reads under the label.
 */
export default function Checkbox({ label, hint, disabled, ...input }) {
  return (
    <label className={`checkbox${disabled ? ' checkbox--disabled' : ''}`}>
      <input type="checkbox" className="checkbox__input" disabled={disabled} {...input} />
      <span className="checkbox__box" aria-hidden="true">
        <Icon name="check" className="checkbox__tick" />
      </span>
      <span className="checkbox__text">
        <span className="checkbox__label">{label}</span>
        {hint && <span className="checkbox__hint">{hint}</span>}
      </span>
    </label>
  );
}
