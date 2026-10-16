import React from 'react';
import { Moon, Bell, Shield, Sliders } from 'lucide-react';
import { Card } from '@/components/ui/Card';
import { Switch } from '@/components/ui/Switch';
import { ThemeToggle } from '@/components/common/ThemeToggle';
import { ProfilePhotoCard } from '@/components/settings/ProfilePhotoCard';
import { ChangePasswordCard } from '@/components/settings/ChangePasswordCard';
import { SetPasswordCard } from '@/components/settings/SetPasswordCard';
import { DeleteAccountCard } from '@/components/settings/DeleteAccountCard';
import { useAuth } from '@/context/AuthContext';

export const RecruiterSettingsPage = () => {
  const { user } = useAuth();
  const hasPassword = user?.hasPassword !== false;

  return (
    <div className="max-w-3xl mx-auto space-y-6 pb-12">
      <div className="space-y-1">
        <h1 className="text-2xl font-bold font-heading text-foreground">Recruiter Settings</h1>
        <p className="text-xs text-muted-foreground">
          Manage employer company branding, password security, and automated candidate digest preferences.
        </p>
      </div>

      {/* Profile Photo / Company Logo Section */}
      <ProfilePhotoCard />

      {/* Password Management Section: Set Password for OAuth users, Change Password for password users */}
      {hasPassword ? <ChangePasswordCard /> : <SetPasswordCard />}

      {/* Studio Preferences Card */}
      <Card glass className="p-6 sm:p-8 space-y-6">
        <div className="flex items-center gap-3 pb-4 border-b border-border/50">
          <div className="w-10 h-10 rounded-xl bg-brand-blue/10 border border-brand-blue/20 flex items-center justify-center text-brand-blue shrink-0">
            <Sliders className="w-5 h-5" />
          </div>
          <div>
            <h3 className="text-lg font-bold font-heading text-foreground">Studio Preferences</h3>
            <p className="text-xs text-muted-foreground">Configure AI workflows and alerting triggers.</p>
          </div>
        </div>

        <div className="flex items-center justify-between py-3 border-b border-border/40">
          <div className="space-y-0.5">
            <h4 className="text-sm font-bold text-foreground flex items-center gap-2">
              <Moon className="w-4 h-4 text-brand-blue" /> Interface Theme
            </h4>
            <p className="text-xs text-muted-foreground">Toggle between high-contrast dark and light modes</p>
          </div>
          <ThemeToggle />
        </div>

        <div className="flex items-center justify-between py-3 border-b border-border/40">
          <div className="space-y-0.5">
            <h4 className="text-sm font-bold text-foreground flex items-center gap-2">
              <Bell className="w-4 h-4 text-brand-blue" /> Candidate Application Digests
            </h4>
            <p className="text-xs text-muted-foreground">Receive instant alerts when a 90%+ match applicant applies</p>
          </div>
          <Switch defaultChecked />
        </div>

        <div className="flex items-center justify-between py-3">
          <div className="space-y-0.5">
            <h4 className="text-sm font-bold text-foreground flex items-center gap-2">
              <Shield className="w-4 h-4 text-emerald-500" /> AI Pre-Screening Automation
            </h4>
            <p className="text-xs text-muted-foreground">Automatically pre-score and tag incoming resumes against job specs</p>
          </div>
          <Switch defaultChecked />
        </div>
      </Card>

      {/* Danger Zone: Permanent Account Deletion */}
      <DeleteAccountCard />
    </div>
  );
};
