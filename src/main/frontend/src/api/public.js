import { api } from './client.js';

// The open positions, at the one address that answers without an account.
export const publicApi = {
  market: () => api.get('/public/offers'),
  offer: (id) => api.get(`/public/offers/${id}`),
};
