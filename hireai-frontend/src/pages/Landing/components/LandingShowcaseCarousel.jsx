import React, { useState, useEffect, useRef } from 'react';
import { 
  Sparkles, 
  UserCheck, 
  UserPlus, 
  Briefcase, 
  BadgeCheck, 
  SearchCheck, 
  Award, 
  CheckCircle2, 
  Clock, 
  ChevronLeft, 
  ChevronRight, 
  ArrowRight,
  Building2,
  FileCheck,
  Code2
} from 'lucide-react';
import { Button } from '@/components/ui/Button';
import { Link } from 'react-router-dom';
import { ROUTES } from '@/constants';

const SLIDES = [
  {
    id: 'ats-matching',
    tabTitle: 'Neural ATS Scoring',
    tabIcon: Sparkles,
    badge: 'OBJECTIVE MATCH SCORING',
    title: 'Explainable 0–100% Match Scores Built on Real Code Competencies',
    description:
      'Unlike keyword-stuffing legacy ATS filters, HireAI evaluates candidate repositories, semantic skill overlap, and verified system scale. Recruiters see exactly why an applicant is shortlisted.',
    statValue: '96.4%',
    statLabel: 'Semantic Match Precision',
    candidate: {
      name: 'Mohit Singh Chouhan',
      role: 'Senior Full-Stack Engineer',
      experience: 'Ex-Vionsys • 5+ Years Exp',
      score: 96,
      skills: ['React', 'Java', 'Spring Boot', 'PostgreSQL', 'Docker', 'AWS'],
      notes: 'Strong alignment with distributed backend requirements and modern reactive frontend stack.',
      appliedTo: 'Associate Software Developer @ HCL',
    },
  },
  {
    id: 'candidate-shortlist',
    tabTitle: 'Automated Shortlisting',
    tabIcon: UserCheck,
    badge: 'STAGE AUTOMATION',
    title: 'Shortlist Qualified Candidates Instantly Without Manual Review',
    description:
      'Candidates scoring above your custom threshold automatically advance to the Shortlisted stage with personalized notifications and calendar interview links dispatched immediately.',
    statValue: '1.2s',
    statLabel: 'Average Screening Time',
    stages: [
      { name: 'Application Received', status: 'completed', time: 'Sep 28, 5:59 PM', icon: FileCheck },
      { name: 'Neural ATS Screened', status: 'completed', time: 'Sep 28, 6:00 PM', icon: Sparkles },
      { name: 'Shortlisted for Interview', status: 'active', time: 'Sep 28, 6:00 PM', icon: UserCheck },
      { name: 'Technical Evaluation', status: 'pending', time: 'Upcoming', icon: Award },
    ],
  },
  {
    id: 'pipeline-funnel',
    tabTitle: 'Recruitment Funnel',
    tabIcon: Briefcase,
    badge: 'PIPELINE VISIBILITY',
    title: 'Real-Time Pipeline Velocity and Candidate Stage Tracking',
    description:
      'Track every applicant from initial application to offer letter in a single intuitive interface. Filter applicants by ATS score, active stage, and hiring committee feedback.',
    statValue: '73%',
    statLabel: 'Time-to-Hire Reduction',
    funnel: [
      { label: 'Active Roles', count: '14 Open', detail: 'Engineering & Product' },
      { label: 'Applicants Screened', count: '142 Candidates', detail: 'This Week' },
      { label: 'Shortlist Rate', count: '18% Top Tier', detail: 'Score >= 70%' },
      { label: 'Offers Accepted', count: '6 Hired', detail: 'Avg 4 Days' },
    ],
  },
  {
    id: 'skill-radar',
    tabTitle: 'Technical Radar',
    tabIcon: BadgeCheck,
    badge: 'VERIFIED BENCHMARKS',
    title: 'Multi-Axis Technical Capability & Code Architecture Scoring',
    description:
      'Go beyond resume bullet points. HireAI tests and validates system architecture, algorithmic logic, test coverage, and API robustness through interactive assessments.',
    statValue: '99th',
    statLabel: 'Percentile Verification',
    skills: [
      { name: 'System Architecture & Concurrency', level: 98 },
      { name: 'Algorithmic Problem Solving', level: 94 },
      { name: 'Database Tuning & Schema Design', level: 92 },
      { name: 'Cloud Infrastructure & CI/CD', level: 89 },
    ],
  },
];

export const LandingShowcaseCarousel = () => {
  const [currentIndex, setCurrentIndex] = useState(0);
  const [isPaused, setIsPaused] = useState(false);
  const timerRef = useRef(null);

  useEffect(() => {
    if (isPaused) return;

    timerRef.current = setInterval(() => {
      setCurrentIndex((prev) => (prev + 1) % SLIDES.length);
    }, 6000);

    return () => {
      if (timerRef.current) clearInterval(timerRef.current);
    };
  }, [isPaused, currentIndex]);

  const handlePrev = () => {
    setCurrentIndex((prev) => (prev === 0 ? SLIDES.length - 1 : prev - 1));
  };

  const handleNext = () => {
    setCurrentIndex((prev) => (prev + 1) % SLIDES.length);
  };

  const current = SLIDES[currentIndex];

  return (
    <section
      className="py-14 sm:py-20 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8"
      onMouseEnter={() => setIsPaused(true)}
      onMouseLeave={() => setIsPaused(false)}
    >
      {/* Header */}
      <div className="text-center max-w-2xl mx-auto mb-10">
        <span className="text-xs font-mono font-bold uppercase tracking-wider text-brand-blue">
          Platform Showcase
        </span>
        <h2 className="text-2xl sm:text-3xl lg:text-4xl font-extrabold font-heading text-foreground tracking-tight mt-2">
          Engineered for Modern Engineering Hiring
        </h2>
        <p className="text-sm sm:text-base text-muted-foreground mt-2.5 font-sans">
          See how HireAI streamlines every step from candidate application to signed offer letter.
        </p>
      </div>

      {/* Segmented Tab Navigation */}
      <div className="flex justify-center mb-8">
        <div className="inline-flex p-1.5 rounded-2xl bg-surface border border-border shadow-xs max-w-full overflow-x-auto">
          {SLIDES.map((slide, idx) => {
            const Icon = slide.tabIcon;
            const isActive = idx === currentIndex;
            return (
              <button
                key={slide.id}
                onClick={() => setCurrentIndex(idx)}
                className={`flex items-center gap-2 px-3 sm:px-4 py-2 rounded-xl text-xs sm:text-sm font-semibold transition-all whitespace-nowrap ${
                  isActive
                    ? 'bg-brand-navy text-white shadow-xs'
                    : 'text-muted-foreground hover:text-foreground'
                }`}
              >
                <Icon className="w-3.5 h-3.5 shrink-0" />
                <span>{slide.tabTitle}</span>
              </button>
            );
          })}
        </div>
      </div>

      {/* Main Interactive Showcase Card */}
      <div className="rounded-2xl bg-surface border border-border p-6 sm:p-10 shadow-lg">
        <div className="grid lg:grid-cols-12 gap-8 items-center">
          {/* Narrative Column (5 cols) */}
          <div className="lg:col-span-5 space-y-5 text-left">
            <span className="inline-flex items-center gap-1.5 text-xs font-mono font-bold text-brand-blue uppercase tracking-wider">
              <span className="w-2 h-2 rounded-full bg-brand-blue" />
              {current.badge}
            </span>

            <h3 className="text-2xl sm:text-3xl font-extrabold font-heading text-foreground leading-tight">
              {current.title}
            </h3>

            <p className="text-sm sm:text-base text-muted-foreground leading-relaxed font-sans">
              {current.description}
            </p>

            {/* Metric Box */}
            <div className="p-4 rounded-xl bg-muted/40 border border-border/80 inline-flex items-center gap-4">
              <div className="text-2xl sm:text-3xl font-extrabold font-heading text-brand-navy dark:text-brand-blue-light">
                {current.statValue}
              </div>
              <div className="text-xs text-muted-foreground font-medium">
                {current.statLabel}
              </div>
            </div>

            <div>
              <Link to={ROUTES.SIGNUP}>
                <Button variant="gradient" size="sm" className="font-bold flex items-center gap-2 px-5 py-2.5 rounded-xl">
                  <span>Explore Candidate Pipeline</span>
                  <ArrowRight className="w-4 h-4" />
                </Button>
              </Link>
            </div>
          </div>

          {/* Interactive UI Preview (7 cols) */}
          <div className="lg:col-span-7">
            {/* Slide 1: Neural ATS Candidate Card */}
            {current.id === 'ats-matching' && current.candidate && (
              <div className="p-5 sm:p-6 rounded-xl bg-background border border-border space-y-4">
                <div className="flex items-center justify-between pb-3 border-b border-border/80">
                  <div className="flex items-center gap-3">
                    <div className="w-10 h-10 rounded-xl bg-brand-navy text-white flex items-center justify-center font-bold text-sm">
                      MC
                    </div>
                    <div>
                      <h4 className="font-bold text-sm text-foreground">
                        {current.candidate.name}
                      </h4>
                      <p className="text-xs text-muted-foreground">
                        {current.candidate.experience}
                      </p>
                    </div>
                  </div>

                  <span className="px-3 py-1 rounded-full bg-emerald-500/15 text-emerald-600 dark:text-emerald-400 font-mono font-bold text-xs flex items-center gap-1 border border-emerald-500/25">
                    <Sparkles className="w-3 h-3" />
                    {current.candidate.score}% ATS Match
                  </span>
                </div>

                <div>
                  <div className="text-xs font-semibold text-muted-foreground mb-2">
                    Verified Competencies Matched
                  </div>
                  <div className="flex flex-wrap gap-1.5">
                    {current.candidate.skills.map((skill) => (
                      <span
                        key={skill}
                        className="px-2.5 py-1 rounded-lg text-xs font-medium bg-surface border border-border text-foreground flex items-center gap-1"
                      >
                        <CheckCircle2 className="w-3 h-3 text-emerald-500" />
                        {skill}
                      </span>
                    ))}
                  </div>
                </div>

                <div className="p-3.5 rounded-lg bg-surface border border-border text-xs text-muted-foreground">
                  <strong className="text-foreground">AI Review Note:</strong> {current.candidate.notes}
                </div>

                <div className="flex items-center justify-between pt-1 text-xs">
                  <span className="text-muted-foreground flex items-center gap-1">
                    <Building2 className="w-3.5 h-3.5 text-brand-blue" />
                    {current.candidate.appliedTo}
                  </span>
                  <span className="text-emerald-600 dark:text-emerald-400 font-bold flex items-center gap-1">
                    <UserCheck className="w-3.5 h-3.5" /> Shortlisted
                  </span>
                </div>
              </div>
            )}

            {/* Slide 2: Automated Shortlist Pipeline */}
            {current.id === 'candidate-shortlist' && (
              <div className="p-5 sm:p-6 rounded-xl bg-background border border-border space-y-3">
                <div className="text-xs font-bold text-muted-foreground uppercase tracking-wider mb-2">
                  Automated Candidate Progression
                </div>
                {current.stages.map((st, i) => {
                  const StIcon = st.icon;
                  const isCurrent = st.status === 'active';
                  return (
                    <div
                      key={st.name}
                      className={`p-3.5 rounded-xl border flex items-center justify-between gap-3 text-xs transition-all ${
                        isCurrent
                          ? 'bg-surface border-brand-blue shadow-xs font-semibold'
                          : 'bg-surface/50 border-border/70 text-muted-foreground'
                      }`}
                    >
                      <div className="flex items-center gap-3">
                        <div className={`w-8 h-8 rounded-lg flex items-center justify-center shrink-0 ${
                          isCurrent ? 'bg-brand-blue text-white' : 'bg-muted text-muted-foreground'
                        }`}>
                          <StIcon className="w-4 h-4" />
                        </div>
                        <div>
                          <div className={isCurrent ? 'text-foreground font-bold' : ''}>
                            {st.name}
                          </div>
                          <div className="text-[11px] text-muted-foreground">{st.time}</div>
                        </div>
                      </div>

                      {isCurrent && (
                        <span className="text-[10px] font-mono font-bold px-2 py-0.5 rounded-full bg-brand-blue/15 text-brand-blue">
                          Current Stage
                        </span>
                      )}
                    </div>
                  );
                })}
              </div>
            )}

            {/* Slide 3: Funnel Metrics */}
            {current.id === 'pipeline-funnel' && (
              <div className="grid grid-cols-2 gap-3.5">
                {current.funnel.map((fn) => (
                  <div key={fn.label} className="p-5 rounded-xl bg-background border border-border space-y-1.5 text-left">
                    <div className="text-xs text-muted-foreground font-medium">{fn.label}</div>
                    <div className="text-xl font-bold font-heading text-foreground">{fn.count}</div>
                    <div className="text-[11px] text-brand-blue font-semibold">{fn.detail}</div>
                  </div>
                ))}
              </div>
            )}

            {/* Slide 4: Skill Radar */}
            {current.id === 'skill-radar' && (
              <div className="p-5 sm:p-6 rounded-xl bg-background border border-border space-y-4 text-left">
                <div className="flex items-center justify-between text-xs font-semibold text-muted-foreground pb-2 border-b border-border/80">
                  <span>Evaluated Code Criteria</span>
                  <span>Percentile Rank</span>
                </div>
                {current.skills.map((sk) => (
                  <div key={sk.name} className="space-y-1.5">
                    <div className="flex justify-between text-xs font-semibold text-foreground">
                      <span>{sk.name}</span>
                      <span className="font-mono text-brand-blue">{sk.level}%</span>
                    </div>
                    <div className="w-full h-2 rounded-full bg-muted overflow-hidden">
                      <div
                        className="h-full rounded-full bg-brand-blue"
                        style={{ width: `${sk.level}%` }}
                      />
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>

        {/* Carousel Bottom Pagination Controls */}
        <div className="flex items-center justify-between pt-6 sm:pt-8 mt-6 border-t border-border/80">
          <div className="flex items-center gap-2">
            {SLIDES.map((_, i) => (
              <button
                key={i}
                onClick={() => setCurrentIndex(i)}
                className={`h-1.5 rounded-full transition-all ${
                  i === currentIndex ? 'w-6 bg-brand-navy dark:bg-brand-accent' : 'w-1.5 bg-muted-foreground/30'
                }`}
                aria-label={`Go to slide ${i + 1}`}
              />
            ))}
          </div>

          <div className="flex items-center gap-1.5">
            <button
              onClick={handlePrev}
              className="w-8 h-8 rounded-lg border border-border bg-surface hover:bg-muted/50 flex items-center justify-center text-foreground transition-all"
              aria-label="Previous Slide"
            >
              <ChevronLeft className="w-4 h-4" />
            </button>
            <button
              onClick={handleNext}
              className="w-8 h-8 rounded-lg border border-border bg-surface hover:bg-muted/50 flex items-center justify-center text-foreground transition-all"
              aria-label="Next Slide"
            >
              <ChevronRight className="w-4 h-4" />
            </button>
          </div>
        </div>
      </div>
    </section>
  );
};
