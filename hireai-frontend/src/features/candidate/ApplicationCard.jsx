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
    badgeStyle: 'bg-[#6D3DF5]/15 text-[#6D3DF5] dark:text-[#A78BFA] border-[#6D3DF5]/30',
    icon: Search,
  },
  shortlisted: {
    label: 'Shortlisted',
    badgeStyle: 'bg-indigo-500/15 text-indigo-500 border-indigo-500/30',
    icon: Sparkles,
  },
  interview: {
    label: 'Interview Invitation',
    badgeStyle: 'bg-pink-500/10 text-pink-500 border-pink-500/20 dark:bg-pink-500/20 dark:border-pink-500/30',
    icon: Sparkles,
  },
  offer: {
    label: 'Offer Extended',
    badgeStyle: 'bg-emerald-500/15 text-emerald-500 border-emerald-500/30 dark:bg-emerald-500/20 dark:border-emerald-500/40',
    icon: Award,
  },
  rejected: {
    label: 'Application Closed',
    badgeStyle: 'bg-red-500/10 text-red-500 border-red-500/20 dark:bg-red-500/20 dark:border-red-500/30',
    icon: XCircle,
  },
  withdrawn: {
    label: 'Withdrawn',
    badgeStyle: 'bg-slate-500/10 text-slate-400 border-slate-500/20 dark:bg-slate-500/20 dark:border-slate-500/30',
    icon: XCircle,
  },
  closed: {
    label: 'Position Closed',
    badgeStyle: 'bg-slate-500/10 text-slate-400 border-slate-500/20 dark:bg-slate-500/20 dark:border-slate-500/30',
    icon: XCircle,
  },
};

export const ApplicationCard = ({ application }) => {
  const navigate = useNavigate();
  const { job, status, appliedAt, updatedAt, aiScore, atsMatchScore } = application;
  const companyName = job?.company?.name || job?.companyName || 'HireAI Employer';
  const jobTitle = job?.title || 'Applied Position';

  const currentStage = STAGE_CONFIG[status] || {
    label: 'Application Received',
    badgeStyle: 'bg-blue-500/10 text-blue-500 border-blue-500/20',
    icon: Clock,
  };

  const StageIcon = currentStage.icon;
  const lastUpdated = updatedAt || appliedAt;
  const matchPercent = aiScore || (atsMatchScore ? Math.round(atsMatchScore) : 85);

  return (
    <Card hoverable className="p-5 flex flex-col sm:flex-row sm:items-center justify-between gap-6">
      {/* Job Info */}
      <div className="flex items-center gap-4 shrink-0">
        <div className="w-12 h-12 rounded-2xl bg-gradient-to-br from-[#C63FC5]/20 to-[#F56681]/20 border border-[#C63FC5]/30 flex items-center justify-center font-bold text-lg text-[#C63FC5] dark:text-[#F56681] shrink-0 shadow-sm">
          {companyName[0] || 'H'}
        </div>
        <div>
          <h3 className="font-bold text-base font-heading text-foreground">{jobTitle}</h3>
          <p className="text-xs text-muted-foreground font-medium flex items-center gap-1.5 mt-0.5">
            <Building2 className="w-3.5 h-3.5 text-[#F56681]" />
            {companyName} • Applied on {formatShortDate(appliedAt)}
          </p>
        </div>
      </div>

      {/* Dynamic Current Stage Badge, ATS Score & Chat Action */}
      <div className="flex flex-col sm:items-end gap-2.5 shrink-0">
        <div className="flex items-center gap-2 flex-wrap">
          {matchPercent && (
            <span className="inline-flex items-center gap-1 text-[11px] font-mono font-bold px-2.5 py-1 rounded-full bg-gradient-to-r from-[#C63FC5]/10 via-[#F56681]/10 to-[#FC9559]/10 border border-[#F56681]/30 text-foreground">
              <Sparkles className="w-3 h-3 text-[#F56681]" /> {matchPercent}% ATS Match
            </span>
          )}
          <div className={`inline-flex items-center gap-1.5 px-3.5 py-1 rounded-full border text-xs font-bold shadow-sm ${currentStage.badgeStyle}`}>
            <StageIcon className="w-3.5 h-3.5" />
            <span>{currentStage.label}</span>
          </div>
          <Button
            size="sm"
            variant="outline"
            onClick={() => navigate(ROUTES.CANDIDATE_INBOX)}
            className="h-7 text-xs border-blue-500/30 text-blue-500 hover:bg-blue-500/10 flex items-center gap-1 px-2.5"
          >
            <MessageSquare className="w-3 h-3" />
            <span>Chat</span>
          </Button>
        </div>
        
        <p className="text-[11px] text-muted-foreground flex items-center gap-1 font-medium">
          <Clock className="w-3 h-3 text-muted-foreground/70" />
          <span>Status updated {formatRelativeTime(lastUpdated)}</span>
        </p>
      </div>
    </Card>
  );
};
