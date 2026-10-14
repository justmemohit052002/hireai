import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { authApi } from '@/services/api/auth.api';
import { candidateApi } from '@/services/api/candidate.api';
import { recruiterApi } from '@/services/api/recruiter.api';
import { getStoredToken, getStoredUser, clearAuthSession } from '@/services/api/apiClient';

const AuthContext = createContext(null);

export function normalizeRole(backendRole) {
  if (!backendRole) return null;
  let roleStr = '';
  if (typeof backendRole === 'object') {
    roleStr = backendRole.name || backendRole.role || backendRole.authority || JSON.stringify(backendRole);
  } else {
    roleStr = String(backendRole);
  }
  const upper = roleStr.toUpperCase();
  if (upper.includes('SUB_ADMIN')) return 'sub_admin';
  if (upper.includes('ADMIN')) return 'admin';
  if (upper.includes('RECRUITER') || upper.includes('HR')) return 'recruiter';
  if (upper.includes('CANDIDATE') || upper.includes('USER') || upper.includes('JOB_SEEKER')) return 'candidate';
  return roleStr.toLowerCase();
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => getStoredUser());
  const [isLoading, setIsLoading] = useState(true);

  // Normalize role string ('candidate' | 'recruiter')
  const role = normalizeRole(user?.role);
  const isAuthenticated = Boolean(user && getStoredToken());

  // Verify and hydrate current user on initial startup
  const refreshUserProfile = useCallback(async () => {
    const token = getStoredToken();
    if (!token) {
      setIsLoading(false);
      return;
    }
    try {
      const me = await authApi.getMe();
      if (me) {
        let avatar = me.avatarUrl || null;
        const userRole = normalizeRole(me.role);

        if (!avatar) {
          if (userRole === 'candidate') {
            try {
              const candProfile = await candidateApi.getMyProfile();
              if (candProfile?.profilePhotoUrl) {
                avatar = candProfile.profilePhotoUrl;
              }
            } catch {}
          } else if (userRole === 'recruiter') {
            try {
              const recProfile = await recruiterApi.getMyProfile();
              if (recProfile?.profilePhotoUrl) {
                avatar = recProfile.profilePhotoUrl;
              }
            } catch {}
          }
        }

        setUser((prev) => {
          const updated = {
            ...prev,
            ...me,
            id: me.id || prev?.id,
            role: me.role || prev?.role,
            avatarUrl: avatar || prev?.avatarUrl || null,
            hasPassword: me.hasPassword !== undefined ? Boolean(me.hasPassword) : (prev?.hasPassword ?? true),
            authProvider: me.authProvider || prev?.authProvider || 'LOCAL',
            mustChangePassword: me.mustChangePassword !== undefined ? Boolean(me.mustChangePassword) : Boolean(prev?.mustChangePassword),
          };
          localStorage.setItem('hireai_user', JSON.stringify(updated));
          return updated;
        });
      }
    } catch (err) {
      console.warn('[AuthContext] Session validation failed:', err.message);
      // If unauthorized, clear invalid session
      if (err.status === 401) {
        clearAuthSession();
        setUser(null);
      }
    } finally {
      setIsLoading(false);
    }
  }, []);

  const updateMustChangePassword = useCallback((mustChange) => {
    setUser((prev) => {
      if (!prev) return prev;
      const updated = { ...prev, mustChangePassword: Boolean(mustChange) };
      localStorage.setItem('hireai_user', JSON.stringify(updated));
      return updated;
    });
  }, []);

  const updateUserAvatar = useCallback((newAvatarUrl) => {
    setUser((prev) => {
      if (!prev) return prev;
      const updated = { ...prev, avatarUrl: newAvatarUrl };
      localStorage.setItem('hireai_user', JSON.stringify(updated));
      return updated;
    });
  }, []);

  const updateUserPasswordStatus = useCallback((hasPassword) => {
    setUser((prev) => {
      if (!prev) return prev;
      const updated = { ...prev, hasPassword: Boolean(hasPassword) };
      localStorage.setItem('hireai_user', JSON.stringify(updated));
      return updated;
    });
  }, []);

  useEffect(() => {
    refreshUserProfile();

    const handleLogoutEvent = () => {
      setUser(null);
    };

    window.addEventListener('hireai-logout', handleLogoutEvent);
    return () => window.removeEventListener('hireai-logout', handleLogoutEvent);
  }, [refreshUserProfile]);

  const login = async (email, password, requestedRole) => {
    setIsLoading(true);
    try {
      const response = await authApi.login(email, password, requestedRole);
      const loggedUser = {
        id: response.id || response.userId,
        email: response.email,
        firstName: response.firstName,
        lastName: response.lastName,
        role: response.role,
        companyName: response.companyName,
        avatarUrl: response.avatarUrl || response.profilePhotoUrl || null,
        hasPassword: response.hasPassword !== undefined ? Boolean(response.hasPassword) : true,
        mustChangePassword: Boolean(response.mustChangePassword),
        authProvider: response.authProvider || 'LOCAL',
      };
      setUser(loggedUser);
      return { success: true, user: loggedUser, role: normalizeRole(response.role), mustChangePassword: Boolean(response.mustChangePassword) };
    } catch (error) {
      console.error('[AuthContext] Login failed:', error);
      throw error;
    } finally {
      setIsLoading(false);
    }
  };

  const registerCandidate = async (payload) => {
    setIsLoading(true);
    try {
      const response = await authApi.registerCandidate(payload);
      if (response && response.requiresVerification) {
        return { requiresVerification: true, email: response.email, message: response.message };
      }
      const registeredUser = {
        id: response.id || response.userId,
        email: response.email,
        firstName: response.firstName,
        lastName: response.lastName,
        role: response.role,
        avatarUrl: response.avatarUrl || response.profilePhotoUrl || null,
        hasPassword: true,
        authProvider: 'LOCAL',
      };
      setUser(registeredUser);
      return { success: true, user: registeredUser, role: 'candidate' };
    } finally {
      setIsLoading(false);
    }
  };

  const registerRecruiter = async (payload) => {
    setIsLoading(true);
    try {
      const response = await authApi.registerRecruiter(payload);
      if (response && response.requiresVerification) {
        return { requiresVerification: true, email: response.email, message: response.message };
      }
      const registeredUser = {
        id: response.id || response.userId,
        email: response.email,
        firstName: response.firstName,
        lastName: response.lastName,
        role: response.role,
        companyName: response.companyName,
        avatarUrl: response.avatarUrl || response.profilePhotoUrl || null,
        hasPassword: true,
        authProvider: 'LOCAL',
      };
      setUser(registeredUser);
      return { success: true, user: registeredUser, role: 'recruiter' };
    } finally {
      setIsLoading(false);
    }
  };

  const verifyEmailOtp = async ({ email, otp }) => {
    setIsLoading(true);
    try {
      const response = await authApi.verifyEmailOtp({ email, otp });
      const loggedUser = {
        id: response.id || response.userId,
        email: response.email,
        firstName: response.firstName,
        lastName: response.lastName,
        role: response.role,
        companyName: response.companyName,
        avatarUrl: response.avatarUrl || response.profilePhotoUrl || null,
        hasPassword: response.hasPassword !== undefined ? Boolean(response.hasPassword) : true,
        authProvider: response.authProvider || 'LOCAL',
      };
      setUser(loggedUser);
      return { success: true, user: loggedUser, role: normalizeRole(response.role) };
    } finally {
      setIsLoading(false);
    }
  };

  const resendEmailOtp = async ({ email }) => {
    return authApi.resendEmailOtp({ email });
  };

  const loginWithGoogle = async (payload) => {
    setIsLoading(true);
    try {
      const response = await authApi.loginWithGoogle(payload);
      const effectiveRole = response?.role || payload?.role;
      const loggedUser = {
        id: response?.id || response?.userId,
        email: response?.email,
        firstName: response?.firstName,
        lastName: response?.lastName,
        role: effectiveRole,
        companyName: response?.companyName || payload?.companyName,
        avatarUrl: response?.avatarUrl || response?.profilePhotoUrl || null,
        hasPassword: response?.hasPassword !== undefined ? Boolean(response.hasPassword) : false,
        authProvider: response?.authProvider || 'GOOGLE',
      };
      setUser(loggedUser);
      return { success: true, user: loggedUser, role: normalizeRole(effectiveRole) };
    } finally {
      setIsLoading(false);
    }
  };

  const loginWithLinkedIn = async (payload) => {
    setIsLoading(true);
    try {
      const response = await authApi.loginWithLinkedIn(payload);
      const effectiveRole = response?.role || payload?.role;
      const loggedUser = {
        id: response?.id || response?.userId,
        email: response?.email,
        firstName: response?.firstName,
        lastName: response?.lastName,
        role: effectiveRole,
        companyName: response?.companyName || payload?.companyName,
        avatarUrl: response?.avatarUrl || response?.profilePhotoUrl || null,
        hasPassword: response?.hasPassword !== undefined ? Boolean(response.hasPassword) : false,
        authProvider: response?.authProvider || 'LINKEDIN',
      };
      setUser(loggedUser);
      return { success: true, user: loggedUser, role: normalizeRole(effectiveRole) };
    } finally {
      setIsLoading(false);
    }
  };

  const logout = async () => {
    try {
      await authApi.logout();
    } finally {
      clearAuthSession();
      setUser(null);
    }
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        role,
        isAuthenticated,
        isLoading,
        login,
        registerCandidate,
        registerRecruiter,
        verifyEmailOtp,
        resendEmailOtp,
        loginWithGoogle,
        loginWithLinkedIn,
        logout,
        refreshUserProfile,
        updateUserAvatar,
        updateUserPasswordStatus,
        updateMustChangePassword,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
