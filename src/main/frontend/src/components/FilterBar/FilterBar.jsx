import { useId } from 'react';
import Icon from '../Icon/Icon.jsx';
import Select from '../Select/Select.jsx';
import './FilterBar.css';

/**
 * Picks one dimension to narrow a listing by, and the value within it. With no
 * dimension the caller groups by its default instead, so a listing is never a
 * flat run. A free text dimension gets an input backed by a datalist.
 * `onDimension` also clears the value, which belongs to its dimension.
 */
export default function FilterBar({ dimensions, dimension, value, onDimension, onValue, count }) {
  const listId = useId();
  const active = dimensions.find((d) => d.key === dimension);

  return (
    <div className="filters">
      <Select
        label="Filtrer par"
        value={dimension}
        onChange={(event) => onDimension(event.target.value)}
        options={dimensions.map((d) => ({ value: d.key, label: d.label }))}
        placeholder="Aucun filtre"
      />

      {active?.type === 'search' && (
        <div className="filters__field">
          <label className="field__label" htmlFor={listId}>{active.valueLabel}</label>
          <div className="filters__search">
            <Icon name="search" className="filters__search-icon" />
            <input
              id={listId}
              type="search"
              className="field__input filters__search-input"
              list={`${listId}-options`}
              value={value}
              placeholder={active.placeholder}
              onChange={(event) => onValue(event.target.value)}
            />
          </div>
          <datalist id={`${listId}-options`}>
            {active.options.map((option) => (
              <option key={option} value={option} />
            ))}
          </datalist>
        </div>
      )}

      {active?.type === 'select' && (
        <Select
          label={active.valueLabel}
          value={value}
          onChange={(event) => onValue(event.target.value)}
          options={active.options}
          placeholder="Tous"
        />
      )}

      {active?.type === 'date' && (
        <div className="filters__field">
          <label className="field__label" htmlFor={listId}>{active.valueLabel}</label>
          <input id={listId} type="date" className="field__input" value={value}
            onChange={(event) => onValue(event.target.value)} />
        </div>
      )}

      <p className="filters__count" aria-live="polite">{count}</p>
    </div>
  );
}
