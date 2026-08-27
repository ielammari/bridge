import { useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { applicationsApi } from '../../api/applications.js';
import { offersApi } from '../../api/offers.js';
import { profileApi } from '../../api/profile.js';
import ApplyDialog from '../../components/ApplyDialog/ApplyDialog.jsx';
import Button from '../../components/Button/Button.jsx';
import CardGrid from '../../components/CardGrid/CardGrid.jsx';
import EmptyState from '../../components/EmptyState/EmptyState.jsx';
import ErrorState from '../../components/ErrorState/ErrorState.jsx';
import { MatchMark } from '../../components/MatchNote/MatchNote.jsx';
import OfferCard from '../../components/OfferCard/OfferCard.jsx';
import SaveOffer from '../../components/SaveOffer/SaveOffer.jsx';
import Skeleton from '../../components/Skeleton/Skeleton.jsx';
import Toggle from '../../components/Toggle/Toggle.jsx';
import { useToast } from '../../components/Toast/ToastContext.jsx';
import useResource from '../../hooks/useResource.js';
import Workspace from '../Workspace/Workspace.jsx';
import './candidateFeed.css';

// Which offers are on screen, kept in the address.
const SCOPE = 'voir';
const ALL = 'toutes';

const SCOPES = [
  { value: 'compatibles', label: 'Compatibles' },
  { value: ALL, label: 'Toutes' },
];

export default function CandidateFeed() {
  const toast = useToast();
  const [params, setParams] = useSearchParams();
  const [applied, setApplied] = useState(() => new Set());
  const [documents, setDocuments] = useState([]);
  const [saved, setSaved] = useState(() => new Set());
  const [choosing, setChoosing] = useState(null);
  const [sending, setSending] = useState(false);

  const scope = params.get(SCOPE) === ALL ? ALL : 'compatibles';
  const wide = scope === ALL;

  function setScope(next) {
    const changed = new URLSearchParams(params);
    if (next === ALL) {
      changed.set(SCOPE, ALL);
    } else {
      changed.delete(SCOPE);
    }
    setParams(changed, { replace: true });
  }

  const { status, data, reload, pending: loading, leaving } = useResource(async () => {
    const [feed, mine, profile, kept] = await Promise.all([
      offersApi.feed(wide ? 'all' : 'compatible'),
      applicationsApi.mine(),
      profileApi.read(),
      offersApi.saved(),
    ]);
    setApplied(new Set(mine.map((a) => a.offerId)));
    setDocuments(profile.cvs);
    setSaved(new Set(kept.map((entry) => entry.offer.id)));
    return feed;
  }, [wide]);

  const entries = data ?? [];
  // Measured against the whole market, so the counts do not follow the toggle.
  const matched = entries.filter((entry) => entry.match.compatible);

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
      info="Une offre est compatible lorsque vous possédez tous ses traits obligatoires et au moins le niveau d'études demandé. « Toutes » montre aussi celles qui ne le sont pas, sans permettre d'y postuler."
      stats={status === 'ready' ? [
        { value: matched.length, label: matched.length > 1 ? 'compatibles' : 'compatible' },
        { value: matched.filter((entry) => !applied.has(entry.offer.id)).length, label: 'à postuler' },
      ] : []}
      action={(
        <Toggle
          name="scope"
          label="Offres à afficher"
          value={scope}
          options={SCOPES}
          onChange={setScope}
        />
      )}
    >

      {loading && <Skeleton leaving={leaving} label="Recherche des offres" />}

      {status === 'error' && (
        <ErrorState onRetry={reload}>
          Les offres n'ont pas pu être chargées. Réessayez dans un instant.
        </ErrorState>
      )}

      {status === 'ready' && entries.length === 0 && (
        wide ? (
          <EmptyState title="Aucune offre publiée pour le moment.">
            Rien n'est ouvert au recrutement. Revenez plus tard, ou enrichissez votre profil en
            attendant.
          </EmptyState>
        ) : (
          <EmptyState
            title="Aucune offre ne correspond pour le moment."
            actionLabel="Compléter mon profil"
            actionTo="/profil"
          >
            Une offre est compatible si vous possédez tous ses traits obligatoires et le niveau
            demandé. Basculez sur « Toutes » pour voir le reste du marché.
          </EmptyState>
        )
      )}

      {status === 'ready' && entries.length > 0 && (
        <CardGrid label={wide ? 'Toutes les offres' : 'Offres compatibles'}>
          {entries.map(({ offer, match }) => {
            // An offer already applied to says so, and nothing else.
            const sent = applied.has(offer.id);
            return (
              <OfferCard
                key={offer.id}
                offer={offer}
                badge={<SaveOffer offerId={offer.id} saved={saved.has(offer.id)} />}
              >
                {sent ? (
                  <span className="offercard__applied">Candidature envoyée</span>
                ) : (
                  <>
                    <MatchMark match={match} />
                    <Button disabled={!match.compatible} onClick={() => setChoosing(offer)}>
                      Postuler
                    </Button>
                  </>
                )}
              </OfferCard>
            );
          })}
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
