import { useLocation, useNavigate, useParams } from 'react-router-dom';
import { peopleApi } from '../../api/people.js';
import Button from '../../components/Button/Button.jsx';
import CardGrid from '../../components/CardGrid/CardGrid.jsx';
import EmptyState from '../../components/EmptyState/EmptyState.jsx';
import ErrorState from '../../components/ErrorState/ErrorState.jsx';
import Icon from '../../components/Icon/Icon.jsx';
import OfferLink from '../../components/OfferLink/OfferLink.jsx';
import Skeleton from '../../components/Skeleton/Skeleton.jsx';
import StatusBadge from '../../components/StatusBadge/StatusBadge.jsx';
import { useToast } from '../../components/Toast/ToastContext.jsx';
import { DEGREE_LABELS, GENDER_OPTIONS } from '../../constants/enums.js';
import { longDate } from '../../constants/format.js';
import { useAuth } from '../../context/AuthContext.jsx';
import useResource from '../../hooks/useResource.js';
import Workspace from '../Workspace/Workspace.jsx';
import './person.css';

const genderLabel = (value) =>
  GENDER_OPTIONS.find((option) => option.value === value)?.label ?? null;

const period = (entry) =>
  (entry.endYear ? `${entry.startYear} à ${entry.endYear}` : `depuis ${entry.startYear}`);

/** A labelled fact, dropped entirely when there is nothing to show. */
function Fact({ label, children }) {
  if (children === null || children === undefined || children === '') return null;
  return (
    <div className="person__fact">
      <dt className="person__fact-label">{label}</dt>
      <dd className="person__fact-value">{children}</dd>
    </div>
  );
}

/**
 * One candidate at their own address, reached from any listing that names them.
 *
 * What arrives has already been filtered by who asked, so the page renders what
 * it is given and makes no judgement of its own about who may see what.
 */
export default function PersonPage() {
  const { id } = useParams();
  const location = useLocation();
  const navigate = useNavigate();
  const toast = useToast();
  const { user } = useAuth();

  const { status, data, reload, pending, leaving } = useResource(
    () => peopleApi.dossier(id),
    [id],
  );

  const back = location.state?.from
    ? { to: location.state.from, label: 'Retour' }
    : null;

  async function viewCv() {
    try {
      const blob = await peopleApi.cv(id);
      const url = URL.createObjectURL(blob);
      window.open(url, '_blank', 'noopener');
      setTimeout(() => URL.revokeObjectURL(url), 10_000);
    } catch (apiError) {
      toast.error(apiError.message);
    }
  }

  if (status !== 'ready') {
    return (
      <Workspace title="Profil" back={back}>
        {pending && <Skeleton variant="record" leaving={leaving} label="Chargement du profil" />}
        {status === 'error' && (
          <ErrorState onRetry={reload}>
            Cette personne n'a pas pu être chargée, ou elle ne vous est pas accessible.
          </ErrorState>
        )}
      </Workspace>
    );
  }

  const name = `${data.firstName} ${data.lastName}`;
  const isSelf = user.id === data.id;

  return (
    <Workspace title={name} subtitle={data.email} back={back}>
      <div className="person">
        <div className="person__col">
          <section className="card" aria-labelledby="identity-title">
            <div className="card__head">
              <h2 id="identity-title" className="card__title">Coordonnées</h2>
            </div>
            <div className="card__body">
              <dl className="person__facts">
                <Fact label="Téléphone">{data.phone}</Fact>
                <Fact label="Date de naissance">
                  {data.birthDate ? longDate(data.birthDate) : null}
                </Fact>
                <Fact label="Sexe">{genderLabel(data.gender)}</Fact>
                <Fact label="Ville">{data.city}</Fact>
                <Fact label="Pays">{data.country}</Fact>
                <Fact label="Inscrit le">
                  {data.registrationDate ? longDate(data.registrationDate) : null}
                </Fact>
              </dl>
            </div>
          </section>

          <section className="card" aria-labelledby="level-title">
            <div className="card__head">
              <h2 id="level-title" className="card__title">Niveau d'études</h2>
            </div>
            <div className="card__body">
              <p className="person__level">
                {data.degree ? DEGREE_LABELS[data.degree] : 'Non renseigné'}
              </p>
            </div>
          </section>

          <section className="card" aria-labelledby="cv-title">
            <div className="card__head">
              <h2 id="cv-title" className="card__title">CV</h2>
            </div>
            <div className="card__body">
              {data.hasCv ? (
                <Button variant="secondary" onClick={viewCv}>
                  <Icon name="download" /> Ouvrir le CV
                </Button>
              ) : (
                <p className="person__none">Aucun CV déposé.</p>
              )}
            </div>
          </section>
        </div>

        <div className="person__col">
          <section className="card" aria-labelledby="path-title">
            <div className="card__head">
              <h2 id="path-title" className="card__title">Parcours</h2>
            </div>
            <div className="card__body">
              {data.education.length === 0 ? (
                <p className="person__none">Aucune formation renseignée.</p>
              ) : (
                <ol className="path">
                  {data.education.map((entry) => (
                    <li key={entry.id} className="path__item">
                      <div className="path__entry">
                        <p className="path__title">
                          {entry.title}
                          {!entry.endYear && <span className="path__ongoing">En cours</span>}
                        </p>
                        <p className="path__where">
                          {entry.institution}
                          {entry.fieldOfStudy && ` · ${entry.fieldOfStudy}`}
                        </p>
                        <p className="path__period">{period(entry)}</p>
                      </div>
                    </li>
                  ))}
                </ol>
              )}
            </div>
          </section>

          <section className="card" aria-labelledby="person-traits-title">
            <div className="card__head">
              <h2 id="person-traits-title" className="card__title">Compétences et traits</h2>
              <p className="card__subtitle">{data.traits.length} au profil.</p>
            </div>
            <div className="card__body">
              {data.traits.length === 0 ? (
                <p className="person__none">Aucun trait renseigné.</p>
              ) : (
                <ul className="person__traits">
                  {data.traits.map((trait) => (
                    <li key={trait.traitId} className="person__trait">
                      {trait.label}
                      {trait.level && <span className="person__trait-level">{trait.level}</span>}
                    </li>
                  ))}
                </ul>
              )}
            </div>
          </section>
        </div>
      </div>

      <section className="card" aria-labelledby="person-apps-title">
        <div className="card__head">
          <h2 id="person-apps-title" className="card__title">Candidatures</h2>
        </div>
        <div className="card__body">
          {data.applications.length === 0 ? (
            <EmptyState title="Aucune candidature.">
              {isSelf
                ? 'Vos candidatures apparaîtront ici.'
                : 'Cette personne n\'a encore postulé à aucune offre.'}
            </EmptyState>
          ) : (
            <CardGrid label={`Candidatures de ${name}`}>
              {data.applications.map((application) => (
                <li key={application.id} className="tile">
                  <div className="tile__head">
                    <h3 className="tile__title">
                      <OfferLink id={application.offerId}>{application.offerTitle}</OfferLink>
                    </h3>
                    <StatusBadge status={application.status} />
                  </div>
                  <p className="tile__facts">
                    <span>Déposée le {longDate(application.applicationDate)}</span>
                  </p>
                  <div className="tile__foot">
                    <Button
                      variant="text"
                      onClick={() => navigate(`/historique/candidatures/${application.id}`, {
                        state: { from: `${location.pathname}${location.search}` },
                      })}
                    >
                      Voir le dossier
                    </Button>
                  </div>
                </li>
              ))}
            </CardGrid>
          )}
        </div>
      </section>
    </Workspace>
  );
}
