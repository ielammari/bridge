import { useState } from 'react';
import { settingsApi } from '../../api/settings.js';
import Button from '../../components/Button/Button.jsx';
import Field from '../../components/Field/Field.jsx';
import FormErrorSummary from '../../components/FormErrorSummary/FormErrorSummary.jsx';
import InfoHint from '../../components/InfoHint/InfoHint.jsx';
import PasswordField from '../../components/PasswordField/PasswordField.jsx';
import PasswordRules from '../../components/PasswordField/PasswordRules.jsx';
import Select from '../../components/Select/Select.jsx';
import { useToast } from '../../components/Toast/ToastContext.jsx';
import { ROLE_OPTIONS } from '../../constants/enums.js';
import { passwordProblem } from '../../constants/password.js';
import { emailFormat } from '../../constants/validation.js';
import useForm from '../../hooks/useForm.js';

const EMPTY = { firstName: '', lastName: '', email: '', role: '', password: '' };

const RULES = {
  firstName: { label: 'Prénom', required: 'Indiquez le prénom.' },
  lastName: { label: 'Nom', required: 'Indiquez le nom.' },
  email: { label: 'Adresse email', required: 'Indiquez l\'adresse email.', format: emailFormat },
  role: { label: 'Rôle', required: 'Choisissez un rôle.' },
  password: {
    label: 'Mot de passe initial',
    required: 'Choisissez un mot de passe initial.',
    format: (value, values) => passwordProblem(value, values),
  },
};

/** Creating the accounts public signup cannot produce: HR and technical expert. */
export default function ProvisionSection() {
  const toast = useToast();
  const [saving, setSaving] = useState(false);
  const form = useForm(EMPTY, RULES);

  const submit = form.handleSubmit(async (values) => {
    setSaving(true);
    try {
      const created = await settingsApi.provision({
        firstName: values.firstName.trim(),
        lastName: values.lastName.trim(),
        email: values.email.trim(),
        role: values.role,
        password: values.password,
      });
      form.setValues(EMPTY);
      toast.success(`Compte créé pour ${created.firstName} ${created.lastName}.`);
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
          Créer un compte
          <InfoHint label="À propos des comptes">
            L'inscription publique ne crée que des comptes candidat. Les comptes RH et expert se créent ici.
          </InfoHint>
        </h2>
      </div>
      <div className="card__body">
        <FormErrorSummary errors={form.currentErrors()} rules={RULES} />

        <div className="settings__grid">
          <Field label="Prénom" {...form.field('firstName')} />
          <Field label="Nom" {...form.field('lastName')} />
          <Field label="Adresse email" type="email" {...form.field('email')} />
          <Select label="Rôle" options={ROLE_OPTIONS} placeholder="Choisir" {...form.field('role')} />
        </div>

        <PasswordField label="Mot de passe initial" autoComplete="new-password"
          rulesId="provision-rules" {...form.field('password')} />
        <PasswordRules id="provision-rules" value={form.values.password} context={form.values} />

        <div className="settings__actions">
          <Button type="submit" loading={saving}>Créer le compte</Button>
        </div>
      </div>
    </form>
  );
}
