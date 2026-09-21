import React from 'react';
import { Link } from 'react-router-dom';
import {
  ClipboardList,
  Search,
  UserCheck,
  Send,
  FileText,
  BarChart3,
} from 'lucide-react';
import { Button } from '@/components/ui/Button';
import { ROUTES } from '@/constants';

export const LandingFeatures = () => {
  const features = [
    { title: 'Smart Job Posting', icon: ClipboardList, bg: 'bg-[#C63FC5]' },
    { title: 'Advanced Search', icon: Search, bg: 'bg-brand-blue text-white' },
    { title: 'Assessment', icon: UserCheck, bg: 'bg-brand-blue-light' },
    { title: 'Application Tracking', icon: Send, bg: 'bg-[#FC9559]' },
    { title: 'Interview Management', icon: FileText, bg: 'bg-[#C63FC5]' },
    { title: 'Analytics & Report', icon: BarChart3, bg: 'bg-brand-blue text-white' },
  ];

  return (
    <section
      id="features"
      className="relative px-6 sm:px-10 bg-cover bg-center bg-no-repeat rounded-3xl max-w-7xl mx-auto my-12 overflow-hidden shadow-2xl"
      style={{
        backgroundImage: "url('/images/feature-bg.png')",
      }}
    >
      {/* Dark Overlay matching Landing_page_S PowerfulFeatures */}
      <div className="absolute inset-0 bg-black/75 backdrop-blur-xs pointer-events-none"></div>

      <div className="relative z-10 mx-auto px-2 sm:px-5 py-16 lg:py-24 max-w-6xl">
        <div className="grid lg:grid-cols-2 gap-10 lg:gap-12 items-center">
          {/* Left Content */}
          <div className="text-foreground">
            <h2 className="text-4xl md:text-5xl font-bold font-heading leading-tight">
              Powerful Features
            </h2>

            <p className="mt-6 text-base sm:text-lg leading-relaxed text-gray-300 max-w-xl font-sans">
              Streamline candidate discovery, rank applicants accurately with neural machine learning algorithms, and eliminate manual hiring bottlenecks.
            </p>

            <div className="mt-10">
              <Link to={ROUTES.SIGNUP}>
                <Button className="bg-brand-navy text-white hover:bg-brand-blue font-semibold px-10 py-4 rounded-full text-base transition shadow-xl">
                  For HR
                </Button>
              </Link>
            </div>
          </div>

          {/* Right Cards */}
          <div className="grid sm:grid-cols-2 gap-5">
            {features.map((item, idx) => {
              const IconComp = item.icon;
              return (
                <div
                  key={idx}
                  className="bg-[#2A2B48]/80 hover:bg-surface/20 backdrop-blur-md border border-white/15 rounded-3xl p-6 flex items-center gap-4 transition-all hover-lift"
                >
                  <div className={`w-12 h-12 rounded-full ${item.bg} flex items-center justify-center shrink-0 shadow-lg text-foreground`}>
                    <IconComp className="w-5 h-5 stroke-[2.2]" />
                  </div>

                  <h3 className="text-foreground text-base md:text-lg font-medium font-heading">
                    {item.title}
                  </h3>
                </div>
              );
            })}
          </div>
        </div>
      </div>
    </section>
  );
};
