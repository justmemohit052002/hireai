import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '@/context/AuthContext';
import { ROUTES } from '@/constants';

import { LandingPageLoader } from './components/LandingPageLoader';
import { LandingHero } from './components/LandingHero';
import { LandingLogoMarquee } from './components/LandingLogoMarquee';
import { LandingShowcaseCarousel } from './components/LandingShowcaseCarousel';
import { LandingHiring } from './components/LandingHiring';
import { LandingStats } from './components/LandingStats';
import { LandingWorks } from './components/LandingWorks';
import { LandingFeatures } from './components/LandingFeatures';
import { LandingCTA } from './components/LandingCTA';

export const LandingPage = () => {
  const { isAuthenticated, role } = useAuth();
  const navigate = useNavigate();
  const [showPageLoader, setShowPageLoader] = useState(true);

  // Redirect authenticated users to their respective dashboards
  useEffect(() => {
    if (isAuthenticated) {
      if (role === 'candidate') {
        navigate(ROUTES.CANDIDATE_JOBS, { replace: true });
      } else if (role === 'recruiter') {
        navigate(ROUTES.RECRUITER_DASHBOARD, { replace: true });
      }
    }
  }, [isAuthenticated, role, navigate]);

  return (
    <>
      {/* 0. Initial Web Page Loading Animation Effect */}
      {showPageLoader && (
        <LandingPageLoader onComplete={() => setShowPageLoader(false)} />
      )}

      <div className="space-y-4 pb-20 overflow-x-hidden animate-fade-in-up">
        {/* 1. Hero Section With Floating Candidate Hiring Badges */}
        <LandingHero />

        {/* 2. Continuous Horizontal Marquee Slider (Wellfound / Top Companies Style) */}
        <LandingLogoMarquee />

        {/* 3. Interactive Candidate & Hiring Showcase Carousel */}
        <LandingShowcaseCarousel />

        {/* 4. Dual Audience Hiring Section (For Recruiters & Candidates) */}
        <LandingHiring />

        {/* 5. Platform Stats Grid */}
        <LandingStats />

        {/* 6. How Candidate Hiring Works 6-Step Workflow */}
        <LandingWorks />

        {/* 7. Powerful Features Section */}
        <LandingFeatures />

        {/* 8. Call to Action Banner */}
        <LandingCTA />
      </div>
    </>
  );
};
