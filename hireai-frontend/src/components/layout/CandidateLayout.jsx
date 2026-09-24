import React from 'react';
import { Outlet } from 'react-router-dom';
import { TopNav } from './TopNav';

export const CandidateLayout = () => {
  return (
    <div className="min-h-screen text-foreground flex flex-col p-4 md:p-6">
      {/* Main Content Area */}
      <div className="w-full max-w-7xl mx-auto flex-1 flex flex-col min-w-0">
        <TopNav role="candidate" />
        <main className="flex-1 flex flex-col min-w-0">
          <Outlet />
        </main>
      </div>
    </div>
  );
};
