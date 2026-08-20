import { useState } from 'react';
import { settingsApi } from '../../api/settings.js';
import Button from '../../components/Button/Button.jsx';
import Field from '../../components/Field/Field.jsx';
import FormErrorSummary from '../../components/FormErrorSummary/FormErrorSummary.jsx';
import InfoHint from '../../components/InfoHint/InfoHint.jsx';
import Select from '../../components/Select/Select.jsx';
import { useToast } from '../../components/Toast/ToastContext.jsx';
import { GENDER_OPTIONS } from '../../constants/enums.js';
import { localDate } from '../../constants/format.js';
import { birthDateProblem, phoneFormat } from '../../constants/validation.js';
import useForm from '../../hooks/useForm.js';

const RULES = {
  firstName: { label: 'Prénom', required: 'Indiquez votre prénom.' },
  lastName: { label: 'Nom', required: 'Indiquez votre nom.' },
  phone: { label: 'Téléphone', format: phoneFormat },
  birthDate: { label: 'Date de naissance', format: birthDateProblem },
  gender: { label: 'Sexe' },
  city: { label: 'Ville' },
  country: { label: 'Pays' },
};

/**
 * Who the candidate is, on the page a recruiter reads. The sign in address
 * stays with the account, being a credential rather than a detail.
 */
export default function IdentitySection({ account }) {
  const toast = useToast();
  const [saving, setSaving] = useState(false);

  const form = useForm({
    firstName: account.firstName ?? '',
    lastName: account.lastName ?? '',
    phone: account.phone ?? '',
    birthDate: account.birthDate ?? '',
    gender: account.gender ?? '',
    city: account.city ?? '',
    country: account.country ?? '',
  }, RULES);

  const submit = form.handleSubmit(async (values) => {
    setSaving(true);
    try {
      // The account is written whole, so a form holding half of it sends the
      // other half back unchanged rather than clearing it.
      await settingsApi.updateAccount({
        ...account,
        ...values,
        gender: values.gender || null,
        birthDate: values.birthDate || null,
      });
      toast.success('Informations enregistrées.');
    } catch (apiError) {
      toast.error(apiError.message);
    } finally {
      setSaving(false);
    }
  });

  return (
    <form className="card" onSubmit={submit} noValidate>
      <div className="card__head">
        <h2 className="card__title">
          Informations personnelles
          <InfoHint label="À propos de ces informations">
            Visibles par le recruteur qui examine vos candidatures.
          </InfoHint>
        </h2>
      </div>
      <div className="card__body">
        <FormErrorSummary errors={form.currentErrors()} rules={RULES} />

        <div className="settings__grid">
          <Field label="Prénom" autoComplete="given-name" {...form.field('firstName')} />
          <Field label="Nom" autoComplete="family-name" {...form.field('lastName')} />
          <Field label="Téléphone" type="tel" autoComplete="tel" hint="Facultatif"
            {...form.field('phone')} />
          <Field label="Date de naissance" type="date" max={localDate()}
            {...form.field('birthDate')} />
          <Select label="Sexe" options={GENDER_OPTIONS} placeholder="Ne pas préciser"
            hint="Facultatif" {...form.field('gender')} />
          <Field label="Ville" autoComplete="address-level2" hint="Facultatif"
            {...form.field('city')} />
          <Field label="Pays" autoComplete="country-name" hint="Facultatif"
            {...form.field('country')} />
        </div>

        <div className="settings__actions">
          <Button type="submit" loading={saving}>Enregistrer</Button>
        </div>
      </div>
    </form>
  );
}
