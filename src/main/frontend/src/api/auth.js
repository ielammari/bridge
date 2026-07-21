import { api } from './client.js';

export const authApi = {
  register: (payload) => api.post('/auth/register', payload),
  login: (credentials) => api.post('/auth/login', credentials),
  me: () => api.get('/auth/me'),
};
