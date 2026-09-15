import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { authApi } from '@/services/api/auth.api';
import { getStoredToken, getStoredUser, clearAuthSession } from '@/services/api/apiClient';

const AuthContext = createContext(null);

export function normalizeRole(backendRole) {
  if (!backendRole) return null;
  const upper = String(backendRole).toUpperCase();
  if (upper.includes('RECRUITER') || upper.includes('ADMIN')) return 'recruiter';
  if (upper.includes('CANDIDATE')) return 'candidate';
  return backendRole.toLowerCase();
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
        setUser((prev) => ({
          ...prev,
          ...me,
          id: me.id || prev?.id,
          role: me.role || prev?.role,
        }));
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

  useEffect(() => {
    refreshUserProfile();

    const handleLogoutEvent = () => {
      setUser(null);
    };

    window.addEventListener('hireai-logout', handleLogoutEvent);
    return () => window.removeEventListener('hireai-logout', handleLogoutEvent);
  }, [refreshUserProfile]);

  const login = async (email, password) => {
    setIsLoading(true);
    try {
      const response = await authApi.login(email, password);
      const loggedUser = {
        id: response.id || response.userId,
        email: response.email,
        firstName: response.firstName,
        lastName: response.lastName,
        role: response.role,
        companyName: response.companyName,
      };
      setUser(loggedUser);
      return { success: true, user: loggedUser, role: normalizeRole(response.role) };
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
      const registeredUser = {
        id: response.id || response.userId,
        email: response.email,
        firstName: response.firstName,
        lastName: response.lastName,
        role: response.role,
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
      const registeredUser = {
        id: response.id || response.userId,
        email: response.email,
        firstName: response.firstName,
        lastName: response.lastName,
        role: response.role,
        companyName: response.companyName,
      };
      setUser(registeredUser);
      return { success: true, user: registeredUser, role: 'recruiter' };
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
        logout,
        refreshUserProfile,
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
