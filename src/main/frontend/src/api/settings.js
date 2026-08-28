import { api } from './client.js';

export const settingsApi = {
  account: () => api.get('/settings/account'),
  updateAccount: (payload) => api.put('/settings/account', payload),
  changePassword: (payload) => api.post('/settings/password', payload),
  linkGoogle: (idToken) => api.put('/settings/google', { idToken }),
  unlinkGoogle: () => api.delete('/settings/google'),
  notifications: () => api.get('/settings/notifications'),
  silence: (types) => api.put('/settings/notifications', types),
  organisation: () => api.get('/settings/organisation'),
  updateOrganisation: (payload) => api.put('/settings/organisation', payload),
  provision: (payload) => api.post('/settings/accounts', payload),
};
