import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { AlertTriangle, Trash2, Lock, Mail, ShieldAlert, RefreshCw, AlertCircle } from 'lucide-react';
import { Card } from '@/components/ui/Card';
import { Input } from '@/components/ui/Input';
import { Button } from '@/components/ui/Button';
import { Modal } from '@/components/ui/Modal';
import { authApi } from '@/services/api/auth.api';
import { useAuth } from '@/context/AuthContext';
import { clearAuthSession } from '@/services/api/apiClient';

export const DeleteAccountCard = () => {
  const { user, role } = useAuth();
  const navigate = useNavigate();

  const [isModalOpen, setIsModalOpen] = useState(false);
  const [currentPassword, setCurrentPassword] = useState('');
  const [otp, setOtp] = useState('');
  const [confirmationText, setConfirmationText] = useState('');
  const [codeSent, setCodeSent] = useState(false);
  const [isSendingCode, setIsSendingCode] = useState(false);
  const [isDeleting, setIsDeleting] = useState(false);
  const [cooldown, setCooldown] = useState(0);
  const [errorMessage, setErrorMessage] = useState('');

  const hasPassword = user?.hasPassword !== false;
  const isRecruiter = role === 'recruiter';

  // Cooldown timer for OTP resend
  useEffect(() => {
    if (cooldown <= 0) return;
    const interval = setInterval(() => {
      setCooldown((prev) => (prev > 0 ? prev - 1 : 0));
    }, 1000);
    return () => clearInterval(interval);
  }, [cooldown]);

  const handleOpenModal = () => {
    setErrorMessage('');
    setCurrentPassword('');
    setOtp('');
    setConfirmationText('');
    setCodeSent(false);
    setIsModalOpen(true);
  };

  const handleCloseModal = () => {
    if (isDeleting) return;
    setIsModalOpen(false);
    setErrorMessage('');
    setCurrentPassword('');
    setOtp('');
    setConfirmationText('');
  };

  const handleSendCode = async () => {
    setErrorMessage('');
    setIsSendingCode(true);

    try {
      await authApi.requestDeleteAccountOtp();
      setCodeSent(true);
      setCooldown(60);
    } catch (err) {
      console.error('Request delete account OTP failed:', err);
      const msg = err.message || 'Failed to dispatch verification code. Please try again.';
      setErrorMessage(msg);
    } finally {
      setIsSendingCode(false);
    }
  };

  const isFormValid = () => {
    if (confirmationText.trim() !== 'DELETE') {
      return false;
    }
    if (hasPassword) {
      return Boolean(currentPassword && currentPassword.trim().length > 0);
    }
    return Boolean(otp && otp.trim().length === 6);
  };

  const handleDeleteConfirm = async (e) => {
    e.preventDefault();
    if (!isFormValid()) return;

    setErrorMessage('');
    setIsDeleting(true);

    try {
      await authApi.deleteAccount({
        currentPassword: hasPassword ? currentPassword : null,
        otp: !hasPassword ? otp.trim() : null,
        confirmationText: confirmationText.trim(),
      });

      // Clear all local session tokens and close connections
      clearAuthSession();
      window.dispatchEvent(new Event('hireai-logout'));

      // Redirect to login page with feedback message
      navigate('/login', {
        state: { message: 'Your account has been deleted successfully.' },
        replace: true,
      });
    } catch (err) {
      console.error('Account deletion failed:', err);
      const msg = err.message || 'Failed to delete account. Please verify your credentials and try again.';
      setErrorMessage(msg);
      setIsDeleting(false);
    }
  };

  return (
    <>
      <Card
        glass
        className="p-6 sm:p-8 space-y-6 border-red-500/30 bg-red-500/[0.02] dark:bg-red-950/[0.08]"
      >
        <div className="flex items-center gap-3 pb-4 border-b border-red-500/20">
          <div className="w-10 h-10 rounded-xl bg-red-500/10 border border-red-500/20 flex items-center justify-center text-red-500 shrink-0">
            <AlertTriangle className="w-5 h-5" />
          </div>
          <div>
            <h3 className="text-lg font-bold font-heading text-red-600 dark:text-red-400">Danger Zone</h3>
            <p className="text-xs text-muted-foreground">Irreversible and destructive account operations</p>
          </div>
        </div>

        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 py-1">
          <div className="space-y-1 max-w-xl">
            <h4 className="text-sm font-bold text-foreground">Delete HireAI Account</h4>
            <p className="text-xs text-muted-foreground leading-relaxed">
              {isRecruiter
                ? 'Permanently delete your recruiter account. All active job postings will be closed immediately, company branding cleared, and recruiter team privileges revoked. This action cannot be undone.'
                : 'Permanently delete your candidate profile. All resume files, profile images, and personal data will be purged. Your past applications will be anonymized for employer audit compliance. This action cannot be undone.'}
            </p>
          </div>
          <Button
            type="button"
            variant="danger"
            onClick={handleOpenModal}
            className="shrink-0 rounded-xl font-semibold px-5 py-2.5"
          >
            <Trash2 className="w-4 h-4 mr-2" />
            Delete Account
          </Button>
        </div>
      </Card>

      {/* Confirmation Modal */}
      <Modal
        isOpen={isModalOpen}
        onClose={handleCloseModal}
        title="Delete Account Confirmation"
        description="This action is permanent and cannot be reversed."
        maxWidth="md"
      >
        <form onSubmit={handleDeleteConfirm} className="space-y-5 pt-2">
          {/* Warning Banner */}
          <div className="p-4 rounded-xl bg-red-500/10 border border-red-500/25 text-red-600 dark:text-red-400 text-xs space-y-1.5 leading-relaxed">
            <div className="flex items-center gap-2 font-bold text-sm">
              <ShieldAlert className="w-4 h-4 shrink-0 text-red-500" />
              <span>Warning: Destructive Action</span>
            </div>
            <p>
              {isRecruiter
                ? 'Deleting your account will immediately close all active job listings you manage and revoke administrative dashboard access. Any pending applicants will see the positions as closed.'
                : 'Deleting your account will delete your uploaded resume, clear personal contact information, and close your profile. Employer records for past submitted applications will be anonymized.'}
            </p>
          </div>

          {/* Password confirmation for password users */}
          {hasPassword ? (
            <div className="space-y-1.5">
              <label className="text-xs font-semibold text-foreground flex items-center gap-1.5">
                <Lock className="w-3.5 h-3.5 text-brand-blue" />
                Current Password <span className="text-red-500">*</span>
              </label>
              <Input
                type="password"
                placeholder="Enter your current password"
                value={currentPassword}
                onChange={(e) => setCurrentPassword(e.target.value)}
                required
                disabled={isDeleting}
              />
            </div>
          ) : (
            /* OTP confirmation for OAuth-only users */
            <div className="space-y-3 p-4 rounded-xl bg-surface-2/60 border border-border/60">
              <div className="space-y-1">
                <div className="flex items-center gap-2 text-xs font-bold text-foreground">
                  <Mail className="w-4 h-4 text-brand-blue" />
                  <span>Email Verification Required</span>
                </div>
                <p className="text-xs text-muted-foreground">
                  Because you registered via {user?.authProvider || 'Google/LinkedIn'}, we must verify your identity with a 6-digit confirmation code sent to {user?.email || 'your email'}.
                </p>
              </div>

              {!codeSent ? (
                <Button
                  type="button"
                  variant="outline"
                  onClick={handleSendCode}
                  isLoading={isSendingCode}
                  className="w-full rounded-xl text-xs"
                >
                  <Mail className="w-4 h-4 mr-2 text-brand-blue" />
                  Send Verification Code
                </Button>
              ) : (
                <div className="space-y-2">
                  <div className="flex items-center justify-between text-xs">
                    <span className="font-semibold text-foreground">Enter 6-Digit Code</span>
                    <button
                      type="button"
                      onClick={handleSendCode}
                      disabled={cooldown > 0 || isSendingCode}
                      className="text-xs font-semibold text-brand-blue hover:underline disabled:opacity-50 disabled:no-underline cursor-pointer flex items-center gap-1"
                    >
                      <RefreshCw className={`w-3 h-3 ${isSendingCode ? 'animate-spin' : ''}`} />
                      {cooldown > 0 ? `Resend code in ${cooldown}s` : 'Resend code'}
                    </button>
                  </div>
                  <Input
                    type="text"
                    maxLength={6}
                    placeholder="123456"
                    value={otp}
                    onChange={(e) => setOtp(e.target.value.replace(/\D/g, ''))}
                    className="text-center font-mono tracking-widest text-lg h-11"
                    disabled={isDeleting}
                    required
                  />
                </div>
              )}
            </div>
          )}

          {/* DELETE confirmation phrase */}
          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-foreground">
              To verify, type <span className="font-mono font-bold text-red-500">DELETE</span> below <span className="text-red-500">*</span>
            </label>
            <Input
              type="text"
              placeholder="DELETE"
              value={confirmationText}
              onChange={(e) => setConfirmationText(e.target.value)}
              disabled={isDeleting}
              required
            />
          </div>

          {/* Error Message */}
          {errorMessage && (
            <div className="p-3 rounded-xl bg-red-500/10 border border-red-500/20 text-red-500 text-xs flex items-start gap-2">
              <AlertCircle className="w-4 h-4 shrink-0 mt-0.5" />
              <span>{errorMessage}</span>
            </div>
          )}

          {/* Actions */}
          <div className="flex items-center justify-end gap-3 pt-3 border-t border-border/50">
            <Button
              type="button"
              variant="outline"
              onClick={handleCloseModal}
              disabled={isDeleting}
              className="rounded-xl px-4 text-xs font-semibold"
            >
              Cancel
            </Button>
            <Button
              type="submit"
              variant="danger"
              isLoading={isDeleting}
              disabled={!isFormValid() || isDeleting}
              className="rounded-xl px-5 text-xs font-semibold"
            >
              <Trash2 className="w-4 h-4 mr-1.5" />
              Confirm Deletion
            </Button>
          </div>
        </form>
      </Modal>
    </>
  );
};
