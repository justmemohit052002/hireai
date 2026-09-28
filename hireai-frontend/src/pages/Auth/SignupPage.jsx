import React, { useState, useEffect, useRef } from 'react';
import { useNavigate, useLocation, Link } from 'react-router-dom';
import {
  User,
  Building2,
  Lock,
  Mail,
  ArrowRight,
  ArrowLeft,
  AlertCircle,
  CheckCircle2,
  RefreshCw,
  KeyRound,
  ShieldCheck,
} from 'lucide-react';
import { useAuth } from '@/context/AuthContext';
import { Card } from '@/components/ui/Card';
import { Input } from '@/components/ui/Input';
import { PhoneInput } from '@/components/ui/PhoneInput';
import { Button } from '@/components/ui/Button';
import { Logo } from '@/components/common/Logo';
import { OAuthButtons } from '@/components/auth/OAuthButtons';
import { ROUTES } from '@/constants';

export const SignupPage = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const { registerCandidate, registerRecruiter, verifyEmailOtp, resendEmailOtp } = useAuth();

  const [step, setStep] = useState(() => location.state?.step || 1);
  const [role, setRole] = useState(() => location.state?.role || 'candidate');
  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [email, setEmail] = useState(() => location.state?.email || '');
  const [phoneNumber, setPhoneNumber] = useState('');
  const [password, setPassword] = useState('');
  const [companyName, setCompanyName] = useState('');

  // OTP verification state
  const [otp, setOtp] = useState(['', '', '', '', '', '']);
  const otpInputs = useRef([]);
  const [resendCooldown, setResendCooldown] = useState(0);
  const [resendSuccess, setResendSuccess] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');

  // Countdown timer for OTP resend
  useEffect(() => {
    let timer;
    if (resendCooldown > 0) {
      timer = setInterval(() => {
        setResendCooldown((prev) => (prev > 0 ? prev - 1 : 0));
      }, 1000);
    }
    return () => clearInterval(timer);
  }, [resendCooldown]);

  const handleNext = async (e) => {
    e.preventDefault();
    setErrorMsg('');
    setResendSuccess('');

    if (step === 1) {
      if (!firstName.trim() || !lastName.trim()) {
        setErrorMsg('Please enter both your first and last name.');
        return;
      }
      setStep(2);
      return;
    }

    // Step 2 Submission (Registration & trigger OTP)
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
      let result;
      if (role === 'candidate') {
        result = await registerCandidate({
          firstName,
          lastName,
          email: email.trim().toLowerCase(),
          phoneNumber: phoneNumber.trim(),
          password,
        });
      } else {
        result = await registerRecruiter({
          firstName,
          lastName,
          email: email.trim().toLowerCase(),
          phoneNumber: phoneNumber.trim(),
          password,
          companyName: companyName.trim(),
        });
      }

      // If already verified or bypassed, direct login
      if (result && result.user && !result.requiresVerification) {
        if (role === 'recruiter' || result.role === 'recruiter') {
          navigate(ROUTES.RECRUITER_DASHBOARD);
        } else {
          navigate(ROUTES.CANDIDATE_JOBS);
        }
        return;
      }

      // Transition to Step 3: OTP Verification
      setStep(3);
      setResendCooldown(60);
      setResendSuccess(`A 6-digit verification code has been sent to ${email.trim()}`);
      setTimeout(() => {
        otpInputs.current[0]?.focus();
      }, 100);
    } catch (err) {
      setErrorMsg(err.message || 'Registration failed. Please check your details.');
    } finally {
      setIsLoading(false);
    }
  };

  const handleOtpChange = (index, value) => {
    setErrorMsg('');
    setResendSuccess('');

    // Handle full paste of 6 digits
    const cleaned = value.replace(/\D/g, '');
    if (cleaned.length > 1) {
      const digits = cleaned.slice(0, 6).split('');
      const newOtp = [...otp];
      digits.forEach((d, i) => {
        if (i < 6) newOtp[i] = d;
      });
      setOtp(newOtp);
      const nextIndex = Math.min(digits.length, 5);
      otpInputs.current[nextIndex]?.focus();
      return;
    }

    const digit = cleaned.slice(-1);
    const newOtp = [...otp];
    newOtp[index] = digit;
    setOtp(newOtp);

    if (digit && index < 5) {
      otpInputs.current[index + 1]?.focus();
    }
  };

  const handleOtpKeyDown = (index, e) => {
    if (e.key === 'Backspace' && !otp[index] && index > 0) {
      otpInputs.current[index - 1]?.focus();
    }
  };

  const handleVerifyOtp = async (e) => {
    e.preventDefault();
    const enteredOtp = otp.join('');
    if (enteredOtp.length !== 6) {
      setErrorMsg('Please enter all 6 digits of your verification code.');
      return;
    }

    setIsLoading(true);
    setErrorMsg('');
    setResendSuccess('');

    try {
      const res = await verifyEmailOtp({
        email: email.trim().toLowerCase(),
        otp: enteredOtp,
      });

      const userRole = res?.role || role;
      if (userRole === 'recruiter' || role === 'recruiter') {
        navigate(ROUTES.RECRUITER_DASHBOARD);
      } else {
        navigate(ROUTES.CANDIDATE_JOBS);
      }
    } catch (err) {
      setErrorMsg(err.message || 'Invalid or expired OTP code. Please try again.');
    } finally {
      setIsLoading(false);
    }
  };

  const handleResend = async () => {
    if (resendCooldown > 0 || isLoading) return;
    setErrorMsg('');
    setResendSuccess('');
    setIsLoading(true);

    try {
      await resendEmailOtp({ email: email.trim().toLowerCase() });
      setResendCooldown(60);
      setResendSuccess(`A fresh 6-digit verification code has been sent to ${email.trim()}`);
      setOtp(['', '', '', '', '', '']);
      otpInputs.current[0]?.focus();
    } catch (err) {
      setErrorMsg(err.message || 'Failed to resend verification code. Please wait and try again.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="w-full my-auto">
      <Card glass className="w-full max-w-md mx-auto p-6 sm:p-8 space-y-6 shadow-2xl rounded-[28px] border border-white/20 dark:border-white/10">
        <div className="text-center space-y-1.5">
          <Logo size="lg" className="justify-center mb-1.5" />
          <h2 className="text-2xl font-bold font-heading text-foreground">
            {step === 3 ? 'Verify Your Email' : 'Create Your Account'}
          </h2>
          <p className="text-xs text-muted-foreground">
            {step === 1 && 'Step 1 of 3 • Choose Account Role'}
            {step === 2 && 'Step 2 of 3 • Account Details & Password'}
            {step === 3 && 'Step 3 of 3 • Secure Email OTP Verification'}
          </p>
        </div>

        {/* Step Indicator Bar */}
        <div className="flex gap-2">
          <div className={`h-1.5 flex-1 rounded-full transition-all duration-300 ${step >= 1 ? 'bg-brand-blue' : 'bg-muted'}`} />
          <div className={`h-1.5 flex-1 rounded-full transition-all duration-300 ${step >= 2 ? 'bg-brand-blue' : 'bg-muted'}`} />
          <div className={`h-1.5 flex-1 rounded-full transition-all duration-300 ${step >= 3 ? 'bg-brand-blue' : 'bg-muted'}`} />
        </div>

        {/* Success Notice */}
        {resendSuccess && (
          <div className="p-3.5 rounded-xl bg-emerald-500/10 border border-emerald-500/20 text-emerald-500 text-xs font-medium flex items-start gap-2.5">
            <CheckCircle2 className="w-4 h-4 shrink-0 mt-0.5" />
            <span>{resendSuccess}</span>
          </div>
        )}

        {/* Error Notice */}
        {errorMsg && (
          <div className="p-3.5 rounded-xl bg-red-500/10 border border-red-500/20 text-red-500 text-xs font-medium flex items-start gap-2.5">
            <AlertCircle className="w-4 h-4 shrink-0 mt-0.5" />
            <span>{errorMsg}</span>
          </div>
        )}

        {step < 3 ? (
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
                    {role === 'recruiter' ? 'Work Email' : 'Email Address'}
                  </label>
                  <Input
                    type="email"
                    required
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    placeholder={role === 'recruiter' ? 'recruiter@company.com' : 'alex@example.com'}
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
                      placeholder="e.g. Vionsys Technologies"
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
                    Verify Email <ArrowRight className="w-4 h-4 ml-1" />
                  </Button>
                </div>
              </>
            )}
          </form>
        ) : (
          /* Step 3: 6-Digit OTP Verification Screen */
          <form onSubmit={handleVerifyOtp} className="space-y-6">
            <div className="text-center space-y-2">
              <div className="w-12 h-12 rounded-2xl bg-brand-blue/10 border border-brand-blue/20 text-brand-blue mx-auto flex items-center justify-center">
                <ShieldCheck className="w-6 h-6 text-brand-accent" />
              </div>
              <p className="text-xs text-muted-foreground leading-relaxed px-2">
                We sent a 6-digit security code to{' '}
                <span className="font-semibold text-foreground underline">{email}</span>. Enter the code below to complete activation.
              </p>
            </div>

            {/* 6-box OTP Input */}
            <div className="flex justify-center gap-2 sm:gap-3 my-4">
              {otp.map((digit, idx) => (
                <input
                  key={idx}
                  ref={(el) => (otpInputs.current[idx] = el)}
                  type="text"
                  inputMode="numeric"
                  maxLength={1}
                  value={digit}
                  onChange={(e) => handleOtpChange(idx, e.target.value)}
                  onKeyDown={(e) => handleOtpKeyDown(idx, e)}
                  className="w-11 h-13 sm:w-12 sm:h-14 text-center text-xl font-mono font-bold rounded-xl bg-surface-2 border border-border focus:border-brand-accent focus:ring-2 focus:ring-brand-accent/20 outline-none transition-all text-foreground shadow-sm"
                  autoFocus={idx === 0}
                />
              ))}
            </div>

            <Button
              type="submit"
              variant="gradient"
              size="lg"
              isLoading={isLoading}
              className="w-full font-bold shadow-xl py-3"
            >
              <KeyRound className="w-4 h-4 mr-2" />
              Activate Account
            </Button>

            <div className="flex flex-col sm:flex-row items-center justify-between gap-2 pt-2 text-xs">
              <button
                type="button"
                onClick={() => setStep(2)}
                className="text-muted-foreground hover:text-foreground inline-flex items-center gap-1 transition-colors"
              >
                <ArrowLeft className="w-3.5 h-3.5" /> Edit email address
              </button>

              <button
                type="button"
                onClick={handleResend}
                disabled={resendCooldown > 0 || isLoading}
                className={`font-semibold inline-flex items-center gap-1 transition-colors ${
                  resendCooldown > 0
                    ? 'text-muted-foreground cursor-not-allowed opacity-60'
                    : 'text-brand-accent hover:underline'
                }`}
              >
                <RefreshCw className={`w-3.5 h-3.5 ${isLoading ? 'animate-spin' : ''}`} />
                {resendCooldown > 0 ? `Resend code (${resendCooldown}s)` : 'Resend code'}
              </button>
            </div>
          </form>
        )}

        {step < 3 && (
          <OAuthButtons role={role} companyName={companyName} onError={(err) => setErrorMsg(err)} />
        )}

        <div className="text-center pt-2 text-xs text-muted-foreground">
          Already have an account?{' '}
          <Link to={ROUTES.LOGIN} state={{ role }} className="font-bold text-[#F56681] hover:underline">
            Sign In
          </Link>
        </div>
      </Card>
    </div>
  );
};
