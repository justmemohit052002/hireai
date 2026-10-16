import React, { useState, useEffect } from 'react';
import { cn, getInitials } from '@/utils';
import { apiClient } from '@/services/api/apiClient';

export const Avatar = ({
  src,
  name,
  size = 'md',
  badge,
  className,
  ...props
}) => {
  const [blobUrl, setBlobUrl] = useState(null);
  const [imgError, setImgError] = useState(false);

  useEffect(() => {
    let isCancelled = false;
    let localCreatedUrl = null;

    if (!src) {
      setBlobUrl(null);
      setImgError(false);
      return;
    }

    // Direct object URLs or data URLs
    if (src.startsWith('blob:') || src.startsWith('data:')) {
      setBlobUrl(src);
      setImgError(false);
      return;
    }

    // External third-party URLs (e.g. Google profile photos)
    const isExternal =
      /^https?:\/\//i.test(src) &&
      !src.includes(window.location.host) &&
      !src.includes('localhost:8080') &&
      !src.includes('127.0.0.1:8080');

    if (isExternal) {
      setBlobUrl(src);
      setImgError(false);
      return;
    }

    // Internal endpoint: fetch image binary with Bearer token
    setImgError(false);
    apiClient
      .get(src, { silent: true })
      .then((data) => {
        if (isCancelled) return;
        if (data instanceof Blob && data.size > 0) {
          localCreatedUrl = URL.createObjectURL(data);
          setBlobUrl(localCreatedUrl);
          setImgError(false);
        } else {
          setImgError(true);
          setBlobUrl(null);
        }
      })
      .catch(() => {
        if (!isCancelled) {
          setImgError(true);
          setBlobUrl(null);
        }
      });

    return () => {
      isCancelled = true;
      if (localCreatedUrl) {
        URL.revokeObjectURL(localCreatedUrl);
      }
    };
  }, [src]);

  const sizeClasses = {
    sm: 'w-8 h-8 text-xs',
    md: 'w-10 h-10 text-sm',
    lg: 'w-12 h-12 text-base',
    xl: 'w-16 h-16 text-lg',
  };

  return (
    <div className="relative inline-block">
      <div
        className={cn(
          'relative flex items-center justify-center rounded-full overflow-hidden font-semibold border border-brand-blue/25 bg-brand-navy/15 text-brand-blue dark:text-brand-blue-light select-none shrink-0 shadow-sm',
          sizeClasses[size],
          className
        )}
        {...props}
      >
        {blobUrl && !imgError ? (
          <img
            src={blobUrl}
            alt={name || 'User Avatar'}
            onError={() => setImgError(true)}
            className="w-full h-full object-cover"
          />
        ) : (
          <span>{getInitials(name)}</span>
        )}
      </div>
      {badge && <div className="absolute -bottom-0.5 -right-0.5">{badge}</div>}
    </div>
  );
};

