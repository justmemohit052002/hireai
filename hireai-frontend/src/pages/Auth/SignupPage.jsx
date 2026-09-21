import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { User, Building2, Lock, Mail, ArrowRight, ArrowLeft, AlertCircle } from 'lucide-react';
import { useAuth } from '@/context/AuthContext';
import { Card } from '@/components/ui/Card';
import { Input } from '@/components/ui/Input';
import { PhoneInput } from '@/components/ui/PhoneInput';
import { Button } from '@/components/ui/Button';
import { Logo } from '@/components/common/Logo';
import { ROUTES } from '@/constants';

export const SignupPage = () => {
  const [step, setStep] = useState(1);
  const [role, setRole] = useState('candidate');
  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [email, setEmail] = useState('');
  const [phoneNumber, setPhoneNumber] = useState('');
  const [password, setPassword] = useState('');
  const [companyName, setCompanyName] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');

  const { registerCandidate, registerRecruiter } = useAuth();
  const navigate = useNavigate();

  const handleNext = async (e) => {
    e.preventDefault();
    setErrorMsg('');

    if (step === 1) {
      if (!firstName.trim() || !lastName.trim()) {
        setErrorMsg('Please enter both your first and last name.');
        return;
      }
      setStep(2);
      return;
    }

    // Step 2 Submission
    const rawDigits = phoneNumber.replace(/\D/g, '');
    if (!phoneNumber || rawDigits.length < 8) {
      setErrorMsg('Please provide a valid mobile number with country code.');
      return;
    }

    if (role === 'recruiter' && !companyName.trim()) {
      setErrorMsg('Company name is required for recruiter accounts.');
      return;
    }

    if (password.length < 8) {
      setErrorMsg('Password must be at least 8 characters long.');
      return;
    }

    setIsLoading(true);
    try {
      if (role === 'candidate') {
        await registerCandidate({
          firstName,
          lastName,
          email,
          phoneNumber: phoneNumber.trim(),
          password,
        });
        navigate(ROUTES.CANDIDATE_JOBS);
      } else {
        await registerRecruiter({
          firstName,
          lastName,
          email,
          phoneNumber: phoneNumber.trim(),
          password,
          companyName,
        });
        navigate(ROUTES.RECRUITER_DASHBOARD);
      }
    } catch (err) {
      setErrorMsg(err.message || 'Registration failed. Please check your details.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-[80vh] flex items-center justify-center px-4 py-12">
      <Card glass className="w-full max-w-md p-8 space-y-6 shadow-2xl rounded-[28px] border border-white/20 dark:border-white/10">
        <div className="text-center space-y-2">
          <Logo size="lg" className="justify-center mb-2" />
          <h2 className="text-2xl font-bold font-heading text-foreground">Create Your Account</h2>
          <p className="text-xs text-muted-foreground">Step {step} of 2 • Account Configuration</p>
        </div>

        {/* Step Indicator Bar */}
        <div className="flex gap-2">
          <div className={`h-1.5 flex-1 rounded-full ${step >= 1 ? 'bg-brand-blue' : 'bg-muted'}`} />
          <div className={`h-1.5 flex-1 rounded-full ${step >= 2 ? 'bg-brand-blue' : 'bg-muted'}`} />
        </div>

        {/* Error Notice */}
        {errorMsg && (
          <div className="p-3.5 rounded-xl bg-red-500/10 border border-red-500/20 text-red-500 text-xs font-medium flex items-start gap-2.5">
            <AlertCircle className="w-4 h-4 shrink-0 mt-0.5" />
            <span>{errorMsg}</span>
          </div>
        )}

        <form onSubmit={handleNext} className="space-y-4">
          {step === 1 ? (
            <>
              {/* Role Selection */}
              <label className="block text-xs font-bold uppercase tracking-wider text-muted-foreground mb-1">
                Select Account Type
              </label>
              <div className="grid grid-cols-2 gap-3 mb-4">
                <button
                  type="button"
                  onClick={() => setRole('candidate')}
                  className={`p-4 rounded-2xl border text-center transition-all flex flex-col items-center gap-2 ${
                    role === 'candidate'
                      ? 'bg-[#C63FC5]/10 border-[#C63FC5] text-foreground font-bold shadow-md'
                      : 'bg-surface-2 border-border text-muted-foreground'
                  }`}
                >
                  <User className="w-6 h-6 text-[#C63FC5]" />
                  <span className="text-xs">Job Seeker</span>
                </button>
                <button
                  type="button"
                  onClick={() => setRole('recruiter')}
                  className={`p-4 rounded-2xl border text-center transition-all flex flex-col items-center gap-2 ${
                    role === 'recruiter'
                      ? 'bg-brand-navy/10 border-[#22214B] dark:border-[#F56681] text-foreground font-bold shadow-md'
                      : 'bg-surface-2 border-border text-muted-foreground'
                  }`}
                >
                  <Building2 className="w-6 h-6 text-[#22214B] dark:text-[#F56681]" />
                  <span className="text-xs">Recruiter</span>
                </button>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-bold uppercase tracking-wider text-muted-foreground mb-1.5">
                    First Name
                  </label>
                  <Input
                    type="text"
                    required
                    value={firstName}
                    onChange={(e) => setFirstName(e.target.value)}
                    placeholder="e.g. Alex"
                  />
                </div>
                <div>
                  <label className="block text-xs font-bold uppercase tracking-wider text-muted-foreground mb-1.5">
                    Last Name
                  </label>
                  <Input
                    type="text"
                    required
                    value={lastName}
                    onChange={(e) => setLastName(e.target.value)}
                    placeholder="e.g. Rivera"
                  />
                </div>
              </div>

              <Button type="submit" variant="gradient" size="lg" className="w-full font-bold shadow-xl mt-4">
                Continue <ArrowRight className="w-4 h-4 ml-2" />
              </Button>
            </>
          ) : (
            <>
              <div>
                <label className="block text-xs font-bold uppercase tracking-wider text-muted-foreground mb-1.5">
                  Work / Personal Email
                </label>
                <Input
                  type="email"
                  required
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="name@example.com"
                  icon={<Mail className="w-4 h-4" />}
                />
              </div>

              <div>
                <label className="block text-xs font-bold uppercase tracking-wider text-muted-foreground mb-1.5">
                  Mobile Number
                </label>
                <PhoneInput
                  required
                  value={phoneNumber}
                  onChange={(val) => setPhoneNumber(val)}
                />
              </div>

              {role === 'recruiter' && (
                <div>
                  <label className="block text-xs font-bold uppercase tracking-wider text-muted-foreground mb-1.5">
                    Company Name
                  </label>
                  <Input
                    type="text"
                    required
                    value={companyName}
                    onChange={(e) => setCompanyName(e.target.value)}
                    placeholder="e.g. Anthropic"
                    icon={<Building2 className="w-4 h-4" />}
                  />
                </div>
              )}

              <div>
                <label className="block text-xs font-bold uppercase tracking-wider text-muted-foreground mb-1.5">
                  Set Password
                </label>
                <Input
                  type="password"
                  required
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="At least 8 characters"
                  icon={<Lock className="w-4 h-4" />}
                />
              </div>

              <div className="flex gap-3 pt-2">
                <Button type="button" variant="ghost" onClick={() => setStep(1)} className="flex-1">
                  <ArrowLeft className="w-4 h-4 mr-1" /> Back
                </Button>
                <Button type="submit" variant="gradient" isLoading={isLoading} className="flex-1 font-bold shadow-xl">
                  Complete Sign Up
                </Button>
              </div>
            </>
          )}
        </form>

        <div className="text-center pt-2 text-xs text-muted-foreground">
          Already have an account?{' '}
          <Link to={ROUTES.LOGIN} className="font-bold text-[#F56681] hover:underline">
            Sign In
          </Link>
        </div>
      </Card>
    </div>
  );
};
