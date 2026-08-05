// What each role keeps a record of. The sidebar carries one History entry; this
// decides what sits behind it.
const BY_ROLE = {
  CANDIDAT: [{ to: '/historique/candidatures', label: 'Mes candidatures' }],
  RH: [
    { to: '/historique/candidatures', label: 'Candidatures' },
    { to: '/historique/offres', label: 'Offres clôturées' },
    { to: '/historique/embauches', label: 'Embauches' },
    { to: '/historique/evaluations', label: 'Mes évaluations' },
  ],
  EXPERT: [{ to: '/historique/evaluations', label: 'Mes évaluations' }],
};

export const historyTabs = (role) => BY_ROLE[role] ?? [];

export const historyHome = (role) => historyTabs(role)[0]?.to ?? '/historique/candidatures';
