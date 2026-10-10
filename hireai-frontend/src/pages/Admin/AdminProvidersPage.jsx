import React, { useState, useEffect } from 'react';
import {
  Key,
  Server,
  ShieldCheck,
  CheckCircle2,
  XCircle,
  AlertTriangle,
  RefreshCw,
  Lock,
  Cpu,
  Settings,
  HelpCircle,
} from 'lucide-react';
import { Card } from '@/components/ui/Card';
import { Badge } from '@/components/ui/Badge';
import { Button } from '@/components/ui/Button';
import { Skeleton } from '@/components/ui/Skeleton';
import { adminApi } from '@/services/api/admin.api';

export const AdminProvidersPage = () => {
  const [providersData, setProvidersData] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isRefreshing, setIsRefreshing] = useState(false);

  const fetchProviders = async (silent = false) => {
    try {
      if (!silent) setIsLoading(true);
      else setIsRefreshing(true);

      const data = await adminApi.getProviders();
      setProvidersData(data);
    } catch (err) {
      console.error('Failed to load provider configuration:', err);
    } finally {
      setIsLoading(false);
      setIsRefreshing(false);
    }
  };

  useEffect(() => {
    fetchProviders();
  }, []);

  if (isLoading) {
    return (
      <div className="space-y-6 max-w-7xl mx-auto pb-12">
        <Skeleton className="h-20 w-full rounded-3xl" />
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          {[1, 2, 3].map((i) => (
            <Skeleton key={i} className="h-64 rounded-3xl" />
          ))}
        </div>
      </div>
    );
  }

  const activeProvider = providersData?.activeProvider || 'ollama';
  const activeModel = providersData?.activeModel || 'llama3.2:3b';
  const ollama = providersData?.ollama || {};
  const gemini = providersData?.gemini || {};
  const groq = providersData?.groq || {};

  return (
    <div className="space-y-6 max-w-7xl mx-auto pb-16">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 p-5 sm:p-6 rounded-3xl glass border border-white/20 dark:border-white/10 shadow-xl">
        <div className="space-y-1">
          <h1 className="text-2xl font-bold font-heading text-foreground flex items-center gap-2.5">
            <Key className="w-6 h-6 text-brand-blue" />
            AI Provider & Key Governance
          </h1>
          <p className="text-xs sm:text-sm text-muted-foreground">
            Read-only configuration view of active AI providers, key presence indicators, and reliability timeouts.
          </p>
        </div>

        <Button
          variant="outline"
          size="sm"
          onClick={() => fetchProviders(true)}
          disabled={isRefreshing}
          className="gap-2 rounded-xl text-xs font-semibold self-start sm:self-auto cursor-pointer"
        >
          <RefreshCw className={`w-3.5 h-3.5 ${isRefreshing ? 'animate-spin text-brand-blue' : ''}`} />
          {isRefreshing ? 'Checking...' : 'Check Reachability'}
        </Button>
      </div>

      {/* Security Guarantee Notice */}
      <div className="p-4 rounded-2xl bg-indigo-500/10 border border-indigo-500/20 flex items-start gap-3 text-xs text-indigo-900 dark:text-indigo-200">
        <ShieldCheck className="w-5 h-5 text-indigo-500 shrink-0 mt-0.5" />
        <div className="space-y-0.5">
          <span className="font-bold text-indigo-600 dark:text-indigo-400">Strict Zero-Secret Exposure Policy</span>
          <p className="text-muted-foreground leading-relaxed">
            Per architectural guidelines, raw credentials and secret tokens are NEVER stored in or transmitted to the client application. Only reachability indicators and masked prefixes are rendered.
          </p>
        </div>
      </div>

      {/* 3 Provider Cards: Ollama, Gemini, Groq */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        {/* Card 1: Ollama Local */}
        <Card
          glass
          className={`p-6 rounded-3xl border shadow-xl space-y-4 relative overflow-hidden ${
            activeProvider === 'ollama' ? 'border-brand-blue/50 ring-2 ring-brand-blue/20' : 'border-white/20 dark:border-white/10'
          }`}
        >
          {activeProvider === 'ollama' && (
            <div className="absolute top-4 right-4">
              <Badge variant="outline" className="text-[10px] bg-emerald-500/10 text-emerald-500 border-emerald-500/20 font-bold">
                ACTIVE
              </Badge>
            </div>
          )}

          <div className="space-y-1">
            <span className="text-[10px] font-bold uppercase tracking-wider text-muted-foreground">Local Inference</span>
            <h2 className="text-lg font-bold text-foreground">Ollama Local Engine</h2>
          </div>

          <div className="space-y-3 pt-2 text-xs">
            <div className="flex items-center justify-between pb-2 border-b border-border/40">
              <span className="text-muted-foreground">Reachability:</span>
              <span
                className={`inline-flex items-center gap-1 font-bold ${
                  ollama.reachable ? 'text-emerald-500' : 'text-rose-500'
                }`}
              >
                {ollama.reachable ? <CheckCircle2 className="w-3.5 h-3.5" /> : <XCircle className="w-3.5 h-3.5" />}
                {ollama.reachable ? 'Reachable (Port 11434)' : 'Unreachable'}
              </span>
            </div>

            <div className="flex items-center justify-between pb-2 border-b border-border/40">
              <span className="text-muted-foreground">Default Model:</span>
              <span className="font-mono font-bold text-foreground">{ollama.model || 'llama3.2:3b'}</span>
            </div>

            <div className="flex items-center justify-between pb-2 border-b border-border/40">
              <span className="text-muted-foreground">Base Endpoint:</span>
              <span className="font-mono text-[11px] text-muted-foreground">{ollama.baseUrl || 'http://localhost:11434'}</span>
            </div>

            <div className="flex items-center justify-between">
              <span className="text-muted-foreground">Billing Cost:</span>
              <span className="font-bold text-emerald-500">Free ($0.00)</span>
            </div>
          </div>
        </Card>

        {/* Card 2: Google Gemini */}
        <Card
          glass
          className={`p-6 rounded-3xl border shadow-xl space-y-4 relative overflow-hidden ${
            activeProvider === 'gemini' ? 'border-brand-blue/50 ring-2 ring-brand-blue/20' : 'border-white/20 dark:border-white/10'
          }`}
        >
          {activeProvider === 'gemini' && (
            <div className="absolute top-4 right-4">
              <Badge variant="outline" className="text-[10px] bg-emerald-500/10 text-emerald-500 border-emerald-500/20 font-bold">
                ACTIVE
              </Badge>
            </div>
          )}

          <div className="space-y-1">
            <span className="text-[10px] font-bold uppercase tracking-wider text-muted-foreground">Cloud Provider</span>
            <h2 className="text-lg font-bold text-foreground">Google Gemini API</h2>
          </div>

          <div className="space-y-3 pt-2 text-xs">
            <div className="flex items-center justify-between pb-2 border-b border-border/40">
              <span className="text-muted-foreground">API Key Status:</span>
              <span
                className={`inline-flex items-center gap-1 font-bold ${
                  gemini.isConfigured ? 'text-emerald-500' : 'text-muted-foreground'
                }`}
              >
                {gemini.isConfigured ? <CheckCircle2 className="w-3.5 h-3.5" /> : <XCircle className="w-3.5 h-3.5" />}
                {gemini.isConfigured ? 'Configured' : 'Not Configured'}
              </span>
            </div>

            <div className="flex items-center justify-between pb-2 border-b border-border/40">
              <span className="text-muted-foreground">Key Fingerprint:</span>
              <span className="font-mono text-[11px] text-foreground">
                {gemini.keyMasked || 'Not Configured'}
              </span>
            </div>

            <div className="flex items-center justify-between pb-2 border-b border-border/40">
              <span className="text-muted-foreground">Target Model:</span>
              <span className="font-mono font-bold text-foreground">{gemini.model || 'gemini-1.5-flash'}</span>
            </div>

            <div className="flex items-center justify-between">
              <span className="text-muted-foreground">Quota Ceiling:</span>
              <span className="font-semibold text-foreground">250 RPM / Day</span>
            </div>
          </div>
        </Card>

        {/* Card 3: Groq Cloud API */}
        <Card
          glass
          className={`p-6 rounded-3xl border shadow-xl space-y-4 relative overflow-hidden ${
            activeProvider === 'groq' ? 'border-brand-blue/50 ring-2 ring-brand-blue/20' : 'border-white/20 dark:border-white/10'
          }`}
        >
          {activeProvider === 'groq' && (
            <div className="absolute top-4 right-4">
              <Badge variant="outline" className="text-[10px] bg-emerald-500/10 text-emerald-500 border-emerald-500/20 font-bold">
                ACTIVE
              </Badge>
            </div>
          )}

          <div className="space-y-1">
            <span className="text-[10px] font-bold uppercase tracking-wider text-muted-foreground">Cloud Provider</span>
            <h2 className="text-lg font-bold text-foreground">Groq Cloud API</h2>
          </div>

          <div className="space-y-3 pt-2 text-xs">
            <div className="flex items-center justify-between pb-2 border-b border-border/40">
              <span className="text-muted-foreground">API Key Status:</span>
              <span
                className={`inline-flex items-center gap-1 font-bold ${
                  groq.isConfigured ? 'text-emerald-500' : 'text-muted-foreground'
                }`}
              >
                {groq.isConfigured ? <CheckCircle2 className="w-3.5 h-3.5" /> : <XCircle className="w-3.5 h-3.5" />}
                {groq.isConfigured ? 'Configured' : 'Not Configured'}
              </span>
            </div>

            <div className="flex items-center justify-between pb-2 border-b border-border/40">
              <span className="text-muted-foreground">Key Fingerprint:</span>
              <span className="font-mono text-[11px] text-foreground">
                {groq.keyMasked || 'Not Configured'}
              </span>
            </div>

            <div className="flex items-center justify-between pb-2 border-b border-border/40">
              <span className="text-muted-foreground">Target Model:</span>
              <span className="font-mono font-bold text-foreground">{groq.model || 'openai/gpt-oss-20b'}</span>
            </div>

            <div className="flex items-center justify-between">
              <span className="text-muted-foreground">Speed Target:</span>
              <span className="font-semibold text-foreground">Ultra-Low Latency</span>
            </div>
          </div>
        </Card>
      </div>

      {/* Runtime Environment & Reliability Settings */}
      <Card glass className="p-6 rounded-3xl border border-white/20 dark:border-white/10 shadow-xl space-y-4">
        <div className="space-y-1 pb-3 border-b border-border/50">
          <h3 className="text-base font-bold font-heading text-foreground flex items-center gap-2">
            <Settings className="w-4 h-4 text-brand-blue" />
            Reliability & Fail-Safe Policies
          </h3>
          <p className="text-xs text-muted-foreground">
            Automatic retry with exponential backoff configuration for handling cold starts.
          </p>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 text-xs">
          <div className="p-4 rounded-2xl bg-surface-2/60 border border-white/10 space-y-1">
            <span className="text-muted-foreground text-[11px]">LLM Timeout Limit</span>
            <div className="text-lg font-bold text-foreground">240 seconds</div>
            <p className="text-[10px] text-muted-foreground">Allows deep resume processing on CPU</p>
          </div>

          <div className="p-4 rounded-2xl bg-surface-2/60 border border-white/10 space-y-1">
            <span className="text-muted-foreground text-[11px]">Max Automated Retries</span>
            <div className="text-lg font-bold text-foreground">2 retries (3 total attempts)</div>
            <p className="text-[10px] text-muted-foreground">Mitigates transient connection glitches</p>
          </div>

          <div className="p-4 rounded-2xl bg-surface-2/60 border border-white/10 space-y-1">
            <span className="text-muted-foreground text-[11px]">Backoff Multiplier</span>
            <div className="text-lg font-bold text-foreground">2.0s linear backoff</div>
            <p className="text-[10px] text-muted-foreground">Prevents request storms during load</p>
          </div>
        </div>
      </Card>
    </div>
  );
};
