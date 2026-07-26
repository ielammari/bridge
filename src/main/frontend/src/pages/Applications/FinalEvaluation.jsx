import { useState } from 'react';
import { applicationsApi } from '../../api/applications.js';
import Alert from '../../components/Alert/Alert.jsx';
import Button from '../../components/Button/Button.jsx';
import Field from '../../components/Field/Field.jsx';
import Select from '../../components/Select/Select.jsx';
import { CONTRACT_OPTIONS, REMOTE_OPTIONS } from '../../constants/enums.js';

const EMPTY = {
  comment: '',
  expectedSalary: '', availabilityDate: '', envisagedContract: '', noticePeriod: '',
  scheduleFlexibility: '', remoteExpectation: '', cultureFit: '',
  negotiatedSalary: '', startDate: '', finalContract: '', trialPeriod: '',
  executiveStatus: false, benefits: '',
};

const num = (v) => (v === '' ? null : Number(v));
const str = (v) => (v.trim() === '' ? null : v.trim());

/**
 * The final HR decision. Interview data is recorded whatever the outcome; the
 * hiring terms below are used only when the candidate is hired.
 */
export default function FinalEvaluation({ app, offerTitle, onDone, onCancel }) {
  const [form, setForm] = useState(EMPTY);
  const [errors, setErrors] = useState({});
  const [failure, setFailure] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  const set = (key) => (e) => setForm({ ...form, [key]: e.target.value });

  function interviewPayload() {
    return {
      expectedSalary: num(form.expectedSalary),
      availabilityDate: form.availabilityDate || null,
      envisagedContract: form.envisagedContract || null,
      noticePeriod: str(form.noticePeriod),
      scheduleFlexibility: str(form.scheduleFlexibility),
      remoteExpectation: form.remoteExpectation || null,
      cultureFit: str(form.cultureFit),
    };
  }

  async function submit(decision) {
    setFailure(null);
    if (decision === 'VALIDEE') {
      const found = {};
      if (!form.negotiatedSalary) found.negotiatedSalary = 'Indiquez le salaire négocié.';
      if (!form.startDate) found.startDate = 'Indiquez la date de prise de poste.';
      if (!form.finalContract) found.finalContract = 'Choisissez le contrat final.';
      setErrors(found);
      if (Object.keys(found).length > 0) return;
    }

    setSubmitting(true);
    try {
      const updated = await applicationsApi.finalize(app.id, {
        decision,
        comment: str(form.comment),
        interview: interviewPayload(),
        hiring: decision === 'VALIDEE'
          ? {
              negotiatedSalary: num(form.negotiatedSalary),
              startDate: form.startDate,
              finalContract: form.finalContract,
              trialPeriod: str(form.trialPeriod),
              executiveStatus: form.executiveStatus,
              benefits: str(form.benefits),
            }
          : null,
      });
      onDone(updated);
    } catch (apiError) {
      setFailure(apiError.message);
      setSubmitting(false);
    }
  }

  return (
    <div className="final">
      <div className="final__head">
        <div>
          <h2 className="final__title">Entretien final : {app.candidateFirstName} {app.candidateLastName}</h2>
          <p className="final__sub">{offerTitle}</p>
        </div>
        <Button variant="text" onClick={onCancel}>Retour à la liste</Button>
      </div>

      {failure && <Alert>{failure}</Alert>}

      <section className="card">
        <div className="card__head">
          <h2 className="card__title">Bilan de l'entretien</h2>
          <p className="card__subtitle">Conservé quelle que soit la décision.</p>
        </div>
        <div className="card__body">
          <Field label="Commentaire global" value={form.comment} onChange={set('comment')}
            multiline rows={3} />
          <div className="final__grid">
            <Field label="Salaire attendu (€)" type="number" value={form.expectedSalary}
              onChange={set('expectedSalary')} />
            <Field label="Disponibilité" type="date" value={form.availabilityDate}
              onChange={set('availabilityDate')} />
            <Select label="Contrat envisagé" value={form.envisagedContract}
              onChange={set('envisagedContract')} options={CONTRACT_OPTIONS} placeholder="Non précisé" />
            <Field label="Préavis" value={form.noticePeriod} onChange={set('noticePeriod')} />
            <Field label="Flexibilité horaire" value={form.scheduleFlexibility}
              onChange={set('scheduleFlexibility')} />
            <Select label="Attentes télétravail" value={form.remoteExpectation}
              onChange={set('remoteExpectation')} options={REMOTE_OPTIONS} placeholder="Non précisé" />
          </div>
          <Field label="Adéquation avec la culture" value={form.cultureFit}
            onChange={set('cultureFit')} multiline rows={2} />
        </div>
      </section>

      <section className="card">
        <div className="card__head">
          <h2 className="card__title">Conditions d'embauche</h2>
          <p className="card__subtitle">À remplir uniquement en cas d'embauche.</p>
        </div>
        <div className="card__body">
          <div className="final__grid">
            <Field label="Salaire négocié (€)" type="number" value={form.negotiatedSalary}
              onChange={set('negotiatedSalary')} error={errors.negotiatedSalary} />
            <Field label="Date de prise de poste" type="date" value={form.startDate}
              onChange={set('startDate')} error={errors.startDate} />
            <Select label="Contrat final" value={form.finalContract} onChange={set('finalContract')}
              options={CONTRACT_OPTIONS} placeholder="Choisir" error={errors.finalContract} />
            <Field label="Période d'essai" value={form.trialPeriod} onChange={set('trialPeriod')} />
          </div>
          <label className="final__check">
            <input type="checkbox" checked={form.executiveStatus}
              onChange={(e) => setForm({ ...form, executiveStatus: e.target.checked })} />
            Statut cadre
          </label>
          <Field label="Avantages" value={form.benefits} onChange={set('benefits')} multiline rows={2} />
        </div>
      </section>

      <div className="final__decide">
        <Button variant="secondary" onClick={() => submit('REFUSEE')} loading={submitting}>
          Ne pas retenir
        </Button>
        <Button onClick={() => submit('VALIDEE')} loading={submitting}>
          Valider l'embauche
        </Button>
      </div>
    </div>
  );
}
