import React, { useEffect } from 'react';
import { Link } from 'react-router-dom';
import { ROUTES } from '@/constants';
// CHANGE IMAGE PATH HERE (or swap out with any student visual asset)
import heroStudentImg from '@/assets/images/girl.png';
import './LandingHero.css';

/**
 * Production-Ready Hero Section Component
 * Fully responsive, keyboard accessible, viewport-budgeted, hardware-accelerated animations
 */
export const LandingHero = () => {
  // High-Performance Vanilla JavaScript Mouse Parallax clamped to 12px on fine pointer devices
  useEffect(() => {
    const heroEl = document.getElementById('hero');
    const visualWrapper = document.getElementById('hero-visual-wrapper');
    if (!heroEl || !visualWrapper) return;

    // Check accessibility & device pointer type
    const prefersReducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)');
    const isFinePointer = window.matchMedia('(pointer: fine)');

    // Bail out immediately on touch devices or if user prefers reduced motion
    if (prefersReducedMotion.matches || !isFinePointer.matches) {
      return;
    }

    const logoCards = visualWrapper.querySelectorAll('.hero-logo-card[data-parallax]');
    if (!logoCards || logoCards.length === 0) return;

    const items = [];
    for (let i = 0; i < logoCards.length; i++) {
      const card = logoCards[i];
      const depth = parseFloat(card.getAttribute('data-parallax')) || 1.0;
      items.push({
        el: card,
        depth: depth,
        currentX: 0,
        currentY: 0,
        targetX: 0,
        targetY: 0,
        lastX: 0,
        lastY: 0
      });
    }

    let isHovering = false;
    let rafId = null;
    let cachedRect = null;
    const MAX_OFFSET = 12;     // Clamped movement limit in px
    const LERP_FACTOR = 0.075; // Silky-smooth spring dampening

    const updateRect = () => {
      cachedRect = heroEl.getBoundingClientRect();
    };

    const updateParallax = () => {
      let hasSignificantDelta = false;

      for (let j = 0; j < items.length; j++) {
        const item = items[j];

        item.currentX += (item.targetX - item.currentX) * LERP_FACTOR;
        item.currentY += (item.targetY - item.currentY) * LERP_FACTOR;

        const dx = item.targetX - item.currentX;
        const dy = item.targetY - item.currentY;
        if (Math.abs(dx) > 0.02 || Math.abs(dy) > 0.02) {
          hasSignificantDelta = true;
        }

        // Only commit style mutation if value delta exceeds subpixel threshold
        if (Math.abs(item.currentX - item.lastX) > 0.01 || Math.abs(item.currentY - item.lastY) > 0.01) {
          item.lastX = item.currentX;
          item.lastY = item.currentY;
          item.el.style.setProperty('--hero-px', `${item.currentX.toFixed(2)}px`);
          item.el.style.setProperty('--hero-py', `${item.currentY.toFixed(2)}px`);
        }
      }

      if (isHovering || hasSignificantDelta) {
        rafId = requestAnimationFrame(updateParallax);
      } else {
        // Fully settled - clean snap to 0 to avoid idle work
        for (let k = 0; k < items.length; k++) {
          const item = items[k];
          item.currentX = 0;
          item.currentY = 0;
          item.lastX = 0;
          item.lastY = 0;
          item.el.style.setProperty('--hero-px', '0px');
          item.el.style.setProperty('--hero-py', '0px');
        }
        rafId = null;
      }
    };

    const onMouseEnter = () => {
      updateRect();
    };

    const onMouseMove = (e) => {
      if (!cachedRect) updateRect();
      let relX = (e.clientX - cachedRect.left - cachedRect.width / 2) / (cachedRect.width / 2);
      let relY = (e.clientY - cachedRect.top - cachedRect.height / 2) / (cachedRect.height / 2);

      relX = Math.max(-1, Math.min(1, relX));
      relY = Math.max(-1, Math.min(1, relY));

      for (let k = 0; k < items.length; k++) {
        const item = items[k];
        const targetX = relX * MAX_OFFSET * item.depth;
        const targetY = relY * MAX_OFFSET * item.depth;

        item.targetX = Math.max(-MAX_OFFSET, Math.min(MAX_OFFSET, targetX));
        item.targetY = Math.max(-MAX_OFFSET, Math.min(MAX_OFFSET, targetY));
      }

      if (!isHovering) {
        isHovering = true;
        if (!rafId) {
          rafId = requestAnimationFrame(updateParallax);
        }
      }
    };

    const onMouseLeave = () => {
      isHovering = false;
      for (let m = 0; m < items.length; m++) {
        items[m].targetX = 0;
        items[m].targetY = 0;
      }
      if (!rafId) {
        rafId = requestAnimationFrame(updateParallax);
      }
    };

    heroEl.addEventListener('mouseenter', onMouseEnter, { passive: true });
    heroEl.addEventListener('mousemove', onMouseMove, { passive: true });
    heroEl.addEventListener('mouseleave', onMouseLeave, { passive: true });
    window.addEventListener('resize', updateRect, { passive: true });
    window.addEventListener('scroll', updateRect, { passive: true });

    return () => {
      if (rafId) cancelAnimationFrame(rafId);
      heroEl.removeEventListener('mouseenter', onMouseEnter);
      heroEl.removeEventListener('mousemove', onMouseMove);
      heroEl.removeEventListener('mouseleave', onMouseLeave);
      window.removeEventListener('resize', updateRect);
      window.removeEventListener('scroll', updateRect);
      for (let n = 0; n < items.length; n++) {
        items[n].el.style.removeProperty('--hero-px');
        items[n].el.style.removeProperty('--hero-py');
      }
    };
  }, []);

  return (
    <section className="hero-section" id="hero" aria-label="Hero Introduction">
      <div className="hero-container">
        {/* ─────────────────────────────────────────────────────────────
             ROOT CAUSE 5: LEFT COLUMN: Headings, 3-Line Title, CTAs & Proof
        ───────────────────────────────────────────────────────────── */}
        <div className="hero-content">
          {/* Eyebrow Tag with yellow accent dot matching navbar */}
          <div className="hero-eyebrow">
            <span className="hero-eyebrow-dot" aria-hidden="true" />
            {/* EDIT EYEBROW TEXT HERE */}
            <span>Next-Gen Technical Career Platform</span>
          </div>

          {/* Main Headline (Strictly 3 Lines as requested) */}
          {/* EDIT HEADLINE TEXT HERE */}
          <h1 className="hero-title">
            Launch Your Dream
            <br />
            Career With
            <br />
            <span className="hero-title-highlight">
              Proven Tech Skills
              <span className="hero-title-underline" aria-hidden="true" />
            </span>
          </h1>

          {/* Supporting Text */}
          {/* EDIT SUPPORTING TEXT HERE */}
          <p className="hero-subtitle">
            Connect directly with top tier tech companies. Showcase verified projects, bypass
            traditional recruiter filters, and land high-growth software engineering roles faster.
          </p>

          {/* Action Buttons: Red Primary & Navy Outline Secondary */}
          <div className="hero-actions">
            {/* Primary CTA: Change link & label as needed */}
            <Link to={ROUTES.SIGNUP} className="hero-btn hero-btn-primary" id="hero-btn-primary">
              <span>Get Started</span>
              <svg
                className="hero-btn-icon"
                width="18"
                height="18"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2.2"
                strokeLinecap="round"
                strokeLinejoin="round"
                aria-hidden="true"
              >
                <line x1="5" y1="12" x2="19" y2="12" />
                <polyline points="12 5 19 12 12 19" />
              </svg>
            </Link>

            {/* Secondary CTA: White with Navy Border */}
            <Link
              to={ROUTES.CANDIDATE_JOBS}
              className="hero-btn hero-btn-secondary"
              id="hero-btn-secondary"
            >
              <svg
                className="hero-btn-icon"
                width="18"
                height="18"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2.2"
                strokeLinecap="round"
                strokeLinejoin="round"
                aria-hidden="true"
              >
                <rect x="2" y="7" width="20" height="14" rx="2" ry="2" />
                <path d="M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16" />
              </svg>
              <span>Explore Companies</span>
            </Link>
          </div>

          {/* Genuine Capability Trust Line: auto-hidden on short screens (<700px) */}
          <div className="hero-trust">
            <div className="hero-trust-avatars" aria-hidden="true">
              <div className="hero-avatar hero-avatar-1">ATS</div>
              <div className="hero-avatar hero-avatar-2">94%</div>
              <div className="hero-avatar hero-avatar-3">RBAC</div>
            </div>
            <div className="hero-trust-text">
              <div className="flex items-center gap-1.5 text-xs font-semibold text-emerald-600 dark:text-emerald-400">
                <svg className="w-3.5 h-3.5 shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                  <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
                  <path d="m9 12 2 2 4-4" />
                </svg>
                <span>Automated Technical Screening</span>
              </div>
              <p className="hero-trust-label">
                <strong>Deterministic parsing</strong> &bull; Objective skill matrix &amp; verified pipeline
              </p>
            </div>
          </div>
        </div>

        {/* ─────────────────────────────────────────────────────────────
             ROOT CAUSE 2 & 3: RIGHT COLUMN: Grounded Visual & Symmetric Ellipse Arc
        ───────────────────────────────────────────────────────────── */}
        <div className="hero-visual-column">
          <div className="hero-visual-wrapper" id="hero-visual-wrapper">
            {/* Single thin dashed elliptical SVG arc: cx=50%, cy=34%, rx=40%, ry=26% */}
            <svg
              className="hero-ellipse-arc"
              viewBox="0 0 100 100"
              preserveAspectRatio="none"
              aria-hidden="true"
            >
              <path d="M 10 34 A 40 26 0 0 1 90 34" className="hero-ellipse-path" />
            </svg>

            {/* Student Image: Uses exactly 86% height; top 14% is headroom for upper icons */}
            <img
              src={heroStudentImg}
              alt="Student using a laptop to explore job opportunities"
              className="hero-student-img"
              width={1024}
              height={1318}
              loading="eager"
              fetchPriority="high"
            />

            {/* ─────────────────────────────────────────────────────────
                 7 SYMMETRIC COMPANY LOGOS ON THE UPPER ELLIPTICAL ARC
                 Positions:
                 Logo 1 (170°): left 10.6%, top 29.5%, medium (hide-mobile)
                 Logo 2 (142°): left 18.5%, top 18.0%, large
                 Logo 3 (114°): left 33.7%, top 10.2%, small (hide-1280, hide-mobile)
                 Logo 4 (90°):  left 50.0%, top 8.0%,  large
                 Logo 5 (66°):  left 66.3%, top 10.2%, small (hide-1280, hide-mobile)
                 Logo 6 (38°):  left 81.5%, top 18.0%, large
                 Logo 7 (10°):  left 89.4%, top 29.5%, medium
            ───────────────────────────────────────────────────────── */}

            {/* LOGO 1: Medium at 170° (Left: 10.6%, Top: 29.5%) */}
            <a
              href="#company-1"
              className="hero-logo-card hero-logo--md hero-logo-1 hero-logo-hide-mobile"
              data-parallax="0.8"
              aria-label="Company 1: 140 open engineering roles"
            >
              <div className="hero-logo-float hero-float-1">
                <svg
                  className="hero-logo-icon"
                  viewBox="0 0 32 32"
                  fill="none"
                  xmlns="http://www.w3.org/2000/svg"
                  aria-hidden="true"
                >
                  <rect width="32" height="32" rx="7" fill="#FFEDED" />
                  <path
                    d="M16 7L24 11.5V20.5L16 25L8 20.5V11.5L16 7Z"
                    stroke="#E31E24"
                    strokeWidth="2.2"
                    strokeLinejoin="round"
                  />
                  <circle cx="16" cy="16" r="3" fill="#E31E24" />
                </svg>
              </div>
              <span className="hero-tooltip hero-tooltip--right" role="tooltip">
                <strong>140 openings</strong> &bull;{' '}
                <span className="hero-tooltip-tag">Apply now</span>
              </span>
            </a>

            {/* LOGO 2: Large at 142° (Left: 18.5%, Top: 18.0%) */}
            <a
              href="#company-2"
              className="hero-logo-card hero-logo--lg hero-logo-2"
              data-parallax="1.2"
              aria-label="Company 2: 95 open engineering roles"
            >
              <div className="hero-logo-float hero-float-2">
                <svg
                  className="hero-logo-icon"
                  viewBox="0 0 32 32"
                  fill="none"
                  xmlns="http://www.w3.org/2000/svg"
                  aria-hidden="true"
                >
                  <rect width="32" height="32" rx="8" fill="#FEFDE8" />
                  <path
                    d="M9 19C7.34 19 6 17.66 6 16C6 14.47 7.14 13.2 8.62 13.03C9.28 10.15 11.88 8 15 8C18.66 8 21.66 10.74 21.97 14.34C23.69 14.63 25 16.15 25 18C25 20.21 23.21 22 21 22H10"
                    stroke="#112358"
                    strokeWidth="2.2"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                  />
                </svg>
              </div>
              <span className="hero-tooltip hero-tooltip--right" role="tooltip">
                <strong>95 openings</strong> &bull;{' '}
                <span className="hero-tooltip-tag">Apply now</span>
              </span>
            </a>

            {/* LOGO 3: Small at 114° (Left: 33.7%, Top: 10.2%) */}
            <a
              href="#company-3"
              className="hero-logo-card hero-logo--sm hero-logo-3 hero-logo-hide-1280 hero-logo-hide-mobile"
              data-parallax="0.5"
              aria-label="Company 3: 210 open engineering roles"
            >
              <div className="hero-logo-float hero-float-3">
                <svg
                  className="hero-logo-icon"
                  viewBox="0 0 32 32"
                  fill="none"
                  xmlns="http://www.w3.org/2000/svg"
                  aria-hidden="true"
                >
                  <rect width="32" height="32" rx="6" fill="#EDF4FF" />
                  <path
                    d="M11 12C8.79 12 7 13.79 7 16C7 18.21 8.79 20 11 20C13.5 20 15 17 17 15C19 13 20.5 12 22 12C24.21 12 26 13.79 26 16C26 18.21 24.21 20 22 20C19.5 20 18 17 16 15"
                    stroke="#112358"
                    strokeWidth="2.2"
                    strokeLinecap="round"
                  />
                </svg>
              </div>
              <span className="hero-tooltip hero-tooltip--bottom" role="tooltip">
                <strong>210 openings</strong> &bull;{' '}
                <span className="hero-tooltip-tag">Apply now</span>
              </span>
            </a>

            {/* LOGO 4: Large at 90° (Left: 50.0%, Top: 8.0%) */}
            <a
              href="#company-4"
              className="hero-logo-card hero-logo--lg hero-logo-4"
              data-parallax="1.2"
              aria-label="Company 4: 175 open engineering roles"
            >
              <div className="hero-logo-float hero-float-4">
                <svg
                  className="hero-logo-icon"
                  viewBox="0 0 32 32"
                  fill="none"
                  xmlns="http://www.w3.org/2000/svg"
                  aria-hidden="true"
                >
                  <rect width="32" height="32" rx="8" fill="#FFEDED" />
                  <path
                    d="M16 6L25 11V21L16 26L7 21V11L16 6Z"
                    stroke="#E31E24"
                    strokeWidth="2.2"
                    strokeLinejoin="round"
                  />
                  <path d="M16 16L25 11M16 16L7 11M16 16V26" stroke="#E31E24" strokeWidth="2.2" />
                </svg>
              </div>
              <span className="hero-tooltip hero-tooltip--bottom" role="tooltip">
                <strong>175 openings</strong> &bull;{' '}
                <span className="hero-tooltip-tag">Apply now</span>
              </span>
            </a>

            {/* LOGO 5: Small at 66° (Left: 66.3%, Top: 10.2%) */}
            <a
              href="#company-5"
              className="hero-logo-card hero-logo--sm hero-logo-5 hero-logo-hide-1280 hero-logo-hide-mobile"
              data-parallax="0.5"
              aria-label="Company 5: 62 open engineering roles"
            >
              <div className="hero-logo-float hero-float-5">
                <svg
                  className="hero-logo-icon"
                  viewBox="0 0 32 32"
                  fill="none"
                  xmlns="http://www.w3.org/2000/svg"
                  aria-hidden="true"
                >
                  <rect width="32" height="32" rx="6" fill="#FEFDE8" />
                  <polygon
                    points="16,6 19.5,13 26,16 19.5,19 16,26 12.5,19 6,16 12.5,13"
                    fill="#112358"
                  />
                </svg>
              </div>
              <span className="hero-tooltip hero-tooltip--bottom" role="tooltip">
                <strong>62 openings</strong> &bull;{' '}
                <span className="hero-tooltip-tag">Apply now</span>
              </span>
            </a>

            {/* LOGO 6: Large at 38° (Left: 81.5%, Top: 18.0%) */}
            <a
              href="#company-6"
              className="hero-logo-card hero-logo--lg hero-logo-6"
              data-parallax="1.2"
              aria-label="Company 6: 80 open engineering roles"
            >
              <div className="hero-logo-float hero-float-6">
                <svg
                  className="hero-logo-icon"
                  viewBox="0 0 32 32"
                  fill="none"
                  xmlns="http://www.w3.org/2000/svg"
                  aria-hidden="true"
                >
                  <rect width="32" height="32" rx="8" fill="#EDF4FF" />
                  <path
                    d="M16 6L24 10V16C24 21 20.5 24.5 16 26C11.5 24.5 8 21 8 16V10L16 6Z"
                    stroke="#112358"
                    strokeWidth="2.2"
                    strokeLinejoin="round"
                  />
                  <path
                    d="M13 16L15.5 18.5L20 13.5"
                    stroke="#112358"
                    strokeWidth="2.2"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                  />
                </svg>
              </div>
              <span className="hero-tooltip hero-tooltip--left" role="tooltip">
                <strong>80 openings</strong> &bull;{' '}
                <span className="hero-tooltip-tag">Apply now</span>
              </span>
            </a>

            {/* LOGO 7: Medium at 10° (Left: 89.4%, Top: 29.5%) */}
            <a
              href="#company-7"
              className="hero-logo-card hero-logo--md hero-logo-7"
              data-parallax="0.8"
              aria-label="Company 7: 48 open engineering roles"
            >
              <div className="hero-logo-float hero-float-7">
                <svg
                  className="hero-logo-icon"
                  viewBox="0 0 32 32"
                  fill="none"
                  xmlns="http://www.w3.org/2000/svg"
                  aria-hidden="true"
                >
                  <rect width="32" height="32" rx="7" fill="#FFEDED" />
                  <path
                    d="M6 16H10L13 9L18 23L21 16H26"
                    stroke="#E31E24"
                    strokeWidth="2.2"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                  />
                </svg>
              </div>
              <span className="hero-tooltip hero-tooltip--left" role="tooltip">
                <strong>48 openings</strong> &bull;{' '}
                <span className="hero-tooltip-tag">Apply now</span>
              </span>
            </a>

            {/* ─────────────────────────────────────────────────────────
                 ROOT CAUSE 4: NOTIFICATION CARDS INSIDE WRAPPER
                 Notice 1: left 2%, bottom 24%
                 Notice 2: right 2%, bottom 36%
                 Hidden when screen height < 640px
            ───────────────────────────────────────────────────────── */}
            <div className="hero-notification-card hero-notice-1" aria-hidden="true">
              <div className="hero-notice-icon hero-notice-icon--pink">
                <svg
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="2.2"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                >
                  <path d="M2 12s3-7 10-7 10 7 10 7-3 7-10 7-10-7-10-7Z" />
                  <circle cx="12" cy="12" r="3" />
                </svg>
              </div>
              <div className="hero-notice-content">
                <span className="hero-notice-title">ATS Match Scored</span>
                <span className="hero-notice-desc">Technical Stack &bull; 94% Match</span>
              </div>
            </div>

            <div className="hero-notification-card hero-notice-2" aria-hidden="true">
              <div className="hero-notice-icon hero-notice-icon--yellow">
                <svg
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="2.2"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                >
                  <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" />
                  <polyline points="22 4 12 14.01 9 11.01" />
                </svg>
              </div>
              <div className="hero-notice-content">
                <span className="hero-notice-title">Pipeline Transition</span>
                <span className="hero-notice-desc">Shortlisted &bull; Technical Review</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* ─────────────────────────────────────────────────────────────────
           MOBILE TICKER STRIP (< 768px only)
           Displays partner companies in a smooth horizontal scroll track
      ───────────────────────────────────────────────────────────────── */}
      <div className="hero-mobile-ticker" aria-label="Partner Companies">
        <div className="hero-mobile-ticker-title">Our Learners Work At</div>
        <div
          className="hero-mobile-ticker-track"
          tabIndex={0}
          role="region"
          aria-label="Partner companies list"
        >
          <div className="hero-mobile-ticker-card">
            <svg viewBox="0 0 32 32" fill="none">
              <rect width="32" height="32" rx="7" fill="#FFEDED" />
              <path
                d="M16 7L24 11.5V20.5L16 25L8 20.5V11.5L16 7Z"
                stroke="#E31E24"
                strokeWidth="2.2"
              />
              <circle cx="16" cy="16" r="3" fill="#E31E24" />
            </svg>
            <span>Company 1</span>
          </div>
          <div className="hero-mobile-ticker-card">
            <svg viewBox="0 0 32 32" fill="none">
              <rect width="32" height="32" rx="7" fill="#FEFDE8" />
              <path
                d="M9 19C7.34 19 6 17.66 6 16C6 14.47 7.14 13.2 8.62 13.03C9.28 10.15 11.88 8 15 8C18.66 8 21.66 10.74 21.97 14.34C23.69 14.63 25 16.15 25 18C25 20.21 23.21 22 21 22H10"
                stroke="#112358"
                strokeWidth="2.2"
              />
            </svg>
            <span>Company 2</span>
          </div>
          <div className="hero-mobile-ticker-card">
            <svg viewBox="0 0 32 32" fill="none">
              <rect width="32" height="32" rx="7" fill="#EDF4FF" />
              <path
                d="M11 12C8.79 12 7 13.79 7 16C7 18.21 8.79 20 11 20C13.5 20 15 17 17 15C19 13 20.5 12 22 12C24.21 12 26 13.79 26 16C26 18.21 24.21 20 22 20C19.5 20 18 17 16 15"
                stroke="#112358"
                strokeWidth="2.2"
              />
            </svg>
            <span>Company 3</span>
          </div>
          <div className="hero-mobile-ticker-card">
            <svg viewBox="0 0 32 32" fill="none">
              <rect width="32" height="32" rx="7" fill="#FFEDED" />
              <path
                d="M16 6L25 11V21L16 26L7 21V11L16 6Z"
                stroke="#E31E24"
                strokeWidth="2.2"
              />
              <path d="M16 16L25 11M16 16L7 11M16 16V26" stroke="#E31E24" strokeWidth="2.2" />
            </svg>
            <span>Company 4</span>
          </div>
          <div className="hero-mobile-ticker-card">
            <svg viewBox="0 0 32 32" fill="none">
              <rect width="32" height="32" rx="7" fill="#FEFDE8" />
              <polygon
                points="16,6 19.5,13 26,16 19.5,19 16,26 12.5,19 6,16 12.5,13"
                fill="#112358"
              />
            </svg>
            <span>Company 5</span>
          </div>
          <div className="hero-mobile-ticker-card">
            <svg viewBox="0 0 32 32" fill="none">
              <rect width="32" height="32" rx="7" fill="#EDF4FF" />
              <path
                d="M16 6L24 10V16C24 21 20.5 24.5 16 26C11.5 24.5 8 21 8 16V10L16 6Z"
                stroke="#112358"
                strokeWidth="2.2"
              />
              <path
                d="M13 16L15.5 18.5L20 13.5"
                stroke="#112358"
                strokeWidth="2.2"
              />
            </svg>
            <span>Company 6</span>
          </div>
          <div className="hero-mobile-ticker-card">
            <svg viewBox="0 0 32 32" fill="none">
              <rect width="32" height="32" rx="7" fill="#FFEDED" />
              <path
                d="M6 16H10L13 9L18 23L21 16H26"
                stroke="#E31E24"
                strokeWidth="2.2"
              />
            </svg>
            <span>Company 7</span>
          </div>
        </div>
      </div>
    </section>
  );
};
