import React, { useState, useEffect } from 'react';

/**
 * Non-blocking, high-performance top loading progress line & ambient page reveal
 * Inspired by Linear and GitHub's page stream loader.
 */
export const LandingPageLoader = ({ onComplete }) => {
  const [progress, setProgress] = useState(25);
  const [isVisible, setIsVisible] = useState(true);

  useEffect(() => {
    const t1 = setTimeout(() => setProgress(65), 80);
    const t2 = setTimeout(() => setProgress(100), 220);
    const t3 = setTimeout(() => {
      setIsVisible(false);
      if (onComplete) onComplete();
    }, 450);

    return () => {
      clearTimeout(t1);
      clearTimeout(t2);
      clearTimeout(t3);
    };
  }, [onComplete]);

  if (!isVisible) return null;

  return (
    <div className="fixed top-0 left-0 right-0 z-50 pointer-events-none">
      <div className="h-[2.5px] w-full bg-transparent overflow-hidden">
        <div
          className="h-full bg-gradient-to-r from-brand-blue via-brand-navy to-brand-accent transition-all duration-300 ease-out shadow-[0_0_12px_rgba(1,146,198,0.8)]"
          style={{
            width: `${progress}%`,
            transitionProperty: 'width',
          }}
        />
      </div>
    </div>
  );
};
