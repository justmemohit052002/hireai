import React from 'react';
import { Link } from 'react-router-dom';
import { Button } from '@/components/ui/Button';
import { ROUTES } from '@/constants';

export const LandingCTA = () => {
  return (
    <section className="py-16 lg:py-24 max-w-5xl mx-auto px-5 text-center">
      <div>
        {/* Heading */}
        <h2 className="text-4xl md:text-4xl lg:text-5xl font-bold font-heading leading-tight text-foreground">
          Ready to{' '}
          <span className="gradient-text-brand">
            Simplify,
          </span>{' '}
          Your{' '}
          <span className="gradient-text-brand">
            Hiring?
          </span>
        </h2>

        {/* Description */}
        <p className="mt-6 text-muted-foreground text-base md:text-lg leading-relaxed max-w-3xl mx-auto font-sans">
          Join ambitious engineering teams and top developer candidates using HireAI to slash technical recruitment cycles by 70%.
        </p>

        {/* Buttons */}
        <div className="mt-10 flex flex-col sm:flex-row justify-center items-center gap-5">
          <Link to={ROUTES.SIGNUP} className="w-full sm:w-auto">
            <Button size="lg" variant="gradient" className="w-full sm:w-auto px-8 py-3.5 rounded-full bg-gradient-to-r from-[#C63FC5] via-[#F56681] to-[#FC9559] hover:opacity-95 text-white text-lg font-semibold shadow-xl shadow-[#C63FC5]/25 transition">
              Get Started
            </Button>
          </Link>

          <Link to={ROUTES.LOGIN} className="w-full sm:w-auto">
            <Button size="lg" className="w-full sm:w-auto px-8 py-3.5 rounded-full bg-[#6D3DF5] hover:bg-[#5b2fd4] text-white text-lg font-semibold shadow-lg transition">
              Book Demo
            </Button>
          </Link>
        </div>
      </div>
    </section>
  );
};
