import { apiClient } from './apiClient';

export const recruiterApi = {
  /** Get current authenticated recruiter profile */
  getMyProfile: async () => {
    return apiClient.get('/recruiter/profile');
  },

  /** Create recruiter profile */
  createProfile: async (profileData) => {
    return apiClient.post('/recruiter/profile', profileData);
  },

  /** Update recruiter profile */
  updateProfile: async (profileData) => {
    return apiClient.put('/recruiter/profile', profileData);
  },

  /** Upload company logo or recruiter photo */
  uploadPhoto: async (file) => {
    const formData = new FormData();
    formData.append('file', file);
    return apiClient.upload('/recruiter/profile/photo', formData);
  },

  /** Delete recruiter photo */
  deletePhoto: async () => {
    return apiClient.delete('/recruiter/profile/photo');
  },

  /** Get recruiter profile by user ID */
  getProfileByUserId: async (userId) => {
    return apiClient.get(`/recruiter/profile/${userId}`);
  },
};
