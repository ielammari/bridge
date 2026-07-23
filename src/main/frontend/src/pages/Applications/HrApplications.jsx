import { useEffect, useState } from 'react';
import { applicationsApi } from '../../api/applications.js';
import { offersApi } from '../../api/offers.js';
import Alert from '../../components/Alert/Alert.jsx';
import Button from '../../components/Button/Button.jsx';
import Icon from '../../components/Icon/Icon.jsx';
import Select from '../../components/Select/Select.jsx';
import StatusBadge from '../../components/StatusBadge/StatusBadge.jsx';
import Workspace from '../Workspace/Workspace.jsx';
import './hrApplications.css';

const dateFormat = new Intl.DateTimeFormat('fr-FR', { day: 'numeric', month: 'long', year: 'numeric' });

export default function HrApplications() {
  const [status, setStatus] = useState('loading');
  const [offers, setOffers] = useState([]);
  const [offerId, setOfferId] = useState('');
  const [applications, setApplications] = useState([]);
  const [loadingApps, setLoadingApps] = useState(false);

  useEffect(() => {
    let cancelled = false;
    offersApi.list()
      .then((list) => {
        if (cancelled) return;
        setOffers(list);
        if (list.length > 0) setOfferId(String(list[0].id));
        setStatus('ready');
      })
      .catch(() => {
        if (!cancelled) setStatus('error');
      });
    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    if (!offerId) {
      setApplications([]);
      return;
    }
    let cancelled = false;
    setLoadingApps(true);
    applicationsApi.forOffer(offerId)
      .then((data) => {
        if (!cancelled) setApplications(data);
      })
      .finally(() => {
        if (!cancelled) setLoadingApps(false);
      });
    return () => {
      cancelled = true;
    };
  }, [offerId]);

  async function viewCv(id) {
    const blob = await applicationsApi.cv(id);
    const url = URL.createObjectURL(blob);
    window.open(url, '_blank', 'noopener');
    setTimeout(() => URL.revokeObjectURL(url), 10_000);
  }

  if (status === 'loading') {
    return <Workspace title="Candidatures"><p className="hrapps__muted">Chargement...</p></Workspace>;
  }
  if (status === 'error') {
    return <Workspace title="Candidatures"><Alert>Les candidatures n'ont pas pu être chargées.</Alert></Workspace>;
  }

  if (offers.length === 0) {
    return (
      <Workspace title="Candidatures">
        <div className="hrapps__empty">
          <p>Vous n'avez pas encore d'offre. Créez une offre pour recevoir des candidatures.</p>
        </div>
      </Workspace>
    );
  }

  return (
    <Workspace title="Candidatures">
      <div className="hrapps__filter">
        <Select
          label="Offre"
          value={offerId}
          onChange={(e) => setOfferId(e.target.value)}
          options={offers.map((o) => ({ value: String(o.id), label: o.title }))}
        />
      </div>

      {loadingApps ? (
        <p className="hrapps__muted">Chargement des candidatures...</p>
      ) : applications.length === 0 ? (
        <div className="hrapps__empty">
          <p>Aucune candidature pour cette offre pour le moment.</p>
        </div>
      ) : (
        <ul className="hrapps__list">
          {applications.map((app) => (
            <li key={app.id} className="appline">
              <div className="appline__who">
                <span className="appline__name">{app.candidateFirstName} {app.candidateLastName}</span>
                <span className="appline__email">{app.candidateEmail}</span>
              </div>
              <span className="appline__date">Postulé le {dateFormat.format(new Date(app.applicationDate))}</span>
              <StatusBadge status={app.status} />
              <Button variant="text" onClick={() => viewCv(app.id)}>
                <Icon name="download" /> CV
              </Button>
            </li>
          ))}
        </ul>
      )}
    </Workspace>
  );
}
