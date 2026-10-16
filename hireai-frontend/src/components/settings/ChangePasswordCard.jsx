import React, { useState } from 'react';
import { KeyRound, Lock, CheckCircle2, AlertCircle, ShieldCheck } from 'lucide-react';
import { Card } from '@/components/ui/Card';
import { Input } from '@/components/ui/Input';
import { Button } from '@/components/ui/Button';
import { authApi } from '@/services/api/auth.api';

export const ChangePasswordCard = () => {
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [successMessage, setSuccessMessage] = useState('');
  const [errorMessage, setErrorMessage] = useState('');

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErrorMessage('');
    setSuccessMessage('');

    // Client-side validations
    if (!currentPassword) {
      setErrorMessage('Current password is required.');
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
    if (newPassword === currentPassword) {
      setErrorMessage('New password cannot be the same as your current password.');
      return;
    }
    if (newPassword !== confirmPassword) {
      setErrorMessage('New password and confirm password do not match.');
      return;
    }

    setIsLoading(true);
    try {
      await authApi.changePassword({
        currentPassword,
        newPassword,
        confirmPassword,
      });

      setSuccessMessage('Password changed successfully. Active refresh sessions have been updated.');
      setCurrentPassword('');
      setNewPassword('');
      setConfirmPassword('');
    } catch (err) {
      console.error('Password change error:', err);
      const msg = err.message || 'Failed to update password. Please verify your current password.';
      setErrorMessage(msg);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <Card glass className="p-6 sm:p-8 space-y-6">
      <div className="flex items-center gap-3 pb-4 border-b border-border/50">
        <div className="w-10 h-10 rounded-xl bg-brand-blue/10 border border-brand-blue/20 flex items-center justify-center text-brand-blue shrink-0">
          <KeyRound className="w-5 h-5" />
        </div>
        <div>
          <h3 className="text-lg font-bold font-heading text-foreground">Change Password</h3>
          <p className="text-xs text-muted-foreground">
            Update your account password to maintain security. Active sessions will be refreshed.
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

      <form onSubmit={handleSubmit} className="space-y-4 max-w-xl">
        <div className="space-y-1.5">
          <label className="text-xs font-bold text-foreground">Current Password</label>
          <Input
            type="password"
            placeholder="Enter your existing password"
            value={currentPassword}
            onChange={(e) => setCurrentPassword(e.target.value)}
            disabled={isLoading}
            icon={<Lock className="w-4 h-4 text-muted-foreground" />}
            autoComplete="current-password"
          />
        </div>

        <div className="space-y-1.5">
          <label className="text-xs font-bold text-foreground">New Password</label>
          <Input
            type="password"
            placeholder="Minimum 8 characters with letters and numbers"
            value={newPassword}
            onChange={(e) => setNewPassword(e.target.value)}
            disabled={isLoading}
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
            disabled={isLoading}
            icon={<Lock className="w-4 h-4 text-muted-foreground" />}
            autoComplete="new-password"
          />
        </div>

        {/* Password Strength Checklist */}
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
            <li className={newPassword && currentPassword && newPassword !== currentPassword ? 'text-emerald-500 dark:text-emerald-400 font-medium' : ''}>
              • Different from current
            </li>
          </ul>
        </div>

        <div className="pt-2">
          <Button
            type="submit"
            isLoading={isLoading}
            disabled={isLoading || !currentPassword || !newPassword || !confirmPassword}
            className="rounded-xl px-5"
          >
            Update Password
          </Button>
        </div>
      </form>
    </Card>
  );
};
