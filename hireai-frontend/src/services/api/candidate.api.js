import { apiClient } from './apiClient';

export const candidateApi = {
  /** Get authenticated candidate's profile */
  getMyProfile: async () => {
    return apiClient.get('/candidate/profile');
  },

  /** Create candidate profile */
  createProfile: async (profileData) => {
    return apiClient.post('/candidate/profile', profileData);
  },

  /** Update candidate profile */
  updateProfile: async (profileData) => {
    return apiClient.put('/candidate/profile', profileData);
  },

  /** Upload candidate profile photo */
  uploadPhoto: async (file) => {
    const formData = new FormData();
    formData.append('file', file);
    return apiClient.upload('/candidate/profile/photo', formData);
  },

  /** Delete candidate profile photo */
  deletePhoto: async () => {
    return apiClient.delete('/candidate/profile/photo');
  },

  /** Upload resume and trigger AI parsing */
  uploadResume: async (file) => {
    const formData = new FormData();
    formData.append('file', file);
    return apiClient.upload('/candidate/resume/upload', formData);
  },

  /** Get parsed resume metadata */
  getMyResume: async () => {
    return apiClient.get('/candidate/resume');
  },

  /** Download candidate's own resume binary file */
  downloadResume: async () => {
    return apiClient.get('/candidate/resume/download');
  },

  /** Delete resume */
  deleteResume: async () => {
    return apiClient.delete('/candidate/resume');
  },

  /** Send chat message to AI Pre-Screening Bot */
  sendAiChatMessage: async (message) => {
    return apiClient.post('/candidate/chat/message', { message });
  },

  /** Get AI Chat history */
  getAiChatHistory: async () => {
    return apiClient.get('/candidate/chat/history');
  },

  /** Reset AI Chat conversation session */
  resetAiChat: async () => {
    return apiClient.post('/candidate/chat/reset');
  },

  /** Get AI interview screening questions for a job application */
  getInterviewQuestions: async (applicationId) => {
    return apiClient.get(`/candidate/applications/${applicationId}/interview`);
  },

  /** Submit AI interview answers for automated evaluation */
  submitInterviewAnswers: async (applicationId, answers) => {
    return apiClient.post(`/candidate/applications/${applicationId}/interview/submit`, {
      answers, // [ { questionId, answerText } ]
    });
  },
};
