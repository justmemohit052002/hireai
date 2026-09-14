import React from 'react';
import { Outlet } from 'react-router-dom';
import { TopNav } from './TopNav';

export const RecruiterLayout = () => {
  return (
    <div className="min-h-screen flex flex-col text-foreground">
      {/* Main Content Area */}
      <div className="w-full max-w-7xl mx-auto flex-1 flex flex-col min-w-0">
        <TopNav role="recruiter" title="Recruiter Studio" />
        <main className="pb-10 px-4 md:px-6 flex-1">
          <Outlet />
        </main>
      </div>
    </div>
  );
};

