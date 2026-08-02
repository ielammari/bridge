import { applicationsApi } from '../../api/applications.js';
import EmptyState from '../../components/EmptyState/EmptyState.jsx';
import ErrorState from '../../components/ErrorState/ErrorState.jsx';
import FunnelRail from '../../components/FunnelRail/FunnelRail.jsx';
import Skeleton from '../../components/Skeleton/Skeleton.jsx';
import StatusBadge from '../../components/StatusBadge/StatusBadge.jsx';
import { APPLICATION_ALERTS, CONTRACT_LABELS } from '../../constants/enums.js';
import { clockTime, longDate } from '../../constants/format.js';
import useResource from '../../hooks/useResource.js';
import Workspace from '../Workspace/Workspace.jsx';
import './candidateApplications.css';

// When an interview is booked, its date replaces the generic "date to come"
// line; a hire shows the start date.
function alertText(app) {
  if ((app.status === 'EXAMEN_TECHNIQUE' || app.status === 'ENTRETIEN_RH') && app.appointmentDate) {
    const label = app.status === 'EXAMEN_TECHNIQUE' ? 'Votre examen technique' : 'Votre entretien RH';
    return `${label} est fixé au ${longDate(app.appointmentDate)} à ${clockTime(app.appointmentTime)}.`;
  }
  if (app.status === 'EMBAUCHEE' && app.hiringStartDate) {
    return `Félicitations, vous êtes embauché(e). Prise de poste le ${longDate(app.hiringStartDate)}.`;
  }
  return APPLICATION_ALERTS[app.status];
}

export default function CandidateApplications() {
  const { status, data, reload } = useResource(() => applicationsApi.mine());

  return (
    <Workspace title="Mes candidatures">
      {status === 'loading' && <Skeleton label="Chargement de vos candidatures" />}

      {status === 'error' && (
        <ErrorState onRetry={reload}>
          Vos candidatures n'ont pas pu être chargées. Réessayez dans un instant.
        </ErrorState>
      )}

      {status === 'ready' && data.length === 0 && (
        <EmptyState
          title="Vous n'avez pas encore postulé."
          actionLabel="Voir les offres compatibles"
          actionTo="/offres"
        >
          Les offres qui correspondent à votre profil vous attendent. Chaque candidature envoyée
          apparaîtra ici avec son avancement.
        </EmptyState>
      )}

      {status === 'ready' && data.length > 0 && (
        <ul className="apps__list">
          {data.map((app) => (
            <li key={app.id} className="apptrack">
              <div className="apptrack__head">
                <div>
                  <h2 className="apptrack__title">{app.offerTitle}</h2>
                  <p className="apptrack__meta">
                    <span>{CONTRACT_LABELS[app.contractType]}</span>
                    {app.location && <span>{app.location}</span>}
                    <span>Envoyée le {longDate(app.applicationDate)}</span>
                  </p>
                </div>
                <StatusBadge status={app.status} />
              </div>

              <FunnelRail status={app.status} />

              <p className={`apptrack__alert${app.status === 'REFUSEE' ? ' apptrack__alert--refused' : ''}`}>
                {alertText(app)}
              </p>
            </li>
          ))}
        </ul>
      )}
    </Workspace>
  );
}
