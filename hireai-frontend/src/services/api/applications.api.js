import { apiClient } from './apiClient';

export const applicationsApi = {
  /**
   * Apply to a job posting (Candidate)
   * Supports both multipart (with fresh resume file) and JSON (using existing profile resume).
   */
  applyToJob: async (jobId, { coverNote = '', file = null } = {}) => {
    const rawFile = file?.raw || (file instanceof File ? file : null);
    if (rawFile) {
      const formData = new FormData();
      formData.append('file', rawFile);
      if (coverNote) {
        formData.append('coverNote', coverNote);
      }
      return apiClient.upload(`/jobs/${jobId}/apply`, formData);
    } else {
      return apiClient.post(`/jobs/${jobId}/apply`, { coverNote });
    }
  },

  /** Get all applications submitted by the logged-in candidate */
  getMyApplications: async () => {
    return apiClient.get('/candidate/applications');
  },

  /** Get all applicants for a job posting owned by recruiter (Ranked by ATS match score) */
  getJobApplications: async (jobId) => {
    return apiClient.get(`/jobs/${jobId}/applications`);
  },

  /** Update applicant recruitment stage (Recruiter) */
  updateApplicationStatus: async (applicationId, { status, feedbackNotes = '' }) => {
    return apiClient.patch(`/applications/${applicationId}/status`, {
      status, // 'SCREENING' | 'SHORTLISTED' | 'INTERVIEW_SCHEDULED' | 'OFFERED' | 'REJECTED'
      feedbackNotes,
    });
  },

  /** Get details for a specific application */
  getApplicationById: async (applicationId) => {
    return apiClient.get(`/applications/${applicationId}`);
  },

  /** Download applicant resume binary via authenticated request */
  downloadApplicationResume: async (applicationId) => {
    return apiClient.get(`/applications/${applicationId}/resume/download`);
  },

  /** Download candidate resume binary by candidateId (Recruiter/Admin) */
  downloadCandidateResume: async (candidateId) => {
    return apiClient.get(`/candidates/${candidateId}/resume/download`);
  },

  /** Download applicant resume binary URL */
  getResumeDownloadUrl: (applicationId) => {
    return `/applications/${applicationId}/resume/download`;
  },

  /** Trigger AI Decision Engine for an application (Recruiter) */
  finalizeAiDecision: async (applicationId) => {
    return apiClient.post(`/recruiter/ai/applications/${applicationId}/decision`);
  },

  /** Compute semantic ATS match score on demand */
  computeAtsScore: async (jobId, candidateId) => {
    return apiClient.post(`/recruiter/ai/match/${jobId}/${candidateId}`);
  },
};
