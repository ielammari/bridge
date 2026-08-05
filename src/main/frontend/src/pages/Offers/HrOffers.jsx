import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { offersApi } from '../../api/offers.js';
import Button from '../../components/Button/Button.jsx';
import CardGrid from '../../components/CardGrid/CardGrid.jsx';
import ConfirmDialog from '../../components/ConfirmDialog/ConfirmDialog.jsx';
import EmptyState from '../../components/EmptyState/EmptyState.jsx';
import ErrorState from '../../components/ErrorState/ErrorState.jsx';
import OfferLink from '../../components/OfferLink/OfferLink.jsx';
import Skeleton from '../../components/Skeleton/Skeleton.jsx';
import StatusBadge from '../../components/StatusBadge/StatusBadge.jsx';
import { useToast } from '../../components/Toast/ToastContext.jsx';
import { CONTRACT_LABELS } from '../../constants/enums.js';
import useResource from '../../hooks/useResource.js';
import Workspace from '../Workspace/Workspace.jsx';
import './hrOffers.css';

// Publishing exposes an offer to matching candidates and closing withdraws it.
// Neither is reversible, so both are confirmed.
const ACTIONS = {
  publish: {
    title: 'Publier cette offre ?',
    body: 'Elle deviendra visible pour tous les candidats dont le profil correspond, et ils pourront postuler immédiatement.',
    confirmLabel: 'Publier l\'offre',
    tone: 'primary',
    done: 'Offre publiée.',
  },
  close: {
    title: 'Clôturer cette offre ?',
    body: 'Elle disparaîtra du fil des candidats et n\'acceptera plus de nouvelles candidatures. Les candidatures déjà reçues restent traitables.',
    confirmLabel: 'Clôturer l\'offre',
    tone: 'danger',
    done: 'Offre clôturée.',
  },
};

export default function HrOffers() {
  const navigate = useNavigate();
  const toast = useToast();

  const { status, data, setData, reload, pending, leaving } = useResource(() => offersApi.list());
  const [confirming, setConfirming] = useState(null);
  const [busy, setBusy] = useState(false);

  // Closed offers move to the history.
  const offers = (data ?? []).filter((offer) => offer.status !== 'CLOTUREE');

  async function run() {
    const { offer, action } = confirming;
    setBusy(true);
    try {
      const updated = await offersApi[action](offer.id);
      setData((list) => list.map((o) => (o.id === updated.id ? updated : o)));
      toast.success(ACTIONS[action].done);
      setConfirming(null);
    } catch (apiError) {
      toast.error(apiError.message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <Workspace title="Offres">
      {pending && <Skeleton leaving={leaving} label="Chargement des offres" />}

      {status === 'error' && (
        <ErrorState onRetry={reload}>
          Les offres n'ont pas pu être chargées. Réessayez dans un instant.
        </ErrorState>
      )}

      {status === 'ready' && (
        <>
          <div className="hroffers__bar">
            <p className="hroffers__count">
              {offers.length === 0
                ? 'Aucune offre'
                : `${offers.length} offre${offers.length > 1 ? 's' : ''}`}
            </p>
            <Button onClick={() => navigate('/offres/nouvelle')}>Nouvelle offre</Button>
          </div>

          {offers.length === 0 ? (
            <EmptyState
              title="Vous n'avez pas encore créé d'offre."
              actionLabel="Créer la première offre"
              onAction={() => navigate('/offres/nouvelle')}
            >
              Une offre décrit le poste et les traits recherchés. Ce sont ces traits qui décident
              quels candidats la voient.
            </EmptyState>
          ) : (
            <CardGrid label="Vos offres">
              {offers.map((offer) => {
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
                      <span className="mono">
                        {required} trait{required > 1 ? 's' : ''} obligatoire{required > 1 ? 's' : ''}
                      </span>
                    </p>

                    <p className="tile__desc">{offer.description}</p>

                    <div className="tile__foot">
                      <Button variant="text" onClick={() => navigate(`/offres/${offer.id}/modifier`)}>
                        Modifier
                      </Button>
                      {offer.status === 'BROUILLON' && (
                        <Button variant="secondary" onClick={() => setConfirming({ offer, action: 'publish' })}>
                          Publier
                        </Button>
                      )}
                      {offer.status === 'PUBLIEE' && (
                        <Button variant="danger" onClick={() => setConfirming({ offer, action: 'close' })}>
                          Clôturer
                        </Button>
                      )}
                    </div>
                  </li>
                );
              })}
            </CardGrid>
          )}
        </>
      )}

      {confirming && (
        <ConfirmDialog
          open
          title={ACTIONS[confirming.action].title}
          confirmLabel={ACTIONS[confirming.action].confirmLabel}
          tone={ACTIONS[confirming.action].tone}
          busy={busy}
          onConfirm={run}
          onCancel={() => setConfirming(null)}
        >
          <strong>{confirming.offer.title}</strong>. {ACTIONS[confirming.action].body}
        </ConfirmDialog>
      )}
    </Workspace>
  );
}
