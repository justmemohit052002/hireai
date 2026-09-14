import React from 'react';

export const LandingStats = () => {
  return (
    <section className="py-10">
      <div className="max-w-5xl mx-auto px-5">
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
          {/* Card 1 */}
          <div className="border p-2 border-[#6D3DF5]/40 dark:border-[#6D3DF5]/60 rounded-3xl h-44 flex flex-col justify-center items-center shadow-sm hover:shadow-lg transition surface-nested interactive-card">
            <h2 className="text-4xl font-bold font-heading gradient-text-brand">2K+</h2>
            <p className="text-lg mt-3 font-medium text-foreground">
              Companies
            </p>
          </div>

          {/* Card 2 */}
          <div className="border p-2 border-[#6D3DF5]/40 dark:border-[#6D3DF5]/60 rounded-3xl h-44 flex flex-col justify-center items-center shadow-sm hover:shadow-lg transition surface-nested interactive-card">
            <h2 className="text-4xl font-bold font-heading gradient-text-brand">100K+</h2>
            <p className="text-lg mt-3 font-medium text-foreground">
              HR Users
            </p>
          </div>

          {/* Card 3 */}
          <div className="border p-2 border-[#6D3DF5]/40 dark:border-[#6D3DF5]/60 rounded-3xl h-44 flex flex-col justify-center items-center shadow-sm hover:shadow-lg transition surface-nested interactive-card">
            <h2 className="text-4xl font-bold font-heading gradient-text-brand">2M+</h2>
            <p className="text-lg mt-3 font-medium text-foreground">
              Candidates
            </p>
          </div>

          {/* Card 4 */}
          <div className="border p-2 border-[#6D3DF5]/40 dark:border-[#6D3DF5]/60 rounded-3xl h-44 flex flex-col justify-center items-center shadow-sm hover:shadow-lg transition surface-nested interactive-card">
            <h2 className="text-4xl font-bold font-heading gradient-text-brand">2K+</h2>
            <p className="text-lg mt-3 font-medium text-foreground">
              Job Posted
            </p>
          </div>
        </div>
      </div>
    </section>
  );
};
