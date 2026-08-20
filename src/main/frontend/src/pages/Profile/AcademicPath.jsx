import { useState } from 'react';
import Button from '../../components/Button/Button.jsx';
import ConfirmDialog from '../../components/ConfirmDialog/ConfirmDialog.jsx';
import Field from '../../components/Field/Field.jsx';
import InfoHint from '../../components/InfoHint/InfoHint.jsx';
import useForm from '../../hooks/useForm.js';

const THIS_YEAR = new Date().getFullYear();

const year = (value) => {
  const parsed = Number(value);
  return Number.isInteger(parsed) && parsed >= 1950 && parsed <= 2100
    ? null
    : 'Saisissez une année à quatre chiffres.';
};

const RULES = {
  title: { label: 'Intitulé', required: 'Indiquez l\'intitulé du diplôme.' },
  institution: { label: 'Établissement', required: 'Indiquez l\'établissement.' },
  fieldOfStudy: { label: 'Domaine' },
  startYear: {
    label: 'Année de début',
    required: 'Indiquez l\'année de début.',
    format: year,
  },
  endYear: {
    label: 'Année d\'obtention',
    format: (value, values) => year(value)
      ?? (Number(value) < Number(values.startYear)
        ? 'L\'année d\'obtention ne peut pas précéder celle de début.'
        : null),
  },
};

const blank = () => ({ title: '', institution: '', fieldOfStudy: '', startYear: '', endYear: '' });

const valuesOf = (entry) => ({
  title: entry.title ?? '',
  institution: entry.institution ?? '',
  fieldOfStudy: entry.fieldOfStudy ?? '',
  startYear: entry.startYear ? String(entry.startYear) : '',
  endYear: entry.endYear ? String(entry.endYear) : '',
});

/** "2018 à 2020", or "depuis 2021" while it is still being read for. */
const period = (entry) =>
  (entry.endYear ? `${entry.startYear} à ${entry.endYear}` : `depuis ${entry.startYear}`);

/**
 * One qualification being added or changed. It owns its form state, so the
 * caller remounts it with a key and another entry starts clean.
 */
function EducationForm({ initial, submitLabel, onSubmit, onCancel, busy }) {
  const form = useForm(initial, RULES);

  return (
    <form
      className="path__form"
      onSubmit={(event) => {
        event.preventDefault();
        if (!form.attempt()) return;
        onSubmit({
          title: form.values.title.trim(),
          institution: form.values.institution.trim(),
          fieldOfStudy: form.values.fieldOfStudy.trim() || null,
          startYear: Number(form.values.startYear),
          endYear: form.values.endYear ? Number(form.values.endYear) : null,
        });
      }}
    >
      <Field label="Intitulé du diplôme" hint="Ex. : Master informatique" {...form.field('title')} />
      <Field label="Établissement" hint="Ex. : INSA Lyon" {...form.field('institution')} />
      <Field label="Domaine" hint="Facultatif" {...form.field('fieldOfStudy')} />

      <div className="path__years">
        <Field label="Année de début" type="number" placeholder={String(THIS_YEAR - 4)}
          {...form.field('startYear')} />
        <Field label="Année d'obtention" type="number" placeholder={String(THIS_YEAR)}
          hint="Vide si en cours" {...form.field('endYear')} />
      </div>

      <div className="path__actions">
        <Button variant="secondary" type="button" onClick={onCancel}>Annuler</Button>
        <Button type="submit" loading={busy}>{submitLabel}</Button>
      </div>
    </form>
  );
}

/**
 * The qualifications a candidate holds, most recent first. Separate from the
 * education level beside it, which is the one ordered value an offer is
 * filtered against. Editing opens the form in the entry's own place.
 */
export default function AcademicPath({ entries, onAdd, onUpdate, onRemove, busy }) {
  const [editing, setEditing] = useState(null); // null, 'new', or an entry id
  const [confirming, setConfirming] = useState(null);

  const close = () => setEditing(null);

  async function submit(payload) {
    await (editing === 'new' ? onAdd(payload) : onUpdate(editing, payload));
    close();
  }

  return (
    <section className="card" aria-labelledby="path-title">
      <div className="card__head">
        <h2 id="path-title" className="card__title">
          Parcours
          <InfoHint label="À propos du parcours">
            Les diplômes que vous avez obtenus, et celui que vous préparez. Chaque formation est enregistrée dès son ajout.
          </InfoHint>
        </h2>
      </div>

      <div className="card__body">
        {entries.length === 0 && editing !== 'new' && (
          <p className="path__empty">
            Aucune formation renseignée. Votre parcours détaille le niveau d'études et se lit
            avec votre candidature.
          </p>
        )}

        {entries.length > 0 && (
          <ol className="path">
            {entries.map((entry) => (
              <li key={entry.id} className="path__item">
                {editing === entry.id ? (
                  <EducationForm
                    key={entry.id}
                    initial={valuesOf(entry)}
                    submitLabel="Enregistrer la formation"
                    onSubmit={submit}
                    onCancel={close}
                    busy={busy}
                  />
                ) : (
                  <>
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
                    <div className="path__rowactions">
                      <Button variant="text" onClick={() => setEditing(entry.id)}>Modifier</Button>
                      <Button variant="text" onClick={() => setConfirming(entry)}>Supprimer</Button>
                    </div>
                  </>
                )}
              </li>
            ))}
          </ol>
        )}

        {editing === 'new' && (
          <EducationForm
            key="new"
            initial={blank()}
            submitLabel="Ajouter la formation"
            onSubmit={submit}
            onCancel={close}
            busy={busy}
          />
        )}

        {editing === null && (
          <Button variant="secondary" onClick={() => setEditing('new')}>
            Ajouter une formation
          </Button>
        )}
      </div>

      {confirming && (
        <ConfirmDialog
          open
          title="Supprimer cette formation ?"
          confirmLabel="Supprimer la formation"
          tone="danger"
          busy={busy}
          onConfirm={async () => {
            await onRemove(confirming.id);
            setConfirming(null);
          }}
          onCancel={() => setConfirming(null)}
        >
          <strong>{confirming.title}</strong> ({confirming.institution}) sera retirée de votre
          parcours. Cette suppression ne se reprend pas.
        </ConfirmDialog>
      )}
    </section>
  );
}
