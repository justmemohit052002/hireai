import React from 'react'
import EndpointCard from '../components/EndpointCard'
import { useAuth } from '../context/AuthContext'

export default function UserTester() {
  const { user } = useAuth()

  const sampleUpdateUser = {
    firstName: user?.firstName || 'Aarav',
    lastName: user?.lastName || 'Sharma',
    phoneNumber: '9876543210',
  }

  return (
    <div className="tester-container">
      <div className="tester-header">
        <div className="tester-title-wrap">
          <h2>User Self-Service Module (/users)</h2>
          <p className="tester-subtitle">
            Fetch authenticated user details, update profile information, and inspect users by UUID.
          </p>
        </div>
      </div>

      <div className="endpoints-list">
        {/* GET /users/me */}
        <EndpointCard
          title="Get Current User Profile (/users/me)"
          description="Retrieves the profile of the currently authenticated user from SecurityContext."
          method="GET"
          path="/users/me"
          authRequired="AUTHENTICATED"
        />

        {/* PUT /users/me */}
        <EndpointCard
          title="Update Current User (/users/me)"
          description="Updates personal details (firstName, lastName, phoneNumber) of the logged-in user."
          method="PUT"
          path="/users/me"
          authRequired="AUTHENTICATED"
          defaultPayload={sampleUpdateUser}
        />

        {/* GET /users/{userId} */}
        <EndpointCard
          title="Get User by ID (/users/{userId})"
          description="Retrieves user details for a given User UUID."
          method="GET"
          path="/users/{userId}"
          pathParams={['userId']}
          authRequired="AUTHENTICATED"
        />
      </div>
    </div>
  )
}
