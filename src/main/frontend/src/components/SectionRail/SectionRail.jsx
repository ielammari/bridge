import { useEffect, useRef, useState } from 'react';
import './SectionRail.css';

/**
 * An in page index for a long settings or profile screen. Each entry is a real
 * anchor, so the browser scrolls and the address names the section; the
 * highlight only reports where the reader is.
 */
export default function SectionRail({ sections, children }) {
  const [current, setCurrent] = useState(sections[0]?.id);
  const container = useRef(null);

  useEffect(() => {
    // The section being read is the last one whose top has passed under the
    // header. Sections the page runs out of scroll before reaching share what
    // is left in proportion to their height, so every entry answers for a
    // stretch of the page.
    function read() {
      const nodes = sections
        .map(({ id }) => document.getElementById(id))
        .filter(Boolean);
      if (nodes.length === 0) return;

      const workspace = container.current?.closest('.workspace');
      const header = workspace
        ? parseFloat(getComputedStyle(workspace).getPropertyValue('--header-h')) || 0
        : 0;
      const line = header + 24;
      const end = document.documentElement.scrollHeight - window.innerHeight;

      // Where each section takes over: the scroll that puts its top on the line.
      const starts = nodes
        .map((node) => node.getBoundingClientRect().top + window.scrollY - line);

      const short = starts.findIndex((start) => start > end);
      if (short >= 0) {
        const from = Math.max(0, short - 1);
        const tail = nodes.slice(from);
        const total = tail.reduce((sum, node) => sum + node.offsetHeight, 0) || 1;
        let start = Math.max(0, starts[from]);
        const room = Math.max(0, end - start);

        tail.forEach((node, index) => {
          start += (room * node.offsetHeight) / total;
          if (from + index + 1 < starts.length) starts[from + index + 1] = start;
        });
      }

      const passed = starts.filter((start) => start <= window.scrollY).length;
      setCurrent(nodes[Math.max(0, passed - 1)].id);
    }

    read();
    window.addEventListener('scroll', read, { passive: true });
    window.addEventListener('resize', read);
    return () => {
      window.removeEventListener('scroll', read);
      window.removeEventListener('resize', read);
    };
  }, [sections]);

  return (
    <div className="railpage" ref={container}>
      <nav className="railpage__rail" aria-label="Sections de la page">
        <ul className="railpage__list">
          {sections.map((section) => (
            <li key={section.id}>
              <a
                href={`#${section.id}`}
                className={`railpage__link${current === section.id ? ' railpage__link--on' : ''}`}
                aria-current={current === section.id ? 'true' : undefined}
              >
                {section.label}
              </a>
            </li>
          ))}
        </ul>
      </nav>

      <div className="railpage__content">{children}</div>
    </div>
  );
}
