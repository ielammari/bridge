import { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import AuthLayout from './AuthLayout.jsx';
import Alert from '../../components/Alert/Alert.jsx';
import Button from '../../components/Button/Button.jsx';
import Field from '../../components/Field/Field.jsx';
import FormErrorSummary from '../../components/FormErrorSummary/FormErrorSummary.jsx';
import PasswordField from '../../components/PasswordField/PasswordField.jsx';
import { HOME_BY_ROLE } from '../../components/ProtectedRoute/ProtectedRoute.jsx';
import { emailFormat } from '../../constants/validation.js';
import { useAuth } from '../../context/AuthContext.jsx';
import useDocumentTitle from '../../hooks/useDocumentTitle.js';
import useForm from '../../hooks/useForm.js';

const RULES = {
  email: {
    label: 'Adresse email',
    required: 'Indiquez votre adresse email.',
    format: emailFormat,
  },
  password: {
    label: 'Mot de passe',
    required: 'Indiquez votre mot de passe.',
  },
};

export default function Login() {
  useDocumentTitle('Se connecter');

  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const form = useForm({ email: '', password: '' }, RULES);
  const [failure, setFailure] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  // Set when a session expired mid use, to explain the redirect.
  const expired = location.state?.expired;

  const submit = form.handleSubmit(async (values) => {
    setFailure(null);
    setSubmitting(true);
    try {
      const user = await login({ email: values.email.trim(), password: values.password });
      const target = location.state?.from?.pathname ?? HOME_BY_ROLE[user.role] ?? '/';
      navigate(target, { replace: true });
    } catch (apiError) {
      setFailure(apiError.message);
      setSubmitting(false);
      document.querySelector('[name="password"]')?.focus();
    }
  });

  return (
    <AuthLayout
      title="Se connecter"
      footer={
        <>
          Pas encore de compte ? <Link to="/inscription">Créer un compte candidat</Link>
        </>
      }
    >
      <form className="auth__form" onSubmit={submit} noValidate>
        {expired && <Alert tone="info">Votre session a expiré. Reconnectez-vous pour continuer.</Alert>}
        {failure && <Alert>{failure}</Alert>}
        <FormErrorSummary errors={form.currentErrors()} rules={RULES} />

        <Field label="Adresse email" type="email" autoComplete="email" {...form.field('email')} />

        <PasswordField
          label="Mot de passe"
          autoComplete="current-password"
          {...form.field('password')}
        />

        <Button type="submit" fullWidth loading={submitting}>
          Se connecter
        </Button>
      </form>
    </AuthLayout>
  );
}
