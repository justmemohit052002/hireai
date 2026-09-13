import { MOCK_JOBS } from '@/mock/jobs';

export function seedMockJobs() {
  return MOCK_JOBS.map((j) => ({
    ...j,
    listingStatus: j.listingStatus || 'open',
    status: j.status || 'active',
  }));
}

export function normalizeJob(backendJob) {
  if (!backendJob) return null;
  return {
    id: backendJob.id,
    title: backendJob.title,
    description: backendJob.description,
    department: backendJob.department || 'General',
    location: backendJob.location || 'Remote',
    type: (backendJob.jobType || 'FULL_TIME').toLowerCase().replace('_', '-'),
    jobType: backendJob.jobType,
    workplaceType: (backendJob.workplaceType || 'HYBRID').toLowerCase().replace('_', '-'),
    level: (backendJob.experienceLevel || 'MID').toLowerCase().replace('_', '-'),
    experienceLevel: backendJob.experienceLevel,
    salary: {
      min: backendJob.salaryMin ?? backendJob.minSalary ?? 0,
      max: backendJob.salaryMax ?? backendJob.maxSalary ?? 0,
      currency: backendJob.currency || 'INR',
      period: backendJob.salaryPeriod || 'year',
    },
    skills: Array.isArray(backendJob.skills) ? backendJob.skills : [],
    educationRequirements: backendJob.educationRequirements || [],
    benefits: backendJob.benefits || [],
    applicationDeadline: backendJob.applicationDeadline,
    postedAt: backendJob.createdAt || new Date().toISOString(),
    applicantsCount: Number(backendJob.applicantsCount || 0),
    listingStatus: (backendJob.status || 'OPEN').toLowerCase() === 'open' ? 'open' : 'closed',
    status: (backendJob.status || 'OPEN').toLowerCase() === 'open' ? 'active' : 'closed',
    company: {
      id: backendJob.companyId || 'comp-1',
      name: backendJob.companyName || 'HireAI Partner',
      industry: 'Technology',
      location: backendJob.location || 'Global',
    },
    raw: backendJob,
  };
}

export function normalizeApplication(backendApp) {
  if (!backendApp) return null;
  const stageMap = {
    APPLIED: 'applied',
    SCREENING: 'screening',
    SHORTLISTED: 'shortlisted',
    INTERVIEW_SCHEDULED: 'interview',
    OFFERED: 'offer',
    REJECTED: 'rejected',
  };

  return {
    id: backendApp.id,
    jobId: backendApp.jobId,
    candidateId: backendApp.candidateId,
    candidateName: backendApp.candidateName || 'Candidate',
    candidateEmail: backendApp.candidateEmail || '',
    candidatePhone: backendApp.candidatePhone || '',
    coverLetter: backendApp.coverNote || '',
    coverNote: backendApp.coverNote || '',
    resumeFileName: backendApp.resumeFileName || '',
    status: stageMap[backendApp.status] || (backendApp.status || 'applied').toLowerCase(),
    rawStatus: backendApp.status,
    aiScore: backendApp.atsMatchScore != null ? Math.round(backendApp.atsMatchScore) : 85,
    atsMatchScore: backendApp.atsMatchScore,
    skillsIdentified: backendApp.skillsIdentified || [],
    feedbackNotes: backendApp.feedbackNotes || '',
    appliedAt: backendApp.createdAt || new Date().toISOString(),
    updatedAt: backendApp.updatedAt || backendApp.createdAt || new Date().toISOString(),
    raw: backendApp,
  };
}
