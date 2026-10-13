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
  Key,
  FileCode,
  ArrowUpRight,
  RefreshCw,
  Search,
  ExternalLink,
  ShieldCheck,
  ChevronRight,
  Database,
  Layers,
} from 'lucide-react';
import { Card } from '@/components/ui/Card';
import { Badge } from '@/components/ui/Badge';
import { Button } from '@/components/ui/Button';
import { Modal } from '@/components/ui/Modal';
import { Skeleton } from '@/components/ui/Skeleton';
import { adminApi } from '@/services/api/admin.api';
import { ROUTES } from '@/constants';

export const AdminDashboardPage = () => {
  const [healthData, setHealthData] = useState(null);
  const [statsData, setStatsData] = useState(null);
  const [recentLogs, setRecentLogs] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [error, setError] = useState(null);
  const [selectedLog, setSelectedLog] = useState(null);

  const loadDashboardData = async (silent = false) => {
    try {
      if (!silent) setIsLoading(true);
      else setIsRefreshing(true);
      setError(null);

      const [health, stats, logsRes] = await Promise.all([
        adminApi.getHealth().catch((err) => ({
          overallStatus: 'DEGRADED',
          error: err.message,
          modules: [],
        })),
        adminApi.getStats().catch(() => null),
        adminApi.getLogs({ page: 1, size: 8 }).catch(() => ({ items: [] })),
      ]);

      setHealthData(health);
      setStatsData(stats);
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
    loadDashboardData();
    // Auto-poll every 30s
    const timer = setInterval(() => loadDashboardData(true), 30000);
    return () => clearInterval(timer);
  }, []);

  if (isLoading) {
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

  const modules = healthData?.modules || [];
  const overallStatus = healthData?.overallStatus || 'UNKNOWN';
  const providers = healthData?.providers || {};
  const activeProvider = providers.activeProvider || 'ollama';
  const isEngineAvailable = statsData && statsData.available !== false;
  const quota = statsData?.quota || { dailyLimit: 250, usedToday: 0, percentageUsed: 0 };
  const totalCalls = statsData?.totalCalls || 0;
  const avgLatency = statsData?.avgLatencySeconds || 0;
  const activeUsers = healthData?.activeUsersCount ?? '-';

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
              v1.0 Live
            </Badge>
          </div>
          <p className="text-xs sm:text-sm text-muted-foreground">
            Real-time telemetry, 6 AI engine modules health, LLM quotas, and governance.
          </p>
        </div>

        <div className="flex items-center gap-3 relative z-10">
          <Button
            variant="outline"
            size="sm"
            onClick={() => loadDashboardData(true)}
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

      {/* Amber alert when engine is unreachable */}
      {statsData && statsData.available === false && (
        <div className="flex items-start sm:items-center gap-3 p-4 rounded-2xl bg-amber-500/10 border border-amber-500/30 text-amber-700 dark:text-amber-300">
          <AlertTriangle className="w-5 h-5 shrink-0 text-amber-500 mt-0.5 sm:mt-0" />
          <div className="space-y-0.5">
            <p className="text-xs font-bold text-amber-800 dark:text-amber-200 uppercase tracking-wider">
              AI Engine Unreachable
            </p>
            <p className="text-xs">
              {statsData?.message || 'AI engine unreachable'} - live telemetry, failure breakdown, and quota usage are currently offline.
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

      {/* Top 4 Metric Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {/* Metric 1: System Operational State */}
        <Card glass className="p-5 rounded-2xl border border-white/20 dark:border-white/10 shadow-lg space-y-3">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-muted-foreground uppercase tracking-wider">AI Health</span>
            <div className={`p-2 rounded-xl ${overallStatus === 'HEALTHY' ? 'bg-emerald-500/10 text-emerald-500' : 'bg-amber-500/10 text-amber-500'}`}>
              <Activity className="w-4 h-4" />
            </div>
          </div>
          <div>
            <div className="text-2xl font-black text-foreground flex items-center gap-2">
              {overallStatus === 'HEALTHY' ? '6 / 6 Online' : 'Degraded State'}
            </div>
            <p className="text-[11px] text-muted-foreground mt-0.5">
              Provider: <span className="font-bold uppercase text-foreground">{activeProvider}</span> ({providers.activeModel || 'llama3.2'})
            </p>
          </div>
        </Card>

        {/* Metric 2: Today's LLM Volume */}
        <Card glass className="p-5 rounded-2xl border border-white/20 dark:border-white/10 shadow-lg space-y-3">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-muted-foreground uppercase tracking-wider">LLM Calls</span>
            <div className={`p-2 rounded-xl ${isEngineAvailable ? 'bg-blue-500/10 text-blue-500' : 'bg-amber-500/10 text-amber-500'}`}>
              <Cpu className="w-4 h-4" />
            </div>
          </div>
          <div>
            {!isEngineAvailable ? (
              <div>
                <div className="text-2xl font-bold text-amber-500">Unreachable</div>
                <p className="text-[11px] text-amber-600 dark:text-amber-400 mt-0.5">
                  AI engine unreachable
                </p>
              </div>
            ) : (
              <div>
                <div className="text-2xl font-black text-foreground">{totalCalls} calls</div>
                <p className="text-[11px] text-muted-foreground mt-0.5">
                  {statsData?.todayCalls || 0} today : {statsData?.successRate || 100}% success rate
                </p>
              </div>
            )}
          </div>
        </Card>

        {/* Metric 3: Average Latency */}
        <Card glass className="p-5 rounded-2xl border border-white/20 dark:border-white/10 shadow-lg space-y-3">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-muted-foreground uppercase tracking-wider">Avg Latency</span>
            <div className={`p-2 rounded-xl ${isEngineAvailable ? 'bg-brand-blue/10 text-brand-blue' : 'bg-amber-500/10 text-amber-500'}`}>
              <Clock className="w-4 h-4" />
            </div>
          </div>
          <div>
            <div className="text-2xl font-black text-foreground">{isEngineAvailable ? `${avgLatency}s` : '-'}</div>
            <p className="text-[11px] text-muted-foreground mt-0.5">
              {isEngineAvailable ? 'Local Ollama response time' : 'Telemetry offline'}
            </p>
          </div>
        </Card>

        {/* Metric 4: Active System Users */}
        <Card glass className="p-5 rounded-2xl border border-white/20 dark:border-white/10 shadow-lg space-y-3">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-muted-foreground uppercase tracking-wider">Directory Users</span>
            <div className="p-2 rounded-xl bg-amber-500/10 text-amber-500">
              <Users className="w-4 h-4" />
            </div>
          </div>
          <div>
            <div className="text-2xl font-black text-foreground">{activeUsers} Active</div>
            <p className="text-[11px] text-muted-foreground mt-0.5">
              Total {healthData?.totalUsersCount ?? '-'} registered accounts
            </p>
          </div>
        </Card>
      </div>

      {/* Main Section: 6 AI Engine Modules Health Grid */}
      <div className="space-y-4">
        <div className="flex items-center justify-between">
          <div className="space-y-0.5">
            <h2 className="text-lg font-bold font-heading text-foreground flex items-center gap-2">
              <Layers className="w-5 h-5 text-brand-blue" />
              AI Engine Module Reachability (6 Modules)
            </h2>
            <p className="text-xs text-muted-foreground">
              Monitored via backend polling of AI Engine <code className="text-[11px] px-1 py-0.5 rounded bg-muted">/health</code>
            </p>
          </div>
          <Link to={ROUTES.ADMIN_PROVIDERS} className="text-xs text-brand-blue hover:underline font-bold flex items-center gap-1">
            Provider Keys <ChevronRight className="w-3.5 h-3.5" />
          </Link>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {modules.map((mod) => {
            const isHealthy = mod.status === 'HEALTHY' || mod.status === 'UP';
            return (
              <Card
                key={mod.id}
                glass
                className="p-5 rounded-2xl border border-white/20 dark:border-white/10 shadow-md hover:shadow-xl transition-all space-y-3 relative overflow-hidden"
              >
                <div className="flex items-start justify-between gap-2">
                  <div className="space-y-0.5">
                    <span className="text-[10px] font-bold uppercase tracking-wider text-muted-foreground">
                      {mod.category}
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

                <p className="text-xs text-muted-foreground leading-relaxed line-clamp-2">
                  {mod.description}
                </p>

                <div className="pt-2 border-t border-border/50 flex items-center justify-between text-[11px]">
                  <div className="flex items-center gap-1 text-muted-foreground">
                    <span className="font-semibold text-foreground">{mod.provider}</span>
                  </div>
                  <code className="text-[10px] text-muted-foreground font-mono bg-surface-2 px-1.5 py-0.5 rounded">
                    {mod.endpoint.split(' ')[0]}
                  </code>
                </div>
              </Card>
            );
          })}
        </div>
      </div>

      {/* Two Column Section: Provider Quota Bar & Recent AI Call Logs */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Left Col (1 Col): Provider Breakdown & Quota Meter */}
        <div className="space-y-4">
          <Card glass className="p-5 sm:p-6 rounded-3xl border border-white/20 dark:border-white/10 shadow-xl space-y-5">
            <div>
              <h3 className="text-base font-bold font-heading text-foreground flex items-center gap-2">
                <Cpu className="w-4 h-4 text-brand-blue" />
                Provider Traffic & Quota
              </h3>
              <p className="text-xs text-muted-foreground mt-0.5">
                Active provider configuration and rate limits
              </p>
            </div>

            {/* Provider Breakdown Bars */}
            <div className="space-y-3">
              <div className="flex items-center justify-between text-xs">
                <span className="font-semibold text-foreground">Ollama (Local LLM)</span>
                <Badge variant="outline" className="text-[10px] bg-emerald-500/10 text-emerald-500 border-emerald-500/20">
                  Active Provider
                </Badge>
              </div>
              <div className="h-2 w-full rounded-full bg-surface-2 overflow-hidden">
                <div className="h-full bg-emerald-500 rounded-full" style={{ width: '100%' }} />
              </div>
              <p className="text-[11px] text-muted-foreground">
                Model: <span className="font-mono text-foreground">{providers.ollama?.model || 'llama3.2:3b'}</span> (100% of calls)
              </p>
            </div>

            {/* Gemini Free Tier Daily Quota Meter */}
            {!isEngineAvailable || !statsData?.quota ? (
              <div className="p-4 rounded-2xl bg-amber-500/10 border border-amber-500/20 text-xs text-amber-700 dark:text-amber-300">
                Quota counters and rate limits are unavailable while the AI engine is unreachable.
              </div>
            ) : (
              <div className="p-4 rounded-2xl bg-surface-2/70 border border-white/10 space-y-2.5">
                <div className="flex items-center justify-between text-xs">
                  <span className="font-bold text-foreground">Gemini Free-Tier Quota</span>
                  <span className="font-mono text-[11px] text-muted-foreground">
                    {quota.usedToday} / {quota.dailyLimit} Daily Req
                  </span>
                </div>
                
                <div className="h-2.5 w-full rounded-full bg-muted/60 overflow-hidden">
                  <div
                    className={`h-full rounded-full transition-all ${
                      quota.percentageUsed > 80 ? 'bg-rose-500' : quota.percentageUsed > 50 ? 'bg-amber-500' : 'bg-brand-blue'
                    }`}
                    style={{ width: `${Math.min(100, Math.max(2, quota.percentageUsed))}%` }}
                  />
                </div>

                <div className="flex items-center justify-between text-[10px] text-muted-foreground">
                  <span>{quota.remainingToday} requests remaining today</span>
                  <span>{quota.percentageUsed}% used</span>
                </div>
              </div>
            )}

            {/* Read-Only Keys Summary Note */}
            <div className="text-[11px] text-muted-foreground bg-muted/40 p-3 rounded-xl border border-border/40 space-y-1">
              <div className="font-semibold text-foreground flex items-center gap-1.5">
                <ShieldCheck className="w-3.5 h-3.5 text-brand-blue" />
                Zero Secret Leakage
              </div>
              <p>API keys are securely sanitized on the backend. Only reachability indicators are transmitted to UI.</p>
            </div>

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
                  Real call log feed from <code className="text-[10px] font-mono px-1 py-0.5 rounded bg-muted">llm_call_log.jsonl</code>
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
                <span className="text-[10px] text-muted-foreground">Privacy Safe (200 chars max)</span>
              </div>
              <pre className="p-3 rounded-xl bg-muted/60 font-mono text-[11px] text-foreground/90 whitespace-pre-wrap break-words max-h-32 overflow-y-auto border border-border">
                {selectedLog.prompt_preview || selectedLog.promptPreview || '(Empty or not captured)'}
              </pre>
            </div>

            {/* Truncated Response Preview (200 Chars) */}
            <div className="space-y-1">
              <div className="flex items-center justify-between">
                <span className="text-[11px] font-bold text-foreground">Sanitized Response Preview</span>
                <span className="text-[10px] text-muted-foreground">Privacy Safe (200 chars max)</span>
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
