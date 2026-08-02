import { useEffect, useRef } from 'react';
import Button from '../Button/Button.jsx';
import FunnelRail from '../FunnelRail/FunnelRail.jsx';
import Icon from '../Icon/Icon.jsx';
import './ConfirmDialog.css';

/**
 * The second step in front of an action that cannot be taken back.
 *
 * Uses the native dialog element for the focus trap, Escape, and backdrop.
 * `nextStatus` renders the funnel rail at the state the action leads to.
 */
export default function ConfirmDialog({
  open,
  title,
  children,
  confirmLabel,
  cancelLabel = 'Annuler',
  tone = 'primary',
  nextStatus = null,
  missing = [],
  busy = false,
  onConfirm,
  onCancel,
}) {
  const ref = useRef(null);

  useEffect(() => {
    const dialog = ref.current;
    if (!dialog) return;
    if (open && !dialog.open) dialog.showModal();
    if (!open && dialog.open) dialog.close();
  }, [open]);

  if (!open) return null;

  return (
    <dialog
      ref={ref}
      className="confirm"
      onCancel={(event) => {
        event.preventDefault();
        onCancel();
      }}
    >
      <h2 className="confirm__title">{title}</h2>
      <div className="confirm__body">{children}</div>

      {nextStatus && (
        <div className="confirm__rail">
          <p className="confirm__rail-caption">Après cette action</p>
          <FunnelRail status={nextStatus} />
        </div>
      )}

      {missing.length > 0 && (
        <div className="confirm__missing">
          <p className="confirm__missing-title">
            <Icon name="warning" /> Ces champs facultatifs sont restés vides
          </p>
          <ul className="confirm__missing-list">
            {missing.map((item) => (
              <li key={item.key}>{item.label}</li>
            ))}
          </ul>
          <p className="confirm__missing-note">
            Vous pouvez continuer sans les remplir, ou revenir en arrière pour les compléter.
          </p>
        </div>
      )}

      <div className="confirm__actions">
        <Button variant="secondary" onClick={onCancel} disabled={busy}>
          {cancelLabel}
        </Button>
        <Button variant={tone === 'danger' ? 'danger' : 'primary'} onClick={onConfirm} loading={busy}>
          {confirmLabel}
        </Button>
      </div>
    </dialog>
  );
}
