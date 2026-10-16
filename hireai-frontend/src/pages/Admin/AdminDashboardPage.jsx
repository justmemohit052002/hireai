import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import {
  Activity,
  CheckCircle2,
  AlertTriangle,
  XCircle,
  Cpu,
  Clock,
  Users,
  Server,
  FileCode,
  ArrowUpRight,
  RefreshCw,
  Search,
  ExternalLink,
  ShieldCheck,
  ChevronRight,
  Database,
  Layers,
  Briefcase,
  FileText,
  Mail,
  Zap,
} from 'lucide-react';
import { Card } from '@/components/ui/Card';
import { Badge } from '@/components/ui/Badge';
import { Button } from '@/components/ui/Button';
import { Modal } from '@/components/ui/Modal';
import { Skeleton } from '@/components/ui/Skeleton';
import { adminApi } from '@/services/api/admin.api';
import { ROUTES } from '@/constants';
import { PlatformTimeSeriesChart } from '@/components/admin/PlatformTimeSeriesChart';

export const AdminDashboardPage = () => {
  const [selectedRange, setSelectedRange] = useState('7d');
  const [platformMetrics, setPlatformMetrics] = useState(null);
  const [healthData, setHealthData] = useState(null);
  const [recentLogs, setRecentLogs] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [error, setError] = useState(null);
  const [selectedLog, setSelectedLog] = useState(null);

  const loadDashboardData = async (silent = false, range = selectedRange) => {
    try {
      if (!silent) setIsLoading(true);
      else setIsRefreshing(true);
      setError(null);

      const [metricsRes, healthRes, logsRes] = await Promise.all([
        adminApi.getPlatformMetrics(range).catch((err) => {
          console.warn('Platform metrics fetch failed, fallback to defaults:', err.message);
          return null;
        }),
        adminApi.getHealth().catch((err) => ({
          overallStatus: 'DEGRADED',
          error: err.message,
          modules: [],
        })),
        adminApi.getLogs({ page: 1, size: 8 }).catch(() => ({ items: [] })),
      ]);

      if (metricsRes) {
        setPlatformMetrics(metricsRes);
      }
      setHealthData(healthRes);
      setRecentLogs(logsRes?.items || []);
    } catch (err) {
      console.error('Failed to load admin dashboard data:', err);
      setError('Unable to fetch live system telemetry. Please verify AI Engine and Backend status.');
    } finally {
      setIsLoading(false);
      setIsRefreshing(false);
    }
  };

  useEffect(() => {
    loadDashboardData(false, selectedRange);
    // Background polling every 45 seconds (reads cached data without live AI hits)
    const timer = setInterval(() => loadDashboardData(true, selectedRange), 45000);
    return () => clearInterval(timer);
  }, [selectedRange]);

  const handleRangeChange = (newRange) => {
    setSelectedRange(newRange);
  };

  if (isLoading && !platformMetrics) {
    return (
      <div className="space-y-6 max-w-7xl mx-auto pb-12">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <Skeleton className="h-10 w-64 rounded-xl" />
          <Skeleton className="h-10 w-36 rounded-xl" />
        </div>
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          {[1, 2, 3, 4].map((i) => (
            <Skeleton key={i} className="h-28 rounded-2xl" />
          ))}
        </div>
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <Skeleton className="h-96 lg:col-span-2 rounded-2xl" />
          <Skeleton className="h-96 rounded-2xl" />
        </div>
      </div>
    );
  }

  // Derived Telemetry Values
  const overview = platformMetrics?.overview || {};
  const timeSeries = platformMetrics?.timeSeries || [];
  const aiPerf = platformMetrics?.aiPerformance || {};
  const sysHealth = platformMetrics?.systemHealth || {};
  const appPerf = platformMetrics?.appPerformance || {};
  const modules = sysHealth.modules?.length ? sysHealth.modules : (healthData?.modules || []);

  const overallStatus = sysHealth.aiEngineReachability || healthData?.overallStatus || 'UNKNOWN';
  const quota = aiPerf.geminiQuota || { dailyLimit: 250, usedToday: 0, percentageUsed: 0 };
  const totalCalls = aiPerf.totalCalls ?? 0;
  const avgLatency = aiPerf.avgLatencySeconds ?? 0;

  // Format uptime duration helper
  const formatDuration = (seconds) => {
    if (!seconds || seconds <= 0) return '0m';
    const d = Math.floor(seconds / 86400);
    const h = Math.floor((seconds % 86400) / 3600);
    const m = Math.floor((seconds % 3600) / 60);
    if (d > 0) return `${d}d ${h}h`;
    if (h > 0) return `${h}h ${m}m`;
    return `${m}m`;
  };

  const formatLastSeen = (timestamp) => {
    if (!timestamp) return 'Never';
    const diffSec = Math.max(0, Math.floor((Date.now() - new Date(timestamp).getTime()) / 1000));
    if (diffSec < 60) return `${diffSec}s ago`;
    const diffMin = Math.floor(diffSec / 60);
    if (diffMin < 60) return `${diffMin} min ago`;
    const diffHours = Math.floor(diffMin / 60);
    if (diffHours < 24) return `${diffHours}h ago`;
    return new Date(timestamp).toLocaleDateString();
  };

  return (
    <div className="space-y-6 max-w-7xl mx-auto pb-16">
      {/* Header Banner */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 p-5 sm:p-6 rounded-3xl glass border border-white/20 dark:border-white/10 shadow-xl relative overflow-hidden">
        <div className="absolute -right-12 -top-12 w-48 h-48 bg-brand-blue/10 dark:bg-brand-blue/20 rounded-full blur-3xl pointer-events-none" />
        <div className="space-y-1 relative z-10">
          <div className="flex items-center gap-2.5">
            <h1 className="text-2xl sm:text-3xl font-bold font-heading text-foreground">
              Admin Ops Center
            </h1>
            <Badge variant="outline" className="text-xs bg-brand-blue/10 text-brand-blue border-brand-blue/20 font-bold">
              Production Telemetry
            </Badge>
            {platformMetrics?.cached && (
              <Badge variant="outline" className="text-[10px] bg-slate-500/10 text-muted-foreground border-border font-mono">
                Cached Snapshot
              </Badge>
            )}
          </div>
          <p className="text-xs sm:text-sm text-muted-foreground">
            Platform health, AI engine metrics, user governance, and infrastructure reachability.
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-3 relative z-10">
          {/* Date Range Selector */}
          <div className="inline-flex rounded-xl bg-surface-2 p-1 border border-border">
            {['today', '7d', '30d'].map((rng) => (
              <button
                key={rng}
                onClick={() => handleRangeChange(rng)}
                className={`px-3 py-1 text-xs font-bold rounded-lg transition-all cursor-pointer ${
                  selectedRange === rng
                    ? 'bg-brand-blue text-white shadow-xs'
                    : 'text-muted-foreground hover:text-foreground'
                }`}
              >
                {rng === 'today' ? 'Today' : rng === '7d' ? '7 Days' : '30 Days'}
              </button>
            ))}
          </div>

          <Button
            variant="outline"
            size="sm"
            onClick={() => loadDashboardData(true, selectedRange)}
            disabled={isRefreshing}
            className="gap-2 rounded-xl text-xs font-semibold cursor-pointer border-white/20"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${isRefreshing ? 'animate-spin text-brand-blue' : ''}`} />
            {isRefreshing ? 'Polling...' : 'Sync Telemetry'}
          </Button>

          <Link to={ROUTES.ADMIN_USERS}>
            <Button size="sm" className="gap-2 rounded-xl text-xs font-bold shadow-md cursor-pointer">
              <Users className="w-3.5 h-3.5" /> Manage Users
            </Button>
          </Link>
        </div>
      </div>

      {/* Informative alert banner when engine is not fully healthy */}
      {overallStatus !== 'HEALTHY' && (
        <div className="flex items-start sm:items-center gap-3 p-4 rounded-2xl bg-amber-500/10 border border-amber-500/30 text-amber-700 dark:text-amber-300">
          <AlertTriangle className="w-5 h-5 shrink-0 text-amber-500 mt-0.5 sm:mt-0" />
          <div className="space-y-0.5">
            <p className="text-xs font-bold text-amber-800 dark:text-amber-200 uppercase tracking-wider">
              {overallStatus === 'SERVICE_UNREACHABLE' || overallStatus === 'OFFLINE'
                ? 'AI Engine: Service Unreachable (Port 8000)'
                : overallStatus === 'SERVICE_ERROR'
                ? 'AI Engine: Service Error'
                : overallStatus === 'MODULE_DEGRADED'
                ? 'AI Engine: Module Degraded'
                : `AI Engine Status: ${overallStatus}`}
            </p>
            <p className="text-xs">
              {healthData?.statusMessage || sysHealth.statusMessage ||
                (overallStatus === 'SERVICE_UNREACHABLE' || overallStatus === 'OFFLINE'
                  ? `AI engine microservice on port 8000 is not reachable (connection refused or timeout). Last seen: ${formatLastSeen(sysHealth.lastSuccessfulPoll || healthData?.lastSuccessfulPoll)}. Requests fail-safe to cached states.`
                  : 'AI background poller snapshot indicates module degradation. Requests fail-safe to cached states.')}
            </p>
          </div>
        </div>
      )}

      {error && (
        <div className="p-4 rounded-2xl bg-rose-500/10 border border-rose-500/20 text-rose-600 dark:text-rose-400 text-xs font-medium flex items-center gap-3">
          <AlertTriangle className="w-5 h-5 shrink-0" />
          <span>{error}</span>
        </div>
      )}

      {/* Top 4 System Overview Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {/* Metric 1: System Operational State */}
        <Card glass className="p-5 rounded-2xl border border-white/20 dark:border-white/10 shadow-lg space-y-3">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-muted-foreground uppercase tracking-wider">AI Reachability</span>
            <div className={`p-2 rounded-xl ${overallStatus === 'HEALTHY' ? 'bg-emerald-500/10 text-emerald-500' : 'bg-amber-500/10 text-amber-500'}`}>
              <Activity className="w-4 h-4" />
            </div>
          </div>
          <div>
            <div className="text-2xl font-black text-foreground flex items-center gap-2">
              {overallStatus === 'HEALTHY'
                ? '6 / 6 Modules UP'
                : overallStatus === 'SERVICE_UNREACHABLE' || overallStatus === 'OFFLINE'
                ? 'Engine Unreachable'
                : 'Degraded State'}
            </div>
            <p className="text-[11px] text-muted-foreground mt-0.5">
              Uptime: <span className="font-bold text-foreground">{formatDuration(sysHealth.backendUptimeSeconds)}</span> : DB: <span className="font-bold text-emerald-500">{sysHealth.databaseConnectivity || 'UP'}</span>
            </p>
          </div>
        </Card>

        {/* Metric 2: Platform Users & Active Counts */}
        <Card glass className="p-5 rounded-2xl border border-white/20 dark:border-white/10 shadow-lg space-y-3">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-muted-foreground uppercase tracking-wider">Directory & Active</span>
            <div className="p-2 rounded-xl bg-brand-blue/10 text-brand-blue">
              <Users className="w-4 h-4" />
            </div>
          </div>
          <div>
            <div className="text-2xl font-black text-foreground">
              {overview.totalUsers ?? '-'} Users
            </div>
            <p className="text-[11px] text-muted-foreground mt-0.5">
              Active: <span className="font-bold text-foreground">{overview.activeUsers24h ?? 0}</span> (24h) : <span className="font-bold text-foreground">{overview.activeUsers7d ?? 0}</span> (7d)
            </p>
          </div>
        </Card>

        {/* Metric 3: Jobs & Applications */}
        <Card glass className="p-5 rounded-2xl border border-white/20 dark:border-white/10 shadow-lg space-y-3">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-muted-foreground uppercase tracking-wider">Jobs & Applications</span>
            <div className="p-2 rounded-xl bg-emerald-500/10 text-emerald-500">
              <Briefcase className="w-4 h-4" />
            </div>
          </div>
          <div>
            <div className="text-2xl font-black text-foreground">
              {overview.totalJobsPosted ?? 0} Jobs
            </div>
            <p className="text-[11px] text-muted-foreground mt-0.5">
              <span className="font-bold text-emerald-600 dark:text-emerald-400">{overview.openJobs ?? 0} Open</span> : {overview.totalApplications ?? 0} Applications ({overview.avgApplicationsPerJob ?? 0}/job)
            </p>
          </div>
        </Card>

        {/* Metric 4: AI Volume & Latency */}
        <Card glass className="p-5 rounded-2xl border border-white/20 dark:border-white/10 shadow-lg space-y-3">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-muted-foreground uppercase tracking-wider">AI Calls & Latency</span>
            <div className="p-2 rounded-xl bg-amber-500/10 text-amber-500">
              <Cpu className="w-4 h-4" />
            </div>
          </div>
          <div>
            <div className="text-2xl font-black text-foreground">
              {aiPerf.callsToday ?? 0} Today
            </div>
            <p className="text-[11px] text-muted-foreground mt-0.5">
              Avg <span className="font-bold text-foreground">{avgLatency}s</span> (p95: {aiPerf.p95LatencySeconds ?? 0}s) : {aiPerf.successRate ?? 100}% Success
            </p>
          </div>
        </Card>
      </div>

      {/* Time-Series Charts Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Chart 1: Signups vs Daily Active Users */}
        <Card glass className="p-5 sm:p-6 rounded-3xl border border-white/20 dark:border-white/10 shadow-xl space-y-4">
          <PlatformTimeSeriesChart
            data={timeSeries}
            series={[
              { key: 'signups', label: 'New Signups', color: '#0192C6' },
              { key: 'activeUsers', label: 'Daily Active Users', color: '#10B981' },
            ]}
            title="User Growth & Daily Activity"
            subtitle={`Daily trend over selected scope (${selectedRange})`}
            height={200}
          />
        </Card>

        {/* Chart 2: API Request Volume & Error Rate */}
        <Card glass className="p-5 sm:p-6 rounded-3xl border border-white/20 dark:border-white/10 shadow-xl space-y-4">
          <PlatformTimeSeriesChart
            data={timeSeries}
            series={[
              { key: 'apiRequests', label: 'API Requests', color: '#004499' },
              { key: 'apiErrors', label: 'API Errors', color: '#EF4444' },
            ]}
            title="Backend Traffic & Error Volume"
            subtitle={`API throughput tracked via in-memory metrics`}
            height={200}
          />
        </Card>
      </div>

      {/* 6 AI Engine Modules Health Grid */}
      <div className="space-y-4">
        <div className="flex items-center justify-between">
          <div className="space-y-0.5">
            <h2 className="text-lg font-bold font-heading text-foreground flex items-center gap-2">
              <Layers className="w-5 h-5 text-brand-blue" />
              AI Engine Module Reachability (6 Modules)
            </h2>
            <p className="text-xs text-muted-foreground">
              Polled every 45s from AI engine <code className="text-[11px] px-1 py-0.5 rounded bg-muted">/health</code> - zero page-load hit.
            </p>
          </div>
          <Link to={ROUTES.ADMIN_PROVIDERS} className="text-xs text-brand-blue hover:underline font-bold flex items-center gap-1">
            Provider Keys <ChevronRight className="w-3.5 h-3.5" />
          </Link>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {modules.map((mod) => {
            const isHealthy = mod.status === 'HEALTHY' || mod.status === 'UP';
            const durationSec = mod.durationInCurrentStateSeconds || 0;
            return (
              <Card
                key={mod.id}
                glass
                className="p-5 rounded-2xl border border-white/20 dark:border-white/10 shadow-md hover:shadow-xl transition-all space-y-3 relative overflow-hidden"
              >
                <div className="flex items-start justify-between gap-2">
                  <div className="space-y-0.5">
                    <span className="text-[10px] font-bold uppercase tracking-wider text-muted-foreground">
                      {mod.category || 'AI Module'}
                    </span>
                    <h3 className="text-sm font-bold text-foreground">{mod.name}</h3>
                  </div>

                  <span
                    className={`inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-md text-[10px] font-bold shrink-0 ${
                      isHealthy
                        ? 'bg-emerald-500/15 text-emerald-600 dark:text-emerald-400 border border-emerald-500/20'
                        : 'bg-rose-500/15 text-rose-600 dark:text-rose-400 border border-rose-500/20'
                    }`}
                  >
                    <span className={`w-1.5 h-1.5 rounded-full ${isHealthy ? 'bg-emerald-500' : 'bg-rose-500'}`} />
                    {mod.status}
                  </span>
                </div>

                <div className="text-[11px] text-muted-foreground space-y-1">
                  <div className="flex items-center justify-between">
                    <span>Provider / Model:</span>
                    <span className="font-bold text-foreground">{mod.provider || 'ollama'} ({mod.model || 'default'})</span>
                  </div>
                  <div className="flex items-center justify-between">
                    <span>State Duration:</span>
                    <span className="font-mono text-foreground font-semibold">{formatDuration(durationSec)}</span>
                  </div>
                  {(overallStatus === 'SERVICE_UNREACHABLE' || overallStatus === 'OFFLINE' || mod.status === 'OFFLINE') ? (
                    <div className="pt-1 text-[11px] text-rose-500/90 dark:text-rose-400 font-medium truncate">
                      AI engine unreachable (last seen {formatLastSeen(sysHealth.lastSuccessfulPoll || healthData?.lastSuccessfulPoll)})
                    </div>
                  ) : mod.error ? (
                    <div className="pt-1 text-[11px] text-amber-600 dark:text-amber-400 font-medium truncate" title={mod.error}>
                      Module degraded: {mod.error}
                    </div>
                  ) : null}
                </div>

                <div className="pt-2 border-t border-border/50 flex items-center justify-between text-[11px]">
                  <span className="text-muted-foreground">
                    Last check: <span className="font-mono text-foreground">{sysHealth.lastHealthPollAt ? new Date(sysHealth.lastHealthPollAt).toLocaleTimeString() : 'Recent'}</span>
                  </span>
                  <code className="text-[10px] text-muted-foreground font-mono bg-surface-2 px-1.5 py-0.5 rounded">
                    {mod.endpoint ? mod.endpoint.split(' ')[0] : `/api/v1/${mod.id}`}
                  </code>
                </div>
              </Card>
            );
          })}
        </div>
      </div>

      {/* Two Column Section: System Infrastructure & AI Call Feed */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Left Col (1 Col): Quota, System Health & Endpoint Stats */}
        <div className="space-y-4">
          {/* Provider Traffic & Gemini Quota Meter */}
          <Card glass className="p-5 sm:p-6 rounded-3xl border border-white/20 dark:border-white/10 shadow-xl space-y-5">
            <div>
              <h3 className="text-base font-bold font-heading text-foreground flex items-center gap-2">
                <Cpu className="w-4 h-4 text-brand-blue" />
                Provider Quota & Traffic
              </h3>
              <p className="text-xs text-muted-foreground mt-0.5">
                AI provider split and Gemini daily rate limit tracking
              </p>
            </div>

            {/* Gemini Free Tier Daily Quota Meter */}
            <div className="p-4 rounded-2xl bg-surface-2/70 border border-white/10 space-y-2.5">
              <div className="flex items-center justify-between text-xs">
                <span className="font-bold text-foreground">Gemini Free-Tier Quota</span>
                <span className="font-mono text-[11px] text-muted-foreground">
                  {quota.usedToday ?? 0} / {quota.dailyLimit ?? 250} Daily Req
                </span>
              </div>
              
              <div className="h-2.5 w-full rounded-full bg-muted/60 overflow-hidden">
                <div
                  className={`h-full rounded-full transition-all ${
                    (quota.percentageUsed ?? 0) > 80 ? 'bg-rose-500' : (quota.percentageUsed ?? 0) > 50 ? 'bg-amber-500' : 'bg-brand-blue'
                  }`}
                  style={{ width: `${Math.min(100, Math.max(2, quota.percentageUsed ?? 0))}%` }}
                />
              </div>

              <div className="flex items-center justify-between text-[10px] text-muted-foreground">
                <span>{(quota.remainingToday ?? Math.max(0, 250 - (quota.usedToday ?? 0)))} requests remaining</span>
                <span>{quota.percentageUsed ?? 0}% used</span>
              </div>
            </div>

            {/* System Health Indicators */}
            <div className="space-y-2 pt-2 border-t border-border/40">
              <span className="text-[10px] font-bold uppercase tracking-wider text-muted-foreground">
                Infrastructure Health
              </span>
              <div className="grid grid-cols-2 gap-2 text-[11px]">
                <div className="p-2.5 rounded-xl bg-surface-2 border border-border/40">
                  <span className="text-muted-foreground text-[10px] block">PostgreSQL DB</span>
                  <span className="font-bold text-emerald-500 flex items-center gap-1 mt-0.5">
                    <CheckCircle2 className="w-3 h-3" /> Connected
                  </span>
                </div>
                <div className="p-2.5 rounded-xl bg-surface-2 border border-border/40">
                  <span className="text-muted-foreground text-[10px] block">Email Service</span>
                  <span className="font-bold text-foreground flex items-center gap-1 mt-0.5">
                    <Mail className="w-3 h-3 text-brand-blue" />
                    {sysHealth.emailSmtpConfigured ? 'SMTP Live' : 'Simulated'}
                  </span>
                </div>
              </div>
            </div>

            {/* Application Endpoint Performance */}
            {appPerf.endpointGroups && appPerf.endpointGroups.length > 0 && (
              <div className="space-y-2 pt-2 border-t border-border/40">
                <span className="text-[10px] font-bold uppercase tracking-wider text-muted-foreground">
                  Endpoint Latencies
                </span>
                <div className="space-y-1.5 max-h-40 overflow-y-auto">
                  {appPerf.endpointGroups.map((eg) => (
                    <div key={eg.group} className="flex items-center justify-between text-[11px] p-2 rounded-lg bg-surface-2/60">
                      <span className="font-semibold text-foreground uppercase text-[10px]">{eg.group}</span>
                      <div className="flex items-center gap-2 font-mono text-[10px] text-muted-foreground">
                        <span>{eg.avgLatencyMs}ms avg</span>
                        <span>({eg.totalRequests} req)</span>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}

            <Link to={ROUTES.ADMIN_LLM_USAGE} className="block">
              <Button variant="outline" size="sm" className="w-full text-xs font-bold gap-1.5 rounded-xl cursor-pointer">
                View Deep Usage Metrics <ArrowUpRight className="w-3.5 h-3.5" />
              </Button>
            </Link>
          </Card>
        </div>

        {/* Right Col (2 Cols): Live AI Call Log Feed */}
        <div className="lg:col-span-2 space-y-4">
          <Card glass className="p-5 sm:p-6 rounded-3xl border border-white/20 dark:border-white/10 shadow-xl space-y-4">
            <div className="flex items-center justify-between">
              <div>
                <h3 className="text-base font-bold font-heading text-foreground flex items-center gap-2">
                  <FileCode className="w-4 h-4 text-brand-blue" />
                  Live AI Engine Invocations
                </h3>
                <p className="text-xs text-muted-foreground mt-0.5">
                  Privacy-safe audit records from <code className="text-[10px] font-mono px-1 py-0.5 rounded bg-muted">llm_call_log.jsonl</code>
                </p>
              </div>

              <Link to={ROUTES.ADMIN_LOGS}>
                <Button variant="ghost" size="sm" className="text-xs font-bold text-brand-blue gap-1 cursor-pointer">
                  Full Audit Log <ArrowUpRight className="w-3.5 h-3.5" />
                </Button>
              </Link>
            </div>

            {recentLogs.length === 0 ? (
              <div className="text-center py-12 text-xs text-muted-foreground">
                No recent AI calls recorded in logs yet.
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs">
                  <thead>
                    <tr className="border-b border-border/50 text-muted-foreground">
                      <th className="pb-2.5 font-bold">Module</th>
                      <th className="pb-2.5 font-bold">Provider</th>
                      <th className="pb-2.5 font-bold">Latency</th>
                      <th className="pb-2.5 font-bold">Status</th>
                      <th className="pb-2.5 font-bold text-right">Action</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-border/40">
                    {recentLogs.map((log) => {
                      const isSuccess = (log.status || 'SUCCESS').toUpperCase() === 'SUCCESS';
                      return (
                        <tr
                          key={log.id || Math.random()}
                          className="hover:bg-surface-2/50 transition-colors group cursor-pointer"
                          onClick={() => setSelectedLog(log)}
                        >
                          <td className="py-2.5 font-semibold text-foreground">
                            <div className="flex items-center gap-1.5">
                              <span className="w-1.5 h-1.5 rounded-full bg-brand-blue" />
                              <span className="font-mono text-[11px]">{log.task_type || log.taskType}</span>
                            </div>
                          </td>
                          <td className="py-2.5 text-muted-foreground capitalize">
                            {log.provider}
                          </td>
                          <td className="py-2.5 font-mono text-[11px] text-foreground">
                            {log.latency_seconds || log.latencySeconds || 0}s
                          </td>
                          <td className="py-2.5">
                            <span
                              className={`inline-flex items-center gap-1 px-2 py-0.5 rounded-md text-[10px] font-bold ${
                                isSuccess
                                  ? 'bg-emerald-500/10 text-emerald-500'
                                  : 'bg-rose-500/10 text-rose-500'
                              }`}
                            >
                              {isSuccess ? <CheckCircle2 className="w-3 h-3" /> : <XCircle className="w-3 h-3" />}
                              {log.status || 'SUCCESS'}
                            </span>
                          </td>
                          <td className="py-2.5 text-right">
                            <Button
                              variant="ghost"
                              size="sm"
                              className="h-7 text-[11px] font-semibold text-brand-blue hover:text-brand-blue/80"
                              onClick={(e) => {
                                e.stopPropagation();
                                setSelectedLog(log);
                              }}
                            >
                              Inspect
                            </Button>
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            )}
          </Card>
        </div>
      </div>

      {/* Drill-Down Inspector Modal for Call Logs */}
      {selectedLog && (
        <Modal
          isOpen={Boolean(selectedLog)}
          onClose={() => setSelectedLog(null)}
          title={`AI Call Log Inspection: ${selectedLog.task_type || selectedLog.taskType}`}
        >
          <div className="space-y-4 text-xs">
            <div className="grid grid-cols-2 gap-3 p-3.5 rounded-2xl bg-surface-2 border border-white/10">
              <div>
                <span className="text-[10px] text-muted-foreground uppercase font-bold">Log ID</span>
                <p className="font-mono text-[11px] text-foreground truncate">{selectedLog.id}</p>
              </div>
              <div>
                <span className="text-[10px] text-muted-foreground uppercase font-bold">Timestamp</span>
                <p className="font-mono text-[11px] text-foreground truncate">
                  {selectedLog.timestamp ? new Date(selectedLog.timestamp).toLocaleString() : 'Recent'}
                </p>
              </div>
              <div>
                <span className="text-[10px] text-muted-foreground uppercase font-bold">Provider / Attempts</span>
                <p className="font-semibold text-foreground capitalize">
                  {selectedLog.provider} (Attempt {selectedLog.attempt || 1})
                </p>
              </div>
              <div>
                <span className="text-[10px] text-muted-foreground uppercase font-bold">Latency</span>
                <p className="font-semibold text-foreground">
                  {selectedLog.latency_seconds || selectedLog.latencySeconds || 0} seconds
                </p>
              </div>
            </div>

            {selectedLog.error_message && (
              <div className="p-3.5 rounded-xl bg-rose-500/10 border border-rose-500/20 text-rose-600 dark:text-rose-400 space-y-1">
                <span className="text-[10px] uppercase font-black">Failure Reason</span>
                <p className="font-mono text-[11px]">{selectedLog.error_message}</p>
              </div>
            )}

            {/* Truncated Prompt Preview (200 Chars) */}
            <div className="space-y-1">
              <div className="flex items-center justify-between">
                <span className="text-[11px] font-bold text-foreground">Sanitized Prompt Preview</span>
                <span className="text-[10px] text-muted-foreground">Privacy Safe (first 200 chars max)</span>
              </div>
              <pre className="p-3 rounded-xl bg-muted/60 font-mono text-[11px] text-foreground/90 whitespace-pre-wrap break-words max-h-32 overflow-y-auto border border-border">
                {selectedLog.prompt_preview || selectedLog.promptPreview || '(Empty or not captured)'}
              </pre>
            </div>

            {/* Truncated Response Preview (200 Chars) */}
            <div className="space-y-1">
              <div className="flex items-center justify-between">
                <span className="text-[11px] font-bold text-foreground">Sanitized Response Preview</span>
                <span className="text-[10px] text-muted-foreground">Privacy Safe (first 200 chars max)</span>
              </div>
              <pre className="p-3 rounded-xl bg-muted/60 font-mono text-[11px] text-foreground/90 whitespace-pre-wrap break-words max-h-32 overflow-y-auto border border-border">
                {selectedLog.response_preview || selectedLog.responsePreview || '(Empty or not captured)'}
              </pre>
            </div>

            <div className="flex justify-end pt-2">
              <Button size="sm" variant="outline" onClick={() => setSelectedLog(null)} className="rounded-xl text-xs">
                Close Inspector
              </Button>
            </div>
          </div>
        </Modal>
      )}
    </div>
  );
};
