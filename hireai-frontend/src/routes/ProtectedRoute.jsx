import React from 'react';
import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from '@/context/AuthContext';
import { ROUTES } from '@/constants';
import { Skeleton } from '@/components/ui/Skeleton';

export const ProtectedRoute = ({ allowedRole }) => {
  const { isAuthenticated, role, isLoading } = useAuth();

  if (isLoading) {
    return (
      <div className="p-8 space-y-4 max-w-4xl mx-auto min-h-[60vh] flex flex-col justify-center">
        <Skeleton className="h-10 w-1/3 rounded-xl mx-auto" />
        <Skeleton className="h-48 w-full rounded-2xl" />
      </div>
    );
  }

  if (!isAuthenticated) {
    return <Navigate to={ROUTES.LOGIN} replace />;
  }

  if (allowedRole && role) {
    // If route requires admin, only admin is allowed
    if (allowedRole === 'admin' && role !== 'admin') {
      const fallback = role === 'candidate' ? ROUTES.CANDIDATE_JOBS : ROUTES.RECRUITER_DASHBOARD;
      return <Navigate to={fallback} replace />;
    }

    // If route requires recruiter or candidate, ensure match
    if (allowedRole !== 'admin' && role !== allowedRole && role !== 'admin') {
      const fallback = role === 'candidate' ? ROUTES.CANDIDATE_JOBS : ROUTES.RECRUITER_DASHBOARD;
      return <Navigate to={fallback} replace />;
    }
  }

  return <Outlet />;
};
