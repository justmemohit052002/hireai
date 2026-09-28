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
      {/* 0. Non-blocking Top Page Stream Loading Animation */}
      {showPageLoader && (
        <LandingPageLoader onComplete={() => setShowPageLoader(false)} />
      )}

      <div className="space-y-6 sm:space-y-10 pb-20 overflow-x-hidden">
        {/* 1. Hero Section With Live Interactive ATS Dashboard Preview */}
        <LandingHero />

        {/* 2. Continuous Horizontal Marquee Slider (Wellfound Style) */}
        <LandingLogoMarquee />

        {/* 3. Interactive Candidate & Hiring Showcase Carousel */}
        <LandingShowcaseCarousel />

        {/* 4. Dual Audience Hiring Section (Recruiters & Engineers) */}
        <LandingHiring />

        {/* 5. Platform Performance Stats */}
        <LandingStats />

        {/* 6. How Candidate Hiring Works 4-Step Progressive Timeline */}
        <LandingWorks />

        {/* 7. Platform Capabilities Feature Grid */}
        <LandingFeatures />

        {/* 8. Call to Action Banner */}
        <LandingCTA />
      </div>
    </>
  );
};
