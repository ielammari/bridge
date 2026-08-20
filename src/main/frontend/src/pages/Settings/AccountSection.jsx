import { useState } from 'react';
import { settingsApi } from '../../api/settings.js';
import Button from '../../components/Button/Button.jsx';
import Field from '../../components/Field/Field.jsx';
import FormErrorSummary from '../../components/FormErrorSummary/FormErrorSummary.jsx';
import InfoHint from '../../components/InfoHint/InfoHint.jsx';
import { useToast } from '../../components/Toast/ToastContext.jsx';
import { emailFormat } from '../../constants/validation.js';
import useForm from '../../hooks/useForm.js';

const RULES = {
  email: { label: 'Adresse email', required: 'Indiquez votre adresse email.', format: emailFormat },
};

/**
 * The address the account signs in with. Name and contact details belong to the
 * profile, which is what a recruiter reads.
 */
export default function AccountSection({ account }) {
  const toast = useToast();
  const [saving, setSaving] = useState(false);

  const form = useForm({ email: account.email ?? '' }, RULES);

  const submit = form.handleSubmit(async (values) => {
    setSaving(true);
    try {
      // The account is written whole, so the fields this form does not hold go
      // back unchanged rather than being cleared.
      await settingsApi.updateAccount({ ...account, ...values });
      toast.success('Adresse email mise à jour.');
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
          Adresse email
          <InfoHint label="À propos de l'adresse">
            C'est avec cette adresse que vous vous connectez.
          </InfoHint>
        </h2>
      </div>
      <div className="card__body">
        <FormErrorSummary errors={form.currentErrors()} rules={RULES} />

        <Field label="Adresse email" type="email" autoComplete="email" {...form.field('email')} />

        <div className="settings__actions">
          <Button type="submit" loading={saving}>Enregistrer l'adresse</Button>
        </div>
      </div>
    </form>
  );
}
