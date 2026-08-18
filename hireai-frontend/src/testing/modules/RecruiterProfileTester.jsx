import React, { useState } from 'react'
import EndpointCard from '../components/EndpointCard'
import { useAuth } from '../context/AuthContext'

export default function RecruiterProfileTester() {
  const { user } = useAuth()

  const sampleCreateRecruiterProfile = {
    companyName: 'TechVionsys Global AI Inc',
    designation: 'Director of Talent Acquisition',
    companyWebsite: 'https://vionsys.com',
    companyEmail: 'careers@vionsys.com',
    companyPhone: '9812345678',
    companyLogoUrl: 'https://images.unsplash.com/photo-1549719386-74dfcbf7dbed?w=128',
    companyDescription: 'Enterprise AI and full-stack software development company building scalable SaaS solutions.',
    industry: 'Information Technology & AI Services',
    companySize: 250,
    country: 'India',
    state: 'Maharashtra',
    city: 'Pune',
    address: 'Vionsys Tech Park, Hinjewadi Phase 1',
  }

  const sampleUpdateRecruiterProfile = {
    companyName: 'TechVionsys Global Solutions',
    designation: 'Head of Global Recruitment',
    companyWebsite: 'https://vionsys.com/careers',
    companyEmail: 'talent@vionsys.com',
    companyPhone: '9812345679',
    companyLogoUrl: 'https://images.unsplash.com/photo-1549719386-74dfcbf7dbed?w=128',
    companyDescription: 'Leading provider of next-gen cloud recruitment platforms and intelligent ATS solutions.',
    industry: 'Software Development & Artificial Intelligence',
    companySize: 300,
    country: 'India',
    state: 'Maharashtra',
    city: 'Pune',
    address: 'Vionsys Tech Park Tower 2, Hinjewadi',
  }

  return (
    <div className="tester-container">
      <div className="tester-header">
        <div className="tester-title-wrap">
          <h2>Recruiter Profile Management Module (/recruiter/profile)</h2>
          <p className="tester-subtitle">
            Create, retrieve, and update corporate employer profiles, company metadata, and recruitment ownership.
          </p>
        </div>
      </div>

      <div className="endpoints-list">
        {/* POST /recruiter/profile */}
        <EndpointCard
          title="Create Recruiter Company Profile"
          description="Creates an employer company profile linked to the authenticated recruiter user account."
          method="POST"
          path="/recruiter/profile"
          authRequired="RECRUITER"
          defaultPayload={sampleCreateRecruiterProfile}
        />

        {/* GET /recruiter/profile */}
        <EndpointCard
          title="Get My Recruiter Profile"
          description="Fetches company profile details for the currently logged-in recruiter."
          method="GET"
          path="/recruiter/profile"
          authRequired="RECRUITER"
        />

        {/* GET /recruiter/profile/{userId} */}
        <EndpointCard
          title="Get Recruiter Profile by User ID"
          description="Fetches recruiter company profile by associated User UUID."
          method="GET"
          path="/recruiter/profile/{userId}"
          pathParams={['userId']}
          authRequired="AUTHENTICATED"
        />

        {/* PUT /recruiter/profile */}
        <EndpointCard
          title="Update Recruiter Profile"
          description="Updates company details, designation, contact information, and logo for the recruiter."
          method="PUT"
          path="/recruiter/profile"
          authRequired="RECRUITER"
          defaultPayload={sampleUpdateRecruiterProfile}
        />
      </div>
    </div>
  )
}
