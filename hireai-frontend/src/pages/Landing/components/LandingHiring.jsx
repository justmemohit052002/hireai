import React from 'react';
import { Link } from 'react-router-dom';
import { 
  UserCheck, 
  Briefcase, 
  UserPlus, 
  Sparkles, 
  FileCheck, 
  BadgeCheck, 
  ArrowRight 
} from 'lucide-react';
import { Button } from '@/components/ui/Button';
import { ROUTES } from '@/constants';

export const LandingHiring = () => {
  return (
    <section id="hiring" className="py-16 md:py-20 max-w-7xl mx-auto px-5 sm:px-6">
      <div className="max-w-5xl mx-auto">
        {/* Heading */}
        <div className="text-center max-w-2xl mx-auto">
          <div className="inline-flex items-center gap-2 px-3.5 py-1 rounded-full bg-brand-blue-light/30 text-brand-navy dark:text-brand-blue-light border border-brand-blue/20 font-mono text-xs font-bold uppercase tracking-wider mb-4 shadow-xs">
            <UserCheck className="w-3.5 h-3.5 text-brand-blue" />
            <span>Dual-Sided Hiring Experience</span>
          </div>

          <h2 className="text-3xl sm:text-4xl lg:text-5xl font-extrabold font-heading tracking-tight text-foreground">
            Built for <span className="gradient-text-brand">Hiring Teams</span> & Talent
          </h2>

          <p className="mt-4 text-muted-foreground text-sm sm:text-base leading-relaxed font-sans">
            Tailored recruitment intelligence built specifically for tech recruiters seeking top talent and engineers advancing their careers.
          </p>
        </div>

        {/* Cards */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-8 mt-12 sm:mt-16">
          {/* HR / Recruiter Card */}
          <div className="bg-surface border border-brand-blue/30 rounded-3xl p-8 md:p-10 shadow-xl flex flex-col justify-between hover-lift transition-all relative overflow-hidden group">
            <div className="pointer-events-none absolute -top-12 -right-12 w-48 h-48 bg-brand-blue/10 rounded-full blur-2xl group-hover:bg-brand-blue/20 transition-colors" />

            <div>
              <div className="flex items-center justify-between">
                <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-brand-blue/15 text-brand-blue dark:text-brand-blue-light border border-brand-blue/25 text-xs font-mono font-bold uppercase">
                  <UserPlus className="w-3.5 h-3.5" /> For Recruiters & HR
                </span>
                <span className="text-xs font-semibold text-muted-foreground">Hire 3x Faster</span>
              </div>

              <h3 className="text-3xl font-extrabold font-heading text-foreground mt-4">
                Scale Your Engineering Team
              </h3>

              <p className="mt-3 text-sm text-muted-foreground leading-relaxed font-sans">
                Post open positions, let neural algorithms score candidate skill matches, and shortlist top talent in minutes without manual resume review.
              </p>

              {/* Recruiter Features with Candidate Icons */}
              <div className="space-y-3.5 mt-8">
                <div className="flex items-center gap-3">
                  <div className="w-8 h-8 rounded-xl bg-brand-blue/15 text-brand-blue flex items-center justify-center shrink-0">
                    <Briefcase className="w-4 h-4" />
                  </div>
                  <span className="text-sm font-semibold text-foreground">
                    Post & Manage High-Throughput Roles
                  </span>
                </div>

                <div className="flex items-center gap-3">
                  <div className="w-8 h-8 rounded-xl bg-brand-blue/15 text-brand-blue flex items-center justify-center shrink-0">
                    <UserCheck className="w-4 h-4 stroke-[2.2]" />
                  </div>
                  <span className="text-sm font-semibold text-foreground">
                    Automated Candidate ATS Ranking (0–100%)
                  </span>
                </div>

                <div className="flex items-center gap-3">
                  <div className="w-8 h-8 rounded-xl bg-brand-blue/15 text-brand-blue flex items-center justify-center shrink-0">
                    <Sparkles className="w-4 h-4" />
                  </div>
                  <span className="text-sm font-semibold text-foreground">
                    One-Click Interview Invitations & Feedback
                  </span>
                </div>
              </div>
            </div>

            <div className="mt-10 pt-6 border-t border-border/60">
              <Link to={ROUTES.SIGNUP}>
                <Button className="w-full bg-brand-navy text-white hover:bg-brand-blue py-3 rounded-full text-base font-bold transition shadow-lg flex items-center justify-center gap-2">
                  <span>Start Hiring Candidates</span>
                  <ArrowRight className="w-4 h-4" />
                </Button>
              </Link>
            </div>
          </div>

          {/* Candidate Card */}
          <div className="bg-gradient-to-br from-brand-navy via-brand-dark to-[#101c42] text-white border border-white/20 rounded-3xl p-8 md:p-10 shadow-2xl flex flex-col justify-between hover-lift transition-all relative overflow-hidden group">
            <div className="pointer-events-none absolute -bottom-12 -right-12 w-48 h-48 bg-brand-accent/15 rounded-full blur-2xl group-hover:bg-brand-accent/25 transition-colors" />

            <div>
              <div className="flex items-center justify-between">
                <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-white/10 text-brand-accent border border-white/20 text-xs font-mono font-bold uppercase">
                  <BadgeCheck className="w-3.5 h-3.5" /> For Candidates & Engineers
                </span>
                <span className="text-xs font-semibold text-brand-blue-light">100% Free For Talent</span>
              </div>

              <h3 className="text-3xl font-extrabold font-heading text-white mt-4">
                Elevate Your Tech Career
              </h3>

              <p className="mt-3 text-sm text-gray-300 leading-relaxed font-sans">
                Showcase your verified engineering skills, receive automated transparent match scores, and land top opportunities at industry-leading companies.
              </p>

              {/* Candidate Features */}
              <div className="space-y-3.5 mt-8">
                <div className="flex items-center gap-3">
                  <div className="w-8 h-8 rounded-xl bg-white/10 text-brand-accent flex items-center justify-center shrink-0">
                    <FileCheck className="w-4 h-4" />
                  </div>
                  <span className="text-sm font-semibold text-white">
                    1-Click Semantic Resume Applications
                  </span>
                </div>

                <div className="flex items-center gap-3">
                  <div className="w-8 h-8 rounded-xl bg-white/10 text-brand-accent flex items-center justify-center shrink-0">
                    <BadgeCheck className="w-4 h-4" />
                  </div>
                  <span className="text-sm font-semibold text-white">
                    Live ATS Scoring & Skill Gap Analytics
                  </span>
                </div>

                <div className="flex items-center gap-3">
                  <div className="w-8 h-8 rounded-xl bg-white/10 text-brand-accent flex items-center justify-center shrink-0">
                    <UserCheck className="w-4 h-4" />
                  </div>
                  <span className="text-sm font-semibold text-white">
                    Direct Visibility to Executive Engineering Leads
                  </span>
                </div>
              </div>
            </div>

            <div className="mt-10 pt-6 border-t border-white/15">
              <Link to={ROUTES.SIGNUP}>
                <Button variant="gradient" className="w-full font-bold py-3 rounded-full text-base transition shadow-xl shadow-brand-accent/20 flex items-center justify-center gap-2">
                  <span>Create Candidate Profile</span>
                  <ArrowRight className="w-4 h-4" />
                </Button>
              </Link>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
};
