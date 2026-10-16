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
  Mail,
  Calendar,
  Eye,
  EyeOff,
  UserPlus,
  KeyRound,
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
  const { user: currentUser, role: currentRole, updateMustChangePassword } = useAuth();
  const isAdmin = currentRole === 'admin';
  const isSubAdmin = currentRole === 'sub_admin';

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

  // Add Sub-Admin Modal State
  const [isCreateSubAdminOpen, setIsCreateSubAdminOpen] = useState(false);
  const [subAdminForm, setSubAdminForm] = useState({
    name: '',
    email: '',
    password: '',
    confirmPassword: '',
  });
  const [showSubAdminPassword, setShowSubAdminPassword] = useState(false);
  const [subAdminConfirmed, setSubAdminConfirmed] = useState(false);
  const [isCreatingSubAdmin, setIsCreatingSubAdmin] = useState(false);
  const [createSubAdminError, setCreateSubAdminError] = useState('');

  // Forced First Login Password Change Modal State
  const [showMustChangePasswordModal, setShowMustChangePasswordModal] = useState(
    Boolean(currentUser?.mustChangePassword)
  );
  const [changePasswordForm, setChangePasswordForm] = useState({
    currentPassword: '',
    newPassword: '',
    confirmPassword: '',
  });
  const [showCurrentPassword, setShowCurrentPassword] = useState(false);
  const [showNewPassword, setShowNewPassword] = useState(false);
  const [isChangingPassword, setIsChangingPassword] = useState(false);
  const [changePasswordError, setChangePasswordError] = useState('');

  // Check whenever currentUser changes
  useEffect(() => {
    if (currentUser?.mustChangePassword) {
      setShowMustChangePasswordModal(true);
    }
  }, [currentUser?.mustChangePassword]);

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
      setActionSuccessMsg(
        `Successfully updated role of ${roleModalUser.firstName} ${roleModalUser.lastName} to ${newSelectedRole}.`
      );
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
        `User ${statusModalUser.firstName} ${statusModalUser.lastName} account is now ${
          newEnabled ? 'ACTIVE' : 'DEACTIVATED'
        }.`
      );
      setStatusModalUser(null);
      await fetchUsers(true);
    } catch (err) {
      setActionErrorMsg(err.message || 'Failed to update user status.');
    } finally {
      setIsActionPending(false);
    }
  };

  // Handle Create Sub-Admin Submission
  const handleCreateSubAdmin = async (e) => {
    e.preventDefault();
    setCreateSubAdminError('');

    if (!subAdminForm.name.trim()) {
      setCreateSubAdminError('Full name is required.');
      return;
    }
    if (!subAdminForm.email.trim()) {
      setCreateSubAdminError('Email address is required.');
      return;
    }
    if (subAdminForm.password.length < 8) {
      setCreateSubAdminError('Password must be at least 8 characters long.');
      return;
    }
    if (subAdminForm.password !== subAdminForm.confirmPassword) {
      setCreateSubAdminError('Password and confirmation do not match.');
      return;
    }
    if (!subAdminConfirmed) {
      setCreateSubAdminError('Please confirm the acknowledgment checkbox.');
      return;
    }

    try {
      setIsCreatingSubAdmin(true);
      const rawName = subAdminForm.name.trim();
      const nameParts = rawName.split(/\s+/);
      const firstName = nameParts[0] || '';
      const lastName = nameParts.length > 1 ? nameParts.slice(1).join(' ') : nameParts[0] || '';

      await adminApi.createSubAdmin({
        firstName,
        lastName,
        email: subAdminForm.email.trim().toLowerCase(),
        password: subAdminForm.password,
        confirmPassword: subAdminForm.confirmPassword,
      });

      setActionSuccessMsg(`Sub-Administrator created successfully for ${subAdminForm.email}.`);
      setIsCreateSubAdminOpen(false);
      setSubAdminForm({ name: '', email: '', password: '', confirmPassword: '' });
      setSubAdminConfirmed(false);
      await fetchUsers(true);
    } catch (err) {
      setCreateSubAdminError(err.message || 'Failed to create sub-admin.');
    } finally {
      setIsCreatingSubAdmin(false);
    }
  };

  // Handle Forced First Login Password Change
  const handleChangePasswordSubmit = async (e) => {
    e.preventDefault();
    setChangePasswordError('');

    if (!changePasswordForm.currentPassword) {
      setChangePasswordError('Current password is required.');
      return;
    }
    if (changePasswordForm.newPassword.length < 8) {
      setChangePasswordError('New password must be at least 8 characters long.');
      return;
    }
    if (changePasswordForm.newPassword !== changePasswordForm.confirmPassword) {
      setChangePasswordError('New password and confirmation do not match.');
      return;
    }

    try {
      setIsChangingPassword(true);
      await adminApi.changePassword({
        currentPassword: changePasswordForm.currentPassword,
        newPassword: changePasswordForm.newPassword,
        confirmPassword: changePasswordForm.confirmPassword,
      });

      if (updateMustChangePassword) {
        updateMustChangePassword(false);
      }
      setShowMustChangePasswordModal(false);
      setActionSuccessMsg('Your password has been changed successfully.');
    } catch (err) {
      setChangePasswordError(err.message || 'Failed to update password.');
    } finally {
      setIsChangingPassword(false);
    }
  };

  // Calculate password strength score (0-4)
  const getPasswordStrength = (pwd) => {
    let score = 0;
    if (pwd.length >= 8) score++;
    if (/[A-Z]/.test(pwd)) score++;
    if (/[0-9]/.test(pwd)) score++;
    if (/[^A-Za-z0-9]/.test(pwd)) score++;
    return score;
  };

  const pwdScore = getPasswordStrength(subAdminForm.password);

  const getRoleBadge = (roleName) => {
    const role = (roleName || '').toUpperCase();
    if (role.includes('SUB_ADMIN')) {
      return (
        <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-md text-[10px] font-bold bg-sky-500/15 text-sky-700 dark:text-sky-300 border border-sky-500/30">
          <Shield className="w-3 h-3" /> Sub-Admin
        </span>
      );
    }
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
            Manage system accounts, enforce least-privilege administrative access, and review login activity.
          </p>
        </div>

        <div className="flex items-center gap-3 self-start sm:self-auto">
          {isAdmin && (
            <Button
              size="sm"
              onClick={() => setIsCreateSubAdminOpen(true)}
              className="gap-2 rounded-xl text-xs font-bold shadow-md cursor-pointer bg-brand-blue hover:bg-brand-blue/90 text-white"
            >
              <UserPlus className="w-3.5 h-3.5" /> Add Sub-Admin
            </Button>
          )}

          <Button
            variant="outline"
            size="sm"
            onClick={() => fetchUsers(true)}
            disabled={isRefreshing}
            className="gap-2 rounded-xl text-xs font-semibold cursor-pointer border-white/20"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${isRefreshing ? 'animate-spin text-brand-blue' : ''}`} />
            {isRefreshing ? 'Syncing...' : 'Sync Directory'}
          </Button>
        </div>
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
            <option value="ROLE_SUB_ADMIN">Sub-Admins Only</option>
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
            {isSubAdmin ? 'Sub-Admin Mode: Role changes and admin modifications restricted.' : 'Administrator Mode: Full privileges.'}
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
                  <th className="pb-3 font-bold">Status</th>
                  <th className="pb-3 font-bold">Last Login</th>
                  <th className="pb-3 font-bold">Provider</th>
                  <th className="pb-3 font-bold">Created</th>
                  <th className="pb-3 font-bold text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border/40">
                {users.map((u) => {
                  const isSelf = currentUser?.email && currentUser.email.toLowerCase() === u.email.toLowerCase();
                  const targetIsAdminOrSubAdmin = (u.role || '').includes('ADMIN');
                  // Sub-admins cannot modify admins or other sub-admins, and cannot change roles
                  const canModifyStatus = isAdmin ? !isSelf : (!isSelf && !targetIsAdminOrSubAdmin);
                  const canChangeRole = isAdmin && !isSelf;

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
                              {u.mustChangePassword && (
                                <span className="text-[9px] px-1 py-0.2 rounded bg-amber-500/15 text-amber-700 dark:text-amber-300 font-bold border border-amber-500/20">
                                  Reset Req
                                </span>
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

                      {/* Last Login Column */}
                      <td className="py-3 font-mono text-[11px] text-muted-foreground">
                        {u.lastLoginAt ? new Date(u.lastLoginAt).toLocaleString() : 'Never logged in'}
                      </td>

                      <td className="py-3 font-mono text-[11px] text-muted-foreground uppercase">
                        {u.authProvider || 'LOCAL'}
                      </td>

                      <td className="py-3 font-mono text-[11px] text-muted-foreground">
                        {u.createdAt ? new Date(u.createdAt).toLocaleDateString() : 'Initial'}
                      </td>

                      <td className="py-3 text-right space-x-2">
                        {/* Change Role Button (Admin only) */}
                        {isAdmin && (
                          <Button
                            variant="outline"
                            size="sm"
                            disabled={!canChangeRole}
                            className="h-7 text-[11px] font-bold rounded-lg cursor-pointer"
                            onClick={() => {
                              setRoleModalUser(u);
                              setNewSelectedRole(u.role || 'ROLE_RECRUITER');
                            }}
                          >
                            <Shield className="w-3 h-3 mr-1" /> Change Role
                          </Button>
                        )}

                        {/* Deactivate / Activate Button */}
                        <Button
                          variant={u.enabled ? 'outline' : 'default'}
                          size="sm"
                          disabled={!canModifyStatus}
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

        {/* Pagination controls */}
        {totalPages > 1 && (
          <div className="flex items-center justify-between pt-4 border-t border-border/50 text-xs">
            <span className="text-muted-foreground">
              Page <span className="font-bold text-foreground">{page + 1}</span> of {totalPages}
            </span>
            <div className="flex items-center gap-2">
              <Button
                variant="outline"
                size="sm"
                disabled={page === 0}
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                className="h-7 px-3 rounded-lg text-xs"
              >
                Previous
              </Button>
              <Button
                variant="outline"
                size="sm"
                disabled={page >= totalPages - 1}
                onClick={() => setPage((p) => p + 1)}
                className="h-7 px-3 rounded-lg text-xs"
              >
                Next
              </Button>
            </div>
          </div>
        )}
      </Card>

      {/* Modal: Change Role Confirmation */}
      {roleModalUser && (
        <Modal
          isOpen={Boolean(roleModalUser)}
          onClose={() => setRoleModalUser(null)}
          title={`Update User Role: ${roleModalUser.firstName} ${roleModalUser.lastName}`}
        >
          <div className="space-y-4 text-xs">
            <p className="text-muted-foreground">
              Assigning a new role alters platform access permissions immediately.
            </p>

            <div className="space-y-1.5">
              <label className="font-bold text-foreground">Select New Role</label>
              <select
                value={newSelectedRole}
                onChange={(e) => setNewSelectedRole(e.target.value)}
                className="w-full px-3 py-2 rounded-xl text-xs font-semibold bg-surface-2 border border-border text-foreground focus:outline-hidden"
              >
                <option value="ROLE_CANDIDATE">Candidate (Job seeker)</option>
                <option value="ROLE_RECRUITER">Recruiter (Job poster / ATS)</option>
                <option value="ROLE_SUB_ADMIN">Sub-Administrator (Ops & User Management)</option>
                <option value="ROLE_ADMIN">System Administrator (Full Privileges)</option>
              </select>
            </div>

            <div className="p-3 rounded-xl bg-amber-500/10 border border-amber-500/20 text-amber-700 dark:text-amber-300 space-y-1">
              <div className="font-bold flex items-center gap-1.5">
                <AlertTriangle className="w-3.5 h-3.5" /> Confirmation Warning
              </div>
              <p>
                Changing role will invalidate active user sessions and require them to sign in again with their new privileges.
              </p>
            </div>

            <div className="flex justify-end gap-2 pt-2">
              <Button
                variant="outline"
                size="sm"
                onClick={() => setRoleModalUser(null)}
                className="rounded-xl text-xs"
              >
                Cancel
              </Button>
              <Button
                size="sm"
                disabled={isActionPending}
                onClick={handleConfirmRoleChange}
                className="rounded-xl text-xs font-bold bg-brand-blue text-white"
              >
                {isActionPending ? 'Updating...' : 'Confirm Role Change'}
              </Button>
            </div>
          </div>
        </Modal>
      )}

      {/* Modal: Deactivate / Activate Account Confirmation */}
      {statusModalUser && (
        <Modal
          isOpen={Boolean(statusModalUser)}
          onClose={() => setStatusModalUser(null)}
          title={statusModalUser.enabled ? 'Deactivate User Account' : 'Reactivate User Account'}
        >
          <div className="space-y-4 text-xs">
            <p className="text-muted-foreground">
              Are you sure you want to {statusModalUser.enabled ? 'deactivate' : 'reactivate'} the account for{' '}
              <strong className="text-foreground">{statusModalUser.email}</strong>?
            </p>

            <div
              className={`p-3.5 rounded-xl border text-xs space-y-1 ${
                statusModalUser.enabled
                  ? 'bg-rose-500/10 border-rose-500/20 text-rose-700 dark:text-rose-300'
                  : 'bg-emerald-500/10 border-emerald-500/20 text-emerald-700 dark:text-emerald-300'
              }`}
            >
              <div className="font-bold flex items-center gap-1.5">
                <AlertTriangle className="w-3.5 h-3.5" />
                {statusModalUser.enabled ? 'Immediate Access Revocation' : 'Access Restoration'}
              </div>
              <p>
                {statusModalUser.enabled
                  ? 'Deactivation will immediately revoke all active refresh tokens and block login attempts. Historical candidate and job records will remain intact.'
                  : 'Reactivation will allow the user to sign in immediately.'}
              </p>
            </div>

            <div className="flex justify-end gap-2 pt-2">
              <Button
                variant="outline"
                size="sm"
                onClick={() => setStatusModalUser(null)}
                className="rounded-xl text-xs"
              >
                Cancel
              </Button>
              <Button
                size="sm"
                disabled={isActionPending}
                onClick={handleConfirmStatusToggle}
                className={`rounded-xl text-xs font-bold ${
                  statusModalUser.enabled
                    ? 'bg-rose-600 hover:bg-rose-700 text-white'
                    : 'bg-emerald-600 hover:bg-emerald-700 text-white'
                }`}
              >
                {isActionPending
                  ? 'Processing...'
                  : statusModalUser.enabled
                  ? 'Confirm Deactivation'
                  : 'Confirm Reactivation'}
              </Button>
            </div>
          </div>
        </Modal>
      )}

      {/* Modal: Create Sub-Administrator */}
      {isCreateSubAdminOpen && (
        <Modal
          isOpen={isCreateSubAdminOpen}
          onClose={() => {
            setIsCreateSubAdminOpen(false);
            setCreateSubAdminError('');
          }}
          title="Create Sub-Administrator Account"
        >
          <form onSubmit={handleCreateSubAdmin} className="space-y-4 text-xs">
            <p className="text-muted-foreground">
              Provision a new administrative account with limited ops privileges. The new sub-admin will be required to change their temporary password upon their first sign-in.
            </p>

            {createSubAdminError && (
              <div className="p-3 rounded-xl bg-rose-500/10 border border-rose-500/20 text-rose-600 dark:text-rose-400 text-xs font-semibold flex items-center gap-2">
                <XCircle className="w-4 h-4 shrink-0" />
                <span>{createSubAdminError}</span>
              </div>
            )}

            <div className="space-y-1.5">
              <label className="font-bold text-foreground">Full Name</label>
              <Input
                value={subAdminForm.name}
                onChange={(e) => setSubAdminForm({ ...subAdminForm, name: e.target.value })}
                placeholder="e.g. Alex Morgan"
                required
                className="rounded-xl text-xs"
              />
            </div>

            <div className="space-y-1.5">
              <label className="font-bold text-foreground">Email Address</label>
              <Input
                type="email"
                value={subAdminForm.email}
                onChange={(e) => setSubAdminForm({ ...subAdminForm, email: e.target.value })}
                placeholder="subadmin@vionsys.com"
                required
                className="rounded-xl text-xs"
              />
            </div>

            <div className="space-y-1.5">
              <label className="font-bold text-foreground">Temporary Initial Password</label>
              <div className="relative">
                <Input
                  type={showSubAdminPassword ? 'text' : 'password'}
                  value={subAdminForm.password}
                  onChange={(e) => setSubAdminForm({ ...subAdminForm, password: e.target.value })}
                  placeholder="Min 8 characters..."
                  required
                  className="rounded-xl text-xs pr-9"
                />
                <button
                  type="button"
                  onClick={() => setShowSubAdminPassword(!showSubAdminPassword)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground cursor-pointer"
                >
                  {showSubAdminPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                </button>
              </div>

              {/* Password strength indicator */}
              <div className="space-y-1 pt-1">
                <div className="h-1.5 w-full bg-surface-2 rounded-full overflow-hidden flex gap-1">
                  {[1, 2, 3, 4].map((step) => (
                    <div
                      key={step}
                      className={`h-full flex-1 rounded-full transition-all ${
                        pwdScore >= step
                          ? step <= 2
                            ? 'bg-amber-500'
                            : 'bg-emerald-500'
                          : 'bg-muted'
                      }`}
                    />
                  ))}
                </div>
                <div className="text-[10px] text-muted-foreground flex justify-between">
                  <span>Strength: {pwdScore <= 1 ? 'Weak' : pwdScore <= 3 ? 'Good' : 'Strong'}</span>
                  <span>Min 8 chars, mixed case, number, symbol</span>
                </div>
              </div>
            </div>

            <div className="space-y-1.5">
              <label className="font-bold text-foreground">Confirm Temporary Password</label>
              <Input
                type="password"
                value={subAdminForm.confirmPassword}
                onChange={(e) => setSubAdminForm({ ...subAdminForm, confirmPassword: e.target.value })}
                placeholder="Re-enter temporary password..."
                required
                className="rounded-xl text-xs"
              />
            </div>

            <div className="p-3.5 rounded-xl bg-surface-2 border border-border/40 space-y-2">
              <label className="flex items-start gap-2.5 cursor-pointer">
                <input
                  type="checkbox"
                  checked={subAdminConfirmed}
                  onChange={(e) => setSubAdminConfirmed(e.target.checked)}
                  className="mt-0.5 rounded text-brand-blue focus:ring-brand-blue"
                />
                <span className="text-[11px] text-foreground font-medium leading-relaxed">
                  I confirm that this account will have SUB_ADMIN access to system telemetry, call logs, and candidate/recruiter account management, and must change password upon first sign-in.
                </span>
              </label>
            </div>

            <div className="flex justify-end gap-2 pt-2">
              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={() => setIsCreateSubAdminOpen(false)}
                className="rounded-xl text-xs"
              >
                Cancel
              </Button>
              <Button
                type="submit"
                size="sm"
                disabled={isCreatingSubAdmin}
                className="rounded-xl text-xs font-bold bg-brand-blue text-white"
              >
                {isCreatingSubAdmin ? 'Creating...' : 'Create Sub-Administrator'}
              </Button>
            </div>
          </form>
        </Modal>
      )}

      {/* Modal: Mandatory Password Change on First Login */}
      {showMustChangePasswordModal && (
        <Modal
          isOpen={showMustChangePasswordModal}
          onClose={() => {}} // Non-dismissible
          title="Password Change Required"
        >
          <form onSubmit={handleChangePasswordSubmit} className="space-y-4 text-xs">
            <div className="p-3.5 rounded-xl bg-amber-500/10 border border-amber-500/20 text-amber-800 dark:text-amber-200 space-y-1">
              <div className="font-bold flex items-center gap-1.5">
                <KeyRound className="w-4 h-4 text-amber-500" /> First Sign-In Security Policy
              </div>
              <p className="text-[11px] leading-relaxed">
                Your account was provisioned with a temporary password. You must establish a new, confidential password before continuing.
              </p>
            </div>

            {changePasswordError && (
              <div className="p-3 rounded-xl bg-rose-500/10 border border-rose-500/20 text-rose-600 dark:text-rose-400 text-xs font-semibold flex items-center gap-2">
                <XCircle className="w-4 h-4 shrink-0" />
                <span>{changePasswordError}</span>
              </div>
            )}

            <div className="space-y-1.5">
              <label className="font-bold text-foreground">Current Temporary Password</label>
              <div className="relative">
                <Input
                  type={showCurrentPassword ? 'text' : 'password'}
                  value={changePasswordForm.currentPassword}
                  onChange={(e) =>
                    setChangePasswordForm({ ...changePasswordForm, currentPassword: e.target.value })
                  }
                  required
                  placeholder="Enter temporary password"
                  className="rounded-xl text-xs pr-9"
                />
                <button
                  type="button"
                  onClick={() => setShowCurrentPassword(!showCurrentPassword)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground cursor-pointer"
                >
                  {showCurrentPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                </button>
              </div>
            </div>

            <div className="space-y-1.5">
              <label className="font-bold text-foreground">New Password</label>
              <div className="relative">
                <Input
                  type={showNewPassword ? 'text' : 'password'}
                  value={changePasswordForm.newPassword}
                  onChange={(e) =>
                    setChangePasswordForm({ ...changePasswordForm, newPassword: e.target.value })
                  }
                  required
                  placeholder="At least 8 characters"
                  className="rounded-xl text-xs pr-9"
                />
                <button
                  type="button"
                  onClick={() => setShowNewPassword(!showNewPassword)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground cursor-pointer"
                >
                  {showNewPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                </button>
              </div>
            </div>

            <div className="space-y-1.5">
              <label className="font-bold text-foreground">Confirm New Password</label>
              <Input
                type="password"
                value={changePasswordForm.confirmPassword}
                onChange={(e) =>
                  setChangePasswordForm({ ...changePasswordForm, confirmPassword: e.target.value })
                }
                required
                placeholder="Re-enter new password"
                className="rounded-xl text-xs"
              />
            </div>

            <div className="pt-2">
              <Button
                type="submit"
                size="sm"
                disabled={isChangingPassword}
                className="w-full rounded-xl text-xs font-bold bg-brand-blue text-white"
              >
                {isChangingPassword ? 'Updating Password...' : 'Save New Password & Continue'}
              </Button>
            </div>
          </form>
        </Modal>
      )}
    </div>
  );
};
