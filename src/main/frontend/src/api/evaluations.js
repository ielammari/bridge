import { api } from './client.js';

export const evaluationsApi = {
  pendingTechnical: () => api.get('/evaluations/technical/pending'),
  technicalContext: (applicationId) => api.get(`/evaluations/technical/${applicationId}`),
  submitTechnical: (applicationId, payload) => api.post(`/evaluations/technical/${applicationId}`, payload),
};
