import { offersApi } from '../../api/offers.js';
import CardGrid from '../../components/CardGrid/CardGrid.jsx';
import EmptyState from '../../components/EmptyState/EmptyState.jsx';
import ErrorState from '../../components/ErrorState/ErrorState.jsx';
import OfferCard from '../../components/OfferCard/OfferCard.jsx';
import Skeleton from '../../components/Skeleton/Skeleton.jsx';
import StatusBadge from '../../components/StatusBadge/StatusBadge.jsx';
import TabNav from '../../components/TabNav/TabNav.jsx';
import { useAuth } from '../../context/AuthContext.jsx';
import useResource from '../../hooks/useResource.js';
import Workspace from '../Workspace/Workspace.jsx';
import { historyTabs } from './tabs.js';

/** Offers that no longer accept applications. */
export default function PastOffers() {
  const { user } = useAuth();
  const { status, data, reload, pending, leaving } = useResource(() => offersApi.list());

  const closed = (data ?? []).filter((offer) => offer.status === 'CLOTUREE');

  return (
    <Workspace
      title="Historique"
      toolbar={<TabNav items={historyTabs(user.role)} label="Sections de l'historique" />}
    >

      {pending && <Skeleton leaving={leaving} label="Chargement des offres clôturées" />}

      {status === 'error' && (
        <ErrorState onRetry={reload}>
          Les offres n'ont pas pu être chargées. Réessayez dans un instant.
        </ErrorState>
      )}

      {status === 'ready' && closed.length === 0 && (
        <EmptyState title="Aucune offre clôturée.">
          Une offre clôturée quitte la liste active et son dossier reste consultable ici.
        </EmptyState>
      )}

      {status === 'ready' && closed.length > 0 && (
        <CardGrid label="Offres clôturées">
          {closed.map((offer) => (
            <OfferCard key={offer.id} offer={offer}
              badge={<StatusBadge status={offer.status} />} />
          ))}
        </CardGrid>
      )}
    </Workspace>
  );
}
