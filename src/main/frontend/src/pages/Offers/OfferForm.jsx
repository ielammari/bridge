import { useMemo, useState } from 'react';
import Alert from '../../components/Alert/Alert.jsx';
import Button from '../../components/Button/Button.jsx';
import Field from '../../components/Field/Field.jsx';
import Icon from '../../components/Icon/Icon.jsx';
import Select from '../../components/Select/Select.jsx';
import TraitPicker from '../../components/TraitPicker/TraitPicker.jsx';
import { CONTRACT_OPTIONS, DEGREE_OPTIONS, REMOTE_OPTIONS } from '../../constants/enums.js';
import './offerForm.css';

function initialFrom(offer) {
  if (!offer) {
    return {
      title: '', description: '', requiredDegree: '', contractType: '',
      location: '', remoteMode: '', salaryMin: '', salaryMax: '',
      traitIds: [], mandatoryById: {},
    };
  }
  const mandatoryById = {};
  offer.requirements.forEach((r) => {
    mandatoryById[r.traitId] = r.mandatory;
  });
  return {
    title: offer.title,
    description: offer.description,
    requiredDegree: offer.requiredDegree ?? '',
    contractType: offer.contractType ?? '',
    location: offer.location ?? '',
    remoteMode: offer.remoteMode ?? '',
    salaryMin: offer.salaryMin ?? '',
    salaryMax: offer.salaryMax ?? '',
    traitIds: offer.requirements.map((r) => r.traitId),
    mandatoryById,
  };
}

function validate(form) {
  const errors = {};
  if (!form.title.trim()) errors.title = 'Donnez un titre à l\'offre.';
  if (!form.description.trim()) errors.description = 'Décrivez le poste.';
  if (!form.requiredDegree) errors.requiredDegree = 'Choisissez le diplôme requis.';
  if (!form.contractType) errors.contractType = 'Choisissez le type de contrat.';
  if (form.salaryMin && form.salaryMax && Number(form.salaryMin) > Number(form.salaryMax)) {
    errors.salaryMax = 'Le salaire maximum doit être supérieur au minimum.';
  }
  if (form.traitIds.length === 0) {
    errors.traits = 'Sélectionnez au moins un trait pour l\'offre.';
  } else if (!form.traitIds.some((id) => form.mandatoryById[id])) {
    errors.traits = 'Marquez au moins un trait comme obligatoire.';
  }
  return errors;
}

/**
 * Create or edit an offer. On create, the two actions decide draft vs publish;
 * on edit, a single save keeps the current status (publish and close live on
 * the list). New traits default to required, since an offer needs at least one.
 */
export default function OfferForm({ mode, offer, catalogue, onSubmit, onCancel, submitting }) {
  const [form, setForm] = useState(() => initialFrom(offer));
  const [errors, setErrors] = useState({});
  const [failure, setFailure] = useState(null);

  const labelById = useMemo(() => {
    const map = new Map();
    catalogue.forEach((cat) => cat.traits.forEach((t) => map.set(t.id, { label: t.label, category: cat.label })));
    return map;
  }, [catalogue]);

  const set = (key) => (event) => setForm({ ...form, [key]: event.target.value });

  function setTraitIds(ids) {
    const mandatoryById = { ...form.mandatoryById };
    ids.forEach((id) => {
      if (mandatoryById[id] === undefined) mandatoryById[id] = true; // default required
    });
    Object.keys(mandatoryById).forEach((id) => {
      if (!ids.includes(Number(id))) delete mandatoryById[Number(id)];
    });
    setForm({ ...form, traitIds: ids, mandatoryById });
  }

  function setMandatory(id, mandatory) {
    setForm({ ...form, mandatoryById: { ...form.mandatoryById, [id]: mandatory } });
  }

  async function submit(publishNow) {
    const found = validate(form);
    setErrors(found);
    if (Object.keys(found).length > 0) return;

    setFailure(null);
    const payload = {
      title: form.title.trim(),
      description: form.description.trim(),
      requiredDegree: form.requiredDegree,
      contractType: form.contractType,
      location: form.location.trim() || null,
      remoteMode: form.remoteMode || null,
      salaryMin: form.salaryMin === '' ? null : Number(form.salaryMin),
      salaryMax: form.salaryMax === '' ? null : Number(form.salaryMax),
      requirements: form.traitIds.map((id) => ({ traitId: id, mandatory: !!form.mandatoryById[id] })),
      publishNow,
    };

    try {
      await onSubmit(payload);
    } catch (apiError) {
      setFailure(apiError.message);
    }
  }

  return (
    <div className="offerform">
      <div className="offerform__head">
        <h2>{mode === 'edit' ? 'Modifier l\'offre' : 'Nouvelle offre'}</h2>
        <Button variant="text" onClick={onCancel}>Retour à la liste</Button>
      </div>

      {failure && <Alert>{failure}</Alert>}

      <section className="card">
        <div className="card__body">
          <Field label="Titre du poste" value={form.title} onChange={set('title')}
            error={errors.title} required />
          <Field label="Description" value={form.description} onChange={set('description')}
            error={errors.description} multiline rows={6} required />
          <div className="offerform__grid">
            <Select label="Diplôme requis" value={form.requiredDegree} onChange={set('requiredDegree')}
              options={DEGREE_OPTIONS} placeholder="Choisir" required />
            <Select label="Type de contrat" value={form.contractType} onChange={set('contractType')}
              options={CONTRACT_OPTIONS} placeholder="Choisir" required />
            <Field label="Localisation" value={form.location} onChange={set('location')} hint="Facultatif." />
            <Select label="Télétravail" value={form.remoteMode} onChange={set('remoteMode')}
              options={REMOTE_OPTIONS} placeholder="Non précisé" />
            <Field label="Salaire minimum (€)" type="number" value={form.salaryMin}
              onChange={set('salaryMin')} hint="Facultatif." />
            <Field label="Salaire maximum (€)" type="number" value={form.salaryMax}
              onChange={set('salaryMax')} error={errors.salaryMax} hint="Facultatif." />
          </div>
        </div>
      </section>

      <section className="card">
        <div className="card__head">
          <h2 className="card__title">Traits recherchés</h2>
          <p className="card__subtitle">
            Choisissez les traits, puis marquez chacun comme obligatoire (filtre les candidats) ou
            atout (utilisé pour le classement).
          </p>
        </div>
        <div className="card__body">
          {errors.traits && <Alert>{errors.traits}</Alert>}
          <TraitPicker catalogue={catalogue} value={form.traitIds} onChange={setTraitIds} />

          {form.traitIds.length > 0 && (
            <ul className="reqlist">
              {form.traitIds.map((id) => {
                const info = labelById.get(id);
                const mandatory = !!form.mandatoryById[id];
                return (
                  <li key={id} className="reqlist__row">
                    <span className="reqlist__label">
                      {info?.label ?? id}
                      <span className="reqlist__cat">{info?.category}</span>
                    </span>
                    <div className="segmented" role="group" aria-label={`Type de trait pour ${info?.label ?? ''}`}>
                      <button type="button" aria-pressed={mandatory}
                        className={`segmented__opt${mandatory ? ' segmented__opt--on' : ''}`}
                        onClick={() => setMandatory(id, true)}>
                        <Icon name="check" /> Obligatoire
                      </button>
                      <button type="button" aria-pressed={!mandatory}
                        className={`segmented__opt${!mandatory ? ' segmented__opt--on' : ''}`}
                        onClick={() => setMandatory(id, false)}>
                        Atout
                      </button>
                    </div>
                  </li>
                );
              })}
            </ul>
          )}
        </div>
      </section>

      <div className="offerform__actions">
        <Button variant="secondary" onClick={onCancel}>Annuler</Button>
        {mode === 'edit' ? (
          <Button onClick={() => submit(false)} loading={submitting}>Enregistrer les modifications</Button>
        ) : (
          <>
            <Button variant="secondary" onClick={() => submit(false)} loading={submitting}>
              Enregistrer le brouillon
            </Button>
            <Button onClick={() => submit(true)} loading={submitting}>Publier l'offre</Button>
          </>
        )}
      </div>
    </div>
  );
}
