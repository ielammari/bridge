import { api } from './client.js';

export const applicationsApi = {
  apply: (offerId) => api.post('/applications', { offerId }),
  mine: () => api.get('/applications/mine'),
  forOffer: (offerId) => api.get(`/applications?offerId=${offerId}`),
  cv: (id) => api.getBlob(`/applications/${id}/cv`),
};
