import React, { createContext, useContext, useState, useEffect } from 'react'
import { getGlobalToken, setGlobalToken, subscribeToApiEvents } from '../apiClient'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [token, setToken] = useState(getGlobalToken())
  const [user, setUser] = useState(() => {
    try {
      const saved = localStorage.getItem('hireai_auth_user')
      return saved ? JSON.parse(saved) : null
    } catch {
      return null
    }
  })

  // Stored profiles for quick identity toggle
  const [savedCandidate, setSavedCandidate] = useState(() => {
    try {
      const saved = localStorage.getItem('hireai_saved_candidate')
      return saved ? JSON.parse(saved) : null
    } catch {
      return null
    }
  })

  const [savedRecruiter, setSavedRecruiter] = useState(() => {
    try {
      const saved = localStorage.getItem('hireai_saved_recruiter')
      return saved ? JSON.parse(saved) : null
    } catch {
      return null
    }
  })

  useEffect(() => {
    const unsub = subscribeToApiEvents((event) => {
      if (event.type === 'TOKEN_CHANGED') {
        setToken(event.token)
      }
    })
    return unsub
  }, [])

  const setAuthSession = (authData) => {
    if (!authData) {
      setGlobalToken('')
      setUser(null)
      localStorage.removeItem('hireai_auth_user')
      return
    }

    const { accessToken, refreshToken, userId, firstName, lastName, email, role } = authData
    const userPayload = { userId, firstName, lastName, email, role, refreshToken }

    setGlobalToken(accessToken)
    setUser(userPayload)
    localStorage.setItem('hireai_auth_user', JSON.stringify(userPayload))

    if (role === 'ROLE_CANDIDATE' || role?.includes('CANDIDATE')) {
      setSavedCandidate({ ...authData })
      localStorage.setItem('hireai_saved_candidate', JSON.stringify(authData))
    } else if (role === 'ROLE_RECRUITER' || role?.includes('RECRUITER')) {
      setSavedRecruiter({ ...authData })
      localStorage.setItem('hireai_saved_recruiter', JSON.stringify(authData))
    }
  }

  const logout = () => {
    setAuthSession(null)
  }

  const switchToSavedCandidate = () => {
    if (savedCandidate) {
      setAuthSession(savedCandidate)
      return true
    }
    return false
  }

  const switchToSavedRecruiter = () => {
    if (savedRecruiter) {
      setAuthSession(savedRecruiter)
      return true
    }
    return false
  }

  return (
    <AuthContext.Provider
      value={{
        token,
        user,
        isAuthenticated: !!token,
        role: user?.role || 'NONE',
        savedCandidate,
        savedRecruiter,
        setAuthSession,
        logout,
        setCustomToken: (t) => {
          setGlobalToken(t)
          setToken(t)
        },
        switchToSavedCandidate,
        switchToSavedRecruiter,
      }}
    >
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider')
  }
  return context
}
