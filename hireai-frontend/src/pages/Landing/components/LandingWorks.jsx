import React from 'react';
import { 
  Briefcase, 
  FileCheck, 
  UserCheck, 
  Award,
  ArrowRight
} from 'lucide-react';

export const LandingWorks = () => {
  const steps = [
    {
      num: '01',
      title: 'Post Technical Role',
      desc: 'Define tech stack requirements, compensation brackets, and required skills with automatic AI semantic tagging.',
      icon: Briefcase,
    },
    {
      num: '02',
      title: 'Neural ATS Scoring',
      desc: 'Uploaded resumes are parsed in 1.2s and objectively scored from 0 to 100% against job criteria.',
      icon: FileCheck,
    },
    {
      num: '03',
      title: 'Instant Shortlisting',
      desc: 'Top-tier candidates meeting your threshold automatically advance to interview invitations without manual triage.',
      icon: UserCheck,
    },
    {
      num: '04',
      title: 'Offer & Onboard',
      desc: 'Collaborate with your engineering hiring committee, extend competitive offers, and welcome top talent.',
      icon: Award,
    },
  ];

  return (
    <section id="how-it-works" className="py-14 sm:py-20 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
      {/* Heading */}
      <div className="text-center max-w-2xl mx-auto mb-12 sm:mb-16">
        <span className="text-xs font-mono font-bold uppercase tracking-wider text-brand-blue">
          Hiring Workflow
        </span>
        <h2 className="text-2xl sm:text-3xl lg:text-4xl font-extrabold font-heading text-foreground tracking-tight mt-2">
          How Candidate Hiring Works on HireAI
        </h2>
        <p className="text-sm sm:text-base text-muted-foreground mt-2 font-sans">
          A transparent 4-stage recruitment pipeline designed to eliminate friction for hiring teams and candidates.
        </p>
      </div>

      {/* 4-Step Progressive Timeline Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
        {steps.map((step, index) => {
          const StepIcon = step.icon;
          return (
            <div
              key={step.num}
              className="p-6 rounded-2xl bg-surface border border-border shadow-xs hover:shadow-md transition-all duration-200 flex flex-col justify-between space-y-5 text-left relative group"
            >
              <div className="flex items-center justify-between">
                <div className="w-10 h-10 rounded-xl bg-muted/70 text-brand-navy dark:text-brand-blue-light flex items-center justify-center shrink-0">
                  <StepIcon className="w-5 h-5 stroke-[2.2]" />
                </div>
                <span className="font-mono text-xs font-bold text-muted-foreground/70">
                  STEP {step.num}
                </span>
              </div>

              <div>
                <h3 className="font-bold text-base text-foreground font-heading">
                  {step.title}
                </h3>
                <p className="text-xs sm:text-sm text-muted-foreground mt-2 leading-relaxed font-sans">
                  {step.desc}
                </p>
              </div>

              <div className="pt-3 border-t border-border/60 text-xs font-semibold text-brand-blue flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
                <span>Learn more</span>
                <ArrowRight className="w-3.5 h-3.5" />
              </div>
            </div>
          );
        })}
      </div>
    </section>
  );
};
