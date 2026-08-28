import { settingsApi } from '../../api/settings.js';
import ErrorState from '../../components/ErrorState/ErrorState.jsx';
import { useGoogleClientId } from '../../components/GoogleButton/GoogleButton.jsx';
import SectionRail from '../../components/SectionRail/SectionRail.jsx';
import Skeleton from '../../components/Skeleton/Skeleton.jsx';
import { useAuth } from '../../context/AuthContext.jsx';
import useResource from '../../hooks/useResource.js';
import Workspace from '../Workspace/Workspace.jsx';
import AccountSection from './AccountSection.jsx';
import AppearanceSection from './AppearanceSection.jsx';
import GoogleSection from './GoogleSection.jsx';
import NotificationSection from './NotificationSection.jsx';
import OrganisationSection from './OrganisationSection.jsx';
import PasswordSection from './PasswordSection.jsx';
import ProvisionSection from './ProvisionSection.jsx';
import './settings.css';

/**
 * Everything an actor configures, in sections that save one at a time: a phone
 * number and a password are not the same act.
 */
export default function Settings() {
  const { user } = useAuth();
  const isHr = user.role === 'RH';

  // Google signs in candidates, and only where this deployment carries a
  // client id, so the entry exists exactly when the section does.
  const google = Boolean(useGoogleClientId()) && user.role === 'CANDIDAT';

  const { status, data, reload, pending, leaving } = useResource(() => settingsApi.account());

  // Built from what this role gets, so it can never name a section that is not
  // on the page.
  const sections = [
    { id: 'compte', label: 'Compte' },
    { id: 'securite', label: 'Mot de passe' },
    ...(google ? [{ id: 'google', label: 'Compte Google' }] : []),
    // A candidate's two are short and both say how the application behaves for
    // them, so they share an entry; a recruiter's notifications hold their own.
    ...(isHr
      ? [{ id: 'notifications', label: 'Notifications' }, { id: 'apparence', label: 'Apparence' }]
      : [{ id: 'notifications', label: 'Préférences' }]),
    // One entry for the two sections that configure the company rather than the
    // account holder.
    ...(isHr ? [{ id: 'entretiens', label: 'Organisation' }] : []),
  ];

  return (
    <Workspace title="Paramètres">
      {pending && <Skeleton variant="form" count={4} leaving={leaving} label="Chargement des paramètres" />}

      {status === 'error' && (
        <ErrorState onRetry={reload}>
          Vos paramètres n'ont pas pu être chargés. Réessayez dans un instant.
        </ErrorState>
      )}

      {status === 'ready' && (
        <SectionRail sections={sections}>
          <div id="compte"><AccountSection account={data} /></div>
          <div id="securite"><PasswordSection /></div>
          {google && <div id="google"><GoogleSection /></div>}
          <div id="notifications"><NotificationSection /></div>
          <div id="apparence"><AppearanceSection /></div>
          {isHr && <div id="entretiens"><OrganisationSection /></div>}
          {isHr && <div id="comptes"><ProvisionSection /></div>}
        </SectionRail>
      )}
    </Workspace>
  );
}
