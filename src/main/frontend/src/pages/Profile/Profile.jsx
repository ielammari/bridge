import { useState } from 'react';
import { profileApi } from '../../api/profile.js';
import { traitsApi } from '../../api/traits.js';
import Button from '../../components/Button/Button.jsx';
import CvUpload from '../../components/CvUpload/CvUpload.jsx';
import ErrorState from '../../components/ErrorState/ErrorState.jsx';
import Select from '../../components/Select/Select.jsx';
import Skeleton from '../../components/Skeleton/Skeleton.jsx';
import { useToast } from '../../components/Toast/ToastContext.jsx';
import TraitPicker from '../../components/TraitPicker/TraitPicker.jsx';
import { DEGREE_OPTIONS } from '../../constants/enums.js';
import useForm from '../../hooks/useForm.js';
import useResource from '../../hooks/useResource.js';
import Workspace from '../Workspace/Workspace.jsx';
import AcademicPath from './AcademicPath.jsx';
import './profile.css';

// Identity and contact details are configured in the settings; the profile
// holds what the matching gate reads, and what stands behind it.
const RULES = {
  degree: { label: 'Niveau d\'études' },
};

function snapshot(values, traitIds) {
  return JSON.stringify({
    degree: values.degree,
    traitIds: [...traitIds].sort((a, b) => a - b),
  });
}

export default function Profile() {
  const toast = useToast();

  const [hasCv, setHasCv] = useState(false);
  const [traitIds, setTraitIds] = useState([]);
  const [education, setEducation] = useState([]);
  const [baseline, setBaseline] = useState('');
  const [saving, setSaving] = useState(false);
  const [pathBusy, setPathBusy] = useState(false);

  const form = useForm({ degree: '' }, RULES);

  const { status, data, reload, pending, leaving } = useResource(async () => {
    const [profile, catalogue] = await Promise.all([profileApi.read(), traitsApi.catalogue()]);
    const values = { degree: profile.degree ?? '' };
    const ids = profile.traits.map((t) => t.traitId);
    form.setValues(values);
    setTraitIds(ids);
    setEducation(profile.education);
    setBaseline(snapshot(values, ids));
    setHasCv(profile.hasCv);
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

  async function uploadCv(file) {
    const updated = await profileApi.uploadCv(file);
    setHasCv(updated.hasCv);
    toast.success('CV mis à jour.');
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
    <Workspace title="Mon profil">
      <p className="profile__intro">
        Complétez votre profil pour voir les offres qui correspondent à vos compétences. Les traits
        obligatoires d'une offre sont comparés à ceux de votre profil.
      </p>

      {/* The three narrow cards share the left column so the trait catalogue,
          much the tallest thing here, starts at the top of its own. */}
      <div className="profile__columns">
        <div className="profile__col">
          <section className="card" aria-labelledby="level-title">
            <div className="card__head">
              <h2 id="level-title" className="card__title">Niveau d'études</h2>
              <p className="card__subtitle">Comparé au niveau demandé par chaque offre.</p>
            </div>
            <div className="card__body">
              <Select label="Niveau d'études atteint" labelHidden options={DEGREE_OPTIONS}
                placeholder="Sélectionnez votre niveau" {...form.field('degree')} />
            </div>
          </section>

          <AcademicPath
            entries={education}
            busy={pathBusy}
            onAdd={pathAction(profileApi.addEducation, 'Formation ajoutée.')}
            onUpdate={pathAction(profileApi.updateEducation, 'Formation modifiée.')}
            onRemove={pathAction(profileApi.removeEducation, 'Formation supprimée.')}
          />

          <section className="card" aria-labelledby="cv-title">
            <div className="card__head">
              <h2 id="cv-title" className="card__title">CV</h2>
              <p className="card__subtitle">Obligatoire au moment de postuler.</p>
            </div>
            <div className="card__body">
              <CvUpload hasCv={hasCv} onUpload={uploadCv} onDownload={profileApi.downloadCv} />
            </div>
          </section>
        </div>

        <div className="profile__col">
          <section className="card" aria-labelledby="traits-title">
            <div className="card__head">
              <h2 id="traits-title" className="card__title">Compétences et traits</h2>
              <p className="card__subtitle">
                Sélectionnez tout ce qui vous décrit : compétences techniques, niveau d'expérience,
                langues et atouts.
              </p>
            </div>
            <div className="card__body">
              <TraitPicker catalogue={data} value={traitIds} onChange={setTraitIds} />
            </div>
          </section>
        </div>
      </div>

      {(dirty || saving) && (
        <div className="profile__savebar" role="status">
          <p className="profile__savestate">Modifications non enregistrées</p>
          <Button onClick={save} loading={saving}>Enregistrer</Button>
        </div>
      )}
    </Workspace>
  );
}
