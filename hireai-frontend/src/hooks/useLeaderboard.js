import { useQuery } from '@tanstack/react-query';
import { applicationsApi } from '@/services/api/applications.api';
import { normalizeApplication } from '@/utils/normalizers';

/**
 * Hook to fetch ATS ranked candidate leaderboard for a specific job requisition.
 */
export function useLeaderboard(jobId, options = {}) {
  const { enabled = true } = options;

  const query = useQuery({
    queryKey: ['leaderboard', jobId],
    enabled: Boolean(jobId) && enabled,
    queryFn: async () => {
      try {
        const remoteApps = await applicationsApi.getJobApplications(jobId);
        if (Array.isArray(remoteApps)) {
          const normalized = remoteApps.map(normalizeApplication).filter(Boolean);
          return normalized.sort((a, b) => (b.atsMatchScore ?? b.aiScore ?? 0) - (a.atsMatchScore ?? a.aiScore ?? 0));
        }
        return [];
      } catch (err) {
        console.warn(`[useLeaderboard] Could not fetch applicants for job ${jobId}:`, err.message);
        return [];
      }
    },
  });

  return {
    applicants: query.data || [],
    isLoading: query.isLoading,
    isFetching: query.isFetching,
    isError: query.isError,
    error: query.error,
    refetch: query.refetch,
  };
}
