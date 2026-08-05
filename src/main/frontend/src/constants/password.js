// The password policy. Drives the checklist shown while choosing; PasswordPolicy
// on the server holds the same rules and enforces them.

const fold = (text) =>
  text.normalize('NFD').replace(/\p{Diacritic}/gu, '').toLowerCase();

// Fragments shorter than four characters match too much to be worth checking.
function personalFragments({ firstName = '', lastName = '', email = '' }) {
  return [firstName, lastName, email.split('@')[0] ?? '']
    .map((part) => fold(part.trim()))
    .filter((part) => part.length >= 4);
}

export const PASSWORD_RULES = [
  {
    id: 'length',
    label: '12 caractères au minimum',
    test: (password) => password.length >= 12,
  },
  {
    id: 'lower',
    label: 'Une lettre minuscule',
    test: (password) => /\p{Ll}/u.test(password),
  },
  {
    id: 'upper',
    label: 'Une lettre majuscule',
    test: (password) => /\p{Lu}/u.test(password),
  },
  {
    id: 'digit',
    label: 'Un chiffre',
    test: (password) => /\p{Nd}/u.test(password),
  },
  {
    id: 'symbol',
    label: 'Un caractère spécial, par exemple ! ? @ - _ .',
    test: (password) => /[^\p{L}\p{Nd}]/u.test(password),
  },
  {
    id: 'repeat',
    label: 'Jamais trois fois le même caractère de suite',
    test: (password) => !/(.)\1\1/u.test(password),
  },
  {
    id: 'personal',
    label: 'Ne reprend ni votre nom ni votre adresse email',
    test: (password, context = {}) => {
      const folded = fold(password);
      return !personalFragments(context).some((part) => folded.includes(part));
    },
  },
];

// One message for the field; the checklist says which rule failed.
export function passwordProblem(password, context) {
  return PASSWORD_RULES.some((rule) => !rule.test(password ?? '', context))
    ? 'Ce mot de passe ne remplit pas toutes les conditions ci-dessous.'
    : null;
}
