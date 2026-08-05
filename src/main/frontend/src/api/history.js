import { api } from './client.js';

export const historyApi = {
  myApplication: (id) => api.get(`/history/mine/${id}`),
  trail: (applicationId) => api.get(`/history/applications/${applicationId}`),
  closedApplications: () => api.get('/history/applications'),
  hirings: () => api.get('/history/hirings'),
  authoredEvaluations: () => api.get('/history/evaluations'),
};
