import React from 'react';
import { Outlet } from 'react-router-dom';
import { FloatingNavbar } from './FloatingNavbar';
import { Footer } from './Footer';

export const PublicLayout = () => {
  return (
    <div className="min-h-screen flex flex-col text-foreground">
      <FloatingNavbar />
      <main className="flex-1 pt-24">
        <Outlet />
      </main>
      <Footer />
    </div>
  );
};
