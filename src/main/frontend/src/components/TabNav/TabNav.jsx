import { NavLink } from 'react-router-dom';
import './TabNav.css';

/**
 * Secondary navigation inside a section. The sidebar carries one entry per
 * section; this splits the section itself.
 */
export default function TabNav({ items, label }) {
  if (items.length < 2) return null;

  return (
    <nav className="tabnav" aria-label={label}>
      {items.map((item) => (
        <NavLink
          key={item.to}
          to={item.to}
          className={({ isActive }) => `tabnav__tab${isActive ? ' tabnav__tab--active' : ''}`}
        >
          {item.label}
        </NavLink>
      ))}
    </nav>
  );
}
