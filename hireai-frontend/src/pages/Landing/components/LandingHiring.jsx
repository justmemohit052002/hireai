import React from 'react';
import { Link } from 'react-router-dom';
import { Check } from 'lucide-react';
import { Button } from '@/components/ui/Button';
import { ROUTES } from '@/constants';

export const LandingHiring = () => {
  return (
    <section id="hiring" className="py-12 md:py-16">
      <div className="max-w-5xl mx-auto px-5">
        {/* Heading */}
        <div className="text-center">
          <h2 className="text-4xl md:text-5xl font-bold font-heading tracking-tight text-foreground">
            Simplify <span className="gradient-text-brand">Hiring.</span>
          </h2>

          <p className="mt-4 text-muted-foreground text-base md:text-md max-w-2xl mx-auto font-sans">
            Tailored recruitment features built specifically for HR engineering managers and ambitious software developers.
          </p>
        </div>

        {/* Cards */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-8 mt-12">
          {/* HR Card */}
          <div className="bg-[#6D3DF5]/10 border border-[#6D3DF5]/25 rounded-3xl p-8 md:p-10 surface-nested dark:bg-[#6D3DF5]/15 flex flex-col justify-between interactive-card transition">
            <div>
              <h3 className="text-4xl font-bold font-heading text-foreground">
                For HR
              </h3>

              <p className="mt-6 text-sm md:text-base text-muted-foreground leading-relaxed max-w-md font-sans">
                Post technical roles, let neural algorithms rank applicant code repositories, and shortlist top candidates instantly.
              </p>

              {/* Feature 1 */}
              <div className="flex items-center gap-4 mt-8">
                <div className="w-6 h-6 rounded-full bg-[#6D3DF5] text-white flex items-center justify-center shrink-0 shadow-md">
                  <Check className="w-3.5 h-3.5 stroke-[3]" />
                </div>
                <h4 className="text-base font-medium text-foreground">
                  Create and Manage Jobs
                </h4>
              </div>

              {/* Feature 2 */}
              <div className="flex items-center gap-4 mt-5">
                <div className="w-6 h-6 rounded-full bg-[#6D3DF5] text-white flex items-center justify-center shrink-0 shadow-md">
                  <Check className="w-3.5 h-3.5 stroke-[3]" />
                </div>
                <h4 className="text-base font-medium text-foreground">
                  Neural Candidate Ranking & Analytics
                </h4>
              </div>
            </div>

            <div className="mt-10">
              <Link to={ROUTES.SIGNUP}>
                <Button variant="secondary" size="lg" className="px-6 py-3 rounded-full text-base font-semibold transition shadow-lg">
                  Explore HR Dashboard
                </Button>
              </Link>
            </div>
          </div>

          {/* Candidate Card */}
          <div className="bg-gradient-to-br from-[#22214B] via-[#1A193B] to-[#12112E] border border-white/15 rounded-3xl p-8 md:p-10 shadow-2xl text-white flex flex-col justify-between interactive-card transition">
            <div>
              <h3 className="text-4xl font-bold font-heading text-white">
                For Candidates
              </h3>

              <p className="mt-6 text-sm md:text-base text-white/80 leading-relaxed max-w-md font-sans">
                Showcase developer profiles, receive direct recruiter feedback, and apply to top engineering teams in 1 click.
              </p>

              {/* Feature 1 */}
              <div className="flex items-center gap-4 mt-8">
                <div className="w-6 h-6 rounded-full bg-white text-[#22214B] flex items-center justify-center shrink-0 shadow-md">
                  <Check className="w-3.5 h-3.5 stroke-[3]" />
                </div>
                <h4 className="text-base font-medium text-white">
                  1-Click AI Job Applications
                </h4>
              </div>

              {/* Feature 2 */}
              <div className="flex items-center gap-4 mt-5">
                <div className="w-6 h-6 rounded-full bg-white text-[#22214B] flex items-center justify-center shrink-0 shadow-md">
                  <Check className="w-3.5 h-3.5 stroke-[3]" />
                </div>
                <h4 className="text-base font-medium text-white">
                  Real-Time Skill Scoring & Verification
                </h4>
              </div>
            </div>

            <div className="mt-10">
              <Link to={ROUTES.SIGNUP}>
                <Button variant="primary" size="lg" className="px-6 py-3 rounded-full text-base font-semibold transition shadow-lg">
                  Explore Candidates Dashboard
                </Button>
              </Link>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
};
