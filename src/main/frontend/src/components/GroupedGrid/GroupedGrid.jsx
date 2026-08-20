import CardGrid from '../CardGrid/CardGrid.jsx';
import './GroupedGrid.css';

/**
 * A listing split into labelled sections. A single group hides its heading but
 * still renders it, so the document outline keeps a level between the page
 * title and the cards.
 */
export default function GroupedGrid({ sections, size, render }) {
  if (sections.length === 1) {
    return (
      <section className="grouped__section">
        <h2 className="visually-hidden">{sections[0].label}</h2>
        <CardGrid size={size} label={sections[0].label}>
          {sections[0].items.map(render)}
        </CardGrid>
      </section>
    );
  }

  return (
    <div className="grouped">
      {sections.map((section) => (
        <section key={section.key} className="grouped__section">
          <h2 className="grouped__heading">
            {section.label}
            <span className="grouped__count mono">{section.items.length}</span>
          </h2>
          <CardGrid size={size} label={section.label}>
            {section.items.map(render)}
          </CardGrid>
        </section>
      ))}
    </div>
  );
}
