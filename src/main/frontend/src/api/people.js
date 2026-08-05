import { api } from './client.js';

export const peopleApi = {
  dossier: (id) => api.get(`/people/${id}`),
  cv: (id) => api.getBlob(`/people/${id}/cv`),
};
