import { api } from './client.js';

export const authApi = {
  register: (payload) => api.post('/auth/register', payload),
  login: (credentials) => api.post('/auth/login', credentials),
  google: (idToken) => api.post('/auth/google', { idToken }),
  providers: () => api.get('/auth/providers'),
  complete: (payload) => api.post('/auth/complete', payload),
  me: () => api.get('/auth/me'),
};
