import React, { useState } from 'react'
import EndpointCard from '../components/EndpointCard'
import { useAuth } from '../context/AuthContext'

export default function ApplicationTester() {
  const { user } = useAuth()

  const sampleApplyRequest = {
    coverNote: 'I am highly enthusiastic about this role. I have extensive hands-on experience designing Spring Boot microservices and RESTful backends in production.',
  }

  const sampleUpdateStatus = {
    status: 'SHORTLISTED',
    recruiterNotes: 'Candidate demonstrated strong architectural depth and deep familiarity with Spring Boot and PostgreSQL. Advanced to Technical Round 1.',
  }

  return (
    <div className="tester-container">
      <div className="tester-header">
        <div className="tester-title-wrap">
          <h2>Job Applications & AI ATS Scoring Engine (/applications)</h2>
          <p className="tester-subtitle">
            Test candidate job submissions with real-time AI ATS match score calculations, recruiter applicant ranking, and stage pipelines.
          </p>
        </div>
      </div>

      <div className="tester-section-heading">
        <h3>🎯 Candidate Application & ATS Scoring</h3>
      </div>

      <div className="endpoints-list">
        {/* POST /jobs/{jobId}/apply */}
        <EndpointCard
          title="Apply to Job (/jobs/{jobId}/apply)"
          description="Candidate submits job application with cover note. Automatically triggers the intelligent ATS Match Engine to evaluate candidate profile skills and experience against job criteria."
          method="POST"
          path="/jobs/{jobId}/apply"
          pathParams={['jobId']}
          authRequired="CANDIDATE"
          defaultPayload={sampleApplyRequest}
        />

        {/* GET /candidate/applications */}
        <EndpointCard
          title="View My Applications (/candidate/applications)"
          description="Candidate retrieves all their submitted applications along with real-time stage status and ATS match percentages."
          method="GET"
          path="/candidate/applications"
          authRequired="CANDIDATE"
        />
      </div>

      <div className="tester-section-heading" style={{ marginTop: '2.5rem' }}>
        <h3>📊 Recruiter ATS Pipeline & Applicant Ranking</h3>
      </div>

      <div className="endpoints-list">
        {/* GET /jobs/{jobId}/applications */}
        <EndpointCard
          title="List Job Applicants Ranked by ATS Match (/jobs/{jobId}/applications)"
          description="Recruiter retrieves all candidate applications for a specific job, automatically ordered from highest ATS match score to lowest."
          method="GET"
          path="/jobs/{jobId}/applications"
          pathParams={['jobId']}
          authRequired="RECRUITER"
        />

        {/* PATCH /applications/{applicationId}/status */}
        <EndpointCard
          title="Update Application Stage & Feedback (/applications/{applicationId}/status)"
          description="Recruiter updates candidate stage (SCREENING, SHORTLISTED, INTERVIEW_SCHEDULED, OFFERED, REJECTED) and appends recruiter review notes."
          method="PATCH"
          path="/applications/{applicationId}/status"
          pathParams={['applicationId']}
          authRequired="RECRUITER"
          defaultPayload={sampleUpdateStatus}
        />

        {/* GET /applications/{applicationId} */}
        <EndpointCard
          title="Get Application Details by ID (/applications/{applicationId})"
          description="Fetches full application details, timestamp, cover note, and matching/missing skills breakdown."
          method="GET"
          path="/applications/{applicationId}"
          pathParams={['applicationId']}
          authRequired="AUTHENTICATED"
        />
      </div>
    </div>
  )
}
