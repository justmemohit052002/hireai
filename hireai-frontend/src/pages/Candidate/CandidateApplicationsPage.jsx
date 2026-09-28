import React from 'react';
import { ApplicationCard } from '@/features/candidate/ApplicationCard';
import { useJobs } from '@/context/JobsContext';
import { MOCK_APPLICATIONS } from '@/mock';
import { Sparkles, Briefcase, FileText } from 'lucide-react';
import { Link } from 'react-router-dom';
import { ROUTES } from '@/constants';
import { Button } from '@/components/ui/Button';

export const CandidateApplicationsPage = () => {
  const { applications, jobs, isLoadingApps, fetchApplications } = useJobs();
  const [filter, setFilter] = React.useState('active'); // 'active' | 'all'

  // Fetch latest application state from backend on mount
  React.useEffect(() => {
    fetchApplications();
  }, [fetchApplications]);

  // Hydrate application records with full job objects from context or backend metadata
  const hydratedApplications = applications.map((app) => {
    const matchedJob = jobs.find((j) => j.id === app.jobId);
    const mockJob = MOCK_APPLICATIONS.find((a) => a.id === app.id)?.job;
    const companyTitle = app.companyName || app.raw?.companyName || matchedJob?.company?.name || 'HireAI Partner';
    const jobTitle = app.jobTitle || app.raw?.jobTitle || matchedJob?.title || 'Software Position';

    const job = matchedJob ? {
      ...matchedJob,
      title: matchedJob.title || jobTitle,
      company: matchedJob.company?.name ? matchedJob.company : { name: companyTitle },
    } : {
      id: app.jobId,
      title: jobTitle,
      company: { name: companyTitle },
      status: 'OPEN',
      listingStatus: 'active',
      ...(mockJob || {}),
    };

    return {
      ...app,
      job,
    };
  }).sort((a, b) => {
    const timeA = new Date(a.appliedAt || a.createdAt || 0).getTime();
    const timeB = new Date(b.appliedAt || b.createdAt || 0).getTime();
    return timeB - timeA;
  });

  const activeApplications = hydratedApplications.filter((a) => a.status !== 'withdrawn');
  const displayedApplications = filter === 'active' ? activeApplications : hydratedApplications;

  return (
    <div className="space-y-6 pb-12">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-2xl font-bold font-heading text-foreground">Active Applications</h2>
          <p className="text-xs text-muted-foreground">
            Track status, stage updates, and live ATS match scoring across your active job pipeline.
          </p>
        </div>
        <div className="flex items-center gap-2">
          {hydratedApplications.some((a) => a.status === 'withdrawn') && (
            <div className="flex items-center bg-muted/60 p-1 rounded-xl text-xs font-semibold">
              <button
                onClick={() => setFilter('active')}
                className={`px-3 py-1 rounded-lg transition-all ${
                  filter === 'active'
                    ? 'bg-background text-foreground shadow-sm'
                    : 'text-muted-foreground hover:text-foreground'
                }`}
              >
                Active ({activeApplications.length})
              </button>
              <button
                onClick={() => setFilter('all')}
                className={`px-3 py-1 rounded-lg transition-all ${
                  filter === 'all'
                    ? 'bg-background text-foreground shadow-sm'
                    : 'text-muted-foreground hover:text-foreground'
                }`}
              >
                All ({hydratedApplications.length})
              </button>
            </div>
          )}
          <span className="text-xs font-mono font-bold px-3 py-1 rounded-full bg-brand-blue-light/30 text-brand-navy dark:text-brand-blue-light border border-brand-blue/20">
            {activeApplications.length} Applications Active
          </span>
        </div>
      </div>

      {displayedApplications.length === 0 ? (
        <div className="py-16 text-center space-y-4 rounded-3xl border border-dashed border-border/80 p-8">
          <div className="w-14 h-14 rounded-2xl bg-brand-blue-light/20 border border-brand-blue/20 flex items-center justify-center mx-auto text-brand-blue">
            <Briefcase className="w-7 h-7" />
          </div>
          <div className="space-y-1">
            <h3 className="text-lg font-bold text-foreground">
              {filter === 'active' && hydratedApplications.length > 0
                ? 'No active applications'
                : 'No applications submitted yet'}
            </h3>
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
          {displayedApplications.map((app) => (
            <ApplicationCard key={app.id} application={app} />
          ))}
        </div>
      )}
    </div>
  );
};
