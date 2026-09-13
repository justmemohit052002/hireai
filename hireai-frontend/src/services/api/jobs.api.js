import { apiClient } from './apiClient';

/**
 * Normalizes frontend job object to Spring Boot JobRequest payload format.
 */
export function formatJobPayload(jobData) {
  // Normalize employmentType
  const rawType = (jobData.employmentType || jobData.jobType || jobData.type || 'FULL_TIME')
    .toUpperCase()
    .replace(/[-\s]/g, '_');
  let employmentType = 'FULL_TIME';
  if (rawType === 'PART_TIME') employmentType = 'PART_TIME';
  else if (rawType === 'CONTRACT') employmentType = 'CONTRACT';
  else if (rawType === 'INTERNSHIP') employmentType = 'INTERNSHIP';
  else if (rawType === 'FREELANCE') employmentType = 'FREELANCE';

  // Normalize experienceLevel
  const rawLevel = (jobData.experienceLevel || jobData.level || 'MID_LEVEL')
    .toUpperCase()
    .replace(/[-\s]/g, '_');
  let experienceLevel = 'MID_LEVEL';
  if (rawLevel === 'FRESHER' || rawLevel === 'ENTRY') experienceLevel = 'FRESHER';
  else if (rawLevel === 'JUNIOR') experienceLevel = 'JUNIOR';
  else if (rawLevel === 'MID' || rawLevel === 'MID_LEVEL') experienceLevel = 'MID_LEVEL';
  else if (rawLevel === 'SENIOR') experienceLevel = 'SENIOR';
  else if (rawLevel === 'LEAD' || rawLevel === 'EXECUTIVE' || rawLevel === 'PRINCIPAL') experienceLevel = 'LEAD';

  // Normalize currency
  const rawCurrency = (jobData.currency || jobData.salary?.currency || 'INR').toUpperCase();
  const validCurrencies = ['INR', 'USD', 'EUR', 'GBP', 'AED'];
  const currency = validCurrencies.includes(rawCurrency) ? rawCurrency : 'INR';

  // Normalize skills (at least 1 non-empty skill)
  let skills = [];
  if (Array.isArray(jobData.skills)) {
    skills = jobData.skills.map((s) => (typeof s === 'string' ? s.trim() : '')).filter(Boolean);
  } else if (typeof jobData.skills === 'string') {
    skills = jobData.skills.split(',').map((s) => s.trim()).filter(Boolean);
  }
  if (skills.length === 0) {
    skills = ['General Engineering'];
  }

  // Normalize education (max 200 chars)
  let education = '';
  if (typeof jobData.education === 'string') {
    education = jobData.education.trim();
  } else if (Array.isArray(jobData.educationRequirements)) {
    education = jobData.educationRequirements.join(', ').trim();
  } else if (typeof jobData.educationRequirements === 'string') {
    education = jobData.educationRequirements.trim();
  }
  if (education.length > 200) {
    education = education.substring(0, 200);
  }

  // Normalize location
  const location = (jobData.location && jobData.location.trim()) || 'Remote';

  // Normalize remote
  const remote =
    jobData.remote === true ||
    jobData.workplaceType === 'remote' ||
    rawType === 'REMOTE' ||
    location.toLowerCase().includes('remote');

  // Normalize salary
  const rawMin = jobData.salaryMin ?? jobData.minSalary ?? jobData.salary?.min;
  const rawMax = jobData.salaryMax ?? jobData.maxSalary ?? jobData.salary?.max;
  const minSal = rawMin !== '' && rawMin != null && !isNaN(Number(rawMin)) ? Number(rawMin) : null;
  const maxSal = rawMax !== '' && rawMax != null && !isNaN(Number(rawMax)) ? Number(rawMax) : null;

  // Normalize openings
  const openings = Number(jobData.openings) > 0 ? Number(jobData.openings) : 1;

  // Normalize applicationDeadline:
  let applicationDeadline = jobData.applicationDeadline || jobData.deadline || null;
  if (applicationDeadline) {
    const d = new Date(applicationDeadline);
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    if (isNaN(d.getTime()) || d <= today) {
      const future = new Date(Date.now() + 30 * 24 * 60 * 60 * 1000);
      applicationDeadline = future.toISOString().split('T')[0];
    } else {
      applicationDeadline = applicationDeadline.split('T')[0];
    }
  } else {
    const future = new Date(Date.now() + 30 * 24 * 60 * 60 * 1000);
    applicationDeadline = future.toISOString().split('T')[0];
  }

  const payload = {
    title: (jobData.title || '').trim(),
    description: (jobData.description || '').trim(),
    employmentType,
    experienceLevel,
    location,
    remote,
    salaryMin: minSal,
    salaryMax: maxSal,
    currency,
    skills,
    education: education || null,
    openings,
    applicationDeadline,
  };

  if (jobData.status) {
    const s = String(jobData.status).toUpperCase();
    if (['OPEN', 'CLOSED', 'PAUSED', 'DRAFT'].includes(s)) {
      payload.status = s;
    }
  }

  return payload;
}

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
    const payload = formatJobPayload(jobData);
    return apiClient.post('/jobs', payload);
  },

  /** Update an existing job posting (Recruiter) */
  updateJob: async (id, jobData) => {
    const payload = formatJobPayload(jobData);
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
