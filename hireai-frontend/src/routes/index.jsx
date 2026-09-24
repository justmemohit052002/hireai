import React, { lazy, Suspense } from 'react';
import { createBrowserRouter, RouterProvider } from 'react-router-dom';
import { PublicLayout } from '@/components/layout/PublicLayout';
import { AuthLayout } from '@/components/layout/AuthLayout';
import { CandidateLayout } from '@/components/layout/CandidateLayout';
import { RecruiterLayout } from '@/components/layout/RecruiterLayout';
import { ProtectedRoute } from './ProtectedRoute';
import { ROUTES } from '@/constants';
import { Skeleton } from '@/components/ui/Skeleton';

// Lazy loaded page components for performance & code splitting
const LandingPage = lazy(() => import('@/pages/Landing/LandingPage').then((m) => ({ default: m.LandingPage })));
const LoginPage = lazy(() => import('@/pages/Auth/LoginPage').then((m) => ({ default: m.LoginPage })));
const SignupPage = lazy(() => import('@/pages/Auth/SignupPage').then((m) => ({ default: m.SignupPage })));

// Candidate pages
const CandidateJobsPage = lazy(() => import('@/pages/Candidate/CandidateJobsPage').then((m) => ({ default: m.CandidateJobsPage })));
const CandidateApplicationsPage = lazy(() => import('@/pages/Candidate/CandidateApplicationsPage').then((m) => ({ default: m.CandidateApplicationsPage })));
const CandidateInboxPage = lazy(() => import('@/pages/Candidate/CandidateInboxPage').then((m) => ({ default: m.CandidateInboxPage })));
const CandidateProfilePage = lazy(() => import('@/pages/Candidate/CandidateProfilePage').then((m) => ({ default: m.CandidateProfilePage })));
const CandidateSettingsPage = lazy(() => import('@/pages/Candidate/CandidateSettingsPage').then((m) => ({ default: m.CandidateSettingsPage })));

// Recruiter pages
const RecruiterDashboardPage = lazy(() => import('@/pages/Recruiter/RecruiterDashboardPage').then((m) => ({ default: m.RecruiterDashboardPage })));
const RecruiterJobsPage = lazy(() => import('@/pages/Recruiter/RecruiterJobsPage').then((m) => ({ default: m.RecruiterJobsPage })));
const CreateJobPage = lazy(() => import('@/pages/Recruiter/CreateJobPage').then((m) => ({ default: m.CreateJobPage })));
const LeaderboardPage = lazy(() => import('@/pages/Recruiter/LeaderboardPage').then((m) => ({ default: m.LeaderboardPage })));
const RecruiterInboxPage = lazy(() => import('@/pages/Recruiter/RecruiterInboxPage').then((m) => ({ default: m.RecruiterInboxPage })));
const RecruiterProfilePage = lazy(() => import('@/pages/Recruiter/RecruiterProfilePage').then((m) => ({ default: m.RecruiterProfilePage })));
const RecruiterSettingsPage = lazy(() => import('@/pages/Recruiter/RecruiterSettingsPage').then((m) => ({ default: m.RecruiterSettingsPage })));

const SuspenseFallback = () => (
  <div className="p-8 space-y-4 max-w-4xl mx-auto">
    <Skeleton className="h-12 w-3/4 rounded-2xl" />
    <Skeleton className="h-64 w-full rounded-2xl" />
  </div>
);

const router = createBrowserRouter([
  // Public Landing Page (Floating Navbar + Hero + Footer)
  {
    path: '/',
    element: <PublicLayout />,
    children: [
      {
        index: true,
        element: (
          <Suspense fallback={<SuspenseFallback />}>
            <LandingPage />
          </Suspense>
        ),
      },
    ],
  },

  // Auth Routes (Clean AuthLayout with no floating navbar or footer)
  {
    element: <AuthLayout />,
    children: [
      {
        path: ROUTES.LOGIN,
        element: (
          <Suspense fallback={<SuspenseFallback />}>
            <LoginPage />
          </Suspense>
        ),
      },
      {
        path: ROUTES.SIGNUP,
        element: (
          <Suspense fallback={<SuspenseFallback />}>
            <SignupPage />
          </Suspense>
        ),
      },
    ],
  },

  // Candidate Protected Layout Routes
  {
    path: '/candidate',
    element: <ProtectedRoute allowedRole="candidate" />,
    children: [
      {
        element: <CandidateLayout />,
        children: [
          {
            path: 'jobs',
            element: (
              <Suspense fallback={<SuspenseFallback />}>
                <CandidateJobsPage />
              </Suspense>
            ),
          },
          {
            path: 'applications',
            element: (
              <Suspense fallback={<SuspenseFallback />}>
                <CandidateApplicationsPage />
              </Suspense>
            ),
          },
          {
            path: 'inbox',
            element: (
              <Suspense fallback={<SuspenseFallback />}>
                <CandidateInboxPage />
              </Suspense>
            ),
          },
          {
            path: 'profile',
            element: (
              <Suspense fallback={<SuspenseFallback />}>
                <CandidateProfilePage />
              </Suspense>
            ),
          },
          {
            path: 'settings',
            element: (
              <Suspense fallback={<SuspenseFallback />}>
                <CandidateSettingsPage />
              </Suspense>
            ),
          },
        ],
      },
    ],
  },

  // Recruiter Protected Layout Routes
  {
    path: '/recruiter',
    element: <ProtectedRoute allowedRole="recruiter" />,
    children: [
      {
        element: <RecruiterLayout />,
        children: [
          {
            path: 'dashboard',
            element: (
              <Suspense fallback={<SuspenseFallback />}>
                <RecruiterDashboardPage />
              </Suspense>
            ),
          },
          {
            path: 'jobs',
            element: (
              <Suspense fallback={<SuspenseFallback />}>
                <RecruiterJobsPage />
              </Suspense>
            ),
          },
          {
            path: 'jobs/create',
            element: (
              <Suspense fallback={<SuspenseFallback />}>
                <CreateJobPage />
              </Suspense>
            ),
          },
          {
            path: 'jobs/edit/:id',
            element: (
              <Suspense fallback={<SuspenseFallback />}>
                <CreateJobPage />
              </Suspense>
            ),
          },
          {
            path: 'leaderboard',
            element: (
              <Suspense fallback={<SuspenseFallback />}>
                <LeaderboardPage />
              </Suspense>
            ),
          },
          {
            path: 'inbox',
            element: (
              <Suspense fallback={<SuspenseFallback />}>
                <RecruiterInboxPage />
              </Suspense>
            ),
          },
          {
            path: 'profile',
            element: (
              <Suspense fallback={<SuspenseFallback />}>
                <RecruiterProfilePage />
              </Suspense>
            ),
          },
          {
            path: 'settings',
            element: (
              <Suspense fallback={<SuspenseFallback />}>
                <RecruiterSettingsPage />
              </Suspense>
            ),
          },
        ],
      },
    ],
  },
]);

export const AppRoutes = () => {
  return <RouterProvider router={router} />;
};
