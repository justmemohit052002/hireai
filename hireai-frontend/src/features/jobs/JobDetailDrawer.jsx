import React from 'react';
import {
  MapPin,
  Clock,
  Building2,
  CheckCircle2,
  Sparkles,
  Send,
  Briefcase,
  Layers,
  Calendar,
  DollarSign
} from 'lucide-react';
import { Drawer } from '@/components/ui/Drawer';
import { Button } from '@/components/ui/Button';
import { Badge } from '@/components/ui/Badge';
import { formatSalary, formatRelativeTime, getJobTypeLabel, getExperienceLevelLabel } from '@/utils';

export const JobDetailDrawer = ({
  job,
  isOpen,
  onClose,
  onApply,
  isPaused = false,
}) => {
  if (!job) return null;

  const companyName = job.company?.name || job.companyName || 'HireAI Partner';
  const companyInitial = companyName ? companyName[0].toUpperCase() : 'H';
  const salaryText = formatSalary(job.salary);

  const headerNode = (
    <div className="space-y-3.5 pr-8">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div className="flex items-center gap-3.5 min-w-0">
          <div className="w-12 h-12 sm:w-13 sm:h-13 rounded-2xl bg-brand-navy/10 dark:bg-brand-blue/15 border border-brand-blue/30 flex items-center justify-center font-bold text-xl sm:text-2xl text-brand-blue shrink-0 shadow-xs">
            {companyInitial}
          </div>
          <div className="min-w-0">
            <h2 className="text-xl sm:text-2xl font-bold font-heading text-foreground truncate">{job.title}</h2>
            <p className="text-xs sm:text-sm font-medium text-muted-foreground flex items-center gap-1.5 mt-0.5">
              <Building2 className="w-4 h-4 text-brand-blue shrink-0" />
              <span className="font-semibold text-foreground">{companyName}</span>
              {job.department && <span>• {job.department}</span>}
            </p>
          </div>
        </div>

        <div className="flex flex-col items-start sm:items-end gap-1 shrink-0">
          <div className="flex items-center gap-1.5 font-mono font-bold text-foreground text-sm bg-surface-2 px-3 py-1.5 rounded-xl border border-border">
            <DollarSign className="w-4 h-4 text-emerald-500" />
            <span>{salaryText}</span>
          </div>
          {job.applicationDeadline && (
            <span className="text-[11px] text-amber-500 font-semibold">
              Apply before: {job.applicationDeadline}
            </span>
          )}
        </div>
      </div>

      {/* Quick Badges */}
      <div className="flex flex-wrap items-center gap-2 pt-0.5">
        <Badge variant="default">{getJobTypeLabel(job.type) || job.jobType || 'Full-time'}</Badge>
        <Badge variant="secondary" className="capitalize">{job.workplaceType || 'Hybrid'}</Badge>
        <Badge variant="secondary">{getExperienceLevelLabel(job.level) || job.experienceLevel || 'Mid Level'}</Badge>
        <div className="inline-flex items-center gap-1 px-2.5 py-1 rounded-lg bg-surface-2 border border-border text-xs text-muted-foreground">
          <MapPin className="w-3.5 h-3.5 text-brand-blue" />
          <span>{job.location || 'Remote'}</span>
        </div>
        {job.postedAt && (
          <div className="inline-flex items-center gap-1 px-2.5 py-1 rounded-lg bg-surface-2 border border-border text-xs text-muted-foreground">
            <Clock className="w-3.5 h-3.5 text-muted-foreground" />
            <span>{formatRelativeTime(job.postedAt)}</span>
          </div>
        )}
      </div>
    </div>
  );

  const footerNode = (
    <div className="w-full flex items-center justify-end gap-3">
      <Button
        type="button"
        variant="outline"
        size="default"
        onClick={onClose}
        className="px-5 font-medium"
      >
        Close
      </Button>
      {isPaused ? (
        <Button disabled variant="outline" size="default" className="cursor-not-allowed opacity-60">
          Applications Paused
        </Button>
      ) : (
        <Button
          type="button"
          onClick={() => {
            onClose();
            onApply(job);
          }}
          size="default"
          variant="default"
          className="font-bold shadow-md px-6 inline-flex items-center gap-2"
        >
          <Send className="w-4 h-4" />
          <span>Apply For This Position</span>
        </Button>
      )}
    </div>
  );

  return (
    <Drawer
      isOpen={isOpen}
      onClose={onClose}
      position="center"
      width="2xl"
      header={headerNode}
      footer={footerNode}
    >
      {/* Skills Required */}
      {job.skills && job.skills.length > 0 && (
        <div className="space-y-2.5">
          <h3 className="text-xs font-bold uppercase tracking-wider text-foreground">Required Tech Stack & Skills</h3>
          <div className="flex flex-wrap gap-1.5">
            {job.skills.map((skill, idx) => (
              <span
                key={idx}
                className="text-xs font-semibold px-3 py-1 rounded-lg bg-surface-2 hover:bg-surface-3 text-foreground border border-border/60"
              >
                {skill}
              </span>
            ))}
          </div>
        </div>
      )}

      {/* About the Role */}
      <div className="space-y-2">
        <h3 className="text-xs font-bold uppercase tracking-wider text-foreground">About the Role</h3>
        <p className="text-xs sm:text-sm text-muted-foreground leading-relaxed whitespace-pre-line">
          {job.description || 'No detailed description provided for this job opening.'}
        </p>
      </div>

      {/* Responsibilities */}
      {job.responsibilities && job.responsibilities.length > 0 && (
        <div className="space-y-2.5">
          <h3 className="text-xs font-bold uppercase tracking-wider text-foreground">Key Responsibilities</h3>
          <ul className="grid grid-cols-1 sm:grid-cols-2 gap-2.5">
            {job.responsibilities.map((item, idx) => (
              <li key={idx} className="flex items-start gap-2.5 text-xs text-muted-foreground bg-surface-2/50 p-2.5 rounded-xl border border-border/50">
                <CheckCircle2 className="w-4 h-4 text-emerald-500 shrink-0 mt-0.5" />
                <span>{item}</span>
              </li>
            ))}
          </ul>
        </div>
      )}

      {/* Requirements */}
      {job.requirements && job.requirements.length > 0 && (
        <div className="space-y-2.5">
          <h3 className="text-xs font-bold uppercase tracking-wider text-foreground">Requirements & Qualifications</h3>
          <ul className="grid grid-cols-1 sm:grid-cols-2 gap-2.5">
            {job.requirements.map((item, idx) => (
              <li key={idx} className="flex items-start gap-2.5 text-xs text-muted-foreground bg-surface-2/50 p-2.5 rounded-xl border border-border/50">
                <CheckCircle2 className="w-4 h-4 text-brand-blue shrink-0 mt-0.5" />
                <span>{item}</span>
              </li>
            ))}
          </ul>
        </div>
      )}

      {/* Perks & Benefits */}
      {job.benefits && job.benefits.length > 0 && (
        <div className="space-y-2.5">
          <h3 className="text-xs font-bold uppercase tracking-wider text-foreground">Perks & Benefits</h3>
          <div className="flex flex-wrap gap-2">
            {job.benefits.map((benefit, idx) => (
              <span key={idx} className="text-xs font-semibold px-3 py-1.5 rounded-xl bg-surface-2 border border-border/60 text-foreground">
                {benefit}
              </span>
            ))}
          </div>
        </div>
      )}
    </Drawer>
  );
};
