import Icon from '../Icon/Icon.jsx';
import './Checkbox.css';

/**
 * A checkbox drawn from the design tokens. The native input stays in place and
 * keeps its semantics, focus, and keyboard behaviour; it is only made invisible
 * so the box beside it can be drawn instead.
 *
 * `hint` reads under the label, for a choice whose consequence is not obvious
 * from its name.
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
