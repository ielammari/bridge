import { useEffect, useRef, useState } from 'react';
import { authApi } from '../../api/auth.js';
import './GoogleButton.css';

const SCRIPT_SRC = 'https://accounts.google.com/gsi/client';

/** Google's own bounds for a rendered button. */
const MIN_WIDTH = 200;
const MAX_WIDTH = 400;

let script = null;
let configured = null;

function loadScript() {
  if (!script) {
    script = new Promise((resolve, reject) => {
      const tag = document.createElement('script');
      tag.src = SCRIPT_SRC;
      tag.async = true;
      tag.onload = resolve;
      tag.onerror = reject;
      document.head.appendChild(tag);
    });
  }
  return script;
}

/**
 * The client id this deployment carries, or null where Google is not
 * configured or its script never arrived. It never changes within a session,
 * so it is read once and shared. A caller uses it to decide whether to offer
 * Google at all.
 */
export function useGoogleClientId() {
  const [clientId, setClientId] = useState(null);

  useEffect(() => {
    let cancelled = false;
    if (!configured) {
      configured = authApi.providers().then((providers) => providers.googleClientId);
    }
    configured
      .then((id) => (id ? loadScript().then(() => id) : null))
      .then((id) => {
        if (id && !cancelled) setClientId(id);
      })
      .catch(() => {});
    return () => {
      cancelled = true;
    };
  }, []);

  return clientId;
}

/**
 * Google's own button, which it draws inside an iframe this application cannot
 * style. The row around it holds the height of our own buttons so a column
 * keeps one rhythm, and the width Google is asked for is measured from that
 * row, because Google takes pixels rather than a proportion.
 *
 * It wears Google's light button in both themes, so the mark people recognise
 * looks the same wherever it appears.
 */
export default function GoogleButton({ clientId, onCredential, busy = false }) {
  const host = useRef(null);
  const [width, setWidth] = useState(0);

  // Read through a ref, so a new callback identity never re-renders the button.
  const deliver = useRef(onCredential);
  deliver.current = onCredential;

  useEffect(() => {
    const node = host.current;
    if (!node) return undefined;

    const observer = new ResizeObserver(([entry]) => {
      setWidth(Math.round(entry.contentRect.width));
    });
    observer.observe(node);
    return () => observer.disconnect();
  }, []);

  useEffect(() => {
    const node = host.current;
    const google = window.google?.accounts?.id;
    if (!clientId || !width || !node || !google) return;

    google.initialize({
      client_id: clientId,
      callback: (response) => deliver.current(response.credential),
    });

    node.replaceChildren();
    google.renderButton(node, {
      type: 'standard',
      theme: 'outline',
      size: 'large',
      text: 'continue_with',
      shape: 'rectangular',
      logo_alignment: 'center',
      locale: 'fr',
      width: Math.min(Math.max(width, MIN_WIDTH), MAX_WIDTH),
    });
  }, [clientId, width]);

  return (
    <div
      className={`gbutton${busy ? ' gbutton--busy' : ''}`}
      aria-busy={busy || undefined}
      ref={host}
    />
  );
}
