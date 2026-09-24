import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Menu, X } from 'lucide-react';
import { Logo } from '@/components/common/Logo';
import { ThemeToggle } from '@/components/common/ThemeToggle';
import { Button } from '@/components/ui/Button';
import { ROUTES } from '@/constants';
import { useAuth } from '@/context/AuthContext';

export const FloatingNavbar = () => {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const { isAuthenticated, role } = useAuth();
  const navigate = useNavigate();

  const handleDashboardClick = () => {
    if (role === 'candidate') {
      navigate(ROUTES.CANDIDATE_JOBS);
    } else {
      navigate(ROUTES.RECRUITER_DASHBOARD);
    }
    setMobileMenuOpen(false);
  };

  return (
    <header className="fixed top-3 sm:top-4 inset-x-0 z-50 flex flex-col items-center justify-center px-3 sm:px-4 pointer-events-none">
      <nav className="pointer-events-auto flex items-center justify-between w-full max-w-6xl h-14 sm:h-16 px-4 sm:px-6 rounded-full glass border border-white/20 dark:border-white/10 shadow-2xl backdrop-blur-xl">
        {/* Brand Logo */}
        <Link to="/" className="flex items-center gap-2 shrink-0">
          <Logo size="md" />
        </Link>

        {/* Center Links (Desktop) */}
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
        <div className="flex items-center gap-2 sm:gap-3 shrink-0">
          <ThemeToggle />

          {isAuthenticated ? (
            <Button onClick={handleDashboardClick} variant="default" size="sm" className="rounded-full hidden sm:inline-flex text-xs">
              Dashboard
            </Button>
          ) : (
            <div className="hidden sm:flex items-center gap-2">
              <Link to={ROUTES.LOGIN}>
                <Button variant="ghost" size="sm" className="rounded-full text-xs">
                  Sign In
                </Button>
              </Link>
              <Link to={ROUTES.SIGNUP}>
                <Button variant="gradient" size="sm" className="rounded-full text-xs shadow-md shadow-brand-blue/20">
                  Get Started
                </Button>
              </Link>
            </div>
          )}

          {/* Mobile Menu Toggle Button */}
          <button
            type="button"
            onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
            className="md:hidden p-2 rounded-full text-muted-foreground hover:text-foreground hover:bg-surface-2/60 transition-colors cursor-pointer"
            aria-label="Toggle Landing Menu"
          >
            {mobileMenuOpen ? <X className="w-5 h-5 text-foreground" /> : <Menu className="w-5 h-5" />}
          </button>
        </div>
      </nav>

      {/* Mobile Drawer Dropdown */}
      {mobileMenuOpen && (
        <div className="pointer-events-auto md:hidden w-full max-w-6xl mt-2 p-4 rounded-2xl glass border border-white/20 dark:border-white/10 shadow-2xl backdrop-blur-2xl space-y-3 animate-in fade-in slide-in-from-top-2 duration-200">
          <div className="flex flex-col gap-2 text-sm font-semibold text-muted-foreground">
            <a
              href="#hiring"
              onClick={() => setMobileMenuOpen(false)}
              className="px-3 py-2 rounded-xl hover:bg-surface-2/60 hover:text-foreground transition-colors"
            >
              For HR & Candidates
            </a>
            <a
              href="#features"
              onClick={() => setMobileMenuOpen(false)}
              className="px-3 py-2 rounded-xl hover:bg-surface-2/60 hover:text-foreground transition-colors"
            >
              Features
            </a>
            <a
              href="#how-it-works"
              onClick={() => setMobileMenuOpen(false)}
              className="px-3 py-2 rounded-xl hover:bg-surface-2/60 hover:text-foreground transition-colors"
            >
              How it Works
            </a>
          </div>

          <div className="pt-2 border-t border-border/40 flex flex-col gap-2">
            {isAuthenticated ? (
              <Button onClick={handleDashboardClick} variant="default" size="sm" className="w-full rounded-xl">
                Go to Dashboard
              </Button>
            ) : (
              <div className="grid grid-cols-2 gap-2">
                <Link to={ROUTES.LOGIN} onClick={() => setMobileMenuOpen(false)}>
                  <Button variant="outline" size="sm" className="w-full rounded-xl text-xs">
                    Sign In
                  </Button>
                </Link>
                <Link to={ROUTES.SIGNUP} onClick={() => setMobileMenuOpen(false)}>
                  <Button variant="gradient" size="sm" className="w-full rounded-xl text-xs shadow-md">
                    Get Started
                  </Button>
                </Link>
              </div>
            )}
          </div>
        </div>
      )}
    </header>
  );
};
