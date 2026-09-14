import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Logo } from '@/components/common/Logo';
import { ThemeToggle } from '@/components/common/ThemeToggle';
import { Button } from '@/components/ui/Button';
import { ROUTES } from '@/constants';
import { useAuth } from '@/context/AuthContext';

export const FloatingNavbar = () => {
  const { isAuthenticated, role } = useAuth();
  const navigate = useNavigate();

  const handleDashboardClick = () => {
    if (role === 'candidate') {
      navigate(ROUTES.CANDIDATE_JOBS);
    } else {
      navigate(ROUTES.RECRUITER_DASHBOARD);
    }
  };

  return (
    <header className="fixed top-0 inset-x-0 z-50 flex justify-center px-4 mt-3 pointer-events-none">
      <nav className="pointer-events-auto flex items-center justify-between w-full max-w-6xl h-16 px-6 glass-island text-foreground">
        {/* Brand Logo */}
        <Link to="/" className="flex items-center gap-2 shrink-0">
          <Logo size="md" />
        </Link>

        {/* Center Links */}
        <div className="hidden md:flex items-center gap-8 text-sm font-semibold text-muted-foreground">
          <a href="#hiring" className="hover:text-foreground transition-colors">
            For HR & Candidates
          </a>
          <a href="#features" className="hover:text-foreground transition-colors">
            Features
          </a>
          <a href="#how-it-works" className="hover:text-foreground transition-colors">
            How it Works
          </a>
        </div>

        {/* Right Actions */}
        <div className="flex items-center gap-3 shrink-0">
          <ThemeToggle />

          {isAuthenticated ? (
            <Button onClick={handleDashboardClick} variant="default" size="sm" className="rounded-full">
              Go to Dashboard
            </Button>
          ) : (
            <div className="flex items-center gap-2">
              <Link to={ROUTES.LOGIN}>
                <Button variant="outline" size="sm" className="rounded-full">
                  Sign In
                </Button>
              </Link>
              <Link to={ROUTES.SIGNUP}>
                <Button variant="accent" size="sm" className="rounded-full font-semibold">
                  Get Started
                </Button>
              </Link>
            </div>
          )}
        </div>
      </nav>
    </header>
  );
};
