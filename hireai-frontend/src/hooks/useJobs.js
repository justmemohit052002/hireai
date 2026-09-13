import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { jobsApi } from '@/services/api/jobs.api';
import { useAuth } from '@/context/AuthContext';
import { normalizeJob, seedMockJobs } from '@/utils/normalizers';

/**
 * Main jobs hook for browsing and managing job requisitions.
 * Supports role-based fetching (recruiter vs candidate) and custom filter parameters.
 */
export function useJobs(filters = {}) {
  const queryClient = useQueryClient();
  const { role, isAuthenticated } = useAuth();

  const queryKey = ['jobs', { role, isAuthenticated, ...filters }];

  const query = useQuery({
    queryKey,
    queryFn: async () => {
      try {
        let rawJobs = [];
        if (role === 'recruiter') {
          if (!isAuthenticated) return [];
          rawJobs = await jobsApi.getMyJobs();
        } else {
          rawJobs = await jobsApi.getOpenJobs();
        }

        if (Array.isArray(rawJobs)) {
          return rawJobs.map(normalizeJob).filter(Boolean);
        }
        return [];
      } catch (err) {
        console.warn('[useJobs] Failed to fetch remote jobs:', err.message);
        if (role === 'candidate' && !isAuthenticated) {
          return seedMockJobs();
        }
        if (role === 'recruiter') {
          return [];
        }
        // Fallback for demo / offline viewing
        return seedMockJobs();
      }
    },
  });

  const createMutation = useCreateJob();
  const updateMutation = useUpdateJob();
  const closeMutation = useCloseJob();

  // Optimistic / mutation actions to maintain seamless compatibility
  const toggleJobStatus = async (id) => {
    queryClient.setQueryData(queryKey, (oldJobs = []) =>
      oldJobs.map((j) => {
        if (j.id !== id) return j;
        const nextStatus = j.listingStatus === 'open' ? 'paused' : 'open';
        return {
          ...j,
          listingStatus: nextStatus,
          status: nextStatus === 'open' ? 'active' : 'paused',
        };
      })
    );
  };

  const resumeJob = async (id) => {
    queryClient.setQueryData(queryKey, (oldJobs = []) =>
      oldJobs.map((j) => (j.id === id ? { ...j, listingStatus: 'open', status: 'active' } : j))
    );
  };

  const pauseJob = async (id) => {
    queryClient.setQueryData(queryKey, (oldJobs = []) =>
      oldJobs.map((j) => (j.id === id ? { ...j, listingStatus: 'paused', status: 'paused' } : j))
    );
  };

  const deleteJob = async (id) => {
    queryClient.setQueryData(queryKey, (oldJobs = []) => oldJobs.filter((j) => j.id !== id));
  };

  const createOrUpdateJob = async (jobData) => {
    if (jobData.id && !jobData.id.startsWith('job-')) {
      return updateMutation.mutateAsync({ id: jobData.id, jobData });
    }
    return createMutation.mutateAsync(jobData);
  };

  return {
    jobs: query.data || [],
    isLoading: query.isLoading,
    isFetching: query.isFetching,
    isError: query.isError,
    error: query.error,
    refetch: query.refetch,
    // Mutations & Actions
    createOrUpdateJob,
    closeJob: (id) => closeMutation.mutateAsync(id),
    toggleJobStatus,
    resumeJob,
    pauseJob,
    deleteJob,
  };
}

/**
 * Hook to fetch a single job by ID with caching.
 */
export function useJob(jobId) {
  return useQuery({
    queryKey: ['job', jobId],
    queryFn: async () => {
      if (!jobId) return null;
      const raw = await jobsApi.getJob(jobId);
      return normalizeJob(raw);
    },
    enabled: Boolean(jobId),
  });
}

/**
 * Mutation hook for job creation. Automatically invalidates 'jobs' cache on success.
 */
export function useCreateJob() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (jobData) => {
      try {
        const res = await jobsApi.createJob(jobData);
        return normalizeJob(res) || { ...jobData, id: res?.id || `job-${Date.now()}` };
      } catch (err) {
        console.warn('[useCreateJob] Remote create job failed, falling back locally:', err);
        return {
          ...jobData,
          id: jobData.id || `job-${Date.now()}`,
          postedAt: new Date().toISOString(),
          applicantsCount: 0,
          listingStatus: 'open',
          status: 'active',
        };
      }
    },
    onSuccess: (newJob) => {
      queryClient.setQueriesData({ queryKey: ['jobs'] }, (old = []) => {
        if (!Array.isArray(old)) return [newJob];
        return [newJob, ...old.filter((j) => j.id !== newJob.id)];
      });
      queryClient.invalidateQueries({ queryKey: ['jobs'] });
    },
  });
}

/**
 * Mutation hook for job updates. Automatically invalidates 'jobs' cache on success.
 */
export function useUpdateJob() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({ id, jobData }) => {
      try {
        const res = await jobsApi.updateJob(id, jobData);
        return normalizeJob(res) || { ...jobData, id };
      } catch (err) {
        console.warn('[useUpdateJob] Remote update failed, falling back locally:', err);
        return { ...jobData, id };
      }
    },
    onSuccess: (updatedJob) => {
      queryClient.setQueriesData({ queryKey: ['jobs'] }, (old = []) => {
        if (!Array.isArray(old)) return [updatedJob];
        return old.map((j) => (j.id === updatedJob.id ? updatedJob : j));
      });
      queryClient.invalidateQueries({ queryKey: ['jobs'] });
      queryClient.invalidateQueries({ queryKey: ['job', updatedJob.id] });
    },
  });
}

/**
 * Mutation hook to close a job posting.
 */
export function useCloseJob() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (id) => {
      try {
        await jobsApi.closeJob(id);
      } catch (err) {
        console.warn('[useCloseJob] Remote close failed:', err);
      }
      return id;
    },
    onSuccess: (closedId) => {
      queryClient.setQueriesData({ queryKey: ['jobs'] }, (old = []) => {
        if (!Array.isArray(old)) return [];
        return old.map((j) =>
          j.id === closedId ? { ...j, listingStatus: 'closed', status: 'closed' } : j
        );
      });
      queryClient.invalidateQueries({ queryKey: ['jobs'] });
    },
  });
}

/**
 * Mutation hook to pause or toggle job status.
 */
export function useToggleJobStatus() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({ id, newStatus }) => {
      return { id, newStatus };
    },
    onSuccess: ({ id, newStatus }) => {
      queryClient.setQueriesData({ queryKey: ['jobs'] }, (old = []) => {
        if (!Array.isArray(old)) return [];
        return old.map((j) => {
          if (j.id !== id) return j;
          const status = newStatus || (j.listingStatus === 'open' ? 'paused' : 'open');
          return {
            ...j,
            listingStatus: status,
            status: status === 'open' ? 'active' : 'paused',
          };
        });
      });
      queryClient.invalidateQueries({ queryKey: ['jobs'] });
    },
  });
}
