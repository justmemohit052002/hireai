# 📄 HireAI Backend - API Testing & Debugging Specification Manual

**Platform:** Spring Boot 3.5.5 • Java 24 • PostgreSQL 18 • JJWT (Stateless)  
**Target Host:** `http://localhost:8080`  
**Automated Test Matrix Results:** **46 / 46 Passed (100.0% Success Rate)**  
**PDF Manual:** [HireAI_Backend_API_Testing_and_Debugging_Manual.pdf](file:///c:/Users/mohit/Desktop/Vionsys%20HireAI/HireAI_Backend_API_Testing_and_Debugging_Manual.pdf)

---

## 📑 Table of Contents
1. [System Architecture & Roles](#1-system-architecture--roles)
2. [Authentication & Token APIs (`/auth`, `/test`)](#2-authentication--token-apis-auth-test)
3. [User Self-Service APIs (`/users`)](#3-user-self-service-apis-users)
4. [Recruiter Profile APIs (`/recruiter/profile`)](#4-recruiter-profile-apis-recruiterprofile)
5. [Candidate Profiles & Directory APIs (`/candidate/profile`, `/candidates`)](#5-candidate-profiles--directory-apis-candidateprofile-candidates)
6. [Job Postings Management APIs (`/jobs`)](#6-job-postings-management-apis-jobs)
7. [Job Application & Intelligent ATS Match Pipeline](#7-job-application--intelligent-ats-match-pipeline)
8. [Debugging & Root Cause Analysis (RCA)](#8-debugging--root-cause-analysis-rca)
9. [Full 46-Test Verification Matrix](#9-full-46-test-verification-matrix)
10. [Quick-Start Reproduction Commands](#10-quick-start-reproduction-commands)

---

## 1. System Architecture & Roles

HireAI Backend utilizes Spring Security with stateless JWT Bearer token authentication and role-based authorization.

### Role Hierarchy & Privileges:
- **`ROLE_CANDIDATE`**:
  - Register & authenticate
  - Manage own candidate profile (CTC, experience, location, skills)
  - Browse open job postings (`GET /jobs/open`)
  - Submit applications (`POST /jobs/{jobId}/apply`) with real-time ATS match scoring
  - Track submitted applications & pipeline stages (`GET /candidate/applications`)
- **`ROLE_RECRUITER`**:
  - Register & authenticate
  - Manage employer company profile (`/recruiter/profile`)
  - Create, update, and close job postings (`/jobs`)
  - Access candidate directory with multi-field search and pagination (`/candidates`)
  - Review job applicants ranked by ATS match score (`GET /jobs/{jobId}/applications`)
  - Advance applicant recruitment stages (`PATCH /applications/{id}/status`)
- **`ROLE_ADMIN`**:
  - Full administrative access across all endpoints

---

## 2. Authentication & Token APIs (`/auth`, `/test`)

### 2.1 Candidate Registration
- **Method & Path:** `POST /auth/register/candidate`
- **Auth Level:** `Public`
- **Request Payload:**
```json
{
  "firstName": "Rahul",
  "lastName": "Sharma",
  "email": "rahul.sharma@example.com",
  "password": "Password@123",
  "phoneNumber": "9876543210"
}
```
- **Response (`201 Created`):**
```json
{
  "userId": "dabec5fd-643d-434e-b295-064146d01580",
  "firstName": "Rahul",
  "lastName": "Sharma",
  "email": "rahul.sharma@example.com",
  "role": "ROLE_CANDIDATE",
  "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
  "refreshToken": "eyJhbGciOiJIUzUxMiJ9..."
}
```

### 2.2 Recruiter Registration
- **Method & Path:** `POST /auth/register/recruiter`
- **Auth Level:** `Public`
- **Request Payload:**
```json
{
  "firstName": "Priya",
  "lastName": "Deshmukh",
  "email": "priya.recruiter@acmeai.io",
  "password": "Password@123",
  "phoneNumber": "9765432100"
}
```
- **Response (`201 Created`):**
```json
{
  "userId": "363e78f9-a7db-4b64-9b8c-b45db130d624",
  "firstName": "Priya",
  "lastName": "Deshmukh",
  "email": "priya.recruiter@acmeai.io",
  "role": "ROLE_RECRUITER",
  "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
  "refreshToken": "eyJhbGciOiJIUzUxMiJ9..."
}
```

### 2.3 User Login
- **Method & Path:** `POST /auth/login`
- **Auth Level:** `Public`
- **Request Payload:**
```json
{
  "email": "rahul.sharma@example.com",
  "password": "Password@123"
}
```
- **Response (`200 OK`):** Returns freshly issued JWT tokens with role descriptor.

### 2.4 JWT Connectivity Probe
- **Method & Path:** `GET /test`
- **Auth Level:** `Bearer JWT required`
- **Headers:** `Authorization: Bearer <token>`
- **Response (`200 OK`):** `"JWT Authentication Successful"`
- **Response Without Token (`401 Unauthorized`):** Clean 401 via `JwtAuthenticationEntryPoint`.

---

## 3. User Self-Service APIs (`/users`)

### 3.1 Fetch Current User Profile
- **Method & Path:** `GET /users/me`
- **Auth Level:** `Authenticated (Candidate or Recruiter)`
- **Response (`200 OK`):**
```json
{
  "id": "dabec5fd-643d-434e-b295-064146d01580",
  "firstName": "Rahul",
  "lastName": "Sharma",
  "email": "rahul.sharma@example.com",
  "phoneNumber": "9876543210",
  "role": "ROLE_CANDIDATE",
  "enabled": true
}
```

### 3.2 Update Current User Profile
- **Method & Path:** `PUT /users/me`
- **Auth Level:** `Authenticated`
- **Request Payload:**
```json
{
  "firstName": "Rahul",
  "lastName": "Sharma Updated",
  "phoneNumber": "9876543210"
}
```
- **Response (`200 OK`):** Returns updated User DTO.

### 3.3 Get User by UUID
- **Method & Path:** `GET /users/{userId}`
- **Auth Level:** `Authenticated`

---

## 4. Recruiter Profile APIs (`/recruiter/profile`)

### 4.1 Create Recruiter Company Profile
- **Method & Path:** `POST /recruiter/profile`
- **Auth Level:** `ROLE_RECRUITER` or `ROLE_ADMIN`
- **Request Payload:**
```json
{
  "companyName": "Acme AI Technologies Inc",
  "designation": "Lead Talent Acquisition Partner",
  "companyWebsite": "https://acmeai.io",
  "companyEmail": "hr@acmeai.io",
  "companyPhone": "9988776655",
  "industry": "Information Technology",
  "companySize": 250,
  "country": "India",
  "state": "Maharashtra",
  "city": "Pune",
  "address": "Viman Nagar IT Park, Pune"
}
```
- **Response (`200 OK`):** Returns saved recruiter profile.
- **RBAC Check:** If called with `ROLE_CANDIDATE` token, returns `403 Forbidden`.

### 4.2 Get Logged-in Recruiter Profile
- **Method & Path:** `GET /recruiter/profile`
- **Auth Level:** `ROLE_RECRUITER`

### 4.3 Get Recruiter Profile by User UUID
- **Method & Path:** `GET /recruiter/profile/{userId}`
- **Auth Level:** `ROLE_RECRUITER`

### 4.4 Update Recruiter Profile
- **Method & Path:** `PUT /recruiter/profile`
- **Auth Level:** `ROLE_RECRUITER`

---

## 5. Candidate Profiles & Directory APIs (`/candidate/profile`, `/candidates`)

### 5.1 Candidate Self-Service Profile Creation
- **Method & Path:** `POST /candidate/profile`
- **Auth Level:** `ROLE_CANDIDATE`
- **Request Payload:**
```json
{
  "linkedinUrl": "https://linkedin.com/in/rahulsharma",
  "githubUrl": "https://github.com/rahulsharma",
  "portfolioUrl": "https://rahul.dev",
  "currentCompany": "Tech Pioneers Ltd",
  "currentDesignation": "Senior Java Backend Engineer",
  "experience": 5.5,
  "currentCtc": 1800000,
  "expectedCtc": 2400000,
  "noticePeriod": 30,
  "location": "Pune, India",
  "skillIds": []
}
```
- **Response (`201 Created`):**
```json
{
  "id": "8f73e2fb-38db-46fd-bf76-271211421f3e",
  "candidateId": "CAN-2026-000011",
  "firstName": "Rahul",
  "lastName": "Sharma",
  "currentDesignation": "Senior Java Backend Engineer",
  "experience": 5.5,
  "candidateStatus": "ACTIVE",
  "location": "Pune, India"
}
```

### 5.2 Recruiter Candidate Directory Search & Pagination
- **Method & Path:** `GET /candidates?location=Pune&candidateStatus=ACTIVE&page=0&size=10`
- **Auth Level:** `ROLE_RECRUITER` or `ROLE_ADMIN`
- **Response (`200 OK`):**
```json
{
  "content": [
    {
      "id": "8f73e2fb-38db-46fd-bf76-271211421f3e",
      "candidateId": "CAN-2026-000011",
      "firstName": "Rahul",
      "lastName": "Sharma",
      "experience": 5.5,
      "candidateStatus": "ACTIVE",
      "location": "Pune, India"
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "size": 10,
  "number": 0
}
```

### 5.3 Direct Candidate CRUD for Recruiters
- `POST /candidates`: Recruiter directly adds candidate entry (`201 Created`).
- `GET /candidates/{candidateId}`: Recruiter fetches candidate by UUID (`200 OK`).
- `PUT /candidates/{candidateId}`: Recruiter updates candidate details (`200 OK`).
- `DELETE /candidates/{candidateId}`: Recruiter soft-deletes candidate (`204 No Content`).

---

## 6. Job Postings Management APIs (`/jobs`)

### 6.1 Create Job Posting
- **Method & Path:** `POST /jobs`
- **Auth Level:** `ROLE_RECRUITER`
- **Request Payload:**
```json
{
  "title": "Senior Java Backend Engineer",
  "description": "Looking for Spring Boot, PostgreSQL, and Microservices developer to build high throughput engines.",
  "employmentType": "FULL_TIME",
  "experienceLevel": "SENIOR",
  "location": "Pune, Maharashtra",
  "remote": true,
  "salaryMin": 1800000,
  "salaryMax": 2800000,
  "currency": "INR",
  "skills": ["Java", "Spring Boot", "PostgreSQL", "Microservices", "Docker"],
  "education": "Bachelor or Master in CS/IT",
  "openings": 3,
  "applicationDeadline": "2026-09-30"
}
```
- **Response (`201 Created`):** Returns saved Job with status `OPEN`.

### 6.2 Candidate Browse Open Vacancies
- **Method & Path:** `GET /jobs/open`
- **Auth Level:** `ROLE_CANDIDATE` or `ROLE_RECRUITER`
- **Response (`200 OK`):** List of open active jobs across the system.

### 6.3 Recruiter View Own Jobs
- **Method & Path:** `GET /jobs`
- **Auth Level:** `ROLE_RECRUITER`

### 6.4 Close Job Posting
- **Method & Path:** `PATCH /jobs/{jobId}/close`
- **Auth Level:** `ROLE_RECRUITER`
- **Response (`204 No Content`):** Sets job status to `CLOSED`.

---

## 7. Job Application & Intelligent ATS Match Pipeline

```
Candidate Profile               Job Requirements
(Skills, Exp, Title)            (Skills, Level, Title)
         │                               │
         └───────────────┬───────────────┘
                         ▼
        ┌──────────────────────────────────┐
        │     ATS Match Scoring Engine     │
        │ • Skill Match Weight: 60%        │
        │ • Experience Weight: 25%         │
        │ • Title Match Weight: 15%        │
        └────────────────┬─────────────────┘
                         ▼
             Score >= 70% Threshold?
             ┌───────────┴───────────┐
            YES                      NO
             │                       │
             ▼                       ▼
    Status: SHORTLISTED       Status: APPLIED
    (Auto-Shortlisted)        (Awaiting Review)
```

### 7.1 Candidate Apply to Job
- **Method & Path:** `POST /jobs/{jobId}/apply`
- **Auth Level:** `ROLE_CANDIDATE`
- **Request Payload:**
```json
{
  "coverNote": "5+ years developing Spring Boot cloud microservices and high-concurrency systems."
}
```
- **Response (`201 Created`):**
```json
{
  "success": true,
  "message": "Application submitted successfully with ATS match score calculated",
  "data": {
    "id": "e7bb59e2-c8b0-4d83-a50a-2167e32bd76d",
    "jobTitle": "Senior Java Backend Engineer",
    "candidateName": "Rahul Sharma",
    "atsMatchScore": 40,
    "matchingSkills": ["Java", "Spring Boot"],
    "missingSkills": ["PostgreSQL", "Docker", "Microservices"],
    "status": "APPLIED",
    "recruiterNotes": "ATS Match Score: 40% (Awaiting manual recruiter review)"
  }
}
```

### 7.2 Candidate View My Applications
- **Method & Path:** `GET /candidate/applications`
- **Auth Level:** `ROLE_CANDIDATE`
- **Response (`200 OK`):** List of candidate's applications with live recruitment status.

### 7.3 Recruiter View Applicants Ranked by ATS Score
- **Method & Path:** `GET /jobs/{jobId}/applications`
- **Auth Level:** `ROLE_RECRUITER` (Owner of Job)
- **Response (`200 OK`):** Applications sorted descending by `atsMatchScore`.

### 7.4 Recruiter Advance Pipeline Stage
- **Method & Path:** `PATCH /applications/{applicationId}/status`
- **Auth Level:** `ROLE_RECRUITER`
- **Request Payload:**
```json
{
  "status": "INTERVIEW_SCHEDULED",
  "recruiterNotes": "Passed initial screening. Scheduled Round 1 Technical Architecture interview."
}
```
- **Response (`200 OK`):** Updated application record.

---

## 8. Debugging & Root Cause Analysis (RCA)

### 🐛 Bug 1: Missing User Relationship in `createCandidate` (HTTP 409 / 500)
- **Symptoms:** When a recruiter called `POST /candidates`, the server returned a database constraint violation.
- **Root Cause:** In [`CandidateServiceImpl.java`](file:///c:/Users/mohit/Desktop/Vionsys%20HireAI/hireai-backend/src/main/java/com/vionsys/hireai/candidate/service/impl/CandidateServiceImpl.java), `createCandidate` was building a `Candidate` entity without populating the mandatory `@OneToOne private User user;` field. PostgreSQL rejected the transaction because column `user_id` on table `candidates` is `NOT NULL`.
- **Fix:** Added `User user = userRepository.findById(request.getUserId()).orElseThrow(...)` and `candidate.setUser(user);` prior to `candidateRepository.save(candidate)`.

### 🐛 Bug 2: Access Denied Handled as 401 Unauthorized instead of 403 Forbidden
- **Symptoms:** When an authenticated candidate accessed recruiter endpoints, the server responded with `401 Unauthorized` instead of `403 Forbidden`.
- **Root Cause:** Spring Security filter chain was missing an `AccessDeniedHandler`, and error forwards to `/error` were not permitted in `SecurityConfig.java`, resulting in Spring Security re-evaluating the forward as unauthenticated.
- **Fix:** Implemented [`JwtAccessDeniedHandler.java`](file:///c:/Users/mohit/Desktop/Vionsys%20HireAI/hireai-backend/src/main/java/com/vionsys/hireai/security/jwt/JwtAccessDeniedHandler.java) and configured `.accessDeniedHandler(jwtAccessDeniedHandler)` along with `/error` permit-all in [`SecurityConfig.java`](file:///c:/Users/mohit/Desktop/Vionsys%20HireAI/hireai-backend/src/main/java/com/vionsys/hireai/security/SecurityConfig.java).

### 🐛 Bug 3: Malformed JWT Caused 500 Server Error
- **Symptoms:** Sending an invalid or corrupt token string triggered an unhandled exception inside [`JwtAuthenticationFilter.java`](file:///c:/Users/mohit/Desktop/Vionsys%20HireAI/hireai-backend/src/main/java/com/vionsys/hireai/security/jwt/JwtAuthenticationFilter.java).
- **Root Cause:** `io.jsonwebtoken` parser threw unchecked `JwtException` which bypassed the standard dispatcher exception handler.
- **Fix:** Wrapped token extraction in a try-catch block inside `JwtAuthenticationFilter.doFilterInternal` to cleanly clear the security context and delegate unauthenticated responses to `JwtAuthenticationEntryPoint` (HTTP 401).

---

## 9. Full 46-Test Verification Matrix

| # | Test Scenario | Method & Endpoint | Auth Role | Result |
| :---: | :--- | :--- | :---: | :---: |
| 1 | Unauthenticated Probe | `GET /test` | None | 🟢 **401 PASS** |
| 2 | Malformed Token Probe | `GET /test` | Corrupt JWT | 🟢 **401 PASS** |
| 3 | Candidate Registration | `POST /auth/register/candidate` | Public | 🟢 **201 PASS** |
| 4 | Recruiter Registration | `POST /auth/register/recruiter` | Public | 🟢 **201 PASS** |
| 5 | Duplicate Email Prevention | `POST /auth/register/candidate` | Public | 🟢 **409 PASS** |
| 6 | Candidate Login | `POST /auth/login` | Public | 🟢 **200 PASS** |
| 7 | Recruiter Login | `POST /auth/login` | Public | 🟢 **200 PASS** |
| 8 | Invalid Password Login | `POST /auth/login` | Public | 🟢 **401 PASS** |
| 9 | Authenticated Token Probe | `GET /test` | Candidate | 🟢 **200 PASS** |
| 10 | Get Current User Profile | `GET /users/me` | Authenticated | 🟢 **200 PASS** |
| 11 | Update User Basic Info | `PUT /users/me` | Authenticated | 🟢 **200 PASS** |
| 12 | Get User by UUID | `GET /users/{userId}` | Authenticated | 🟢 **200 PASS** |
| 13 | Candidate Access Recruiter Profile | `POST /recruiter/profile` | Candidate | 🟢 **403 PASS** |
| 14 | Create Recruiter Company Profile | `POST /recruiter/profile` | Recruiter | 🟢 **200 PASS** |
| 15 | Get Recruiter Company Profile | `GET /recruiter/profile` | Recruiter | 🟢 **200 PASS** |
| 16 | Update Recruiter Company Profile | `PUT /recruiter/profile` | Recruiter | 🟢 **200 PASS** |
| 17 | Get Recruiter Profile by User ID | `GET /recruiter/profile/{id}` | Recruiter | 🟢 **200 PASS** |
| 18 | Recruiter Access Candidate Profile | `POST /candidate/profile` | Recruiter | 🟢 **403 PASS** |
| 19 | Create Candidate Profile | `POST /candidate/profile` | Candidate | 🟢 **201 PASS** |
| 20 | Get Candidate Profile | `GET /candidate/profile` | Candidate | 🟢 **200 PASS** |
| 21 | Update Candidate Profile | `PUT /candidate/profile` | Candidate | 🟢 **200 PASS** |
| 22 | Recruiter Direct Candidate Creation | `POST /candidates` | Recruiter | 🟢 **201 PASS** |
| 23 | Recruiter Direct Candidate Update | `PUT /candidates/{id}` | Recruiter | 🟢 **200 PASS** |
| 24 | Recruiter Soft-Delete Candidate | `DELETE /candidates/{id}` | Recruiter | 🟢 **204 PASS** |
| 25 | Paginated Candidate Directory | `GET /candidates` | Recruiter | 🟢 **200 PASS** |
| 26 | Multi-Filter Candidate Search | `GET /candidates?filters` | Recruiter | 🟢 **200 PASS** |
| 27 | Get Candidate by UUID | `GET /candidates/{id}` | Recruiter | 🟢 **200 PASS** |
| 28 | Candidate Access Recruiter Directory | `GET /candidates` | Candidate | 🟢 **403 PASS** |
| 29 | Candidate Post Job Violation | `POST /jobs` | Candidate | 🟢 **403 PASS** |
| 30 | Recruiter Post Job | `POST /jobs` | Recruiter | 🟢 **201 PASS** |
| 31 | Recruiter View Own Jobs | `GET /jobs` | Recruiter | 🟢 **200 PASS** |
| 32 | Candidate Browse Open Jobs | `GET /jobs/open` | Candidate | 🟢 **200 PASS** |
| 33 | Recruiter Get Job by ID | `GET /jobs/{jobId}` | Recruiter | 🟢 **200 PASS** |
| 34 | Recruiter Update Job | `PUT /jobs/{jobId}` | Recruiter | 🟢 **200 PASS** |
| 35 | Candidate Apply to Job + ATS Score | `POST /jobs/{id}/apply` | Candidate | 🟢 **201 PASS** |
| 36 | Duplicate Job Application Conflict | `POST /jobs/{id}/apply` | Candidate | 🟢 **409 PASS** |
| 37 | Candidate View Applications | `GET /candidate/applications` | Candidate | 🟢 **200 PASS** |
| 38 | Recruiter View Ranked Applicants | `GET /jobs/{id}/applications` | Recruiter | 🟢 **200 PASS** |
| 39 | Candidate Alter Pipeline Stage Violation | `PATCH /applications/{id}/status` | Candidate | 🟢 **403 PASS** |
| 40 | Recruiter Advance Pipeline Stage | `PATCH /applications/{id}/status` | Recruiter | 🟢 **200 PASS** |
| 41 | View Application Detail Breakdown | `GET /applications/{id}` | Candidate | 🟢 **200 PASS** |
| 42 | Recruiter Close Job Posting | `PATCH /jobs/{id}/close` | Recruiter | 🟢 **204 PASS** |
| 43 | Confirm Job Status CLOSED | `GET /jobs/{id}` | Recruiter | 🟢 **200 PASS** |
| 44 | Query Non-Existent UUID | `GET /jobs/{fakeUUID}` | Recruiter | 🟢 **404 PASS** |
| 45 | Malformed JSON Body Parsing | `POST /auth/login` | Public | 🟢 **400 PASS** |
| 46 | Missing Required Field Validation | `POST /auth/register/candidate` | Public | 🟢 **400 PASS** |

---

## 10. Quick-Start Reproduction Commands

### Run Full Test Suite:
```powershell
node "c:\Users\mohit\Desktop\Vionsys HireAI\hireai-backend\test_runner.mjs"
```

### Regenerate PDF Manual:
```powershell
node "c:\Users\mohit\Desktop\Vionsys HireAI\hireai-backend\generate_pdf.js"
```
