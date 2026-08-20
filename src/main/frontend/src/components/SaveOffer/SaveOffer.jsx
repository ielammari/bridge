import { useState } from 'react';
import Icon from '../Icon/Icon.jsx';
import { offersApi } from '../../api/offers.js';
import './SaveOffer.css';

/**
 * Keeps an offer, or releases it. The mark flips on the press and is put back
 * only if the write fails.
 */
export default function SaveOffer({ offerId, saved, onChange }) {
  const [kept, setKept] = useState(saved);
  const [busy, setBusy] = useState(false);

  async function toggle(event) {
    event.preventDefault();
    event.stopPropagation();
    if (busy) return;

    const next = !kept;
    setKept(next);
    setBusy(true);
    try {
      await (next ? offersApi.save(offerId) : offersApi.unsave(offerId));
      onChange?.(offerId, next);
    } catch {
      setKept(!next);
    } finally {
      setBusy(false);
    }
  }

  return (
    <button
      type="button"
      className={`saveoffer${kept ? ' saveoffer--on' : ''}`}
      onClick={toggle}
      aria-pressed={kept}
      title={kept ? 'Retirer des offres enregistrées' : 'Enregistrer cette offre'}
      aria-label={kept ? 'Retirer des offres enregistrées' : 'Enregistrer cette offre'}
    >
      <Icon name="bookmark" />
    </button>
  );
}
