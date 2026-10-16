import React from 'react';
import { Link } from 'react-router-dom';
import { Logo } from '@/components/brand/Logo';
import { Mail, Phone } from 'lucide-react';
import { ROUTES } from '@/constants';

export const Footer = () => {
  return (
    <footer className="border-t border-border/40 bg-surface-2/40 backdrop-blur-md pt-12 sm:pt-16 pb-8 sm:pb-10 text-foreground">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-8 sm:gap-10">
        {/* Brand & Contact Column */}
        <div className="space-y-4">
          <Logo variant="full" size="sm" showPro={false} />
          <p className="text-xs text-muted-foreground leading-relaxed">
            Technical recruitment infrastructure matching software engineers with hiring teams through deterministic ATS matching.
          </p>
          <div className="space-y-2 pt-2 text-xs text-muted-foreground">
            <p className="flex items-center gap-2">
              <Mail className="w-3.5 h-3.5 text-brand-blue" /> contact@hireai.app
            </p>
            <p className="flex items-center gap-2">
              <Phone className="w-3.5 h-3.5 text-brand-blue" /> +91 9564358965
            </p>
          </div>
        </div>

        {/* Quick Links Column */}
        <div>
          <h4 className="text-xs font-bold uppercase tracking-wider text-foreground mb-4 font-heading">
            Quick Links
          </h4>
          <ul className="space-y-2.5 text-xs text-muted-foreground font-sans">
            <li><Link to={ROUTES.HOME} className="hover:text-foreground transition-colors">Home</Link></li>
            <li><a href="#hiring" className="hover:text-foreground transition-colors">For HR</a></li>
            <li><a href="#hiring" className="hover:text-foreground transition-colors">For Candidates</a></li>
            <li><a href="#features" className="hover:text-foreground transition-colors">Features</a></li>
            <li><a href="#how-it-works" className="hover:text-foreground transition-colors">How It Works</a></li>
          </ul>
        </div>

        {/* Product & Capabilities Column */}
        <div>
          <h4 className="text-xs font-bold uppercase tracking-wider text-foreground mb-4 font-heading">
            Platform Capabilities
          </h4>
          <ul className="space-y-2.5 text-xs text-muted-foreground font-sans">
            <li><a href="#features" className="hover:text-foreground transition-colors">FastAPI Resume Parser</a></li>
            <li><a href="#features" className="hover:text-foreground transition-colors">Candidate Rank Leaderboards</a></li>
            <li><a href="#features" className="hover:text-foreground transition-colors">Smart Job Management</a></li>
            <li><a href="#features" className="hover:text-foreground transition-colors">Automated Screening</a></li>
          </ul>
        </div>

        {/* Social Column */}
        <div className="space-y-4">
          <h4 className="text-xs font-bold uppercase tracking-wider text-foreground mb-4 font-heading">
            Connect
          </h4>
          <div className="flex items-center gap-3">
            <a
              href="https://linkedin.com"
              target="_blank"
              rel="noreferrer"
              aria-label="LinkedIn"
              className="w-9 h-9 rounded-lg border border-border bg-surface flex items-center justify-center text-muted-foreground hover:text-foreground hover:border-brand-blue transition-all"
            >
              <svg className="w-4 h-4 fill-current" viewBox="0 0 24 24">
                <path d="M19 3a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h14m-.5 15.5v-5.3a3.26 3.26 0 0 0-3.26-3.26c-.85 0-1.84.52-2.28 1.3v-1.11h-2.79v8.37h2.79v-4.93c0-.77.62-1.4 1.39-1.4a1.4 1.4 0 0 1 1.4 1.4v4.93h2.75M6.88 8.56a1.68 1.68 0 0 0 1.68-1.68c0-.93-.75-1.69-1.68-1.69a1.69 1.69 0 0 0-1.69 1.69c0 .93.76 1.68 1.69 1.68m1.39 9.94v-8.37H5.5v8.37h2.77z"/>
              </svg>
            </a>
            <a
              href="https://github.com"
              target="_blank"
              rel="noreferrer"
              aria-label="GitHub"
              className="w-9 h-9 rounded-lg border border-border bg-surface flex items-center justify-center text-muted-foreground hover:text-foreground hover:border-brand-blue transition-all"
            >
              <svg className="w-4 h-4 fill-current" viewBox="0 0 24 24">
                <path d="M12 0C5.37 0 0 5.37 0 12c0 5.31 3.435 9.795 8.205 11.385.6.105.825-.255.825-.57 0-.285-.015-1.23-.015-2.235-3.015.555-3.795-.735-4.035-1.41-.135-.345-.72-1.41-1.23-1.695-.42-.225-1.02-.78-.015-.795.945-.015 1.62.87 1.845 1.23 1.08 1.815 2.805 1.305 3.495.99.105-.78.42-1.305.765-1.605-2.67-.3-5.46-1.335-5.46-5.925 0-1.305.465-2.385 1.23-3.225-.12-.3-.54-1.53.12-3.18 0 0 1.005-.315 3.3 1.23.96-.27 1.98-.405 3-.405s2.04.135 3 .405c2.295-1.56 3.3-1.23 3.3-1.23.66 1.65.24 2.88.12 3.18.765.84 1.23 1.905 1.23 3.225 0 4.605-2.805 5.625-5.475 5.925.435.375.81 1.095.81 2.22 0 1.605-.015 2.895-.015 3.3 0 .315.225.69.825.57A12.02 12.02 0 0 0 24 12c0-6.63-5.37-12-12-12z"/>
              </svg>
            </a>
          </div>
        </div>
      </div>

      {/* Bottom Legal Bar */}
      <div className="max-w-7xl mx-auto px-4 sm:px-6 mt-10 sm:mt-12 pt-6 border-t border-border/40 flex flex-col sm:flex-row items-center justify-between text-xs text-muted-foreground gap-4">
        <p>© 2026 HireAI Inc. All rights reserved.</p>
        <div className="flex gap-4 text-xs">
          <Link to={ROUTES.PRIVACY} className="hover:text-foreground transition-colors">Privacy Policy</Link>
          <span>•</span>
          <Link to={ROUTES.TERMS} className="hover:text-foreground transition-colors">Terms & Conditions</Link>
        </div>
      </div>
    </footer>
  );
};
