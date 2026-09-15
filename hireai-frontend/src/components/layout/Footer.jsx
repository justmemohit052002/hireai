import React from 'react';
import { Logo } from '@/components/common/Logo';
import { Mail, Phone } from 'lucide-react';

export const Footer = () => {
  return (
    <footer className="border-t border-border/40 bg-surface-2/40 backdrop-blur-md pt-16 pb-10 text-foreground">
      <div className="max-w-7xl mx-auto px-6 grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-10">
        {/* Brand & Contact Column */}
        <div className="space-y-4">
          <Logo size="md" />
          <p className="text-xs text-muted-foreground leading-relaxed">
            The all-in-one AI recruitment platform matching top engineering talent with ambitious tech teams.
          </p>
          <div className="space-y-2 pt-2 text-xs text-muted-foreground">
            <p className="flex items-center gap-2">
              <Mail className="w-3.5 h-3.5 text-[#6D3DF5]" /> info@gmail.com
            </p>
            <p className="flex items-center gap-2">
              <Phone className="w-3.5 h-3.5 text-[#6D3DF5]" /> +91 9564358965
            </p>
          </div>
        </div>

        {/* Quick Links Column */}
        <div>
          <h4 className="text-xs font-bold uppercase tracking-wider text-foreground mb-4 font-heading">
            Quick Links
          </h4>
          <ul className="space-y-2.5 text-xs text-muted-foreground font-sans">
            <li><a href="#" className="hover:text-foreground transition-colors">Home</a></li>
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
            <li><a href="#features" className="hover:text-foreground transition-colors">FastAPI Neural Parser</a></li>
            <li><a href="#features" className="hover:text-foreground transition-colors">Candidate Rank Leaderboards</a></li>
            <li><a href="#features" className="hover:text-foreground transition-colors">Smart Job Management</a></li>
            <li><a href="#features" className="hover:text-foreground transition-colors">Automated Screening</a></li>
          </ul>
        </div>

        {/* Social Column */}
        <div className="space-y-4">
          <h4 className="text-xs font-bold uppercase tracking-wider text-foreground mb-4 font-heading">
            Follow Us
          </h4>
          <div className="flex items-center gap-3">
            <a
              href="#"
              aria-label="LinkedIn"
              className="w-9 h-9 rounded-lg border border-border bg-surface flex items-center justify-center text-muted-foreground hover:text-foreground hover:border-[#6D3DF5] transition-all"
            >
              <svg className="w-4 h-4 fill-current" viewBox="0 0 24 24">
                <path d="M19 3a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h14m-.5 15.5v-5.3a3.26 3.26 0 0 0-3.26-3.26c-.85 0-1.84.52-2.28 1.3v-1.11h-2.79v8.37h2.79v-4.93c0-.77.62-1.4 1.39-1.4a1.4 1.4 0 0 1 1.4 1.4v4.93h2.75M6.88 8.56a1.68 1.68 0 0 0 1.68-1.68c0-.93-.75-1.69-1.68-1.69a1.69 1.69 0 0 0-1.69 1.69c0 .93.76 1.68 1.69 1.68m1.39 9.94v-8.37H5.5v8.37h2.77z"/>
              </svg>
            </a>
            <a
              href="#"
              aria-label="Instagram"
              className="w-9 h-9 rounded-lg border border-border bg-surface flex items-center justify-center text-muted-foreground hover:text-foreground hover:border-[#F56681] transition-all"
            >
              <svg className="w-4 h-4 fill-current" viewBox="0 0 24 24">
                <path d="M12 2.163c3.204 0 3.584.012 4.85.07 3.252.148 4.771 1.691 4.919 4.919.058 1.265.069 1.645.069 4.849 0 3.205-.012 3.584-.069 4.849-.149 3.225-1.664 4.771-4.919 4.919-1.266.058-1.644.07-4.85.07-3.204 0-3.584-.012-4.849-.07-3.26-.149-4.771-1.699-4.919-4.92-.058-1.265-.07-1.644-.07-4.849 0-3.204.013-3.583.07-4.849.149-3.227 1.664-4.771 4.919-4.919 1.266-.057 1.645-.069 4.849-.069zm0-2.163c-3.259 0-3.667.014-4.947.072-4.358.2-6.78 2.618-6.98 6.98-.059 1.281-.073 1.689-.073 4.948 0 3.259.014 3.668.072 4.948.2 4.358 2.618 6.78 6.98 6.98 1.281.058 1.689.072 4.948.072 3.259 0 3.668-.014 4.948-.072 4.354-.2 6.782-2.618 6.979-6.98.059-1.28.073-1.689.073-4.948 0-3.259-.014-3.667-.072-4.947-.196-4.354-2.617-6.78-6.979-6.98-1.281-.059-1.69-.073-4.949-.073zm0 5.838c-3.403 0-6.162 2.759-6.162 6.162s2.759 6.163 6.162 6.163 6.162-2.759 6.162-6.163c0-3.403-2.759-6.162-6.162-6.162zm0 10.162c-2.209 0-4-1.79-4-4 0-2.209 1.791-4 4-4s4 1.791 4 4c0 2.21-1.791 4-4 4zm6.406-11.845c-.796 0-1.441.645-1.441 1.44s.645 1.44 1.441 1.44c.795 0 1.439-.645 1.439-1.44s-.644-1.44-1.439-1.44z"/>
              </svg>
            </a>
            <a
              href="#"
              aria-label="Facebook"
              className="w-9 h-9 rounded-lg border border-border bg-surface flex items-center justify-center text-muted-foreground hover:text-foreground hover:border-[#FC9559] transition-all"
            >
              <svg className="w-4 h-4 fill-current" viewBox="0 0 24 24">
                <path d="M9 8H6v4h3v12h5V12h3.642L18 8h-4V6.333C14 5.374 14.5 5 15.5 5H18V0h-3.808C10.592 0 9 1.592 9 4.415V8z"/>
              </svg>
            </a>
          </div>
        </div>
      </div>

      {/* Bottom Legal Bar */}
      <div className="max-w-7xl mx-auto px-6 mt-12 pt-6 border-t border-border/40 flex flex-col sm:flex-row items-center justify-between text-xs text-muted-foreground gap-4">
        <p>© 2026 HireAI Inc. All rights reserved.</p>
        <div className="flex gap-4 text-xs">
          <a href="#" className="hover:text-foreground transition-colors">Privacy Policy</a>
          <span>•</span>
          <a href="#" className="hover:text-foreground transition-colors">Terms & Conditions</a>
        </div>
      </div>
    </footer>
  );
};
