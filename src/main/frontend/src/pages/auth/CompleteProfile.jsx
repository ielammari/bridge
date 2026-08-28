import { useState } from 'react';
import { Navigate, useNavigate } from 'react-router-dom';
import { authApi } from '../../api/auth.js';
import AuthLayout from './AuthLayout.jsx';
import Alert from '../../components/Alert/Alert.jsx';
import Button from '../../components/Button/Button.jsx';
import Field from '../../components/Field/Field.jsx';
import FormErrorSummary from '../../components/FormErrorSummary/FormErrorSummary.jsx';
import Select from '../../components/Select/Select.jsx';
import { HOME_BY_ROLE } from '../../components/ProtectedRoute/ProtectedRoute.jsx';
import { GENDER_OPTIONS } from '../../constants/enums.js';
import { localDate } from '../../constants/format.js';
import { birthDateProblem, phoneFormat } from '../../constants/validation.js';
import { useAuth } from '../../context/AuthContext.jsx';
import useDocumentTitle from '../../hooks/useDocumentTitle.js';
import useForm from '../../hooks/useForm.js';

const RULES = {
  firstName: { label: 'Prénom', required: 'Indiquez votre prénom.' },
  lastName: { label: 'Nom', required: 'Indiquez votre nom.' },
  birthDate: {
    label: 'Date de naissance',
    required: 'Indiquez votre date de naissance.',
    format: birthDateProblem,
  },
  gender: { label: 'Sexe' },
  phone: { label: 'Téléphone', format: phoneFormat },
  city: { label: 'Ville' },
  country: { label: 'Pays' },
};

/**
 * The details a Google signup could not supply. Nothing else in the
 * application answers until they are given.
 */
export default function CompleteProfile() {
  useDocumentTitle('Compléter votre profil');

  const { user, refresh, logout } = useAuth();
  const navigate = useNavigate();

  const form = useForm({
    firstName: user.firstName ?? '',
    lastName: user.lastName ?? '',
    birthDate: '',
    gender: '',
    phone: '',
    city: '',
    country: '',
  }, RULES);

  const [failure, setFailure] = useState(null);
  const [saving, setSaving] = useState(false);

  // Reached by its own address by somebody whose profile is already complete.
  if (!user.mustCompleteProfile) {
    return <Navigate to={HOME_BY_ROLE[user.role] ?? '/'} replace />;
  }

  const submit = form.handleSubmit(async (values) => {
    setFailure(null);
    setSaving(true);
    try {
      await authApi.complete({
        firstName: values.firstName.trim(),
        lastName: values.lastName.trim(),
        birthDate: values.birthDate,
        gender: values.gender || null,
        phone: values.phone.trim(),
        city: values.city.trim(),
        country: values.country.trim(),
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
      wide
      title="Compléter votre profil"
      intro={`Bienvenue ${user.firstName}. Google ne transmet pas votre date de naissance, nécessaire pour accéder à l'application.`}
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

        <div className="auth__grid">
          <Field label="Prénom" autoComplete="given-name" {...form.field('firstName')} />
          <Field label="Nom" autoComplete="family-name" {...form.field('lastName')} />

          <Field label="Date de naissance" type="date" max={localDate()} autoComplete="bday"
            {...form.field('birthDate')} />
          <Select label="Sexe" options={GENDER_OPTIONS} placeholder="Ne pas préciser"
            hint="Facultatif" {...form.field('gender')} />

          <Field label="Téléphone" type="tel" autoComplete="tel" hint="Facultatif"
            {...form.field('phone')} />
          <Field label="Ville" autoComplete="address-level2" hint="Facultatif"
            {...form.field('city')} />

          <Field label="Pays" autoComplete="country-name" hint="Facultatif"
            {...form.field('country')} />
        </div>

        <Button type="submit" fullWidth loading={saving}>
          Enregistrer et continuer
        </Button>
      </form>
    </AuthLayout>
  );
}
