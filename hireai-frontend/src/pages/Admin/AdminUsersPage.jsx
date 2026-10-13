import React, { useState, useEffect } from 'react';
import {
  Users,
  Search,
  Shield,
  ShieldAlert,
  ShieldCheck,
  UserCheck,
  UserX,
  RefreshCw,
  AlertTriangle,
  Lock,
  Unlock,
  CheckCircle2,
  XCircle,
  MoreVertical,
  Mail,
  Phone,
  Calendar,
} from 'lucide-react';
import { Card } from '@/components/ui/Card';
import { Badge } from '@/components/ui/Badge';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Modal } from '@/components/ui/Modal';
import { Skeleton } from '@/components/ui/Skeleton';
import { adminApi } from '@/services/api/admin.api';
import { useAuth } from '@/context/AuthContext';

export const AdminUsersPage = () => {
  const { user: currentUser } = useAuth();
  const [users, setUsers] = useState([]);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const [searchQuery, setSearchQuery] = useState('');
  const [roleFilter, setRoleFilter] = useState('ALL');
  const [enabledFilter, setEnabledFilter] = useState('ALL');

  const [isLoading, setIsLoading] = useState(true);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [actionSuccessMsg, setActionSuccessMsg] = useState('');
  const [actionErrorMsg, setActionErrorMsg] = useState('');

  // Confirmation Modals State
  const [roleModalUser, setRoleModalUser] = useState(null);
  const [newSelectedRole, setNewSelectedRole] = useState('ROLE_RECRUITER');
  const [statusModalUser, setStatusModalUser] = useState(null);
  const [isActionPending, setIsActionPending] = useState(false);

  const fetchUsers = async (silent = false) => {
    try {
      if (!silent) setIsLoading(true);
      else setIsRefreshing(true);
      setActionErrorMsg('');

      const res = await adminApi.getUsers({
        search: searchQuery,
        role: roleFilter,
        enabled: enabledFilter,
        page,
        size,
      });

      setUsers(res?.content || []);
      setTotalElements(res?.totalElements || 0);
      setTotalPages(res?.totalPages || 1);
    } catch (err) {
      console.error('Failed to load user directory:', err);
      setActionErrorMsg(err.message || 'Failed to query user accounts.');
    } finally {
      setIsLoading(false);
      setIsRefreshing(false);
    }
  };

  useEffect(() => {
    fetchUsers();
  }, [page, size, roleFilter, enabledFilter]);

  const handleSearchSubmit = (e) => {
    e.preventDefault();
    setPage(0);
    fetchUsers();
  };

  // Execute Role Update with Confirmation
  const handleConfirmRoleChange = async () => {
    if (!roleModalUser) return;
    try {
      setIsActionPending(true);
      setActionErrorMsg('');
      setActionSuccessMsg('');

      await adminApi.updateUserRole(roleModalUser.id, newSelectedRole);
      setActionSuccessMsg(`Successfully updated role of ${roleModalUser.firstName} ${roleModalUser.lastName} to ${newSelectedRole}.`);
      setRoleModalUser(null);
      await fetchUsers(true);
    } catch (err) {
      setActionErrorMsg(err.message || 'Failed to update user role.');
    } finally {
      setIsActionPending(false);
    }
  };

  // Execute Account Deactivate / Activate with Confirmation
  const handleConfirmStatusToggle = async () => {
    if (!statusModalUser) return;
    try {
      setIsActionPending(true);
      setActionErrorMsg('');
      setActionSuccessMsg('');

      const newEnabled = !statusModalUser.enabled;
      await adminApi.updateUserStatus(statusModalUser.id, {
        enabled: newEnabled,
        accountNonLocked: true,
      });

      setActionSuccessMsg(
        `User ${statusModalUser.firstName} ${statusModalUser.lastName} account is now ${newEnabled ? 'ACTIVE' : 'DEACTIVATED'}.`
      );
      setStatusModalUser(null);
      await fetchUsers(true);
    } catch (err) {
      setActionErrorMsg(err.message || 'Failed to update user status.');
    } finally {
      setIsActionPending(false);
    }
  };

  const getRoleBadge = (roleName) => {
    const role = (roleName || '').toUpperCase();
    if (role.includes('ADMIN')) {
      return (
        <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-md text-[10px] font-bold bg-brand-navy/15 text-brand-navy dark:text-brand-blue-light border border-brand-navy/30">
          <ShieldAlert className="w-3 h-3" /> System Admin
        </span>
      );
    }
    if (role.includes('RECRUITER')) {
      return (
        <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-md text-[10px] font-bold bg-brand-blue/15 text-brand-blue border border-brand-blue/20">
          <ShieldCheck className="w-3 h-3" /> Recruiter
        </span>
      );
    }
    return (
      <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-md text-[10px] font-semibold bg-surface-2 text-muted-foreground border border-border">
        Candidate
      </span>
    );
  };

  return (
    <div className="space-y-6 max-w-7xl mx-auto pb-16">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 p-5 sm:p-6 rounded-3xl glass border border-white/20 dark:border-white/10 shadow-xl">
        <div className="space-y-1">
          <h1 className="text-2xl font-bold font-heading text-foreground flex items-center gap-2.5">
            <Users className="w-6 h-6 text-brand-blue" />
            User Access & Role Governance
          </h1>
          <p className="text-xs sm:text-sm text-muted-foreground">
            Manage PostgreSQL accounts, assign administrative privileges, and soft-deactivate system access.
          </p>
        </div>

        <Button
          variant="outline"
          size="sm"
          onClick={() => fetchUsers(true)}
          disabled={isRefreshing}
          className="gap-2 rounded-xl text-xs font-semibold self-start sm:self-auto cursor-pointer"
        >
          <RefreshCw className={`w-3.5 h-3.5 ${isRefreshing ? 'animate-spin text-brand-blue' : ''}`} />
          {isRefreshing ? 'Syncing...' : 'Sync Directory'}
        </Button>
      </div>

      {/* Notifications */}
      {actionSuccessMsg && (
        <div className="p-4 rounded-2xl bg-emerald-500/10 border border-emerald-500/20 text-emerald-600 dark:text-emerald-400 text-xs font-semibold flex items-center gap-2.5">
          <CheckCircle2 className="w-4 h-4 shrink-0" />
          <span>{actionSuccessMsg}</span>
        </div>
      )}

      {actionErrorMsg && (
        <div className="p-4 rounded-2xl bg-rose-500/10 border border-rose-500/20 text-rose-600 dark:text-rose-400 text-xs font-semibold flex items-center gap-2.5">
          <XCircle className="w-4 h-4 shrink-0" />
          <span>{actionErrorMsg}</span>
        </div>
      )}

      {/* Search & Filter Bar */}
      <Card glass className="p-4 sm:p-5 rounded-2xl border border-white/20 dark:border-white/10 shadow-md">
        <form onSubmit={handleSearchSubmit} className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3">
          <div className="lg:col-span-2 relative">
            <Search className="w-4 h-4 text-muted-foreground absolute left-3 top-1/2 -translate-y-1/2 pointer-events-none" />
            <Input
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Search by name, email, or keywords..."
              className="pl-9 rounded-xl text-xs"
            />
          </div>

          <select
            value={roleFilter}
            onChange={(e) => {
              setRoleFilter(e.target.value);
              setPage(0);
            }}
            className="w-full px-3 py-2 rounded-xl text-xs font-medium bg-surface-2 border border-border text-foreground focus:outline-hidden focus:ring-2 focus:ring-brand-blue/30"
          >
            <option value="ALL">All Roles</option>
            <option value="ROLE_ADMIN">Admins Only</option>
            <option value="ROLE_RECRUITER">Recruiters Only</option>
            <option value="ROLE_CANDIDATE">Candidates Only</option>
          </select>

          <select
            value={enabledFilter}
            onChange={(e) => {
              setEnabledFilter(e.target.value);
              setPage(0);
            }}
            className="w-full px-3 py-2 rounded-xl text-xs font-medium bg-surface-2 border border-border text-foreground focus:outline-hidden focus:ring-2 focus:ring-brand-blue/30"
          >
            <option value="ALL">All Account Statuses</option>
            <option value="true">Active Accounts Only</option>
            <option value="false">Deactivated Only</option>
          </select>
        </form>
      </Card>

      {/* User Table Card */}
      <Card glass className="p-5 sm:p-6 rounded-3xl border border-white/20 dark:border-white/10 shadow-xl space-y-4">
        <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-1.5 pb-3 border-b border-border/50">
          <div className="text-xs text-muted-foreground font-semibold">
            Showing <span className="text-foreground font-bold">{users.length}</span> of{' '}
            <span className="text-foreground font-bold">{totalElements}</span> registered users
          </div>
          <div className="text-[11px] text-muted-foreground">
            Soft deactivation preserves candidate review audit histories.
          </div>
        </div>

        {isLoading ? (
          <div className="space-y-3 py-6">
            {[1, 2, 3, 4, 5].map((i) => (
              <Skeleton key={i} className="h-14 w-full rounded-xl" />
            ))}
          </div>
        ) : users.length === 0 ? (
          <div className="text-center py-16 space-y-2">
            <Users className="w-10 h-10 text-muted-foreground mx-auto opacity-40" />
            <h3 className="text-sm font-bold text-foreground">No accounts found matching filters</h3>
            <p className="text-xs text-muted-foreground">Try clearing search parameters.</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead>
                <tr className="border-b border-border/50 text-muted-foreground">
                  <th className="pb-3 font-bold">User</th>
                  <th className="pb-3 font-bold">Role</th>
                  <th className="pb-3 font-bold">Account Status</th>
                  <th className="pb-3 font-bold">Provider</th>
                  <th className="pb-3 font-bold">Created Date</th>
                  <th className="pb-3 font-bold text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border/40">
                {users.map((u) => {
                  const isSelf = currentUser?.email && currentUser.email.toLowerCase() === u.email.toLowerCase();
                  return (
                    <tr key={u.id} className="hover:bg-surface-2/50 transition-colors">
                      <td className="py-3">
                        <div className="flex items-center gap-2.5">
                          <div className="w-8 h-8 rounded-full bg-brand-blue/15 text-brand-blue font-bold flex items-center justify-center text-xs shrink-0">
                            {u.firstName?.[0] || 'U'}
                          </div>
                          <div>
                            <div className="font-bold text-foreground flex items-center gap-1.5">
                              {u.firstName} {u.lastName}
                              {isSelf && (
                                <Badge variant="outline" className="text-[9px] px-1.5 py-0 bg-brand-accent/20 text-slate-900 dark:text-brand-accent font-black">
                                  You
                                </Badge>
                              )}
                            </div>
                            <div className="text-[11px] text-muted-foreground flex items-center gap-1">
                              <Mail className="w-3 h-3" /> {u.email}
                            </div>
                          </div>
                        </div>
                      </td>

                      <td className="py-3">{getRoleBadge(u.role)}</td>

                      <td className="py-3">
                        <span
                          className={`inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-md text-[10px] font-bold ${
                            u.enabled
                              ? 'bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border border-emerald-500/20'
                              : 'bg-rose-500/10 text-rose-600 dark:text-rose-400 border border-rose-500/20'
                          }`}
                        >
                          <span className={`w-1.5 h-1.5 rounded-full ${u.enabled ? 'bg-emerald-500' : 'bg-rose-500'}`} />
                          {u.enabled ? 'Active' : 'Deactivated'}
                        </span>
                      </td>

                      <td className="py-3 font-mono text-[11px] text-muted-foreground uppercase">
                        {u.authProvider || 'LOCAL'}
                      </td>

                      <td className="py-3 font-mono text-[11px] text-muted-foreground">
                        {u.createdAt ? new Date(u.createdAt).toLocaleDateString() : 'Initial'}
                      </td>

                      <td className="py-3 text-right space-x-2">
                        {/* Change Role Button */}
                        <Button
                          variant="outline"
                          size="sm"
                          disabled={isSelf}
                          className="h-7 text-[11px] font-bold rounded-lg cursor-pointer"
                          onClick={() => {
                            setRoleModalUser(u);
                            setNewSelectedRole(u.role || 'ROLE_RECRUITER');
                          }}
                        >
                          <Shield className="w-3 h-3 mr-1" /> Change Role
                        </Button>

                        {/* Deactivate / Activate Button */}
                        <Button
                          variant={u.enabled ? 'outline' : 'default'}
                          size="sm"
                          disabled={isSelf}
                          className={`h-7 text-[11px] font-bold rounded-lg cursor-pointer ${
                            u.enabled
                              ? 'text-rose-600 dark:text-rose-400 hover:bg-rose-500/10 border-rose-500/20'
                              : 'bg-emerald-600 hover:bg-emerald-700 text-white'
                          }`}
                          onClick={() => setStatusModalUser(u)}
                        >
                          {u.enabled ? (
                            <>
                              <UserX className="w-3 h-3 mr-1" /> Deactivate
                            </>
                          ) : (
                            <>
                              <UserCheck className="w-3 h-3 mr-1" /> Activate
                            </>
                          )}
                        </Button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}

        {/* Pagination */}
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
                Previous
              </Button>
              <Button
                variant="outline"
                size="sm"
                onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
                disabled={page >= totalPages - 1}
                className="rounded-xl text-xs cursor-pointer"
              >
                Next
              </Button>
            </div>
          </div>
        )}
      </Card>

      {/* Confirmation Modal 1: Change User Role */}
      {roleModalUser && (
        <Modal
          isOpen={Boolean(roleModalUser)}
          onClose={() => setRoleModalUser(null)}
          title="Confirm Role Change"
        >
          <div className="space-y-4 text-xs">
            <div className="p-3.5 rounded-2xl bg-amber-500/10 border border-amber-500/20 text-amber-600 dark:text-amber-400 space-y-1">
              <div className="flex items-center gap-1.5 font-bold">
                <AlertTriangle className="w-4 h-4" /> Elevation / Permission Notice
              </div>
              <p>
                Changing the role of <strong>{roleModalUser.firstName} {roleModalUser.lastName}</strong> ({roleModalUser.email}) directly modifies system authorization boundaries.
              </p>
            </div>

            <div className="space-y-2">
              <label className="font-bold text-foreground block">Select New Role:</label>
              <div className="grid grid-cols-1 gap-2">
                {[
                  {
                    id: 'ROLE_ADMIN',
                    label: 'System Administrator',
                    desc: 'Full operational access to AI engine telemetry, user roles, quotas, and logs.',
                  },
                  {
                    id: 'ROLE_RECRUITER',
                    label: 'Recruiter',
                    desc: 'Access to candidate applications, job postings, ATS scoring, and recruiter inbox.',
                  },
                  {
                    id: 'ROLE_CANDIDATE',
                    label: 'Candidate',
                    desc: 'Standard job seeker portal access to browse open jobs and track applications.',
                  },
                ].map((r) => (
                  <label
                    key={r.id}
                    className={`flex items-start gap-3 p-3 rounded-xl border cursor-pointer transition-all ${
                      newSelectedRole === r.id
                        ? 'bg-brand-blue/10 border-brand-blue text-foreground'
                        : 'bg-surface-2 border-border text-muted-foreground hover:border-border/80'
                    }`}
                  >
                    <input
                      type="radio"
                      name="userRole"
                      value={r.id}
                      checked={newSelectedRole === r.id}
                      onChange={(e) => setNewSelectedRole(e.target.value)}
                      className="mt-0.5 text-brand-blue"
                    />
                    <div>
                      <div className="font-bold text-foreground">{r.label}</div>
                      <div className="text-[11px] text-muted-foreground">{r.desc}</div>
                    </div>
                  </label>
                ))}
              </div>
            </div>

            <div className="flex justify-end gap-2 pt-3 border-t border-border">
              <Button
                variant="outline"
                size="sm"
                onClick={() => setRoleModalUser(null)}
                disabled={isActionPending}
                className="rounded-xl text-xs"
              >
                Cancel
              </Button>
              <Button
                size="sm"
                onClick={handleConfirmRoleChange}
                disabled={isActionPending}
                className="rounded-xl text-xs font-bold shadow-md bg-brand-blue text-white"
              >
                {isActionPending ? 'Updating Role...' : 'Confirm Role Assignment'}
              </Button>
            </div>
          </div>
        </Modal>
      )}

      {/* Confirmation Modal 2: Deactivate / Activate Account */}
      {statusModalUser && (
        <Modal
          isOpen={Boolean(statusModalUser)}
          onClose={() => setStatusModalUser(null)}
          title={statusModalUser.enabled ? 'Confirm Account Deactivation' : 'Confirm Account Activation'}
        >
          <div className="space-y-4 text-xs">
            {statusModalUser.enabled ? (
              <div className="p-3.5 rounded-2xl bg-rose-500/10 border border-rose-500/20 text-rose-600 dark:text-rose-400 space-y-2">
                <div className="flex items-center gap-1.5 font-bold">
                  <AlertTriangle className="w-4 h-4" /> Deactivation Policy
                </div>
                <p>
                  You are about to soft-deactivate <strong>{statusModalUser.firstName} {statusModalUser.lastName}</strong> ({statusModalUser.email}).
                </p>
                <p className="text-[11px]">
                  This revokes login access immediately while retaining full audit history, job applications, and screening notes intact.
                </p>
              </div>
            ) : (
              <div className="p-3.5 rounded-2xl bg-emerald-500/10 border border-emerald-500/20 text-emerald-600 dark:text-emerald-400 space-y-1">
                <div className="flex items-center gap-1.5 font-bold">
                  <CheckCircle2 className="w-4 h-4" /> Restore Access
                </div>
                <p>
                  Re-enabling account for <strong>{statusModalUser.firstName} {statusModalUser.lastName}</strong> will allow them to log in again and reset any previous login lockouts.
                </p>
              </div>
            )}

            <div className="flex justify-end gap-2 pt-3 border-t border-border">
              <Button
                variant="outline"
                size="sm"
                onClick={() => setStatusModalUser(null)}
                disabled={isActionPending}
                className="rounded-xl text-xs"
              >
                Cancel
              </Button>
              <Button
                size="sm"
                onClick={handleConfirmStatusToggle}
                disabled={isActionPending}
                className={`rounded-xl text-xs font-bold shadow-md ${
                  statusModalUser.enabled
                    ? 'bg-rose-600 hover:bg-rose-700 text-white'
                    : 'bg-emerald-600 hover:bg-emerald-700 text-white'
                }`}
              >
                {isActionPending
                  ? 'Processing...'
                  : statusModalUser.enabled
                  ? 'Confirm Deactivation'
                  : 'Confirm Activation'}
              </Button>
            </div>
          </div>
        </Modal>
      )}
    </div>
  );
};
