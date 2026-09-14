import React from 'react';
import { ApplicationCard } from '@/features/candidate/ApplicationCard';
import { useApplications, useJobs } from '@/hooks';
import { MOCK_APPLICATIONS } from '@/mock';
import { Briefcase, AlertCircle, RotateCcw } from 'lucide-react';
import { Link } from 'react-router-dom';
import { ROUTES } from '@/constants';
import { Button } from '@/components/ui/Button';
import { Skeleton } from '@/components/ui/Skeleton';
import { Card } from '@/components/ui/Card';

export const CandidateApplicationsPage = () => {
  const { applications, isLoading: isLoadingApps, isError, error, refetch } = useApplications();
  const { jobs } = useJobs();

  // Hydrate application records with full job objects from context or mock
  const hydratedApplications = (applications || []).map((app) => {
    const job =
      (jobs || []).find((j) => j.id === app.jobId) ||
      MOCK_APPLICATIONS.find((a) => a.id === app.id)?.job || {
        id: app.jobId,
        title: app.raw?.jobTitle || 'Software Position',
        company: { name: app.raw?.companyName || 'HireAI Partner' },
      };

    return {
      ...app,
      job,
    };
  });

  return (
    <div className="space-y-6 pb-12">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-2xl font-bold font-heading text-foreground">Active Applications</h2>
          <p className="text-xs text-muted-foreground">
            Track status, stage updates, and live ATS match scoring across your active job pipeline.
          </p>
        </div>
        <span className="text-xs font-mono font-bold px-3 py-1 rounded-full bg-gradient-to-r from-[#C63FC5]/15 via-[#F56681]/15 to-[#FC9559]/15 text-foreground border border-[#F56681]/30">
          {hydratedApplications.length} Applications Active
        </span>
      </div>

      {/* Error state banner */}
      {isError && (
        <div className="p-4 rounded-2xl bg-red-500/10 border border-red-500/20 text-red-600 dark:text-red-400 flex items-center justify-between gap-4">
          <div className="flex items-center gap-3">
            <AlertCircle className="w-5 h-5 shrink-0" />
            <div>
              <p className="text-sm font-semibold">Failed to retrieve applications</p>
              <p className="text-xs opacity-80">{error?.message || 'Unable to sync your pipeline with HireAI servers.'}</p>
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

      {isLoadingApps ? (
        <div className="space-y-4">
          {[1, 2, 3].map((idx) => (
            <Card key={idx} className="p-6 space-y-4">
              <div className="flex items-start justify-between">
                <div className="space-y-2">
                  <Skeleton className="h-5 w-48" />
                  <Skeleton className="h-4 w-32" />
                </div>
                <Skeleton className="h-6 w-24 rounded-full" />
              </div>
              <Skeleton className="h-10 w-full rounded-xl" />
            </Card>
          ))}
        </div>
      ) : hydratedApplications.length === 0 ? (
        <div className="py-16 text-center space-y-4 rounded-3xl border border-dashed border-border/80 p-8">
          <div className="w-14 h-14 rounded-2xl bg-gradient-to-br from-[#C63FC5]/10 to-[#F56681]/10 border border-[#F56681]/20 flex items-center justify-center mx-auto text-[#F56681]">
            <Briefcase className="w-7 h-7" />
          </div>
          <div className="space-y-1">
            <h3 className="text-lg font-bold text-foreground">No applications submitted yet</h3>
            <p className="text-xs text-muted-foreground max-w-sm mx-auto">
              Explore open opportunities, match your resume skills with AI, and apply directly.
            </p>
          </div>
          <Link to={ROUTES.CANDIDATE_JOBS}>
            <Button variant="gradient" size="sm" className="font-bold">
              Browse Open Roles
            </Button>
          </Link>
        </div>
      ) : (
        <div className="space-y-4">
          {hydratedApplications.map((app) => (
            <ApplicationCard key={app.id} application={app} />
          ))}
        </div>
      )}
    </div>
  );
};
