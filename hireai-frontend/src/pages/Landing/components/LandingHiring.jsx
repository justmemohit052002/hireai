import React from 'react';
import { Link } from 'react-router-dom';
import { 
  UserCheck, 
  Briefcase, 
  UserPlus, 
  Sparkles, 
  FileCheck, 
  BadgeCheck, 
  ArrowRight,
  ShieldCheck,
  CheckCircle2
} from 'lucide-react';
import { Button } from '@/components/ui/Button';
import { ROUTES } from '@/constants';

export const LandingHiring = () => {
  return (
    <section id="hiring" className="py-14 sm:py-20 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
      {/* Heading */}
      <div className="text-center max-w-2xl mx-auto mb-12 sm:mb-16">
        <span className="text-xs font-mono font-bold uppercase tracking-wider text-brand-blue">
          Dual-Sided Platform
        </span>
        <h2 className="text-2xl sm:text-3xl lg:text-4xl font-extrabold font-heading text-foreground tracking-tight mt-2">
          Tailored for Engineering Teams & Candidates
        </h2>
        <p className="text-sm sm:text-base text-muted-foreground mt-2 font-sans">
          Purpose-built recruitment infrastructure designed to connect technical hiring managers with exceptional developers.
        </p>
      </div>

      {/* Split Comparison Cards */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8 items-stretch">
        {/* Recruiter Card */}
        <div className="p-8 sm:p-10 rounded-2xl bg-surface border border-border shadow-sm flex flex-col justify-between space-y-8 text-left hover:border-brand-blue/40 transition-all">
          <div className="space-y-4">
            <div className="flex items-center justify-between">
              <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-muted text-foreground text-xs font-mono font-bold uppercase">
                <Briefcase className="w-3.5 h-3.5 text-brand-blue" />
                For Recruiters & Engineering Leads
              </span>
              <span className="text-xs font-semibold text-emerald-600 dark:text-emerald-400">
                ● Active Sourcing
              </span>
            </div>

            <h3 className="text-2xl sm:text-3xl font-extrabold font-heading text-foreground">
              Build Your Engineering Dream Team
            </h3>

            <p className="text-sm text-muted-foreground leading-relaxed font-sans">
              Post technical roles, let neural algorithms rank applicant code competencies, and shortlist top talent in minutes without manual triage.
            </p>

            {/* Feature List */}
            <div className="space-y-3 pt-2">
              {[
                { title: 'Deterministic & Semantic ATS Scoring (0–100%)', icon: Sparkles },
                { title: 'Automated Candidate Shortlist Thresholds', icon: UserCheck },
                { title: 'Integrated In-App Messaging & Interview Notes', icon: CheckCircle2 },
                { title: 'Multi-Format Resume Parsing (.pdf, .docx in 1.2s)', icon: FileCheck },
              ].map((item) => {
                const Icon = item.icon;
                return (
                  <div key={item.title} className="flex items-center gap-3 text-xs sm:text-sm font-medium text-foreground">
                    <div className="w-6 h-6 rounded-lg bg-brand-blue/10 text-brand-blue flex items-center justify-center shrink-0">
                      <Icon className="w-3.5 h-3.5" />
                    </div>
                    <span>{item.title}</span>
                  </div>
                );
              })}
            </div>
          </div>

          <div className="pt-6 border-t border-border">
            <Link to={ROUTES.SIGNUP}>
              <Button className="w-full bg-brand-navy text-white hover:bg-brand-blue py-3 rounded-xl text-sm font-bold transition flex items-center justify-center gap-2">
                <UserPlus className="w-4 h-4" />
                <span>Create Recruiter Workspace</span>
                <ArrowRight className="w-4 h-4" />
              </Button>
            </Link>
          </div>
        </div>

        {/* Candidate Card */}
        <div className="p-8 sm:p-10 rounded-2xl bg-surface border border-border shadow-sm flex flex-col justify-between space-y-8 text-left hover:border-brand-blue/40 transition-all">
          <div className="space-y-4">
            <div className="flex items-center justify-between">
              <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-muted text-foreground text-xs font-mono font-bold uppercase">
                <UserCheck className="w-3.5 h-3.5 text-brand-blue" />
                For Software Engineers & Candidates
              </span>
              <span className="text-xs font-semibold text-brand-blue">
                100% Free For Talent
              </span>
            </div>

            <h3 className="text-2xl sm:text-3xl font-extrabold font-heading text-foreground">
              Land Roles Matching Your Real Skills
            </h3>

            <p className="text-sm text-muted-foreground leading-relaxed font-sans">
              Showcase verified technical competencies, receive live match transparency before applying, and get shortlisted by top companies.
            </p>

            {/* Feature List */}
            <div className="space-y-3 pt-2">
              {[
                { title: '1-Click Resume Semantic Matching & Application', icon: FileCheck },
                { title: 'Transparent Skill Gap Feedback on Every Job', icon: BadgeCheck },
                { title: 'Live Stage Progression (Applied → Screened → Offer)', icon: CheckCircle2 },
                { title: 'Direct Chat with Hiring Managers & HR Teams', icon: UserCheck },
              ].map((item) => {
                const Icon = item.icon;
                return (
                  <div key={item.title} className="flex items-center gap-3 text-xs sm:text-sm font-medium text-foreground">
                    <div className="w-6 h-6 rounded-lg bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 flex items-center justify-center shrink-0">
                      <Icon className="w-3.5 h-3.5" />
                    </div>
                    <span>{item.title}</span>
                  </div>
                );
              })}
            </div>
          </div>

          <div className="pt-6 border-t border-border">
            <Link to={ROUTES.SIGNUP}>
              <Button variant="gradient" className="w-full font-bold py-3 rounded-xl text-sm transition flex items-center justify-center gap-2">
                <BadgeCheck className="w-4 h-4" />
                <span>Create Candidate Profile</span>
                <ArrowRight className="w-4 h-4" />
              </Button>
            </Link>
          </div>
        </div>
      </div>
    </section>
  );
};
