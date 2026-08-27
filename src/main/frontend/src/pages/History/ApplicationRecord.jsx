import { useEffect } from 'react';
import { useLocation, useParams, useSearchParams } from 'react-router-dom';
import { historyApi } from '../../api/history.js';
import ErrorState from '../../components/ErrorState/ErrorState.jsx';
import FunnelRail from '../../components/FunnelRail/FunnelRail.jsx';
import OfferLink from '../../components/OfferLink/OfferLink.jsx';
import Skeleton from '../../components/Skeleton/Skeleton.jsx';
import StarRating from '../../components/StarRating/StarRating.jsx';
import StatusBadge from '../../components/StatusBadge/StatusBadge.jsx';
import {
  APPOINTMENT_TYPE_LABELS, CONTRACT_LABELS, DECISION_LABELS,
  EVALUATION_TYPE_LABELS, REMOTE_LABELS,
} from '../../constants/enums.js';
import { clockTime, dateTime, euros, longDate } from '../../constants/format.js';
import { INTERVIEW, fromCalendar, returnLink } from '../../constants/navigation.js';
import { useAuth } from '../../context/AuthContext.jsx';
import useResource from '../../hooks/useResource.js';
import Workspace from '../Workspace/Workspace.jsx';
import './history.css';
import { historyHome } from './tabs.js';

// The evaluation an interview produces, so a record opened at one marks both.
const EVALUATION_OF = { TECHNIQUE: 'TECHNIQUE', RH: 'ENTRETIEN_RH' };

function Terms({ title, subtitle, rows }) {
  const filled = rows.filter(([, value]) => value !== null && value !== undefined && value !== '');
  if (filled.length === 0) return null;

  return (
    <section className="card">
      <div className="card__head">
        <h2 className="card__title">{title}</h2>
        {subtitle && <p className="card__subtitle">{subtitle}</p>}
      </div>
      <div className="card__body">
        <dl className="record__terms">
          {filled.map(([label, value]) => (
            <div key={label} className="record__term">
              <dt>{label}</dt>
              <dd>{value}</dd>
            </div>
          ))}
        </dl>
      </div>
    </section>
  );
}

/**
 * One closed application in full. A candidate reads the facts about their own:
 * the stages, the interviews, and the terms if it ended in a hire. Whoever ran
 * the funnel also reads the evaluations, which never travel to the candidate.
 */
export default function ApplicationRecord() {
  const { id } = useParams();
  const location = useLocation();
  const [params] = useSearchParams();
  const { user } = useAuth();
  const isCandidate = user.role === 'CANDIDAT';
  // Followed from the calendar, which names the interview it means.
  const named = params.get(INTERVIEW);

  const { status, data, reload, pending, leaving } = useResource(
    () => (isCandidate ? historyApi.myApplication(id) : historyApi.trail(id)),
    [id, isCandidate],
  );

  // A record reached from the calendar returns there from the header's other
  // corner; this link keeps to the history.
  const returning = fromCalendar(location.state);
  // Back to the exact listing this was opened from, tab and filters included.
  // The role's first tab is the fallback for a record reached by its own link,
  // where there is no listing to return to.
  const back = {
    to: (returning ? null : location.state?.from) ?? historyHome(user.role),
    label: 'Retour à l\'historique',
  };
  const returnTo = returning ? returnLink(location.state) : null;

  useEffect(() => {
    if (!named || status !== 'ready') return;
    const target = document.getElementById('named-evaluation')
      ?? document.getElementById('named-interview');
    target?.scrollIntoView({ block: 'nearest' });
  }, [named, status]);

  if (status !== 'ready') {
    return (
      <Workspace width="narrow" title="Dossier de candidature" back={back} returnTo={returnTo}>
        {pending && <Skeleton variant="record" leaving={leaving} label="Chargement du dossier" />}
        {status === 'error' && (
          <ErrorState onRetry={reload}>
            Ce dossier n'a pas pu être chargé, ou il ne vous est pas accessible.
          </ErrorState>
        )}
      </Workspace>
    );
  }

  const app = data.application;
  // The interview the address names, and the assessment it produced.
  const markedInterview = named
    ? data.appointments.find((one) => one.type === named)?.id : null;
  const markedEvaluation = named
    ? data.evaluations?.find((one) => one.type === EVALUATION_OF[named])?.id : null;
  const title = isCandidate ? app.offerTitle : `${app.candidateFirstName} ${app.candidateLastName}`;
  // A name is not an identity: two candidates can share one, so the title is
  // the way into the profile that settles which of them this is.
  const titleTo = isCandidate ? `/offres/${app.offerId}` : `/personnes/${app.candidateId}`;
  const subtitle = isCandidate
    ? CONTRACT_LABELS[app.contractType]
    : <OfferLink id={app.offerId}>{app.offerTitle}</OfferLink>;

  // A candidate's record carries no assessment, so it has nothing to set
  // beside the facts and reads as one column.
  const split = !isCandidate;

  return (
    <Workspace width={split ? 'wide' : 'narrow'} title={title} titleTo={titleTo}
      subtitle={subtitle} back={back} returnTo={returnTo}>
      <div className={`doc${split ? ' doc--split' : ''}`}>
        {/* Where the application stands is the record's first fact, and the
            rail wants the width of the page rather than of a column. */}
        <section className="card doc__lead">
          <div className="card__body">
            <div className="record__status">
              <StatusBadge status={app.status} />
              <span className="record__meta">Déposée le {longDate(app.applicationDate)}</span>
            </div>
            <FunnelRail status={app.status} />
          </div>
        </section>

        <aside className="doc__side">
        {data.appointments.length > 0 && (
          <section className="card">
            <div className="card__head">
              <h2 className="card__title">Entretiens</h2>
            </div>
            <div className="card__body">
              <ul className="record__list">
                {data.appointments.map((appointment) => (
                  <li
                    key={appointment.id}
                    className={`record__row${appointment.id === markedInterview
                      ? ' record__row--named' : ''}`}
                    id={appointment.id === markedInterview ? 'named-interview' : undefined}
                  >
                    <span>
                      {APPOINTMENT_TYPE_LABELS[appointment.type] ?? appointment.type}
                      {appointment.evaluatorName && (
                        <span className="record__meta"> avec {appointment.evaluatorName}</span>
                      )}
                    </span>
                    <span className="mono">
                      {longDate(appointment.date)} à {clockTime(appointment.time)}
                    </span>
                  </li>
                ))}
              </ul>
            </div>
          </section>
        )}

        {data.hiring && (
          <Terms
            title="Conditions d'embauche"
            rows={[
              ['Salaire négocié', euros(data.hiring.negotiatedSalary)],
              ['Prise de poste', longDate(data.hiring.startDate)],
              ['Contrat', CONTRACT_LABELS[data.hiring.finalContract]],
              ['Période d\'essai', data.hiring.trialPeriod],
              ['Statut cadre', data.hiring.executiveStatus ? 'Oui' : null],
              ['Avantages', data.hiring.benefits],
            ]}
          />
        )}
        </aside>

        <div className="doc__main">
        {/* Assessments of the candidate: served to the evaluating roles only. */}
        {!isCandidate && data.evaluations.length > 0 && (
          <section className="card">
            <div className="card__head">
              <h2 className="card__title">Évaluations</h2>
            </div>
            <div className="card__body">
              {data.evaluations.map((evaluation) => (
                <article
                  key={evaluation.id}
                  className={`record__eval${evaluation.id === markedEvaluation
                    ? ' record__eval--named' : ''}`}
                  id={evaluation.id === markedEvaluation ? 'named-evaluation' : undefined}
                >
                  <div className="record__eval-head">
                    <h3 className="record__eval-title">
                      {EVALUATION_TYPE_LABELS[evaluation.type] ?? evaluation.type}
                    </h3>
                    <span className={`verdict verdict--${evaluation.decision.toLowerCase()}`}>
                      {DECISION_LABELS[evaluation.decision]}
                    </span>
                  </div>
                  <p className="record__meta">
                    {evaluation.evaluatorName} · {dateTime(evaluation.date)}
                  </p>
                  {evaluation.comment && <p className="record__comment">{evaluation.comment}</p>}
                  {evaluation.scores.length > 0 && (
                    <ul className="record__scores">
                      {evaluation.scores.map((score) => (
                        <li key={score.traitId} className="record__score">
                          <span className="record__trait">{score.label}</span>
                          <StarRating value={score.note} readOnly label={score.label} />
                        </li>
                      ))}
                    </ul>
                  )}
                </article>
              ))}
            </div>
          </section>
        )}

        {!isCandidate && data.interview && (
          <Terms
            title="Bilan de l'entretien final"
            subtitle="Conservé quelle que soit la décision."
            rows={[
              ['Salaire attendu', data.interview.expectedSalary && euros(data.interview.expectedSalary)],
              ['Disponibilité', data.interview.availabilityDate && longDate(data.interview.availabilityDate)],
              ['Contrat envisagé', CONTRACT_LABELS[data.interview.envisagedContract]],
              ['Préavis', data.interview.noticePeriod],
              ['Flexibilité horaire', data.interview.scheduleFlexibility],
              ['Attentes télétravail', REMOTE_LABELS[data.interview.remoteExpectation]],
              ['Adéquation avec la culture', data.interview.cultureFit],
            ]}
          />
        )}

        </div>
      </div>
    </Workspace>
  );
}
