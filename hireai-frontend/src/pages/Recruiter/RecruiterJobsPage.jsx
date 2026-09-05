import React from 'react';
import { useNavigate } from 'react-router-dom';
import { Plus, Edit, Trash2, PauseCircle, PlayCircle, Calendar } from 'lucide-react';
import { Card } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { EmptyState } from '@/components/common/EmptyState';
import { formatSalary, formatShortDate, getJobTypeLabel } from '@/utils';
import { ROUTES } from '@/constants';
import { useJobs } from '@/context/JobsContext';

export const RecruiterJobsPage = () => {
  const navigate = useNavigate();
  const { jobs, toggleJobStatus, deleteJob } = useJobs();

  return (
    <div className="space-y-6 pb-12">
      <div className="flex items-center justify-between">
        <div className="space-y-1">
          <h2 className="text-2xl font-bold font-heading text-foreground tracking-tight">Manage Postings</h2>
          <p className="text-xs text-muted-foreground">View, edit, pause, or create new engineering requisitions.</p>
        </div>
        <Button
          variant="gradient"
          onClick={() => navigate(ROUTES.RECRUITER_JOBS_CREATE)}
          className="font-bold shadow-md shadow-[#C63FC5]/20"
        >
          <Plus className="w-4 h-4 mr-1.5" /> Create Position
        </Button>
      </div>

      {jobs.length > 0 ? (
        <div className="space-y-4">
          {jobs.map((job) => (
            <Card
              key={job.id}
              className="p-5 glass border border-border/60 rounded-2xl shadow-xs flex flex-col md:flex-row items-start md:items-center justify-between gap-4"
            >
              <div className="space-y-2">
                <div className="flex items-center gap-2.5 flex-wrap">
                  <h3 className="text-base font-bold text-foreground">{job.title}</h3>
                  {/* Listing status badge */}
                  <span
                    className={`text-[11px] font-bold uppercase tracking-wider px-2.5 py-0.5 rounded-full ${
                      job.listingStatus === 'open'
                        ? 'bg-emerald-500/15 text-emerald-600 dark:text-emerald-400 border border-emerald-500/30'
                        : job.listingStatus === 'paused'
                        ? 'bg-amber-500/15 text-amber-600 dark:text-amber-400 border border-amber-500/30'
                        : 'bg-slate-500/15 text-slate-500 border border-slate-500/30'
                    }`}
                  >
                    {job.listingStatus}
                  </span>
                  <span className="bg-surface-2 text-muted-foreground border border-border/60 text-[12px] font-medium px-2.5 py-0.5 rounded-md capitalize">
                    {job.workplaceType || 'hybrid'}
                  </span>
                </div>

                <div className="flex items-center gap-4 text-xs font-medium text-muted-foreground flex-wrap">
                  <span>{getJobTypeLabel(job.type)}</span>
                  <span>•</span>
                  <span>{formatSalary(job.salary)}</span>
                  <span>•</span>
                  <span>Posted {formatShortDate(job.postedAt)}</span>
                  {job.applicationDeadline && (
                    <>
                      <span>•</span>
                      <span className="flex items-center gap-1">
                        <Calendar className="w-3.5 h-3.5 text-muted-foreground shrink-0" />
                        <span className="leading-none">Deadline: {job.applicationDeadline}</span>
                      </span>
                    </>
                  )}
                </div>
              </div>

              <div className="flex items-center gap-6 w-full md:w-auto justify-between md:justify-end border-t md:border-t-0 pt-3 md:pt-0 border-border/60">
                <div className="flex items-center gap-6 text-right shrink-0">
                  <div>
                    <div className="text-[11px] font-bold tracking-wider uppercase text-muted-foreground">Applicants</div>
                    <div className="text-base font-bold text-foreground mt-0.5">{job.applicantsCount}</div>
                  </div>
                  <div>
                    <div className="text-[11px] font-bold tracking-wider uppercase text-[#F56681]">AI Parsed</div>
                    <div className="text-base font-bold text-[#F56681] mt-0.5">{Math.floor(job.applicantsCount * 0.94)}</div>
                  </div>
                </div>

                <div className="flex items-center gap-1.5 shrink-0">
                  <Button
                    size="sm"
                    variant="ghost"
                    onClick={() => toggleJobStatus(job.id)}
                    title={job.listingStatus === 'open' ? 'Pause Requisition' : 'Resume Requisition'}
                  >
                    {job.listingStatus === 'open' ? (
                      <PauseCircle className="w-4 h-4 text-amber-500" />
                    ) : (
                      <PlayCircle className="w-4 h-4 text-emerald-500" />
                    )}
                  </Button>
                  <Button
                    size="sm"
                    variant="ghost"
                    title="Edit Job"
                    onClick={() => navigate(`/recruiter/jobs/edit/${job.id}`)}
                  >
                    <Edit className="w-4 h-4 text-muted-foreground" />
                  </Button>
                  <Button
                    size="sm"
                    variant="ghost"
                    onClick={() => deleteJob(job.id)}
                    title="Delete Job"
                    className="text-red-500 hover:text-red-600 hover:bg-red-500/10"
                  >
                    <Trash2 className="w-4 h-4" />
                  </Button>
                </div>
              </div>
            </Card>
          ))}
        </div>
      ) : (
        <EmptyState
          title="No Job Postings Yet"
          description="You haven't created any job requisitions. Publish your first opening to start collecting applicants."
          actionLabel="Create Position"
          onAction={() => navigate(ROUTES.RECRUITER_JOBS_CREATE)}
        />
      )}
    </div>
  );
};
