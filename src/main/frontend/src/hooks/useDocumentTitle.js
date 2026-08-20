import { useEffect } from 'react';

const SUFFIX = 'Bridge';

/**
 * Names the page in the tab, in the history entry, and in what a screen reader
 * announces after a navigation.
 */
export default function useDocumentTitle(title) {
  useEffect(() => {
    document.title = title ? `${title} | ${SUFFIX}` : SUFFIX;
  }, [title]);
}
