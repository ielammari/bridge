import { useLocation, useNavigate } from 'react-router-dom';
import { applicationsApi } from '../../api/applications.js';
import { historyApi } from '../../api/history.js';
import Button from '../../components/Button/Button.jsx';
import EmptyState from '../../components/EmptyState/EmptyState.jsx';
import ErrorState from '../../components/ErrorState/ErrorState.jsx';
import FilterBar from '../../components/FilterBar/FilterBar.jsx';
import GroupedGrid from '../../components/GroupedGrid/GroupedGrid.jsx';
import PersonLink from '../../components/PersonLink/PersonLink.jsx';
import OfferLink from '../../components/OfferLink/OfferLink.jsx';
import Skeleton from '../../components/Skeleton/Skeleton.jsx';
import StatusBadge from '../../components/StatusBadge/StatusBadge.jsx';
import TabNav from '../../components/TabNav/TabNav.jsx';
import { CONTRACT_LABELS, isTerminal, STATUS_LABELS } from '../../constants/enums.js';
import { longDate, monthLabel, monthOf } from '../../constants/format.js';
import { useAuth } from '../../context/AuthContext.jsx';
import useFiltering from '../../hooks/useFiltering.js';
import useResource from '../../hooks/useResource.js';
import Workspace from '../Workspace/Workspace.jsx';
import { historyTabs } from './tabs.js';
import './history.css';

const candidateName = (app) => `${app.candidateFirstName} ${app.candidateLastName}`;

/** With no filter chosen, the record reads outcome by outcome. */
const BY_OUTCOME = {
  of: (app) => app.status,
  labelOf: (key) => STATUS_LABELS[key] ?? key,
};

/**
 * Applications nobody can move any further. A candidate sees their own; HR sees
 * everything that closed, across every offer.
 */
export default function PastApplications() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  // Where this listing currently stands, filters included, so a record
  // opened from it can return to the same view rather than to the top.
  const origin = { from: `${location.pathname}${location.search}` };
  const isCandidate = user.role === 'CANDIDAT';

  const { status, data, reload, pending, leaving } = useResource(
    () => (isCandidate
      ? applicationsApi.mine().then((all) => all.filter((app) => isTerminal(app.status)))
      : historyApi.closedApplications()),
    [isCandidate],
  );

  const closed = data ?? [];
  const unique = (read) => [...new Set(closed.map(read))].sort((a, b) => a.localeCompare(b, 'fr'));

  const dimensions = [
    // Only a recruiter reads this across offers. A candidate filtering their own
    // record by offer would get one application per section.
    ...(isCandidate ? [] : [{
      key: 'offer', label: 'Offre', type: 'select', valueLabel: 'Offre',
      of: (app) => app.offerTitle,
      labelOf: (key) => key,
      options: unique((app) => app.offerTitle).map((title) => ({ value: title, label: title })),
    }]),
    {
      key: 'outcome', label: 'Catégorie', type: 'select', valueLabel: 'Issue',
      of: BY_OUTCOME.of,
      labelOf: BY_OUTCOME.labelOf,
      options: unique((app) => app.status).map((s) => ({ value: s, label: STATUS_LABELS[s] ?? s })),
    },
    {
      key: 'period', label: 'Date', type: 'date', valueLabel: 'Déposée le',
      // Filters on the exact day, gathers by month: one section per day would
      // be as unreadable as no sections at all.
      of: (app) => app.applicationDate.slice(0, 10),
      groupOf: (app) => monthOf(app.applicationDate),
      labelOf: monthLabel,
      compare: (a, b) => b.key.localeCompare(a.key),
    },
  ];

  if (!isCandidate) {
    dimensions.push({
      key: 'candidate', label: 'Candidat', type: 'search', valueLabel: 'Nom du candidat',
      placeholder: 'Commencez à taper un nom',
      of: candidateName,
      labelOf: (key) => key,
      options: unique(candidateName),
    });
  }

  const filtering = useFiltering(closed, dimensions, BY_OUTCOME);

  const card = (app) => (
    <li key={app.id} className="tile">
      <div className="tile__head">
        <h3 className="tile__title">
          {isCandidate ? app.offerTitle
            : <PersonLink id={app.candidateId}>{candidateName(app)}</PersonLink>}
        </h3>
        <StatusBadge status={app.status} />
      </div>

      <p className="tile__facts">
        {isCandidate ? <span>{CONTRACT_LABELS[app.contractType]}</span> : <span><OfferLink id={app.offerId}>{app.offerTitle}</OfferLink></span>}
        <span>Déposée le {longDate(app.applicationDate)}</span>
      </p>

      <div className="tile__foot">
        <Button variant="secondary" onClick={() => navigate(`/historique/candidatures/${app.id}`, { state: origin })}>
          Voir le dossier
        </Button>
      </div>
    </li>
  );

  return (
    <Workspace
      title="Historique"
      toolbar={(
        <>
          <TabNav items={historyTabs(user.role)} label="Sections de l'historique" />
          {status === 'ready' && closed.length > 0 && (
            <FilterBar
              dimensions={dimensions}
              dimension={filtering.dimension}
              value={filtering.value}
              onDimension={filtering.setDimension}
              onValue={filtering.setValue}
              count={`${filtering.kept.length} sur ${closed.length}`}
            />
          )}
        </>
      )}
    >
      {pending && <Skeleton leaving={leaving} label="Chargement de l'historique" />}

      {status === 'error' && (
        <ErrorState onRetry={reload}>
          L'historique n'a pas pu être chargé. Réessayez dans un instant.
        </ErrorState>
      )}

      {status === 'ready' && closed.length === 0 && (
        <EmptyState title="Aucune candidature close pour le moment.">
          {isCandidate
            ? 'Vos candidatures terminées, retenues ou non, resteront consultables ici.'
            : 'Les candidatures rejetées ou abouties quittent la liste active et se retrouvent ici.'}
        </EmptyState>
      )}

      {status === 'ready' && closed.length > 0 && (
        filtering.kept.length === 0 ? (
          <EmptyState title="Aucune candidature ne correspond à ce filtre.">
            Modifiez la valeur, ou revenez à la liste complète en choisissant « Aucun filtre ».
          </EmptyState>
        ) : (
          <GroupedGrid sections={filtering.sections} render={card} />
        )
      )}
    </Workspace>
  );
}
