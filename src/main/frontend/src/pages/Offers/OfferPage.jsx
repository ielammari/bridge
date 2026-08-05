import { useState } from 'react';
import { useLocation, useNavigate, useParams } from 'react-router-dom';
import { applicationsApi } from '../../api/applications.js';
import { offersApi } from '../../api/offers.js';
import Button from '../../components/Button/Button.jsx';
import ErrorState from '../../components/ErrorState/ErrorState.jsx';
import Skeleton from '../../components/Skeleton/Skeleton.jsx';
import StatusBadge from '../../components/StatusBadge/StatusBadge.jsx';
import { useToast } from '../../components/Toast/ToastContext.jsx';
import { CONTRACT_LABELS, DEGREE_LABELS, REMOTE_LABELS } from '../../constants/enums.js';
import { longDate, salaryText } from '../../constants/format.js';
import { useAuth } from '../../context/AuthContext.jsx';
import useResource from '../../hooks/useResource.js';
import Workspace from '../Workspace/Workspace.jsx';
import './offerPage.css';

/** A labelled fact, dropped entirely when the offer does not state it. */
function Fact({ label, children }) {
  if (children === null || children === undefined || children === '') return null;
  return (
    <div className="vitrine__fact">
      <dt className="vitrine__fact-label">{label}</dt>
      <dd className="vitrine__fact-value">{children}</dd>
    </div>
  );
}

/**
 * One offer at full length, which a listing card only summarises.
 *
 * The description runs unclipped, the requirements separate what an applicant
 * must hold from what counts in their favour, and applying sits where the
 * reading ends rather than competing with it.
 */
export default function OfferPage() {
  const { id } = useParams();
  const location = useLocation();
  const navigate = useNavigate();
  const toast = useToast();
  const { user } = useAuth();

  const [applying, setApplying] = useState(false);
  const [applied, setApplied] = useState(false);

  const { status, data, reload, pending, leaving } = useResource(
    () => offersApi.detail(id),
    [id],
  );

  const back = location.state?.from ? { to: location.state.from, label: 'Retour' } : null;

  async function apply() {
    setApplying(true);
    try {
      await applicationsApi.apply(Number(id));
      setApplied(true);
      toast.success('Candidature envoyée. Suivez-la dans « Mes candidatures ».');
    } catch (apiError) {
      toast.error(apiError.message);
    } finally {
      setApplying(false);
    }
  }

  if (status !== 'ready') {
    return (
      <Workspace width="narrow" title="Offre" back={back}>
        {pending && <Skeleton variant="record" leaving={leaving} label="Chargement de l'offre" />}
        {status === 'error' && (
          <ErrorState onRetry={reload}>
            Cette offre n'a pas pu être chargée, ou elle ne vous est pas accessible.
          </ErrorState>
        )}
      </Workspace>
    );
  }

  const { offer, publisherName, alreadyApplied, applicationCount } = data;
  const required = offer.requirements.filter((r) => r.mandatory);
  const plus = offer.requirements.filter((r) => !r.mandatory);
  const salary = salaryText(offer.salaryMin, offer.salaryMax);
  const isCandidate = user.role === 'CANDIDAT';
  const hasApplied = alreadyApplied || applied;

  return (
    <Workspace width="narrow" title={offer.title} back={back}>
      <div className="vitrine">
        <div className="vitrine__banner">
          <span className="vitrine__contract">{CONTRACT_LABELS[offer.contractType]}</span>
          <StatusBadge status={offer.status} />
        </div>

        <section className="card">
          <div className="card__body">
            <dl className="vitrine__facts">
              <Fact label="Lieu">{offer.location}</Fact>
              <Fact label="Télétravail">{REMOTE_LABELS[offer.remoteMode]}</Fact>
              <Fact label="Niveau d'études">{DEGREE_LABELS[offer.requiredDegree]}</Fact>
              <Fact label="Rémunération">
                {salary ? <span className="mono">{salary}</span> : null}
              </Fact>
              <Fact label="Publiée le">
                {offer.publicationDate ? longDate(offer.publicationDate) : null}
              </Fact>
              <Fact label="Publiée par">{publisherName}</Fact>
              <Fact label="Candidatures reçues">
                {applicationCount === null || applicationCount === undefined
                  ? null
                  : String(applicationCount)}
              </Fact>
            </dl>
          </div>
        </section>

        <section className="card" aria-labelledby="offer-desc-title">
          <div className="card__head">
            <h2 id="offer-desc-title" className="card__title">Le poste</h2>
          </div>
          <div className="card__body">
            {/* Runs at full length: this is the reason the page exists. */}
            <p className="vitrine__desc">{offer.description}</p>
          </div>
        </section>

        <section className="card" aria-labelledby="offer-traits-title">
          <div className="card__head">
            <h2 id="offer-traits-title" className="card__title">Profil recherché</h2>
            <p className="card__subtitle">
              Les traits obligatoires conditionnent la candidature. Les autres comptent en votre
              faveur sans être exigés.
            </p>
          </div>
          <div className="card__body">
            <div className="vitrine__group">
              <h3 className="vitrine__group-title">Obligatoires</h3>
              {required.length === 0 ? (
                <p className="vitrine__none">Aucun trait obligatoire.</p>
              ) : (
                <ul className="vitrine__tags">
                  {required.map((r) => (
                    <li key={r.traitId} className="tag tag--required">{r.label}</li>
                  ))}
                </ul>
              )}
            </div>

            <div className="vitrine__group">
              <h3 className="vitrine__group-title">Atouts</h3>
              {plus.length === 0 ? (
                <p className="vitrine__none">Aucun atout déclaré.</p>
              ) : (
                <ul className="vitrine__tags">
                  {plus.map((r) => (
                    <li key={r.traitId} className="tag tag--plus">{r.label}</li>
                  ))}
                </ul>
              )}
            </div>
          </div>
        </section>

        {isCandidate && (
          <div className="vitrine__act">
            {hasApplied ? (
              <>
                <p className="vitrine__applied">Vous avez postulé à cette offre.</p>
                <Button variant="secondary" onClick={() => navigate('/mes-candidatures')}>
                  Suivre ma candidature
                </Button>
              </>
            ) : (
              <>
                <p className="vitrine__note">
                  Votre CV et votre profil seront joints à la candidature.
                </p>
                <Button onClick={apply} loading={applying}>Envoyer la candidature</Button>
              </>
            )}
          </div>
        )}
      </div>
    </Workspace>
  );
}
