import { useLocation, useNavigate, useParams } from 'react-router-dom';
import { peopleApi } from '../../api/people.js';
import Button from '../../components/Button/Button.jsx';
import CardGrid from '../../components/CardGrid/CardGrid.jsx';
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

/** One fact about the person, dropped entirely when it is not on file. */
function Term({ label, children }) {
  if (!children) return null;
  return (
    <div className="terms__item">
      <dt className="terms__value">{children}</dt>
      <dd className="terms__label">{label}</dd>
    </div>
  );
}

/**
 * One candidate at their own address. What arrives is already filtered by who
 * asked, so the page renders what it is given.
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
    <Workspace
      title={name}
      subtitle={data.email}
      back={back}
      stats={[
        { value: data.applications.length, label: data.applications.length > 1 ? 'candidatures' : 'candidature' },
        { value: data.traits.length, label: 'traits' },
      ]}
      action={data.hasCv && (
        <Button variant="secondary" onClick={viewCv}>
          <Icon name="download" /> Ouvrir le CV
        </Button>
      )}
    >
      <div className="doc">
        <dl className="terms">
          <Term label="Niveau d'études">
            {data.degree ? DEGREE_LABELS[data.degree] : null}
          </Term>
          <Term label="Téléphone">{data.phone}</Term>
          <Term label="Ville">{data.city}</Term>
          <Term label="Pays">{data.country}</Term>
          <Term label="Naissance">
            {data.birthDate ? longDate(data.birthDate) : null}
          </Term>
          <Term label="Sexe">{genderLabel(data.gender)}</Term>
        </dl>

        <section className="doc__section">
          <h2 className="doc__heading">Parcours</h2>
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
        </section>

        <section className="doc__section">
          <h2 className="doc__heading">Compétences et traits</h2>
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
        </section>

        <section className="doc__section">
          <h2 className="doc__heading">Candidatures</h2>
          {data.applications.length === 0 ? (
            <p className="person__none">
              {isSelf
                ? 'Vos candidatures apparaîtront ici.'
                : 'Cette personne n\'a encore postulé à aucune offre.'}
            </p>
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
        </section>

        <p className="doc__meta">
          {data.registrationDate && `Inscrit le ${longDate(data.registrationDate)}`}
        </p>
      </div>
    </Workspace>
  );
}
