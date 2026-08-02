import { useState } from 'react';
import { profileApi } from '../../api/profile.js';
import { traitsApi } from '../../api/traits.js';
import Button from '../../components/Button/Button.jsx';
import CvUpload from '../../components/CvUpload/CvUpload.jsx';
import ErrorState from '../../components/ErrorState/ErrorState.jsx';
import Field from '../../components/Field/Field.jsx';
import Select from '../../components/Select/Select.jsx';
import Skeleton from '../../components/Skeleton/Skeleton.jsx';
import { useToast } from '../../components/Toast/ToastContext.jsx';
import TraitPicker from '../../components/TraitPicker/TraitPicker.jsx';
import { DEGREE_OPTIONS } from '../../constants/enums.js';
import { phoneFormat } from '../../constants/validation.js';
import useForm from '../../hooks/useForm.js';
import useResource from '../../hooks/useResource.js';
import Workspace from '../Workspace/Workspace.jsx';
import './profile.css';

const RULES = {
  phone: { label: 'Téléphone', format: phoneFormat },
  degree: { label: 'Diplôme' },
  experienceLevel: { label: 'Niveau d\'expérience' },
};

function snapshot(values, traitIds) {
  return JSON.stringify({
    phone: values.phone,
    degree: values.degree,
    experienceLevel: values.experienceLevel,
    traitIds: [...traitIds].sort((a, b) => a - b),
  });
}

export default function Profile() {
  const toast = useToast();

  const [identity, setIdentity] = useState(null);
  const [hasCv, setHasCv] = useState(false);
  const [traitIds, setTraitIds] = useState([]);
  const [baseline, setBaseline] = useState('');
  const [saving, setSaving] = useState(false);

  const form = useForm({ phone: '', degree: '', experienceLevel: '' }, RULES);

  const { status, data, reload } = useResource(async () => {
    const [profile, catalogue] = await Promise.all([profileApi.read(), traitsApi.catalogue()]);
    const values = {
      phone: profile.phone ?? '',
      degree: profile.degree ?? '',
      experienceLevel: profile.experienceLevel ?? '',
    };
    const ids = profile.traits.map((t) => t.traitId);
    form.setValues(values);
    setTraitIds(ids);
    setBaseline(snapshot(values, ids));
    setHasCv(profile.hasCv);
    setIdentity({
      firstName: profile.firstName,
      lastName: profile.lastName,
      email: profile.email,
    });
    return catalogue;
  });

  const dirty = status === 'ready' && snapshot(form.values, traitIds) !== baseline;

  async function save() {
    if (!form.attempt()) return;
    setSaving(true);
    try {
      const updated = await profileApi.update({
        degree: form.values.degree || null,
        experienceLevel: form.values.experienceLevel || null,
        phone: form.values.phone || null,
        traits: traitIds.map((traitId) => ({ traitId, level: null })),
      });
      const values = {
        phone: updated.phone ?? '',
        degree: updated.degree ?? '',
        experienceLevel: updated.experienceLevel ?? '',
      };
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

  async function uploadCv(file) {
    const updated = await profileApi.uploadCv(file);
    setHasCv(updated.hasCv);
    toast.success('CV mis à jour.');
  }

  if (status === 'loading') {
    return (
      <Workspace title="Mon profil">
        <Skeleton count={4} label="Chargement de votre profil" />
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

      <section className="card" aria-labelledby="identity-title">
        <div className="card__head">
          <h2 id="identity-title" className="card__title">Identité</h2>
        </div>
        <div className="card__body">
          <div className="profile__identity">
            <div>
              <p className="profile__name">{identity.firstName} {identity.lastName}</p>
              <p className="profile__email">{identity.email}</p>
            </div>
          </div>
          <Field label="Téléphone" type="tel" autoComplete="tel" hint="Facultatif."
            {...form.field('phone')} />
        </div>
      </section>

      <section className="card" aria-labelledby="path-title">
        <div className="card__head">
          <h2 id="path-title" className="card__title">Parcours</h2>
        </div>
        <div className="card__body profile__grid">
          <Select label="Diplôme" options={DEGREE_OPTIONS} placeholder="Sélectionnez votre diplôme"
            hint="Comparé au diplôme requis par chaque offre." {...form.field('degree')} />
          <Field label="Niveau d'expérience"
            hint="Facultatif. Par exemple : 5 ans en développement web."
            {...form.field('experienceLevel')} />
        </div>
      </section>

      <section className="card" aria-labelledby="cv-title">
        <div className="card__head">
          <h2 id="cv-title" className="card__title">CV</h2>
        </div>
        <div className="card__body">
          <CvUpload hasCv={hasCv} onUpload={uploadCv} onDownload={profileApi.downloadCv} />
        </div>
      </section>

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

      <div className="profile__savebar">
        <p className="profile__savestate" aria-live="polite">
          {dirty ? 'Modifications non enregistrées' : ''}
        </p>
        <Button onClick={save} loading={saving} disabled={!dirty}>
          Enregistrer les modifications
        </Button>
      </div>
    </Workspace>
  );
}
