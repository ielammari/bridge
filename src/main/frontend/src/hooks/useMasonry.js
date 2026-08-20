import { useCallback, useEffect, useRef } from 'react';

/**
 * Lays a container's children into columns, each item placed under the shortest
 * column so far. Items are positioned individually and animate to their new
 * place when any of them changes height, so a neighbour rises into the space an
 * expanding item opens.
 */
export default function useMasonry(columns, gap) {
  const container = useRef(null);
  const frame = useRef(0);

  const layout = useCallback(() => {
    const root = container.current;
    if (!root) return;

    const items = [...root.children];
    if (items.length === 0) return;

    // One column means the browser's own flow is already right, so the
    // positioning is taken back off rather than recomputed.
    if (columns < 2) {
      root.style.height = '';
      items.forEach((item) => {
        item.style.position = '';
        item.style.width = '';
        item.style.transform = '';
      });
      return;
    }

    const width = (root.clientWidth - gap * (columns - 1)) / columns;
    const heights = new Array(columns).fill(0);

    items.forEach((item) => {
      item.style.position = 'absolute';
      item.style.width = `${width}px`;

      const shortest = heights.indexOf(Math.min(...heights));
      const x = shortest * (width + gap);
      const y = heights[shortest];
      item.style.transform = `translate(${x}px, ${y}px)`;
      heights[shortest] += item.offsetHeight + gap;
    });

    root.style.height = `${Math.max(...heights) - gap}px`;
  }, [columns, gap]);

  // Coalesced to one measurement per frame: an expanding panel fires the
  // observer for its own resize and for the container's at the same moment.
  const schedule = useCallback(() => {
    cancelAnimationFrame(frame.current);
    frame.current = requestAnimationFrame(layout);
  }, [layout]);

  useEffect(() => {
    const root = container.current;
    if (!root) return undefined;

    const observer = new ResizeObserver(schedule);
    observer.observe(root);
    [...root.children].forEach((child) => observer.observe(child));
    schedule();

    return () => {
      observer.disconnect();
      cancelAnimationFrame(frame.current);
    };
  });

  return container;
}
