import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import AuthLayout from './AuthLayout.jsx';
import Alert from '../../components/Alert/Alert.jsx';
import Button from '../../components/Button/Button.jsx';
import Field from '../../components/Field/Field.jsx';
import { HOME_BY_ROLE } from '../../components/ProtectedRoute/ProtectedRoute.jsx';
import { useAuth } from '../../context/AuthContext.jsx';

const EMPTY = { firstName: '', lastName: '', email: '', phone: '', password: '' };

function validate(values) {
  const errors = {};

  if (!values.firstName.trim()) errors.firstName = 'Indiquez votre prénom.';
  if (!values.lastName.trim()) errors.lastName = 'Indiquez votre nom.';

  if (!values.email.trim()) {
    errors.email = 'Indiquez votre adresse email.';
  } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(values.email)) {
    errors.email = 'Cette adresse email n\'est pas valide. Exemple : nom@domaine.fr';
  }

  if (values.phone && !/^[0-9+ .-]{6,20}$/.test(values.phone)) {
    errors.phone = 'Ce numéro n\'est pas valide. Utilisez uniquement des chiffres, espaces, points ou tirets.';
  }

  if (!values.password) {
    errors.password = 'Choisissez un mot de passe.';
  } else if (values.password.length < 8) {
    errors.password = 'Le mot de passe doit contenir au moins 8 caractères.';
  }

  return errors;
}

export default function Signup() {
  const { register } = useAuth();
  const navigate = useNavigate();

  const [form, setForm] = useState(EMPTY);
  const [touched, setTouched] = useState({});
  const [errors, setErrors] = useState({});
  const [failure, setFailure] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  const update = (key) => (event) => {
    const next = { ...form, [key]: event.target.value };
    setForm(next);
    // Once a field has been corrected, clear its error as the user types.
    if (errors[key]) setErrors(validate(next));
  };

  // Validating on blur rather than on every keystroke avoids scolding someone
  // for an incomplete email they are still typing.
  const handleBlur = (key) => () => {
    setTouched({ ...touched, [key]: true });
    setErrors(validate(form));
  };

  async function handleSubmit(event) {
    event.preventDefault();
    const found = validate(form);
    setErrors(found);
    setTouched({ firstName: true, lastName: true, email: true, phone: true, password: true });

    if (Object.keys(found).length > 0) return;

    setFailure(null);
    setSubmitting(true);

    try {
      const user = await register(form);
      navigate(HOME_BY_ROLE[user.role] ?? '/', { replace: true });
    } catch (apiError) {
      setFailure(apiError.message);
      setSubmitting(false);
    }
  }

  const errorFor = (key) => (touched[key] ? errors[key] : undefined);

  return (
    <AuthLayout
      title="Créer un compte"
      intro="Renseignez vos informations pour postuler aux offres qui correspondent à votre profil."
      footer={
        <>
          Vous avez déjà un compte ? <Link to="/connexion">Se connecter</Link>
        </>
      }
    >
      <form className="auth__form" onSubmit={handleSubmit} noValidate>
        {failure && <Alert>{failure}</Alert>}

        <Field
          label="Prénom"
          value={form.firstName}
          onChange={update('firstName')}
          onBlur={handleBlur('firstName')}
          error={errorFor('firstName')}
          autoComplete="given-name"
          required
        />

        <Field
            label="Nom"
            value={form.lastName}
            onChange={update('lastName')}
            onBlur={handleBlur('lastName')}
            error={errorFor('lastName')}
            autoComplete="family-name"
            required
        />

        <Field
          label="Adresse email"
          type="email"
          value={form.email}
          onChange={update('email')}
          onBlur={handleBlur('email')}
          error={errorFor('email')}
          autoComplete="email"
          required
        />

        <Field
          label="Téléphone"
          type="tel"
          value={form.phone}
          onChange={update('phone')}
          onBlur={handleBlur('phone')}
          error={errorFor('phone')}
          hint="Facultatif."
          autoComplete="tel"
        />

        <Field
          label="Mot de passe"
          type="password"
          value={form.password}
          onChange={update('password')}
          onBlur={handleBlur('password')}
          error={errorFor('password')}
          hint="8 caractères minimum."
          autoComplete="new-password"
          required
        />

        <Button type="submit" fullWidth loading={submitting}>
          {submitting ? 'Création...' : 'Créer mon compte'}
        </Button>
      </form>
    </AuthLayout>
  );
}
