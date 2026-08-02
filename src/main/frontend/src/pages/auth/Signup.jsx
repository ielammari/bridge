import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import AuthLayout from './AuthLayout.jsx';
import Alert from '../../components/Alert/Alert.jsx';
import Button from '../../components/Button/Button.jsx';
import Field from '../../components/Field/Field.jsx';
import FormErrorSummary from '../../components/FormErrorSummary/FormErrorSummary.jsx';
import PasswordField from '../../components/PasswordField/PasswordField.jsx';
import PasswordRules from '../../components/PasswordField/PasswordRules.jsx';
import Select from '../../components/Select/Select.jsx';
import { HOME_BY_ROLE } from '../../components/ProtectedRoute/ProtectedRoute.jsx';
import { GENDER_OPTIONS } from '../../constants/enums.js';
import { localDate } from '../../constants/format.js';
import { passwordProblem } from '../../constants/password.js';
import { birthDateProblem, emailFormat, phoneFormat } from '../../constants/validation.js';
import { useAuth } from '../../context/AuthContext.jsx';
import useDocumentTitle from '../../hooks/useDocumentTitle.js';
import useForm from '../../hooks/useForm.js';

const EMPTY = {
  firstName: '',
  lastName: '',
  birthDate: '',
  gender: '',
  email: '',
  phone: '',
  city: '',
  country: '',
  password: '',
  passwordConfirm: '',
};

const RULES = {
  firstName: { label: 'Prénom', required: 'Indiquez votre prénom.' },
  lastName: { label: 'Nom', required: 'Indiquez votre nom.' },
  birthDate: {
    label: 'Date de naissance',
    required: 'Indiquez votre date de naissance.',
    format: birthDateProblem,
  },
  gender: { label: 'Sexe' },
  email: {
    label: 'Adresse email',
    required: 'Indiquez votre adresse email.',
    format: emailFormat,
  },
  phone: { label: 'Téléphone', format: phoneFormat },
  city: { label: 'Ville' },
  country: { label: 'Pays' },
  password: {
    label: 'Mot de passe',
    required: 'Choisissez un mot de passe.',
    format: (value, values) => passwordProblem(value, values),
  },
  passwordConfirm: {
    label: 'Confirmation du mot de passe',
    required: 'Saisissez à nouveau votre mot de passe.',
    format: (value, values) =>
      value === values.password ? null : 'Les deux mots de passe ne sont pas identiques.',
  },
};

export default function Signup() {
  useDocumentTitle('Créer un compte');

  const { register } = useAuth();
  const navigate = useNavigate();

  const form = useForm(EMPTY, RULES);
  const [failure, setFailure] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  const submit = form.handleSubmit(async (values) => {
    setFailure(null);
    setSubmitting(true);
    try {
      const user = await register({
        firstName: values.firstName.trim(),
        lastName: values.lastName.trim(),
        birthDate: values.birthDate,
        gender: values.gender || null,
        email: values.email.trim(),
        phone: values.phone.trim(),
        city: values.city.trim(),
        country: values.country.trim(),
        password: values.password,
      });
      navigate(HOME_BY_ROLE[user.role] ?? '/', { replace: true });
    } catch (apiError) {
      setFailure(apiError.message);
      setSubmitting(false);
    }
  });

  return (
    <AuthLayout
      wide
      title="Créer un compte"
      intro="Le compte créé ici est un compte candidat."
      footer={
        <>
          Vous avez déjà un compte ? <Link to="/connexion">Se connecter</Link>
        </>
      }
    >
      <form className="auth__form" onSubmit={submit} noValidate>
        {failure && <Alert>{failure}</Alert>}
        <FormErrorSummary errors={form.currentErrors()} rules={RULES} />

        <div className="auth__grid">
          <Field label="Prénom" autoComplete="given-name" {...form.field('firstName')} />
          <Field label="Nom" autoComplete="family-name" {...form.field('lastName')} />

          <Field label="Date de naissance" type="date" max={localDate()} autoComplete="bday"
            {...form.field('birthDate')} />
          <Select label="Sexe" options={GENDER_OPTIONS} placeholder="Ne pas préciser"
            hint="Facultatif." {...form.field('gender')} />

          <Field label="Adresse email" type="email" autoComplete="email" {...form.field('email')} />
          <Field label="Téléphone" type="tel" autoComplete="tel" hint="Facultatif."
            {...form.field('phone')} />

          <Field label="Ville" autoComplete="address-level2" hint="Facultatif."
            {...form.field('city')} />
          <Field label="Pays" autoComplete="country-name" hint="Facultatif."
            {...form.field('country')} />

          <PasswordField label="Mot de passe" autoComplete="new-password" rulesId="password-rules"
            {...form.field('password')} />
          <PasswordField label="Confirmer le mot de passe" autoComplete="new-password"
            {...form.field('passwordConfirm')} />

          <div className="auth__span">
            <PasswordRules id="password-rules" value={form.values.password} context={form.values} />
          </div>
        </div>

        <Button type="submit" fullWidth loading={submitting}>
          Créer mon compte
        </Button>
      </form>
    </AuthLayout>
  );
}
