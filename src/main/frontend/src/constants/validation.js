// Format rules shared by the forms. Each returns a message or null, which is
// the shape useForm expects for a `format` rule.

const EMAIL = /^[^\s@]+@[^\s@]+\.[a-z]{2,}$/i;
const PHONE = /^[0-9+ .-]{6,20}$/;

export function emailFormat(value) {
  return EMAIL.test(value.trim())
    ? null
    : 'Cette adresse email n\'est pas valide. Exemple : nom@domaine.fr';
}

export function phoneFormat(value) {
  return PHONE.test(value.trim())
    ? null
    : 'Ce numéro n\'est pas valide. Utilisez des chiffres, espaces, points ou tirets.';
}

export function positiveNumber(value) {
  return Number(value) > 0 ? null : 'Indiquez un montant supérieur à zéro.';
}

const MIN_AGE = 18;
const MAX_AGE = 100;

/** Mirrors the age range AuthService enforces. */
export function birthDateProblem(value) {
  const birth = new Date(`${value}T00:00:00`);
  if (Number.isNaN(birth.getTime())) return 'Cette date n\'est pas valide.';

  const today = new Date();
  if (birth > today) return 'La date de naissance doit être dans le passé.';

  let age = today.getFullYear() - birth.getFullYear();
  const beforeBirthday =
    today.getMonth() < birth.getMonth()
    || (today.getMonth() === birth.getMonth() && today.getDate() < birth.getDate());
  if (beforeBirthday) age -= 1;

  if (age < MIN_AGE) return `Vous devez avoir au moins ${MIN_AGE} ans pour créer un compte.`;
  if (age > MAX_AGE) return 'Vérifiez l\'année saisie, elle semble incorrecte.';
  return null;
}
