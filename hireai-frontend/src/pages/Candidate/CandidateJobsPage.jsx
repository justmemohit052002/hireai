import React, { useState } from 'react';
import { AlertCircle, RotateCcw } from 'lucide-react';
import { DynamicIslandSearch } from '@/components/common/DynamicIslandSearch';
import { JobCard } from '@/features/jobs/JobCard';
import { JobDetailDrawer } from '@/features/jobs/JobDetailDrawer';
import { ApplyModal } from '@/features/jobs/ApplyModal';
import { FilterPanel } from '@/features/jobs/FilterPanel';
import { EmptyState } from '@/components/common/EmptyState';
import { JobCardSkeleton } from '@/components/common/LoadingSkeleton';
import { Button } from '@/components/ui/Button';
import { useJobs, useApplyJob } from '@/hooks';

export const CandidateJobsPage = () => {
  const { jobs, isLoading, isError, error, refetch } = useJobs();
  const applyMutation = useApplyJob();

  const [searchQuery, setSearchQuery] = useState('');
  const [showFilters, setShowFilters] = useState(false);
  const [filters, setFilters] = useState({});
  const [selectedJobForDrawer, setSelectedJobForDrawer] = useState(null);
  const [applyingJob, setApplyingJob] = useState(null);

  // Candidate portal rules:
  //   closed  → hidden entirely
  //   paused  → shown with Apply disabled
  //   open    → shown with Apply active
  const visibleJobs = (jobs || []).filter((j) => j.listingStatus !== 'closed');

  // Apply search + filter on top of the visible set
  const filteredJobs = visibleJobs.filter((job) => {
    if (searchQuery) {
      const q = searchQuery.toLowerCase();
      const matchesTitle = job.title?.toLowerCase().includes(q);
      const matchesCompany = (job.company?.name || job.companyName)?.toLowerCase().includes(q);
      const matchesSkill = job.skills?.some((s) => s.toLowerCase().includes(q));
      if (!matchesTitle && !matchesCompany && !matchesSkill) return false;
    }

    if (filters.jobType && filters.jobType.length > 0) {
      const normJobType = (job.type || job.employmentType || '').toLowerCase().replace(/_/g, '-');
      const matches = filters.jobType.some(
        (ft) =>
          ft.toLowerCase().replace(/_/g, '-') === normJobType ||
          (job.jobType && job.jobType.toLowerCase().replace(/_/g, '-') === ft.toLowerCase().replace(/_/g, '-'))
      );
      if (!matches) return false;
    }

    if (filters.level && filters.level.length > 0) {
      const normLevel = (job.level || '').toLowerCase();
      const rawExpLevel = (job.experienceLevel || '').toLowerCase();
      const matches = filters.level.some((fl) => {
        const target = fl.toLowerCase();
        return (
          normLevel === target ||
          rawExpLevel === target ||
          (target === 'entry' && (rawExpLevel === 'fresher' || rawExpLevel === 'entry')) ||
          (target === 'mid' && (rawExpLevel === 'mid_level' || rawExpLevel === 'mid-level')) ||
          (target === 'senior' && rawExpLevel === 'senior') ||
          (target === 'lead' && (rawExpLevel === 'lead' || rawExpLevel === 'executive'))
        );
      });
      if (!matches) return false;
    }

    if (filters.location && filters.location.trim() !== '') {
      if (!job.location?.toLowerCase().includes(filters.location.toLowerCase())) return false;
    }

    if (filters.salaryMin !== undefined) {
      const jobMax = Number(job.salary?.max ?? job.salaryMax ?? 0);
      if (jobMax > 0 && jobMax < filters.salaryMin) return false;
    }

    return true;
  });

  return (
    <div className="space-y-6">
      {/* Dynamic Search Island Header */}
      <DynamicIslandSearch
        onSearch={(q) => setSearchQuery(q)}
        onFilterToggle={() => setShowFilters(!showFilters)}
      />

      {/* Error State Banner */}
      {isError && (
        <div className="p-4 rounded-2xl bg-red-500/10 border border-red-500/20 text-red-600 dark:text-red-400 flex items-center justify-between gap-4">
          <div className="flex items-center gap-3">
            <AlertCircle className="w-5 h-5 shrink-0" />
            <div>
              <p className="text-sm font-semibold">Unable to load active positions</p>
              <p className="text-xs opacity-80">{error?.message || 'A network error occurred while reaching HireAI servers.'}</p>
            </div>
          </div>
          <Button
            size="sm"
            variant="outline"
            onClick={() => refetch()}
            className="shrink-0 font-semibold gap-1.5"
          >
            <RotateCcw className="w-3.5 h-3.5" /> Retry
          </Button>
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">
        {/* Expandable Filter Panel */}
        {showFilters && (
          <div className="lg:col-span-1">
            <FilterPanel
              filters={filters}
              onChange={(f) => setFilters(f)}
              onReset={() => setFilters({})}
            />
          </div>
        )}

        {/* Jobs List */}
        <div className={showFilters ? 'lg:col-span-3' : 'lg:col-span-4'}>
          {isLoading ? (
            <div className="flex flex-col gap-4">
              <JobCardSkeleton />
              <JobCardSkeleton />
              <JobCardSkeleton />
            </div>
          ) : filteredJobs.length > 0 ? (
            <div className="flex flex-col gap-4">
              {filteredJobs.map((job) => (
                <JobCard
                  key={job.id}
                  job={job}
                  isPaused={job.listingStatus === 'paused'}
                  onSelect={(j) => setSelectedJobForDrawer(j)}
                  onApply={(j) => setApplyingJob(j)}
                />
              ))}
            </div>
          ) : (
            <EmptyState
              title="No Matching Positions Found"
              description="Try broadening your search keywords or resetting active filter pills."
              actionLabel="Reset All Filters"
              onAction={() => {
                setSearchQuery('');
                setFilters({});
              }}
            />
          )}
        </div>
      </div>

      {/* Job Details Bottom Slide-Up Drawer */}
      <JobDetailDrawer
        job={selectedJobForDrawer}
        isOpen={Boolean(selectedJobForDrawer)}
        isPaused={selectedJobForDrawer?.listingStatus === 'paused'}
        onClose={() => setSelectedJobForDrawer(null)}
        onApply={(job) => {
          setSelectedJobForDrawer(null);
          setApplyingJob(job);
        }}
      />

      {/* Apply Modal */}
      <ApplyModal
        job={applyingJob}
        isOpen={Boolean(applyingJob)}
        onClose={() => setApplyingJob(null)}
        onSubmit={async (jobId, coverLetter, resumeFile) => {
          await applyMutation.mutateAsync({ jobId, coverNote: coverLetter, file: resumeFile });
          setApplyingJob(null);
        }}
      />
    </div>
  );
};
