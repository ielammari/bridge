import { api } from './client.js';

export const profileApi = {
  read: () => api.get('/profile'),
  update: (payload) => api.put('/profile', payload),
  uploadCv: (file) => {
    const form = new FormData();
    form.append('file', file);
    return api.post('/profile/cv', form);
  },
  downloadCv: () => api.getBlob('/profile/cv'),
};
