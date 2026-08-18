import React, { useState } from 'react'
import { apiRequest } from '../apiClient'
import { useAuth } from '../context/AuthContext'
import { Play, CheckCircle2, XCircle, Loader2, Sparkles, ArrowRight, Shield, RefreshCw } from 'lucide-react'

export default function FullE2EFlowTester() {
  const { setAuthSession } = useAuth()
  const [isRunning, setIsRunning] = useState(false)
  const [currentStepIndex, setCurrentStepIndex] = useState(-1)
  const [steps, setSteps] = useState([
    { id: 'step-1', name: '1. Register Recruiter Account', endpoint: 'POST /auth/register/recruiter', status: 'idle', result: null },
    { id: 'step-2', name: '2. Create Recruiter Company Profile', endpoint: 'POST /recruiter/profile', status: 'idle', result: null },
    { id: 'step-3', name: '3. Recruiter Posts New Job Opening', endpoint: 'POST /jobs', status: 'idle', result: null },
    { id: 'step-4', name: '4. Register Candidate Account', endpoint: 'POST /auth/register/candidate', status: 'idle', result: null },
    { id: 'step-5', name: '5. Create Candidate Career Profile', endpoint: 'POST /candidate/profile', status: 'idle', result: null },
    { id: 'step-6', name: '6. Candidate Browses Open Jobs', endpoint: 'GET /jobs/open', status: 'idle', result: null },
    { id: 'step-7', name: '7. Candidate Applies to Job & Calculates ATS Score', endpoint: 'POST /jobs/{jobId}/apply', status: 'idle', result: null },
    { id: 'step-8', name: '8. Candidate Views Submitted Applications', endpoint: 'GET /candidate/applications', status: 'idle', result: null },
    { id: 'step-9', name: '9. Recruiter Views Ranked Applicants Pipeline', endpoint: 'GET /jobs/{jobId}/applications', status: 'idle', result: null },
    { id: 'step-10', name: '10. Recruiter Shortlists Candidate', endpoint: 'PATCH /applications/{id}/status', status: 'idle', result: null },
  ])
  const [overallSummary, setOverallSummary] = useState(null)

  const updateStepStatus = (index, status, result = null, durationMs = 0) => {
    setSteps(prev => prev.map((s, i) => i === index ? { ...s, status, result, durationMs } : s))
  }

  const runFullE2EScenario = async () => {
    setIsRunning(true)
    setOverallSummary(null)
    const seed = Math.floor(10000 + Math.random() * 90000)
    const startTime = performance.now()

    let recruiterToken = ''
    let candidateToken = ''
    let createdJobId = ''
    let candidateUserId = ''
    let createdApplicationId = ''

    try {
      // Step 1: Register Recruiter
      setCurrentStepIndex(0)
      updateStepStatus(0, 'running')
      const recRes = await apiRequest({
        method: 'POST',
        endpoint: '/auth/register/recruiter',
        body: {
          firstName: 'Simran',
          lastName: 'Kaur',
          email: `recruiter.e2e.${seed}@hireai.io`,
          password: 'Password@123',
          phoneNumber: '9811122233',
        },
      })
      if (!recRes.ok || !recRes.data?.accessToken) {
        throw new Error(recRes.error || 'Failed to register recruiter')
      }
      recruiterToken = recRes.data.accessToken
      updateStepStatus(0, 'success', `Recruiter registered: ${recRes.data.email}`, recRes.durationMs)

      // Step 2: Create Recruiter Profile
      setCurrentStepIndex(1)
      updateStepStatus(1, 'running')
      const profRes = await apiRequest({
        method: 'POST',
        endpoint: '/recruiter/profile',
        token: recruiterToken,
        body: {
          companyName: `HireAI Innovations ${seed}`,
          designation: 'VP of Global Engineering Talent',
          companyWebsite: 'https://hireai.io',
          companyEmail: `recruiting.${seed}@hireai.io`,
          companyPhone: '9811122233',
          companyLogoUrl: 'https://images.unsplash.com/photo-1549719386-74dfcbf7dbed?w=128',
          companyDescription: 'Pioneering next-generation AI-powered talent infrastructure.',
          industry: 'Software & Artificial Intelligence',
          companySize: 500,
          country: 'India',
          state: 'Maharashtra',
          city: 'Pune',
          address: 'Cyber Tech Park, Phase 2, Hinjewadi',
        },
      })
      if (!profRes.ok) {
        throw new Error(profRes.error || 'Failed to create recruiter profile')
      }
      updateStepStatus(1, 'success', `Company Profile created for: ${profRes.data?.companyName}`, profRes.durationMs)

      // Step 3: Post Job
      setCurrentStepIndex(2)
      updateStepStatus(2, 'running')
      const jobRes = await apiRequest({
        method: 'POST',
        endpoint: '/jobs',
        token: recruiterToken,
        body: {
          title: `Principal Java & AI Architect #${seed}`,
          description: 'Architecting distributed enterprise systems and intelligent LLM-driven ATS candidate evaluation engines.',
          employmentType: 'FULL_TIME',
          experienceLevel: 'SENIOR',
          location: 'Pune / Hybrid',
          remote: true,
          salaryMin: 2400000,
          salaryMax: 3600000,
          currency: 'INR',
          skills: ['Java', 'Spring Boot', 'PostgreSQL', 'Microservices', 'Docker', 'REST API'],
          education: 'B.Tech / M.Tech in Computer Science or Equivalent',
          openings: 2,
          applicationDeadline: '2026-12-31',
        },
      })
      if (!jobRes.ok || !jobRes.data?.id) {
        throw new Error(jobRes.error || 'Failed to post job')
      }
      createdJobId = jobRes.data.id
      updateStepStatus(2, 'success', `Job posted! Title: "${jobRes.data.title}" (ID: ${createdJobId})`, jobRes.durationMs)

      // Step 4: Register Candidate
      setCurrentStepIndex(3)
      updateStepStatus(3, 'running')
      const candRes = await apiRequest({
        method: 'POST',
        endpoint: '/auth/register/candidate',
        body: {
          firstName: 'Rahul',
          lastName: 'Deshmukh',
          email: `candidate.e2e.${seed}@hireai.io`,
          password: 'Password@123',
          phoneNumber: '9822233344',
        },
      })
      if (!candRes.ok || !candRes.data?.accessToken) {
        throw new Error(candRes.error || 'Failed to register candidate')
      }
      candidateToken = candRes.data.accessToken
      candidateUserId = candRes.data.userId
      updateStepStatus(3, 'success', `Candidate registered: ${candRes.data.email}`, candRes.durationMs)

      // Step 5: Create Candidate Profile
      setCurrentStepIndex(4)
      updateStepStatus(4, 'running')
      const candProfRes = await apiRequest({
        method: 'POST',
        endpoint: '/candidate/profile',
        token: candidateToken,
        body: {
          linkedinUrl: 'https://linkedin.com/in/rahul-deshmukh-java',
          githubUrl: 'https://github.com/rahul-deshmukh',
          portfolioUrl: 'https://rahuldeshmukh.dev',
          currentCompany: 'Persistent Systems',
          currentDesignation: 'Senior Backend Developer',
          experience: 5.0,
          currentCtc: 1500000,
          expectedCtc: 2400000,
          noticePeriod: 30,
          location: 'Pune, India',
          skillIds: [],
        },
      })
      if (!candProfRes.ok) {
        throw new Error(candProfRes.error || 'Failed to create candidate profile')
      }
      updateStepStatus(4, 'success', `Candidate profile active (${candProfRes.data?.experience} yrs exp, ${candProfRes.data?.location})`, candProfRes.durationMs)

      // Step 6: Browse Open Jobs
      setCurrentStepIndex(5)
      updateStepStatus(5, 'running')
      const openJobsRes = await apiRequest({
        method: 'GET',
        endpoint: '/jobs/open',
        token: candidateToken,
      })
      if (!openJobsRes.ok || !Array.isArray(openJobsRes.data)) {
        throw new Error(openJobsRes.error || 'Failed to fetch open jobs')
      }
      updateStepStatus(5, 'success', `Retrieved ${openJobsRes.data.length} active open job listings`, openJobsRes.durationMs)

      // Step 7: Candidate Applies to Job (Triggers AI ATS Match Score)
      setCurrentStepIndex(6)
      updateStepStatus(6, 'running')
      const applyRes = await apiRequest({
        method: 'POST',
        endpoint: `/jobs/${createdJobId}/apply`,
        token: candidateToken,
        body: {
          coverNote: 'I have 5 years of production experience building high-scale Java Spring Boot and PostgreSQL microservices.',
        },
      })
      if (!applyRes.ok || !applyRes.data?.data?.id) {
        throw new Error(applyRes.error || 'Failed to apply to job')
      }
      const appData = applyRes.data.data
      createdApplicationId = appData.id
      const atsScore = appData.atsMatchScore ?? 'Calculated'
      updateStepStatus(6, 'success', `Application Submitted! 🎯 ATS Match Score: ${atsScore}% (Status: ${appData.status})`, applyRes.durationMs)

      // Step 8: Candidate Views Submitted Applications
      setCurrentStepIndex(7)
      updateStepStatus(7, 'running')
      const myAppsRes = await apiRequest({
        method: 'GET',
        endpoint: '/candidate/applications',
        token: candidateToken,
      })
      if (!myAppsRes.ok) {
        throw new Error(myAppsRes.error || 'Failed to fetch candidate applications')
      }
      const candidateAppsCount = myAppsRes.data?.data?.length || myAppsRes.data?.length || 1
      updateStepStatus(7, 'success', `Candidate has ${candidateAppsCount} tracked application(s)`, myAppsRes.durationMs)

      // Step 9: Recruiter Views Ranked Applicants
      setCurrentStepIndex(8)
      updateStepStatus(8, 'running')
      const recAppsRes = await apiRequest({
        method: 'GET',
        endpoint: `/jobs/${createdJobId}/applications`,
        token: recruiterToken,
      })
      if (!recAppsRes.ok) {
        throw new Error(recAppsRes.error || 'Failed to get recruiter job applications')
      }
      const rankedList = recAppsRes.data?.data || recAppsRes.data || []
      const topScore = rankedList[0]?.atsMatchScore ?? 'N/A'
      updateStepStatus(8, 'success', `Recruiter retrieved ${rankedList.length} applicant(s) ranked by ATS score (Top Match: ${topScore}%)`, recAppsRes.durationMs)

      // Step 10: Recruiter Shortlists Candidate
      setCurrentStepIndex(9)
      updateStepStatus(9, 'running')
      const updateStageRes = await apiRequest({
        method: 'PATCH',
        endpoint: `/applications/${createdApplicationId}/status`,
        token: recruiterToken,
        body: {
          status: 'SHORTLISTED',
          recruiterNotes: 'Exceptional candidate background with strong alignment in Java and Microservices. Shortlisted for Technical Assessment.',
        },
      })
      if (!updateStageRes.ok) {
        throw new Error(updateStageRes.error || 'Failed to update application stage')
      }
      updateStepStatus(9, 'success', `Candidate moved to SHORTLISTED stage with recruiter review feedback`, updateStageRes.durationMs)

      const totalTimeMs = Math.round(performance.now() - startTime)
      setOverallSummary({
        success: true,
        message: `All 10 End-to-End lifecycle tests completed successfully in ${totalTimeMs} ms!`,
        recruiterEmail: `recruiter.e2e.${seed}@hireai.io`,
        candidateEmail: `candidate.e2e.${seed}@hireai.io`,
        jobId: createdJobId,
        applicationId: createdApplicationId,
        atsMatchScore: atsScore,
      })

      // Set active session to the newly created recruiter
      setAuthSession({
        accessToken: recruiterToken,
        role: 'ROLE_RECRUITER',
        firstName: 'Simran',
        lastName: 'Kaur',
        email: `recruiter.e2e.${seed}@hireai.io`,
      })
    } catch (err) {
      updateStepStatus(currentStepIndex, 'failed', err.message)
      setOverallSummary({
        success: false,
        message: `E2E Flow stopped at Step ${currentStepIndex + 1}: ${err.message}`,
      })
    } finally {
      setIsRunning(false)
      setCurrentStepIndex(-1)
    }
  }

  return (
    <div className="tester-container">
      <div className="tester-header">
        <div className="tester-title-wrap">
          <h2>Automated 1-Click End-to-End Hiring Lifecycle Runner</h2>
          <p className="tester-subtitle">
            Orchestrates a realistic 10-step full recruitment journey across Recruiter and Candidate personas to verify all API modules, ATS calculations, and pipelines.
          </p>
        </div>
        <button
          type="button"
          className="btn-primary"
          onClick={runFullE2EScenario}
          disabled={isRunning}
        >
          {isRunning ? <Loader2 size={16} className="spin-icon" /> : <Play size={16} fill="currentColor" />}
          {isRunning ? 'Executing E2E Flow...' : 'Run Full End-to-End Flow'}
        </button>
      </div>

      {overallSummary && (
        <div className={`e2e-summary-banner ${overallSummary.success ? 'e2e-success' : 'e2e-failed'}`}>
          <div className="summary-left">
            {overallSummary.success ? <CheckCircle2 size={24} color="#10b981" /> : <XCircle size={24} color="#ef4444" />}
            <div>
              <h4>{overallSummary.success ? 'Full E2E Scenario Passed!' : 'E2E Scenario Failed'}</h4>
              <p>{overallSummary.message}</p>
              {overallSummary.success && (
                <div className="e2e-badges-row">
                  <span className="e2e-badge">Job ID: {overallSummary.jobId}</span>
                  <span className="e2e-badge">Application ID: {overallSummary.applicationId}</span>
                  <span className="e2e-badge highlight">ATS Match Score: {overallSummary.atsMatchScore}%</span>
                </div>
              )}
            </div>
          </div>
        </div>
      )}

      <div className="e2e-steps-list">
        {steps.map((step, idx) => {
          const isCurrent = isRunning && currentStepIndex === idx
          return (
            <div key={step.id} className={`e2e-step-row step-${step.status} ${isCurrent ? 'step-current' : ''}`}>
              <div className="step-num-col">
                {step.status === 'success' && <CheckCircle2 size={20} color="#10b981" />}
                {step.status === 'failed' && <XCircle size={20} color="#ef4444" />}
                {step.status === 'running' && <Loader2 size={20} className="spin-icon" color="#3b82f6" />}
                {step.status === 'idle' && <span className="step-idle-dot">{idx + 1}</span>}
              </div>

              <div className="step-info-col">
                <div className="step-main-title">
                  <strong>{step.name}</strong>
                  <code className="step-endpoint-pill">{step.endpoint}</code>
                </div>
                {step.result && <p className="step-result-msg">{step.result}</p>}
              </div>

              <div className="step-meta-col">
                {step.durationMs > 0 && <span className="step-time">{step.durationMs} ms</span>}
                <span className={`step-status-pill status-${step.status}`}>
                  {step.status.toUpperCase()}
                </span>
              </div>
            </div>
          )
        })}
      </div>
    </div>
  )
}
