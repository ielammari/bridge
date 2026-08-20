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
import { DECISION_LABELS, EVALUATION_TYPE_LABELS } from '../../constants/enums.js';
import { dateTime, monthLabel, monthOf } from '../../constants/format.js';
import { useAuth } from '../../context/AuthContext.jsx';
import useFiltering from '../../hooks/useFiltering.js';
import useResource from '../../hooks/useResource.js';
import Workspace from '../Workspace/Workspace.jsx';
import { historyTabs } from './tabs.js';
import './history.css';

/** A recruiter writes at two stages, so their record reads stage by stage. */
const BY_TYPE = {
  of: (row) => row.evaluation.type,
  labelOf: (key) => EVALUATION_TYPE_LABELS[key] ?? key,
};

/** An expert writes one kind, so theirs reads offer by offer. */
const BY_OFFER = {
  of: (row) => row.offerTitle,
  labelOf: (key) => key,
};

/** The evaluations the signed in evaluator has written, with their scores. */
export default function PastEvaluations() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  // Where this listing currently stands, filters included, so a record
  // opened from it can return to the same view rather than to the top.
  const origin = { from: `${location.pathname}${location.search}` };
  const { status, data, reload, pending, leaving } = useResource(() => historyApi.authoredEvaluations());

  const rows = data ?? [];
  const unique = (read) => [...new Set(rows.map(read))].sort((a, b) => a.localeCompare(b, 'fr'));

  // The stage only cuts a list that holds more than one: an expert runs the
  // technical exam and nothing else.
  const writesOneKind = user.role === 'EXPERT';

  const dimensions = [
    {
      key: 'offer', label: 'Offre', type: 'select', valueLabel: 'Offre',
      of: (row) => row.offerTitle,
      labelOf: (key) => key,
      options: unique((row) => row.offerTitle).map((title) => ({ value: title, label: title })),
    },
    ...(writesOneKind ? [] : [{
      key: 'type', label: 'Catégorie', type: 'select', valueLabel: 'Étape',
      of: BY_TYPE.of,
      labelOf: BY_TYPE.labelOf,
      options: unique((row) => row.evaluation.type)
        .map((t) => ({ value: t, label: EVALUATION_TYPE_LABELS[t] ?? t })),
    }]),
    {
      key: 'decision', label: 'Décision', type: 'select', valueLabel: 'Décision',
      of: (row) => row.evaluation.decision,
      labelOf: (key) => DECISION_LABELS[key] ?? key,
      options: unique((row) => row.evaluation.decision)
        .map((d) => ({ value: d, label: DECISION_LABELS[d] ?? d })),
    },
    {
      key: 'period', label: 'Date', type: 'date', valueLabel: 'Rendue le',
      of: (row) => row.evaluation.date.slice(0, 10),
      groupOf: (row) => monthOf(row.evaluation.date),
      labelOf: monthLabel,
      compare: (a, b) => b.key.localeCompare(a.key),
    },
    {
      key: 'candidate', label: 'Candidat', type: 'search', valueLabel: 'Nom du candidat',
      placeholder: 'Commencez à taper un nom',
      of: (row) => row.candidateName,
      labelOf: (key) => key,
      options: unique((row) => row.candidateName),
    },
  ];

  const filtering = useFiltering(rows, dimensions, writesOneKind ? BY_OFFER : BY_TYPE);

  const card = (row) => (
    <li key={row.evaluation.id} className="tile">
      <div className="tile__head">
        <div>
          <h3 className="tile__title">
            <PersonLink id={row.candidateId}>{row.candidateName}</PersonLink>
          </h3>
          <p className="record__meta">{row.offerTitle}</p>
        </div>
        <span className={`verdict verdict--${row.evaluation.decision.toLowerCase()}`}>
          {DECISION_LABELS[row.evaluation.decision]}
        </span>
      </div>

      {/* What was decided, about whom and when. The comment and the scores are
          the record itself, and belong to the dossier the button opens. */}
      <p className="tile__facts">
        {!writesOneKind && <span>{EVALUATION_TYPE_LABELS[row.evaluation.type]}</span>}
        <span>{dateTime(row.evaluation.date)}</span>
      </p>

      <div className="tile__foot">
        <Button variant="secondary"
          onClick={() => navigate(`/historique/candidatures/${row.applicationId}`, { state: origin })}>
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
          {status === 'ready' && rows.length > 0 && (
            <FilterBar
              dimensions={dimensions}
              dimension={filtering.dimension}
              value={filtering.value}
              onDimension={filtering.setDimension}
              onValue={filtering.setValue}
              count={`${filtering.kept.length} sur ${rows.length}`}
            />
          )}
        </>
      )}
    >
      {pending && <Skeleton leaving={leaving} label="Chargement de vos évaluations" />}

      {status === 'error' && (
        <ErrorState onRetry={reload}>
          Vos évaluations n'ont pas pu être chargées. Réessayez dans un instant.
        </ErrorState>
      )}

      {status === 'ready' && rows.length === 0 && (
        <EmptyState title="Vous n'avez encore rendu aucune évaluation.">
          Chaque évaluation que vous rendez reste consultable ici, avec les notes que vous avez
          attribuées.
        </EmptyState>
      )}

      {status === 'ready' && rows.length > 0 && (
        filtering.kept.length === 0 ? (
          <EmptyState title="Aucune évaluation ne correspond à ce filtre.">
            Modifiez la valeur, ou revenez à la liste complète en choisissant « Aucun filtre ».
          </EmptyState>
        ) : (
          <GroupedGrid sections={filtering.sections} size="wide" render={card} />
        )
      )}
    </Workspace>
  );
}
