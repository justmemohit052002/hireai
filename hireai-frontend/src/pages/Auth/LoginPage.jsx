import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { User, Building2, Lock, Mail, ArrowRight, AlertCircle } from 'lucide-react';
import { useAuth } from '@/context/AuthContext';
import { Card } from '@/components/ui/Card';
import { Input } from '@/components/ui/Input';
import { Button } from '@/components/ui/Button';
import { Logo } from '@/components/common/Logo';
import { ROUTES } from '@/constants';

export const LoginPage = () => {
  const [role, setRole] = useState('candidate');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');

  const { login } = useAuth();
  const navigate = useNavigate();

  const handleLogin = async (e) => {
    e.preventDefault();
    setErrorMsg('');
    setIsLoading(true);

    try {
      const result = await login(email, password);
      const targetRole = result.role || role;
      if (targetRole === 'candidate') {
        navigate(ROUTES.CANDIDATE_JOBS);
      } else {
        navigate(ROUTES.RECRUITER_DASHBOARD);
      }
    } catch (err) {
      setErrorMsg(err.message || 'Invalid email or password. Please try again.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-[80vh] flex items-center justify-center px-4 py-12">
      <Card glass className="w-full max-w-md p-8 space-y-6 shadow-2xl rounded-[28px] border border-white/20 dark:border-white/10">
        <div className="text-center space-y-2">
          <Logo size="lg" className="justify-center mb-2" />
          <h2 className="text-2xl font-bold font-heading text-foreground">Welcome Back</h2>
          <p className="text-xs text-muted-foreground">Sign in to your HireAI account</p>
        </div>

        {/* Role Selector Tabs */}
        <div className="grid grid-cols-2 gap-2 p-1.5 rounded-2xl bg-muted/60 glass border border-white/10">
          <button
            type="button"
            onClick={() => setRole('candidate')}
            className={`flex items-center justify-center gap-2 py-2.5 rounded-xl text-xs font-bold transition-all ${
              role === 'candidate'
                ? 'bg-brand-accent text-brand-dark text-foreground shadow-md shadow-[#C63FC5]/20'
                : 'text-muted-foreground hover:text-foreground'
            }`}
          >
            <User className="w-4 h-4" /> Candidate
          </button>
          <button
            type="button"
            onClick={() => setRole('recruiter')}
            className={`flex items-center justify-center gap-2 py-2.5 rounded-xl text-xs font-bold transition-all ${
              role === 'recruiter'
                ? 'bg-brand-navy text-foreground shadow-md shadow-[#22214B]/20'
                : 'text-muted-foreground hover:text-foreground'
            }`}
          >
            <Building2 className="w-4 h-4" /> Recruiter
          </button>
        </div>

        {/* Error Notice */}
        {errorMsg && (
          <div className="p-3.5 rounded-xl bg-red-500/10 border border-red-500/20 text-red-500 text-xs font-medium flex items-start gap-2.5">
            <AlertCircle className="w-4 h-4 shrink-0 mt-0.5" />
            <span>{errorMsg}</span>
          </div>
        )}

        {/* Login Form */}
        <form onSubmit={handleLogin} className="space-y-4">
          <div>
            <label className="block text-xs font-bold uppercase tracking-wider text-muted-foreground mb-1.5">
              Email Address
            </label>
            <Input
              type="email"
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
              type="password"
              required
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="••••••••••••"
              icon={<Lock className="w-4 h-4" />}
            />
          </div>

          <Button type="submit" variant="gradient" size="lg" isLoading={isLoading} className="w-full font-bold shadow-xl">
            Sign In as {role === 'candidate' ? 'Candidate' : 'Recruiter'}
            <ArrowRight className="w-4 h-4 ml-2" />
          </Button>
        </form>

        <div className="text-center pt-2 text-xs text-muted-foreground">
          Don't have an account yet?{' '}
          <Link to={ROUTES.SIGNUP} className="font-bold text-[#F56681] hover:underline">
            Sign Up
          </Link>
        </div>
      </Card>
    </div>
  );
};
