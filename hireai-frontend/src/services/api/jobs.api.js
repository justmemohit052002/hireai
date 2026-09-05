import { apiClient } from './apiClient';

export const jobsApi = {
  /** Browse open jobs (for Candidates & general discovery) */
  getOpenJobs: async () => {
    return apiClient.get('/jobs/open');
  },

  /** Get all job postings created by authenticated recruiter */
  getMyJobs: async () => {
    return apiClient.get('/jobs');
  },

  /** Get single job by ID */
  getJob: async (id) => {
    return apiClient.get(`/jobs/${id}`);
  },

  /** Create a new job posting (Recruiter) */
  createJob: async (jobData) => {
    // Format job data to match Spring Boot JobRequest
    const payload = {
      title: jobData.title,
      description: jobData.description,
      companyName: jobData.companyName || jobData.company?.name,
      department: jobData.department || 'Engineering',
      location: jobData.location,
      jobType: (jobData.jobType || jobData.type || 'FULL_TIME').toUpperCase().replace('-', '_'),
      workplaceType: (jobData.workplaceType || 'HYBRID').toUpperCase().replace('-', '_'),
      experienceLevel: (jobData.experienceLevel || jobData.level || 'MID').toUpperCase().replace('-', '_'),
      minSalary: Number(jobData.minSalary || jobData.salary?.min || 0),
      maxSalary: Number(jobData.maxSalary || jobData.salary?.max || 0),
      currency: (jobData.currency || jobData.salary?.currency || 'USD').toUpperCase(),
      skills: Array.isArray(jobData.skills)
        ? jobData.skills
        : typeof jobData.skills === 'string'
        ? jobData.skills.split(',').map((s) => s.trim()).filter(Boolean)
        : [],
      educationRequirements: Array.isArray(jobData.educationRequirements)
        ? jobData.educationRequirements
        : jobData.educationRequirements
        ? [jobData.educationRequirements]
        : [],
      benefits: Array.isArray(jobData.benefits)
        ? jobData.benefits
        : jobData.benefits
        ? [jobData.benefits]
        : [],
      applicationDeadline: jobData.applicationDeadline || null,
    };

    return apiClient.post('/jobs', payload);
  },

  /** Update an existing job posting (Recruiter) */
  updateJob: async (id, jobData) => {
    const payload = {
      title: jobData.title,
      description: jobData.description,
      companyName: jobData.companyName || jobData.company?.name,
      department: jobData.department,
      location: jobData.location,
      jobType: (jobData.jobType || jobData.type || 'FULL_TIME').toUpperCase().replace('-', '_'),
      workplaceType: (jobData.workplaceType || 'HYBRID').toUpperCase().replace('-', '_'),
      experienceLevel: (jobData.experienceLevel || jobData.level || 'MID').toUpperCase().replace('-', '_'),
      minSalary: Number(jobData.minSalary || jobData.salary?.min || 0),
      maxSalary: Number(jobData.maxSalary || jobData.salary?.max || 0),
      currency: (jobData.currency || jobData.salary?.currency || 'USD').toUpperCase(),
      skills: Array.isArray(jobData.skills)
        ? jobData.skills
        : typeof jobData.skills === 'string'
        ? jobData.skills.split(',').map((s) => s.trim()).filter(Boolean)
        : [],
      educationRequirements: Array.isArray(jobData.educationRequirements)
        ? jobData.educationRequirements
        : [],
      benefits: Array.isArray(jobData.benefits) ? jobData.benefits : [],
      applicationDeadline: jobData.applicationDeadline || null,
    };

    return apiClient.put(`/jobs/${id}`, payload);
  },

  /** Close a job posting so candidates can no longer apply */
  closeJob: async (id) => {
    return apiClient.patch(`/jobs/${id}/close`);
  },

  /** AI-powered Job Description and Interview Questions generator */
  generateAiJd: async ({ jobTitle, requiredSkills, experienceLevel }) => {
    const skillsList = Array.isArray(requiredSkills)
      ? requiredSkills
      : typeof requiredSkills === 'string'
      ? requiredSkills.split(',').map((s) => s.trim()).filter(Boolean)
      : [];

    return apiClient.post('/recruiter/ai/jd/generate', {
      jobTitle: jobTitle,
      requiredSkills: skillsList,
      experienceLevel: experienceLevel,
      job_title: jobTitle,
      required_skills: skillsList,
      experience_level: experienceLevel,
    });
  },
};
