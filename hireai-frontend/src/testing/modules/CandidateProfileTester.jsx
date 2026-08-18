import React, { useState } from 'react'
import EndpointCard from '../components/EndpointCard'
import { useAuth } from '../context/AuthContext'

export default function CandidateProfileTester() {
  const { user } = useAuth()

  const sampleCandidateProfile = {
    linkedinUrl: 'https://linkedin.com/in/arjunsharma-dev',
    githubUrl: 'https://github.com/arjunsharma',
    portfolioUrl: 'https://arjunsharma.dev',
    currentCompany: 'Infosys Ltd',
    currentDesignation: 'Senior Java Backend Engineer',
    experience: 4.5,
    currentCtc: 1200000,
    expectedCtc: 1800000,
    noticePeriod: 30,
    location: 'Pune, Maharashtra',
    skillIds: [],
  }

  const sampleAdminCreateCandidate = {
    userId: user?.userId || '00000000-0000-0000-0000-000000000000',
    firstName: 'Vikram',
    lastName: 'Malhotra',
    email: 'vikram.candidate@test.com',
    phone: '9876543211',
    linkedinUrl: 'https://linkedin.com/in/vikram-dev',
    githubUrl: 'https://github.com/vikram-dev',
    portfolioUrl: 'https://vikram.me',
    currentCompany: 'TCS',
    currentDesignation: 'Full Stack Java Developer',
    experience: 3.5,
    currentCtc: 900000,
    expectedCtc: 1400000,
    noticePeriod: 60,
    location: 'Bangalore, Karnataka',
    skillIds: [],
  }

  return (
    <div className="tester-container">
      <div className="tester-header">
        <div className="tester-title-wrap">
          <h2>Candidate Module & Directory (/candidate/profile, /candidates)</h2>
          <p className="tester-subtitle">
            Candidate self-service profile endpoints and recruiter candidate directory with search, filtering, and pagination.
          </p>
        </div>
      </div>

      <div className="tester-section-heading">
        <h3>👤 Candidate Self-Service APIs (Authenticated Candidate)</h3>
      </div>

      <div className="endpoints-list">
        {/* POST /candidate/profile */}
        <EndpointCard
          title="Create Candidate Profile (/candidate/profile)"
          description="Creates candidate professional profile (experience, CTC, notice period, portfolio links) for the logged-in candidate."
          method="POST"
          path="/candidate/profile"
          authRequired="CANDIDATE"
          defaultPayload={sampleCandidateProfile}
        />

        {/* GET /candidate/profile */}
        <EndpointCard
          title="Get My Candidate Profile (/candidate/profile)"
          description="Retrieves the authenticated candidate's own professional profile."
          method="GET"
          path="/candidate/profile"
          authRequired="CANDIDATE"
        />

        {/* PUT /candidate/profile */}
        <EndpointCard
          title="Update My Candidate Profile (/candidate/profile)"
          description="Updates the authenticated candidate's career details, CTC expectations, and links."
          method="PUT"
          path="/candidate/profile"
          authRequired="CANDIDATE"
          defaultPayload={{
            ...sampleCandidateProfile,
            currentDesignation: 'Lead Java & Cloud Architect',
            experience: 5.5,
            expectedCtc: 2200000,
          }}
        />
      </div>

      <div className="tester-section-heading" style={{ marginTop: '2.5rem' }}>
        <h3>🏢 Recruiter / Admin Candidate Management APIs (/candidates)</h3>
      </div>

      <div className="endpoints-list">
        {/* GET /candidates (List & Filter) */}
        <EndpointCard
          title="Filter & Paginate Candidates (/candidates)"
          description="Search candidates by skill, location, experience, candidate status (ACTIVE, INACTIVE, HIRED, REJECTED), email, and page/size."
          method="GET"
          path="/candidates"
          authRequired="RECRUITER"
          queryParams={[
            'skill',
            'location',
            'candidateStatus',
            'experience',
            'firstName',
            'email',
            'page',
            'size',
            'sortBy',
            'direction',
          ]}
        />

        {/* POST /candidates */}
        <EndpointCard
          title="Create Candidate Record (/candidates)"
          description="Recruiter/Admin endpoint to create a candidate profile linked to a specific user ID."
          method="POST"
          path="/candidates"
          authRequired="RECRUITER"
          defaultPayload={sampleAdminCreateCandidate}
        />

        {/* GET /candidates/{candidateId} */}
        <EndpointCard
          title="Get Candidate by ID (/candidates/{candidateId})"
          description="Fetch a specific candidate's complete record by Candidate UUID."
          method="GET"
          path="/candidates/{candidateId}"
          pathParams={['candidateId']}
          authRequired="RECRUITER"
        />

        {/* PUT /candidates/{candidateId} */}
        <EndpointCard
          title="Update Candidate Record (/candidates/{candidateId})"
          description="Update a candidate's complete information."
          method="PUT"
          path="/candidates/{candidateId}"
          pathParams={['candidateId']}
          authRequired="RECRUITER"
          defaultPayload={sampleAdminCreateCandidate}
        />

        {/* DELETE /candidates/{candidateId} */}
        <EndpointCard
          title="Delete Candidate Record (/candidates/{candidateId})"
          description="Delete a candidate record by UUID."
          method="DELETE"
          path="/candidates/{candidateId}"
          pathParams={['candidateId']}
          authRequired="RECRUITER"
        />
      </div>
    </div>
  )
}
