import React from 'react';
import { Outlet, Link } from 'react-router-dom';
import { ArrowLeft } from 'lucide-react';
import { Logo } from '@/components/common/Logo';
import { ThemeToggle } from '@/components/common/ThemeToggle';

export const AuthLayout = () => {
  return (
    <div className="min-h-screen flex flex-col justify-between text-foreground bg-background relative overflow-x-hidden selection:bg-brand-blue selection:text-white">
      {/* Ambient background lighting effects */}
      <div className="fixed inset-0 pointer-events-none overflow-hidden -z-10">
        <div className="absolute top-[-15%] left-[20%] w-[500px] h-[500px] rounded-full bg-brand-blue/10 dark:bg-brand-blue/15 blur-[120px]" />
        <div className="absolute bottom-[-10%] right-[15%] w-[450px] h-[450px] rounded-full bg-[#C63FC5]/10 dark:bg-[#C63FC5]/15 blur-[130px]" />
      </div>

      {/* Top Header Bar */}
      <header className="w-full max-w-6xl mx-auto px-4 sm:px-6 py-4 flex items-center justify-between z-20">
        <Link to="/" className="flex items-center gap-2 hover:opacity-90 transition-opacity">
          <Logo size="md" />
        </Link>

        <div className="flex items-center gap-3">
          <Link
            to="/"
            className="inline-flex items-center gap-1.5 text-xs font-semibold px-3.5 py-1.5 rounded-full glass border border-border hover:bg-surface-2/60 text-muted-foreground hover:text-foreground transition-all"
          >
            <ArrowLeft className="w-3.5 h-3.5" />
            <span className="hidden sm:inline">Back to Home</span>
            <span className="sm:hidden">Home</span>
          </Link>
          <ThemeToggle />
        </div>
      </header>

      {/* Centered Auth Card Container */}
      <main className="flex-1 flex items-center justify-center p-4 sm:p-6 w-full max-w-md mx-auto z-10">
        <Outlet />
      </main>

      {/* Subtle Footer Note */}
      <footer className="w-full text-center py-4 text-xs text-muted-foreground z-10">
        <p>© 2026 HireAI Inc. Secure Authentication</p>
      </footer>
    </div>
  );
};
