import './Skeleton.css';

/** Placeholder cards at the geometry of the real ones, so nothing jumps when
 *  the data lands. */
export default function Skeleton({ count = 3, label = 'Chargement' }) {
  return (
    <div className="skeleton" role="status" aria-busy="true">
      <span className="visually-hidden">{label}</span>
      {Array.from({ length: count }, (_, index) => (
        <div key={index} className="skeleton__card" aria-hidden="true">
          <div className="skeleton__row">
            <span className="skeleton__bar skeleton__bar--title" />
            <span className="skeleton__bar skeleton__bar--badge" />
          </div>
          <span className="skeleton__bar skeleton__bar--meta" />
          <span className="skeleton__bar skeleton__bar--body" />
          <span className="skeleton__bar skeleton__bar--short" />
        </div>
      ))}
    </div>
  );
}
