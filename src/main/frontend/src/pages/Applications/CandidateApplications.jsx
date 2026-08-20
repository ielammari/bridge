import { useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import { applicationsApi } from '../../api/applications.js';
import CardGrid from '../../components/CardGrid/CardGrid.jsx';
import EmptyState from '../../components/EmptyState/EmptyState.jsx';
import ErrorState from '../../components/ErrorState/ErrorState.jsx';
import FunnelRail from '../../components/FunnelRail/FunnelRail.jsx';
import OfferLink from '../../components/OfferLink/OfferLink.jsx';
import Skeleton from '../../components/Skeleton/Skeleton.jsx';
import StatusBadge from '../../components/StatusBadge/StatusBadge.jsx';
import { APPLICATION_ALERTS, CONTRACT_LABELS, isTerminal } from '../../constants/enums.js';
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
  const [params] = useSearchParams();
  const { status, data, reload, pending, leaving } = useResource(() => applicationsApi.mine());

  // Closed applications move to the history, where their full record lives.
  const active = (data ?? []).filter((app) => !isTerminal(app.status));

  // Followed from a notice, which names one application among several.
  const named = Number(params.get('candidature')) || null;

  useEffect(() => {
    if (!named || status !== 'ready') return;
    document.getElementById(`candidature-${named}`)?.scrollIntoView({ block: 'nearest' });
  }, [named, status]);

  return (
    <Workspace title="Mes candidatures">
      {pending && <Skeleton size="full" leaving={leaving} label="Chargement de vos candidatures" />}

      {status === 'error' && (
        <ErrorState onRetry={reload}>
          Vos candidatures n'ont pas pu être chargées. Réessayez dans un instant.
        </ErrorState>
      )}

      {status === 'ready' && active.length === 0 && (
        <EmptyState
          title="Aucune candidature en cours."
          actionLabel="Voir les offres compatibles"
          actionTo="/offres"
        >
          Les candidatures en cours apparaissent ici avec leur avancement. Celles qui sont closes
          sont consultables dans l'historique.
        </EmptyState>
      )}

      {status === 'ready' && active.length > 0 && (
        <CardGrid size="full" label="Vos candidatures">
          {active.map((app) => (
            <li key={app.id} id={`candidature-${app.id}`}
              className={`tile${app.id === named ? ' tile--named' : ''}`}>
              <div className="tile__head">
                <div>
                  <h2 className="tile__title">
                    <OfferLink id={app.offerId}>{app.offerTitle}</OfferLink>
                  </h2>
                  <p className="tile__facts">
                    <span>{CONTRACT_LABELS[app.contractType]}</span>
                    {app.location && <span>{app.location}</span>}
                    <span>Envoyée le {longDate(app.applicationDate)}</span>
                  </p>
                </div>
                <StatusBadge status={app.status} />
              </div>

              {/* The rail takes the width it needs and the alert sits beside it,
                  rather than under it across an empty band. */}
              <div className="apptrack__progress">
                <FunnelRail status={app.status} />
                <p className={`apptrack__alert${app.status === 'REFUSEE' ? ' apptrack__alert--refused' : ''}`}>
                  {alertText(app)}
                </p>
              </div>
            </li>
          ))}
        </CardGrid>
      )}
    </Workspace>
  );
}
