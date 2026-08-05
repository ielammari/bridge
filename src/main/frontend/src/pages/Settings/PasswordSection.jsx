import { useState } from 'react';
import { settingsApi } from '../../api/settings.js';
import Button from '../../components/Button/Button.jsx';
import FormErrorSummary from '../../components/FormErrorSummary/FormErrorSummary.jsx';
import PasswordField from '../../components/PasswordField/PasswordField.jsx';
import PasswordRules from '../../components/PasswordField/PasswordRules.jsx';
import { useToast } from '../../components/Toast/ToastContext.jsx';
import { passwordProblem } from '../../constants/password.js';
import useForm from '../../hooks/useForm.js';

const EMPTY = { currentPassword: '', newPassword: '', confirmPassword: '' };

const RULES = {
  currentPassword: {
    label: 'Mot de passe actuel',
    required: 'Saisissez votre mot de passe actuel.',
  },
  newPassword: {
    label: 'Nouveau mot de passe',
    required: 'Choisissez un nouveau mot de passe.',
    format: (value, values) => passwordProblem(value, values),
  },
  confirmPassword: {
    label: 'Confirmation',
    required: 'Saisissez à nouveau le nouveau mot de passe.',
    format: (value, values) =>
      value === values.newPassword ? null : 'Les deux mots de passe ne sont pas identiques.',
  },
};

/** Changing the password, proven by the current one. */
export default function PasswordSection() {
  const toast = useToast();
  const [saving, setSaving] = useState(false);
  const form = useForm(EMPTY, RULES);

  const submit = form.handleSubmit(async (values) => {
    setSaving(true);
    try {
      await settingsApi.changePassword({
        currentPassword: values.currentPassword,
        newPassword: values.newPassword,
      });
      form.setValues(EMPTY);
      toast.success('Mot de passe modifié.');
    } catch (apiError) {
      toast.error(apiError.message);
    } finally {
      setSaving(false);
    }
  });

  return (
    <form className="card" onSubmit={submit} noValidate>
      <div className="card__head">
        <h2 className="card__title">Mot de passe</h2>
        <p className="card__subtitle">
          Le mot de passe actuel est demandé pour qu'une session laissée ouverte ne suffise pas à
          le changer.
        </p>
      </div>
      <div className="card__body">
        <FormErrorSummary errors={form.currentErrors()} rules={RULES} />

        <PasswordField label="Mot de passe actuel" autoComplete="current-password"
          {...form.field('currentPassword')} />

        <div className="settings__grid">
          <PasswordField label="Nouveau mot de passe" autoComplete="new-password"
            rulesId="new-password-rules" {...form.field('newPassword')} />
          <PasswordField label="Confirmer le nouveau mot de passe" autoComplete="new-password"
            {...form.field('confirmPassword')} />
        </div>

        <PasswordRules id="new-password-rules" value={form.values.newPassword} context={form.values} />

        <div className="settings__actions">
          <Button type="submit" loading={saving}>Modifier le mot de passe</Button>
        </div>
      </div>
    </form>
  );
}
