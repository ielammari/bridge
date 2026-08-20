import { useState } from 'react';
import { profileApi } from '../../api/profile.js';
import { settingsApi } from '../../api/settings.js';
import { traitsApi } from '../../api/traits.js';
import Button from '../../components/Button/Button.jsx';
import CvLibrary from '../../components/CvLibrary/CvLibrary.jsx';
import ErrorState from '../../components/ErrorState/ErrorState.jsx';
import InfoHint from '../../components/InfoHint/InfoHint.jsx';
import SectionRail from '../../components/SectionRail/SectionRail.jsx';
import Select from '../../components/Select/Select.jsx';
import Skeleton from '../../components/Skeleton/Skeleton.jsx';
import { useToast } from '../../components/Toast/ToastContext.jsx';
import TraitPicker from '../../components/TraitPicker/TraitPicker.jsx';
import { DEGREE_OPTIONS } from '../../constants/enums.js';
import useForm from '../../hooks/useForm.js';
import useResource from '../../hooks/useResource.js';
import Workspace from '../Workspace/Workspace.jsx';
import AcademicPath from './AcademicPath.jsx';
import IdentitySection from './IdentitySection.jsx';
import './profile.css';

// Only the scalar level is behind the save bar; every other section on this
// page commits on its own.
const RULES = {
  degree: { label: 'Niveau d\'études' },
};

const SECTIONS = [
  { id: 'identite', label: 'Informations' },
  { id: 'niveau', label: "Niveau d'études" },
  { id: 'parcours', label: 'Parcours' },
  { id: 'cv', label: 'Mes CV' },
  { id: 'traits', label: 'Compétences' },
];

function snapshot(values, traitIds) {
  return JSON.stringify({
    degree: values.degree,
    traitIds: [...traitIds].sort((a, b) => a - b),
  });
}

export default function Profile() {
  const toast = useToast();

  const [documents, setDocuments] = useState([]);
  const [traitIds, setTraitIds] = useState([]);
  const [education, setEducation] = useState([]);
  const [baseline, setBaseline] = useState('');
  const [saving, setSaving] = useState(false);
  const [pathBusy, setPathBusy] = useState(false);

  const form = useForm({ degree: '' }, RULES);

  const [account, setAccount] = useState(null);

  const { status, data, reload, pending, leaving } = useResource(async () => {
    const [profile, catalogue, identity] = await Promise.all([
      profileApi.read(), traitsApi.catalogue(), settingsApi.account(),
    ]);
    setAccount(identity);
    const values = { degree: profile.degree ?? '' };
    const ids = profile.traits.map((t) => t.traitId);
    form.setValues(values);
    setTraitIds(ids);
    setEducation(profile.education);
    setBaseline(snapshot(values, ids));
    setDocuments(profile.cvs);
    return catalogue;
  });

  const dirty = status === 'ready' && snapshot(form.values, traitIds) !== baseline;

  async function save() {
    if (!form.attempt()) return;
    setSaving(true);
    try {
      const updated = await profileApi.update({
        degree: form.values.degree || null,
        traits: traitIds.map((traitId) => ({ traitId, level: null })),
      });
      const values = { degree: updated.degree ?? '' };
      const ids = updated.traits.map((t) => t.traitId);
      form.setValues(values);
      setTraitIds(ids);
      setBaseline(snapshot(values, ids));
      toast.success('Profil enregistré.');
    } catch (apiError) {
      toast.error(apiError.message);
    } finally {
      setSaving(false);
    }
  }

  /**
   * Each path change is its own record and commits on its own, unlike the level
   * and the traits, which are one form behind one button.
   */
  function pathAction(run, done) {
    return async (...args) => {
      setPathBusy(true);
      try {
        setEducation((await run(...args)).education);
        toast.success(done);
      } catch (apiError) {
        toast.error(apiError.message);
      } finally {
        setPathBusy(false);
      }
    };
  }

  /** Every document action answers with the profile, so one call refreshes. */
  function cvAction(run, done) {
    return async (...args) => {
      setDocuments((await run(...args)).cvs);
      toast.success(done);
    };
  }

  async function openCv() {
    const blob = await profileApi.downloadCv();
    const url = URL.createObjectURL(blob);
    window.open(url, '_blank', 'noopener');
    setTimeout(() => URL.revokeObjectURL(url), 10_000);
  }

  if (status === 'loading') {
    return (
      <Workspace title="Mon profil">
        {pending && (
          <Skeleton variant="form" count={5} leaving={leaving}
            label="Chargement de votre profil" />
        )}
      </Workspace>
    );
  }

  if (status === 'error') {
    return (
      <Workspace title="Mon profil">
        <ErrorState onRetry={reload}>
          Votre profil n'a pas pu être chargé. Réessayez dans un instant.
        </ErrorState>
      </Workspace>
    );
  }

  return (
    <Workspace
      title="Mon profil"
      info="Les offres qui vous sont proposées sont celles dont vous possédez tous les traits obligatoires, avec au moins le niveau d'études demandé."
      stats={[
        { value: traitIds.length, label: 'traits' },
        { value: education.length, label: education.length > 1 ? 'formations' : 'formation' },
        { value: documents.length, label: 'CV' },
      ]}
    >
      <SectionRail sections={SECTIONS}>
        <div id="identite">{account && <IdentitySection account={account} />}</div>

        <section id="niveau" className="card" aria-labelledby="level-title">
          <div className="card__head">
            <h2 id="level-title" className="card__title">
              Niveau d'études
              <InfoHint label="À propos du niveau d'études">
                Une offre demande un niveau minimum : un niveau supérieur y donne accès.
              </InfoHint>
            </h2>
          </div>
          <div className="card__body">
            <Select label="Niveau d'études atteint" labelHidden options={DEGREE_OPTIONS}
              placeholder="Sélectionnez votre niveau" {...form.field('degree')} />
          </div>
        </section>

        <div id="parcours">
          <AcademicPath
            entries={education}
            busy={pathBusy}
            onAdd={pathAction(profileApi.addEducation, 'Formation ajoutée.')}
            onUpdate={pathAction(profileApi.updateEducation, 'Formation modifiée.')}
            onRemove={pathAction(profileApi.removeEducation, 'Formation supprimée.')}
          />
        </div>

        <section id="cv" className="card" aria-labelledby="cv-title">
          <div className="card__head">
            <h2 id="cv-title" className="card__title">
              Mes CV
              <InfoHint label="À propos des CV">
                Un CV est obligatoire au moment de postuler. Vous pouvez en garder plusieurs et
                choisir lequel joindre à chaque candidature.
              </InfoHint>
            </h2>
          </div>
          <div className="card__body">
            <CvLibrary
              documents={documents}
              onUpload={cvAction(profileApi.uploadCv, 'CV ajouté.')}
              onChoose={cvAction(profileApi.chooseCv, 'CV par défaut modifié.')}
              onRemove={cvAction(profileApi.removeCv, 'CV supprimé.')}
              onOpen={openCv}
            />
          </div>
        </section>

        <section id="traits" className="card" aria-labelledby="traits-title">
          <div className="card__head">
            <h2 id="traits-title" className="card__title">
              Compétences et traits
              <InfoHint label="À propos des compétences">
                Tout ce qui vous décrit : compétences techniques, niveau d'expérience, langues
                et atouts. Ils sont comparés aux traits demandés par chaque offre.
              </InfoHint>
            </h2>
          </div>
          <div className="card__body">
            <TraitPicker catalogue={data} value={traitIds} onChange={setTraitIds} />
          </div>
        </section>
      </SectionRail>

      {(dirty || saving) && (
        <div className="profile__savebar" role="status">
          <p className="profile__savestate">Modifications non enregistrées</p>
          <Button onClick={save} loading={saving}>Enregistrer</Button>
        </div>
      )}
    </Workspace>
  );
}
