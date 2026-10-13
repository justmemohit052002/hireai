import React, { useState, useEffect } from 'react';
import {
  ShieldCheck,
  RefreshCw,
  Clock,
  User,
  ArrowRight,
  Eye,
  ChevronLeft,
  ChevronRight,
  Filter,
  FileText,
  AlertCircle,
} from 'lucide-react';
import { Card } from '@/components/ui/Card';
import { Badge } from '@/components/ui/Badge';
import { Button } from '@/components/ui/Button';
import { Modal } from '@/components/ui/Modal';
import { Skeleton } from '@/components/ui/Skeleton';
import { adminApi } from '@/services/api/admin.api';

export const AdminAuditPage = () => {
  const [logs, setLogs] = useState([]);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(15);
  const [isLoading, setIsLoading] = useState(true);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [error, setError] = useState(null);
  const [selectedLog, setSelectedLog] = useState(null);

  const fetchAuditLogs = async (silent = false) => {
    try {
      if (!silent) setIsLoading(true);
      else setIsRefreshing(true);
      setError(null);

      const res = await adminApi.getAuditLogs({ page, size });
      const items = res?.content || res?.items || (Array.isArray(res) ? res : []);
      setLogs(items);
      setTotalElements(res?.totalElements ?? items.length);
      setTotalPages(res?.totalPages ?? 1);
    } catch (err) {
      console.error('Failed to load audit logs:', err);
      setError(err.message || 'Unable to load administrative audit logs.');
    } finally {
      setIsLoading(false);
      setIsRefreshing(false);
    }
  };

  useEffect(() => {
    fetchAuditLogs();
  }, [page, size]);

  const getActionBadge = (action) => {
    const act = (action || '').toUpperCase();
    if (act.includes('ROLE')) {
      return (
        <Badge variant="outline" className="text-[10px] font-bold bg-brand-blue/10 text-brand-blue border-brand-blue/30 font-mono">
          {action}
        </Badge>
      );
    }
    if (act.includes('STATUS') || act.includes('LOCK')) {
      return (
        <Badge variant="outline" className="text-[10px] font-bold bg-amber-500/10 text-amber-600 dark:text-amber-400 border-amber-500/30 font-mono">
          {action}
        </Badge>
      );
    }
    if (act.includes('VIEW')) {
      return (
        <Badge variant="outline" className="text-[10px] font-bold bg-slate-500/10 text-slate-700 dark:text-slate-300 border-slate-500/30 font-mono">
          {action}
        </Badge>
      );
    }
    return (
      <Badge variant="outline" className="text-[10px] font-bold bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border-emerald-500/30 font-mono">
        {action}
      </Badge>
    );
  };

  const formatValueSnippet = (val) => {
    if (!val && val !== 0 && val !== false) return '-';
    const str = typeof val === 'object' ? JSON.stringify(val) : String(val);
    if (str.length > 35) {
      return `${str.substring(0, 35)}...`;
    }
    return str;
  };

  return (
    <div className="space-y-6 max-w-7xl mx-auto pb-16">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 p-5 sm:p-6 rounded-3xl glass border border-white/20 dark:border-white/10 shadow-xl">
        <div className="space-y-1">
          <h1 className="text-2xl font-bold font-heading text-foreground flex items-center gap-2.5">
            <ShieldCheck className="w-6 h-6 text-brand-blue" />
            Administrative Audit Trail
          </h1>
          <p className="text-xs sm:text-sm text-muted-foreground">
            Immutable log of administrative operations, role modifications, account locks, and telemetry access.
          </p>
        </div>

        <Button
          variant="outline"
          size="sm"
          onClick={() => fetchAuditLogs(true)}
          disabled={isRefreshing}
          className="gap-2 rounded-xl text-xs font-semibold self-start sm:self-auto cursor-pointer"
        >
          <RefreshCw className={`w-3.5 h-3.5 ${isRefreshing ? 'animate-spin text-brand-blue' : ''}`} />
          {isRefreshing ? 'Refreshing...' : 'Refresh Audit'}
        </Button>
      </div>

      {error && (
        <div className="p-4 rounded-2xl bg-amber-500/10 border border-amber-500/30 text-amber-700 dark:text-amber-300 text-xs font-medium flex items-center gap-3">
          <AlertCircle className="w-5 h-5 shrink-0 text-amber-500" />
          <span>{error}</span>
        </div>
      )}

      {/* Main Table Card */}
      <Card glass className="p-5 sm:p-6 rounded-3xl border border-white/20 dark:border-white/10 shadow-xl space-y-4">
        <div className="flex items-center justify-between pb-3 border-b border-border/50">
          <div className="text-xs text-muted-foreground font-semibold">
            Showing <span className="text-foreground font-bold">{logs.length}</span> of{' '}
            <span className="text-foreground font-bold">{totalElements}</span> recorded audit events
          </div>
          <div className="flex items-center gap-2">
            <span className="text-xs text-muted-foreground">Rows per page:</span>
            <select
              value={size}
              onChange={(e) => {
                setSize(Number(e.target.value));
                setPage(0);
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
            <FileText className="w-10 h-10 text-muted-foreground mx-auto opacity-40" />
            <h3 className="text-sm font-bold text-foreground">No audit entries found</h3>
            <p className="text-xs text-muted-foreground">Administrative actions will appear here when recorded.</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead>
                <tr className="border-b border-border/50 text-muted-foreground">
                  <th className="pb-3 font-bold">Timestamp</th>
                  <th className="pb-3 font-bold">Actor Email</th>
                  <th className="pb-3 font-bold">Action</th>
                  <th className="pb-3 font-bold">Target User ID</th>
                  <th className="pb-3 font-bold">Old Value</th>
                  <th className="pb-3 font-bold">New Value</th>
                  <th className="pb-3 font-bold text-right">Details</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border/40">
                {logs.map((log) => (
                  <tr
                    key={log.id || `${log.createdAt}-${Math.random()}`}
                    className="hover:bg-surface-2/50 transition-colors cursor-pointer group"
                    onClick={() => setSelectedLog(log)}
                  >
                    <td className="py-3 font-mono text-[11px] text-muted-foreground whitespace-nowrap">
                      {log.createdAt ? new Date(log.createdAt).toLocaleString() : 'N/A'}
                    </td>
                    <td className="py-3 font-medium text-foreground whitespace-nowrap">
                      <div className="flex items-center gap-1.5">
                        <User className="w-3.5 h-3.5 text-brand-blue shrink-0" />
                        <span>{log.actorEmail || 'System'}</span>
                      </div>
                    </td>
                    <td className="py-3 whitespace-nowrap">
                      {getActionBadge(log.action)}
                    </td>
                    <td className="py-3 font-mono text-[11px] text-muted-foreground whitespace-nowrap">
                      {log.targetUserId ? (
                        <span title={log.targetUserId}>
                          {log.targetUserId.substring(0, 8)}...
                        </span>
                      ) : (
                        '-'
                      )}
                    </td>
                    <td className="py-3 font-mono text-[11px] text-muted-foreground max-w-[150px] truncate">
                      {formatValueSnippet(log.oldValue)}
                    </td>
                    <td className="py-3 font-mono text-[11px] text-foreground max-w-[150px] truncate font-semibold">
                      {formatValueSnippet(log.newValue)}
                    </td>
                    <td className="py-3 text-right whitespace-nowrap">
                      <Button
                        variant="ghost"
                        size="sm"
                        className="h-7 text-[11px] font-bold text-brand-blue hover:text-brand-blue/80"
                        onClick={(e) => {
                          e.stopPropagation();
                          setSelectedLog(log);
                        }}
                      >
                        <Eye className="w-3 h-3 mr-1" /> View
                      </Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {/* Pagination Navigation */}
        {totalPages > 1 && (
          <div className="flex items-center justify-between pt-4 border-t border-border/50 text-xs">
            <span className="text-muted-foreground">
              Page <span className="font-bold text-foreground">{page + 1}</span> of{' '}
              <span className="font-bold text-foreground">{totalPages}</span>
            </span>

            <div className="flex items-center gap-2">
              <Button
                variant="outline"
                size="sm"
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                disabled={page <= 0}
                className="rounded-xl text-xs cursor-pointer"
              >
                <ChevronLeft className="w-3.5 h-3.5 mr-1" /> Prev
              </Button>
              <Button
                variant="outline"
                size="sm"
                onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
                disabled={page >= totalPages - 1}
                className="rounded-xl text-xs cursor-pointer"
              >
                Next <ChevronRight className="w-3.5 h-3.5 ml-1" />
              </Button>
            </div>
          </div>
        )}
      </Card>

      {/* Audit Detail Modal */}
      {selectedLog && (
        <Modal
          isOpen={Boolean(selectedLog)}
          onClose={() => setSelectedLog(null)}
          title={`Audit Event Details: ${selectedLog.action || 'Administrative Event'}`}
        >
          <div className="space-y-4 text-xs">
            <div className="grid grid-cols-2 gap-3 p-3.5 rounded-2xl bg-surface-2 border border-white/10">
              <div>
                <span className="text-[10px] text-muted-foreground uppercase font-bold">Event ID</span>
                <p className="font-mono text-[11px] text-foreground truncate">{selectedLog.id || '-'}</p>
              </div>
              <div>
                <span className="text-[10px] text-muted-foreground uppercase font-bold">Timestamp</span>
                <p className="font-mono text-[11px] text-foreground">
                  {selectedLog.createdAt ? new Date(selectedLog.createdAt).toLocaleString() : 'N/A'}
                </p>
              </div>
              <div>
                <span className="text-[10px] text-muted-foreground uppercase font-bold">Actor Email</span>
                <p className="font-semibold text-foreground truncate">{selectedLog.actorEmail || 'System'}</p>
              </div>
              <div>
                <span className="text-[10px] text-muted-foreground uppercase font-bold">Actor User ID</span>
                <p className="font-mono text-[11px] text-foreground truncate">{selectedLog.actorUserId || '-'}</p>
              </div>
              <div>
                <span className="text-[10px] text-muted-foreground uppercase font-bold">Action</span>
                <div className="mt-0.5">{getActionBadge(selectedLog.action)}</div>
              </div>
              <div>
                <span className="text-[10px] text-muted-foreground uppercase font-bold">Target User ID</span>
                <p className="font-mono text-[11px] text-foreground truncate">{selectedLog.targetUserId || '-'}</p>
              </div>
            </div>

            {/* Old Value */}
            <div className="space-y-1">
              <span className="text-[11px] font-bold text-muted-foreground">Previous Value</span>
              <pre className="p-3 rounded-xl bg-muted/60 font-mono text-[11px] text-foreground/90 whitespace-pre-wrap break-words max-h-40 overflow-y-auto border border-border">
                {selectedLog.oldValue ?? '(None)'}
              </pre>
            </div>

            {/* New Value */}
            <div className="space-y-1">
              <span className="text-[11px] font-bold text-foreground">New Value</span>
              <pre className="p-3 rounded-xl bg-muted/60 font-mono text-[11px] text-emerald-600 dark:text-emerald-400 whitespace-pre-wrap break-words max-h-40 overflow-y-auto border border-border">
                {selectedLog.newValue ?? '(None)'}
              </pre>
            </div>

            <div className="flex justify-end pt-2">
              <Button size="sm" variant="outline" onClick={() => setSelectedLog(null)} className="rounded-xl text-xs">
                Close
              </Button>
            </div>
          </div>
        </Modal>
      )}
    </div>
  );
};
