import React, { useState, useEffect } from 'react';
import { NavLink, Link, Outlet, useLocation, useNavigate } from 'react-router-dom';
import {
  Activity,
  Cpu,
  FileCode,
  Users,
  Key,
  ShieldCheck,
  RefreshCw,
  LogOut,
  Menu,
  X,
  Server,
} from 'lucide-react';
import { ThemeToggle } from '@/components/common/ThemeToggle';
import { Logo } from '@/components/brand/Logo';
import { ADMIN_NAV_ITEMS, ROUTES } from '@/constants';
import { adminApi } from '@/services/api/admin.api';
import { useAuth } from '@/context/AuthContext';
import { cn } from '@/utils';

const iconMap = {
  'Overview & Health': <Activity className="w-4 h-4 shrink-0" />,
  'LLM Usage & Quota': <Cpu className="w-4 h-4 shrink-0" />,
  'Call Logs & Audit': <FileCode className="w-4 h-4 shrink-0" />,
  'Audit Trail': <ShieldCheck className="w-4 h-4 shrink-0" />,
  'User Roles & Access': <Users className="w-4 h-4 shrink-0" />,
  'Provider & Keys': <Key className="w-4 h-4 shrink-0" />,
};

export const AdminLayout = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const { user, logout } = useAuth();
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [engineStatus, setEngineStatus] = useState('CHECKING');
  const [activeProvider, setActiveProvider] = useState('ollama');
  const [lastChecked, setLastChecked] = useState(null);
  const [isRefreshing, setIsRefreshing] = useState(false);

  // Close drawer on route change
  useEffect(() => {
    setDrawerOpen(false);
  }, [location.pathname]);

  const handleLogout = async () => {
    try {
      await logout();
    } finally {
      navigate(ROUTES.LOGIN || '/login');
    }
  };

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
    const interval = setInterval(fetchQuickStatus, 45000);
    return () => clearInterval(interval);
  }, []);

  const getStatusBadge = (compact = false) => {
    if (engineStatus === 'HEALTHY' || engineStatus === 'UP') {
      return (
        <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg text-[11px] font-bold bg-emerald-500/15 text-emerald-600 dark:text-emerald-400 border border-emerald-500/20">
          <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse" />
          {compact ? activeProvider.toUpperCase() : `AI Engine Online (${activeProvider.toUpperCase()})`}
        </span>
      );
    }
    if (engineStatus === 'DEGRADED') {
      return (
        <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg text-[11px] font-bold bg-amber-500/15 text-amber-600 dark:text-amber-400 border border-amber-500/20">
          <span className="w-2 h-2 rounded-full bg-amber-500 animate-pulse" />
          {compact ? 'Degraded' : 'AI Engine Degraded'}
        </span>
      );
    }
    return (
      <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg text-[11px] font-bold bg-rose-500/15 text-rose-600 dark:text-rose-400 border border-rose-500/20">
        <span className="w-2 h-2 rounded-full bg-rose-500" />
        {compact ? 'Offline' : 'AI Engine Offline'}
      </span>
    );
  };

  const adminEmail = user?.email || 'admin@vionsys.com';
  const adminName =
    user?.name ||
    (user?.firstName ? `${user.firstName} ${user.lastName || ''}`.trim() : 'System Administrator');

  return (
    <div className="min-h-screen text-foreground flex bg-radial-gradient relative overflow-x-hidden">
      {/* Mobile Backdrop Overlay */}
      {drawerOpen && (
        <div
          onClick={() => setDrawerOpen(false)}
          className="fixed inset-0 bg-black/60 backdrop-blur-xs z-40 md:hidden transition-opacity duration-300"
          aria-hidden="true"
        />
      )}

      {/* Side Drawer / Sidebar */}
      <aside
        className={cn(
          'fixed inset-y-0 left-0 z-50 w-72 flex flex-col bg-card/95 dark:bg-slate-950/95 backdrop-blur-2xl border-r border-border/60 shadow-2xl transition-transform duration-300 ease-in-out',
          drawerOpen ? 'translate-x-0' : '-translate-x-full md:translate-x-0'
        )}
      >
        {/* Sidebar Header: Brand & Ops Badge */}
        <div className="p-4 sm:p-5 border-b border-border/50 flex items-center justify-between gap-3">
          <Link
            to={ROUTES.ADMIN_DASHBOARD}
            className="flex items-center gap-2 outline-none focus-visible:ring-2 focus-visible:ring-brand-blue rounded-lg"
          >
            <Logo variant="full" size="md" showPro />
          </Link>
          <div className="flex items-center gap-1.5">
            <span className="px-2 py-0.5 rounded-md bg-brand-blue/10 border border-brand-blue/20 text-brand-blue text-[10px] font-extrabold uppercase tracking-wider">
              Ops Console
            </span>
            <button
              type="button"
              onClick={() => setDrawerOpen(false)}
              className="md:hidden p-1.5 rounded-lg text-muted-foreground hover:text-foreground hover:bg-surface-2/60 transition-colors cursor-pointer"
              aria-label="Close Drawer"
            >
              <X className="w-5 h-5" />
            </button>
          </div>
        </div>

        {/* Live System Telemetry Card */}
        <div className="px-4 py-3 mx-3 my-3 rounded-xl bg-surface-2/40 border border-border/40 space-y-2">
          <div className="flex items-center justify-between">
            <span className="text-[10px] font-bold uppercase tracking-wider text-muted-foreground flex items-center gap-1">
              <Server className="w-3 h-3 text-brand-blue" />
              Service Status
            </span>
            <button
              type="button"
              onClick={fetchQuickStatus}
              title="Refresh Engine Health"
              className="p-1 rounded-md text-muted-foreground hover:text-foreground hover:bg-surface-2 transition-colors cursor-pointer"
            >
              <RefreshCw className={cn('w-3.5 h-3.5', isRefreshing && 'animate-spin text-brand-blue')} />
            </button>
          </div>
          <div>{getStatusBadge()}</div>
        </div>

        {/* Navigation Items */}
        <div className="px-3 py-1 flex-1 overflow-y-auto space-y-1">
          <div className="px-3 pb-1 text-[10px] font-extrabold uppercase tracking-wider text-muted-foreground">
            Management & Controls
          </div>
          <nav className="space-y-1">
            {ADMIN_NAV_ITEMS.map((item) => (
              <NavLink
                key={item.href}
                to={item.href}
                className={({ isActive }) =>
                  cn(
                    'flex items-center gap-3 px-3.5 py-2.5 rounded-xl text-xs font-semibold transition-all duration-150 group',
                    isActive
                      ? 'bg-brand-blue text-white shadow-md font-bold scale-[1.01]'
                      : 'text-muted-foreground hover:text-foreground hover:bg-surface-2/70'
                  )
                }
              >
                <span>{iconMap[item.label] || <Activity className="w-4 h-4 shrink-0" />}</span>
                <span className="truncate">{item.label}</span>
              </NavLink>
            ))}
          </nav>
        </div>

        {/* Sidebar Footer: Theme, Admin Profile, Logout */}
        <div className="p-3.5 border-t border-border/50 bg-surface-2/20 space-y-3">
          {/* Theme & Quick Settings */}
          <div className="flex items-center justify-between px-2">
            <span className="text-[11px] font-semibold text-muted-foreground">Appearance</span>
            <ThemeToggle />
          </div>

          {/* Admin User Identifier */}
          <div className="flex items-center gap-2.5 px-2.5 py-2 rounded-xl bg-card border border-border/40">
            <div className="w-8 h-8 rounded-lg bg-brand-blue/15 border border-brand-blue/30 text-brand-blue font-bold text-xs flex items-center justify-center shrink-0">
              AD
            </div>
            <div className="flex-1 min-w-0">
              <p className="text-xs font-bold text-foreground truncate">{adminName}</p>
              <p className="text-[10px] text-muted-foreground truncate">{adminEmail}</p>
            </div>
          </div>

          {/* Prominent Log Out Button */}
          <button
            type="button"
            onClick={handleLogout}
            title="Log out of Admin Ops Console"
            className="w-full flex items-center justify-center gap-2 px-3.5 py-2.5 rounded-xl text-xs font-semibold text-rose-600 dark:text-rose-400 bg-rose-500/10 hover:bg-rose-500/15 border border-rose-500/20 transition-all cursor-pointer shadow-xs"
          >
            <LogOut className="w-4 h-4 shrink-0" />
            <span>Log Out</span>
          </button>
        </div>
      </aside>

      {/* Main Content Area */}
      <div className="flex-1 min-w-0 md:pl-72 flex flex-col min-h-screen">
        {/* Mobile Top Header (Visible only on small viewports) */}
        <header className="md:hidden sticky top-0 z-30 flex items-center justify-between px-3 sm:px-4 h-14 bg-card/90 dark:bg-slate-950/90 backdrop-blur-md border-b border-border/60">
          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={() => setDrawerOpen(true)}
              className="p-1.5 sm:p-2 rounded-lg text-foreground hover:bg-surface-2/70 transition-colors cursor-pointer"
              aria-label="Open Admin Menu Drawer"
            >
              <Menu className="w-5 h-5" />
            </button>
            <Link to={ROUTES.ADMIN_DASHBOARD} className="flex items-center gap-1.5">
              <Logo variant="full" size="sm" showPro />
            </Link>
          </div>

          <div className="flex items-center gap-1.5 sm:gap-2">
            {getStatusBadge(true)}
            <ThemeToggle />
          </div>
        </header>

        {/* Dynamic Route Content */}
        <main className="flex-1 p-3 sm:p-6 lg:p-8 animate-in fade-in duration-200">
          <Outlet />
        </main>
      </div>
    </div>
  );
};
