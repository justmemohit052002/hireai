import React from 'react';
import { NavLink, Link } from 'react-router-dom';
import { ThemeToggle } from '@/components/common/ThemeToggle';
import { NotificationBell } from '@/components/common/NotificationBell';
import { ProfileMenu } from '@/components/common/ProfileMenu';
import { Logo } from '@/components/common/Logo';
import { CANDIDATE_NAV_ITEMS, RECRUITER_NAV_ITEMS, ROUTES } from '@/constants';
import { cn } from '@/utils';

export const TopNav = ({ title, role }) => {
  const isCandidate = role === 'candidate';
  // Candidate navbar keeps only "Browse Jobs" and "Applications" as primary links (Profile, Inbox, Settings are accessible via user profile menu)
  const navItems = isCandidate
    ? CANDIDATE_NAV_ITEMS.filter((item) => item.label === 'Browse Jobs' || item.label === 'Applications')
    : RECRUITER_NAV_ITEMS.filter((item) => item.label !== 'Profile' && item.label !== 'Settings');
  const homeRoute = isCandidate ? ROUTES.CANDIDATE_JOBS : ROUTES.RECRUITER_DASHBOARD;

  return (
    <header className="sticky top-3 z-40 flex justify-center w-full mt-3 mb-6 px-4 pointer-events-none">
      <nav className="pointer-events-auto flex items-center justify-between w-full max-w-6xl h-16 px-6 glass-island text-foreground">
        {/* Brand Logo - Matches Landing Navbar */}
        <Link to={homeRoute} className="flex items-center gap-2 shrink-0">
          <Logo size="md" />
        </Link>

        {/* Center Navigation Links - Candidate shows Browse Jobs & Applications */}
        <div className="hidden md:flex items-center gap-2 text-sm font-semibold">
          {navItems.map((item) => (
            <NavLink
              key={item.href}
              to={item.href}
              className={({ isActive }) =>
                cn(
                  'px-4 py-1.5 rounded-full text-xs sm:text-sm font-semibold transition-all duration-150 whitespace-nowrap',
                  isActive
                    ? 'bg-brand-blue text-white shadow-xs font-bold'
                    : 'text-muted-foreground hover:text-foreground hover:bg-surface-2'
                )
              }
            >
              {item.label}
            </NavLink>
          ))}
        </div>

        {/* Right Actions & Controls - ThemeToggle, NotificationBell, ProfileMenu */}
        <div className="flex items-center gap-3 shrink-0">
          <ThemeToggle />
          <NotificationBell />
          <div className="h-5 w-px bg-[var(--glass-border)] mx-0.5" />
          <ProfileMenu />
        </div>
      </nav>
    </header>
  );
};
