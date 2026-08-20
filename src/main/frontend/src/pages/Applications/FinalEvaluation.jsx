import { useState } from 'react';
import { applicationsApi } from '../../api/applications.js';
import Alert from '../../components/Alert/Alert.jsx';
import Button from '../../components/Button/Button.jsx';
import Checkbox from '../../components/Checkbox/Checkbox.jsx';
import ConfirmDialog from '../../components/ConfirmDialog/ConfirmDialog.jsx';
import Field from '../../components/Field/Field.jsx';
import FormErrorSummary from '../../components/FormErrorSummary/FormErrorSummary.jsx';
import InfoHint from '../../components/InfoHint/InfoHint.jsx';
import Select from '../../components/Select/Select.jsx';
import { CONTRACT_OPTIONS, REMOTE_OPTIONS } from '../../constants/enums.js';
import { localDate } from '../../constants/format.js';
import { positiveNumber } from '../../constants/validation.js';
import useForm from '../../hooks/useForm.js';
import './finalEvaluation.css';

const EMPTY = {
  comment: '',
  expectedSalary: '', availabilityDate: '', envisagedContract: '', noticePeriod: '',
  scheduleFlexibility: '', remoteExpectation: '', cultureFit: '',
  negotiatedSalary: '', startDate: '', finalContract: '', trialPeriod: '',
  executiveStatus: false, benefits: '',
};

const RULES = {
  comment: { label: 'Commentaire global' },
  expectedSalary: { label: 'Salaire attendu', format: positiveNumber },
  availabilityDate: { label: 'Disponibilité' },
  envisagedContract: { label: 'Contrat envisagé' },
  noticePeriod: { label: 'Préavis' },
  scheduleFlexibility: { label: 'Flexibilité horaire' },
  remoteExpectation: { label: 'Attentes télétravail' },
  cultureFit: { label: 'Adéquation avec la culture' },

  negotiatedSalary: {
    label: 'Salaire négocié',
    required: 'Indiquez le salaire négocié.',
    format: positiveNumber,
  },
  startDate: {
    label: 'Date de prise de poste',
    required: 'Indiquez la date de prise de poste.',
    format: (value) =>
      value < localDate() ? 'Cette date est déjà passée.' : null,
  },
  finalContract: { label: 'Contrat final', required: 'Choisissez le contrat final.' },
  trialPeriod: { label: 'Période d\'essai' },
  benefits: { label: 'Avantages' },
};

const INTERVIEW_KEYS = [
  'comment', 'expectedSalary', 'availabilityDate', 'envisagedContract',
  'noticePeriod', 'scheduleFlexibility', 'remoteExpectation', 'cultureFit',
];

const HIRING_KEYS = ['negotiatedSalary', 'startDate', 'finalContract', 'trialPeriod', 'benefits'];

const num = (v) => (v === '' ? null : Number(v));
const str = (v) => (v.trim() === '' ? null : v.trim());

/**
 * The final HR decision. Interview data is recorded whatever the outcome and
 * the hiring terms only when hiring, so each action validates its own fields.
 * The two stand side by side where there is room.
 */
export default function FinalEvaluation({ app, offerTitle, onDone }) {
  const form = useForm(EMPTY, RULES);
  const [confirming, setConfirming] = useState(null);
  const [failure, setFailure] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  function attempt(decision) {
    const keys = decision === 'VALIDEE' ? [...INTERVIEW_KEYS, ...HIRING_KEYS] : INTERVIEW_KEYS;
    if (form.attempt(keys)) setConfirming(decision);
  }

  async function send() {
    const decision = confirming;
    const { values } = form;
    setFailure(null);
    setSubmitting(true);
    try {
      const updated = await applicationsApi.finalize(app.id, {
        decision,
        comment: str(values.comment),
        interview: {
          expectedSalary: num(values.expectedSalary),
          availabilityDate: values.availabilityDate || null,
          envisagedContract: values.envisagedContract || null,
          noticePeriod: str(values.noticePeriod),
          scheduleFlexibility: str(values.scheduleFlexibility),
          remoteExpectation: values.remoteExpectation || null,
          cultureFit: str(values.cultureFit),
        },
        hiring: decision === 'VALIDEE'
          ? {
              negotiatedSalary: num(values.negotiatedSalary),
              startDate: values.startDate,
              finalContract: values.finalContract,
              trialPeriod: str(values.trialPeriod),
              executiveStatus: values.executiveStatus,
              benefits: str(values.benefits),
            }
          : null,
      });
      onDone(updated, decision);
    } catch (apiError) {
      setFailure(apiError.message);
      setConfirming(null);
      setSubmitting(false);
    }
  }

  return (
    <div className="final">
      {failure && <Alert>{failure}</Alert>}
      <FormErrorSummary errors={form.currentErrors()} rules={RULES} />

      <div className="final__sections">
      <section className="card">
        <div className="card__head">
          <h2 className="card__title">Bilan de l'entretien</h2>
          <p className="card__subtitle">Conservé quelle que soit la décision.</p>
        </div>
        <div className="card__body">
          <Field label="Commentaire global" multiline rows={3} {...form.field('comment')} />
          <div className="final__grid">
            <Field label="Salaire attendu (€)" type="number" min="0"
              {...form.field('expectedSalary')} />
            <Field label="Disponibilité" type="date" {...form.field('availabilityDate')} />
            <Select label="Contrat envisagé" options={CONTRACT_OPTIONS} placeholder="Non précisé"
              {...form.field('envisagedContract')} />
            <Field label="Préavis" hint="Par exemple : 2 mois" {...form.field('noticePeriod')} />
            <Field label="Flexibilité horaire" hint="Par exemple : horaires décalés"
              {...form.field('scheduleFlexibility')} />
            <Select label="Attentes télétravail" options={REMOTE_OPTIONS} placeholder="Non précisé"
              {...form.field('remoteExpectation')} />
          </div>
          <Field label="Adéquation avec la culture" multiline rows={2} {...form.field('cultureFit')} />
        </div>
      </section>

      <section className="card">
        <div className="card__head">
          <h2 className="card__title">
            Conditions d'embauche
            <InfoHint label="Quand ces champs servent">
              Demandées uniquement si vous validez l'embauche.
            </InfoHint>
          </h2>
        </div>
        <div className="card__body">
          <div className="final__grid">
            <Field label="Salaire négocié (€)" type="number" min="0"
              {...form.field('negotiatedSalary')} />
            <Field label="Date de prise de poste" type="date" min={localDate()}
              {...form.field('startDate')} />
            <Select label="Contrat final" options={CONTRACT_OPTIONS} placeholder="Choisir"
              {...form.field('finalContract')} />
            <Field label="Période d'essai" {...form.field('trialPeriod')} />
          </div>
          <Checkbox label="Statut cadre" checked={form.values.executiveStatus}
            onChange={(e) => form.setValue('executiveStatus', e.target.checked)} />
          <Field label="Avantages" multiline rows={2} {...form.field('benefits')} />
        </div>
      </section>
      </div>

      <div className="final__decide">
        <Button variant="danger" onClick={() => attempt('REFUSEE')}>
          Ne pas retenir
        </Button>
        <Button onClick={() => attempt('VALIDEE')}>
          Valider l'embauche
        </Button>
      </div>

      <ConfirmDialog
        open={confirming === 'REFUSEE'}
        title="Ne pas retenir ce candidat ?"
        confirmLabel="Ne pas retenir"
        tone="danger"
        nextStatus="REFUSEE"
        missing={form.emptyOptional(INTERVIEW_KEYS)}
        busy={submitting}
        onConfirm={send}
        onCancel={() => setConfirming(null)}
      >
        <strong>{app.candidateFirstName} {app.candidateLastName}</strong> en est informé
        immédiatement et la candidature est close. Le bilan de l'entretien reste conservé.
      </ConfirmDialog>

      <ConfirmDialog
        open={confirming === 'VALIDEE'}
        title="Valider l'embauche ?"
        confirmLabel="Valider l'embauche"
        nextStatus="EMBAUCHEE"
        missing={form.emptyOptional([...INTERVIEW_KEYS, ...HIRING_KEYS])}
        busy={submitting}
        onConfirm={send}
        onCancel={() => setConfirming(null)}
      >
        Un dossier d'embauche est créé pour{' '}
        <strong>{app.candidateFirstName} {app.candidateLastName}</strong> sur le poste{' '}
        <strong>{offerTitle}</strong>, et la confirmation lui est envoyée.
      </ConfirmDialog>
    </div>
  );
}
