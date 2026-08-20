import { api } from './client.js';

// Every academic path call answers with the whole profile, so one request both
// writes the change and refreshes the page.
export const profileApi = {
  read: () => api.get('/profile'),
  update: (payload) => api.put('/profile', payload),
  addEducation: (payload) => api.post('/profile/education', payload),
  updateEducation: (id, payload) => api.put(`/profile/education/${id}`, payload),
  removeEducation: (id) => api.delete(`/profile/education/${id}`),
  uploadCv: (file, label) => {
    const form = new FormData();
    form.append('file', file);
    if (label) form.append('label', label);
    return api.post('/profile/cv', form);
  },
  chooseCv: (id) => api.put(`/profile/cv/${id}`),
  removeCv: (id) => api.delete(`/profile/cv/${id}`),
  downloadCv: () => api.getBlob('/profile/cv'),
};
