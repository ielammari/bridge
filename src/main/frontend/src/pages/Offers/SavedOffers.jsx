import { offersApi } from '../../api/offers.js';
import CardGrid from '../../components/CardGrid/CardGrid.jsx';
import EmptyState from '../../components/EmptyState/EmptyState.jsx';
import ErrorState from '../../components/ErrorState/ErrorState.jsx';
import { MatchMark } from '../../components/MatchNote/MatchNote.jsx';
import OfferCard from '../../components/OfferCard/OfferCard.jsx';
import SaveOffer from '../../components/SaveOffer/SaveOffer.jsx';
import Skeleton from '../../components/Skeleton/Skeleton.jsx';
import useResource from '../../hooks/useResource.js';
import Workspace from '../Workspace/Workspace.jsx';

/**
 * The offers a candidate kept to come back to. Releasing one drops it from the
 * list at once: the page is the list. A kept offer can drift out of reach when
 * its traits change, so each says where the candidate stands.
 */
export default function SavedOffers() {
  const { status, data, setData, reload, pending, leaving } = useResource(() => offersApi.saved());
  const entries = data ?? [];

  return (
    <Workspace
      title="Offres enregistrées"
      stats={status === 'ready' ? [
        { value: entries.length, label: entries.length > 1 ? 'enregistrées' : 'enregistrée' },
      ] : []}
    >
      {pending && <Skeleton leaving={leaving} label="Chargement des offres enregistrées" />}

      {status === 'error' && (
        <ErrorState onRetry={reload}>
          Vos offres enregistrées n'ont pas pu être chargées.
        </ErrorState>
      )}

      {status === 'ready' && entries.length === 0 && (
        <EmptyState
          title="Aucune offre enregistrée."
          actionLabel="Parcourir les offres"
          actionTo="/offres"
        >
          Gardez une offre depuis la liste ou depuis sa page pour la retrouver ici.
        </EmptyState>
      )}

      {status === 'ready' && entries.length > 0 && (
        <CardGrid label="Offres enregistrées">
          {entries.map(({ offer, match }) => (
            <OfferCard
              key={offer.id}
              offer={offer}
              badge={(
                <SaveOffer
                  offerId={offer.id}
                  saved
                  onChange={(id) => setData((list) => list.filter((e) => e.offer.id !== id))}
                />
              )}
            >
              {!match.compatible && <MatchMark match={match} />}
            </OfferCard>
          ))}
        </CardGrid>
      )}
    </Workspace>
  );
}
