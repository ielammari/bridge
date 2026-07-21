import { api } from './client.js';

export const traitsApi = {
  catalogue: () => api.get('/traits'),
};
