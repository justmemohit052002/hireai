import React, { useState, useEffect } from 'react';
import { KeyRound, Lock, CheckCircle2, AlertCircle, ShieldCheck, Mail, ArrowRight, RefreshCw, Info } from 'lucide-react';
import { Card } from '@/components/ui/Card';
import { Input } from '@/components/ui/Input';
import { Button } from '@/components/ui/Button';
import { authApi } from '@/services/api/auth.api';
import { useAuth } from '@/context/AuthContext';

export const SetPasswordCard = () => {
  const { user, updateUserPasswordStatus } = useAuth();
  const [otp, setOtp] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [codeSent, setCodeSent] = useState(false);
  const [isSendingCode, setIsSendingCode] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [cooldown, setCooldown] = useState(0);
  const [successMessage, setSuccessMessage] = useState('');
  const [errorMessage, setErrorMessage] = useState('');

  // Provider display name
  const providerRaw = user?.authProvider || 'GOOGLE';
  const providerName = providerRaw.toUpperCase() === 'GOOGLE' 
    ? 'Google' 
    : providerRaw.toUpperCase() === 'LINKEDIN' 
      ? 'LinkedIn' 
      : 'Google/LinkedIn';

  // Cooldown countdown timer
  useEffect(() => {
    if (cooldown <= 0) return;
    const interval = setInterval(() => {
      setCooldown((prev) => (prev > 0 ? prev - 1 : 0));
    }, 1000);
    return () => clearInterval(interval);
  }, [cooldown]);

  const handleSendCode = async () => {
    setErrorMessage('');
    setSuccessMessage('');
    setIsSendingCode(true);

    try {
      await authApi.requestSetPasswordOtp();
      setCodeSent(true);
      setCooldown(60);
      setSuccessMessage(`A 6-digit verification code has been sent to ${user?.email || 'your registered email'}.`);
    } catch (err) {
      console.error('Request set-password OTP failed:', err);
      const msg = err.message || 'Failed to send verification code. Please try again.';
      setErrorMessage(msg);
    } finally {
      setIsSendingCode(false);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErrorMessage('');
    setSuccessMessage('');

    const cleanOtp = otp.trim();
    if (!cleanOtp) {
      setErrorMessage('Verification code is required.');
      return;
    }
    if (cleanOtp.length !== 6 || !/^\d{6}$/.test(cleanOtp)) {
      setErrorMessage('Verification code must be exactly 6 digits.');
      return;
    }

    if (!newPassword) {
      setErrorMessage('New password is required.');
      return;
    }
    if (newPassword.length < 8) {
      setErrorMessage('New password must be at least 8 characters long.');
      return;
    }
    if (!/[A-Za-z]/.test(newPassword) || !/[0-9]/.test(newPassword)) {
      setErrorMessage('New password must contain both letters and numbers.');
      return;
    }
    if (newPassword !== confirmPassword) {
      setErrorMessage('New password and confirm password do not match.');
      return;
    }

    setIsSubmitting(true);
    try {
      await authApi.confirmSetPassword({
        otp: cleanOtp,
        newPassword,
        confirmPassword,
      });

      setSuccessMessage('Password successfully set. You can now log in using either email + password or your OAuth provider.');
      // Smoothly switch card to ChangePasswordCard
      setTimeout(() => {
        updateUserPasswordStatus(true);
      }, 1500);
    } catch (err) {
      console.error('Set password confirmation failed:', err);
      const msg = err.message || 'Failed to set password. Please check your verification code and try again.';
      setErrorMessage(msg);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <Card glass className="p-6 sm:p-8 space-y-6">
      <div className="flex items-center gap-3 pb-4 border-b border-border/50">
        <div className="w-10 h-10 rounded-xl bg-brand-blue/10 border border-brand-blue/20 flex items-center justify-center text-brand-blue shrink-0">
          <KeyRound className="w-5 h-5" />
        </div>
        <div>
          <h3 className="text-lg font-bold font-heading text-foreground">Set Account Password</h3>
          <p className="text-xs text-muted-foreground">
            Configure an account password to log in directly with your email address in addition to your OAuth provider.
          </p>
        </div>
      </div>

      {/* OAuth Info Banner */}
      <div className="p-4 rounded-xl bg-brand-blue/10 border border-brand-blue/20 flex items-start gap-3 text-xs text-foreground">
        <Info className="w-4 h-4 text-brand-blue shrink-0 mt-0.5" />
        <div className="space-y-1">
          <p className="font-semibold">
            You signed in with {providerName}. Set a password to also log in with your email.
          </p>
          <p className="text-muted-foreground text-[11px] leading-relaxed">
            Your OAuth authentication will remain active. Setting a password allows you to access HireAI using either your {providerName} account or standard email credentials.
          </p>
        </div>
      </div>

      {successMessage && (
        <div className="p-4 rounded-xl bg-emerald-500/10 border border-emerald-500/30 text-emerald-500 dark:text-emerald-400 text-xs font-medium flex items-start gap-2.5">
          <CheckCircle2 className="w-4 h-4 shrink-0 mt-0.5" />
          <span>{successMessage}</span>
        </div>
      )}

      {errorMessage && (
        <div className="p-4 rounded-xl bg-red-500/10 border border-red-500/30 text-red-500 dark:text-red-400 text-xs font-medium flex items-start gap-2.5">
          <AlertCircle className="w-4 h-4 shrink-0 mt-0.5" />
          <span>{errorMessage}</span>
        </div>
      )}

      {!codeSent ? (
        <div className="space-y-4 max-w-xl">
          <div className="p-4 rounded-xl bg-surface-2/60 border border-border/40 space-y-2">
            <div className="flex items-center gap-2 text-xs font-medium text-foreground">
              <Mail className="w-4 h-4 text-brand-blue" />
              <span>Registered Email:</span>
              <span className="font-semibold text-brand-blue">{user?.email}</span>
            </div>
            <p className="text-[11px] text-muted-foreground">
              To verify account ownership, a 6-digit confirmation code will be dispatched to your registered address.
            </p>
          </div>

          <Button
            type="button"
            onClick={handleSendCode}
            isLoading={isSendingCode}
            disabled={isSendingCode}
            className="rounded-xl px-5 flex items-center gap-2"
          >
            <span>Send Verification Code</span>
            <ArrowRight className="w-4 h-4" />
          </Button>
        </div>
      ) : (
        <form onSubmit={handleSubmit} className="space-y-4 max-w-xl">
          <div className="space-y-1.5">
            <div className="flex items-center justify-between">
              <label className="text-xs font-bold text-foreground">6-Digit Verification Code</label>
              <button
                type="button"
                onClick={handleSendCode}
                disabled={isSendingCode || cooldown > 0}
                className="text-xs text-brand-blue hover:text-brand-cyan disabled:text-muted-foreground font-medium flex items-center gap-1 transition-colors"
              >
                <RefreshCw className={`w-3 h-3 ${isSendingCode ? 'animate-spin' : ''}`} />
                {cooldown > 0 ? `Resend code in ${cooldown}s` : 'Resend code'}
              </button>
            </div>
            <Input
              type="text"
              placeholder="Enter 6-digit code from email"
              value={otp}
              onChange={(e) => setOtp(e.target.value.replace(/\D/g, '').slice(0, 6))}
              disabled={isSubmitting}
              maxLength={6}
              autoComplete="one-time-code"
            />
          </div>

          <div className="space-y-1.5">
            <label className="text-xs font-bold text-foreground">New Password</label>
            <Input
              type="password"
              placeholder="Minimum 8 characters with letters and numbers"
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
              disabled={isSubmitting}
              icon={<Lock className="w-4 h-4 text-muted-foreground" />}
              autoComplete="new-password"
            />
          </div>

          <div className="space-y-1.5">
            <label className="text-xs font-bold text-foreground">Confirm New Password</label>
            <Input
              type="password"
              placeholder="Re-enter your new password"
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              disabled={isSubmitting}
              icon={<Lock className="w-4 h-4 text-muted-foreground" />}
              autoComplete="new-password"
            />
          </div>

          {/* Password Requirements Checklist */}
          <div className="p-3.5 rounded-xl bg-surface-2/60 border border-border/40 text-xs space-y-1.5">
            <p className="font-semibold text-foreground flex items-center gap-1.5 text-[11px] uppercase tracking-wide">
              <ShieldCheck className="w-3.5 h-3.5 text-brand-blue" /> Password Requirements:
            </p>
            <ul className="grid grid-cols-1 sm:grid-cols-2 gap-1 text-[11px] text-muted-foreground">
              <li className={newPassword.length >= 8 ? 'text-emerald-500 dark:text-emerald-400 font-medium' : ''}>
                • At least 8 characters
              </li>
              <li className={/[A-Za-z]/.test(newPassword) && /[0-9]/.test(newPassword) ? 'text-emerald-500 dark:text-emerald-400 font-medium' : ''}>
                • Letters and numbers
              </li>
              <li className={confirmPassword && newPassword === confirmPassword ? 'text-emerald-500 dark:text-emerald-400 font-medium' : ''}>
                • Passwords match
              </li>
              <li className={otp.trim().length === 6 ? 'text-emerald-500 dark:text-emerald-400 font-medium' : ''}>
                • 6-digit verification code
              </li>
            </ul>
          </div>

          <div className="pt-2 flex items-center gap-3">
            <Button
              type="submit"
              isLoading={isSubmitting}
              disabled={isSubmitting || otp.trim().length !== 6 || !newPassword || !confirmPassword}
              className="rounded-xl px-5"
            >
              Set Password
            </Button>
          </div>
        </form>
      )}
    </Card>
  );
};
