import { useLocation, useNavigate } from 'react-router-dom';
import { historyApi } from '../../api/history.js';
import Button from '../../components/Button/Button.jsx';
import EmptyState from '../../components/EmptyState/EmptyState.jsx';
import ErrorState from '../../components/ErrorState/ErrorState.jsx';
import FilterBar from '../../components/FilterBar/FilterBar.jsx';
import GroupedGrid from '../../components/GroupedGrid/GroupedGrid.jsx';
import PersonLink from '../../components/PersonLink/PersonLink.jsx';
import Skeleton from '../../components/Skeleton/Skeleton.jsx';
import TabNav from '../../components/TabNav/TabNav.jsx';
import { CONTRACT_LABELS } from '../../constants/enums.js';
import { euros, longDate } from '../../constants/format.js';
import { useAuth } from '../../context/AuthContext.jsx';
import useFiltering from '../../hooks/useFiltering.js';
import useResource from '../../hooks/useResource.js';
import Workspace from '../Workspace/Workspace.jsx';
import { historyTabs } from './tabs.js';
import './history.css';

/** With no filter chosen, the register reads offer by offer. */
const BY_OFFER = {
  of: (row) => row.offerTitle,
  labelOf: (key) => key,
};

/** Everyone hired, and the terms each was hired on. */
export default function Hirings() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  // Where this listing currently stands, filters included, so a record
  // opened from it can return to the same view rather than to the top.
  const origin = { from: `${location.pathname}${location.search}` };
  const { status, data, reload, pending, leaving } = useResource(() => historyApi.hirings());

  const hires = data ?? [];
  const unique = (read) => [...new Set(hires.map(read))].sort((a, b) => a.localeCompare(b, 'fr'));

  const dimensions = [
    {
      key: 'offer', label: 'Offre', type: 'select', valueLabel: 'Offre',
      of: BY_OFFER.of,
      labelOf: BY_OFFER.labelOf,
      options: unique((row) => row.offerTitle).map((title) => ({ value: title, label: title })),
    },
    {
      key: 'candidate', label: 'Personne recrutée', type: 'search', valueLabel: 'Nom',
      placeholder: 'Commencez à taper un nom',
      of: (row) => row.candidateName,
      labelOf: (key) => key,
      options: unique((row) => row.candidateName),
    },
  ];

  const filtering = useFiltering(hires, dimensions, BY_OFFER);

  const card = (row) => (
    <li key={row.hiring.id} className="tile">
      <div className="tile__head">
        <div>
          <h3 className="tile__title">
            <PersonLink id={row.candidateId}>{row.candidateName}</PersonLink>
          </h3>
          <p className="hire__email">{row.candidateEmail}</p>
        </div>
      </div>

      <p className="tile__facts">
        <span>{row.offerTitle}</span>
        <span>{CONTRACT_LABELS[row.hiring.finalContract]}</span>
      </p>

      <dl className="hire__terms">
        <dt>Salaire négocié</dt>
        <dd className="mono">{euros(row.hiring.negotiatedSalary)}</dd>
        <dt>Prise de poste</dt>
        <dd>{longDate(row.hiring.startDate)}</dd>
        {row.hiring.trialPeriod && (
          <>
            <dt>Période d'essai</dt>
            <dd>{row.hiring.trialPeriod}</dd>
          </>
        )}
        {row.hiring.executiveStatus && (
          <>
            <dt>Statut</dt>
            <dd>Cadre</dd>
          </>
        )}
        {row.hiring.benefits && (
          <>
            <dt>Avantages</dt>
            <dd>{row.hiring.benefits}</dd>
          </>
        )}
      </dl>

      <div className="tile__foot">
        <Button variant="secondary"
          onClick={() => navigate(`/historique/candidatures/${row.applicationId}`, { state: origin })}>
          Voir le dossier
        </Button>
      </div>
    </li>
  );

  return (
    <Workspace title="Historique">
      <TabNav items={historyTabs(user.role)} label="Sections de l'historique" />

      {pending && <Skeleton leaving={leaving} label="Chargement des embauches" />}

      {status === 'error' && (
        <ErrorState onRetry={reload}>
          Les embauches n'ont pas pu être chargées. Réessayez dans un instant.
        </ErrorState>
      )}

      {status === 'ready' && hires.length === 0 && (
        <EmptyState title="Aucune embauche enregistrée.">
          Valider un entretien final crée un dossier d'embauche, qui apparaîtra ici avec ses
          conditions.
        </EmptyState>
      )}

      {status === 'ready' && hires.length > 0 && (
        <>
          <FilterBar
            dimensions={dimensions}
            dimension={filtering.dimension}
            value={filtering.value}
            onDimension={filtering.setDimension}
            onValue={filtering.setValue}
            count={`${filtering.kept.length} sur ${hires.length}`}
          />

          {filtering.kept.length === 0 ? (
            <EmptyState title="Aucune embauche ne correspond à ce filtre.">
              Modifiez la valeur, ou revenez à la liste complète en choisissant « Aucun filtre ».
            </EmptyState>
          ) : (
            <GroupedGrid sections={filtering.sections} render={card} />
          )}
        </>
      )}
    </Workspace>
  );
}
