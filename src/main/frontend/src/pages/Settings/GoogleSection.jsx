import { useState } from 'react';
import { settingsApi } from '../../api/settings.js';
import Button from '../../components/Button/Button.jsx';
import GoogleButton, { useGoogleClientId } from '../../components/GoogleButton/GoogleButton.jsx';
import { useToast } from '../../components/Toast/ToastContext.jsx';
import { useAuth } from '../../context/AuthContext.jsx';

/**
 * The Google identity this account signs in with. Associating one is a
 * deliberate act taken from a session that already proved the password, which
 * is why signing in with Google never links anything by itself.
 */
export default function GoogleSection() {
  const { user, refresh } = useAuth();
  const toast = useToast();
  const clientId = useGoogleClientId();
  const [working, setWorking] = useState(false);

  // Reserved for candidates, the only accounts Google signs in.
  if (!clientId || user.role !== 'CANDIDAT') return null;

  const link = async (credential) => {
    setWorking(true);
    try {
      await settingsApi.linkGoogle(credential);
      await refresh();
      toast.success('Compte Google associé.');
    } catch (apiError) {
      toast.error(apiError.message);
    } finally {
      setWorking(false);
    }
  };

  const unlink = async () => {
    setWorking(true);
    try {
      await settingsApi.unlinkGoogle();
      await refresh();
      toast.success('Compte Google dissocié.');
    } catch (apiError) {
      toast.error(apiError.message);
    } finally {
      setWorking(false);
    }
  };

  return (
    <section className="card">
      <div className="card__head">
        <h2 className="card__title">Compte Google</h2>
      </div>
      <div className="card__body">
        {user.googleLinked ? (
          <>
            <p className="settings__note">
              Un compte Google est associé. Vous pouvez vous connecter avec lui.
            </p>
            <div className="settings__actions">
              {!user.hasPassword && (
                <p className="settings__blocked">Définissez d'abord un mot de passe.</p>
              )}
              <Button
                variant="danger"
                onClick={unlink}
                loading={working}
                disabled={!user.hasPassword}
              >
                Dissocier
              </Button>
            </div>
          </>
        ) : (
          <>
            <p className="settings__note">
              Associez un compte Google pour vous connecter sans mot de passe.
            </p>
            <div className="settings__google">
              <GoogleButton clientId={clientId} onCredential={link} busy={working} />
            </div>
          </>
        )}
      </div>
    </section>
  );
}
