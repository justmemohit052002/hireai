import React, { useState, useEffect } from 'react';
import {
  FileCode,
  Search,
  Filter,
  CheckCircle2,
  XCircle,
  Clock,
  RefreshCw,
  Eye,
  AlertTriangle,
  ChevronLeft,
  ChevronRight,
  ShieldCheck,
  Terminal,
} from 'lucide-react';
import { Card } from '@/components/ui/Card';
import { Badge } from '@/components/ui/Badge';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Select } from '@/components/ui/Select';
import { Modal } from '@/components/ui/Modal';
import { Skeleton } from '@/components/ui/Skeleton';
import { adminApi } from '@/services/api/admin.api';

export const AdminCallLogsPage = () => {
  const [logs, setLogs] = useState([]);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [page, setPage] = useState(1);
  const [size, setSize] = useState(15);
  const [searchQuery, setSearchQuery] = useState('');
  const [taskFilter, setTaskFilter] = useState('ALL');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [providerFilter, setProviderFilter] = useState('ALL');

  const [isLoading, setIsLoading] = useState(true);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [selectedLog, setSelectedLog] = useState(null);

  const fetchLogs = async (silent = false) => {
    try {
      if (!silent) setIsLoading(true);
      else setIsRefreshing(true);

      const res = await adminApi.getLogs({
        query: searchQuery,
        taskType: taskFilter,
        status: statusFilter,
        provider: providerFilter,
        page,
        size,
      });

      setLogs(res?.items || []);
      setTotalElements(res?.totalElements || 0);
      setTotalPages(res?.totalPages || 1);
    } catch (err) {
      console.error('Failed to query call logs:', err);
    } finally {
      setIsLoading(false);
      setIsRefreshing(false);
    }
  };

  useEffect(() => {
    fetchLogs();
  }, [page, size, taskFilter, statusFilter, providerFilter]);

  const handleSearchSubmit = (e) => {
    e.preventDefault();
    setPage(1);
    fetchLogs();
  };

  return (
    <div className="space-y-6 max-w-7xl mx-auto pb-16">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 p-5 sm:p-6 rounded-3xl glass border border-white/20 dark:border-white/10 shadow-xl">
        <div className="space-y-1">
          <h1 className="text-2xl font-bold font-heading text-foreground flex items-center gap-2.5">
            <FileCode className="w-6 h-6 text-brand-blue" />
            AI Call Logs & Audit Trail
          </h1>
          <p className="text-xs sm:text-sm text-muted-foreground">
            Searchable audit history of AI engine calls with latency, attempts, failure root-causes, and privacy previews.
          </p>
        </div>

        <Button
          variant="outline"
          size="sm"
          onClick={() => fetchLogs(true)}
          disabled={isRefreshing}
          className="gap-2 rounded-xl text-xs font-semibold self-start sm:self-auto cursor-pointer"
        >
          <RefreshCw className={`w-3.5 h-3.5 ${isRefreshing ? 'animate-spin text-brand-blue' : ''}`} />
          {isRefreshing ? 'Refreshing...' : 'Refresh Logs'}
        </Button>
      </div>

      {/* Filter and Search Bar */}
      <Card glass className="p-4 sm:p-5 rounded-2xl border border-white/20 dark:border-white/10 shadow-md">
        <form onSubmit={handleSearchSubmit} className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-3">
          {/* Search Query Input */}
          <div className="lg:col-span-2 relative">
            <Search className="w-4 h-4 text-muted-foreground absolute left-3 top-1/2 -translate-y-1/2 pointer-events-none" />
            <Input
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Search by prompt, task, ID or keyword..."
              className="pl-9 rounded-xl text-xs"
            />
          </div>

          {/* Task Type Filter */}
          <select
            value={taskFilter}
            onChange={(e) => {
              setTaskFilter(e.target.value);
              setPage(1);
            }}
            className="w-full px-3 py-2 rounded-xl text-xs font-medium bg-surface-2 border border-border text-foreground focus:outline-hidden focus:ring-2 focus:ring-brand-blue/30"
          >
            <option value="ALL">All Modules</option>
            <option value="jd_generate">Job Description Generator</option>
            <option value="resume_parse">Resume Parser</option>
            <option value="match_score">Match Engine</option>
            <option value="chatbot">Chatbot</option>
            <option value="interview_questions">Interview Questions</option>
            <option value="interview_evaluate">Interview Evaluation</option>
            <option value="decision_finalize">Decision Engine</option>
          </select>

          {/* Status Filter */}
          <select
            value={statusFilter}
            onChange={(e) => {
              setStatusFilter(e.target.value);
              setPage(1);
            }}
            className="w-full px-3 py-2 rounded-xl text-xs font-medium bg-surface-2 border border-border text-foreground focus:outline-hidden focus:ring-2 focus:ring-brand-blue/30"
          >
            <option value="ALL">All Statuses</option>
            <option value="SUCCESS">Success Only</option>
            <option value="FAILED">Failed Only</option>
          </select>

          {/* Search Action Button */}
          <Button type="submit" size="sm" className="rounded-xl text-xs font-bold shadow-md cursor-pointer">
            <Filter className="w-3.5 h-3.5 mr-1" /> Apply Filters
          </Button>
        </form>
      </Card>

      {/* Logs Table Card */}
      <Card glass className="p-5 sm:p-6 rounded-3xl border border-white/20 dark:border-white/10 shadow-xl space-y-4">
        <div className="flex items-center justify-between pb-3 border-b border-border/50">
          <div className="text-xs text-muted-foreground font-semibold">
            Showing <span className="text-foreground font-bold">{logs.length}</span> of{' '}
            <span className="text-foreground font-bold">{totalElements}</span> recorded log entries
          </div>
          <div className="flex items-center gap-2">
            <span className="text-xs text-muted-foreground">Rows:</span>
            <select
              value={size}
              onChange={(e) => {
                setSize(Number(e.target.value));
                setPage(1);
              }}
              className="px-2 py-1 rounded-lg text-xs bg-surface-2 border border-border text-foreground"
            >
              <option value={10}>10</option>
              <option value={15}>15</option>
              <option value={25}>25</option>
              <option value={50}>50</option>
            </select>
          </div>
        </div>

        {isLoading ? (
          <div className="space-y-3 py-6">
            {[1, 2, 3, 4, 5].map((i) => (
              <Skeleton key={i} className="h-12 w-full rounded-xl" />
            ))}
          </div>
        ) : logs.length === 0 ? (
          <div className="text-center py-16 space-y-2">
            <FileCode className="w-10 h-10 text-muted-foreground mx-auto opacity-40" />
            <h3 className="text-sm font-bold text-foreground">No call logs match your query</h3>
            <p className="text-xs text-muted-foreground">Try clearing filters or running an AI task from the platform.</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead>
                <tr className="border-b border-border/50 text-muted-foreground">
                  <th className="pb-3 font-bold">Module / Task</th>
                  <th className="pb-3 font-bold">Provider</th>
                  <th className="pb-3 font-bold">Latency</th>
                  <th className="pb-3 font-bold">Attempt</th>
                  <th className="pb-3 font-bold">Status</th>
                  <th className="pb-3 font-bold">Timestamp</th>
                  <th className="pb-3 font-bold text-right">Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border/40">
                {logs.map((log) => {
                  const isSuccess = (log.status || 'SUCCESS').toUpperCase() === 'SUCCESS';
                  return (
                    <tr
                      key={log.id || Math.random()}
                      className="hover:bg-surface-2/50 transition-colors cursor-pointer group"
                      onClick={() => setSelectedLog(log)}
                    >
                      <td className="py-3 font-semibold text-foreground">
                        <div className="flex items-center gap-2">
                          <span className="w-1.5 h-1.5 rounded-full bg-brand-blue" />
                          <span className="font-mono text-[11px] font-bold">{log.task_type || log.taskType}</span>
                        </div>
                      </td>
                      <td className="py-3 font-medium capitalize text-muted-foreground">
                        {log.provider}
                      </td>
                      <td className="py-3 font-mono text-[11px] text-foreground">
                        {log.latency_seconds || log.latencySeconds || 0}s
                      </td>
                      <td className="py-3 text-muted-foreground font-mono">
                        {log.attempt || 1}
                      </td>
                      <td className="py-3">
                        <span
                          className={`inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[10px] font-bold ${
                            isSuccess
                              ? 'bg-emerald-500/10 text-emerald-500'
                              : 'bg-rose-500/10 text-rose-500'
                          }`}
                        >
                          {isSuccess ? <CheckCircle2 className="w-3 h-3" /> : <XCircle className="w-3 h-3" />}
                          {log.status || 'SUCCESS'}
                        </span>
                      </td>
                      <td className="py-3 text-muted-foreground font-mono text-[10px]">
                        {log.timestamp ? new Date(log.timestamp).toLocaleTimeString() : 'N/A'}
                      </td>
                      <td className="py-3 text-right">
                        <Button
                          variant="ghost"
                          size="sm"
                          className="h-7 text-[11px] font-bold text-brand-blue hover:text-brand-blue/80"
                          onClick={(e) => {
                            e.stopPropagation();
                            setSelectedLog(log);
                          }}
                        >
                          <Eye className="w-3 h-3 mr-1" /> Inspect
                        </Button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}

        {/* Pagination Navigation */}
        {totalPages > 1 && (
          <div className="flex items-center justify-between pt-4 border-t border-border/50 text-xs">
            <span className="text-muted-foreground">
              Page <span className="font-bold text-foreground">{page}</span> of{' '}
              <span className="font-bold text-foreground">{totalPages}</span>
            </span>

            <div className="flex items-center gap-2">
              <Button
                variant="outline"
                size="sm"
                onClick={() => setPage((p) => Math.max(1, p - 1))}
                disabled={page <= 1}
                className="rounded-xl text-xs cursor-pointer"
              >
                <ChevronLeft className="w-3.5 h-3.5 mr-1" /> Prev
              </Button>
              <Button
                variant="outline"
                size="sm"
                onClick={() => setPage((p) => Math.min(totalPages, p + 1))}
                disabled={page >= totalPages}
                className="rounded-xl text-xs cursor-pointer"
              >
                Next <ChevronRight className="w-3.5 h-3.5 ml-1" />
              </Button>
            </div>
          </div>
        )}
      </Card>

      {/* Drill-Down Inspector Modal */}
      {selectedLog && (
        <Modal
          isOpen={Boolean(selectedLog)}
          onClose={() => setSelectedLog(null)}
          title={`Call Details Inspector: ${selectedLog.task_type || selectedLog.taskType}`}
        >
          <div className="space-y-4 text-xs">
            <div className="grid grid-cols-2 gap-3 p-3.5 rounded-2xl bg-surface-2 border border-white/10">
              <div>
                <span className="text-[10px] text-muted-foreground uppercase font-bold">Log Record ID</span>
                <p className="font-mono text-[11px] text-foreground truncate">{selectedLog.id}</p>
              </div>
              <div>
                <span className="text-[10px] text-muted-foreground uppercase font-bold">Recorded Timestamp</span>
                <p className="font-mono text-[11px] text-foreground truncate">
                  {selectedLog.timestamp ? new Date(selectedLog.timestamp).toLocaleString() : 'Recent'}
                </p>
              </div>
              <div>
                <span className="text-[10px] text-muted-foreground uppercase font-bold">Engine Provider</span>
                <p className="font-semibold text-foreground capitalize">
                  {selectedLog.provider} (Attempt {selectedLog.attempt || 1})
                </p>
              </div>
              <div>
                <span className="text-[10px] text-muted-foreground uppercase font-bold">Execution Duration</span>
                <p className="font-semibold text-foreground">
                  {selectedLog.latency_seconds || selectedLog.latencySeconds || 0}s
                </p>
              </div>
            </div>

            {selectedLog.error_message && (
              <div className="p-3.5 rounded-xl bg-rose-500/10 border border-rose-500/20 text-rose-600 dark:text-rose-400 space-y-1">
                <span className="text-[10px] uppercase font-black flex items-center gap-1.5">
                  <AlertTriangle className="w-3.5 h-3.5" /> Call Error Message
                </span>
                <p className="font-mono text-[11px]">{selectedLog.error_message}</p>
              </div>
            )}

            {/* Truncated Prompt Preview (200 Chars) */}
            <div className="space-y-1">
              <div className="flex items-center justify-between">
                <span className="text-[11px] font-bold text-foreground">Prompt Snippet (Sanitized)</span>
                <span className="text-[10px] text-muted-foreground">Truncated to 200 chars max</span>
              </div>
              <pre className="p-3 rounded-xl bg-muted/60 font-mono text-[11px] text-foreground/90 whitespace-pre-wrap break-words max-h-36 overflow-y-auto border border-border">
                {selectedLog.prompt_preview || selectedLog.promptPreview || '(No prompt preview available)'}
              </pre>
            </div>

            {/* Truncated Response Preview (200 Chars) */}
            <div className="space-y-1">
              <div className="flex items-center justify-between">
                <span className="text-[11px] font-bold text-foreground">Response Snippet (Sanitized)</span>
                <span className="text-[10px] text-muted-foreground">Truncated to 200 chars max</span>
              </div>
              <pre className="p-3 rounded-xl bg-muted/60 font-mono text-[11px] text-foreground/90 whitespace-pre-wrap break-words max-h-36 overflow-y-auto border border-border">
                {selectedLog.response_preview || selectedLog.responsePreview || '(No response preview available)'}
              </pre>
            </div>

            <div className="flex justify-end pt-2">
              <Button size="sm" variant="outline" onClick={() => setSelectedLog(null)} className="rounded-xl text-xs">
                Dismiss Inspector
              </Button>
            </div>
          </div>
        </Modal>
      )}
    </div>
  );
};
