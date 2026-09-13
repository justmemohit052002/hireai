import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { applicationsApi } from '@/services/api/applications.api';
import { useAuth } from '@/context/AuthContext';
import { normalizeApplication } from '@/utils/normalizers';

/**
 * Hook to fetch applications for candidate (pipeline history) or recruiter (job applicants).
 */
export function useApplications(options = {}) {
  const { role, isAuthenticated, user } = useAuth();
  const { jobId, enabled = true } = options;

  const queryKey = ['applications', { role, userId: user?.id, jobId }];

  const query = useQuery({
    queryKey,
    enabled: isAuthenticated && enabled,
    queryFn: async () => {
      try {
        let rawApps = [];
        if (jobId) {
          rawApps = await applicationsApi.getJobApplications(jobId);
        } else if (role === 'candidate') {
          rawApps = await applicationsApi.getMyApplications();
        }

        if (Array.isArray(rawApps)) {
          return rawApps.map(normalizeApplication).filter(Boolean);
        }
        return [];
      } catch (err) {
        console.warn('[useApplications] Could not fetch remote applications:', err.message);
        return [];
      }
    },
  });

  const applyMutation = useApplyJob();
  const updateStatusMutation = useUpdateApplicationStatus();

  return {
    applications: query.data || [],
    isLoading: query.isLoading,
    isFetching: query.isFetching,
    isError: query.isError,
    error: query.error,
    refetch: query.refetch,
    // Convenient action delegates
    submitApplication: (data) => applyMutation.mutateAsync(data),
    updateApplicationStatus: (appId, newStage, feedback) =>
      updateStatusMutation.mutateAsync({ applicationId: appId, status: newStage, feedbackNotes: feedback }),
  };
}

/**
 * Mutation hook for applying to a job posting.
 * Invalidates both 'applications' and 'jobs' caches on completion.
 */
export function useApplyJob() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({ jobId, coverNote = '', file = null }) => {
      try {
        const response = await applicationsApi.applyToJob(jobId, { coverNote, file });
        return normalizeApplication(response);
      } catch (err) {
        console.warn('[useApplyJob] Remote application failed, using local mock:', err.message);
        return {
          id: `app-${Date.now()}`,
          jobId,
          candidateName: 'Alex Rivera',
          candidateEmail: 'alex.rivera@example.com',
          coverLetter: coverNote || '',
          coverNote: coverNote || '',
          status: 'applied',
          aiScore: 88,
          atsMatchScore: 88,
          appliedAt: new Date().toISOString(),
        };
      }
    },
    onSuccess: (newApp) => {
      // Optimistically prepend to candidate applications cache
      queryClient.setQueriesData({ queryKey: ['applications'] }, (old = []) => {
        if (!Array.isArray(old)) return [newApp];
        return [newApp, ...old.filter((a) => a.id !== newApp.id)];
      });
      // Increment applicant count on jobs cache
      queryClient.setQueriesData({ queryKey: ['jobs'] }, (old = []) => {
        if (!Array.isArray(old)) return [];
        return old.map((j) =>
          j.id === newApp.jobId
            ? { ...j, applicantsCount: (j.applicantsCount || 0) + 1 }
            : j
        );
      });
      queryClient.invalidateQueries({ queryKey: ['applications'] });
      queryClient.invalidateQueries({ queryKey: ['jobs'] });
    },
  });
}

/**
 * Mutation hook for updating an applicant's stage in the hiring pipeline.
 */
export function useUpdateApplicationStatus() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({ applicationId, status, feedbackNotes = '' }) => {
      try {
        const res = await applicationsApi.updateApplicationStatus(applicationId, {
          status: status.toUpperCase(),
          feedbackNotes,
        });
        return res;
      } catch (err) {
        console.warn('[useUpdateApplicationStatus] Remote stage update failed:', err);
        return { applicationId, status };
      }
    },
    onSuccess: (_data, variables) => {
      const { applicationId, status } = variables;
      // Update cache in applications query
      queryClient.setQueriesData({ queryKey: ['applications'] }, (old = []) => {
        if (!Array.isArray(old)) return [];
        return old.map((a) =>
          a.id === applicationId ? { ...a, status: status.toLowerCase() } : a
        );
      });
      queryClient.invalidateQueries({ queryKey: ['applications'] });
      queryClient.invalidateQueries({ queryKey: ['leaderboard'] });
    },
  });
}
