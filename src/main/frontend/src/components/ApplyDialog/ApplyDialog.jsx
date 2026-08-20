import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import ConfirmDialog from '../ConfirmDialog/ConfirmDialog.jsx';
import { shortDate } from '../../constants/format.js';
import './ApplyDialog.css';

/**
 * Confirms an application and settles the CV it carries: a choice between
 * several, a statement when there is one, a link to the profile when there is
 * none, since a CV is required to apply.
 */
export default function ApplyDialog({ offer, documents, busy, onConfirm, onCancel }) {
  const navigate = useNavigate();
  const preferred = documents.find((cv) => cv.isDefault) ?? documents[0];
  const [chosen, setChosen] = useState(preferred?.id ?? null);

  if (documents.length === 0) {
    return (
      <ConfirmDialog
        open
        title="Un CV est nécessaire pour postuler"
        confirmLabel="Déposer un CV"
        cancelLabel="Fermer"
        onConfirm={() => navigate('/profil')}
        onCancel={onCancel}
      >
        <p>
          Votre candidature pour l'offre <strong>{offer.title}</strong> doit être accompagnée
          d'un CV. Ajoutez-en un à votre profil, puis revenez postuler.
        </p>
      </ConfirmDialog>
    );
  }

  return (
    <ConfirmDialog
      open
      title="Envoyer votre candidature ?"
      confirmLabel="Envoyer la candidature"
      busy={busy}
      onConfirm={() => onConfirm(chosen)}
      onCancel={onCancel}
    >
      <p>
        Votre profil et le CV joint seront transmis pour l'offre{' '}
        <strong>{offer.title}</strong>.
      </p>

      {documents.length > 1 ? (
        <fieldset className="applycv">
          <legend className="applycv__legend">CV à joindre</legend>
          {documents.map((cv) => (
            <label
              key={cv.id}
              className={`applycv__choice${chosen === cv.id ? ' applycv__choice--on' : ''}`}
            >
              <input
                type="radio"
                name="cv"
                className="visually-hidden"
                checked={chosen === cv.id}
                onChange={() => setChosen(cv.id)}
              />
              <span className="applycv__name">{cv.label}</span>
              <span className="applycv__date">{shortDate(cv.uploadedAt)}</span>
            </label>
          ))}
        </fieldset>
      ) : (
        <p className="applycv__single">CV joint : <strong>{preferred.label}</strong>.</p>
      )}
    </ConfirmDialog>
  );
}
