import { api } from './client.js';

// Every academic path call answers with the whole profile, so one request both
// writes the change and refreshes the page.
export const profileApi = {
  read: () => api.get('/profile'),
  update: (payload) => api.put('/profile', payload),
  addEducation: (payload) => api.post('/profile/education', payload),
  updateEducation: (id, payload) => api.put(`/profile/education/${id}`, payload),
  removeEducation: (id) => api.delete(`/profile/education/${id}`),
  uploadCv: (file) => {
    const form = new FormData();
    form.append('file', file);
    return api.post('/profile/cv', form);
  },
  downloadCv: () => api.getBlob('/profile/cv'),
};
