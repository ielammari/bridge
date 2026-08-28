import { useState } from 'react';
import { useLocation, useNavigate, useParams } from 'react-router-dom';
import { applicationsApi } from '../../api/applications.js';
import { offersApi } from '../../api/offers.js';
import { profileApi } from '../../api/profile.js';
import ApplyDialog from '../../components/ApplyDialog/ApplyDialog.jsx';
import Button from '../../components/Button/Button.jsx';
import ErrorState from '../../components/ErrorState/ErrorState.jsx';
import MatchNote from '../../components/MatchNote/MatchNote.jsx';
import OfferDocument from '../../components/OfferDocument/OfferDocument.jsx';
import SaveOffer from '../../components/SaveOffer/SaveOffer.jsx';
import Skeleton from '../../components/Skeleton/Skeleton.jsx';
import StatusBadge from '../../components/StatusBadge/StatusBadge.jsx';
import { useToast } from '../../components/Toast/ToastContext.jsx';
import { CONTRACT_LABELS, DEGREE_LABELS, REMOTE_LABELS } from '../../constants/enums.js';
import { longDate, salaryText } from '../../constants/format.js';
import { useAuth } from '../../context/AuthContext.jsx';
import useResource from '../../hooks/useResource.js';
import Workspace from '../Workspace/Workspace.jsx';
import './offerPage.css';

/** One hard fact, dropped entirely when the offer does not state it. */
function Term({ label, children }) {
  if (!children) return null;
  return (
    <div className="terms__item">
      <dt className="terms__value">{children}</dt>
      <dd className="terms__label">{label}</dd>
    </div>
  );
}

/**
 * One offer at full length. The description and the profile sought run in the
 * main column, with the terms and the way to act on the offer beside them, so
 * the decision stays in reach at any depth of the page.
 */
export default function OfferPage() {
  const { id } = useParams();
  const location = useLocation();
  const navigate = useNavigate();
  const toast = useToast();
  const { user } = useAuth();

  const [applying, setApplying] = useState(false);
  const [applied, setApplied] = useState(false);
  const [choosing, setChoosing] = useState(false);
  const [documents, setDocuments] = useState([]);

  const isCandidate = user.role === 'CANDIDAT';

  const { status, data, reload, pending, leaving } = useResource(async () => {
    const detail = await offersApi.detail(id);
    // Only a candidate can apply, so only a candidate needs their documents.
    if (isCandidate) {
      setDocuments((await profileApi.read()).cvs);
    }
    return detail;
  }, [id]);

  const back = location.state?.from ? { to: location.state.from, label: 'Retour' } : null;

  async function apply(cvId) {
    setApplying(true);
    try {
      await applicationsApi.apply(Number(id), cvId);
      setApplied(true);
      setChoosing(false);
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

  const { offer, saved, publisherName, alreadyApplied, applicationCount, match } = data;
  const hasApplied = alreadyApplied || applied;
  const chosen = documents.find((cv) => cv.isDefault) ?? documents[0];

  return (
    <Workspace
      title={offer.title}
      subtitle={[offer.company, offer.location].filter(Boolean).join(' · ')}
      back={back}
      // How many applied is the recruiter's business; the DTO already withholds
      // it from anyone else, so an absent count simply shows nothing.
      stats={applicationCount == null ? [] : [
        { value: applicationCount, label: applicationCount > 1 ? 'candidatures' : 'candidature' },
      ]}
      action={(
        <div className="vitrine__marks">
          <StatusBadge status={offer.status} />
          {isCandidate && <SaveOffer offerId={offer.id} saved={saved} />}
        </div>
      )}
    >
      <div className="doc doc--split doc--side-first">
        {/* What the offer is worth deciding on: the terms, the way to act on
            them, and who published it. */}
        <aside className="doc__side">
          <dl className="terms terms--stacked">
            <Term label="Contrat">{CONTRACT_LABELS[offer.contractType]}</Term>
            <Term label="Lieu">{offer.location}</Term>
            <Term label="Télétravail">{REMOTE_LABELS[offer.remoteMode]}</Term>
            <Term label="Niveau minimum">{DEGREE_LABELS[offer.requiredDegree]}</Term>
            <Term label="Rémunération">{salaryText(offer.salaryMin, offer.salaryMax)}</Term>
          </dl>

          {isCandidate && !hasApplied && (
            <MatchNote match={match} requiredDegree={offer.requiredDegree} />
          )}

          {isCandidate && (
            <div className="actionbar">
              {hasApplied ? (
                <>
                  <p className="vitrine__applied">Vous avez postulé à cette offre.</p>
                  <Button variant="secondary" onClick={() => navigate('/mes-candidatures')}>
                    Suivre
                  </Button>
                </>
              ) : (
                <>
                  <p className="actionbar__note">
                    {chosen ? `CV : ${chosen.label}` : 'Aucun CV déposé'}
                  </p>
                  <Button
                    disabled={match != null && !match.compatible}
                    onClick={() => setChoosing(true)}
                  >
                    Postuler
                  </Button>
                </>
              )}
            </div>
          )}

          <p className="doc__meta">
            {offer.publicationDate ? `Publiée le ${longDate(offer.publicationDate)}` : 'Non publiée'}
            {publisherName && ` par ${publisherName}`}
          </p>
        </aside>

        <div className="doc__main">
          <OfferDocument description={offer.description} requirements={offer.requirements} />
        </div>
      </div>

      {choosing && (
        <ApplyDialog
          offer={offer}
          documents={documents}
          busy={applying}
          onConfirm={apply}
          onCancel={() => setChoosing(false)}
        />
      )}
    </Workspace>
  );
}
