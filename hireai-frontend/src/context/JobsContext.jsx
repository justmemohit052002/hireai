import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { jobsApi } from '@/services/api/jobs.api';
import { applicationsApi } from '@/services/api/applications.api';
import { useAuth } from './AuthContext';
import { MOCK_JOBS } from '@/mock/jobs';
import { MOCK_APPLICATIONS } from '@/mock/applications';

function seedMockJobs() {
  return MOCK_JOBS.map((j) => ({
    ...j,
    listingStatus: j.listingStatus || 'open',
    status: j.status || 'active',
  }));
}

function normalizeJob(backendJob) {
  if (!backendJob) return null;
  const rawType = backendJob.employmentType || backendJob.jobType || 'FULL_TIME';
  const rawLevel = backendJob.experienceLevel || backendJob.level || 'MID_LEVEL';
  const statusStr = String(backendJob.status || 'OPEN').toUpperCase();

  let listingStatus = 'open';
  if (statusStr === 'PAUSED') listingStatus = 'paused';
  else if (statusStr === 'CLOSED') listingStatus = 'closed';

  return {
    id: backendJob.id,
    title: backendJob.title,
    description: backendJob.description,
    department: backendJob.department || 'Engineering',
    location: backendJob.location || 'Remote',
    employmentType: rawType,
    type: rawType.toLowerCase().replace(/_/g, '-'),
    jobType: rawType,
    workplaceType: backendJob.remote
      ? 'remote'
      : (backendJob.workplaceType || 'HYBRID').toLowerCase().replace(/_/g, '-'),
    remote: Boolean(backendJob.remote),
    level: rawLevel.toLowerCase().replace(/_/g, '-'),
    experienceLevel: rawLevel,
    salary: {
      min: backendJob.salaryMin ?? backendJob.minSalary ?? 0,
      max: backendJob.salaryMax ?? backendJob.maxSalary ?? 0,
      currency: backendJob.currency || 'INR',
      period: backendJob.salaryPeriod || 'year',
    },
    salaryMin: backendJob.salaryMin ?? backendJob.minSalary ?? 0,
    salaryMax: backendJob.salaryMax ?? backendJob.maxSalary ?? 0,
    currency: backendJob.currency || 'INR',
    skills: Array.isArray(backendJob.skills) ? backendJob.skills : [],
    educationRequirements: backendJob.education ? [backendJob.education] : backendJob.educationRequirements || [],
    education: backendJob.education || '',
    benefits: backendJob.benefits || [],
    openings: backendJob.openings || 1,
    applicationDeadline: backendJob.applicationDeadline,
    postedAt: backendJob.createdAt || new Date().toISOString(),
    applicantsCount: Number(backendJob.applicantsCount || 0),
    listingStatus,
    status: listingStatus === 'open' ? 'active' : listingStatus,
    company: {
      id: backendJob.companyId || backendJob.recruiterProfileId || 'comp-1',
      name: backendJob.companyName || 'HireAI Partner',
      industry: 'Technology',
      location: backendJob.location || 'Global',
    },
    companyName: backendJob.companyName || 'HireAI Partner',
    raw: backendJob,
  };
}

function normalizeApplication(backendApp) {
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

const JobsContext = createContext(null);

export const JobsProvider = ({ children }) => {
  const { role, isAuthenticated, user } = useAuth();
  const [jobs, setJobs] = useState([]);
  const [applications, setApplications] = useState([]);
  const [isLoadingJobs, setIsLoadingJobs] = useState(false);
  const [isLoadingApps, setIsLoadingApps] = useState(false);

  // Fetch jobs from backend
  const fetchJobs = useCallback(async () => {
    setIsLoadingJobs(true);
    try {
      let rawJobs = [];
      if (role === 'recruiter') {
        if (!isAuthenticated) {
          setJobs([]);
          return;
        }
        // Recruiter only gets their own created postings
        rawJobs = await jobsApi.getMyJobs();
      } else {
        // Candidates and public view open postings
        rawJobs = await jobsApi.getOpenJobs();
      }

      if (Array.isArray(rawJobs)) {
        const normalized = rawJobs.map(normalizeJob).filter(Boolean);
        setJobs(normalized);
      } else {
        setJobs([]);
      }
    } catch (err) {
      console.warn('[JobsContext] Could not fetch remote jobs:', err.message);
      if (role === 'candidate' && !isAuthenticated) {
        setJobs(seedMockJobs());
      } else if (role === 'recruiter') {
        setJobs([]);
      }
    } finally {
      setIsLoadingJobs(false);
    }
  }, [role, isAuthenticated]);

  // Fetch applications from backend
  const fetchApplications = useCallback(async (jobIdForRecruiter = null) => {
    if (!isAuthenticated) {
      setApplications([]);
      return;
    }
    setIsLoadingApps(true);
    try {
      if (role === 'candidate') {
        const myApps = await applicationsApi.getMyApplications();
        if (Array.isArray(myApps)) {
          const normalized = myApps.map(normalizeApplication).filter(Boolean);
          setApplications(normalized);
        }
      } else if (role === 'recruiter' && jobIdForRecruiter) {
        const jobApps = await applicationsApi.getJobApplications(jobIdForRecruiter);
        if (Array.isArray(jobApps)) {
          const normalized = jobApps.map(normalizeApplication).filter(Boolean);
          setApplications((prev) => {
            const others = prev.filter((a) => a.jobId !== jobIdForRecruiter);
            return [...normalized, ...others];
          });
        }
      }
    } catch (err) {
      console.warn('[JobsContext] Could not fetch remote applications:', err.message);
    } finally {
      setIsLoadingApps(false);
    }
  }, [role, isAuthenticated]);

  // Sync on user login/logout/role switch
  useEffect(() => {
    setJobs([]);
    setApplications([]);
    fetchJobs();
    if (isAuthenticated) {
      fetchApplications();
    }
  }, [user?.id, role, isAuthenticated, fetchJobs, fetchApplications]);

  /** Create or Update Job */
  const createOrUpdateJob = useCallback(async (jobData) => {
    try {
      let result;
      if (jobData.id && !String(jobData.id).startsWith('job-')) {
        result = await jobsApi.updateJob(jobData.id, jobData);
      } else {
        result = await jobsApi.createJob(jobData);
      }
      const normalized = normalizeJob(result);
      if (normalized) {
        setJobs((prev) => [normalized, ...prev.filter((j) => j.id !== normalized.id)]);
        return normalized;
      }
      return result;
    } catch (err) {
      console.error('[JobsContext] Remote create/update job failed:', err);
      if (isAuthenticated) {
        throw err;
      }
      const localJob = {
        ...jobData,
        id: jobData.id || `job-${Date.now()}`,
        postedAt: new Date().toISOString(),
        applicantsCount: 0,
        listingStatus: 'open',
        status: 'active',
      };
      setJobs((prev) => [localJob, ...prev.filter((j) => j.id !== localJob.id)]);
      return localJob;
    }
  }, [isAuthenticated]);

  /** Close a job */
  const closeJob = useCallback(async (id) => {
    try {
      await jobsApi.closeJob(id);
    } catch (err) {
      console.warn('[JobsContext] Remote close failed:', err);
    }
    setJobs((prev) =>
      prev.map((j) =>
        j.id === id ? { ...j, listingStatus: 'closed', status: 'closed' } : j
      )
    );
  }, []);

  /** Pause or toggle a job */
  const toggleJobStatus = useCallback(async (id) => {
    const targetJob = jobs.find((j) => j.id === id);
    const nextListing = targetJob?.listingStatus === 'open' ? 'paused' : 'open';
    const nextStatus = nextListing === 'open' ? 'OPEN' : 'PAUSED';

    setJobs((prev) =>
      prev.map((j) => {
        if (j.id !== id) return j;
        return {
          ...j,
          listingStatus: nextListing,
          status: nextListing === 'open' ? 'active' : 'paused',
        };
      })
    );

    if (targetJob && !String(id).startsWith('job-')) {
      try {
        await jobsApi.updateJob(id, { ...targetJob, status: nextStatus });
      } catch (err) {
        console.warn('[JobsContext] Failed to sync job status with backend:', err);
      }
    }
  }, [jobs]);

  /** Resume a job */
  const resumeJob = useCallback(async (id) => {
    setJobs((prev) =>
      prev.map((j) =>
        j.id === id ? { ...j, listingStatus: 'open', status: 'active' } : j
      )
    );
    const targetJob = jobs.find((j) => j.id === id);
    if (targetJob && !String(id).startsWith('job-')) {
      try {
        await jobsApi.updateJob(id, { ...targetJob, status: 'OPEN' });
      } catch (err) {
        console.warn('[JobsContext] Failed to resume job on backend:', err);
      }
    }
  }, [jobs]);

  /** Pause a job */
  const pauseJob = useCallback(async (id) => {
    setJobs((prev) =>
      prev.map((j) =>
        j.id === id ? { ...j, listingStatus: 'paused', status: 'paused' } : j
      )
    );
    const targetJob = jobs.find((j) => j.id === id);
    if (targetJob && !String(id).startsWith('job-')) {
      try {
        await jobsApi.updateJob(id, { ...targetJob, status: 'PAUSED' });
      } catch (err) {
        console.warn('[JobsContext] Failed to pause job on backend:', err);
      }
    }
  }, [jobs]);

  /** Delete / Close a job */
  const deleteJob = useCallback(async (id) => {
    if (!String(id).startsWith('job-')) {
      try {
        await jobsApi.closeJob(id);
      } catch (err) {
        console.warn('[JobsContext] Remote delete/close failed:', err);
      }
    }
    setJobs((prev) => prev.filter((j) => j.id !== id));
  }, []);

  /** Submit candidate application */
  const submitApplication = useCallback(async ({ jobId, coverLetter, coverNote, resumeFile, file }) => {
    const note = coverNote || coverLetter || '';
    const resume = file || resumeFile || null;
    try {
      const response = await applicationsApi.applyToJob(jobId, { coverNote: note, file: resume });
      const normalized = normalizeApplication(response);
      if (normalized) {
        setApplications((prev) => [normalized, ...prev]);
      }
      setJobs((prev) =>
        prev.map((j) =>
          j.id === jobId ? { ...j, applicantsCount: (j.applicantsCount || 0) + 1 } : j
        )
      );
      return normalized || response;
    } catch (err) {
      console.warn('[JobsContext] Remote apply failed:', err.message);
      if (isAuthenticated) {
        throw err;
      }
      const newApp = {
        id: `app-${Date.now()}`,
        jobId,
        candidateName: 'Candidate',
        candidateEmail: '',
        coverLetter: note,
        status: 'applied',
        aiScore: 85,
        appliedAt: new Date().toISOString(),
      };
      setApplications((prev) => [newApp, ...prev]);
      setJobs((prev) =>
        prev.map((j) =>
          j.id === jobId ? { ...j, applicantsCount: (j.applicantsCount || 0) + 1 } : j
        )
      );
      return newApp;
    }
  }, [isAuthenticated]);

  /** Update application stage */
  const updateApplicationStatus = useCallback(async (appId, newStage, feedback = '') => {
    try {
      await applicationsApi.updateApplicationStatus(appId, { status: newStage.toUpperCase(), feedbackNotes: feedback });
    } catch (err) {
      console.warn('[JobsContext] Remote stage update failed, applying locally:', err);
    }
    setApplications((prev) =>
      prev.map((a) => (a.id === appId ? { ...a, status: newStage.toLowerCase() } : a))
    );
  }, []);

  return (
    <JobsContext.Provider
      value={{
        jobs,
        applications,
        isLoadingJobs,
        isLoadingApps,
        fetchJobs,
        refreshJobs: fetchJobs,
        fetchApplications,
        createOrUpdateJob,
        closeJob,
        toggleJobStatus,
        resumeJob,
        pauseJob,
        deleteJob,
        submitApplication,
        updateApplicationStatus,
      }}
    >
      {children}
    </JobsContext.Provider>
  );
};

export const useJobs = () => {
  const context = useContext(JobsContext);
  if (!context) {
    throw new Error('useJobs must be used within a JobsProvider');
  }
  return context;
};
