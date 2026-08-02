import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { applicationsApi } from '../../api/applications.js';
import { messagesApi } from '../../api/messages.js';
import { offersApi } from '../../api/offers.js';
import Button from '../../components/Button/Button.jsx';
import ConfirmDialog from '../../components/ConfirmDialog/ConfirmDialog.jsx';
import EmptyState from '../../components/EmptyState/EmptyState.jsx';
import ErrorState from '../../components/ErrorState/ErrorState.jsx';
import Field from '../../components/Field/Field.jsx';
import Icon from '../../components/Icon/Icon.jsx';
import Scheduler from '../../components/Scheduler/Scheduler.jsx';
import Select from '../../components/Select/Select.jsx';
import Skeleton from '../../components/Skeleton/Skeleton.jsx';
import StatusBadge from '../../components/StatusBadge/StatusBadge.jsx';
import { useToast } from '../../components/Toast/ToastContext.jsx';
import { clockTime, longDate } from '../../constants/format.js';
import useResource from '../../hooks/useResource.js';
import Workspace from '../Workspace/Workspace.jsx';
import './hrApplications.css';

function interviewText(app) {
  const label = app.status === 'EXAMEN_TECHNIQUE' ? 'Examen technique' : 'Entretien RH';
  if (!app.appointmentDate) return `${label} à planifier`;
  return `${label} le ${longDate(app.appointmentDate)} à ${clockTime(app.appointmentTime)}`;
}

// Both outcomes are irreversible, so both are confirmed.
const DECISIONS = {
  VALIDEE: {
    title: 'Valider cette candidature ?',
    body: 'Le candidat passe à l\'examen technique. Vous devrez ensuite en fixer la date.',
    confirmLabel: 'Valider et passer à l\'examen technique',
    tone: 'primary',
    nextStatus: 'EXAMEN_TECHNIQUE',
    done: 'Candidature validée. Planifiez l\'examen technique.',
  },
  REFUSEE: {
    title: 'Rejeter cette candidature ?',
    body: 'Le candidat en est informé immédiatement et la candidature est close. Cette décision ne se reprend pas.',
    confirmLabel: 'Rejeter la candidature',
    tone: 'danger',
    nextStatus: 'REFUSEE',
    done: 'Candidature rejetée. Le candidat a été prévenu.',
  },
};

export default function HrApplications() {
  const navigate = useNavigate();
  const toast = useToast();

  const offersRes = useResource(() => offersApi.list());
  const offers = offersRes.data ?? [];
  const [offerId, setOfferId] = useState('');

  useEffect(() => {
    if (offers.length > 0 && !offerId) setOfferId(String(offers[0].id));
  }, [offers, offerId]);

  const appsRes = useResource(
    () => (offerId ? applicationsApi.forOffer(offerId) : Promise.resolve([])),
    [offerId],
  );
  const applications = appsRes.data ?? [];

  const [panel, setPanel] = useState(null); // { id, mode: 'preselect' | 'schedule' }
  const [comment, setComment] = useState('');
  const [confirming, setConfirming] = useState(null); // { app, decision }
  const [busy, setBusy] = useState(false);

  function replace(updated) {
    appsRes.setData((list) => list.map((a) => (a.id === updated.id ? updated : a)));
  }

  async function viewCv(id) {
    try {
      const blob = await applicationsApi.cv(id);
      const url = URL.createObjectURL(blob);
      window.open(url, '_blank', 'noopener');
      setTimeout(() => URL.revokeObjectURL(url), 10_000);
    } catch (apiError) {
      toast.error(apiError.message);
    }
  }

  /** Opening an application marks the notices about it read. */
  async function markSeen(app) {
    try {
      await messagesApi.readForApplication(app.id);
    } catch {
      // A side effect of looking, not a requested action: the count corrects
      // itself on the next read.
    }
  }

  async function openPreselection(app) {
    setComment('');
    setPanel({ id: app.id, mode: 'preselect' });
    markSeen(app);
    if (app.status === 'NOUVELLE') {
      try {
        replace(await applicationsApi.review(app.id));
      } catch (apiError) {
        toast.error(apiError.message);
      }
    }
  }

  async function decide() {
    const { app, decision } = confirming;
    setBusy(true);
    try {
      replace(await applicationsApi.preselect(app.id, { decision, comment: comment || null }));
      toast.success(DECISIONS[decision].done);
      setConfirming(null);
      setPanel(null);
    } catch (apiError) {
      toast.error(apiError.message);
    } finally {
      setBusy(false);
    }
  }

  if (offersRes.status === 'loading') {
    return (
      <Workspace title="Candidatures">
        <Skeleton label="Chargement des candidatures" />
      </Workspace>
    );
  }

  if (offersRes.status === 'error') {
    return (
      <Workspace title="Candidatures">
        <ErrorState onRetry={offersRes.reload}>
          Les candidatures n'ont pas pu être chargées. Réessayez dans un instant.
        </ErrorState>
      </Workspace>
    );
  }

  if (offers.length === 0) {
    return (
      <Workspace title="Candidatures">
        <EmptyState
          title="Aucune offre, donc aucune candidature."
          actionLabel="Créer une offre"
          onAction={() => navigate('/offres/nouvelle')}
        >
          Les candidatures arrivent par les offres publiées. Créez et publiez une offre pour
          commencer à en recevoir.
        </EmptyState>
      </Workspace>
    );
  }

  return (
    <Workspace title="Candidatures">
      <div className="hrapps__filter">
        <Select label="Offre" value={offerId} onChange={(e) => setOfferId(e.target.value)}
          options={offers.map((o) => ({ value: String(o.id), label: o.title }))} />
      </div>

      {appsRes.status === 'loading' && <Skeleton label="Chargement des candidatures" />}

      {appsRes.status === 'error' && (
        <ErrorState onRetry={appsRes.reload}>
          Les candidatures de cette offre n'ont pas pu être chargées.
        </ErrorState>
      )}

      {appsRes.status === 'ready' && applications.length === 0 && (
        <EmptyState title="Aucune candidature pour cette offre.">
          Seuls les candidats qui possèdent tous les traits obligatoires de cette offre peuvent la
          voir et y postuler.
        </EmptyState>
      )}

      {appsRes.status === 'ready' && applications.length > 0 && (
        <ul className="hrapps__list">
          {applications.map((app) => {
            const scheduling = app.status === 'EXAMEN_TECHNIQUE' || app.status === 'ENTRETIEN_RH';
            const screening = app.status === 'NOUVELLE' || app.status === 'EN_REVUE';
            const open = panel?.id === app.id;
            return (
              <li key={app.id} className="appcard">
                <div className="appcard__row">
                  <button type="button" className="appcard__who" onClick={() => markSeen(app)}
                    aria-label={`Candidature de ${app.candidateFirstName} ${app.candidateLastName}`}>
                    <span className="appcard__name">{app.candidateFirstName} {app.candidateLastName}</span>
                    <span className="appcard__email">{app.candidateEmail}</span>
                    <span className="appcard__date">Postulé le {longDate(app.applicationDate)}</span>
                  </button>

                  <div className="appcard__state">
                    <StatusBadge status={app.status} />
                    {scheduling && <span className="appcard__interview">{interviewText(app)}</span>}
                  </div>

                  <div className="appcard__actions">
                    <Button variant="text" onClick={() => viewCv(app.id)}>
                      <Icon name="download" /> CV
                    </Button>
                    {screening && (
                      <Button variant="secondary"
                        onClick={() => (open ? setPanel(null) : openPreselection(app))}>
                        Présélectionner
                      </Button>
                    )}
                    {scheduling && (
                      <Button variant="secondary"
                        onClick={() => setPanel(open ? null : { id: app.id, mode: 'schedule' })}>
                        {app.appointmentDate ? 'Reprogrammer' : 'Planifier'}
                      </Button>
                    )}
                    {app.status === 'ENTRETIEN_RH' && (
                      <Button onClick={() => navigate(`/candidatures/${app.id}/entretien`)}>
                        Finaliser l'entretien
                      </Button>
                    )}
                  </div>
                </div>

                {open && panel.mode === 'preselect' && (
                  <div className="appcard__panel">
                    <Field label="Commentaire de présélection" value={comment}
                      onChange={(e) => setComment(e.target.value)} multiline rows={3}
                      hint="Facultatif, conservé avec l'évaluation." />
                    <div className="appcard__decide">
                      <Button variant="danger" onClick={() => setConfirming({ app, decision: 'REFUSEE' })}>
                        Rejeter
                      </Button>
                      <Button onClick={() => setConfirming({ app, decision: 'VALIDEE' })}>
                        Valider et passer à l'examen technique
                      </Button>
                    </div>
                  </div>
                )}

                {open && panel.mode === 'schedule' && (
                  <div className="appcard__panel">
                    <Scheduler
                      applicationId={app.id}
                      current={app.appointmentDate ? { date: app.appointmentDate } : null}
                      onScheduled={(updated) => {
                        replace(updated);
                        setPanel(null);
                        toast.success('Entretien planifié. Le candidat a été prévenu.');
                      }}
                    />
                  </div>
                )}
              </li>
            );
          })}
        </ul>
      )}

      {confirming && (
        <ConfirmDialog
          open
          title={DECISIONS[confirming.decision].title}
          confirmLabel={DECISIONS[confirming.decision].confirmLabel}
          tone={DECISIONS[confirming.decision].tone}
          nextStatus={DECISIONS[confirming.decision].nextStatus}
          missing={comment.trim() ? [] : [{ key: 'comment', label: 'Commentaire de présélection' }]}
          busy={busy}
          onConfirm={decide}
          onCancel={() => setConfirming(null)}
        >
          <strong>{confirming.app.candidateFirstName} {confirming.app.candidateLastName}</strong>.{' '}
          {DECISIONS[confirming.decision].body}
        </ConfirmDialog>
      )}
    </Workspace>
  );
}
