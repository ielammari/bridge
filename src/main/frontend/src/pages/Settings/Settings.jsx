import { settingsApi } from '../../api/settings.js';
import ErrorState from '../../components/ErrorState/ErrorState.jsx';
import Skeleton from '../../components/Skeleton/Skeleton.jsx';
import { useAuth } from '../../context/AuthContext.jsx';
import useResource from '../../hooks/useResource.js';
import Workspace from '../Workspace/Workspace.jsx';
import AccountSection from './AccountSection.jsx';
import AppearanceSection from './AppearanceSection.jsx';
import NotificationSection from './NotificationSection.jsx';
import OrganisationSection from './OrganisationSection.jsx';
import PasswordSection from './PasswordSection.jsx';
import ProvisionSection from './ProvisionSection.jsx';
import './settings.css';

/**
 * Everything an actor configures, in sections that save one at a time: a single
 * save button over the whole page would make changing a phone number and
 * changing a password feel like the same act.
 */
export default function Settings() {
  const { user } = useAuth();
  const isHr = user.role === 'RH';

  const { status, data, reload, pending, leaving } = useResource(() => settingsApi.account());

  return (
    <Workspace width="narrow" title="Paramètres">
      {pending && <Skeleton variant="form" count={4} leaving={leaving} label="Chargement des paramètres" />}

      {status === 'error' && (
        <ErrorState onRetry={reload}>
          Vos paramètres n'ont pas pu être chargés. Réessayez dans un instant.
        </ErrorState>
      )}

      {status === 'ready' && (
        <>
          <AccountSection account={data} />
          <PasswordSection />
          <NotificationSection />
          <AppearanceSection />
          {isHr && <OrganisationSection />}
          {isHr && <ProvisionSection />}
        </>
      )}
    </Workspace>
  );
}
