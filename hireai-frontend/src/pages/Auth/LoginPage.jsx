import React, { useState } from 'react';
import { useNavigate, useLocation, Link } from 'react-router-dom';
import { User, Building2, Lock, Mail, ArrowRight, AlertCircle, CheckCircle2 } from 'lucide-react';
import { useAuth } from '@/context/AuthContext';
import { Card } from '@/components/ui/Card';
import { Input } from '@/components/ui/Input';
import { Button } from '@/components/ui/Button';
import { Logo } from '@/components/brand/Logo';
import { OAuthButtons } from '@/components/auth/OAuthButtons';
import { ROUTES } from '@/constants';

export const LoginPage = () => {
  const location = useLocation();
  const [role, setRole] = useState(() => location.state?.role || 'candidate');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');
  const [isUnverified, setIsUnverified] = useState(false);

  const infoMessage = location.state?.message;

  const { login } = useAuth();
  const navigate = useNavigate();

  const handleLogin = async (e) => {
    e.preventDefault();
    setErrorMsg('');
    setIsUnverified(false);
    setIsLoading(true);

    try {
      const result = await login(email, password, role);
      const targetRole = result.role || role;
      if (targetRole === 'admin') {
        navigate(ROUTES.ADMIN_DASHBOARD);
      } else if (targetRole === 'recruiter') {
        navigate(ROUTES.RECRUITER_DASHBOARD);
      } else {
        navigate(ROUTES.CANDIDATE_JOBS);
      }
    } catch (err) {
      const msg = err.message || '';
      if (
        err.status === 403 ||
        err.error === 'ACCOUNT_NOT_VERIFIED' ||
        msg.toLowerCase().includes('not verified')
      ) {
        setIsUnverified(true);
        setErrorMsg('Your account email is not verified yet. Please enter the 6-digit OTP code to complete activation.');
      } else if (err.status === 404) {
        setIsUnverified(false);
        setErrorMsg(msg || 'User not found. Please sign up first.');
      } else if (err.status === 401) {
        setIsUnverified(false);
        setErrorMsg(msg || 'Invalid email or password. Please try again.');
      } else if (err.status === 429) {
        setIsUnverified(false);
        setErrorMsg(msg || 'Too many failed login attempts. Please try again later.');
      } else if (err.status >= 500) {
        setIsUnverified(false);
        setErrorMsg('An unexpected server error occurred. Please try again later.');
      } else {
        setIsUnverified(false);
        setErrorMsg(msg || 'Request failed. Please check your credentials and try again.');
      }
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="w-full my-auto">
      <Card glass className="w-full max-w-md mx-auto p-5 sm:p-6 space-y-4 sm:space-y-4.5 shadow-2xl rounded-[24px] border border-white/20 dark:border-white/10">
        <div className="text-center space-y-1">
          <Logo variant="full" size="lg" showPro className="justify-center mb-1" />
          <h2 className="text-xl sm:text-2xl font-bold font-heading text-foreground">Welcome Back</h2>
          <p className="text-xs text-muted-foreground">Sign in to your HireAI account</p>
        </div>

        {/* Success / Info Notice */}
        {infoMessage && (
          <div className="p-3.5 rounded-xl bg-emerald-500/10 border border-emerald-500/20 text-emerald-600 dark:text-emerald-400 text-xs font-medium flex items-start gap-2.5">
            <CheckCircle2 className="w-4 h-4 shrink-0 mt-0.5 text-emerald-500" />
            <span>{infoMessage}</span>
          </div>
        )}

        {/* Role Selector Tabs */}
        <div className="grid grid-cols-2 gap-2 p-1.5 rounded-2xl bg-muted/60 glass border border-white/10">
          <button
            type="button"
            onClick={() => setRole('candidate')}
            className={`flex items-center justify-center gap-2 py-2 rounded-xl text-xs font-bold transition-all ${
              role === 'candidate'
                ? 'bg-brand-accent text-slate-950 shadow-md shadow-brand-accent/20'
                : 'text-muted-foreground hover:text-foreground'
            }`}
          >
            <User className="w-4 h-4" /> Candidate
          </button>
          <button
            type="button"
            onClick={() => setRole('recruiter')}
            className={`flex items-center justify-center gap-2 py-2 rounded-xl text-xs font-bold transition-all ${
              role === 'recruiter'
                ? 'bg-brand-accent text-slate-950 shadow-md shadow-brand-accent/20'
                : 'text-muted-foreground hover:text-foreground'
            }`}
          >
            <Building2 className="w-4 h-4" /> Recruiter
          </button>
        </div>

        {/* Error Notice */}
        {errorMsg && (
          <div className="p-3.5 rounded-xl bg-red-500/10 border border-red-500/20 text-red-500 text-xs font-medium space-y-2">
            <div className="flex items-start gap-2.5">
              <AlertCircle className="w-4 h-4 shrink-0 mt-0.5" />
              <span>{errorMsg}</span>
            </div>
            {isUnverified && (
              <Button
                type="button"
                variant="gradient"
                size="sm"
                onClick={() => navigate(ROUTES.SIGNUP, { state: { email, step: 3, role } })}
                className="w-full mt-2 font-bold shadow-md"
              >
                Verify Email Now <ArrowRight className="w-3.5 h-3.5 ml-1.5" />
              </Button>
            )}
          </div>
        )}

        {/* Login Form */}
        <form onSubmit={handleLogin} className="space-y-4">
          <div>
            <label className="block text-xs font-bold uppercase tracking-wider text-muted-foreground mb-1.5">
              Email Address
            </label>
            <Input
              id="login-email"
              name="email"
              type="email"
              autoComplete="email"
              required
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder={role === 'candidate' ? 'alex@example.com' : 'recruiter@company.com'}
              icon={<Mail className="w-4 h-4" />}
            />
          </div>

          <div>
            <label className="block text-xs font-bold uppercase tracking-wider text-muted-foreground mb-1.5">
              Password
            </label>
            <Input
              id="login-password"
              name="password"
              type="password"
              autoComplete="current-password"
              required
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="Enter your password"
              icon={<Lock className="w-4 h-4" />}
            />
          </div>

          <Button type="submit" variant="gradient" size="lg" isLoading={isLoading} className="w-full font-bold shadow-xl">
            Sign In as {role === 'candidate' ? 'Candidate' : 'Recruiter'}
            <ArrowRight className="w-4 h-4 ml-2" />
          </Button>
        </form>

        <OAuthButtons role={role} onError={(err) => setErrorMsg(err)} />

        <div className="text-center pt-2 text-xs text-muted-foreground">
          Don't have an account yet?{' '}
          <Link to={ROUTES.SIGNUP} state={{ role }} className="font-bold text-brand-blue dark:text-brand-accent hover:underline">
            Sign Up
          </Link>
        </div>
      </Card>
    </div>
  );
};
