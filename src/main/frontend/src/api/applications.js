import { api } from './client.js';

export const applicationsApi = {
  apply: (offerId) => api.post('/applications', { offerId }),
  mine: () => api.get('/applications/mine'),
  forOffer: (offerId) => api.get(`/applications?offerId=${offerId}`),
  cv: (id) => api.getBlob(`/applications/${id}/cv`),
  review: (id) => api.post(`/applications/${id}/review`),
  preselect: (id, payload) => api.post(`/applications/${id}/preselection`, payload),
  schedule: (id, payload) => api.post(`/applications/${id}/schedule`, payload),
  finalize: (id, payload) => api.post(`/applications/${id}/finalize`, payload),
};
