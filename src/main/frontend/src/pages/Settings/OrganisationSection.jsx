import { useState } from 'react';
import { settingsApi } from '../../api/settings.js';
import Button from '../../components/Button/Button.jsx';
import ErrorState from '../../components/ErrorState/ErrorState.jsx';
import Field from '../../components/Field/Field.jsx';
import InfoHint from '../../components/InfoHint/InfoHint.jsx';
import Skeleton from '../../components/Skeleton/Skeleton.jsx';
import { useToast } from '../../components/Toast/ToastContext.jsx';
import useResource from '../../hooks/useResource.js';

/** The hourly grid every interview in the company is booked onto. */
export default function OrganisationSection() {
  const toast = useToast();
  const [saving, setSaving] = useState(false);
  const [hours, setHours] = useState(null);
  const { status, reload, pending, leaving } = useResource(async () => {
    const settings = await settingsApi.organisation();
    setHours({ firstHour: String(settings.firstHour), lastHour: String(settings.lastHour) });
    return settings;
  });

  async function save(event) {
    event.preventDefault();
    setSaving(true);
    try {
      const saved = await settingsApi.updateOrganisation({
        firstHour: Number(hours.firstHour),
        lastHour: Number(hours.lastHour),
      });
      setHours({ firstHour: String(saved.firstHour), lastHour: String(saved.lastHour) });
      toast.success('Plage horaire mise à jour.');
    } catch (apiError) {
      toast.error(apiError.message);
    } finally {
      setSaving(false);
    }
  }

  return (
    <form className="card" onSubmit={save} noValidate>
      <div className="card__head">
        <h2 className="card__title">
          Plage des entretiens
          <InfoHint label="À propos de la plage">
            Les heures proposées lors de la planification, pour toute l'entreprise.
          </InfoHint>
        </h2>
      </div>
      <div className="card__body">
        {pending && <Skeleton variant="form" count={2} leaving={leaving} label="Chargement de la plage horaire" />}

        {status === 'error' && (
          <ErrorState onRetry={reload}>La plage horaire n'a pas pu être chargée.</ErrorState>
        )}

        {status === 'ready' && hours && (
          <>
            <div className="settings__grid">
              <Field label="Première heure" type="number" min="0" max="23" name="firstHour"
                value={hours.firstHour}
                onChange={(e) => setHours({ ...hours, firstHour: e.target.value })} />
              <Field label="Dernière heure" type="number" min="0" max="23" name="lastHour"
                value={hours.lastHour}
                onChange={(e) => setHours({ ...hours, lastHour: e.target.value })} />
            </div>
            <div className="settings__actions">
              <Button type="submit" loading={saving}>Enregistrer la plage</Button>
            </div>
          </>
        )}
      </div>
    </form>
  );
}
