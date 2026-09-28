import React, { useState, useEffect } from 'react';
import { Sparkles, UserCheck, Briefcase } from 'lucide-react';

export const LandingPageLoader = ({ onComplete }) => {
  const [progress, setProgress] = useState(15);
  const [loadingText, setLoadingText] = useState('Initializing AI Talent Graph...');
  const [isFadingOut, setIsFadingOut] = useState(false);

  useEffect(() => {
    // Step 1: Rapid realistic progress progression
    const t1 = setTimeout(() => {
      setProgress(45);
      setLoadingText('Loading Verified Candidate Pipeline...');
    }, 180);

    const t2 = setTimeout(() => {
      setProgress(85);
      setLoadingText('Calibrating Neural ATS Scoring...');
    }, 400);

    const t3 = setTimeout(() => {
      setProgress(100);
      setLoadingText('Welcome to HireAI');
    }, 650);

    const t4 = setTimeout(() => {
      setIsFadingOut(true);
    }, 850);

    const t5 = setTimeout(() => {
      if (onComplete) onComplete();
    }, 1150);

    return () => {
      clearTimeout(t1);
      clearTimeout(t2);
      clearTimeout(t3);
      clearTimeout(t4);
      clearTimeout(t5);
    };
  }, [onComplete]);

  return (
    <div
      className={`fixed inset-0 z-50 flex flex-col items-center justify-center bg-background/95 backdrop-blur-xl transition-all duration-400 ease-out ${
        isFadingOut ? 'opacity-0 pointer-events-none scale-102' : 'opacity-100'
      }`}
    >
      {/* Top Thin Progress Bar */}
      <div className="fixed top-0 left-0 right-0 h-1 bg-muted/40 overflow-hidden">
        <div
          className="h-full bg-gradient-to-r from-brand-blue via-[#C63FC5] to-brand-accent transition-all duration-300 ease-out shadow-xs"
          style={{ width: `${progress}%` }}
        />
      </div>

      {/* Center Branding Animation */}
      <div className="flex flex-col items-center max-w-sm px-6 text-center space-y-6">
        {/* Animated Glowing Logo Shield */}
        <div className="relative">
          {/* Pulsing Glow Ring */}
          <div className="absolute -inset-4 rounded-3xl bg-gradient-to-tr from-brand-blue/30 via-[#C63FC5]/30 to-brand-accent/30 blur-xl animate-pulse" />

          {/* Logo Badge */}
          <div className="relative w-20 h-20 rounded-3xl bg-surface border border-border shadow-2xl flex items-center justify-center transform transition-transform duration-300 hover:scale-105">
            <div className="w-14 h-14 rounded-2xl bg-gradient-to-br from-brand-navy to-brand-blue flex items-center justify-center shadow-inner">
              <span className="text-2xl font-black font-heading text-white tracking-wider flex items-center">
                H<span className="text-brand-accent">.</span>
              </span>
            </div>
            {/* Tiny Candidate Hiring Icon */}
            <div className="absolute -bottom-1 -right-1 w-7 h-7 rounded-full bg-brand-accent text-brand-dark flex items-center justify-center shadow-md border-2 border-surface">
              <UserCheck className="w-3.5 h-3.5 stroke-[2.5]" />
            </div>
          </div>
        </div>

        {/* Text and Status */}
        <div className="space-y-2">
          <div className="flex items-center justify-center gap-2">
            <h2 className="text-2xl font-black font-heading text-foreground tracking-tight">
              Hire<span className="text-brand-blue">AI</span>
            </h2>
            <span className="text-[10px] font-mono font-bold px-2 py-0.5 rounded-full bg-brand-blue-light/30 text-brand-navy dark:text-brand-blue-light border border-brand-blue/20">
              PRO
            </span>
          </div>

          <p className="text-xs font-mono text-muted-foreground flex items-center justify-center gap-1.5 transition-all">
            <Sparkles className="w-3 h-3 text-brand-blue animate-spin" />
            <span>{loadingText}</span>
          </p>
        </div>

        {/* Segmented Loading Bar */}
        <div className="w-48 h-1.5 rounded-full bg-muted/60 overflow-hidden p-0.5">
          <div
            className="h-full rounded-full bg-brand-blue dark:bg-brand-accent transition-all duration-300 ease-out"
            style={{ width: `${progress}%` }}
          />
        </div>
      </div>
    </div>
  );
};
