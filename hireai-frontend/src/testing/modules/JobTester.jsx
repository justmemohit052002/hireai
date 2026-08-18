import React, { useState } from 'react'
import EndpointCard from '../components/EndpointCard'
import { useAuth } from '../context/AuthContext'

export default function JobTester() {
  const { user } = useAuth()

  const sampleCreateJob = {
    title: 'Senior Java & AI Backend Engineer',
    description: 'We are seeking an experienced Senior Java Developer to design scalable microservices and integrate LLM-powered recruitment pipelines.',
    employmentType: 'FULL_TIME',
    experienceLevel: 'SENIOR',
    location: 'Pune, Maharashtra',
    remote: true,
    salaryMin: 1800000,
    salaryMax: 2800000,
    currency: 'INR',
    skills: ['Java', 'Spring Boot', 'PostgreSQL', 'Microservices', 'Docker', 'REST API', 'LLM'],
    education: 'B.Tech / B.E. in Computer Science, IT or related disciplines',
    openings: 2,
    applicationDeadline: '2026-12-31',
  }

  const sampleUpdateJob = {
    title: 'Staff Java Cloud Architect',
    description: 'Lead engineering teams building high-throughput recruitment platforms with AI ATS scoring engines.',
    employmentType: 'FULL_TIME',
    experienceLevel: 'LEAD',
    location: 'Pune / Remote',
    remote: true,
    salaryMin: 2500000,
    salaryMax: 3800000,
    currency: 'INR',
    skills: ['Java 21', 'Spring Boot 3', 'PostgreSQL', 'Kubernetes', 'AWS', 'System Design'],
    education: 'Bachelor or Master in Computer Science',
    openings: 1,
    applicationDeadline: '2026-12-31',
  }

  return (
    <div className="tester-container">
      <div className="tester-header">
        <div className="tester-title-wrap">
          <h2>Job Management Module (/jobs)</h2>
          <p className="tester-subtitle">
            Recruiter job posting, open job browsing for candidates, job updates, and vacancy closure.
          </p>
        </div>
      </div>

      <div className="endpoints-list">
        {/* POST /jobs */}
        <EndpointCard
          title="Post a New Job (Recruiter Only)"
          description="Creates an active job posting under the logged-in recruiter's company profile."
          method="POST"
          path="/jobs"
          authRequired="RECRUITER"
          defaultPayload={sampleCreateJob}
        />

        {/* GET /jobs/open */}
        <EndpointCard
          title="Browse All Open Jobs (/jobs/open)"
          description="Public / Candidate job discovery endpoint to explore all active positions with required skills and compensation."
          method="GET"
          path="/jobs/open"
          authRequired="AUTHENTICATED"
        />

        {/* GET /jobs */}
        <EndpointCard
          title="List My Posted Jobs (Recruiter Only)"
          description="Retrieves all jobs created and managed by the authenticated recruiter."
          method="GET"
          path="/jobs"
          authRequired="RECRUITER"
        />

        {/* GET /jobs/{jobId} */}
        <EndpointCard
          title="Get Job by ID (/jobs/{jobId})"
          description="Fetches full details of a specific job posting."
          method="GET"
          path="/jobs/{jobId}"
          pathParams={['jobId']}
          authRequired="RECRUITER"
        />

        {/* PUT /jobs/{jobId} */}
        <EndpointCard
          title="Update Job Posting (/jobs/{jobId})"
          description="Modifies job specifications, skills, salary, and requirements."
          method="PUT"
          path="/jobs/{jobId}"
          pathParams={['jobId']}
          authRequired="RECRUITER"
          defaultPayload={sampleUpdateJob}
        />

        {/* PATCH /jobs/{jobId}/close */}
        <EndpointCard
          title="Close Job Posting (/jobs/{jobId}/close)"
          description="Changes job status to CLOSED, archiving the posting from candidate search."
          method="PATCH"
          path="/jobs/{jobId}/close"
          pathParams={['jobId']}
          authRequired="RECRUITER"
        />
      </div>
    </div>
  )
}
