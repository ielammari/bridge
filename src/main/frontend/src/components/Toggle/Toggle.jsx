import './Toggle.css';

/**
 * An exclusive choice between named options, as one switch whose marker slides
 * to the chosen side. Built on real radios, so arrow keys move between them.
 */
export default function Toggle({ name, label, value, options, onChange }) {
  const index = Math.max(0, options.findIndex((option) => option.value === value));

  return (
    <fieldset className="toggle">
      <legend className="visually-hidden">{label}</legend>
      <div
        className="toggle__track"
        style={{ '--toggle-count': options.length, '--toggle-index': index }}
      >
        <span className="toggle__marker" aria-hidden="true" />
        {options.map((option) => (
          <label key={option.value} className="toggle__side">
            <input
              className="visually-hidden"
              type="radio"
              name={name}
              value={option.value}
              checked={option.value === value}
              onChange={() => onChange(option.value)}
            />
            <span className="toggle__label">{option.label}</span>
          </label>
        ))}
      </div>
    </fieldset>
  );
}
