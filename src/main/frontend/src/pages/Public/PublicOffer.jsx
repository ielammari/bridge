import { Link, useLocation, useParams } from 'react-router-dom';
import { publicApi } from '../../api/public.js';
import ErrorState from '../../components/ErrorState/ErrorState.jsx';
import OfferDocument from '../../components/OfferDocument/OfferDocument.jsx';
import PublicLayout from '../../components/PublicChrome/PublicLayout.jsx';
import Skeleton from '../../components/Skeleton/Skeleton.jsx';
import { CONTRACT_LABELS, DEGREE_LABELS, REMOTE_LABELS } from '../../constants/enums.js';
import { longDate, salaryText } from '../../constants/format.js';
import { withSuite } from '../../constants/navigation.js';
import { useAuth } from '../../context/AuthContext.jsx';
import useDocumentTitle from '../../hooks/useDocumentTitle.js';
import useResource from '../../hooks/useResource.js';
import './public.css';

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
 * One open position, readable by anyone. The document is the application's own;
 * what differs is the way to act on it, which for a visitor runs through
 * signing in and returns here as the offer they came for.
 */
export default function PublicOffer() {
  const { id } = useParams();
  const location = useLocation();
  const { user, loading } = useAuth();

  const { status, data, reload, pending, leaving } = useResource(() => publicApi.offer(id), [id]);

  useDocumentTitle(data?.offer.title ?? 'Offre');

  const back = location.state?.from ?? '/emplois';
  // Applying happens inside the application, on the page that knows whether
  // this candidate qualifies. A visitor signs in and is returned to it.
  const inside = `/offres/${id}`;
  const applyTo = user ? inside : withSuite('/connexion', inside);
  const canApply = !loading && (!user || user.role === 'CANDIDAT');

  return (
    <PublicLayout>
      <section className="pubband pubband--head">
        <div className="pubband__inner pubhead-band">
          <div className="pubhead-band__title">
            <Link to={back} className="pubback">Retour aux offres</Link>
            <h1 className="pubtitle">{status === 'ready' ? data.offer.title : 'Offre'}</h1>
            {status === 'ready' && (
              <p className="pubtitle__note">
                {[data.offer.company, data.offer.location].filter(Boolean).join(' · ')}
              </p>
            )}
          </div>
        </div>
      </section>

      <section className="pubband pubband--doc">
        <div className="pubband__inner">
          {pending && <Skeleton variant="record" leaving={leaving} label="Chargement de l'offre" />}

          {status === 'error' && (
            <ErrorState onRetry={reload}>
              Cette offre n'a pas pu être chargée. Elle a peut-être été clôturée.
            </ErrorState>
          )}

          {status === 'ready' && (
            <div className="doc doc--split doc--side-first">
              <aside className="doc__side">
                <dl className="terms terms--stacked">
                  <Term label="Contrat">{CONTRACT_LABELS[data.offer.contractType]}</Term>
                  <Term label="Lieu">{data.offer.location}</Term>
                  <Term label="Télétravail">{REMOTE_LABELS[data.offer.remoteMode]}</Term>
                  <Term label="Niveau minimum">{DEGREE_LABELS[data.requiredDegree]}</Term>
                  <Term label="Rémunération">
                    {salaryText(data.offer.salaryMin, data.offer.salaryMax)}
                  </Term>
                </dl>

                {canApply && (
                  <div className="actionbar">
                    <p className="actionbar__note">
                      {user ? 'Compte candidat' : 'Compte candidat requis'}
                    </p>
                    <Link to={applyTo} className="button button--primary">Postuler</Link>
                  </div>
                )}

                <p className="doc__meta">Publiée le {longDate(data.offer.publicationDate)}</p>
              </aside>

              <div className="doc__main">
                <OfferDocument description={data.description} requirements={data.requirements} />
              </div>
            </div>
          )}
        </div>
      </section>
    </PublicLayout>
  );
}
