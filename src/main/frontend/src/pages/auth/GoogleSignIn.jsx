import { useCallback, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import GoogleButton, { useGoogleClientId } from '../../components/GoogleButton/GoogleButton.jsx';
import { landingFor } from '../../components/ProtectedRoute/ProtectedRoute.jsx';
import { useAuth } from '../../context/AuthContext.jsx';

/**
 * The other way in, under the form it follows. Nothing is drawn until the
 * button exists, so a blocked script or a server carrying no client id leaves
 * the page as it was.
 */
export default function GoogleSignIn({ onError, suite }) {
  const { loginWithGoogle } = useAuth();
  const navigate = useNavigate();
  const clientId = useGoogleClientId();
  const [busy, setBusy] = useState(false);

  const accept = useCallback(async (credential) => {
    onError(null);
    setBusy(true);
    try {
      const user = await loginWithGoogle(credential);
      navigate(suite ?? landingFor(user), { replace: true });
    } catch (apiError) {
      onError(apiError.message);
      setBusy(false);
    }
  }, [loginWithGoogle, navigate, onError, suite]);

  if (!clientId) return null;

  return (
    <div className="auth__alt">
      <p className="auth__or"><span>ou</span></p>
      <GoogleButton clientId={clientId} onCredential={accept} busy={busy} />
    </div>
  );
}
