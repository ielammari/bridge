import { api } from './client.js';

export const calendarApi = {
  /**
   * The interviews between two days. `scope` is what the caller is asking for:
   * their own calendar, the exams they arranged, or a named evaluator's.
   */
  range: (from, to, { scope = 'MINE', evaluatorId } = {}) =>
    api.get(`/calendar?from=${from}&to=${to}&scope=${scope}`
      + (evaluatorId ? `&evaluatorId=${evaluatorId}` : '')),
};
