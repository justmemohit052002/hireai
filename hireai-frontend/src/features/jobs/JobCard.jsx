import React from 'react';
import {
  MapPin,
  Clock,
  Building2,
  ChevronRight,
  Send,
  Eye,
  DollarSign,
  Briefcase
} from 'lucide-react';
import { Card } from '@/components/ui/Card';
import { Badge } from '@/components/ui/Badge';
import { Button } from '@/components/ui/Button';
import { formatRelativeTime, formatSalary, getJobTypeLabel, getExperienceLevelLabel } from '@/utils';

export const JobCard = ({ job, onSelect, onApply, isPaused = false }) => {
  const [showPausedBanner, setShowPausedBanner] = React.useState(false);

  const companyName = job.company?.name || job.companyName || 'HireAI Partner';
  const companyInitial = companyName ? companyName[0].toUpperCase() : 'H';
  const salaryDisplay = formatSalary(job.salary);

  return (
    <div className="flex flex-col w-full">
      <Card
        hoverable
        className="flex flex-col md:flex-row md:items-center gap-5 justify-between group relative overflow-hidden p-5 transition-all cursor-pointer"
        onClick={() => onSelect && onSelect(job)}
      >
        {/* Top Banner Accent */}
        <div className="absolute top-0 inset-x-0 h-1 bg-gradient-to-r from-[#C63FC5] via-[#F56681] to-[#FC9559] opacity-0 group-hover:opacity-100 transition-opacity" />

        {/* Left / Main info area */}
        <div className="flex-1 min-w-0">
          {/* Header */}
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 mb-2">
            <div className="flex items-center gap-3">
              <div className="w-11 h-11 rounded-2xl bg-gradient-to-br from-[#C63FC5]/15 via-[#F56681]/15 to-[#FC9559]/15 border border-[#F56681]/30 flex items-center justify-center font-bold text-foreground shrink-0 text-base shadow-sm">
                {companyInitial}
              </div>
              <div className="min-w-0">
                <div className="flex items-center gap-2 flex-wrap">
                  <h3 className="font-bold text-base sm:text-lg font-heading text-foreground group-hover:text-primary transition-colors line-clamp-1">
                    {job.title}
                  </h3>
                  <Badge variant="default" className="shrink-0 font-mono text-[10px]">
                    {getJobTypeLabel(job.type) || job.jobType || 'Full-time'}
                  </Badge>
                  <span className="text-[10px] font-semibold px-2 py-0.5 rounded-md border border-border/60 bg-surface-2 text-muted-foreground capitalize">
                    {job.workplaceType || 'Hybrid'}
                  </span>
                  {isPaused && (
                    <span className="text-[10px] font-bold px-2 py-0.5 rounded-md bg-amber-500/15 text-amber-400 border border-amber-500/30">
                      Paused
                    </span>
                  )}
                </div>
                <p className="text-xs text-muted-foreground font-medium flex items-center gap-1.5 mt-0.5">
                  <Building2 className="w-3.5 h-3.5 text-primary" />
                  <span>{companyName}</span>
                  {job.department && <span>• {job.department}</span>}
                </p>
              </div>
            </div>
          </div>

          {/* Description snippet */}
          <p className="text-xs text-muted-foreground line-clamp-2 leading-relaxed mb-3">
            {job.description || 'Join our innovative engineering team to build state-of-the-art products.'}
          </p>

          {/* Skills Tag Cloud */}
          <div className="flex flex-wrap gap-1.5">
            {job.skills && job.skills.slice(0, 4).map((skill, idx) => (
              <span
                key={idx}
                className="text-[10px] font-semibold px-2 py-0.5 rounded-md bg-surface-2 text-foreground border border-border/50"
              >
                {skill}
              </span>
            ))}
            {job.skills && job.skills.length > 4 && (
              <span className="text-[10px] font-medium px-1.5 py-0.5 text-muted-foreground">
                +{job.skills.length - 4} more
              </span>
            )}
          </div>
        </div>

        {/* Right area metadata + actions */}
        <div className="flex flex-col sm:flex-row md:flex-col items-start sm:items-center md:items-end justify-between md:justify-center gap-3 shrink-0 pt-3 md:pt-0 border-t md:border-t-0 md:border-l border-border/50 md:pl-5 min-w-[210px]">
          {/* Metadata Pills */}
          <div className="flex flex-col gap-1.5 text-xs text-muted-foreground w-full md:items-end">
            <div className="flex items-center gap-2 flex-wrap md:justify-end">
              <div className="flex items-center gap-1.5 bg-surface-2/80 px-2.5 py-1 rounded-lg border border-border/40 text-xs text-muted-foreground">
                <MapPin className="w-3.5 h-3.5 text-primary shrink-0" />
                <span className="truncate max-w-[130px]">{job.location || 'Remote'}</span>
              </div>
              <div className="flex items-center gap-1 bg-surface-2/80 px-2.5 py-1 rounded-lg border border-border/40 font-mono font-bold text-foreground text-xs">
                <span>{salaryDisplay}</span>
              </div>
            </div>
            <div className="flex items-center gap-2 flex-wrap text-[10px] text-muted-foreground px-1 md:justify-end">
              <span className="flex items-center">
                <Clock className="w-3 h-3 mr-1" />
                {formatRelativeTime(job.postedAt)}
              </span>
              {job.applicationDeadline && (
                <span className="text-amber-500 font-semibold">
                  • Apply by: {job.applicationDeadline}
                </span>
              )}
            </div>
          </div>

          {/* Action Buttons: View Details (Drawer) & Apply */}
          <div className="flex flex-col items-start md:items-end gap-2 w-full mt-1">
            {isPaused && showPausedBanner && (
              <div className="w-full text-[11px] font-medium text-amber-400 bg-amber-500/10 border border-amber-500/20 rounded-lg px-2.5 py-1.5 text-center">
                Position is paused and not accepting applications.
              </div>
            )}
            <div className="flex items-center gap-2 w-full md:justify-end">
              <Button
                size="sm"
                variant="outline"
                className="text-xs h-8 px-3 gap-1"
                onClick={(e) => {
                  e.stopPropagation();
                  if (onSelect) onSelect(job);
                }}
              >
                <Eye className="w-3.5 h-3.5" />
                <span>Details</span>
              </Button>

              {isPaused ? (
                <button
                  type="button"
                  className="inline-flex items-center gap-1 text-xs font-semibold px-3 py-1.5 rounded-lg bg-surface-3 text-muted-foreground cursor-not-allowed h-8"
                  onClick={(e) => {
                    e.stopPropagation();
                    setShowPausedBanner(true);
                    setTimeout(() => setShowPausedBanner(false), 3500);
                  }}
                >
                  Paused
                </button>
              ) : (
                <Button
                  size="sm"
                  variant="gradient"
                  className="font-bold text-xs h-8 px-3.5 shadow-md"
                  onClick={(e) => {
                    e.stopPropagation();
                    if (onApply) onApply(job);
                  }}
                >
                  <span>Apply Now</span>
                  <ChevronRight className="w-3.5 h-3.5 ml-0.5" />
                </Button>
              )}
            </div>
          </div>
        </div>
      </Card>
    </div>
  );
};
