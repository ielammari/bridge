import { useMemo, useRef, useState } from 'react';
import Alert from '../../components/Alert/Alert.jsx';
import Button from '../../components/Button/Button.jsx';
import ConfirmDialog from '../../components/ConfirmDialog/ConfirmDialog.jsx';
import Field from '../../components/Field/Field.jsx';
import FormErrorSummary from '../../components/FormErrorSummary/FormErrorSummary.jsx';
import Icon from '../../components/Icon/Icon.jsx';
import InfoHint from '../../components/InfoHint/InfoHint.jsx';
import Select from '../../components/Select/Select.jsx';
import TraitPicker from '../../components/TraitPicker/TraitPicker.jsx';
import { CONTRACT_OPTIONS, DEGREE_OPTIONS, REMOTE_OPTIONS } from '../../constants/enums.js';
import { positiveNumber } from '../../constants/validation.js';
import useForm from '../../hooks/useForm.js';
import useMediaQuery from '../../hooks/useMediaQuery.js';
import './offerForm.css';

const RULES = {
  title: { label: 'Titre du poste', required: 'Donnez un titre à l\'offre.' },
  company: { label: 'Entreprise' },
  description: { label: 'Description', required: 'Décrivez le poste.' },
  requiredDegree: { label: 'Niveau d\'études requis', required: 'Choisissez le niveau d\'études requis.' },
  contractType: { label: 'Type de contrat', required: 'Choisissez le type de contrat.' },
  location: { label: 'Localisation' },
  remoteMode: { label: 'Télétravail' },
  salaryMin: { label: 'Salaire minimum', format: positiveNumber },
  salaryMax: {
    label: 'Salaire maximum',
    format: (value, values) => {
      const positive = positiveNumber(value);
      if (positive) return positive;
      return values.salaryMin && Number(value) < Number(values.salaryMin)
        ? 'Le salaire maximum doit être supérieur au minimum.'
        : null;
    },
  },
};

function initialFrom(offer) {
  if (!offer) {
    return {
      title: '', company: '', description: '', requiredDegree: '', contractType: '',
      location: '', remoteMode: '', salaryMin: '', salaryMax: '',
    };
  }
  return {
    title: offer.title,
    company: offer.company ?? '',
    description: offer.description,
    requiredDegree: offer.requiredDegree ?? '',
    contractType: offer.contractType ?? '',
    location: offer.location ?? '',
    remoteMode: offer.remoteMode ?? '',
    salaryMin: offer.salaryMin ?? '',
    salaryMax: offer.salaryMax ?? '',
  };
}

function initialTraits(offer) {
  const mandatoryById = {};
  (offer?.requirements ?? []).forEach((r) => {
    mandatoryById[r.traitId] = r.mandatory;
  });
  return { traitIds: (offer?.requirements ?? []).map((r) => r.traitId), mandatoryById };
}

/**
 * Create or edit an offer. On create the two actions decide draft against
 * publish; on edit a single save keeps the current status. New traits default
 * to required.
 */
export default function OfferForm({ mode, offer, catalogue, onSubmit, onCancel, submitting }) {
  const form = useForm(initialFrom(offer), RULES);
  const [traits, setTraits] = useState(() => initialTraits(offer));
  const [confirming, setConfirming] = useState(null);
  const [failure, setFailure] = useState(null);
  const [attempted, setAttempted] = useState(false);
  const traitsRef = useRef(null);
  const reduceMotion = useMediaQuery('(prefers-reduced-motion: reduce)');

  const published = offer?.status === 'PUBLIEE';

  const labelById = useMemo(() => {
    const map = new Map();
    catalogue.forEach((cat) =>
      cat.traits.forEach((t) => map.set(t.id, { label: t.label, category: cat.label })));
    return map;
  }, [catalogue]);

  // A form level rule on the same timing as the field ones: silent until the
  // first submit attempt.
  function traitProblem() {
    if (traits.traitIds.length === 0) return 'Sélectionnez au moins un trait pour l\'offre.';
    if (!traits.traitIds.some((id) => traits.mandatoryById[id])) {
      return 'Marquez au moins un trait comme obligatoire : sans lui, aucune offre ne filtre les candidats.';
    }
    return null;
  }

  function setTraitIds(ids) {
    setTraits((current) => {
      const mandatoryById = { ...current.mandatoryById };
      ids.forEach((id) => {
        if (mandatoryById[id] === undefined) mandatoryById[id] = true;
      });
      Object.keys(mandatoryById).forEach((id) => {
        if (!ids.includes(Number(id))) delete mandatoryById[Number(id)];
      });
      return { traitIds: ids, mandatoryById };
    });
  }

  function setMandatory(id, mandatory) {
    setTraits((current) => ({
      ...current,
      mandatoryById: { ...current.mandatoryById, [id]: mandatory },
    }));
  }

  function payload(publishNow) {
    const { values } = form;
    return {
      title: values.title.trim(),
      company: values.company.trim() || null,
      description: values.description.trim(),
      requiredDegree: values.requiredDegree,
      contractType: values.contractType,
      location: values.location.trim() || null,
      remoteMode: values.remoteMode || null,
      salaryMin: values.salaryMin === '' ? null : Number(values.salaryMin),
      salaryMax: values.salaryMax === '' ? null : Number(values.salaryMax),
      requirements: traits.traitIds.map((id) => ({
        traitId: id,
        mandatory: Boolean(traits.mandatoryById[id]),
      })),
      publishNow,
    };
  }

  async function send(publishNow) {
    setFailure(null);
    try {
      await onSubmit(payload(publishNow));
    } catch (apiError) {
      setFailure(apiError.message);
      setConfirming(null);
    }
  }

  // A draft changes nothing anyone can see; anything reaching candidates is
  // confirmed first.
  function attempt(intent) {
    setAttempted(true);
    const fieldsOk = form.attempt();

    // The trait rule sits above the buttons that raise it, so the page brings
    // the section's top on screen. A field summary higher up takes precedence.
    if (fieldsOk && traitProblem()) {
      traitsRef.current?.scrollIntoView({
        block: 'start',
        behavior: reduceMotion ? 'auto' : 'smooth',
      });
    }

    if (!fieldsOk || traitProblem()) return;
    if (intent === 'draft') {
      send(false);
      return;
    }
    setConfirming(intent);
  }

  const traitError = attempted ? traitProblem() : null;

  return (
    <div className="offerform">
      {failure && <Alert>{failure}</Alert>}
      <FormErrorSummary errors={form.currentErrors()} rules={RULES} />

      <section className="card">
        <div className="card__body">
          <div className="offerform__grid">
            <Field label="Titre du poste" {...form.field('title')} />
            <Field label="Entreprise" hint="Bridge par défaut" {...form.field('company')} />
          </div>
          <Field label="Description" multiline rows={6} {...form.field('description')} />
          <div className="offerform__grid">
            <Select label="Niveau d'études requis" options={DEGREE_OPTIONS} placeholder="Choisir"
              {...form.field('requiredDegree')} />
            <Select label="Type de contrat" options={CONTRACT_OPTIONS} placeholder="Choisir"
              {...form.field('contractType')} />
            <Field label="Localisation" hint="Facultatif" {...form.field('location')} />
            <Select label="Télétravail" options={REMOTE_OPTIONS} placeholder="Non précisé"
              {...form.field('remoteMode')} />
            <Field label="Salaire minimum (€)" type="number" min="0" hint="Facultatif"
              {...form.field('salaryMin')} />
            <Field label="Salaire maximum (€)" type="number" min="0" hint="Facultatif"
              {...form.field('salaryMax')} />
          </div>
        </div>
      </section>

      <section className="card offerform__traits" ref={traitsRef}>
        <div className="card__head">
          <h2 className="card__title">
            Traits recherchés
            <InfoHint label="Obligatoire ou atout">
              Choisissez les traits, puis marquez chacun comme obligatoire (filtre les candidats) ou atout (utilisé pour le classement).
            </InfoHint>
          </h2>
        </div>
        <div className="card__body">
          {traitError && <Alert>{traitError}</Alert>}
          <TraitPicker catalogue={catalogue} value={traits.traitIds} onChange={setTraitIds}
            chipNote={(id) => (traits.mandatoryById[id] ? 'Obligatoire' : 'Atout')} />

          {traits.traitIds.length > 0 && (
            <ul className="reqlist">
              {traits.traitIds.map((id) => {
                const info = labelById.get(id);
                const mandatory = Boolean(traits.mandatoryById[id]);
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
          <Button onClick={() => attempt(published ? 'update' : 'draft')} loading={submitting}>
            Enregistrer les modifications
          </Button>
        ) : (
          <>
            <Button variant="secondary" onClick={() => attempt('draft')} loading={submitting}>
              Enregistrer le brouillon
            </Button>
            <Button onClick={() => attempt('publish')} loading={submitting}>
              Publier l'offre
            </Button>
          </>
        )}
      </div>

      <ConfirmDialog
        open={confirming === 'publish'}
        title="Publier cette offre ?"
        confirmLabel="Publier l'offre"
        busy={submitting}
        missing={form.emptyOptional()}
        onConfirm={() => send(true)}
        onCancel={() => setConfirming(null)}
      >
        <strong>{form.values.title}</strong> deviendra visible pour tous les candidats dont le
        profil correspond, et ils pourront postuler immédiatement.
      </ConfirmDialog>

      <ConfirmDialog
        open={confirming === 'update'}
        title="Modifier une offre déjà publiée ?"
        confirmLabel="Enregistrer les modifications"
        busy={submitting}
        missing={form.emptyOptional()}
        onConfirm={() => send(false)}
        onCancel={() => setConfirming(null)}
      >
        Cette offre est publiée. Changer ses traits obligatoires change aussi qui la voit : des
        candidats qui y avaient accès peuvent la perdre de vue.
      </ConfirmDialog>
    </div>
  );
}
