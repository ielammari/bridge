import { api } from './client.js';

export const scheduleApi = {
  day: (date, evaluatorId) =>
    api.get(`/schedule/day?date=${date}${evaluatorId ? `&evaluatorId=${evaluatorId}` : ''}`),
  experts: (date) => api.get(`/schedule/experts?date=${date}`),
};
