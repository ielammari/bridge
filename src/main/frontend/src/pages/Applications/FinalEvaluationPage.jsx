import { useLocation, useNavigate, useParams } from 'react-router-dom';
import { applicationsApi } from '../../api/applications.js';
import EmptyState from '../../components/EmptyState/EmptyState.jsx';
import ErrorState from '../../components/ErrorState/ErrorState.jsx';
import Skeleton from '../../components/Skeleton/Skeleton.jsx';
import { useToast } from '../../components/Toast/ToastContext.jsx';
import useResource from '../../hooks/useResource.js';
import Workspace from '../Workspace/Workspace.jsx';
import FinalEvaluation from './FinalEvaluation.jsx';
import './finalEvaluation.css';

const APPLICATIONS = '/candidatures';

/** The final HR interview for one application, at its own address. */
export default function FinalEvaluationPage() {
  const { id } = useParams();
  const location = useLocation();
  const navigate = useNavigate();
  const toast = useToast();

  // Back to the list as it stood, selected offer included. The bare list is the
  // fallback for an interview reached by its own link.
  const returnTo = location.state?.from ?? APPLICATIONS;
  const back = { to: returnTo, label: 'Retour aux candidatures' };

  const { status, data, reload, pending, leaving } = useResource(() => applicationsApi.get(id), [id]);

  const title = 'Entretien final';

  if (status !== 'ready') {
    return (
      <Workspace width="narrow" title={title} back={back}>
        {pending && <Skeleton variant="form" count={5} leaving={leaving} label="Chargement de la candidature" />}
        {status === 'error' && (
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
      <Workspace width="narrow" title={title} subtitle={name} back={back}>
        <EmptyState
          title="Cette candidature n'est plus à l'étape de l'entretien final."
          actionLabel="Retour aux candidatures"
          actionTo={returnTo}
        >
          Son étape actuelle ne permet pas de saisir un bilan d'entretien.
        </EmptyState>
      </Workspace>
    );
  }

  return (
    <Workspace width="narrow" title={title} subtitle={`${name} · ${data.offerTitle}`} back={back}>
      <FinalEvaluation
        app={data}
        offerTitle={data.offerTitle}
        onDone={(_updated, decision) => {
          toast.success(
            decision === 'VALIDEE'
              ? 'Embauche validée. La confirmation a été envoyée au candidat.'
              : 'Candidature close. Le candidat a été prévenu.',
          );
          navigate(returnTo);
        }}
      />
    </Workspace>
  );
}
