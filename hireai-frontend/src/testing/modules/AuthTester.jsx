import React, { useState } from 'react'
import EndpointCard from '../components/EndpointCard'
import { useAuth } from '../context/AuthContext'
import { Key, UserCheck, Briefcase, LogIn, CheckCircle, RefreshCw } from 'lucide-react'

export default function AuthTester() {
  const { setAuthSession, user, token, logout } = useAuth()
  const [randomSuffix, setRandomSuffix] = useState(() => Math.floor(1000 + Math.random() * 9000))

  const regenerateSeed = () => {
    setRandomSuffix(Math.floor(1000 + Math.random() * 9000))
  }

  const sampleCandidateRegister = {
    firstName: 'Arjun',
    lastName: 'Sharma',
    email: `arjun.candidate${randomSuffix}@test.com`,
    password: 'Password@123',
    phoneNumber: '9876543210',
  }

  const sampleRecruiterRegister = {
    firstName: 'Priya',
    lastName: 'Verma',
    email: `priya.recruiter${randomSuffix}@test.com`,
    password: 'Password@123',
    phoneNumber: '9812345678',
  }

  const sampleLogin = {
    email: `arjun.candidate${randomSuffix}@test.com`,
    password: 'Password@123',
  }

  const handleAuthSuccess = (data) => {
    if (data && data.accessToken) {
      setAuthSession(data)
    }
  }

  return (
    <div className="tester-container">
      <div className="tester-header">
        <div className="tester-title-wrap">
          <h2>Authentication & JWT Testing Module</h2>
          <p className="tester-subtitle">
            Test candidate registration, recruiter registration, login authentication, and verify JWT stateless filters.
          </p>
        </div>
        <button type="button" className="btn-secondary" onClick={regenerateSeed}>
          <RefreshCw size={14} /> Generate Fresh Test Emails (#{randomSuffix})
        </button>
      </div>

      {user && (
        <div className="active-session-banner">
          <div className="session-left">
            <CheckCircle size={18} color="#10b981" />
            <div>
              <strong>Currently Authenticated:</strong> {user.firstName} {user.lastName} ({user.email}) - <span className="role-tag">{user.role}</span>
            </div>
          </div>
          <button type="button" className="btn-tiny btn-danger" onClick={logout}>
            Logout / Clear Token
          </button>
        </div>
      )}

      <div className="endpoints-list">
        {/* Register Candidate */}
        <EndpointCard
          title="Register Candidate"
          description="Registers a new job-seeker account with ROLE_CANDIDATE and returns initial JWT tokens."
          method="POST"
          path="/auth/register/candidate"
          authRequired="NONE"
          defaultPayload={sampleCandidateRegister}
          onSuccess={handleAuthSuccess}
        />

        {/* Register Recruiter */}
        <EndpointCard
          title="Register Recruiter"
          description="Registers a new employer/recruiter account with ROLE_RECRUITER and returns initial JWT tokens."
          method="POST"
          path="/auth/register/recruiter"
          authRequired="NONE"
          defaultPayload={sampleRecruiterRegister}
          onSuccess={handleAuthSuccess}
        />

        {/* Login */}
        <EndpointCard
          title="User Login"
          description="Authenticates credentials (email + password) and generates a fresh 15-minute Access Token & 7-day Refresh Token."
          method="POST"
          path="/auth/login"
          authRequired="NONE"
          defaultPayload={sampleLogin}
          onSuccess={handleAuthSuccess}
        />

        {/* JWT Test Ping */}
        <EndpointCard
          title="JWT Verification Ping (/test)"
          description="Verifies whether the currently attached Bearer token is valid and accepted by Spring Security."
          method="GET"
          path="/test"
          authRequired="AUTHENTICATED"
        />
      </div>
    </div>
  )
}
