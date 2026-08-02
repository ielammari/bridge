import { useNavigate, useParams } from 'react-router-dom';
import { applicationsApi } from '../../api/applications.js';
import EmptyState from '../../components/EmptyState/EmptyState.jsx';
import ErrorState from '../../components/ErrorState/ErrorState.jsx';
import Skeleton from '../../components/Skeleton/Skeleton.jsx';
import { useToast } from '../../components/Toast/ToastContext.jsx';
import useResource from '../../hooks/useResource.js';
import Workspace from '../Workspace/Workspace.jsx';
import FinalEvaluation from './FinalEvaluation.jsx';
import './finalEvaluation.css';

const BACK = { to: '/candidatures', label: 'Retour aux candidatures' };

/** The final HR interview for one application, at its own address. */
export default function FinalEvaluationPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const toast = useToast();

  const { status, data, reload } = useResource(() => applicationsApi.get(id), [id]);

  const title = 'Entretien final';

  if (status !== 'ready') {
    return (
      <Workspace title={title} back={BACK}>
        {status === 'loading' ? (
          <Skeleton count={2} label="Chargement de la candidature" />
        ) : (
          <ErrorState onRetry={reload}>
            Cette candidature n'a pas pu être chargée. Réessayez dans un instant.
          </ErrorState>
        )}
      </Workspace>
    );
  }

  const name = `${data.candidateFirstName} ${data.candidateLastName}`;

  if (data.status !== 'ENTRETIEN_RH') {
    return (
      <Workspace title={title} subtitle={name} back={BACK}>
        <EmptyState
          title="Cette candidature n'est plus à l'étape de l'entretien final."
          actionLabel="Retour aux candidatures"
          actionTo="/candidatures"
        >
          Son étape actuelle ne permet pas de saisir un bilan d'entretien.
        </EmptyState>
      </Workspace>
    );
  }

  return (
    <Workspace title={title} subtitle={`${name} · ${data.offerTitle}`} back={BACK}>
      <FinalEvaluation
        app={data}
        offerTitle={data.offerTitle}
        onDone={(_updated, decision) => {
          toast.success(
            decision === 'VALIDEE'
              ? 'Embauche validée. La confirmation a été envoyée au candidat.'
              : 'Candidature close. Le candidat a été prévenu.',
          );
          navigate('/candidatures');
        }}
      />
    </Workspace>
  );
}
