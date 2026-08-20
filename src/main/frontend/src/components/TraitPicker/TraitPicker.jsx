import { useMemo, useState } from 'react';
import Icon from '../Icon/Icon.jsx';
import useMasonry from '../../hooks/useMasonry.js';
import useMediaQuery from '../../hooks/useMediaQuery.js';
import './TraitPicker.css';

// Accent and case insensitive, so "francais" matches "Français".
function fold(text) {
  return text.normalize('NFD').replace(/\p{Diacritic}/gu, '').toLowerCase();
}

/**
 * Selects traits from a catalogue grouped by category, with a search that
 * narrows the option set. `chipNote` labels each selected chip with what the
 * caller has decided about that trait.
 */
export default function TraitPicker({ catalogue, value, onChange, chipNote }) {
  const [query, setQuery] = useState('');
  const [openCategories, setOpenCategories] = useState(() => new Set());

  // Two columns while there is room for them; below that the browser's own
  // flow is left alone.
  const narrow = useMediaQuery('(max-width: 48rem)');
  const categories = useMasonry(narrow ? 1 : 2, 8);

  const selected = useMemo(() => new Set(value), [value]);
  const needle = fold(query.trim());

  // Category list with traits filtered by the search, empty categories dropped.
  const filtered = useMemo(() => {
    if (!needle) {
      return catalogue.map((category) => ({ ...category, matches: category.traits }));
    }
    return catalogue
      .map((category) => ({
        ...category,
        matches: category.traits.filter((trait) => fold(trait.label).includes(needle)),
      }))
      .filter((category) => category.matches.length > 0);
  }, [catalogue, needle]);

  const labelById = useMemo(() => {
    const map = new Map();
    for (const category of catalogue) {
      for (const trait of category.traits) {
        map.set(trait.id, trait.label);
      }
    }
    return map;
  }, [catalogue]);

  const searching = needle.length > 0;

  function toggle(traitId) {
    const next = new Set(selected);
    if (next.has(traitId)) {
      next.delete(traitId);
    } else {
      next.add(traitId);
    }
    onChange([...next]);
  }

  function toggleCategory(categoryId) {
    const next = new Set(openCategories);
    if (next.has(categoryId)) {
      next.delete(categoryId);
    } else {
      next.add(categoryId);
    }
    setOpenCategories(next);
  }

  function selectedCountIn(category) {
    return category.traits.reduce((count, trait) => count + (selected.has(trait.id) ? 1 : 0), 0);
  }

  return (
    <div className="picker">
      <div className="picker__search">
        <Icon name="search" className="picker__search-icon" />
        <input
          type="search"
          className="picker__search-input"
          placeholder="Rechercher une compétence, une langue, un atout..."
          value={query}
          onChange={(event) => setQuery(event.target.value)}
          aria-label="Rechercher un trait"
        />
      </div>

      {selected.size > 0 && (
        <div className="picker__selected" aria-live="polite">
          <p className="picker__selected-count">
            {selected.size} sélectionné{selected.size > 1 ? 's' : ''}
          </p>
          <ul className="picker__chips">
            {[...selected].map((id) => (
              <li key={id}>
                <button
                  type="button"
                  className="chip"
                  onClick={() => toggle(id)}
                  aria-label={`Retirer ${labelById.get(id) ?? ''}`}
                >
                  <span>{labelById.get(id) ?? '—'}</span>
                  {chipNote && <span className="chip__note mono">{chipNote(id)}</span>}
                  <Icon name="close" className="chip__remove" />
                </button>
              </li>
            ))}
          </ul>
        </div>
      )}

      <ul className="picker__categories" ref={categories}>
        {filtered.map((category) => {
          const open = searching || openCategories.has(category.id);
          const count = selectedCountIn(category);
          const panelId = `picker-cat-${category.id}`;

          return (
            <li key={category.id} className="picker__category">
              <button
                type="button"
                className="picker__category-head"
                aria-expanded={open}
                aria-controls={panelId}
                onClick={() => toggleCategory(category.id)}
                disabled={searching}
              >
                <span className="picker__category-name">{category.label}</span>
                <span className="picker__category-meta">
                  {count > 0 && <span className="picker__badge">{count}</span>}
                  <Icon name="chevron" className={`picker__caret${open ? ' picker__caret--open' : ''}`} />
                </span>
              </button>

              {open && (
                <div id={panelId} className="picker__options">
                  {category.matches.map((trait) => {
                    const isOn = selected.has(trait.id);
                    return (
                      <button
                        key={trait.id}
                        type="button"
                        className={`trait${isOn ? ' trait--on' : ''}`}
                        aria-pressed={isOn}
                        onClick={() => toggle(trait.id)}
                      >
                        <Icon name="check" className="trait__check" />
                        <span>{trait.label}</span>
                      </button>
                    );
                  })}
                </div>
              )}
            </li>
          );
        })}

        {filtered.length === 0 && (
          <li className="picker__empty">Aucun trait ne correspond à votre recherche.</li>
        )}
      </ul>
    </div>
  );
}
