import { useEffect, useState } from 'react';
import { offersApi } from '../../api/offers.js';
import { traitsApi } from '../../api/traits.js';
import Alert from '../../components/Alert/Alert.jsx';
import Button from '../../components/Button/Button.jsx';
import StatusBadge from '../../components/StatusBadge/StatusBadge.jsx';
import { CONTRACT_LABELS } from '../../constants/enums.js';
import Workspace from '../Workspace/Workspace.jsx';
import OfferForm from './OfferForm.jsx';
import './hrOffers.css';

export default function HrOffers() {
  const [status, setStatus] = useState('loading');
  const [offers, setOffers] = useState([]);
  const [catalogue, setCatalogue] = useState([]);
  const [view, setView] = useState({ mode: 'list' }); // list | create | edit
  const [submitting, setSubmitting] = useState(false);
  const [banner, setBanner] = useState(null);

  useEffect(() => {
    let cancelled = false;
    Promise.all([offersApi.list(), traitsApi.catalogue()])
      .then(([list, cats]) => {
        if (cancelled) return;
        setOffers(list);
        setCatalogue(cats);
        setStatus('ready');
      })
      .catch(() => {
        if (!cancelled) setStatus('error');
      });
    return () => {
      cancelled = true;
    };
  }, []);

  async function refresh() {
    setOffers(await offersApi.list());
  }

  async function submit(payload) {
    setSubmitting(true);
    try {
      if (view.mode === 'edit') {
        await offersApi.update(view.offer.id, payload);
        setBanner('Offre mise à jour.');
      } else {
        await offersApi.create(payload);
        setBanner(payload.publishNow ? 'Offre publiée.' : 'Brouillon enregistré.');
      }
      await refresh();
      setView({ mode: 'list' });
    } finally {
      setSubmitting(false);
    }
  }

  async function act(id, action) {
    await offersApi[action](id);
    await refresh();
  }

  if (status === 'loading') {
    return <Workspace title="Offres"><p className="hroffers__muted">Chargement des offres...</p></Workspace>;
  }
  if (status === 'error') {
    return <Workspace title="Offres"><Alert>Les offres n'ont pas pu être chargées.</Alert></Workspace>;
  }

  if (view.mode !== 'list') {
    return (
      <Workspace title="Offres">
        <OfferForm
          mode={view.mode}
          offer={view.offer}
          catalogue={catalogue}
          submitting={submitting}
          onSubmit={submit}
          onCancel={() => setView({ mode: 'list' })}
        />
      </Workspace>
    );
  }

  return (
    <Workspace title="Offres">
      <div className="hroffers__bar">
        <p className="hroffers__count">{offers.length} offre{offers.length > 1 ? 's' : ''}</p>
        <Button onClick={() => setView({ mode: 'create' })}>Nouvelle offre</Button>
      </div>

      {banner && <Alert tone="info">{banner}</Alert>}

      {offers.length === 0 ? (
        <div className="hroffers__empty">
          <p>Vous n'avez pas encore créé d'offre.</p>
          <Button onClick={() => setView({ mode: 'create' })}>Créer la première offre</Button>
        </div>
      ) : (
        <ul className="hroffers__list">
          {offers.map((offer) => (
            <li key={offer.id} className="offercard">
              <div className="offercard__main">
                <div className="offercard__title-row">
                  <h3 className="offercard__title">{offer.title}</h3>
                  <StatusBadge status={offer.status} />
                </div>
                <p className="offercard__meta">
                  <span>{CONTRACT_LABELS[offer.contractType]}</span>
                  {offer.location && <span>{offer.location}</span>}
                  <span className="mono">{offer.requirements.filter((r) => r.mandatory).length} obligatoire(s)</span>
                </p>
              </div>
              <div className="offercard__actions">
                <Button variant="text" onClick={() => setView({ mode: 'edit', offer })}>Modifier</Button>
                {offer.status !== 'PUBLIEE' && offer.status !== 'CLOTUREE' && (
                  <Button variant="secondary" onClick={() => act(offer.id, 'publish')}>Publier</Button>
                )}
                {offer.status === 'PUBLIEE' && (
                  <Button variant="secondary" onClick={() => act(offer.id, 'close')}>Clôturer</Button>
                )}
              </div>
            </li>
          ))}
        </ul>
      )}
    </Workspace>
  );
}
