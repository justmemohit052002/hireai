import React from 'react';
import { Link } from 'react-router-dom';
import { 
  UserCheck, 
  UserPlus, 
  Briefcase, 
  Sparkles, 
  BadgeCheck, 
  Clock, 
  Users, 
  ArrowRight 
} from 'lucide-react';
import { Button } from '@/components/ui/Button';
import { ROUTES } from '@/constants';

export const LandingHero = () => {
  return (
    <section className="relative overflow-hidden bg-gradient-to-r from-[#6D3DF5]/10 via-[#C63FC5]/10 to-[#FC9559]/10 py-16 lg:py-24 px-6 md:px-10 rounded-3xl max-w-7xl mx-auto my-4 border border-border/40">
      <div className="mx-auto max-w-7xl">
        <div className="grid lg:grid-cols-2 gap-12 items-center">
          {/* Left Content */}
          <div className="text-left space-y-6">
            {/* Hiring Badge */}
            <div className="inline-flex items-center gap-2.5 bg-brand-blue-light/20 text-brand-navy dark:bg-brand-blue-light/15 dark:text-brand-blue-light border border-[#6D3DF5]/30 rounded-full px-4 py-1.5 backdrop-blur-md text-xs sm:text-sm font-semibold shadow-xs">
              <span className="w-2.5 h-2.5 rounded-full bg-brand-accent text-brand-dark animate-pulse" />
              <UserCheck className="w-4 h-4 text-brand-blue" />
              <span>All-In-One Candidate Hiring & ATS Platform</span>
            </div>

            {/* Heading */}
            <h1 className="text-4xl sm:text-5xl lg:text-6xl font-extrabold font-heading leading-tight text-foreground tracking-tight">
              Simplify Hiring.
              <br />
              Empower{' '}
              <span className="gradient-text-brand">
                Careers.
              </span>
            </h1>

            {/* Description */}
            <p className="text-base sm:text-lg leading-relaxed text-muted-foreground font-sans max-w-xl">
              Match elite software engineers with ambitious technical teams in real time. 
              Eliminate manual resume screening friction with live neural ATS scoring, automated candidate ranking, and instant interview scheduling.
            </p>

            {/* Candidate Hiring Action Buttons */}
            <div className="flex flex-col sm:flex-row gap-4 pt-2">
              <Link to={ROUTES.SIGNUP}>
                <Button
                  size="lg"
                  variant="gradient"
                  className="w-full sm:w-auto font-bold px-8 py-3.5 rounded-full text-base sm:text-lg shadow-xl shadow-[#C63FC5]/25 hover:opacity-95 transition flex items-center justify-center gap-2"
                >
                  <UserPlus className="w-5 h-5" />
                  <span>Hire Top Candidates</span>
                  <ArrowRight className="w-4 h-4" />
                </Button>
              </Link>

              <Link to={ROUTES.CANDIDATE_JOBS}>
                <Button
                  size="lg"
                  className="w-full sm:w-auto bg-brand-navy text-white hover:bg-brand-blue font-bold px-8 py-3.5 rounded-full text-base sm:text-lg shadow-lg transition flex items-center justify-center gap-2"
                >
                  <Briefcase className="w-5 h-5 text-brand-accent" />
                  <span>Browse Open Roles</span>
                </Button>
              </Link>
            </div>

            {/* Candidate & Recruiter Trust Signals */}
            <div className="pt-4 border-t border-border/60 flex flex-wrap items-center gap-5 text-xs text-muted-foreground font-medium">
              <div className="flex items-center gap-1.5">
                <BadgeCheck className="w-4 h-4 text-emerald-500" />
                <span>Verified Tech Candidates</span>
              </div>
              <div className="flex items-center gap-1.5">
                <Clock className="w-4 h-4 text-brand-blue" />
                <span>73% Faster Sourcing</span>
              </div>
              <div className="flex items-center gap-1.5">
                <Users className="w-4 h-4 text-[#C63FC5]" />
                <span>45,000+ Active Engineers</span>
              </div>
            </div>
          </div>

          {/* Right Images (Dual stacked layout with dynamic floating candidate badges) */}
          <div className="relative flex justify-center lg:justify-end mt-8 lg:mt-0">
            {/* Ambient Background Glow */}
            <div className="absolute -inset-2 bg-gradient-to-r from-[#C63FC5]/20 via-[#F56681]/20 to-[#FC9559]/20 rounded-[36px] blur-2xl opacity-70 pointer-events-none" />

            {/* Primary Hero Image */}
            <img
              src="/images/banner-img.png"
              alt="HireAI Recruitment Platform"
              className="rounded-[30px] object-cover shadow-2xl border border-white/20 dark:border-white/10 w-full max-w-[540px] h-auto relative z-10"
            />

            {/* Floating Top Left Badge: Candidate Shortlisted */}
            <div className="hidden sm:flex absolute -top-5 -left-4 z-20 items-center gap-2.5 bg-surface/95 border border-border shadow-xl rounded-2xl px-4 py-2.5 backdrop-blur-md animate-float-slow">
              <div className="w-9 h-9 rounded-xl bg-emerald-500/15 text-emerald-600 dark:text-emerald-400 flex items-center justify-center">
                <UserCheck className="w-5 h-5 stroke-[2.5]" />
              </div>
              <div>
                <div className="text-xs font-bold text-foreground">Top Candidate Hired</div>
                <div className="text-[10px] text-muted-foreground flex items-center gap-1">
                  <Sparkles className="w-3 h-3 text-brand-blue" /> 96% Match • Uber Backend
                </div>
              </div>
            </div>

            {/* Floating Bottom Right Badge: ATS Match Live Radar */}
            <div className="hidden sm:flex absolute -bottom-5 -right-4 z-20 items-center gap-3 bg-surface/95 border border-border shadow-xl rounded-2xl px-4 py-2.5 backdrop-blur-md">
              <div className="w-9 h-9 rounded-xl bg-brand-blue/15 text-brand-blue flex items-center justify-center">
                <Sparkles className="w-5 h-5" />
              </div>
              <div>
                <div className="text-xs font-bold text-foreground">Neural ATS Scoring</div>
                <div className="text-[10px] font-mono text-emerald-500 font-semibold flex items-center gap-1">
                  <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-ping" /> Real-time 1.2s screening
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
};
