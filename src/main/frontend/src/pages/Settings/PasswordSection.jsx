import { useState } from 'react';
import { settingsApi } from '../../api/settings.js';
import Button from '../../components/Button/Button.jsx';
import FormErrorSummary from '../../components/FormErrorSummary/FormErrorSummary.jsx';
import InfoHint from '../../components/InfoHint/InfoHint.jsx';
import PasswordField from '../../components/PasswordField/PasswordField.jsx';
import PasswordRules from '../../components/PasswordField/PasswordRules.jsx';
import { useToast } from '../../components/Toast/ToastContext.jsx';
import { passwordProblem } from '../../constants/password.js';
import { useAuth } from '../../context/AuthContext.jsx';
import useForm from '../../hooks/useForm.js';

const EMPTY = { currentPassword: '', newPassword: '', confirmPassword: '' };

const CURRENT = {
  currentPassword: {
    label: 'Mot de passe actuel',
    required: 'Saisissez votre mot de passe actuel.',
  },
};

const RULES = {
  newPassword: {
    label: 'Nouveau mot de passe',
    required: 'Choisissez un nouveau mot de passe.',
    format: (value, values) =>
      (value === values.currentPassword
        ? 'Le nouveau mot de passe doit être différent de l\'ancien.'
        : passwordProblem(value, values)),
  },
  confirmPassword: {
    label: 'Confirmation',
    required: 'Saisissez à nouveau le nouveau mot de passe.',
    format: (value, values) =>
      value === values.newPassword ? null : 'Les deux mots de passe ne sont pas identiques.',
  },
};

/**
 * Changing the password, proven by the current one. An account that reaches
 * the application through Google alone has none to prove, so it sets its
 * first one here instead.
 */
export default function PasswordSection() {
  const toast = useToast();
  const { user, refresh } = useAuth();
  const [saving, setSaving] = useState(false);

  const first = !user.hasPassword;
  const rules = first ? RULES : { ...CURRENT, ...RULES };
  const form = useForm(EMPTY, rules);

  const submit = form.handleSubmit(async (values) => {
    setSaving(true);
    try {
      await settingsApi.changePassword({
        currentPassword: first ? null : values.currentPassword,
        newPassword: values.newPassword,
      });
      form.setValues(EMPTY);
      if (first) await refresh();
      toast.success(first ? 'Mot de passe défini.' : 'Mot de passe modifié.');
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
          {first ? 'Définir un mot de passe' : 'Mot de passe'}
          {!first && (
            <InfoHint label="Pourquoi le mot de passe actuel">
              Le mot de passe actuel est demandé pour qu'une session laissée ouverte ne suffise pas à le changer.
            </InfoHint>
          )}
        </h2>
      </div>
      <div className="card__body">
        <FormErrorSummary errors={form.currentErrors()} rules={rules} />

        {first ? (
          <p className="settings__note">
            Ce compte se connecte avec Google. Un mot de passe est une seconde façon d'y accéder.
          </p>
        ) : (
          <PasswordField label="Mot de passe actuel" autoComplete="current-password"
            {...form.field('currentPassword')} />
        )}

        <div className="settings__grid">
          <PasswordField label={first ? 'Mot de passe' : 'Nouveau mot de passe'}
            autoComplete="new-password"
            rulesId="new-password-rules" {...form.field('newPassword')} />
          <PasswordField label={first ? 'Confirmer le mot de passe' : 'Confirmer le nouveau mot de passe'}
            autoComplete="new-password" {...form.field('confirmPassword')} />
        </div>

        <PasswordRules id="new-password-rules" value={form.values.newPassword} context={form.values} />

        <div className="settings__actions">
          <Button type="submit" loading={saving}>
            {first ? 'Définir le mot de passe' : 'Modifier le mot de passe'}
          </Button>
        </div>
      </div>
    </form>
  );
}
