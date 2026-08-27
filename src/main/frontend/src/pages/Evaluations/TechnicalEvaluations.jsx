import { useEffect } from 'react';
import { useLocation, useNavigate, useSearchParams } from 'react-router-dom';
import { evaluationsApi } from '../../api/evaluations.js';
import Button from '../../components/Button/Button.jsx';
import CardGrid from '../../components/CardGrid/CardGrid.jsx';
import EmptyState from '../../components/EmptyState/EmptyState.jsx';
import ErrorState from '../../components/ErrorState/ErrorState.jsx';
import Icon from '../../components/Icon/Icon.jsx';
import PersonLink from '../../components/PersonLink/PersonLink.jsx';
import OfferLink from '../../components/OfferLink/OfferLink.jsx';
import Skeleton from '../../components/Skeleton/Skeleton.jsx';
import { useToast } from '../../components/Toast/ToastContext.jsx';
import { clockTime, shortDate } from '../../constants/format.js';
import { returnLink } from '../../constants/navigation.js';
import { isDue } from '../../constants/schedule.js';
import useResource from '../../hooks/useResource.js';
import Workspace from '../Workspace/Workspace.jsx';
import './technicalEvaluations.css';

function whenText(app) {
  return `Examen le ${shortDate(app.appointmentDate)} à ${clockTime(app.appointmentTime)}`;
}

export default function TechnicalEvaluations() {
  const navigate = useNavigate();
  const location = useLocation();
  const toast = useToast();
  const [params] = useSearchParams();
  // Where this list currently stands, so an evaluation opened from it can
  // return to the same view.
  const origin = { from: `${location.pathname}${location.search}` };
  // Followed from the calendar, which names one exam among several.
  const named = Number(params.get('fiche')) || null;
  const { status, data, reload, pending: loading, leaving } = useResource(
    () => evaluationsApi.pendingTechnical(),
  );

  const pending = data ?? [];

  useEffect(() => {
    if (!named || status !== 'ready') return;
    document.getElementById(`exam-${named}`)?.scrollIntoView({ block: 'nearest' });
  }, [named, status]);

  // The CV attached to the application, opened from the exam itself.
  async function viewCv(applicationId) {
    try {
      const blob = await evaluationsApi.cv(applicationId);
      const url = URL.createObjectURL(blob);
      window.open(url, '_blank', 'noopener');
      setTimeout(() => URL.revokeObjectURL(url), 10_000);
    } catch (apiError) {
      toast.error(apiError.message);
    }
  }

  return (
    <Workspace title="Évaluations" returnTo={returnLink(location.state)}>

      {loading && <Skeleton leaving={leaving} label="Chargement des évaluations" />}

      {status === 'error' && (
        <ErrorState onRetry={reload}>
          Les évaluations n'ont pas pu être chargées. Réessayez dans un instant.
        </ErrorState>
      )}

      {status === 'ready' && pending.length === 0 && (
        <EmptyState title="Aucun examen ne vous est attribué.">
          Un examen apparaît ici lorsqu'un recruteur vous le confie en fixant sa date. Vous serez
          prévenu à ce moment.
        </EmptyState>
      )}

      {status === 'ready' && pending.length > 0 && (
        <CardGrid label="Évaluations en attente">
          {pending.map((app) => (
            <li key={app.applicationId} id={`exam-${app.applicationId}`}
              className={`tile${app.applicationId === named ? ' tile--named' : ''}`}>
              <div className="tile__head">
                <h2 className="tile__title">
                  <PersonLink id={app.candidateId}>{app.candidateFirstName} {app.candidateLastName}</PersonLink>
                </h2>
              </div>
              <p className="tile__facts">
                <span><OfferLink id={app.offerId}>{app.offerTitle}</OfferLink></span>
                <span>{whenText(app)}</span>
              </p>
              <div className="tile__foot evals__foot">
                {!isDue(app) && <span className="tile__blocked">Pas encore eu lieu</span>}
                <Button variant="secondary" onClick={() => viewCv(app.applicationId)}>
                  <Icon name="download" /> CV
                </Button>
                <Button disabled={!isDue(app)}
                  onClick={() => navigate(`/evaluations/${app.applicationId}`, { state: origin })}>
                  Évaluer
                </Button>
              </div>
            </li>
          ))}
        </CardGrid>
      )}
    </Workspace>
  );
}
