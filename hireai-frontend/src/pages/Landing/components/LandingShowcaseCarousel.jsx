import React, { useState, useEffect, useRef } from 'react';
import { 
  Sparkles, 
  UserCheck, 
  UserPlus, 
  Briefcase, 
  BadgeCheck, 
  SearchCheck, 
  GraduationCap, 
  Award, 
  Users, 
  CheckCircle2, 
  Clock, 
  ChevronLeft, 
  ChevronRight, 
  ArrowRight,
  Send,
  Building2,
  FileCheck
} from 'lucide-react';
import { Button } from '@/components/ui/Button';
import { Link } from 'react-router-dom';
import { ROUTES } from '@/constants';

const SLIDES = [
  {
    id: 'ats-match',
    tabTitle: 'AI Candidate Match',
    tabIcon: UserCheck,
    tag: 'NEURAL ATS ENGINE',
    title: 'Precision Candidate Matching With 96%+ Semantic Accuracy',
    description:
      'Eliminate hundreds of hours wasted reviewing unqualified resumes. HireAI deeply analyzes candidate technical repositories, verified projects, and career trajectory.',
    statLabel: 'Candidate Match Rate',
    statValue: '96.4%',
    cardBadge: 'Top 1% Candidate Match',
    candidate: {
      name: 'Aarav Sharma',
      role: 'Senior Distributed Systems Architect',
      experience: '7+ Years • Ex-Uber & Microsoft',
      avatarLetter: 'A',
      appliedFor: 'Staff Backend Engineer @ CloudScale',
      matchScore: 96,
      skills: ['Go', 'Kubernetes', 'Kafka', 'Distributed Systems', 'gRPC', 'PostgreSQL'],
      assessment: 'Demonstrated top-tier distributed consensus & high-throughput concurrency design.',
      status: 'Shortlisted for Technical Round',
    },
  },
  {
    id: 'hiring-pipeline',
    tabTitle: 'Automated Pipeline',
    tabIcon: Briefcase,
    tag: 'CANDIDATE FUNNEL WORKFLOW',
    title: 'Streamlined Hiring Pipeline from First Application to Offer Letter',
    description:
      'Manage candidates across automated workflow stages without spreadsheet chaos. Automated status alerts keep both recruiters and candidates in sync.',
    statLabel: 'Time-to-Hire Reduction',
    statValue: '73%',
    cardBadge: 'Live Candidate Flow',
    pipelineStages: [
      { name: 'Application Received', count: 48, time: '2m ago', active: false, icon: FileCheck },
      { name: 'AI Skill Screened', count: 24, time: '15m ago', active: false, icon: SearchCheck },
      { name: 'Shortlisted for Interview', count: 8, time: '1h ago', active: true, icon: UserCheck },
      { name: 'Offer Extended & Signed', count: 3, time: 'Today', active: false, icon: Award },
    ],
  },
  {
    id: 'skill-radar',
    tabTitle: 'Technical Skill Radar',
    tabIcon: BadgeCheck,
    tag: 'VERIFIED CANDIDATE DNA',
    title: 'Multi-Axis Technical Capability & Code Architecture Scoring',
    description:
      'Go beyond resume claims with multi-dimensional skill evaluation. Analyze system architecture, algorithmic logic, testing practices, and delivery velocity.',
    statLabel: 'Verification Accuracy',
    statValue: '99.1%',
    cardBadge: 'Code & Architecture Radar',
    radarSkills: [
      { name: 'Distributed Systems & Microservices', level: 98, color: 'bg-emerald-500' },
      { name: 'Algorithmic Problem Solving', level: 94, color: 'bg-brand-blue' },
      { name: 'Database Tuning & Concurrency', level: 91, color: 'bg-[#C63FC5]' },
      { name: 'Cloud Infrastructure & DevOps', level: 88, color: 'bg-[#FC9559]' },
      { name: 'API Security & Resilience', level: 95, color: 'bg-indigo-500' },
    ],
    verifiedTag: 'LeetCode & GitHub Architecture Verified',
  },
  {
    id: 'decision-engine',
    tabTitle: 'Decision Engine & AI Chat',
    tabIcon: Users,
    tag: 'EXECUTIVE RECRUITER COPILOT',
    title: 'Conversational Talent Discovery and Instant Candidate Insights',
    description:
      'Search through thousands of engineering candidates using natural language. Query skills, past scale, and culture alignment with contextual AI.',
    statLabel: 'Recruiter Time Saved',
    statValue: '18 hrs/wk',
    cardBadge: 'Executive AI Copilot',
    chatDemo: [
      {
        sender: 'Recruiter',
        text: 'Find candidates with hands-on Kafka and Go experience who built low-latency APIs.',
      },
      {
        sender: 'HireAI Assistant',
        text: 'Identified 12 verified candidates. Top recommendation: Ananya Patel (98% match, 6 yrs experience, 400k QPS event pipeline).',
        highlightCandidate: {
          name: 'Ananya Patel',
          score: '98%',
          status: 'Open to Immediate Interview',
        },
      },
    ],
  },
];

export const LandingShowcaseCarousel = () => {
  const [currentIndex, setCurrentIndex] = useState(0);
  const [isPaused, setIsPaused] = useState(false);
  const timerRef = useRef(null);

  // Auto-advance slides every 5.5 seconds unless hovered
  useEffect(() => {
    if (isPaused) return;

    timerRef.current = setInterval(() => {
      setCurrentIndex((prev) => (prev + 1) % SLIDES.length);
    }, 5500);

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

  const currentSlide = SLIDES[currentIndex];

  return (
    <section 
      className="py-16 md:py-24 relative overflow-hidden"
      onMouseEnter={() => setIsPaused(true)}
      onMouseLeave={() => setIsPaused(false)}
    >
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        {/* Section Header */}
        <div className="text-center max-w-3xl mx-auto mb-10 sm:mb-14">
          <div className="inline-flex items-center gap-2 px-4 py-1.5 rounded-full bg-brand-blue-light/30 dark:bg-brand-blue-light/15 border border-brand-blue/30 text-brand-navy dark:text-brand-blue-light font-mono text-xs font-bold uppercase tracking-wider mb-4 shadow-xs">
            <UserCheck className="w-3.5 h-3.5 text-brand-blue" />
            <span>Interactive Hiring Platform Showcase</span>
          </div>

          <h2 className="text-3xl sm:text-4xl lg:text-5xl font-extrabold font-heading text-foreground tracking-tight">
            Built for High-Growth Teams{' '}
            <span className="gradient-text-brand">Hiring Top Candidates</span>
          </h2>
          <p className="mt-4 text-base sm:text-lg text-muted-foreground font-sans">
            Explore how modern tech recruiters shortlist and interview elite talent with automated intelligence.
          </p>
        </div>

        {/* Carousel Tabs Navigation */}
        <div className="flex flex-wrap items-center justify-center gap-2 sm:gap-3 mb-8 sm:mb-12">
          {SLIDES.map((slide, idx) => {
            const Icon = slide.tabIcon;
            const isActive = idx === currentIndex;
            return (
              <button
                key={slide.id}
                onClick={() => setCurrentIndex(idx)}
                className={`flex items-center gap-2 px-4 sm:px-5 py-2.5 rounded-full text-xs sm:text-sm font-semibold transition-all duration-300 border ${
                  isActive
                    ? 'bg-brand-navy text-white dark:bg-brand-accent dark:text-brand-dark border-transparent shadow-lg shadow-brand-navy/20 dark:shadow-brand-accent/20 scale-105'
                    : 'bg-surface/80 text-muted-foreground hover:text-foreground border-border/60 hover:border-brand-blue/30'
                }`}
              >
                <Icon className={`w-4 h-4 ${isActive ? 'text-brand-accent dark:text-brand-dark' : 'text-brand-blue'}`} />
                <span>{slide.tabTitle}</span>
              </button>
            );
          })}
        </div>

        {/* Carousel Main Container */}
        <div className="relative rounded-3xl bg-surface/90 border border-border/80 shadow-2xl p-6 sm:p-10 lg:p-12 backdrop-blur-md overflow-hidden">
          {/* Ambient Lighting Orbs */}
          <div className="pointer-events-none absolute -top-24 -left-24 w-72 h-72 bg-brand-blue/15 rounded-full blur-3xl" />
          <div className="pointer-events-none absolute -bottom-24 -right-24 w-72 h-72 bg-[#C63FC5]/15 rounded-full blur-3xl" />

          {/* Slide Content Grid */}
          <div className="grid lg:grid-cols-12 gap-8 lg:gap-12 items-center relative z-10">
            {/* Left Narrative Column (5 cols) */}
            <div className="lg:col-span-5 space-y-6">
              <div className="inline-flex items-center gap-2 text-xs font-mono font-bold uppercase tracking-wider text-brand-blue dark:text-brand-accent">
                <Sparkles className="w-3.5 h-3.5" />
                <span>{currentSlide.tag}</span>
              </div>

              <h3 className="text-2xl sm:text-3xl lg:text-4xl font-bold font-heading text-foreground leading-tight">
                {currentSlide.title}
              </h3>

              <p className="text-sm sm:text-base text-muted-foreground leading-relaxed font-sans">
                {currentSlide.description}
              </p>

              {/* Dynamic Metric Pill */}
              <div className="flex items-center gap-4 pt-2">
                <div className="p-4 rounded-2xl bg-muted/40 dark:bg-muted/20 border border-border/80">
                  <div className="text-3xl sm:text-4xl font-extrabold font-heading text-brand-navy dark:text-brand-accent">
                    {currentSlide.statValue}
                  </div>
                  <div className="text-xs font-medium text-muted-foreground mt-0.5">
                    {currentSlide.statLabel}
                  </div>
                </div>

                <div className="space-y-1">
                  <div className="flex items-center gap-1.5 text-xs font-bold text-foreground">
                    <CheckCircle2 className="w-4 h-4 text-emerald-500" />
                    <span>Candidate Verified</span>
                  </div>
                  <p className="text-[11px] text-muted-foreground">
                    Live telemetry calculated in real-time
                  </p>
                </div>
              </div>

              {/* Primary Action Button */}
              <div className="pt-2">
                <Link to={ROUTES.SIGNUP}>
                  <Button variant="gradient" className="font-bold flex items-center gap-2 px-6 rounded-full">
                    <span>Hire Qualified Candidates</span>
                    <ArrowRight className="w-4 h-4" />
                  </Button>
                </Link>
              </div>
            </div>

            {/* Right Interactive Visual Card (7 cols) */}
            <div className="lg:col-span-7">
              {/* SLIDE 1: ATS MATCHING CANDIDATE SPOTLIGHT */}
              {currentSlide.id === 'ats-match' && currentSlide.candidate && (
                <div className="rounded-2xl bg-surface border border-border p-6 sm:p-8 shadow-xl space-y-6">
                  {/* Candidate Header */}
                  <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b border-border/60">
                    <div className="flex items-center gap-4">
                      <div className="w-14 h-14 rounded-2xl bg-gradient-to-tr from-brand-navy to-brand-blue text-white flex items-center justify-center font-bold text-xl shadow-md">
                        {currentSlide.candidate.avatarLetter}
                      </div>
                      <div>
                        <div className="flex items-center gap-2">
                          <h4 className="font-bold text-lg text-foreground font-heading">
                            {currentSlide.candidate.name}
                          </h4>
                          <span className="inline-flex items-center gap-1 text-[11px] font-semibold px-2 py-0.5 rounded-full bg-emerald-500/15 text-emerald-600 dark:text-emerald-400 border border-emerald-500/25">
                            <BadgeCheck className="w-3 h-3" /> Verified Talent
                          </span>
                        </div>
                        <p className="text-xs text-muted-foreground font-medium mt-0.5">
                          {currentSlide.candidate.role}
                        </p>
                        <p className="text-[11px] text-muted-foreground/80">
                          {currentSlide.candidate.experience}
                        </p>
                      </div>
                    </div>

                    {/* Match Badge */}
                    <div className="flex sm:flex-col items-center sm:items-end gap-2 shrink-0">
                      <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-brand-blue/15 text-brand-blue dark:text-brand-accent border border-brand-blue/30 font-mono font-bold text-sm">
                        <Sparkles className="w-3.5 h-3.5" />
                        <span>{currentSlide.candidate.matchScore}% ATS Match</span>
                      </span>
                      <span className="text-[11px] text-emerald-600 dark:text-emerald-400 font-semibold flex items-center gap-1">
                        <UserCheck className="w-3 h-3" /> Auto-Shortlisted
                      </span>
                    </div>
                  </div>

                  {/* Skills Grid */}
                  <div>
                    <div className="text-xs font-semibold text-muted-foreground mb-2 flex items-center justify-between">
                      <span>Evaluated Technical Skills</span>
                      <span className="text-brand-blue text-[11px]">All criteria matched</span>
                    </div>
                    <div className="flex flex-wrap gap-2">
                      {currentSlide.candidate.skills.map((skill) => (
                        <span
                          key={skill}
                          className="px-3 py-1 rounded-lg text-xs font-semibold bg-brand-blue-light/20 text-brand-navy dark:text-brand-blue-light border border-brand-blue/20 flex items-center gap-1.5"
                        >
                          <CheckCircle2 className="w-3 h-3 text-brand-blue" />
                          {skill}
                        </span>
                      ))}
                    </div>
                  </div>

                  {/* AI Assessment Quote Box */}
                  <div className="p-4 rounded-xl bg-muted/40 dark:bg-muted/20 border border-border/80 text-xs text-muted-foreground space-y-1">
                    <span className="font-bold text-foreground flex items-center gap-1.5">
                      <Sparkles className="w-3.5 h-3.5 text-brand-blue" /> AI Hiring Recommendation:
                    </span>
                    <p className="leading-relaxed">
                      "{currentSlide.candidate.assessment}"
                    </p>
                  </div>

                  {/* Action Bar */}
                  <div className="flex items-center justify-between gap-3 pt-2">
                    <span className="text-xs font-semibold text-muted-foreground flex items-center gap-1.5">
                      <Building2 className="w-3.5 h-3.5 text-brand-blue" />
                      Applied for: <strong className="text-foreground">{currentSlide.candidate.appliedFor}</strong>
                    </span>

                    <Button size="sm" variant="gradient" className="font-bold text-xs">
                      Invite to Interview
                    </Button>
                  </div>
                </div>
              )}

              {/* SLIDE 2: HIRING FUNNEL PIPELINE */}
              {currentSlide.id === 'hiring-pipeline' && (
                <div className="rounded-2xl bg-surface border border-border p-6 sm:p-8 shadow-xl space-y-6">
                  <div className="flex items-center justify-between pb-3 border-b border-border/60">
                    <div>
                      <h4 className="font-bold text-base text-foreground font-heading">
                        Senior Engineering Recruitment Pipeline
                      </h4>
                      <p className="text-xs text-muted-foreground">
                        Live candidate status tracking across active job positions
                      </p>
                    </div>
                    <span className="text-xs font-mono font-bold px-3 py-1 rounded-full bg-emerald-500/15 text-emerald-500 border border-emerald-500/25">
                      Pipeline Active
                    </span>
                  </div>

                  <div className="space-y-3">
                    {currentSlide.pipelineStages.map((stage, i) => {
                      const StageIcon = stage.icon;
                      return (
                        <div
                          key={stage.name}
                          className={`p-4 rounded-xl border transition-all flex items-center justify-between gap-4 ${
                            stage.active
                              ? 'bg-brand-blue-light/20 border-brand-blue/40 shadow-sm'
                              : 'bg-muted/30 dark:bg-muted/10 border-border/70'
                          }`}
                        >
                          <div className="flex items-center gap-3">
                            <div className={`w-9 h-9 rounded-xl flex items-center justify-center shrink-0 ${
                              stage.active 
                                ? 'bg-brand-blue text-white shadow-xs' 
                                : 'bg-muted text-muted-foreground'
                            }`}>
                              <StageIcon className="w-4 h-4" />
                            </div>
                            <div>
                              <div className="font-bold text-xs sm:text-sm text-foreground flex items-center gap-2">
                                <span>{stage.name}</span>
                                {stage.active && (
                                  <span className="text-[10px] font-mono px-2 py-0.2 rounded-full bg-brand-blue text-white">
                                    Current Stage
                                  </span>
                                )}
                              </div>
                              <span className="text-[11px] text-muted-foreground flex items-center gap-1 mt-0.5">
                                <Clock className="w-3 h-3" /> Updated {stage.time}
                              </span>
                            </div>
                          </div>

                          <div className="text-right shrink-0">
                            <span className="font-mono font-bold text-sm sm:text-base text-foreground">
                              {stage.count}
                            </span>
                            <div className="text-[10px] text-muted-foreground">Candidates</div>
                          </div>
                        </div>
                      );
                    })}
                  </div>
                </div>
              )}

              {/* SLIDE 3: TECHNICAL RADAR & ASSESSMENTS */}
              {currentSlide.id === 'skill-radar' && (
                <div className="rounded-2xl bg-surface border border-border p-6 sm:p-8 shadow-xl space-y-6">
                  <div className="flex items-center justify-between pb-3 border-b border-border/60">
                    <div>
                      <h4 className="font-bold text-base text-foreground font-heading">
                        Candidate Technical Skill Verification
                      </h4>
                      <p className="text-xs text-muted-foreground">
                        Algorithmic evaluation and architectural benchmarks
                      </p>
                    </div>
                    <span className="inline-flex items-center gap-1 text-xs font-mono font-bold px-3 py-1 rounded-full bg-brand-accent/25 text-brand-navy dark:text-brand-accent border border-brand-accent/30">
                      <Award className="w-3.5 h-3.5" /> 99th Percentile
                    </span>
                  </div>

                  {/* Skills Progress Bars */}
                  <div className="space-y-4">
                    {currentSlide.radarSkills.map((skill) => (
                      <div key={skill.name} className="space-y-1.5">
                        <div className="flex justify-between text-xs font-semibold text-foreground">
                          <span>{skill.name}</span>
                          <span className="font-mono font-bold text-brand-blue dark:text-brand-accent">
                            {skill.level}%
                          </span>
                        </div>
                        <div className="w-full h-2.5 rounded-full bg-muted/60 overflow-hidden">
                          <div
                            className={`h-full rounded-full transition-all duration-700 ${skill.color}`}
                            style={{ width: `${skill.level}%` }}
                          />
                        </div>
                      </div>
                    ))}
                  </div>

                  <div className="p-3.5 rounded-xl bg-emerald-500/10 border border-emerald-500/20 flex items-center justify-between gap-3 text-xs text-emerald-600 dark:text-emerald-400 font-medium">
                    <span className="flex items-center gap-1.5 font-bold">
                      <BadgeCheck className="w-4 h-4 text-emerald-500" />
                      {currentSlide.verifiedTag}
                    </span>
                    <span className="text-[11px] underline cursor-pointer hover:opacity-80">
                      View Audit Log
                    </span>
                  </div>
                </div>
              )}

              {/* SLIDE 4: DECISION ENGINE & CHAT */}
              {currentSlide.id === 'decision-engine' && (
                <div className="rounded-2xl bg-surface border border-border p-6 sm:p-8 shadow-xl space-y-4">
                  <div className="flex items-center justify-between pb-3 border-b border-border/60">
                    <div className="flex items-center gap-2.5">
                      <div className="w-8 h-8 rounded-full bg-brand-blue text-white flex items-center justify-center font-bold text-xs">
                        AI
                      </div>
                      <div>
                        <h4 className="font-bold text-sm text-foreground font-heading">
                          HireAI Recruiter Copilot
                        </h4>
                        <span className="text-[10px] text-emerald-500 font-medium flex items-center gap-1">
                          <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse" /> Live Query Ready
                        </span>
                      </div>
                    </div>
                    <span className="text-[11px] font-mono font-semibold text-muted-foreground">
                      Natural Language Search
                    </span>
                  </div>

                  {/* Chat Bubbles */}
                  <div className="space-y-3 pt-2">
                    <div className="flex justify-end">
                      <div className="max-w-[85%] bg-brand-navy text-white rounded-2xl rounded-tr-xs px-4 py-2.5 text-xs leading-relaxed shadow-sm">
                        {currentSlide.chatDemo[0].text}
                      </div>
                    </div>

                    <div className="flex justify-start">
                      <div className="max-w-[90%] bg-muted/50 dark:bg-muted/20 border border-border/80 rounded-2xl rounded-tl-xs p-4 text-xs leading-relaxed space-y-3">
                        <p className="text-foreground">
                          {currentSlide.chatDemo[1].text}
                        </p>

                        {/* Candidate Quick Card inside chat */}
                        <div className="p-3 rounded-xl bg-surface border border-brand-blue/30 flex items-center justify-between gap-3 shadow-xs">
                          <div className="flex items-center gap-2.5">
                            <div className="w-8 h-8 rounded-xl bg-brand-blue-light/30 text-brand-navy dark:text-brand-blue-light font-bold flex items-center justify-center text-xs">
                              AP
                            </div>
                            <div>
                              <div className="font-bold text-foreground text-xs">
                                {currentSlide.chatDemo[1].highlightCandidate.name}
                              </div>
                              <div className="text-[10px] text-muted-foreground">
                                {currentSlide.chatDemo[1].highlightCandidate.status}
                              </div>
                            </div>
                          </div>

                          <div className="flex items-center gap-2">
                            <span className="px-2 py-0.5 rounded-full bg-brand-blue/15 text-brand-blue dark:text-brand-accent font-bold text-[11px]">
                              {currentSlide.chatDemo[1].highlightCandidate.score}
                            </span>
                            <Button size="xs" variant="primary" className="text-[10px] px-2.5 py-1">
                              View Profile
                            </Button>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              )}
            </div>
          </div>

          {/* Carousel Footer Controls */}
          <div className="flex items-center justify-between pt-8 sm:pt-10 mt-8 border-t border-border/60">
            {/* Slide Indicator Dots */}
            <div className="flex items-center gap-2">
              {SLIDES.map((_, idx) => (
                <button
                  key={idx}
                  onClick={() => setCurrentIndex(idx)}
                  className={`h-2 rounded-full transition-all duration-300 ${
                    idx === currentIndex
                      ? 'w-8 bg-brand-blue dark:bg-brand-accent'
                      : 'w-2 bg-muted-foreground/30 hover:bg-muted-foreground/50'
                  }`}
                  aria-label={`Slide ${idx + 1}`}
                />
              ))}
            </div>

            {/* Left / Right Nav Arrows */}
            <div className="flex items-center gap-2">
              <button
                onClick={handlePrev}
                className="w-9 h-9 rounded-full bg-muted/60 hover:bg-muted text-foreground border border-border/70 flex items-center justify-center transition-all hover:scale-105"
                aria-label="Previous Slide"
              >
                <ChevronLeft className="w-4 h-4" />
              </button>
              <button
                onClick={handleNext}
                className="w-9 h-9 rounded-full bg-muted/60 hover:bg-muted text-foreground border border-border/70 flex items-center justify-center transition-all hover:scale-105"
                aria-label="Next Slide"
              >
                <ChevronRight className="w-4 h-4" />
              </button>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
};
