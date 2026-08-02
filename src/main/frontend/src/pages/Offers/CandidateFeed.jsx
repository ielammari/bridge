import { useState } from 'react';
import { applicationsApi } from '../../api/applications.js';
import { offersApi } from '../../api/offers.js';
import Button from '../../components/Button/Button.jsx';
import EmptyState from '../../components/EmptyState/EmptyState.jsx';
import ErrorState from '../../components/ErrorState/ErrorState.jsx';
import Skeleton from '../../components/Skeleton/Skeleton.jsx';
import { useToast } from '../../components/Toast/ToastContext.jsx';
import { CONTRACT_LABELS, DEGREE_LABELS, REMOTE_LABELS } from '../../constants/enums.js';
import { euros } from '../../constants/format.js';
import useResource from '../../hooks/useResource.js';
import Workspace from '../Workspace/Workspace.jsx';
import './candidateFeed.css';

function salaryText(min, max) {
  if (min && max) return `${euros(min)} à ${euros(max)}`;
  if (min) return `À partir de ${euros(min)}`;
  if (max) return `Jusqu'à ${euros(max)}`;
  return null;
}

export default function CandidateFeed() {
  const toast = useToast();
  const [applied, setApplied] = useState(() => new Set());
  const [pending, setPending] = useState(null);

  const { status, data, reload } = useResource(async () => {
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
        diplôme requis.
      </p>

      {status === 'loading' && <Skeleton label="Recherche des offres compatibles" />}

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
          Une offre n'apparaît ici que si vous possédez tous ses traits obligatoires et le diplôme
          demandé. Enrichir votre profil élargit les correspondances.
        </EmptyState>
      )}

      {status === 'ready' && data.length > 0 && (
        <ul className="feed__list">
          {data.map((offer) => {
            const required = offer.requirements.filter((r) => r.mandatory);
            const plus = offer.requirements.filter((r) => !r.mandatory);
            const salary = salaryText(offer.salaryMin, offer.salaryMax);
            const hasApplied = applied.has(offer.id);
            return (
              <li key={offer.id} className="feedcard">
                <div className="feedcard__head">
                  <h2 className="feedcard__title">{offer.title}</h2>
                  <span className="feedcard__contract">{CONTRACT_LABELS[offer.contractType]}</span>
                </div>

                <p className="feedcard__facts">
                  {offer.location && <span>{offer.location}</span>}
                  {offer.remoteMode && <span>{REMOTE_LABELS[offer.remoteMode]}</span>}
                  <span>Diplôme : {DEGREE_LABELS[offer.requiredDegree]}</span>
                  {salary && <span className="mono">{salary}</span>}
                </p>

                <p className="feedcard__desc">{offer.description}</p>

                <div className="feedcard__traits">
                  {required.map((r) => (
                    <span key={r.traitId} className="tag tag--required">{r.label}</span>
                  ))}
                  {plus.map((r) => (
                    <span key={r.traitId} className="tag tag--plus">{r.label}</span>
                  ))}
                </div>

                <div className="feedcard__foot">
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
        </ul>
      )}
    </Workspace>
  );
}
