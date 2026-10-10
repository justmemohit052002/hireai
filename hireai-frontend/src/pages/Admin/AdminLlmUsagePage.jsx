import React, { useState, useEffect } from 'react';
import {
  Cpu,
  Clock,
  Zap,
  BarChart3,
  PieChart,
  RefreshCw,
  AlertCircle,
  CheckCircle2,
  TrendingUp,
  ShieldCheck,
  Server,
  Layers,
} from 'lucide-react';
import { Card } from '@/components/ui/Card';
import { Badge } from '@/components/ui/Badge';
import { Button } from '@/components/ui/Button';
import { Skeleton } from '@/components/ui/Skeleton';
import { adminApi } from '@/services/api/admin.api';

export const AdminLlmUsagePage = () => {
  const [stats, setStats] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isRefreshing, setIsRefreshing] = useState(false);

  const fetchStats = async (silent = false) => {
    try {
      if (!silent) setIsLoading(true);
      else setIsRefreshing(true);

      const data = await adminApi.getStats();
      setStats(data);
    } catch (err) {
      console.error('Failed to fetch LLM stats:', err);
    } finally {
      setIsLoading(false);
      setIsRefreshing(false);
    }
  };

  useEffect(() => {
    fetchStats();
  }, []);

  if (isLoading) {
    return (
      <div className="space-y-6 max-w-7xl mx-auto pb-12">
        <Skeleton className="h-20 w-full rounded-3xl" />
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
          {[1, 2, 3, 4].map((i) => (
            <Skeleton key={i} className="h-32 rounded-2xl" />
          ))}
        </div>
        <Skeleton className="h-96 w-full rounded-3xl" />
      </div>
    );
  }

  const quota = stats?.quota || { dailyLimit: 250, usedToday: 0, remainingToday: 250, percentageUsed: 0 };
  const moduleBreakdown = stats?.moduleBreakdown || [];
  const providerDistribution = stats?.providerDistribution || [];

  return (
    <div className="space-y-6 max-w-7xl mx-auto pb-16">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 p-5 sm:p-6 rounded-3xl glass border border-white/20 dark:border-white/10 shadow-xl">
        <div className="space-y-1">
          <h1 className="text-2xl font-bold font-heading text-foreground flex items-center gap-2.5">
            <Cpu className="w-6 h-6 text-brand-blue" />
            LLM Usage & Cost Governance
          </h1>
          <p className="text-xs sm:text-sm text-muted-foreground">
            Granular breakdown of AI invocation volumes, provider splits, latency metrics, and API quotas.
          </p>
        </div>

        <Button
          variant="outline"
          size="sm"
          onClick={() => fetchStats(true)}
          disabled={isRefreshing}
          className="gap-2 rounded-xl text-xs font-semibold self-start sm:self-auto cursor-pointer"
        >
          <RefreshCw className={`w-3.5 h-3.5 ${isRefreshing ? 'animate-spin text-brand-blue' : ''}`} />
          {isRefreshing ? 'Refreshing...' : 'Refresh Stats'}
        </Button>
      </div>

      {/* 4 Summary Stats */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <Card glass className="p-5 rounded-2xl border border-white/20 dark:border-white/10 shadow-md space-y-2">
          <span className="text-[11px] font-bold text-muted-foreground uppercase tracking-wider">All-Time Calls</span>
          <div className="text-3xl font-black text-foreground">{stats?.totalCalls || 0}</div>
          <p className="text-xs text-muted-foreground">Cumulative log entries</p>
        </Card>

        <Card glass className="p-5 rounded-2xl border border-white/20 dark:border-white/10 shadow-md space-y-2">
          <span className="text-[11px] font-bold text-muted-foreground uppercase tracking-wider">Today's Invocations</span>
          <div className="text-3xl font-black text-foreground text-brand-blue">{stats?.todayCalls || 0}</div>
          <p className="text-xs text-muted-foreground">{stats?.weekCalls || 0} calls past 7 days</p>
        </Card>

        <Card glass className="p-5 rounded-2xl border border-white/20 dark:border-white/10 shadow-md space-y-2">
          <span className="text-[11px] font-bold text-muted-foreground uppercase tracking-wider">Mean Response Latency</span>
          <div className="text-3xl font-black text-foreground">{stats?.avgLatencySeconds || 0}s</div>
          <p className="text-xs text-muted-foreground">Across all providers & modules</p>
        </Card>

        <Card glass className="p-5 rounded-2xl border border-white/20 dark:border-white/10 shadow-md space-y-2">
          <span className="text-[11px] font-bold text-muted-foreground uppercase tracking-wider">Reliability Score</span>
          <div className="text-3xl font-black text-emerald-500">{stats?.successRate || 100}%</div>
          <p className="text-xs text-muted-foreground">{stats?.failedCalls || 0} failed calls recorded</p>
        </Card>
      </div>

      {/* Quota & Cost Control Card */}
      <Card glass className="p-6 rounded-3xl border border-white/20 dark:border-white/10 shadow-xl space-y-5">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 pb-4 border-b border-border/50">
          <div>
            <h2 className="text-base font-bold font-heading text-foreground flex items-center gap-2">
              <Zap className="w-4 h-4 text-brand-blue" />
              API Quota & Free Tier Throttle Protection
            </h2>
            <p className="text-xs text-muted-foreground mt-0.5">
              Protects against unexpected rate limits on cloud providers (Gemini / Groq)
            </p>
          </div>
          <Badge variant="outline" className="text-xs bg-brand-blue/10 text-brand-blue border-brand-blue/20 font-bold self-start">
            Gemini Free Tier
          </Badge>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 items-center">
          <div className="space-y-2">
            <span className="text-xs font-semibold text-muted-foreground">Daily Quota Consumption</span>
            <div className="text-2xl font-black text-foreground">
              {quota.usedToday} <span className="text-sm font-normal text-muted-foreground">/ {quota.dailyLimit} requests</span>
            </div>
            <p className="text-xs text-muted-foreground">
              {quota.remainingToday} requests remaining before 24h reset
            </p>
          </div>

          <div className="md:col-span-2 space-y-2">
            <div className="flex justify-between text-xs font-semibold">
              <span className="text-foreground">Quota Utilization</span>
              <span className="font-mono text-brand-blue">{quota.percentageUsed}%</span>
            </div>
            <div className="h-3 w-full rounded-full bg-muted/70 overflow-hidden">
              <div
                className={`h-full rounded-full transition-all ${
                  quota.percentageUsed > 80 ? 'bg-rose-500' : quota.percentageUsed > 50 ? 'bg-amber-500' : 'bg-brand-blue'
                }`}
                style={{ width: `${Math.min(100, Math.max(2, quota.percentageUsed))}%` }}
              />
            </div>
            <div className="flex justify-between text-[10px] text-muted-foreground">
              <span>0% (Safe)</span>
              <span>50% (Moderate)</span>
              <span>100% (Throttled)</span>
            </div>
          </div>
        </div>
      </Card>

      {/* Module Breakdown & Provider Distribution */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Module Volume & Latency Table */}
        <Card glass className="p-6 rounded-3xl border border-white/20 dark:border-white/10 shadow-xl space-y-4">
          <div className="space-y-1">
            <h3 className="text-base font-bold font-heading text-foreground flex items-center gap-2">
              <Layers className="w-4 h-4 text-brand-blue" />
              Volume Breakdown by AI Module
            </h3>
            <p className="text-xs text-muted-foreground">
              Call counts and average processing duration per task
            </p>
          </div>

          {moduleBreakdown.length === 0 ? (
            <div className="text-center py-10 text-xs text-muted-foreground">
              No module statistics recorded yet.
            </div>
          ) : (
            <div className="space-y-4 pt-2">
              {moduleBreakdown.map((item) => (
                <div key={item.module} className="space-y-1.5 p-3 rounded-2xl bg-surface-2/60 border border-white/10">
                  <div className="flex items-center justify-between text-xs">
                    <span className="font-bold font-mono text-foreground">{item.module}</span>
                    <span className="font-semibold text-muted-foreground">
                      {item.callCount} calls ({item.percentage}%)
                    </span>
                  </div>
                  <div className="h-2 w-full rounded-full bg-muted overflow-hidden">
                    <div className="h-full bg-brand-blue rounded-full" style={{ width: `${item.percentage}%` }} />
                  </div>
                  <div className="flex items-center justify-between text-[11px] text-muted-foreground pt-0.5">
                    <span>Avg Latency: <span className="font-mono text-foreground">{item.avgLatencySeconds}s</span></span>
                  </div>
                </div>
              ))}
            </div>
          )}
        </Card>

        {/* Provider Distribution Card */}
        <Card glass className="p-6 rounded-3xl border border-white/20 dark:border-white/10 shadow-xl space-y-4">
          <div className="space-y-1">
            <h3 className="text-base font-bold font-heading text-foreground flex items-center gap-2">
              <Server className="w-4 h-4 text-brand-blue" />
              Provider Traffic Share
            </h3>
            <p className="text-xs text-muted-foreground">
              Distribution between Local (Ollama) and Cloud (Gemini / Groq)
            </p>
          </div>

          {providerDistribution.length === 0 ? (
            <div className="text-center py-10 text-xs text-muted-foreground">
              No provider data available.
            </div>
          ) : (
            <div className="space-y-4 pt-2">
              {providerDistribution.map((p) => (
                <div key={p.provider} className="p-4 rounded-2xl bg-surface-2/60 border border-white/10 space-y-2">
                  <div className="flex items-center justify-between text-xs">
                    <span className="font-bold uppercase text-foreground">{p.provider}</span>
                    <span className="font-mono text-brand-blue font-bold">{p.percentage}%</span>
                  </div>
                  <div className="h-2 w-full rounded-full bg-muted overflow-hidden">
                    <div
                      className="h-full bg-emerald-500 rounded-full"
                      style={{ width: `${p.percentage}%` }}
                    />
                  </div>
                  <p className="text-[11px] text-muted-foreground">
                    Total Calls handled: <span className="font-bold text-foreground">{p.callCount}</span>
                  </p>
                </div>
              ))}

              <div className="p-3.5 rounded-2xl bg-muted/40 border border-border/40 text-[11px] text-muted-foreground space-y-1">
                <span className="font-semibold text-foreground flex items-center gap-1">
                  <ShieldCheck className="w-3.5 h-3.5 text-brand-blue" />
                  Cost Impact Note
                </span>
                <p>
                  Local Ollama execution incurs zero third-party API billing costs. Cloud LLMs are only invoked when configured in <code className="text-[10px] font-mono">.env</code>.
                </p>
              </div>
            </div>
          )}
        </Card>
      </div>
    </div>
  );
};
