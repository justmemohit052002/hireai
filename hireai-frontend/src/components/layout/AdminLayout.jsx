import React, { useState, useEffect } from 'react';
import { NavLink, Link, Outlet, useLocation } from 'react-router-dom';
import {
  Activity,
  Cpu,
  FileCode,
  Users,
  Key,
  ShieldCheck,
  RefreshCw,
  LogOut,
  Sparkles,
  Menu,
  X,
  Server,
} from 'lucide-react';
import { ThemeToggle } from '@/components/common/ThemeToggle';
import { ProfileMenu } from '@/components/common/ProfileMenu';
import { Logo } from '@/components/common/Logo';
import { Badge } from '@/components/ui/Badge';
import { Button } from '@/components/ui/Button';
import { ADMIN_NAV_ITEMS, ROUTES } from '@/constants';
import { adminApi } from '@/services/api/admin.api';
import { cn } from '@/utils';

const iconMap = {
  'Overview & Health': <Activity className="w-4 h-4" />,
  'LLM Usage & Quota': <Cpu className="w-4 h-4" />,
  'Call Logs & Audit': <FileCode className="w-4 h-4" />,
  'User Roles & Access': <Users className="w-4 h-4" />,
  'Provider & Keys': <Key className="w-4 h-4" />,
};

export const AdminLayout = () => {
  const location = useLocation();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const [engineStatus, setEngineStatus] = useState('CHECKING');
  const [activeProvider, setActiveProvider] = useState('ollama');
  const [lastChecked, setLastChecked] = useState(null);
  const [isRefreshing, setIsRefreshing] = useState(false);

  const fetchQuickStatus = async () => {
    try {
      setIsRefreshing(true);
      const data = await adminApi.getHealth();
      setEngineStatus(data?.overallStatus || 'HEALTHY');
      setActiveProvider(data?.providers?.activeProvider || 'ollama');
      setLastChecked(new Date());
    } catch (err) {
      setEngineStatus('DEGRADED');
    } finally {
      setIsRefreshing(false);
    }
  };

  useEffect(() => {
    fetchQuickStatus();
    // Poll status every 45s
    const interval = setInterval(fetchQuickStatus, 45000);
    return () => clearInterval(interval);
  }, []);

  const getStatusBadge = () => {
    if (engineStatus === 'HEALTHY' || engineStatus === 'UP') {
      return (
        <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-[11px] font-bold bg-emerald-500/15 text-emerald-600 dark:text-emerald-400 border border-emerald-500/20">
          <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse" />
          AI Engine Online ({activeProvider.toUpperCase()})
        </span>
      );
    }
    if (engineStatus === 'DEGRADED') {
      return (
        <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-[11px] font-bold bg-amber-500/15 text-amber-600 dark:text-amber-400 border border-amber-500/20">
          <span className="w-2 h-2 rounded-full bg-amber-500 animate-pulse" />
          AI Engine Degraded
        </span>
      );
    }
    return (
      <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-[11px] font-bold bg-rose-500/15 text-rose-600 dark:text-rose-400 border border-rose-500/20">
        <span className="w-2 h-2 rounded-full bg-rose-500" />
        AI Engine Offline
      </span>
    );
  };

  return (
    <div className="min-h-screen text-foreground flex flex-col p-3 sm:p-5 md:p-6 bg-radial-gradient">
      <div className="w-full max-w-7xl mx-auto flex-1 flex flex-col min-w-0">
        {/* Sticky Admin Floating Header */}
        <header className="sticky top-2 sm:top-4 z-40 flex flex-col items-center justify-center w-full mb-4 sm:mb-6 pointer-events-none">
          <nav className="pointer-events-auto flex items-center justify-between w-full h-15 sm:h-17 px-4 sm:px-6 rounded-2xl sm:rounded-full glass border border-white/20 dark:border-white/10 shadow-2xl backdrop-blur-xl">
            {/* Logo & Admin Ops Label */}
            <div className="flex items-center gap-3 shrink-0">
              <Link to={ROUTES.ADMIN_DASHBOARD} className="flex items-center gap-2">
                <Logo size="md" />
              </Link>
              <div className="hidden lg:flex items-center gap-1.5 px-2 py-0.5 rounded-md bg-indigo-500/10 border border-indigo-500/20 text-indigo-600 dark:text-indigo-400 text-[10px] font-extrabold uppercase tracking-wider">
                <ShieldCheck className="w-3.5 h-3.5" /> Ops Console
              </div>
            </div>

            {/* Center Navigation Links (Desktop) */}
            <div className="hidden md:flex items-center gap-1.5 lg:gap-2 text-xs font-semibold">
              {ADMIN_NAV_ITEMS.map((item) => (
                <NavLink
                  key={item.href}
                  to={item.href}
                  className={({ isActive }) =>
                    cn(
                      'relative px-3.5 py-2 rounded-full text-xs font-semibold transition-all duration-200 whitespace-nowrap flex items-center gap-2',
                      isActive
                        ? 'bg-slate-900 text-white dark:bg-white dark:text-slate-950 shadow-md font-bold scale-[1.02]'
                        : 'text-muted-foreground hover:text-foreground hover:bg-surface-2/60'
                    )
                  }
                >
                  <span className="shrink-0">{iconMap[item.label]}</span>
                  <span>{item.label}</span>
                </NavLink>
              ))}
            </div>

            {/* Right Actions & Status */}
            <div className="flex items-center gap-2 sm:gap-3 shrink-0">
              <div className="hidden xl:block">{getStatusBadge()}</div>
              
              <button
                type="button"
                onClick={fetchQuickStatus}
                title="Refresh System Health"
                className="p-2 rounded-full text-muted-foreground hover:text-foreground hover:bg-surface-2/60 transition-colors cursor-pointer"
              >
                <RefreshCw className={cn('w-4 h-4', isRefreshing && 'animate-spin text-brand-blue')} />
              </button>

              <ThemeToggle />
              <div className="h-5 w-px bg-border/60 mx-0.5" />
              <ProfileMenu />

              {/* Mobile Hamburger Button */}
              <button
                type="button"
                onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
                className="md:hidden p-2 rounded-xl text-muted-foreground hover:text-foreground hover:bg-surface-2/60 transition-colors ml-1 cursor-pointer"
                aria-label="Toggle Admin Navigation"
              >
                {mobileMenuOpen ? <X className="w-5 h-5 text-foreground" /> : <Menu className="w-5 h-5" />}
              </button>
            </div>
          </nav>

          {/* Mobile Drawer Menu */}
          {mobileMenuOpen && (
            <div className="pointer-events-auto md:hidden w-full mt-2 p-3.5 rounded-2xl glass border border-white/20 dark:border-white/10 shadow-2xl backdrop-blur-2xl animate-in fade-in slide-in-from-top-2 duration-200">
              <div className="flex items-center justify-between mb-3 pb-2 border-b border-border/50">
                <div className="text-xs font-bold text-muted-foreground">Admin Controls</div>
                <div>{getStatusBadge()}</div>
              </div>
              <div className="grid grid-cols-1 gap-1.5">
                {ADMIN_NAV_ITEMS.map((item) => (
                  <NavLink
                    key={item.href}
                    to={item.href}
                    onClick={() => setMobileMenuOpen(false)}
                    className={({ isActive }) =>
                      cn(
                        'flex items-center gap-2.5 px-3.5 py-2.5 rounded-xl text-xs font-semibold transition-all',
                        isActive
                          ? 'bg-slate-900 text-white dark:bg-white dark:text-slate-950 font-bold shadow-md'
                          : 'text-muted-foreground hover:text-foreground hover:bg-surface-2/60'
                      )
                    }
                  >
                    <span className="shrink-0">{iconMap[item.label]}</span>
                    <span>{item.label}</span>
                  </NavLink>
                ))}
              </div>
            </div>
          )}
        </header>

        {/* Main Content Area */}
        <main className="flex-1 flex flex-col min-w-0 animate-in fade-in duration-300">
          <Outlet />
        </main>
      </div>
    </div>
  );
};
