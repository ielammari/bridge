import { useEffect } from 'react';

const SUFFIX = 'Bridge';
const NAMED = new RegExp(`\\b${SUFFIX}\\b`, 'i');

/**
 * Names the page in the tab, in the history entry, and in what a screen reader
 * announces after a navigation. A title that already names Bridge stands alone.
 */
export default function useDocumentTitle(title) {
  useEffect(() => {
    if (!title) document.title = SUFFIX;
    else document.title = NAMED.test(title) ? title : `${title} | ${SUFFIX}`;
  }, [title]);
}
