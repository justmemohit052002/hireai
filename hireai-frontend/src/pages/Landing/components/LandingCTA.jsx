import React from 'react';
import { Link } from 'react-router-dom';
import { UserPlus, Briefcase, ArrowRight } from 'lucide-react';
import { Button } from '@/components/ui/Button';
import { ROUTES } from '@/constants';

export const LandingCTA = () => {
  return (
    <section className="py-16 sm:py-24 max-w-5xl mx-auto px-4 sm:px-6 text-center">
      <div className="p-8 sm:p-14 rounded-3xl bg-surface border border-border shadow-lg relative overflow-hidden">
        {/* Subtle Ambient Backing */}
        <div className="pointer-events-none absolute inset-0 bg-gradient-to-b from-brand-blue/5 via-transparent to-transparent -z-10" />

        <div className="max-w-2xl mx-auto space-y-4">
          <span className="text-xs font-mono font-bold uppercase tracking-wider text-brand-blue">
            Get Started in Seconds
          </span>

          <h2 className="text-3xl sm:text-4xl lg:text-5xl font-extrabold font-heading text-foreground tracking-tight leading-tight">
            Ready to Transform Your Engineering Hiring?
          </h2>

          <p className="text-sm sm:text-base text-muted-foreground leading-relaxed font-sans max-w-xl mx-auto">
            Join thousands of engineering managers, technical recruiters, and software developers using HireAI for faster, bias-free candidate placements.
          </p>

          <div className="pt-6 flex flex-col sm:flex-row items-center justify-center gap-3.5">
            <Link to={ROUTES.SIGNUP} className="w-full sm:w-auto">
              <Button size="lg" variant="gradient" className="w-full sm:w-auto font-bold px-8 py-3.5 rounded-xl text-base shadow-md flex items-center justify-center gap-2">
                <UserPlus className="w-4 h-4" />
                <span>Start Hiring Free</span>
                <ArrowRight className="w-4 h-4" />
              </Button>
            </Link>

            <Link to={ROUTES.CANDIDATE_JOBS} className="w-full sm:w-auto">
              <Button size="lg" variant="outline" className="w-full sm:w-auto border-border hover:bg-muted/50 font-semibold px-7 py-3.5 rounded-xl text-base flex items-center justify-center gap-2">
                <Briefcase className="w-4 h-4 text-brand-blue" />
                <span>Explore Technical Roles</span>
              </Button>
            </Link>
          </div>
        </div>
      </div>
    </section>
  );
};
