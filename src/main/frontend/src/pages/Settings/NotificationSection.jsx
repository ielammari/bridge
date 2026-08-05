import { useState } from 'react';
import { settingsApi } from '../../api/settings.js';
import Checkbox from '../../components/Checkbox/Checkbox.jsx';
import ErrorState from '../../components/ErrorState/ErrorState.jsx';
import Skeleton from '../../components/Skeleton/Skeleton.jsx';
import { useToast } from '../../components/Toast/ToastContext.jsx';
import { NOTIFICATION_LABELS } from '../../constants/enums.js';
import useResource from '../../hooks/useResource.js';

/**
 * Which notifications reach the inbox. The list is scoped to the role, so it
 * only ever offers what this account actually receives, and a notification the
 * account cannot turn off is shown as such rather than hidden.
 */
export default function NotificationSection() {
  const toast = useToast();
  const [saving, setSaving] = useState(null);
  const { status, data, setData, reload, pending, leaving } = useResource(
    () => settingsApi.notifications(),
  );

  async function toggle(type, silenced) {
    const next = silenced
      ? [...data.silenced, type]
      : data.silenced.filter((t) => t !== type);

    setSaving(type);
    try {
      setData(await settingsApi.silence(next));
      toast.success(silenced ? 'Notification désactivée.' : 'Notification réactivée.');
    } catch (apiError) {
      toast.error(apiError.message);
    } finally {
      setSaving(null);
    }
  }

  return (
    <section className="card">
      <div className="card__head">
        <h2 className="card__title">Notifications</h2>
        <p className="card__subtitle">Ce qui vous est signalé dans votre boîte de réception.</p>
      </div>
      <div className="card__body">
        {pending && (
          <Skeleton variant="form" count={3} leaving={leaving}
            label="Chargement des notifications" />
        )}

        {status === 'error' && (
          <ErrorState onRetry={reload}>
            Vos préférences n'ont pas pu être chargées.
          </ErrorState>
        )}

        {status === 'ready' && (
          <>
            {data.silenceable.length > 0 && (
              <ul className="settings__toggles">
                {data.silenceable.map((type) => {
                  const silenced = data.silenced.includes(type);
                  return (
                    <li key={type}>
                      <Checkbox
                        label={NOTIFICATION_LABELS[type] ?? type}
                        checked={!silenced}
                        disabled={saving === type}
                        onChange={(event) => toggle(type, !event.target.checked)}
                      />
                    </li>
                  );
                })}
              </ul>
            )}

            {data.always.length > 0 && (
              <div className="settings__always">
                <p className="settings__always-title">Toujours transmises</p>
                <ul className="settings__toggles">
                  {data.always.map((type) => (
                    <li key={type}>
                      <Checkbox
                        label={NOTIFICATION_LABELS[type] ?? type}
                        checked
                        readOnly
                        disabled
                      />
                    </li>
                  ))}
                </ul>
                <p className="settings__note">
                  {data.silenceable.length > 0
                    ? 'Ces notifications portent sur une décision, elles vous parviennent toujours.'
                    : 'Toutes vos notifications portent sur vos propres candidatures : elles vous parviennent toujours.'}
                </p>
              </div>
            )}
          </>
        )}
      </div>
    </section>
  );
}
