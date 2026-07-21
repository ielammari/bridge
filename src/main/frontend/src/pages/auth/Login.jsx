import { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import AuthLayout from './AuthLayout.jsx';
import Alert from '../../components/Alert/Alert.jsx';
import Button from '../../components/Button/Button.jsx';
import Field from '../../components/Field/Field.jsx';
import { HOME_BY_ROLE } from '../../components/ProtectedRoute/ProtectedRoute.jsx';
import { useAuth } from '../../context/AuthContext.jsx';

export default function Login() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const [form, setForm] = useState({ email: '', password: '' });
  const [error, setError] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  const update = (key) => (event) => setForm({ ...form, [key]: event.target.value });

  async function handleSubmit(event) {
    event.preventDefault();
    setError(null);
    setSubmitting(true);

    try {
      const user = await login(form);
      const target = location.state?.from?.pathname ?? HOME_BY_ROLE[user.role] ?? '/';
      navigate(target, { replace: true });
    } catch (apiError) {
      setError(apiError.message);
      setSubmitting(false);
    }
  }

  return (
    <AuthLayout
      title="Se connecter"
      intro="Accédez à votre espace pour suivre vos candidatures et vos entretiens."
      footer={
        <>
          Pas encore de compte ? <Link to="/inscription">Créer un compte candidat</Link>
        </>
      }
    >
      <form className="auth__form" onSubmit={handleSubmit} noValidate>
        {error && <Alert>{error}</Alert>}

        <Field
          label="Adresse email"
          type="email"
          value={form.email}
          onChange={update('email')}
          autoComplete="email"
          required
        />

        <Field
          label="Mot de passe"
          type="password"
          value={form.password}
          onChange={update('password')}
          autoComplete="current-password"
          required
        />

        <Button type="submit" fullWidth loading={submitting}>
          {submitting ? 'Connexion...' : 'Se connecter'}
        </Button>
      </form>
    </AuthLayout>
  );
}
