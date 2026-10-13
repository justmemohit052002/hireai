import React from 'react';
import { Outlet, Link } from 'react-router-dom';
import { ArrowLeft } from 'lucide-react';
import { Logo } from '@/components/brand/Logo';
import { ThemeToggle } from '@/components/common/ThemeToggle';

export const AuthLayout = () => {
  return (
    <div className="h-screen max-h-screen w-full flex flex-col justify-between text-foreground bg-background relative overflow-y-auto sm:overflow-hidden selection:bg-brand-blue selection:text-white">
      {/* Ambient background lighting effects */}
      <div className="fixed inset-0 pointer-events-none overflow-hidden -z-10">
        <div className="absolute top-[-15%] left-[20%] w-[500px] h-[500px] rounded-full bg-brand-blue/10 dark:bg-brand-blue/15 blur-[120px]" />
        <div className="absolute bottom-[-10%] right-[15%] w-[450px] h-[450px] rounded-full bg-brand-navy/10 dark:bg-brand-navy/15 blur-[130px]" />
      </div>

      {/* Top Header Bar */}
      <header className="w-full max-w-6xl mx-auto px-4 sm:px-6 py-2.5 sm:py-3 flex items-center justify-between z-20 shrink-0">
        <Link to="/" className="flex items-center gap-2 hover:opacity-90 transition-opacity">
          <Logo variant="full" size="md" showPro />
        </Link>

        <div className="flex items-center gap-3">
          <Link
            to="/"
            className="inline-flex items-center gap-1.5 text-xs font-semibold px-3 py-1.5 rounded-lg border border-border bg-surface hover:bg-surface-2 text-muted-foreground hover:text-foreground transition-all"
          >
            <ArrowLeft className="w-3.5 h-3.5" />
            <span className="hidden sm:inline">Back to Home</span>
            <span className="sm:hidden">Home</span>
          </Link>
          <ThemeToggle />
        </div>
      </header>

      {/* Centered Auth Card Container - zero vertical page scroll */}
      <main className="flex-1 flex items-center justify-center p-3 sm:p-4 w-full max-w-md mx-auto z-10 overflow-y-auto sm:overflow-visible">
        <Outlet />
      </main>
    </div>
  );
};
