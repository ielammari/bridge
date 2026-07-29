import { api } from './client.js';

export const messagesApi = {
  inbox: () => api.get('/messages'),
  unreadCount: () => api.get('/messages/unread-count'),
  markRead: (id) => api.post(`/messages/${id}/read`),
  markAllRead: () => api.post('/messages/read-all'),
};
