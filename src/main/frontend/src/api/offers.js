import { api } from './client.js';

export const offersApi = {
  feed: () => api.get('/offers/feed'),
  list: () => api.get('/offers'),
  get: (id) => api.get(`/offers/${id}`),
  // Readable by every role, unlike the rest of this module.
  detail: (id) => api.get(`/offers/${id}/detail`),
  create: (payload) => api.post('/offers', payload),
  update: (id, payload) => api.put(`/offers/${id}`, payload),
  publish: (id) => api.post(`/offers/${id}/publish`),
  close: (id) => api.post(`/offers/${id}/close`),
};
