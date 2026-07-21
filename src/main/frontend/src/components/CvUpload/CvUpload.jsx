import { useRef, useState } from 'react';
import Button from '../Button/Button.jsx';
import Icon from '../Icon/Icon.jsx';
import './CvUpload.css';

const MAX_BYTES = 5 * 1024 * 1024;

/**
 * Uploads, replaces, and downloads the candidate CV. Validates the file before
 * it leaves the browser, so the common mistakes fail instantly with a clear
 * reason rather than after a round trip.
 */
export default function CvUpload({ hasCv, onUpload, onDownload }) {
  const inputRef = useRef(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);

  function pick() {
    setError(null);
    inputRef.current?.click();
  }

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
      await onUpload(file);
    } catch (apiError) {
      setError(apiError.message);
    } finally {
      setBusy(false);
    }
  }

  async function download() {
    setError(null);
    try {
      const blob = await onDownload();
      const url = URL.createObjectURL(blob);
      window.open(url, '_blank', 'noopener');
      // Revoke on the next tick, once the new tab has taken the URL.
      setTimeout(() => URL.revokeObjectURL(url), 10_000);
    } catch (apiError) {
      setError(apiError.message);
    }
  }

  return (
    <div className="cv">
      <div className="cv__state">
        <span className={`cv__icon${hasCv ? ' cv__icon--present' : ''}`}>
          <Icon name="file" />
        </span>
        <div>
          <p className="cv__status">{hasCv ? 'CV déposé' : 'Aucun CV déposé'}</p>
          <p className="cv__note">
            {hasCv
              ? 'Votre CV sera joint à vos candidatures.'
              : 'PDF, 5 Mo maximum. Le CV devient obligatoire au moment de postuler.'}
          </p>
        </div>
      </div>

      <div className="cv__actions">
        {hasCv && (
          <Button variant="text" onClick={download}>
            <Icon name="download" /> Télécharger
          </Button>
        )}
        <Button variant="secondary" onClick={pick} loading={busy}>
          <Icon name="upload" /> {hasCv ? 'Remplacer' : 'Déposer un CV'}
        </Button>
      </div>

      <input
        ref={inputRef}
        type="file"
        accept="application/pdf"
        className="visually-hidden"
        onChange={handleFile}
        tabIndex={-1}
      />

      {error && (
        <p className="cv__error" role="alert">
          {error}
        </p>
      )}
    </div>
  );
}
