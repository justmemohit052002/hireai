import React, { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '@/context/AuthContext';
import { ROUTES } from '@/constants';

import { LandingHero } from './components/LandingHero';
import { LandingHiring } from './components/LandingHiring';
import { LandingStats } from './components/LandingStats';
import { LandingWorks } from './components/LandingWorks';
import { LandingFeatures } from './components/LandingFeatures';
import { LandingCTA } from './components/LandingCTA';

export const LandingPage = () => {
  const { isAuthenticated, role } = useAuth();
  const navigate = useNavigate();

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
    <div className="space-y-4 pb-20 overflow-x-hidden">
      {/* 1. Hero Section */}
      <LandingHero />

      {/* 2. Dual Audience Hiring Section */}
      <LandingHiring />

      {/* 3. Platform Stats Grid */}
      <LandingStats />

      {/* 4. How Hiring Works Workflow */}
      <LandingWorks />

      {/* 5. Powerful Features Section */}
      <LandingFeatures />

      {/* 6. Call to Action Banner */}
      <LandingCTA />
    </div>
  );
};
