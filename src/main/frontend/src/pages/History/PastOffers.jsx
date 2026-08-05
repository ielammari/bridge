import { offersApi } from '../../api/offers.js';
import CardGrid from '../../components/CardGrid/CardGrid.jsx';
import EmptyState from '../../components/EmptyState/EmptyState.jsx';
import ErrorState from '../../components/ErrorState/ErrorState.jsx';
import OfferLink from '../../components/OfferLink/OfferLink.jsx';
import Skeleton from '../../components/Skeleton/Skeleton.jsx';
import StatusBadge from '../../components/StatusBadge/StatusBadge.jsx';
import TabNav from '../../components/TabNav/TabNav.jsx';
import { CONTRACT_LABELS } from '../../constants/enums.js';
import { longDate } from '../../constants/format.js';
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
    <Workspace title="Historique">
      <TabNav items={historyTabs(user.role)} label="Sections de l'historique" />

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
          {closed.map((offer) => {
            const required = offer.requirements.filter((r) => r.mandatory).length;
            return (
              <li key={offer.id} className="tile tile--openable">
                <div className="tile__head">
                  <h2 className="tile__title">
                    <OfferLink id={offer.id} className="tile__stretch">{offer.title}</OfferLink>
                  </h2>
                  <StatusBadge status={offer.status} />
                </div>

                <p className="tile__facts">
                  <span>{CONTRACT_LABELS[offer.contractType]}</span>
                  {offer.location && <span>{offer.location}</span>}
                  {offer.publicationDate && <span>Publiée le {longDate(offer.publicationDate)}</span>}
                  <span className="mono">
                    {required} trait{required > 1 ? 's' : ''} obligatoire{required > 1 ? 's' : ''}
                  </span>
                </p>

                <p className="tile__desc">{offer.description}</p>
              </li>
            );
          })}
        </CardGrid>
      )}
    </Workspace>
  );
}
