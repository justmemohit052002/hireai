import React from 'react';
import { Moon, Bell, Shield } from 'lucide-react';
import { Card } from '@/components/ui/Card';
import { Switch } from '@/components/ui/Switch';
import { ThemeToggle } from '@/components/common/ThemeToggle';

export const RecruiterSettingsPage = () => {
  return (
    <div className="max-w-3xl mx-auto space-y-6">
      <Card surface-nested className="p-8 space-y-6">
        <h2 className="text-2xl font-bold font-heading text-foreground pb-4 border-b border-border/50">
          Recruiter Studio Settings
        </h2>

        <div className="flex items-center justify-between py-3 border-b border-border/40">
          <div className="space-y-0.5">
            <h4 className="text-sm font-bold text-foreground flex items-center gap-2">
              <Moon className="w-4 h-4 text-blue-500" /> Interface Theme
            </h4>
            <p className="text-xs text-muted-foreground">Toggle between high-contrast dark and light modes</p>
          </div>
          <ThemeToggle />
        </div>

        <div className="flex items-center justify-between py-3 border-b border-border/40">
          <div className="space-y-0.5">
            <h4 className="text-sm font-bold text-foreground flex items-center gap-2">
              <Bell className="w-4 h-4 text-purple-500" /> Candidate Application Digests
            </h4>
            <p className="text-xs text-muted-foreground">Receive instant alerts when a 90%+ match applicant applies</p>
          </div>
          <Switch defaultChecked />
        </div>

        <div className="flex items-center justify-between py-3 border-b border-border/40">
          <div className="space-y-0.5">
            <h4 className="text-sm font-bold text-foreground flex items-center gap-2">
              <Shield className="w-4 h-4 text-emerald-500" /> FastAPI AI Screening Automation
            </h4>
            <p className="text-xs text-muted-foreground">Automatically pre-score and tag incoming resumes against job specs</p>
          </div>
          <Switch defaultChecked />
        </div>
      </Card>
    </div>
  );
};
