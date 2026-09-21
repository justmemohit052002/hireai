import React, { useState } from 'react';
import { NavLink, Link } from 'react-router-dom';
import { Menu, X, MessageSquare, Briefcase, FileText, LayoutDashboard, Trophy } from 'lucide-react';
import { ThemeToggle } from '@/components/common/ThemeToggle';
import { NotificationBell } from '@/components/common/NotificationBell';
import { ProfileMenu } from '@/components/common/ProfileMenu';
import { Logo } from '@/components/common/Logo';
import { CANDIDATE_NAV_ITEMS, RECRUITER_NAV_ITEMS, ROUTES } from '@/constants';
import { useChat } from '@/context/ChatContext';
import { cn } from '@/utils';

const iconMap = {
  'Browse Jobs': <Briefcase className="w-4 h-4" />,
  'Applications': <FileText className="w-4 h-4" />,
  'Dashboard': <LayoutDashboard className="w-4 h-4" />,
  'Jobs': <Briefcase className="w-4 h-4" />,
  'Leaderboard': <Trophy className="w-4 h-4" />,
  'Inbox': <MessageSquare className="w-4 h-4" />,
};

export const TopNav = ({ title, role }) => {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const { unreadTotal = 0 } = useChat() || {};
  const isCandidate = role === 'candidate';

  // Candidate navbar keeps "Browse Jobs", "Applications", and "Inbox"
  const navItems = isCandidate
    ? CANDIDATE_NAV_ITEMS.filter(
        (item) => item.label === 'Browse Jobs' || item.label === 'Applications' || item.label === 'Inbox'
      )
    : RECRUITER_NAV_ITEMS.filter((item) => item.label !== 'Profile' && item.label !== 'Settings');

  const homeRoute = isCandidate ? ROUTES.CANDIDATE_JOBS : ROUTES.RECRUITER_DASHBOARD;

  return (
    <header className="sticky top-3 sm:top-4 z-40 flex flex-col items-center justify-center w-full mb-4 sm:mb-6 px-2 sm:px-4 pointer-events-none">
      <nav className="pointer-events-auto flex items-center justify-between w-full max-w-6xl h-14 sm:h-16 px-4 sm:px-6 rounded-full glass border border-border shadow-2xl backdrop-blur-xl">
        {/* Brand Logo - Matches Landing Navbar */}
        <Link to={homeRoute} className="flex items-center gap-2 shrink-0">
          <Logo size="md" />
        </Link>

        {/* Center Navigation Links (Desktop) */}
        <div className="hidden md:flex items-center gap-3 lg:gap-4 text-sm font-semibold">
          {navItems.map((item) => {
            const isInbox = item.label === 'Inbox';
            return (
              <NavLink
                key={item.href}
                to={item.href}
                className={({ isActive }) =>
                  cn(
                    'relative px-3.5 py-1.5 rounded-full text-xs sm:text-sm font-semibold transition-all duration-200 whitespace-nowrap flex items-center gap-1.5',
                    isActive
                      ? 'bg-brand-blue text-white shadow-md shadow-brand-blue/20 font-bold scale-[1.02]'
                      : 'text-muted-foreground hover:text-foreground hover:bg-surface-2/60'
                  )
                }
              >
                <span>{item.label}</span>
                {isInbox && unreadTotal > 0 && (
                  <span className="px-1.5 py-0.2 rounded-full bg-brand-accent text-slate-950 text-[10px] font-extrabold shadow-2xs">
                    {unreadTotal}
                  </span>
                )}
              </NavLink>
            );
          })}
        </div>

        {/* Right Actions & Controls - ThemeToggle, NotificationBell, ProfileMenu, Mobile Toggle */}
        <div className="flex items-center gap-2 sm:gap-3 shrink-0">
          <ThemeToggle />
          <NotificationBell />
          <div className="h-5 w-px bg-border/60 mx-0.5" />
          <ProfileMenu />

          {/* Mobile Hamburger Button */}
          <button
            type="button"
            onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
            className="md:hidden p-2 rounded-full text-muted-foreground hover:text-foreground hover:bg-surface-2/60 transition-colors ml-1 cursor-pointer"
            aria-label="Toggle Navigation Menu"
          >
            {mobileMenuOpen ? <X className="w-5 h-5 text-foreground" /> : <Menu className="w-5 h-5" />}
          </button>
        </div>
      </nav>

      {/* Mobile Drawer Dropdown */}
      {mobileMenuOpen && (
        <div className="pointer-events-auto md:hidden w-full max-w-6xl mt-2 p-3 rounded-2xl glass border border-border shadow-2xl backdrop-blur-2xl animate-in fade-in slide-in-from-top-2 duration-200">
          <div className="grid grid-cols-2 gap-2">
            {navItems.map((item) => {
              const isInbox = item.label === 'Inbox';
              return (
                <NavLink
                  key={item.href}
                  to={item.href}
                  onClick={() => setMobileMenuOpen(false)}
                  className={({ isActive }) =>
                    cn(
                      'flex items-center gap-2 px-3 py-2.5 rounded-xl text-xs font-semibold transition-all',
                      isActive
                        ? 'bg-brand-blue text-white font-bold shadow-md shadow-brand-blue/20'
                        : 'text-muted-foreground hover:text-foreground hover:bg-surface-2/60'
                    )
                  }
                >
                  <span className="shrink-0">{iconMap[item.label]}</span>
                  <span className="truncate">{item.label}</span>
                  {isInbox && unreadTotal > 0 && (
                    <span className="ml-auto px-1.5 py-0.2 rounded-full bg-brand-accent text-slate-950 text-[10px] font-black">
                      {unreadTotal}
                    </span>
                  )}
                </NavLink>
              );
            })}
          </div>
        </div>
      )}
    </header>
  );
};
