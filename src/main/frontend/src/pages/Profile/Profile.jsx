import { useEffect, useState } from 'react';
import { profileApi } from '../../api/profile.js';
import { traitsApi } from '../../api/traits.js';
import Alert from '../../components/Alert/Alert.jsx';
import Button from '../../components/Button/Button.jsx';
import CvUpload from '../../components/CvUpload/CvUpload.jsx';
import Field from '../../components/Field/Field.jsx';
import Icon from '../../components/Icon/Icon.jsx';
import Select from '../../components/Select/Select.jsx';
import TraitPicker from '../../components/TraitPicker/TraitPicker.jsx';
import Workspace from '../Workspace/Workspace.jsx';
import './profile.css';

const DEGREE_OPTIONS = [
  { value: 'BAC', label: 'Baccalauréat' },
  { value: 'BAC_2', label: 'Bac +2' },
  { value: 'BAC_3', label: 'Bac +3 (Licence)' },
  { value: 'BAC_5', label: 'Bac +5 (Master)' },
  { value: 'DOCTORAT', label: 'Doctorat' },
];

function snapshot(form) {
  return JSON.stringify({
    phone: form.phone,
    degree: form.degree,
    experienceLevel: form.experienceLevel,
    traitIds: [...form.traitIds].sort((a, b) => a - b),
  });
}

export default function Profile() {
  const [status, setStatus] = useState('loading');
  const [catalogue, setCatalogue] = useState([]);
  const [hasCv, setHasCv] = useState(false);
  const [form, setForm] = useState(null);
  const [saved, setSaved] = useState('');
  const [saving, setSaving] = useState(false);
  const [saveError, setSaveError] = useState(null);
  const [baseline, setBaseline] = useState('');

  useEffect(() => {
    let cancelled = false;
    Promise.all([profileApi.read(), traitsApi.catalogue()])
      .then(([profile, cats]) => {
        if (cancelled) return;
        const loaded = {
          phone: profile.phone ?? '',
          degree: profile.degree ?? '',
          experienceLevel: profile.experienceLevel ?? '',
          traitIds: profile.traits.map((t) => t.traitId),
          email: profile.email,
          firstName: profile.firstName,
          lastName: profile.lastName,
        };
        setCatalogue(cats);
        setHasCv(profile.hasCv);
        setForm(loaded);
        setBaseline(snapshot(loaded));
        setStatus('ready');
      })
      .catch(() => {
        if (!cancelled) setStatus('error');
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const dirty = form && snapshot(form) !== baseline;

  function set(key, val) {
    setForm((f) => ({ ...f, [key]: val }));
    setSaved('');
  }

  async function save() {
    setSaveError(null);
    setSaving(true);
    try {
      const updated = await profileApi.update({
        degree: form.degree || null,
        experienceLevel: form.experienceLevel || null,
        phone: form.phone || null,
        traits: form.traitIds.map((traitId) => ({ traitId, level: null })),
      });
      const next = {
        ...form,
        phone: updated.phone ?? '',
        degree: updated.degree ?? '',
        experienceLevel: updated.experienceLevel ?? '',
        traitIds: updated.traits.map((t) => t.traitId),
      };
      setForm(next);
      setBaseline(snapshot(next));
      setSaved('Profil enregistré.');
    } catch (apiError) {
      setSaveError(apiError.message);
    } finally {
      setSaving(false);
    }
  }

  async function uploadCv(file) {
    const updated = await profileApi.uploadCv(file);
    setHasCv(updated.hasCv);
    setSaved('CV mis à jour.');
  }

  if (status === 'loading') {
    return (
      <Workspace title="Mon profil">
        <p className="profile__loading">Chargement de votre profil...</p>
      </Workspace>
    );
  }

  if (status === 'error') {
    return (
      <Workspace title="Mon profil">
        <Alert>Votre profil n'a pas pu être chargé. Actualisez la page pour réessayer.</Alert>
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
              <p className="profile__name">{form.firstName} {form.lastName}</p>
              <p className="profile__email">{form.email}</p>
            </div>
          </div>
          <Field
            label="Téléphone"
            type="tel"
            value={form.phone}
            onChange={(e) => set('phone', e.target.value)}
            hint="Facultatif."
            autoComplete="tel"
          />
        </div>
      </section>

      <section className="card" aria-labelledby="path-title">
        <div className="card__head">
          <h2 id="path-title" className="card__title">Parcours</h2>
        </div>
        <div className="card__body profile__grid">
          <Select
            label="Diplôme"
            value={form.degree}
            onChange={(e) => set('degree', e.target.value)}
            options={DEGREE_OPTIONS}
            placeholder="Sélectionnez votre diplôme"
            hint="Comparé au diplôme requis par chaque offre."
          />
          <Field
            label="Niveau d'expérience"
            value={form.experienceLevel}
            onChange={(e) => set('experienceLevel', e.target.value)}
            hint="Facultatif. Par exemple : 5 ans en développement web."
          />
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
          <TraitPicker
            catalogue={catalogue}
            value={form.traitIds}
            onChange={(ids) => set('traitIds', ids)}
          />
        </div>
      </section>

      <div className="profile__savebar">
        <div className="profile__savestate" aria-live="polite">
          {saveError && <span className="profile__saveerror">{saveError}</span>}
          {!saveError && saved && (
            <span className="profile__saveok">
              <Icon name="check" /> {saved}
            </span>
          )}
          {!saveError && !saved && dirty && (
            <span className="profile__savehint">Modifications non enregistrées</span>
          )}
        </div>
        <Button onClick={save} loading={saving} disabled={!dirty}>
          Enregistrer les modifications
        </Button>
      </div>
    </Workspace>
  );
}
