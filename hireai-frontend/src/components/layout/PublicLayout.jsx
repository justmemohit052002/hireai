import React from 'react';
import { Outlet } from 'react-router-dom';
import { FloatingNavbar } from './FloatingNavbar';
import { Footer } from './Footer';

export const PublicLayout = () => {
  return (
    <div className="min-h-screen flex flex-col aurora-bg text-foreground selection:bg-blue-500/20 selection:text-blue-500">
      <FloatingNavbar />
      <main className="flex-1 pt-24">
        <Outlet />
      </main>
      <Footer />
    </div>
  );
};
