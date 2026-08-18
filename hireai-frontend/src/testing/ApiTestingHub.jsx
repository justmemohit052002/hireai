import React, { useState, useEffect } from 'react'
import { useAuth } from './context/AuthContext'
import {
  apiRequest,
  getApiBaseUrl,
  setApiBaseUrl,
  subscribeToApiEvents,
  getGlobalToken,
  setGlobalToken,
} from './apiClient'
import AuthTester from './modules/AuthTester'
import UserTester from './modules/UserTester'
import RecruiterProfileTester from './modules/RecruiterProfileTester'
import CandidateProfileTester from './modules/CandidateProfileTester'
import JobTester from './modules/JobTester'
import ApplicationTester from './modules/ApplicationTester'
import FullE2EFlowTester from './modules/FullE2EFlowTester'
import RawApiRunner from './modules/RawApiRunner'

import {
  ShieldCheck,
  Zap,
  Key,
  Users,
  Briefcase,
  Building2,
  FileSpreadsheet,
  Terminal,
  Activity,
  Server,
  Settings,
  LogOut,
  ChevronRight,
  Sparkles,
  Layers,
  Clock,
  CheckCircle2,
  XCircle,
  Trash2,
  SlidersHorizontal,
} from 'lucide-react'

export default function ApiTestingHub({ onSwitchToAppView }) {
  const {
    user,
    token,
    role,
    logout,
    savedCandidate,
    savedRecruiter,
    switchToSavedCandidate,
    switchToSavedRecruiter,
    setAuthSession,
  } = useAuth()

  const [activeTab, setActiveTab] = useState('e2e')
  const [logs, setLogs] = useState([])
  const [showLogsDrawer, setShowLogsDrawer] = useState(false)
  const [serverOnline, setServerOnline] = useState(null)
  const [checkingServer, setCheckingServer] = useState(false)
  const [baseUrlInput, setBaseUrlInput] = useState(getApiBaseUrl())
  const [showSettingsModal, setShowSettingsModal] = useState(false)
  const [customTokenInput, setCustomTokenInput] = useState('')
  const [showTokenModal, setShowTokenModal] = useState(false)

  // Listen to live API requests
  useEffect(() => {
    const unsub = subscribeToApiEvents((event) => {
      if (event.type === 'REQUEST_COMPLETED') {
        setLogs((prev) => [event.log, ...prev].slice(0, 100))
        if (event.log.status > 0) {
          setServerOnline(true)
        }
      }
    })
    return unsub
  }, [])

  // Health check on mount
  useEffect(() => {
    checkServerHealth()
  }, [])

  const checkServerHealth = async () => {
    setCheckingServer(true)
    try {
      const res = await apiRequest({ method: 'GET', endpoint: '/test' })
      // Even if 401/403, server is running and responding!
      if (res.status > 0) {
        setServerOnline(true)
      } else {
        setServerOnline(false)
      }
    } catch {
      setServerOnline(false)
    } finally {
      setCheckingServer(false)
    }
  }

  const handleSaveBaseUrl = () => {
    setApiBaseUrl(baseUrlInput)
    setShowSettingsModal(false)
    checkServerHealth()
  }

  const handleSaveCustomToken = () => {
    setGlobalToken(customTokenInput.trim())
    setShowTokenModal(false)
  }

  const quickLoginPreset = async (roleType) => {
    const seed = Math.floor(1000 + Math.random() * 9000)
    const endpoint = roleType === 'RECRUITER' ? '/auth/register/recruiter' : '/auth/register/candidate'
    const res = await apiRequest({
      method: 'POST',
      endpoint,
      body: {
        firstName: roleType === 'RECRUITER' ? 'Recruiter' : 'Candidate',
        lastName: `Tester${seed}`,
        email: `${roleType.toLowerCase()}.${seed}@hireai.io`,
        password: 'Password@123',
        phoneNumber: '98' + Math.floor(10000000 + Math.random() * 90000000),
      },
    })
    if (res.ok && res.data?.accessToken) {
      setAuthSession(res.data)
      alert(`Success! Created and logged in as ${res.data.email} (${res.data.role})`)
    } else {
      alert(`Quick Login failed: ${res.error || 'Check server connection'}`)
    }
  }

  const tabs = [
    { id: 'e2e', name: '⚡ Automated E2E Flow', icon: Zap, component: FullE2EFlowTester },
    { id: 'auth', name: '🔐 Auth & JWT', icon: Key, component: AuthTester },
    { id: 'user', name: '👤 User Self-Service', icon: Users, component: UserTester },
    { id: 'recruiter', name: '🏢 Recruiter Profiles', icon: Building2, component: RecruiterProfileTester },
    { id: 'candidate', name: '👥 Candidate Directory', icon: Users, component: CandidateProfileTester },
    { id: 'jobs', name: '💼 Job Management', icon: Briefcase, component: JobTester },
    { id: 'applications', name: '🎯 ATS & Applications', icon: FileSpreadsheet, component: ApplicationTester },
    { id: 'raw', name: '🛠️ Raw API Explorer', icon: Terminal, component: RawApiRunner },
  ]

  const ActiveComponent = tabs.find((t) => t.id === activeTab)?.component || FullE2EFlowTester

  return (
    <div className="hub-layout">
      {/* Top Bar */}
      <header className="hub-topbar">
        <div className="hub-topbar-left">
          <div className="hub-brand">
            <span className="hub-brand-logo">
              <Briefcase size={20} />
            </span>
            <div className="hub-brand-titles">
              <span className="brand-main">HireAI</span>
              <span className="brand-sub">API Testing Workbench</span>
            </div>
          </div>

          <div
            className={`server-pill ${
              serverOnline === true ? 'server-online' : serverOnline === false ? 'server-offline' : 'server-checking'
            }`}
            onClick={checkServerHealth}
            title="Click to recheck Spring Boot connectivity"
          >
            <Server size={14} />
            <span>
              {checkingServer
                ? 'Pinging Server...'
                : serverOnline === true
                ? 'Backend: Online (8080)'
                : 'Backend: Offline (Check 8080)'}
            </span>
          </div>
        </div>

        <div className="hub-topbar-right">
          {/* Identity Switcher */}
          <div className="auth-quick-bar">
            {token ? (
              <div className="user-logged-badge">
                <span className={`role-badge-tag role-${role.toLowerCase()}`}>{role}</span>
                <span className="user-email-text">{user?.email || 'Logged In'}</span>
                <button
                  type="button"
                  className="btn-tiny btn-ghost"
                  onClick={() => setShowTokenModal(true)}
                  title="Inspect/Edit Token"
                >
                  <Key size={13} /> Token
                </button>
                <button
                  type="button"
                  className="btn-tiny btn-danger-ghost"
                  onClick={logout}
                  title="Logout"
                >
                  <LogOut size={13} /> Logout
                </button>
              </div>
            ) : (
              <div className="no-auth-quick-actions">
                <span className="no-auth-label">No Active Token</span>
                <button
                  type="button"
                  className="btn-tiny btn-candidate-preset"
                  onClick={() => quickLoginPreset('CANDIDATE')}
                >
                  + Quick Candidate
                </button>
                <button
                  type="button"
                  className="btn-tiny btn-recruiter-preset"
                  onClick={() => quickLoginPreset('RECRUITER')}
                >
                  + Quick Recruiter
                </button>
                <button
                  type="button"
                  className="btn-tiny btn-ghost"
                  onClick={() => setShowTokenModal(true)}
                >
                  <Key size={13} /> Enter JWT
                </button>
              </div>
            )}

            {/* Quick role toggles if saved */}
            {savedCandidate && role !== 'ROLE_CANDIDATE' && (
              <button
                type="button"
                className="btn-tiny btn-candidate-switch"
                onClick={switchToSavedCandidate}
                title={`Switch to ${savedCandidate.email}`}
              >
                Switch to Candidate
              </button>
            )}

            {savedRecruiter && role !== 'ROLE_RECRUITER' && (
              <button
                type="button"
                className="btn-tiny btn-recruiter-switch"
                onClick={switchToSavedRecruiter}
                title={`Switch to ${savedRecruiter.email}`}
              >
                Switch to Recruiter
              </button>
            )}
          </div>

          <div className="topbar-controls">
            <button
              type="button"
              className="btn-icon"
              onClick={() => setShowSettingsModal(true)}
              title="API Base URL Settings"
            >
              <Settings size={16} />
            </button>

            <button
              type="button"
              className="btn-icon"
              onClick={() => setShowLogsDrawer(!showLogsDrawer)}
              title="Toggle Live Logs Stream"
            >
              <Activity size={16} />
              {logs.length > 0 && <span className="logs-count-dot">{logs.length}</span>}
            </button>

            {onSwitchToAppView && (
              <button
                type="button"
                className="btn-secondary btn-switch-view"
                onClick={onSwitchToAppView}
              >
                Launch Auth Form UI <ChevronRight size={14} />
              </button>
            )}
          </div>
        </div>
      </header>

      {/* Main Body */}
      <div className="hub-content-shell">
        {/* Sidebar */}
        <nav className="hub-sidebar">
          <div className="sidebar-section-title">API Feature Modules</div>
          <div className="sidebar-menu">
            {tabs.map((tab) => {
              const Icon = tab.icon
              const isActive = activeTab === tab.id
              return (
                <button
                  key={tab.id}
                  type="button"
                  className={`sidebar-nav-item ${isActive ? 'active' : ''}`}
                  onClick={() => setActiveTab(tab.id)}
                >
                  <Icon size={18} />
                  <span>{tab.name}</span>
                </button>
              )
            })}
          </div>

          <div className="sidebar-footer">
            <div className="sidebar-footer-card">
              <Sparkles size={16} color="#6366f1" />
              <div>
                <strong>Spring Boot 3.x</strong>
                <small>PostgreSQL + JWT + ATS Engine</small>
              </div>
            </div>
          </div>
        </nav>

        {/* Main Work Area */}
        <main className="hub-main-area">
          <ActiveComponent />
        </main>
      </div>

      {/* Live Logs Drawer */}
      {showLogsDrawer && (
        <aside className="logs-drawer">
          <div className="logs-drawer-header">
            <div className="logs-drawer-title">
              <Activity size={16} />
              <span>Live API Request Stream ({logs.length})</span>
            </div>
            <div className="logs-drawer-actions">
              <button type="button" className="btn-tiny" onClick={() => setLogs([])}>
                <Trash2 size={12} /> Clear
              </button>
              <button
                type="button"
                className="btn-icon"
                onClick={() => setShowLogsDrawer(false)}
              >
                ✕
              </button>
            </div>
          </div>

          <div className="logs-list">
            {logs.length === 0 ? (
              <p className="hint-text" style={{ padding: '1rem' }}>
                No API requests dispatched yet. Requests sent through any testing panel will appear here in real time.
              </p>
            ) : (
              logs.map((log) => (
                <div key={log.id} className={`log-item log-${log.ok ? 'ok' : 'err'}`}>
                  <div className="log-item-top">
                    <span className={`log-method method-${log.method.toLowerCase()}`}>
                      {log.method}
                    </span>
                    <span className="log-endpoint">{log.endpoint}</span>
                    <span className={`log-status ${log.ok ? 'status-200' : 'status-err'}`}>
                      {log.status || 'ERR'}
                    </span>
                  </div>
                  <div className="log-item-bottom">
                    <span className="log-time">{log.timestamp}</span>
                    <span className="log-duration">{log.durationMs} ms</span>
                  </div>
                  {!log.ok && log.responseData?.error && (
                    <div className="log-error-msg">{log.responseData.error}</div>
                  )}
                </div>
              ))
            )}
          </div>
        </aside>
      )}

      {/* Settings Modal */}
      {showSettingsModal && (
        <div className="modal-backdrop" onClick={() => setShowSettingsModal(false)}>
          <div className="modal-box" onClick={(e) => e.stopPropagation()}>
            <h3>API Backend Settings</h3>
            <p className="modal-desc">
              Configure backend target URL. By default, the Vite dev server proxies requests to{' '}
              <code>http://localhost:8080</code>.
            </p>

            <div className="modal-field">
              <label>API Base URL:</label>
              <input
                type="text"
                value={baseUrlInput}
                onChange={(e) => setBaseUrlInput(e.target.value)}
                placeholder="Leave empty for default Vite proxy or enter http://localhost:8080"
              />
              <small className="hint-text">
                Leave empty to use automatic relative proxy (<code>/auth</code>, <code>/jobs</code>, etc.)
              </small>
            </div>

            <div className="modal-actions">
              <button
                type="button"
                className="btn-secondary"
                onClick={() => setShowSettingsModal(false)}
              >
                Cancel
              </button>
              <button type="button" className="btn-primary" onClick={handleSaveBaseUrl}>
                Save Settings
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Token Modal */}
      {showTokenModal && (
        <div className="modal-backdrop" onClick={() => setShowTokenModal(false)}>
          <div className="modal-box" onClick={(e) => e.stopPropagation()}>
            <h3>Active JWT Bearer Token</h3>
            <p className="modal-desc">
              Inspect, override, or manually paste a Bearer JWT Token generated from another source or terminal.
            </p>

            <div className="modal-field">
              <label>JWT Access Token:</label>
              <textarea
                rows={5}
                className="code-textarea"
                value={customTokenInput || getGlobalToken()}
                onChange={(e) => setCustomTokenInput(e.target.value)}
                placeholder="Paste Bearer JWT token here..."
              />
            </div>

            <div className="modal-actions">
              <button
                type="button"
                className="btn-secondary"
                onClick={() => {
                  setGlobalToken('')
                  setShowTokenModal(false)
                }}
              >
                Clear Token
              </button>
              <button type="button" className="btn-primary" onClick={handleSaveCustomToken}>
                Apply Token
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
