import React, { useRef, useEffect } from 'react';
import { 
  Cpu, 
  UserCheck, 
  Briefcase, 
  FileCheck, 
  MessageSquare, 
  ShieldCheck, 
  ArrowRight 
} from 'lucide-react';
import { Link } from 'react-router-dom';
import { ROUTES } from '@/constants';

/**
 * Lightweight custom hook for scroll-reveal animation.
 * Uses IntersectionObserver to trigger once per page load without causing React re-renders.
 */
const useReveal = (options = {}) => {
  const ref = useRef(null);

  useEffect(() => {
    const element = ref.current;
    if (!element) return;

    // Progressive enhancement: only hide cards when JavaScript is active
    element.classList.add('reveal-ready');

    // If already revealed (e.g. fast remount in StrictMode), avoid re-observing
    if (element.classList.contains('is-visible')) {
      return;
    }

    if (!('IntersectionObserver' in window)) {
      element.classList.add('is-visible');
      return;
    }

    const observer = new IntersectionObserver(
      ([entry]) => {
        if (entry && entry.isIntersecting) {
          element.classList.add('is-visible');
          observer.unobserve(element);
          observer.disconnect();
        }
      },
      {
        threshold: options.threshold ?? 0.15,
        rootMargin: options.rootMargin ?? '0px 0px -40px 0px',
      }
    );

    observer.observe(element);

    return () => {
      observer.disconnect();
    };
  }, [options.threshold, options.rootMargin]);

  return ref;
};

export const LandingFeatures = () => {
  const gridRef = useReveal({ threshold: 0.15, rootMargin: '0px 0px -40px 0px' });

  const features = [
    {
      title: 'Deterministic ATS Match Scoring',
      desc: 'Deterministic and semantic skill overlap calculations that give recruiters a clear, transparent 0-100% score for every applicant.',
      icon: Cpu,
      iconColor: 'text-brand-blue',
      badge: 'Core Engine',
    },
    {
      title: 'Automated Candidate Shortlisting',
      desc: 'Automatically advance high-scoring candidates exceeding your shortlist threshold and notify recruiters in real time.',
      icon: UserCheck,
      iconColor: 'text-emerald-500',
      badge: 'Workflow',
    },
    {
      title: 'Job Posting Lifecycle',
      desc: 'Draft, publish, pause, and close technical postings with customizable compensation brackets and skill tags.',
      icon: Briefcase,
      iconColor: 'text-brand-navy dark:text-brand-blue-light',
      badge: 'Management',
    },
    {
      title: 'Rapid Resume Parsing',
      desc: 'Multi-format PDF and Word resume parser that extracts contact info, work history, and technical competencies in seconds.',
      icon: FileCheck,
      iconColor: 'text-amber-500',
      badge: 'Parser',
    },
    {
      title: 'Direct In-App Messaging',
      desc: 'Real-time integrated chat allowing direct communication, interview updates, and transparent feedback between recruiters and talent.',
      icon: MessageSquare,
      iconColor: 'text-brand-blue',
      badge: 'Collaboration',
    },
    {
      title: 'Enterprise Role Security',
      desc: 'Strict Spring Boot security with JWT authentication, rate limiting, and role-based access control for candidates and recruiters.',
      icon: ShieldCheck,
      iconColor: 'text-brand-navy dark:text-brand-blue-light',
      badge: 'Security',
    },
  ];

  return (
    <section id="features" className="py-14 sm:py-20 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
      {/* Header */}
      <div className="text-center max-w-2xl mx-auto mb-12 sm:mb-16">
        <span className="text-xs font-mono font-bold uppercase tracking-wider text-brand-blue">
          Platform Capabilities
        </span>
        <h2 className="text-2xl sm:text-3xl lg:text-4xl font-extrabold font-heading text-foreground tracking-tight mt-2">
          Everything You Need to Scale Technical Hiring
        </h2>
        <p className="text-sm sm:text-base text-muted-foreground mt-2 font-sans">
          Engineered to replace bloated recruitment suites with a fast, modern, and developer-friendly experience.
        </p>
      </div>

      {/* Feature Grid */}
      <div 
        ref={gridRef}
        className="features-grid grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5"
      >
        {features.map((item, index) => {
          const Icon = item.icon;
          return (
            <div
              key={item.title}
              style={{ '--i': index }}
              className="feature-reveal-card p-6 rounded-2xl bg-surface border border-border shadow-xs hover:shadow-md transition-all duration-200 flex flex-col justify-between space-y-4 text-left group"
            >
              <div>
                <div className="flex items-center justify-between mb-4">
                  <div className="w-10 h-10 rounded-xl bg-muted/70 flex items-center justify-center shrink-0">
                    <Icon className={`w-5 h-5 ${item.iconColor}`} />
                  </div>
                  <span className="text-[11px] font-mono font-semibold px-2 py-0.5 rounded-md bg-muted text-muted-foreground">
                    {item.badge}
                  </span>
                </div>

                <h3 className="font-bold text-base text-foreground font-heading group-hover:text-brand-blue transition-colors">
                  {item.title}
                </h3>

                <p className="text-xs sm:text-sm text-muted-foreground mt-2 leading-relaxed font-sans">
                  {item.desc}
                </p>
              </div>

              <div className="pt-3 border-t border-border/60 text-xs font-semibold text-brand-blue flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
                <span>View capability</span>
                <ArrowRight className="w-3.5 h-3.5" />
              </div>
            </div>
          );
        })}
      </div>
    </section>
  );
};
