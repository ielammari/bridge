import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { offersApi } from '../../api/offers.js';
import Alert from '../../components/Alert/Alert.jsx';
import { CONTRACT_LABELS, DEGREE_LABELS, REMOTE_LABELS } from '../../constants/enums.js';
import Workspace from '../Workspace/Workspace.jsx';
import './candidateFeed.css';

function salaryText(min, max) {
  if (min && max) return `${Number(min).toLocaleString('fr-FR')} - ${Number(max).toLocaleString('fr-FR')} €`;
  if (min) return `À partir de ${Number(min).toLocaleString('fr-FR')} €`;
  if (max) return `Jusqu'à ${Number(max).toLocaleString('fr-FR')} €`;
  return null;
}

export default function CandidateFeed() {
  const [status, setStatus] = useState('loading');
  const [offers, setOffers] = useState([]);

  useEffect(() => {
    let cancelled = false;
    offersApi.feed()
      .then((data) => {
        if (cancelled) return;
        setOffers(data);
        setStatus('ready');
      })
      .catch(() => {
        if (!cancelled) setStatus('error');
      });
    return () => {
      cancelled = true;
    };
  }, []);

  if (status === 'loading') {
    return <Workspace title="Offres"><p className="feed__muted">Recherche des offres compatibles...</p></Workspace>;
  }
  if (status === 'error') {
    return <Workspace title="Offres"><Alert>Les offres n'ont pas pu être chargées.</Alert></Workspace>;
  }

  return (
    <Workspace title="Offres">
      <p className="feed__intro">
        Ces offres correspondent à votre profil : vous possédez chaque trait obligatoire et le
        diplôme requis.
      </p>

      {offers.length === 0 ? (
        <div className="feed__empty">
          <p className="feed__empty-title">Aucune offre ne correspond pour le moment.</p>
          <p>
            Complétez votre profil (diplôme et traits) pour élargir les correspondances. De
            nouvelles offres peuvent aussi être publiées plus tard.
          </p>
          <Link className="feed__empty-link" to="/profil">Compléter mon profil</Link>
        </div>
      ) : (
        <ul className="feed__list">
          {offers.map((offer) => {
            const required = offer.requirements.filter((r) => r.mandatory);
            const plus = offer.requirements.filter((r) => !r.mandatory);
            const salary = salaryText(offer.salaryMin, offer.salaryMax);
            return (
              <li key={offer.id} className="feedcard">
                <div className="feedcard__head">
                  <h3 className="feedcard__title">{offer.title}</h3>
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
              </li>
            );
          })}
        </ul>
      )}
    </Workspace>
  );
}
