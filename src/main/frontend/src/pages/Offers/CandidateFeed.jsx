import { useState } from 'react';
import { applicationsApi } from '../../api/applications.js';
import { offersApi } from '../../api/offers.js';
import Button from '../../components/Button/Button.jsx';
import CardGrid from '../../components/CardGrid/CardGrid.jsx';
import EmptyState from '../../components/EmptyState/EmptyState.jsx';
import ErrorState from '../../components/ErrorState/ErrorState.jsx';
import OfferLink from '../../components/OfferLink/OfferLink.jsx';
import Skeleton from '../../components/Skeleton/Skeleton.jsx';
import { useToast } from '../../components/Toast/ToastContext.jsx';
import { CONTRACT_LABELS, DEGREE_LABELS, REMOTE_LABELS } from '../../constants/enums.js';
import { salaryText } from '../../constants/format.js';
import useResource from '../../hooks/useResource.js';
import Workspace from '../Workspace/Workspace.jsx';
import './candidateFeed.css';

export default function CandidateFeed() {
  const toast = useToast();
  const [applied, setApplied] = useState(() => new Set());
  const [pending, setPending] = useState(null);

  const { status, data, reload, pending: loading, leaving } = useResource(async () => {
    const [feed, mine] = await Promise.all([offersApi.feed(), applicationsApi.mine()]);
    setApplied(new Set(mine.map((a) => a.offerId)));
    return feed;
  });

  async function apply(offer) {
    setPending(offer.id);
    try {
      await applicationsApi.apply(offer.id);
      setApplied((current) => new Set(current).add(offer.id));
      toast.success(`Candidature envoyée pour « ${offer.title} ».`);
    } catch (apiError) {
      toast.error(apiError.message);
    } finally {
      setPending(null);
    }
  }

  return (
    <Workspace title="Offres">
      <p className="feed__intro">
        Ces offres correspondent à votre profil : vous possédez chaque trait obligatoire et le
        niveau d'études requis.
      </p>

      {loading && <Skeleton leaving={leaving} label="Recherche des offres compatibles" />}

      {status === 'error' && (
        <ErrorState onRetry={reload}>
          Les offres n'ont pas pu être chargées. Réessayez dans un instant.
        </ErrorState>
      )}

      {status === 'ready' && data.length === 0 && (
        <EmptyState
          title="Aucune offre ne correspond pour le moment."
          actionLabel="Compléter mon profil"
          actionTo="/profil"
        >
          Une offre n'apparaît ici que si vous possédez tous ses traits obligatoires et le niveau
          demandé. Enrichir votre profil élargit les correspondances.
        </EmptyState>
      )}

      {status === 'ready' && data.length > 0 && (
        <CardGrid label="Offres compatibles">
          {data.map((offer) => {
            const required = offer.requirements.filter((r) => r.mandatory);
            const plus = offer.requirements.filter((r) => !r.mandatory);
            const salary = salaryText(offer.salaryMin, offer.salaryMax);
            const hasApplied = applied.has(offer.id);
            return (
              <li key={offer.id} className="tile tile--openable">
                <div className="tile__head">
                  <h2 className="tile__title">
                    <OfferLink id={offer.id} className="tile__stretch">{offer.title}</OfferLink>
                  </h2>
                  <span className="feedcard__contract">{CONTRACT_LABELS[offer.contractType]}</span>
                </div>

                <p className="tile__facts">
                  {offer.location && <span>{offer.location}</span>}
                  {offer.remoteMode && <span>{REMOTE_LABELS[offer.remoteMode]}</span>}
                  <span>Niveau d'études : {DEGREE_LABELS[offer.requiredDegree]}</span>
                  {salary && <span className="mono">{salary}</span>}
                </p>

                <p className="tile__desc">{offer.description}</p>

                <div className="feedcard__traits">
                  {required.map((r) => (
                    <span key={r.traitId} className="tag tag--required">{r.label}</span>
                  ))}
                  {plus.map((r) => (
                    <span key={r.traitId} className="tag tag--plus">{r.label}</span>
                  ))}
                </div>

                <div className="tile__foot">
                  {hasApplied ? (
                    <span className="feedcard__applied">Candidature envoyée</span>
                  ) : (
                    <Button onClick={() => apply(offer)} loading={pending === offer.id}>
                      Postuler
                    </Button>
                  )}
                </div>
              </li>
            );
          })}
        </CardGrid>
      )}
    </Workspace>
  );
}
