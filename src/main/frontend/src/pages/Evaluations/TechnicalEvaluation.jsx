import { useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { evaluationsApi } from '../../api/evaluations.js';
import Alert from '../../components/Alert/Alert.jsx';
import Button from '../../components/Button/Button.jsx';
import ConfirmDialog from '../../components/ConfirmDialog/ConfirmDialog.jsx';
import ErrorState from '../../components/ErrorState/ErrorState.jsx';
import Field from '../../components/Field/Field.jsx';
import Skeleton from '../../components/Skeleton/Skeleton.jsx';
import StarRating from '../../components/StarRating/StarRating.jsx';
import { useToast } from '../../components/Toast/ToastContext.jsx';
import useResource from '../../hooks/useResource.js';
import Workspace from '../Workspace/Workspace.jsx';
import './technicalEvaluations.css';

const BACK = { to: '/evaluations', label: 'Retour aux évaluations' };

// Both outcomes are irreversible, so both are confirmed.
const DECISIONS = {
  VALIDEE: {
    title: 'Avis favorable ?',
    body: 'Le candidat passe à l\'entretien RH. Les RH seront prévenus d\'en fixer la date.',
    confirmLabel: 'Avis favorable, passer à l\'entretien RH',
    tone: 'primary',
    nextStatus: 'ENTRETIEN_RH',
    done: 'Avis favorable enregistré. Les RH vont planifier l\'entretien.',
  },
  REFUSEE: {
    title: 'Avis défavorable ?',
    body: 'La candidature est close et le candidat en est informé. Cette décision ne se reprend pas.',
    confirmLabel: 'Avis défavorable',
    tone: 'danger',
    nextStatus: 'REFUSEE',
    done: 'Avis défavorable enregistré. Le candidat a été prévenu.',
  },
};

export default function TechnicalEvaluation() {
  const { id } = useParams();
  const navigate = useNavigate();
  const toast = useToast();

  const [scores, setScores] = useState({});
  const [comment, setComment] = useState('');
  const [confirming, setConfirming] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [failure, setFailure] = useState(null);

  const { status, data, reload } = useResource(async () => {
    const context = await evaluationsApi.technicalContext(id);
    // Required traits open at zero; a plus trait stays null until the expert
    // opts in, so "not evaluated" never reads as "scored zero".
    const initial = {};
    context.traits.forEach((t) => {
      initial[t.traitId] = t.mandatory ? 0 : null;
    });
    setScores(initial);
    return context;
  }, [id]);

  async function send() {
    const decision = confirming;
    setFailure(null);
    setSubmitting(true);
    try {
      await evaluationsApi.submitTechnical(id, {
        decision,
        comment: comment.trim() || null,
        scores: Object.entries(scores)
          .filter(([, units]) => units !== null)
          .map(([traitId, units]) => ({ traitId: Number(traitId), note: units })),
      });
      toast.success(DECISIONS[decision].done);
      navigate('/evaluations');
    } catch (apiError) {
      setFailure(apiError.message);
      setConfirming(null);
      setSubmitting(false);
    }
  }

  if (status !== 'ready') {
    return (
      <Workspace title="Évaluation technique" back={BACK}>
        {status === 'loading' ? (
          <Skeleton count={2} label="Chargement de l'évaluation" />
        ) : (
          <ErrorState onRetry={reload}>
            Cette évaluation n'a pas pu être chargée. Réessayez dans un instant.
          </ErrorState>
        )}
      </Workspace>
    );
  }

  const name = `${data.candidateFirstName} ${data.candidateLastName}`;

  // Plus traits left unscored, surfaced by the confirmation.
  const unrated = data.traits
    .filter((t) => !t.mandatory && scores[t.traitId] === null)
    .map((t) => ({ key: `trait-${t.traitId}`, label: `${t.label} (atout, non noté)` }));

  const missing = comment.trim()
    ? unrated
    : [...unrated, { key: 'comment', label: 'Commentaire global' }];

  return (
    <Workspace title="Évaluation technique" subtitle={`${name} · ${data.offerTitle}`} back={BACK}>
      {failure && <Alert>{failure}</Alert>}

      <section className="card">
        <div className="card__head">
          <h2 className="card__title">Notation des traits</h2>
          <p className="card__subtitle">Notez chaque trait de 0 à 5 étoiles (demi-étoiles possibles).</p>
        </div>
        <div className="card__body">
          <ul className="grid">
            {data.traits.map((trait) => {
              const included = scores[trait.traitId] !== null;
              return (
                <li key={trait.traitId} className="grid__row">
                  <div className="grid__label">
                    <span>{trait.label}</span>
                    <span className={`grid__tag${trait.mandatory ? ' grid__tag--req' : ''}`}>
                      {trait.mandatory ? 'Obligatoire' : 'Atout'}
                    </span>
                  </div>
                  {trait.mandatory || included ? (
                    <StarRating value={scores[trait.traitId] ?? 0}
                      onChange={(units) => setScores((s) => ({ ...s, [trait.traitId]: units }))}
                      label={trait.label} />
                  ) : (
                    <label className="grid__optin">
                      <input type="checkbox" checked={false}
                        onChange={() => setScores((s) => ({ ...s, [trait.traitId]: 0 }))} />
                      Évaluer ce trait
                    </label>
                  )}
                </li>
              );
            })}
          </ul>
        </div>
      </section>

      <section className="card">
        <div className="card__body">
          <Field label="Commentaire global" value={comment}
            onChange={(e) => setComment(e.target.value)}
            multiline rows={4} hint="Facultatif. Votre appréciation de l'examen technique." />
        </div>
      </section>

      <div className="tech__decide">
        <Button variant="danger" onClick={() => setConfirming('REFUSEE')}>
          Avis défavorable
        </Button>
        <Button onClick={() => setConfirming('VALIDEE')}>
          Avis favorable, passer à l'entretien RH
        </Button>
      </div>

      {confirming && (
        <ConfirmDialog
          open
          title={DECISIONS[confirming].title}
          confirmLabel={DECISIONS[confirming].confirmLabel}
          tone={DECISIONS[confirming].tone}
          nextStatus={DECISIONS[confirming].nextStatus}
          missing={missing}
          busy={submitting}
          onConfirm={send}
          onCancel={() => setConfirming(null)}
        >
          <strong>{name}</strong>. {DECISIONS[confirming].body}
        </ConfirmDialog>
      )}
    </Workspace>
  );
}
