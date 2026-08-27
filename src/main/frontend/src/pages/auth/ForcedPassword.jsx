import { useState } from 'react';
import { Navigate, useNavigate } from 'react-router-dom';
import { settingsApi } from '../../api/settings.js';
import AuthLayout from './AuthLayout.jsx';
import Alert from '../../components/Alert/Alert.jsx';
import Button from '../../components/Button/Button.jsx';
import FormErrorSummary from '../../components/FormErrorSummary/FormErrorSummary.jsx';
import PasswordField from '../../components/PasswordField/PasswordField.jsx';
import PasswordRules from '../../components/PasswordField/PasswordRules.jsx';
import { HOME_BY_ROLE } from '../../components/ProtectedRoute/ProtectedRoute.jsx';
import { passwordProblem } from '../../constants/password.js';
import { useAuth } from '../../context/AuthContext.jsx';
import useDocumentTitle from '../../hooks/useDocumentTitle.js';
import useForm from '../../hooks/useForm.js';

const EMPTY = { currentPassword: '', newPassword: '', confirmPassword: '' };

const RULES = {
  currentPassword: {
    label: 'Mot de passe reçu',
    required: 'Saisissez le mot de passe qui vous a été communiqué.',
  },
  newPassword: {
    label: 'Nouveau mot de passe',
    required: 'Choisissez votre mot de passe.',
    format: (value, values) =>
      (value === values.currentPassword
        ? 'Choisissez un mot de passe différent de celui qui vous a été communiqué.'
        : passwordProblem(value, values)),
  },
  confirmPassword: {
    label: 'Confirmation',
    required: 'Saisissez à nouveau votre mot de passe.',
    format: (value, values) =>
      (value === values.newPassword ? null : 'Les deux mots de passe ne sont pas identiques.'),
  },
};

/**
 * Replacing the password somebody else chose for a provisioned account.
 * Nothing else in the application answers until it is done.
 */
export default function ForcedPassword() {
  useDocumentTitle('Choisir un mot de passe');

  const { user, refresh, logout } = useAuth();
  const navigate = useNavigate();
  const form = useForm(EMPTY, RULES);
  const [failure, setFailure] = useState(null);
  const [saving, setSaving] = useState(false);

  // Reached by its own address by somebody who owes no change.
  if (!user.mustChangePassword) {
    return <Navigate to={HOME_BY_ROLE[user.role] ?? '/'} replace />;
  }

  const submit = form.handleSubmit(async (values) => {
    setFailure(null);
    setSaving(true);
    try {
      await settingsApi.changePassword({
        currentPassword: values.currentPassword,
        newPassword: values.newPassword,
      });
      const updated = await refresh();
      navigate(HOME_BY_ROLE[updated.role] ?? '/', { replace: true });
    } catch (apiError) {
      setFailure(apiError.message);
      setSaving(false);
    }
  });

  return (
    <AuthLayout
      title="Choisir un mot de passe"
      intro={`Ce compte a été créé pour vous, ${user.firstName}. Remplacez le mot de passe qui vous a été communiqué par le vôtre pour accéder à l'application.`}
      footer={
        <>
          Ce n'est pas votre compte ?{' '}
          <Button variant="text" onClick={logout}>Se déconnecter</Button>
        </>
      }
    >
      <form className="auth__form" onSubmit={submit} noValidate>
        {failure && <Alert>{failure}</Alert>}
        <FormErrorSummary errors={form.currentErrors()} rules={RULES} />

        <PasswordField label="Mot de passe reçu" autoComplete="current-password"
          {...form.field('currentPassword')} />

        <PasswordField label="Nouveau mot de passe" autoComplete="new-password"
          rulesId="forced-password-rules" {...form.field('newPassword')} />
        <PasswordRules id="forced-password-rules" value={form.values.newPassword}
          context={form.values} />

        <PasswordField label="Confirmer le nouveau mot de passe" autoComplete="new-password"
          {...form.field('confirmPassword')} />

        <Button type="submit" fullWidth loading={saving}>
          Enregistrer et continuer
        </Button>
      </form>
    </AuthLayout>
  );
}
