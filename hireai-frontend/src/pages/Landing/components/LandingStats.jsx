import React from 'react';
import { Sparkles, Clock, UserCheck, Building2 } from 'lucide-react';

export const LandingStats = () => {
  const stats = [
    {
      value: '96.4%',
      label: 'ATS Match Accuracy',
      desc: 'Neural algorithm precision matching resume skills with job criteria.',
      icon: Sparkles,
      iconColor: 'text-brand-blue',
    },
    {
      value: '73%',
      label: 'Faster Time-to-Hire',
      desc: 'Average reduction in sourcing and screening time for recruiters.',
      icon: Clock,
      iconColor: 'text-emerald-500',
    },
    {
      value: '45,000+',
      label: 'Vetted Candidates',
      desc: 'Active developers, architects, and technical specialists.',
      icon: UserCheck,
      iconColor: 'text-brand-navy dark:text-brand-blue-light',
    },
    {
      value: '2,400+',
      label: 'Hiring Companies',
      desc: 'Startups and high-scale enterprises scaling their teams.',
      icon: Building2,
      iconColor: 'text-[#C63FC5]',
    },
  ];

  return (
    <section className="py-12 sm:py-16 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
        {stats.map((item) => {
          const Icon = item.icon;
          return (
            <div
              key={item.label}
              className="p-6 rounded-2xl bg-surface border border-border shadow-xs hover:shadow-md transition-all duration-200 flex flex-col justify-between space-y-4"
            >
              <div className="flex items-center justify-between">
                <span className="text-3xl sm:text-4xl font-extrabold font-heading text-foreground tracking-tight">
                  {item.value}
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
