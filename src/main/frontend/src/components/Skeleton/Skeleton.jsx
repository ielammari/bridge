import CardGrid from '../CardGrid/CardGrid.jsx';
import './Skeleton.css';

/**
 * The placeholder a page shows while its data loads, in the shape of that
 * page's own content.
 *
 * Each variant is built from the classes the real screen uses, inheriting the
 * grid, the card, and the spacing rather than restating them, so it cannot
 * drift out of step when a page is restyled.
 *
 * `leaving` plays the exit. The content mounts only after it finishes, so the
 * two never overlap.
 */
export default function Skeleton({
  variant = 'cards',
  count = 3,
  size,
  leaving = false,
  label = 'Chargement',
}) {
  const items = Array.from({ length: count }, (_, index) => index);

  return (
    <div
      className={`skeleton skeleton--${variant}${leaving ? ' skeleton--leaving' : ''}`}
      role="status"
      aria-busy="true"
    >
      <span className="visually-hidden">{label}</span>
      {render(variant, items, size)}
    </div>
  );
}

function render(variant, items, size) {
  switch (variant) {
    case 'rows':
      return rows(items);
    case 'form':
      return form(items);
    case 'record':
      return record();
    case 'page':
      return page();
    default:
      return cards(items, size);
  }
}

/** A listing: the same grid and the same card the page will fill. */
function cards(items, size) {
  return (
    <CardGrid size={size} label={null}>
      {items.map((index) => (
        <li key={index} className="tile" aria-hidden="true">
          <div className="skeleton__line">
            <span className="skeleton__bar skeleton__bar--title" />
            <span className="skeleton__bar skeleton__bar--badge" />
          </div>
          <span className="skeleton__bar skeleton__bar--meta" />
          <span className="skeleton__bar skeleton__bar--body" />
          <div className="skeleton__line skeleton__line--foot">
            <span className="skeleton__bar skeleton__bar--action" />
            <span className="skeleton__bar skeleton__bar--action" />
          </div>
        </li>
      ))}
    </CardGrid>
  );
}

/** The message list: an icon, two lines of text, and the read control. */
function rows(items) {
  return (
    <ul className="skeleton__rows" aria-hidden="true">
      {items.map((index) => (
        <li key={index} className="skeleton__msg">
          <span className="skeleton__bar skeleton__bar--icon" />
          <div className="skeleton__msgbody">
            <span className="skeleton__bar skeleton__bar--body" />
            <span className="skeleton__bar skeleton__bar--time" />
          </div>
        </li>
      ))}
    </ul>
  );
}

/** A form: a label above a control, repeated, inside the panel that holds it. */
function form(items) {
  return (
    <section className="card" aria-hidden="true">
      <div className="card__body">
        {items.map((index) => (
          <div key={index} className="skeleton__field">
            <span className="skeleton__bar skeleton__bar--label" />
            <span className="skeleton__bar skeleton__bar--control" />
          </div>
        ))}
      </div>
    </section>
  );
}

/** An application record: the status panel with its rail, then one section. */
function record() {
  return (
    <div className="skeleton__stack" aria-hidden="true">
      <section className="card">
        <div className="card__body">
          <div className="skeleton__line">
            <span className="skeleton__bar skeleton__bar--badge" />
            <span className="skeleton__bar skeleton__bar--meta" />
          </div>
          <div className="skeleton__rail">
            {Array.from({ length: 5 }, (_, index) => (
              <span key={index} className="skeleton__node" />
            ))}
          </div>
        </div>
      </section>

      <section className="card">
        <div className="card__head">
          <span className="skeleton__bar skeleton__bar--title" />
        </div>
        <div className="card__body">
          {Array.from({ length: 3 }, (_, index) => (
            <div key={index} className="skeleton__line">
              <span className="skeleton__bar skeleton__bar--meta" />
              <span className="skeleton__bar skeleton__bar--time" />
            </div>
          ))}
        </div>
      </section>
    </div>
  );
}

/** The whole screen, for a wait that precedes any page. */
function page() {
  return (
    <div className="skeleton__page" aria-hidden="true">
      <span className="skeleton__bar skeleton__bar--title" />
      <span className="skeleton__bar skeleton__bar--meta" />
    </div>
  );
}
