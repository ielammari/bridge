import { useEffect, useState } from 'react';
import { evaluationsApi } from '../../api/evaluations.js';
import Alert from '../../components/Alert/Alert.jsx';
import Button from '../../components/Button/Button.jsx';
import Field from '../../components/Field/Field.jsx';
import StarRating from '../../components/StarRating/StarRating.jsx';
import Workspace from '../Workspace/Workspace.jsx';
import './technicalEvaluations.css';

const dateFormat = new Intl.DateTimeFormat('fr-FR', { day: 'numeric', month: 'long' });

function whenText(app) {
  if (!app.appointmentDate) return 'Examen non planifié';
  return `Examen le ${dateFormat.format(new Date(app.appointmentDate))} à ${app.appointmentTime.slice(0, 5)}`;
}

export default function TechnicalEvaluations() {
  const [status, setStatus] = useState('loading');
  const [pending, setPending] = useState([]);
  const [active, setActive] = useState(null); // { context, scores: {traitId: units|null}, comment }
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);

  function loadPending() {
    return evaluationsApi.pendingTechnical().then(setPending);
  }

  useEffect(() => {
    let cancelled = false;
    loadPending()
      .then(() => {
        if (!cancelled) setStatus('ready');
      })
      .catch(() => {
        if (!cancelled) setStatus('error');
      });
    return () => {
      cancelled = true;
    };
  }, []);

  async function open(applicationId) {
    setError(null);
    const context = await evaluationsApi.technicalContext(applicationId);
    const scores = {};
    context.traits.forEach((t) => {
      scores[t.traitId] = t.mandatory ? 0 : null; // required start at zero, optional start unrated
    });
    setActive({ context, scores, comment: '' });
  }

  function setScore(traitId, units) {
    setActive((a) => ({ ...a, scores: { ...a.scores, [traitId]: units } }));
  }

  function toggleOptional(traitId, include) {
    setActive((a) => ({ ...a, scores: { ...a.scores, [traitId]: include ? 0 : null } }));
  }

  async function submit(decision) {
    setError(null);
    setSubmitting(true);
    const scores = Object.entries(active.scores)
      .filter(([, units]) => units !== null)
      .map(([traitId, units]) => ({ traitId: Number(traitId), note: units }));

    try {
      await evaluationsApi.submitTechnical(active.context.applicationId, {
        decision,
        comment: active.comment || null,
        scores,
      });
      setActive(null);
      await loadPending();
    } catch (apiError) {
      setError(apiError.message);
    } finally {
      setSubmitting(false);
    }
  }

  if (status === 'loading') {
    return <Workspace title="Évaluations"><p className="tech__muted">Chargement...</p></Workspace>;
  }
  if (status === 'error') {
    return <Workspace title="Évaluations"><Alert>Les candidatures n'ont pas pu être chargées.</Alert></Workspace>;
  }

  if (active) {
    const { context, scores } = active;
    return (
      <Workspace title="Évaluations">
        <div className="tech__head">
          <div>
            <h2 className="tech__title">{context.candidateFirstName} {context.candidateLastName}</h2>
            <p className="tech__sub">{context.offerTitle}</p>
          </div>
          <Button variant="text" onClick={() => setActive(null)}>Retour</Button>
        </div>

        {error && <Alert>{error}</Alert>}

        <section className="card">
          <div className="card__head">
            <h2 className="card__title">Notation des traits</h2>
            <p className="card__subtitle">Notez chaque trait de 0 à 5 étoiles (demi-étoiles possibles).</p>
          </div>
          <div className="card__body">
            <ul className="grid">
              {context.traits.map((trait) => {
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
                        onChange={(units) => setScore(trait.traitId, units)}
                        label={trait.label} />
                    ) : (
                      <label className="grid__optin">
                        <input type="checkbox" checked={false}
                          onChange={() => toggleOptional(trait.traitId, true)} />
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
            <Field label="Commentaire global" value={active.comment}
              onChange={(e) => setActive((a) => ({ ...a, comment: e.target.value }))}
              multiline rows={4} hint="Votre appréciation de l'examen technique." />
          </div>
        </section>

        <div className="tech__decide">
          <Button variant="secondary" onClick={() => submit('REFUSEE')} loading={submitting}>
            Défavorable
          </Button>
          <Button onClick={() => submit('VALIDEE')} loading={submitting}>
            Favorable, passer à l'entretien RH
          </Button>
        </div>
      </Workspace>
    );
  }

  return (
    <Workspace title="Évaluations">
      <p className="tech__intro">Candidats à évaluer après leur examen technique.</p>
      {pending.length === 0 ? (
        <div className="tech__empty"><p>Aucune évaluation technique en attente.</p></div>
      ) : (
        <ul className="tech__list">
          {pending.map((app) => (
            <li key={app.applicationId} className="techcard">
              <div>
                <span className="techcard__name">{app.candidateFirstName} {app.candidateLastName}</span>
                <span className="techcard__meta">{app.offerTitle} · {whenText(app)}</span>
              </div>
              <Button onClick={() => open(app.applicationId)}>Évaluer</Button>
            </li>
          ))}
        </ul>
      )}
    </Workspace>
  );
}
