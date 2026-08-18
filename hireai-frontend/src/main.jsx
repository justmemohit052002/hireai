import React, { useState } from 'react'
import { createRoot } from 'react-dom/client'
import {
  ArrowRight, BriefcaseBusiness, Building2, Check, CircleUserRound, Eye, EyeOff,
  LockKeyhole, Mail, ShieldCheck, Sparkles, UserRound, ArrowLeft, Terminal,
} from 'lucide-react'
import './styles.css'
import './testing/testing.css'
import { AuthProvider } from './testing/context/AuthContext'
import ApiTestingHub from './testing/ApiTestingHub'

const emptyLogin = { email: '', password: '' }
const emptyRegister = { firstName: '', lastName: '', email: '', phoneNumber: '', password: '', role: 'ROLE_CANDIDATE' }

function PasswordInput({ value, onChange, error }) {
  const [visible, setVisible] = useState(false)
  return (
    <label className="field">
      <span>Password</span>
      <div className={`input-wrap ${error ? 'has-error' : ''}`}>
        <LockKeyhole size={18} />
        <input
          type={visible ? 'text' : 'password'}
          value={value}
          onChange={onChange}
          placeholder="At least 8 characters"
          autoComplete="current-password"
        />
        <button className="icon-button" type="button" onClick={() => setVisible(!visible)} aria-label="Show password">
          {visible ? <EyeOff size={18} /> : <Eye size={18} />}
        </button>
      </div>
      {error && <small className="error">{error}</small>}
    </label>
  )
}

function AuthApp({ onSwitchToTestingHub }) {
  const [mode, setMode] = useState('login')
  const [login, setLogin] = useState(emptyLogin)
  const [register, setRegister] = useState(emptyRegister)
  const [errors, setErrors] = useState({})
  const [notice, setNotice] = useState('')
  const [loading, setLoading] = useState(false)

  const isLogin = mode === 'login'
  const form = isLogin ? login : register

  function switchMode(nextMode) {
    setMode(nextMode)
    setErrors({})
    setNotice('')
  }

  function setValue(name, value) {
    const setter = isLogin ? setLogin : setRegister
    setter(current => ({ ...current, [name]: value }))
    setErrors(current => ({ ...current, [name]: '' }))
  }

  function validate() {
    const next = {}
    if (!form.email.trim()) next.email = 'Email is required'
    else if (!/^\S+@\S+\.\S+$/.test(form.email)) next.email = 'Enter a valid email address'
    if (!form.password) next.password = 'Password is required'
    else if (!isLogin && form.password.length < 8) next.password = 'Use at least 8 characters'
    if (!isLogin) {
      if (!form.role) next.role = 'Choose the type of account you need'
      if (!form.firstName.trim()) next.firstName = 'First name is required'
      if (!form.lastName.trim()) next.lastName = 'Last name is required'
      if (!/^[6-9]\d{9}$/.test(form.phoneNumber)) next.phoneNumber = 'Enter a valid 10-digit mobile number'
    }
    setErrors(next)
    return Object.keys(next).length === 0
  }

  async function handleSubmit(event) {
    event.preventDefault()
    setNotice('')
    if (!validate()) return
    setLoading(true)
    try {
      const endpoint = isLogin
        ? '/auth/login'
        : form.role === 'ROLE_RECRUITER'
        ? '/auth/register/recruiter'
        : '/auth/register/candidate'

      const response = await fetch(endpoint, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(form),
      })
      const payload = await response.json().catch(() => ({}))
      if (!response.ok) {
        const backendErrors = payload.errors || {}
        setErrors(backendErrors)
        throw new Error(payload.message || 'Something went wrong. Please try again.')
      }
      localStorage.setItem('hireai_auth', JSON.stringify(payload))
      localStorage.setItem('hireai_token', payload.accessToken || '')
      setNotice(isLogin ? `Welcome back, ${payload.firstName || 'there'}!` : 'Your account is ready. Welcome to HireAI!')
      if (!isLogin) setRegister(emptyRegister)
    } catch (error) {
      setNotice(error.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <main className="auth-page">
      <div style={{ position: 'fixed', top: '16px', right: '16px', zIndex: 100 }}>
        <button
          type="button"
          className="btn-primary"
          onClick={onSwitchToTestingHub}
          style={{ boxShadow: '0 4px 14px rgba(0,0,0,0.3)', padding: '0.6rem 1rem' }}
        >
          <Terminal size={16} /> Open Full API Testing Workbench
        </button>
      </div>

      <section className="story-panel">
        <div className="brand"><span className="brand-mark"><BriefcaseBusiness size={21} /></span> Hire<span>AI</span></div>
        <div className="story-content">
          <div className="eyebrow"><Sparkles size={15} /> THE SMARTER WAY TO GET HIRED</div>
          <h1>Find work that feels like <em>your next move.</em></h1>
          <p>Build a career profile that gets noticed, then let intelligent matching bring the right opportunities to you.</p>
          <div className="benefits">
            <div><span><Check size={16} /></span><p><strong>Personalized opportunities</strong><small>Roles matched to your skills and ambitions.</small></p></div>
            <div><span><Check size={16} /></span><p><strong>One profile, more reach</strong><small>Put your best work in front of great teams.</small></p></div>
          </div>
        </div>
        <div className="quote"><div className="avatars"><i>AJ</i><i>SM</i><i>RK</i><i>+</i></div><p>Join 10,000+ professionals building what’s next.</p></div>
        <div className="orb orb-one" /><div className="orb orb-two" /><div className="grid" />
      </section>

      <section className="form-panel">
        <div className="mobile-brand brand"><span className="brand-mark"><BriefcaseBusiness size={19} /></span> Hire<span>AI</span></div>
        <div className="form-shell">
          <div className="form-heading">
            <h2>{isLogin ? 'Welcome back' : 'Create your account'}</h2>
            <p>{isLogin ? 'Sign in to continue your career journey.' : 'Start finding opportunities made for you.'}</p>
          </div>
          <div className="tabs" role="tablist">
            <button className={isLogin ? 'active' : ''} onClick={() => switchMode('login')}>Sign in</button>
            <button className={!isLogin ? 'active' : ''} onClick={() => switchMode('register')}>Create account</button>
          </div>
          <form onSubmit={handleSubmit} noValidate>
            {!isLogin && <fieldset className={`role-picker ${errors.role ? 'role-error' : ''}`}>
              <legend>How will you use HireAI?</legend>
              <p className="role-help">Choose the experience that fits you best.</p>
              <div className="role-options">
                <label className={`role-card ${form.role === 'ROLE_CANDIDATE' ? 'selected' : ''}`}>
                  <input type="radio" name="role" value="ROLE_CANDIDATE" checked={form.role === 'ROLE_CANDIDATE'} onChange={e => setValue('role', e.target.value)} />
                  <span className="role-icon"><CircleUserRound size={19} /></span>
                  <span><strong>I'm looking for work</strong><small>Explore roles and get matched with opportunities.</small></span>
                  <span className="radio-dot" />
                </label>
                <label className={`role-card ${form.role === 'ROLE_RECRUITER' ? 'selected' : ''}`}>
                  <input type="radio" name="role" value="ROLE_RECRUITER" checked={form.role === 'ROLE_RECRUITER'} onChange={e => setValue('role', e.target.value)} />
                  <span className="role-icon"><Building2 size={19} /></span>
                  <span><strong>I'm hiring talent</strong><small>Post jobs and connect with qualified candidates.</small></span>
                  <span className="radio-dot" />
                </label>
              </div>
              {errors.role && <small className="error">{errors.role}</small>}
            </fieldset>}
            {!isLogin && <div className="split-fields">
              <label className="field"><span>First name</span><div className={`input-wrap ${errors.firstName ? 'has-error' : ''}`}><UserRound size={18} /><input value={form.firstName} onChange={e => setValue('firstName', e.target.value)} placeholder="Aarav" autoComplete="given-name" /></div>{errors.firstName && <small className="error">{errors.firstName}</small>}</label>
              <label className="field"><span>Last name</span><div className={`input-wrap ${errors.lastName ? 'has-error' : ''}`}><input value={form.lastName} onChange={e => setValue('lastName', e.target.value)} placeholder="Sharma" autoComplete="family-name" /></div>{errors.lastName && <small className="error">{errors.lastName}</small>}</label>
            </div>}
            <label className="field"><span>Email address</span><div className={`input-wrap ${errors.email ? 'has-error' : ''}`}><Mail size={18} /><input type="email" value={form.email} onChange={e => setValue('email', e.target.value)} placeholder="you@example.com" autoComplete="email" /></div>{errors.email && <small className="error">{errors.email}</small>}</label>
            {!isLogin && <label className="field"><span>Mobile number</span><div className={`input-wrap ${errors.phoneNumber ? 'has-error' : ''}`}><span className="country-code">+91</span><input inputMode="numeric" maxLength="10" value={form.phoneNumber} onChange={e => setValue('phoneNumber', e.target.value.replace(/\D/g, ''))} placeholder="98765 43210" autoComplete="tel" /></div>{errors.phoneNumber && <small className="error">{errors.phoneNumber}</small>}</label>}
            <PasswordInput value={form.password} onChange={e => setValue('password', e.target.value)} error={errors.password} />
            {isLogin && <div className="form-options"><label className="remember"><input type="checkbox" /> <span>Remember me</span></label><a href="#forgot">Forgot password?</a></div>}
            <button className="submit" disabled={loading}>{loading ? 'Please wait…' : isLogin ? 'Sign in to HireAI' : 'Create my account'} <ArrowRight size={18} /></button>
            {notice && <p className={notice.startsWith('Welcome') || notice.startsWith('Your account') ? 'notice success' : 'notice'}>{notice}</p>}
          </form>
          <p className="terms">By continuing, you agree to our <a href="#terms">Terms of Service</a> and <a href="#privacy">Privacy Policy</a>.</p>
          <div className="secure-note"><ShieldCheck size={15} /> Your information is encrypted and secure.</div>
        </div>
      </section>
    </main>
  )
}

function RootApp() {
  const [view, setView] = useState('testing') // 'testing' | 'auth'

  return (
    <AuthProvider>
      {view === 'testing' ? (
        <ApiTestingHub onSwitchToAppView={() => setView('auth')} />
      ) : (
        <AuthApp onSwitchToTestingHub={() => setView('testing')} />
      )}
    </AuthProvider>
  )
}

createRoot(document.getElementById('root')).render(<RootApp />)
