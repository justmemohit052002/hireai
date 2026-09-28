import React from 'react';
import { 
  UserPlus, 
  Briefcase, 
  FileCheck, 
  SearchCheck, 
  UserCheck, 
  Award,
  Sparkles
} from 'lucide-react';

export const LandingWorks = () => {
  const steps = [
    {
      num: '01',
      title: 'Register & Build Profile',
      desc: 'Candidates craft rich developer profiles with verified skills; recruiters set up hiring pipelines and company workspaces.',
      icon: UserPlus,
      accent: 'from-[#C63FC5] to-pink-500',
    },
    {
      num: '02',
      title: 'Publish & Discover Roles',
      desc: 'Roles are broadcast across our curated developer network with automated AI semantic tagging and competitive salary brackets.',
      icon: Briefcase,
      accent: 'from-brand-blue to-cyan-500',
    },
    {
      num: '03',
      title: 'Neural ATS Skill Scoring',
      desc: 'Our neural ATS instantly scans resumes, computes candidate skill overlap, and generates transparent match percentages in 1.2s.',
      icon: FileCheck,
      accent: 'from-[#6D3DF5] to-indigo-500',
    },
    {
      num: '04',
      title: 'Assess & Benchmark Talent',
      desc: 'Recruiters review verified coding radar benchmarks and past scale experience to shortlist only the top percentile candidates.',
      icon: SearchCheck,
      accent: 'from-[#FC9559] to-amber-500',
    },
    {
      num: '05',
      title: 'Interview & Collaborate',
      desc: 'One-click automated interview invitations with calendar sync, contextual AI recruiter interview guides, and live feedback scoring.',
      icon: UserCheck,
      accent: 'from-emerald-500 to-teal-500',
    },
    {
      num: '06',
      title: 'Extend Offer & Hire',
      desc: 'Generate competitive offer packages, track acceptance status in real time, and successfully welcome high-impact engineers.',
      icon: Award,
      accent: 'from-[#C63FC5] to-brand-blue',
    },
  ];

  return (
    <section id="how-it-works" className="py-16 md:py-24 px-6 max-w-7xl mx-auto">
      <div className="mx-auto px-2 sm:px-5">
        {/* Heading */}
        <div className="text-center max-w-2xl mx-auto">
          <div className="inline-flex items-center gap-2 px-3.5 py-1 rounded-full bg-brand-blue-light/30 text-brand-navy dark:text-brand-blue-light border border-brand-blue/20 font-mono text-xs font-bold uppercase tracking-wider mb-4 shadow-xs">
            <Sparkles className="w-3.5 h-3.5 text-brand-blue" />
            <span>Candidate Lifecycle Workflow</span>
          </div>

          <h2 className="text-3xl sm:text-4xl lg:text-5xl font-extrabold font-heading tracking-tight text-foreground">
            How <span className="gradient-text-brand">Candidate Hiring</span> Works
          </h2>

          <p className="mt-4 text-muted-foreground text-sm sm:text-base leading-relaxed font-sans">
            A frictionless 6-step recruitment pipeline engineered to connect the right candidate with the right opportunity.
          </p>
        </div>

        {/* Cards Grid */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 mt-12 sm:mt-16">
          {steps.map((step) => {
            const StepIcon = step.icon;
            return (
              <div
                key={step.num}
                className="group relative bg-surface border border-border/80 hover:border-brand-blue/40 rounded-3xl p-8 flex flex-col justify-between hover-lift transition-all duration-300 shadow-md hover:shadow-xl"
              >
                <div>
                  {/* Top Badge: Number and Candidate Hiring Icon */}
                  <div className="flex items-center justify-between">
                    <div className={`w-12 h-12 rounded-2xl bg-gradient-to-tr ${step.accent} text-white font-extrabold text-base flex items-center justify-center shadow-md transition-transform duration-300 group-hover:scale-110`}>
                      <StepIcon className="w-6 h-6 stroke-[2.2]" />
                    </div>

                    <span className="font-mono text-sm font-bold text-muted-foreground/60 px-2.5 py-1 rounded-full bg-muted/40 border border-border/50">
                      STEP {step.num}
                    </span>
                  </div>

                  <h3 className="text-xl font-bold font-heading mt-6 text-foreground group-hover:text-brand-blue transition-colors">
                    {step.title}
                  </h3>

                  <p className="mt-3 text-muted-foreground text-xs sm:text-sm leading-relaxed font-sans">
                    {step.desc}
                  </p>
                </div>

                <div className="mt-6 pt-4 border-t border-border/50 flex items-center gap-1.5 text-xs font-semibold text-brand-blue opacity-0 group-hover:opacity-100 transition-opacity">
                  <UserCheck className="w-3.5 h-3.5" />
                  <span>Candidate Verified Step</span>
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </section>
  );
};
