import { useEffect, useState } from 'react';
import { applicationsApi } from '../../api/applications.js';
import { offersApi } from '../../api/offers.js';
import Alert from '../../components/Alert/Alert.jsx';
import Button from '../../components/Button/Button.jsx';
import Field from '../../components/Field/Field.jsx';
import Icon from '../../components/Icon/Icon.jsx';
import Scheduler from '../../components/Scheduler/Scheduler.jsx';
import Select from '../../components/Select/Select.jsx';
import StatusBadge from '../../components/StatusBadge/StatusBadge.jsx';
import Workspace from '../Workspace/Workspace.jsx';
import './hrApplications.css';

const dateFormat = new Intl.DateTimeFormat('fr-FR', { day: 'numeric', month: 'long', year: 'numeric' });

function appliedOn(iso) {
  return dateFormat.format(new Date(iso));
}

function interviewText(app) {
  const label = app.status === 'EXAMEN_TECHNIQUE' ? 'Examen technique' : 'Entretien RH';
  if (!app.appointmentDate) return `${label} à planifier`;
  return `${label} le ${dateFormat.format(new Date(app.appointmentDate))} à ${app.appointmentTime.slice(0, 5)}`;
}

export default function HrApplications() {
  const [status, setStatus] = useState('loading');
  const [offers, setOffers] = useState([]);
  const [offerId, setOfferId] = useState('');
  const [applications, setApplications] = useState([]);
  const [loadingApps, setLoadingApps] = useState(false);
  const [panel, setPanel] = useState(null); // { id, mode: 'preselect' | 'schedule' }
  const [comment, setComment] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    let cancelled = false;
    offersApi.list()
      .then((list) => {
        if (cancelled) return;
        setOffers(list);
        if (list.length > 0) setOfferId(String(list[0].id));
        setStatus('ready');
      })
      .catch(() => {
        if (!cancelled) setStatus('error');
      });
    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    if (!offerId) {
      setApplications([]);
      return;
    }
    let cancelled = false;
    setLoadingApps(true);
    setPanel(null);
    applicationsApi.forOffer(offerId)
      .then((data) => {
        if (!cancelled) setApplications(data);
      })
      .finally(() => {
        if (!cancelled) setLoadingApps(false);
      });
    return () => {
      cancelled = true;
    };
  }, [offerId]);

  function replace(updated) {
    setApplications((apps) => apps.map((a) => (a.id === updated.id ? updated : a)));
  }

  async function viewCv(id) {
    const blob = await applicationsApi.cv(id);
    const url = URL.createObjectURL(blob);
    window.open(url, '_blank', 'noopener');
    setTimeout(() => URL.revokeObjectURL(url), 10_000);
  }

  async function openPreselection(app) {
    setError(null);
    setComment('');
    setPanel({ id: app.id, mode: 'preselect' });
    if (app.status === 'NOUVELLE') {
      try {
        replace(await applicationsApi.review(app.id));
      } catch (apiError) {
        setError(apiError.message);
      }
    }
  }

  async function decide(app, decision) {
    setError(null);
    setSubmitting(true);
    try {
      replace(await applicationsApi.preselect(app.id, { decision, comment: comment || null }));
      setPanel(null);
    } catch (apiError) {
      setError(apiError.message);
    } finally {
      setSubmitting(false);
    }
  }

  if (status === 'loading') {
    return <Workspace title="Candidatures"><p className="hrapps__muted">Chargement...</p></Workspace>;
  }
  if (status === 'error') {
    return <Workspace title="Candidatures"><Alert>Les candidatures n'ont pas pu être chargées.</Alert></Workspace>;
  }
  if (offers.length === 0) {
    return (
      <Workspace title="Candidatures">
        <div className="hrapps__empty">
          <p>Vous n'avez pas encore d'offre. Créez une offre pour recevoir des candidatures.</p>
        </div>
      </Workspace>
    );
  }

  return (
    <Workspace title="Candidatures">
      <div className="hrapps__filter">
        <Select label="Offre" value={offerId} onChange={(e) => setOfferId(e.target.value)}
          options={offers.map((o) => ({ value: String(o.id), label: o.title }))} />
      </div>

      {error && <Alert>{error}</Alert>}

      {loadingApps ? (
        <p className="hrapps__muted">Chargement des candidatures...</p>
      ) : applications.length === 0 ? (
        <div className="hrapps__empty"><p>Aucune candidature pour cette offre pour le moment.</p></div>
      ) : (
        <ul className="hrapps__list">
          {applications.map((app) => {
            const scheduling = app.status === 'EXAMEN_TECHNIQUE' || app.status === 'ENTRETIEN_RH';
            const screening = app.status === 'NOUVELLE' || app.status === 'EN_REVUE';
            const open = panel?.id === app.id;
            return (
              <li key={app.id} className="appcard">
                <div className="appcard__row">
                  <div className="appcard__who">
                    <span className="appcard__name">{app.candidateFirstName} {app.candidateLastName}</span>
                    <span className="appcard__email">{app.candidateEmail}</span>
                    <span className="appcard__date">Postulé le {appliedOn(app.applicationDate)}</span>
                  </div>

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
                  </div>
                </div>

                {open && panel.mode === 'preselect' && (
                  <div className="appcard__panel">
                    <Field label="Commentaire de présélection" value={comment}
                      onChange={(e) => setComment(e.target.value)} multiline rows={3}
                      hint="Conservé avec l'évaluation." />
                    <div className="appcard__decide">
                      <Button variant="secondary" onClick={() => decide(app, 'REFUSEE')} loading={submitting}>
                        Rejeter
                      </Button>
                      <Button onClick={() => decide(app, 'VALIDEE')} loading={submitting}>
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
                      }}
                    />
                  </div>
                )}
              </li>
            );
          })}
        </ul>
      )}
    </Workspace>
  );
}
