import React, { useState } from 'react';
import { NavLink, useLocation } from 'react-router-dom';
import {
  Briefcase,
  FileText,
  MessageSquare,
  User,
  Settings,
  LayoutDashboard,
  Trophy,
  Building2,
  ChevronLeft,
  ChevronRight,
  LogOut,
  Sparkles,
} from 'lucide-react';
import { Logo } from '@/components/common/Logo';
import { useAuth } from '@/context/AuthContext';
import { CANDIDATE_NAV_ITEMS, RECRUITER_NAV_ITEMS, ROUTES } from '@/constants';
import { cn } from '@/utils';
import { Button } from '@/components/ui/Button';

const iconMap = {
  Briefcase: <Briefcase className="w-5 h-5" />,
  FileText: <FileText className="w-5 h-5" />,
  MessageSquare: <MessageSquare className="w-5 h-5" />,
  User: <User className="w-5 h-5" />,
  Settings: <Settings className="w-5 h-5" />,
  LayoutDashboard: <LayoutDashboard className="w-5 h-5" />,
  Trophy: <Trophy className="w-5 h-5" />,
  Building2: <Building2 className="w-5 h-5" />,
};

export const Sidebar = ({ role }) => {
  const [collapsed, setCollapsed] = useState(false);
  const { logout } = useAuth();
  const location = useLocation();

  const navItems = role === 'candidate' ? CANDIDATE_NAV_ITEMS : RECRUITER_NAV_ITEMS;

  return (
    <aside
      className={cn(
        'fixed top-4 bottom-4 left-4 z-40 flex flex-col rounded-[24px] bg-brand-dark border border-border p-4 transition-all duration-300 shadow-2xl text-white',
        collapsed ? 'w-20' : 'w-64'
      )}
    >
      {/* Brand & Collapse */}
      <div className="flex items-center justify-between pb-6 mb-2 border-b border-white/10">
        <Logo iconOnly={collapsed} size="md" />
        <button
          onClick={() => setCollapsed(!collapsed)}
          className="rounded-full p-1.5 hover:bg-white/10 text-white/70 hover:text-white transition-colors"
          title={collapsed ? 'Expand Sidebar' : 'Collapse Sidebar'}
        >
          {collapsed ? <ChevronRight className="w-4 h-4" /> : <ChevronLeft className="w-4 h-4" />}
        </button>
      </div>

      {/* Navigation Items */}
      <nav className="flex-1 space-y-1.5 py-2 overflow-y-auto">
        {navItems.map((item) => {
          return (
            <NavLink
              key={item.href}
              to={item.href}
              className={({ isActive }) =>
                cn(
                  'flex items-center gap-3 px-3.5 py-3 rounded-xl text-sm font-semibold transition-all duration-200 group',
                  isActive
                    ? 'bg-brand-blue text-white shadow-lg font-bold scale-[1.02]'
                    : 'text-white/75 hover:bg-white/10 hover:text-white'
                )
              }
            >
              <div className="shrink-0">{iconMap[item.icon]}</div>
              {!collapsed && <span>{item.label}</span>}
            </NavLink>
          );
        })}
      </nav>

      {/* AI Engine Banner */}
      {!collapsed && (
        <div className="p-3.5 my-2 rounded-2xl bg-brand-blue/20 border border-brand-blue/30 text-xs space-y-2">
          <div className="flex items-center gap-1.5 font-bold text-white">
            <Sparkles className="w-4 h-4 text-brand-accent animate-pulse" />
            <span>HireAI Engine</span>
          </div>
          <p className="text-[11px] text-white/70">
            FastAPI Matching Active. 99.4% precision rank.
          </p>
        </div>
      )}

      {/* Logout */}
      <div className="pt-2 border-t border-white/10">
        <button
          onClick={logout}
          className={cn(
            'flex items-center gap-3 w-full px-3.5 py-3 rounded-xl text-sm font-semibold text-red-400 hover:bg-red-500/15 hover:text-red-300 transition-colors',
            collapsed && 'justify-center px-0'
          )}
        >
          <LogOut className="w-5 h-5 shrink-0" />
          {!collapsed && <span>Sign Out</span>}
        </button>
      </div>
    </aside>
  );
};
