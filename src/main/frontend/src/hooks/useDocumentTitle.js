import { useEffect } from 'react';

const SUFFIX = 'Bridge';

/**
 * Names the current page in the tab, the history entry, and the first thing a
 * screen reader announces after a navigation.
 */
export default function useDocumentTitle(title) {
  useEffect(() => {
    document.title = title ? `${title} | ${SUFFIX}` : SUFFIX;
  }, [title]);
}
