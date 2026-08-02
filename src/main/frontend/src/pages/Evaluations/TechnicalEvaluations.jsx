import { useNavigate } from 'react-router-dom';
import { evaluationsApi } from '../../api/evaluations.js';
import Button from '../../components/Button/Button.jsx';
import EmptyState from '../../components/EmptyState/EmptyState.jsx';
import ErrorState from '../../components/ErrorState/ErrorState.jsx';
import Skeleton from '../../components/Skeleton/Skeleton.jsx';
import { clockTime, shortDate } from '../../constants/format.js';
import useResource from '../../hooks/useResource.js';
import Workspace from '../Workspace/Workspace.jsx';
import './technicalEvaluations.css';

function whenText(app) {
  if (!app.appointmentDate) return 'Examen non planifié';
  return `Examen le ${shortDate(app.appointmentDate)} à ${clockTime(app.appointmentTime)}`;
}

export default function TechnicalEvaluations() {
  const navigate = useNavigate();
  const { status, data, reload } = useResource(() => evaluationsApi.pendingTechnical());

  const pending = data ?? [];

  return (
    <Workspace title="Évaluations">
      <p className="tech__intro">Candidats à évaluer après leur examen technique.</p>

      {status === 'loading' && <Skeleton label="Chargement des évaluations" />}

      {status === 'error' && (
        <ErrorState onRetry={reload}>
          Les évaluations n'ont pas pu être chargées. Réessayez dans un instant.
        </ErrorState>
      )}

      {status === 'ready' && pending.length === 0 && (
        <EmptyState title="Aucune évaluation technique en attente.">
          Les candidats validés par les RH apparaîtront ici dès que leur examen technique sera
          planifié.
        </EmptyState>
      )}

      {status === 'ready' && pending.length > 0 && (
        <ul className="tech__list">
          {pending.map((app) => (
            <li key={app.applicationId} className="techcard">
              <div>
                <span className="techcard__name">{app.candidateFirstName} {app.candidateLastName}</span>
                <span className="techcard__meta">{app.offerTitle} · {whenText(app)}</span>
              </div>
              <Button onClick={() => navigate(`/evaluations/${app.applicationId}`)}>Évaluer</Button>
            </li>
          ))}
        </ul>
      )}
    </Workspace>
  );
}
