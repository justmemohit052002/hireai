import React from 'react';
import { ApplicationCard } from '@/features/candidate/ApplicationCard';
import { useJobs } from '@/context/JobsContext';
import { MOCK_APPLICATIONS } from '@/mock';
import { Sparkles, Briefcase, FileText } from 'lucide-react';
import { Link } from 'react-router-dom';
import { ROUTES } from '@/constants';
import { Button } from '@/components/ui/Button';

export const CandidateApplicationsPage = () => {
  const { applications, jobs, isLoadingApps } = useJobs();

  // Hydrate application records with full job objects from context or mock
  const hydratedApplications = applications.map((app) => {
    const job =
      jobs.find((j) => j.id === app.jobId) ||
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
        <span className="text-xs font-mono font-bold px-3 py-1 rounded-full bg-brand-blue-light/30 text-brand-navy dark:text-brand-blue-light border border-brand-blue/20">
          {hydratedApplications.length} Applications Active
        </span>
      </div>

      {hydratedApplications.length === 0 ? (
        <div className="py-16 text-center space-y-4 rounded-3xl border border-dashed border-border/80 p-8">
          <div className="w-14 h-14 rounded-2xl bg-brand-blue-light/20 border border-brand-blue/20 flex items-center justify-center mx-auto text-brand-blue">
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
