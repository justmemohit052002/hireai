import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '@/context/AuthContext';
import { ROUTES } from '@/constants';

const GOOGLE_CLIENT_ID = import.meta.env.VITE_GOOGLE_CLIENT_ID || '';

export const OAuthButtons = ({ role = 'candidate', companyName = '', onError }) => {
  const { loginWithGoogle, loginWithLinkedIn } = useAuth();
  const navigate = useNavigate();
  const [loadingProvider, setLoadingProvider] = useState(null);

  const handleGoogleAuth = () => {
    setLoadingProvider('GOOGLE');
    if (onError) onError('');

    // Check if Google Identity Services SDK is ready and Client ID is configured
    if (window.google?.accounts?.oauth2 && GOOGLE_CLIENT_ID) {
      try {
        const tokenClient = window.google.accounts.oauth2.initTokenClient({
          client_id: GOOGLE_CLIENT_ID,
          scope: 'openid email profile',
          callback: async (tokenResponse) => {
            if (tokenResponse.error) {
              setLoadingProvider(null);
              if (tokenResponse.error !== 'popup_closed_by_user') {
                onError?.(tokenResponse.error_description || 'Google sign-in was cancelled.');
              }
              return;
            }

            try {
              const result = await loginWithGoogle({
                token: tokenResponse.access_token,
                role: role.toUpperCase(),
                companyName: role === 'recruiter' ? companyName || 'Company Inc.' : null,
              });

              const targetRole = result?.role?.toLowerCase() || role.toLowerCase();
              if (targetRole === 'recruiter' || role === 'recruiter') {
                navigate(ROUTES.RECRUITER_DASHBOARD);
              } else {
                navigate(ROUTES.CANDIDATE_JOBS);
              }
            } catch (err) {
              console.error('Google OAuth backend error:', err);
              onError?.(err.message || 'Google authentication failed. Please try again.');
            } finally {
              setLoadingProvider(null);
            }
          },
          error_callback: (err) => {
            console.error('Google Identity error:', err);
            setLoadingProvider(null);
            onError?.(err?.message || 'Failed to open Google login popup. Please allow popups.');
          },
        });

        tokenClient.requestAccessToken({ prompt: 'select_account' });
        return;
      } catch (err) {
        console.warn('Error launching Google token client, falling back:', err);
      }
    }

    // Fallback simulation mode if Google script is offline/blocked
    handleSimulatedAuth('GOOGLE');
  };

  const handleSimulatedAuth = async (provider) => {
    try {
      const payload = {
        token: `mock-${provider.toLowerCase()}-token-${Date.now()}`,
        role: role.toUpperCase(),
        companyName: role === 'recruiter' ? companyName || 'Company Inc.' : null,
        email: `${role.toLowerCase()}.${provider.toLowerCase()}@vionsys.com`,
        firstName: provider === 'GOOGLE' ? 'Google' : 'LinkedIn',
        lastName: role === 'recruiter' ? 'Recruiter' : 'Candidate',
      };

      const result = provider === 'GOOGLE' 
        ? await loginWithGoogle(payload) 
        : await loginWithLinkedIn(payload);

      const targetRole = result?.role?.toLowerCase() || role.toLowerCase();
      if (targetRole === 'recruiter' || role === 'recruiter') {
        navigate(ROUTES.RECRUITER_DASHBOARD);
      } else {
        navigate(ROUTES.CANDIDATE_JOBS);
      }
    } catch (err) {
      console.error(`${provider} OAuth failed:`, err);
      if (onError) {
        onError(err.message || `${provider} authentication failed. Please try again.`);
      }
    } finally {
      setLoadingProvider(null);
    }
  };

  const handleOAuth = (provider) => {
    if (provider === 'GOOGLE') {
      handleGoogleAuth();
    } else {
      setLoadingProvider(provider);
      if (onError) onError('');
      handleSimulatedAuth(provider);
    }
  };

  return (
    <div className="w-full space-y-3">
      <div className="relative flex items-center justify-center my-4">
        <div className="border-t border-border w-full" />
        <span className="bg-card px-3 text-[11px] font-semibold uppercase tracking-wider text-muted-foreground shrink-0">
          Or continue with
        </span>
        <div className="border-t border-border w-full" />
      </div>

      <div className="grid grid-cols-2 gap-2.5">
        {/* Google SSO Button */}
        <button
          type="button"
          onClick={() => handleOAuth('GOOGLE')}
          disabled={loadingProvider !== null}
          className="flex items-center justify-center gap-2 px-3 py-2.5 rounded-xl border border-border bg-surface-2 hover:bg-muted text-foreground text-xs font-semibold transition-all shadow-sm hover:shadow active:scale-[0.98] disabled:opacity-50"
        >
          {loadingProvider === 'GOOGLE' ? (
            <div className="w-4 h-4 border-2 border-foreground/30 border-t-foreground rounded-full animate-spin" />
          ) : (
            <svg className="w-4 h-4 shrink-0" viewBox="0 0 24 24">
              <path
                fill="#4285F4"
                d="M23.745 12.27c0-.7-.06-1.4-.19-2.07H12v4.51h6.6c-.29 1.52-1.14 2.8-2.4 3.66v3.04h3.88c2.28-2.09 3.66-5.17 3.66-9.14z"
              />
              <path
                fill="#34A853"
                d="M12 24c3.24 0 5.95-1.08 7.93-2.91l-3.88-3.04c-1.08.72-2.45 1.16-4.05 1.16-3.12 0-5.77-2.1-6.72-4.93H1.24v3.13C3.26 21.48 7.33 24 12 24z"
              />
              <path
                fill="#FBBC05"
                d="M5.28 14.28c-.24-.72-.38-1.49-.38-2.28s.14-1.56.38-2.28V6.59H1.24C.45 8.16 0 9.98 0 12s.45 3.84 1.24 5.41l4.04-3.13z"
              />
              <path
                fill="#EA4335"
                d="M12 4.75c1.77 0 3.35.61 4.6 1.8l3.42-3.42C17.95 1.19 15.24 0 12 0 7.33 0 3.26 2.52 1.24 6.59l4.04 3.13c.95-2.83 3.6-4.97 6.72-4.97z"
              />
            </svg>
          )}
          <span>Google</span>
        </button>

        {/* LinkedIn SSO Button */}
        <button
          type="button"
          onClick={() => handleOAuth('LINKEDIN')}
          disabled={loadingProvider !== null}
          className="flex items-center justify-center gap-2 px-3 py-2.5 rounded-xl border border-border bg-surface-2 hover:bg-muted text-foreground text-xs font-semibold transition-all shadow-sm hover:shadow active:scale-[0.98] disabled:opacity-50"
        >
          {loadingProvider === 'LINKEDIN' ? (
            <div className="w-4 h-4 border-2 border-[#0A66C2]/30 border-t-[#0A66C2] rounded-full animate-spin" />
          ) : (
            <svg className="w-4 h-4 shrink-0" fill="#0A66C2" viewBox="0 0 24 24">
              <path d="M19 3a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h14m-.5 15.5v-5.3a3.26 3.26 0 0 0-3.26-3.26c-.85 0-1.84.52-2.28 1.3v-1.11h-2.79v8.37h2.79v-4.93c0-.77.62-1.4 1.39-1.4a1.4 1.4 0 0 1 1.4 1.4v4.93h2.75M6.88 8.56a1.68 1.68 0 0 0 1.68-1.68c0-.93-.75-1.69-1.68-1.69a1.69 1.69 0 0 0-1.69 1.69c0 .93.76 1.68 1.69 1.68m1.39 9.94v-8.37H5.5v8.37h2.77z" />
            </svg>
          )}
          <span>LinkedIn</span>
        </button>
      </div>
    </div>
  );
};
