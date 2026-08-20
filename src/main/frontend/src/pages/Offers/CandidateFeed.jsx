import { useState } from 'react';
import { applicationsApi } from '../../api/applications.js';
import { offersApi } from '../../api/offers.js';
import { profileApi } from '../../api/profile.js';
import ApplyDialog from '../../components/ApplyDialog/ApplyDialog.jsx';
import Button from '../../components/Button/Button.jsx';
import CardGrid from '../../components/CardGrid/CardGrid.jsx';
import EmptyState from '../../components/EmptyState/EmptyState.jsx';
import ErrorState from '../../components/ErrorState/ErrorState.jsx';
import OfferCard from '../../components/OfferCard/OfferCard.jsx';
import SaveOffer from '../../components/SaveOffer/SaveOffer.jsx';
import Skeleton from '../../components/Skeleton/Skeleton.jsx';
import { useToast } from '../../components/Toast/ToastContext.jsx';
import useResource from '../../hooks/useResource.js';
import Workspace from '../Workspace/Workspace.jsx';
import './candidateFeed.css';

export default function CandidateFeed() {
  const toast = useToast();
  const [applied, setApplied] = useState(() => new Set());
  const [documents, setDocuments] = useState([]);
  const [saved, setSaved] = useState(() => new Set());
  const [choosing, setChoosing] = useState(null);
  const [sending, setSending] = useState(false);

  const { status, data, reload, pending: loading, leaving } = useResource(async () => {
    const [feed, mine, profile, kept] = await Promise.all([
      offersApi.feed(), applicationsApi.mine(), profileApi.read(), offersApi.saved(),
    ]);
    setApplied(new Set(mine.map((a) => a.offerId)));
    setDocuments(profile.cvs);
    setSaved(new Set(kept.map((o) => o.id)));
    return feed;
  });

  async function apply(cvId) {
    const offer = choosing;
    setSending(true);
    try {
      await applicationsApi.apply(offer.id, cvId);
      setApplied((current) => new Set(current).add(offer.id));
      setChoosing(null);
      toast.success(`Candidature envoyée pour « ${offer.title} ».`);
    } catch (apiError) {
      toast.error(apiError.message);
    } finally {
      setSending(false);
    }
  }

  return (
    <Workspace
      title="Offres"
      info="Une offre apparaît ici lorsque vous possédez tous ses traits obligatoires et au moins le niveau d'études demandé."
      // How many match, and how many of those are still open to you.
      stats={status === 'ready' ? [
        { value: data.length, label: data.length > 1 ? 'compatibles' : 'compatible' },
        { value: data.filter((offer) => !applied.has(offer.id)).length, label: 'à postuler' },
      ] : []}
    >

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
          {data.map((offer) => (
            <OfferCard
              key={offer.id}
              offer={offer}
              badge={<SaveOffer offerId={offer.id} saved={saved.has(offer.id)} />}
            >
              {applied.has(offer.id) ? (
                <span className="offercard__applied">Candidature envoyée</span>
              ) : (
                <Button onClick={() => setChoosing(offer)}>Postuler</Button>
              )}
            </OfferCard>
          ))}
        </CardGrid>
      )}
      {choosing && (
        <ApplyDialog
          offer={choosing}
          documents={documents}
          busy={sending}
          onConfirm={apply}
          onCancel={() => setChoosing(null)}
        />
      )}
    </Workspace>
  );
}
