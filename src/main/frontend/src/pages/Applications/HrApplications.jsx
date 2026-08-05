import { useState } from 'react';
import { useLocation, useNavigate, useSearchParams } from 'react-router-dom';
import { applicationsApi } from '../../api/applications.js';
import { messagesApi } from '../../api/messages.js';
import { offersApi } from '../../api/offers.js';
import Button from '../../components/Button/Button.jsx';
import CardGrid from '../../components/CardGrid/CardGrid.jsx';
import ConfirmDialog from '../../components/ConfirmDialog/ConfirmDialog.jsx';
import EmptyState from '../../components/EmptyState/EmptyState.jsx';
import ErrorState from '../../components/ErrorState/ErrorState.jsx';
import Field from '../../components/Field/Field.jsx';
import Icon from '../../components/Icon/Icon.jsx';
import PersonLink from '../../components/PersonLink/PersonLink.jsx';
import Scheduler from '../../components/Scheduler/Scheduler.jsx';
import Select from '../../components/Select/Select.jsx';
import Skeleton from '../../components/Skeleton/Skeleton.jsx';
import StatusBadge from '../../components/StatusBadge/StatusBadge.jsx';
import { useToast } from '../../components/Toast/ToastContext.jsx';
import { isTerminal } from '../../constants/enums.js';
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
// Which offer's applications are on screen, kept in the address.
const OFFER = 'offre';

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
  const location = useLocation();
  const toast = useToast();
  const [params, setParams] = useSearchParams();

  const offersRes = useResource(() => offersApi.list());
  const offers = offersRes.data ?? [];

  // An explicit choice lives in the address; without one the first offer shows.
  // Derived rather than stored, so there is no second copy of the selection.
  const chosen = params.get(OFFER) ?? '';
  const offerId = chosen || (offers.length > 0 ? String(offers[0].id) : '');

  function selectOffer(id) {
    const next = new URLSearchParams(params);
    if (id) {
      next.set(OFFER, id);
    } else {
      next.delete(OFFER);
    }
    setParams(next, { replace: true });
  }

  // Where this list currently stands, so an application opened from it can
  // return to the same offer.
  const origin = { from: `${location.pathname}${location.search}` };

  const appsRes = useResource(
    () => (offerId ? applicationsApi.forOffer(offerId) : Promise.resolve([])),
    [offerId],
  );
  // Rejected and hired applications move to the history.
  const applications = (appsRes.data ?? []).filter((app) => !isTerminal(app.status));

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
        {offersRes.pending && <Skeleton leaving={offersRes.leaving} label="Chargement des candidatures" />}
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
        <Select label="Offre" value={offerId} onChange={(e) => selectOffer(e.target.value)}
          options={offers.map((o) => ({ value: String(o.id), label: o.title }))} />
      </div>

      {appsRes.pending && <Skeleton leaving={appsRes.leaving} label="Chargement des candidatures" />}

      {appsRes.status === 'error' && (
        <ErrorState onRetry={appsRes.reload}>
          Les candidatures de cette offre n'ont pas pu être chargées.
        </ErrorState>
      )}

      {appsRes.status === 'ready' && applications.length === 0 && (
        <EmptyState title="Aucune candidature en cours pour cette offre.">
          Seuls les candidats qui possèdent tous les traits obligatoires peuvent la voir et y
          postuler. Les candidatures closes sont dans l'historique.
        </EmptyState>
      )}

      {appsRes.status === 'ready' && applications.length > 0 && (
        <CardGrid label="Candidatures pour cette offre">
          {applications.map((app) => {
            const scheduling = app.status === 'EXAMEN_TECHNIQUE' || app.status === 'ENTRETIEN_RH';
            const screening = app.status === 'NOUVELLE' || app.status === 'EN_REVUE';
            const open = panel?.id === app.id;
            return (
              <li key={app.id} className={`tile${open ? ' tile--open' : ''}`}>
                <div className="tile__head">
                  {/* Opening the person also settles the notices about this
                      application, which is what looking at it used to do. */}
                  <PersonLink id={app.candidateId} className="appcard__who">
                    <span className="appcard__name" onMouseDown={() => markSeen(app)}>
                      {app.candidateFirstName} {app.candidateLastName}
                    </span>
                    <span className="appcard__email">{app.candidateEmail}</span>
                  </PersonLink>
                  <StatusBadge status={app.status} />
                </div>

                <p className="tile__facts">
                  <span>Postulé le {longDate(app.applicationDate)}</span>
                  {scheduling && <span>{interviewText(app)}</span>}
                </p>

                <div className="tile__foot">
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
                    <Button onClick={() => navigate(`/candidatures/${app.id}/entretien`, { state: origin })}>
                      Finaliser
                    </Button>
                  )}
                </div>

                {open && panel.mode === 'preselect' && (
                  <div className="appcard__panel">
                    <Field label="Commentaire de présélection" value={comment}
                      onChange={(e) => setComment(e.target.value)} multiline rows={3}
                      hint="Facultatif" />
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
        </CardGrid>
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
