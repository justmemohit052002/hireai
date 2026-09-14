import React from 'react';
import { Moon, Bell, Shield, Key } from 'lucide-react';
import { Card } from '@/components/ui/Card';
import { Switch } from '@/components/ui/Switch';
import { ThemeToggle } from '@/components/common/ThemeToggle';

export const CandidateSettingsPage = () => {
  return (
    <div className="max-w-3xl mx-auto space-y-6">
      <Card surface-nested className="p-8 space-y-6">
        <h2 className="text-2xl font-bold font-heading text-foreground pb-4 border-b border-border/50">
          Account Settings
        </h2>

        {/* Theme Settings */}
        <div className="flex items-center justify-between py-3 border-b border-border/40">
          <div className="space-y-0.5">
            <h4 className="text-sm font-bold text-foreground flex items-center gap-2">
              <Moon className="w-4 h-4 text-blue-500" /> Interface Appearance
            </h4>
            <p className="text-xs text-muted-foreground">Toggle between high-contrast dark and light modes</p>
          </div>
          <ThemeToggle />
        </div>

        {/* Notifications */}
        <div className="flex items-center justify-between py-3 border-b border-border/40">
          <div className="space-y-0.5">
            <h4 className="text-sm font-bold text-foreground flex items-center gap-2">
              <Bell className="w-4 h-4 text-purple-500" /> Email Notifications
            </h4>
            <p className="text-xs text-muted-foreground">Receive real-time AI match updates and application status alerts</p>
          </div>
          <Switch defaultChecked />
        </div>

        {/* Privacy */}
        <div className="flex items-center justify-between py-3 border-b border-border/40">
          <div className="space-y-0.5">
            <h4 className="text-sm font-bold text-foreground flex items-center gap-2">
              <Shield className="w-4 h-4 text-emerald-500" /> Profile Visibility
            </h4>
            <p className="text-xs text-muted-foreground">Allow recruiters to discover your profile on the Leaderboard</p>
          </div>
          <Switch defaultChecked />
        </div>
      </Card>
    </div>
  );
};
