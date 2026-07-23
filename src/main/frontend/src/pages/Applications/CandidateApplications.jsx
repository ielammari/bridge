import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { applicationsApi } from '../../api/applications.js';
import Alert from '../../components/Alert/Alert.jsx';
import FunnelRail from '../../components/FunnelRail/FunnelRail.jsx';
import StatusBadge from '../../components/StatusBadge/StatusBadge.jsx';
import { APPLICATION_ALERTS, CONTRACT_LABELS } from '../../constants/enums.js';
import Workspace from '../Workspace/Workspace.jsx';
import './candidateApplications.css';

const dateFormat = new Intl.DateTimeFormat('fr-FR', { day: 'numeric', month: 'long', year: 'numeric' });

export default function CandidateApplications() {
  const [status, setStatus] = useState('loading');
  const [applications, setApplications] = useState([]);

  useEffect(() => {
    let cancelled = false;
    applicationsApi.mine()
      .then((data) => {
        if (cancelled) return;
        setApplications(data);
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
    return <Workspace title="Mes candidatures"><p className="apps__muted">Chargement...</p></Workspace>;
  }
  if (status === 'error') {
    return <Workspace title="Mes candidatures"><Alert>Vos candidatures n'ont pas pu être chargées.</Alert></Workspace>;
  }

  return (
    <Workspace title="Mes candidatures">
      {applications.length === 0 ? (
        <div className="apps__empty">
          <p className="apps__empty-title">Vous n'avez pas encore postulé.</p>
          <p>Parcourez les offres qui correspondent à votre profil pour envoyer une candidature.</p>
          <Link className="apps__empty-link" to="/offres">Voir les offres</Link>
        </div>
      ) : (
        <ul className="apps__list">
          {applications.map((app) => (
            <li key={app.id} className="apptrack">
              <div className="apptrack__head">
                <div>
                  <h3 className="apptrack__title">{app.offerTitle}</h3>
                  <p className="apptrack__meta">
                    <span>{CONTRACT_LABELS[app.contractType]}</span>
                    {app.location && <span>{app.location}</span>}
                    <span>Envoyée le {dateFormat.format(new Date(app.applicationDate))}</span>
                  </p>
                </div>
                <StatusBadge status={app.status} />
              </div>

              <FunnelRail status={app.status} />

              <p className={`apptrack__alert${app.status === 'REFUSEE' ? ' apptrack__alert--refused' : ''}`}>
                {APPLICATION_ALERTS[app.status]}
              </p>
            </li>
          ))}
        </ul>
      )}
    </Workspace>
  );
}
