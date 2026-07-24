import { api } from './client.js';

export const scheduleApi = {
  day: (date) => api.get(`/schedule/day?date=${date}`),
};
