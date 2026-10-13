import React from 'react';
import { Sliders, FileText, ShieldCheck, GitBranch } from 'lucide-react';

export const LandingStats = () => {
  const specs = [
    {
      metric: '0-100%',
      label: 'Deterministic ATS Matrix',
      desc: 'Objective score calculation evaluating technical stack keyword overlap and role years.',
      icon: Sliders,
      iconColor: 'text-brand-blue',
    },
    {
      metric: 'PDF & DOCX',
      label: 'Multi-Format Ingestion',
      desc: 'Fast resume file extraction extracting structured work history, contact details, and skills.',
      icon: FileText,
      iconColor: 'text-emerald-500',
    },
    {
      metric: 'Enterprise RBAC',
      label: 'Role-Based Isolation',
      desc: 'Spring Security and JWT token management separating candidate profiles and recruiter data.',
      icon: ShieldCheck,
      iconColor: 'text-brand-navy dark:text-brand-blue-light',
    },
    {
      metric: 'Synchronous',
      label: 'Pipeline State Sync',
      desc: 'Real-time stage transitions from application received to technical evaluation and final offer.',
      icon: GitBranch,
      iconColor: 'text-brand-blue',
    },
  ];

  return (
    <section className="py-12 sm:py-16 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
        {specs.map((item) => {
          const Icon = item.icon;
          return (
            <div
              key={item.label}
              className="p-6 rounded-2xl bg-surface border border-border shadow-xs hover:shadow-md transition-all duration-200 flex flex-col justify-between space-y-4"
            >
              <div className="flex items-center justify-between">
                <span className="text-xl sm:text-2xl font-extrabold font-heading text-foreground tracking-tight">
                  {item.metric}
                </span>
                <div className="w-9 h-9 rounded-xl bg-muted/60 flex items-center justify-center shrink-0">
                  <Icon className={`w-4 h-4 ${item.iconColor}`} />
                </div>
              </div>

              <div>
                <h3 className="font-bold text-sm text-foreground">
                  {item.label}
                </h3>
                <p className="text-xs text-muted-foreground mt-1 leading-relaxed font-sans">
                  {item.desc}
                </p>
              </div>
            </div>
          );
        })}
      </div>
    </section>
  );
};
