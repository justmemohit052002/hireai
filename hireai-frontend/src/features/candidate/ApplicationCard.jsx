import React from 'react';
import { 
  Building2, 
  FileCheck, 
  Search, 
  Sparkles, 
  Award, 
  XCircle, 
  Clock,
  MessageSquare
} from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { Card } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { formatShortDate, formatRelativeTime } from '@/utils';
import { ROUTES } from '@/constants';
import { useJobs } from '@/context/JobsContext';

const STAGE_CONFIG = {
  applied: {
    label: 'Application Received',
    badgeStyle: 'bg-blue-500/10 text-blue-500 border-blue-500/20 dark:bg-blue-500/20 dark:border-blue-500/30',
    icon: FileCheck,
  },
  screening: {
    label: 'Under Review',
    badgeStyle: 'bg-amber-500/10 text-amber-500 border-amber-500/20 dark:bg-amber-500/20 dark:border-amber-500/30',
    icon: Search,
  },
  technical: {
    label: 'Technical Evaluation',
    badgeStyle: 'bg-brand-blue-light/20 text-brand-navy border-brand-blue/30',
    icon: Search,
  },
  shortlisted: {
    label: 'Shortlisted',
    badgeStyle: 'bg-brand-blue/15 text-brand-blue border-brand-blue/30',
    icon: Sparkles,
  },
  interview: {
    label: 'Interview Invitation',
    badgeStyle: 'bg-brand-blue-light/30 text-brand-blue border-brand-blue/30 dark:bg-brand-blue/20 dark:border-brand-blue/30',
    icon: Sparkles,
  },
  interview_scheduled: {
    label: 'Interview Scheduled',
    badgeStyle: 'bg-brand-blue-light/30 text-brand-blue border-brand-blue/30 dark:bg-brand-blue/20 dark:border-brand-blue/30',
    icon: Sparkles,
  },
  offer: {
    label: 'Offer Extended',
    badgeStyle: 'bg-emerald-500/15 text-emerald-500 border-emerald-500/30 dark:bg-emerald-500/20 dark:border-emerald-500/40',
    icon: Award,
  },
  offered: {
    label: 'Offer Extended',
    badgeStyle: 'bg-emerald-500/15 text-emerald-500 border-emerald-500/30 dark:bg-emerald-500/20 dark:border-emerald-500/40',
    icon: Award,
  },
  rejected: {
    label: 'Not Selected',
    badgeStyle: 'bg-red-500/10 text-red-500 border-red-500/20 dark:bg-red-500/20 dark:border-red-500/30',
    icon: XCircle,
  },
  withdrawn: {
    label: 'Withdrawn',
    badgeStyle: 'bg-slate-500/10 text-muted-foreground border-slate-500/20 dark:bg-slate-500/20 dark:border-slate-500/30',
    icon: XCircle,
  },
  closed: {
    label: 'Position Closed',
    badgeStyle: 'bg-slate-500/10 text-muted-foreground border-slate-500/20 dark:bg-slate-500/20 dark:border-slate-500/30',
    icon: XCircle,
  },
};

export const ApplicationCard = ({ application }) => {
  const navigate = useNavigate();
  const { withdrawApplication } = useJobs();
  const [isWithdrawing, setIsWithdrawing] = React.useState(false);

  const { job, status, appliedAt, updatedAt, aiScore, atsMatchScore } = application;
  const companyName = job?.company?.name || job?.companyName || application.companyName || 'HireAI Employer';
  const jobTitle = job?.title || job?.jobTitle || application.jobTitle || 'Applied Position';

  const isJobClosed = job?.status === 'CLOSED' || job?.listingStatus === 'closed' || job?.status === 'closed';
  const stageKey = (status || 'applied').toLowerCase();
  const currentStage = isJobClosed
    ? STAGE_CONFIG.closed
    : (STAGE_CONFIG[stageKey] || STAGE_CONFIG.applied);

  const StageIcon = currentStage.icon;
  const lastUpdated = updatedAt || appliedAt;
  const matchPercent = aiScore || (atsMatchScore ? Math.round(atsMatchScore) : null);

  const handleWithdraw = async () => {
    if (window.confirm(`Are you sure you want to withdraw your application for "${jobTitle}"?`)) {
      setIsWithdrawing(true);
      try {
        await withdrawApplication(application.id);
      } finally {
        setIsWithdrawing(false);
      }
    }
  };

  return (
    <Card hoverable className="p-4 sm:p-5 flex flex-col sm:flex-row sm:items-center justify-between gap-4 sm:gap-6">
      {/* Job Info */}
      <div className="flex items-center gap-3 sm:gap-4 min-w-0 flex-1">
        <div className="w-11 h-11 sm:w-12 sm:h-12 rounded-xl bg-brand-blue-light/30 border border-brand-blue/20 flex items-center justify-center font-bold text-base sm:text-lg text-brand-navy dark:text-brand-blue-light shrink-0 shadow-sm">
          {companyName[0] || 'H'}
        </div>
        <div className="min-w-0 flex-1">
          <h3 className="font-bold text-base font-heading text-foreground truncate">{jobTitle}</h3>
          <p className="text-xs text-muted-foreground font-medium flex items-center gap-1.5 mt-0.5 truncate">
            <Building2 className="w-3.5 h-3.5 text-brand-blue shrink-0" />
            <span className="truncate">{companyName} • Applied on {formatShortDate(appliedAt)}</span>
          </p>
        </div>
      </div>

      {/* Dynamic Current Stage Badge, ATS Score & Chat Action */}
      <div className="flex flex-col sm:items-end gap-2.5 w-full sm:w-auto shrink-0">
        <div className="flex items-center gap-2 flex-wrap">
          {matchPercent && (
            <span className="inline-flex items-center gap-1 text-[11px] font-mono font-bold px-2.5 py-1 rounded-lg bg-brand-blue-light/20 border border-brand-blue/20 text-foreground">
              <Sparkles className="w-3 h-3 text-brand-blue" /> {matchPercent}% ATS Match
            </span>
          )}
          <div className={`inline-flex items-center gap-1.5 px-3 py-1 rounded-lg border text-xs font-bold shadow-sm ${currentStage.badgeStyle}`}>
            <StageIcon className="w-3.5 h-3.5" />
            <span>{currentStage.label}</span>
          </div>
          <Button
            size="sm"
            variant="outline"
            onClick={() => navigate(ROUTES.CANDIDATE_INBOX)}
            className="h-7 text-xs border-brand-blue/30 text-brand-blue hover:bg-brand-blue-light/20 flex items-center gap-1 px-2.5 rounded-lg"
          >
            <MessageSquare className="w-3 h-3" />
            <span>Chat</span>
          </Button>

          {status !== 'withdrawn' && !isJobClosed && (
            <Button
              size="sm"
              variant="ghost"
              onClick={handleWithdraw}
              disabled={isWithdrawing}
              className="h-7 text-xs text-muted-foreground hover:text-red-500 hover:bg-red-500/10 px-2 font-medium"
              title="Withdraw Application"
            >
              {isWithdrawing ? 'Withdrawing...' : 'Withdraw'}
            </Button>
          )}
        </div>
        
        <p className="text-[11px] text-muted-foreground flex items-center gap-1 font-medium">
          <Clock className="w-3 h-3 text-muted-foreground/70" />
          <span>Status updated {formatRelativeTime(lastUpdated)}</span>
        </p>
      </div>
    </Card>
  );
};
