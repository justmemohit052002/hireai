import React, { useState } from 'react';
import { DynamicIslandSearch } from '@/components/common/DynamicIslandSearch';
import { JobCard } from '@/features/jobs/JobCard';
import { JobDetailDrawer } from '@/features/jobs/JobDetailDrawer';
import { ApplyModal } from '@/features/jobs/ApplyModal';
import { FilterPanel } from '@/features/jobs/FilterPanel';
import { EmptyState } from '@/components/common/EmptyState';
import { useJobs } from '@/context/JobsContext';

export const CandidateJobsPage = () => {
  const { jobs, submitApplication } = useJobs();

  const [searchQuery, setSearchQuery] = useState('');
  const [showFilters, setShowFilters] = useState(false);
  const [filters, setFilters] = useState({});
  const [selectedJobForDrawer, setSelectedJobForDrawer] = useState(null);
  const [applyingJob, setApplyingJob] = useState(null);

  // Candidate portal rules:
  //   closed  → hidden entirely
  //   paused  → shown with Apply disabled
  //   open    → shown with Apply active
  const visibleJobs = jobs.filter((j) => j.listingStatus !== 'closed');

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
      if (!filters.jobType.includes(job.type) && !filters.jobType.includes(job.jobType)) return false;
    }

    if (filters.level && filters.level.length > 0) {
      if (!filters.level.includes(job.level) && !filters.level.includes(job.experienceLevel)) return false;
    }

    if (filters.location && filters.location.trim() !== '') {
      if (!job.location?.toLowerCase().includes(filters.location.toLowerCase())) return false;
    }

    if (filters.salaryMin !== undefined) {
      const jobMax = Number(job.salary?.max || job.salaryMax || 0);
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
          {filteredJobs.length > 0 ? (
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
        onSubmit={(jobId, coverLetter) => {
          submitApplication({ jobId, coverLetter });
          setApplyingJob(null);
        }}
      />
    </div>
  );
};
