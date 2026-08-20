import { api } from './client.js';

export const evaluationsApi = {
  pendingTechnical: () => api.get('/evaluations/technical/pending'),
  technicalContext: (applicationId) => api.get(`/evaluations/technical/${applicationId}`),
  cv: (applicationId) => api.getBlob(`/evaluations/technical/${applicationId}/cv`),
  submitTechnical: (applicationId, payload) => api.post(`/evaluations/technical/${applicationId}`, payload),
};
