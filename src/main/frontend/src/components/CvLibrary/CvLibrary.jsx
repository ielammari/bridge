import { useRef, useState } from 'react';
import Button from '../Button/Button.jsx';
import ConfirmDialog from '../ConfirmDialog/ConfirmDialog.jsx';
import Icon from '../Icon/Icon.jsx';
import { shortDate } from '../../constants/format.js';
import './CvLibrary.css';

const MAX_BYTES = 5 * 1024 * 1024;

/**
 * The CVs a candidate keeps on file. One is the default, which an application
 * proposes, and the rest stay available to pick at that moment. Documents are
 * validated before they leave the browser.
 */
export default function CvLibrary({ documents, onUpload, onChoose, onRemove, onOpen }) {
  const input = useRef(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);
  const [confirming, setConfirming] = useState(null);

  async function handleFile(event) {
    const file = event.target.files?.[0];
    event.target.value = ''; // allow re-selecting the same file after a fix
    if (!file) return;

    if (file.type !== 'application/pdf') {
      setError('Le CV doit être un fichier PDF.');
      return;
    }
    if (file.size > MAX_BYTES) {
      setError('Le CV dépasse la taille maximale de 5 Mo.');
      return;
    }

    setError(null);
    setBusy(true);
    try {
      await onUpload(file, file.name.replace(/\.pdf$/i, '').slice(0, 120));
    } catch (apiError) {
      setError(apiError.message);
    } finally {
      setBusy(false);
    }
  }

  async function run(action) {
    setError(null);
    setBusy(true);
    try {
      await action();
    } catch (apiError) {
      setError(apiError.message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="cvlib">
      {documents.length === 0 ? (
        <p className="cvlib__empty">
          Aucun CV déposé. PDF, 5 Mo maximum.
        </p>
      ) : (
        <ul className="cvlib__list">
          {documents.map((cv) => (
            <li key={cv.id} className={`cvlib__item${cv.isDefault ? ' cvlib__item--default' : ''}`}>
              <span className="cvlib__icon"><Icon name="file" /></span>

              <div className="cvlib__what">
                <p className="cvlib__label">
                  {cv.label}
                  {cv.isDefault && <span className="cvlib__badge">Par défaut</span>}
                </p>
                <p className="cvlib__date">Déposé le {shortDate(cv.uploadedAt)}</p>
              </div>

              <div className="cvlib__actions">
                <Button variant="text" onClick={() => onOpen(cv.id)}>Ouvrir</Button>
                {!cv.isDefault && (
                  <Button variant="text" onClick={() => run(() => onChoose(cv.id))}>
                    Par défaut
                  </Button>
                )}
                <Button variant="text" onClick={() => setConfirming(cv)}>Supprimer</Button>
              </div>
            </li>
          ))}
        </ul>
      )}

      {error && <p className="cvlib__error" role="alert">{error}</p>}

      <Button variant="secondary" onClick={() => input.current?.click()} loading={busy}>
        <Icon name="upload" /> Ajouter un CV
      </Button>

      <input
        ref={input}
        type="file"
        accept="application/pdf"
        className="visually-hidden"
        onChange={handleFile}
        tabIndex={-1}
      />

      {confirming && (
        <ConfirmDialog
          open
          title="Supprimer ce CV ?"
          confirmLabel="Supprimer le CV"
          tone="danger"
          busy={busy}
          onConfirm={async () => {
            await run(() => onRemove(confirming.id));
            setConfirming(null);
          }}
          onCancel={() => setConfirming(null)}
        >
          <strong>{confirming.label}</strong> sera retiré de votre profil. Les candidatures déjà
          envoyées gardent le CV qui leur a été joint.
        </ConfirmDialog>
      )}
    </div>
  );
}
