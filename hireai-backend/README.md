# 🚀 HireAI - AI Powered Recruitment Platform

<div align="center">

![Java](https://img.shields.io/badge/Java-21-orange?style=for-the-badge&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.x-6DB33F?style=for-the-badge&logo=springboot)
![React](https://img.shields.io/badge/React.js-19-61DAFB?style=for-the-badge&logo=react)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-18-4169E1?style=for-the-badge&logo=postgresql)
![JWT](https://img.shields.io/badge/JWT-Authentication-black?style=for-the-badge&logo=jsonwebtokens)

**An Enterprise AI-Powered Recruitment Platform built with Spring Boot, React.js, PostgreSQL and Large Language Models (LLMs).**

</div>

------------------------------------------------------------------------

# 📖 Overview

HireAI is a modern AI-powered recruitment platform designed to simplify
and automate the hiring process for **Candidates**, **Recruiters**, and
**Administrators**.

The platform combines enterprise-grade backend architecture with an
interactive React frontend and Artificial Intelligence to provide
intelligent hiring solutions such as:

- 🤖 ATS Resume Score
- 📄 Resume Parsing
- 🎯 AI Candidate Ranking
- 💼 Job Recommendation
- 🧠 LLM Powered Resume Analysis
- 📊 Recruiter Dashboard
- 👤 Candidate Profile Management

The project follows an **Enterprise Software Development Lifecycle
(SDLC)** using Sprint-based development.

------------------------------------------------------------------------

# 🎯 Objectives

- Build an enterprise-level Recruitment Platform
- Implement secure JWT authentication and role-based authorization
- Develop scalable REST APIs
- Build recruiter and job management modules
- Integrate AI-powered resume analysis and candidate matching
- Build a modern React frontend
- Follow clean architecture and backend best practices
- Prepare the application for AWS deployment

------------------------------------------------------------------------

# ✨ Features

## ✅ Authentication & Authorization

- User Registration
- Recruiter Registration
- User Login
- JWT Authentication
- Access Token
- Refresh Token
- BCrypt Password Encryption
- Spring Security
- Role-Based Authorization
- Custom Authentication Filter
- Custom Authentication Entry Point
- Global Exception Handling
- Stateless Session Management

## ✅ User Management

- View Current User Profile
- View User by ID
- Update User Profile
- Role Management

## ✅ Recruiter Profile

- Create Recruiter Profile
- Get Logged-in Recruiter Profile
- Company Information
- Recruiter Designation
- Company Website
- Company Email and Phone
- Company Logo URL
- Company Description
- Industry
- Company Size
- Company Location
- Recruiter Ownership through authenticated user

## ✅ Job Management

- Create Job
- Get Logged-in Recruiter's Jobs
- Get Job by ID
- Update Job
- Close Job
- Job Status Management
- Employment Type
- Experience Level
- Salary Range
- Skills
- Education Requirements
- Application Deadline
- Remote Job Support
- Recruiter-based Job Ownership
- Recruiter-only API Authorization

## 🚧 Upcoming Features

### Candidate Module

- Candidate Profile
- Skills
- Education
- Experience
- Resume Upload
- Portfolio
- LinkedIn
- GitHub

### Application Module

- Apply for Jobs
- Application Tracking
- Shortlisting
- Interview Scheduling

### Resume Module

- Resume Upload
- Resume Parsing
- Resume Versioning

### AI Module

- ATS Resume Score
- Resume Analysis
- Resume Suggestions
- Candidate Ranking
- AI Job Matching
- AI Interview Questions
- Recruitment Insights
- AI Decision Support

### Admin Module

- User Management
- Recruiter Verification
- Job Moderation
- Analytics Dashboard
- Reports
- System Monitoring

------------------------------------------------------------------------

# 🛠 Tech Stack

## Backend

- Java 21
- Spring Boot 3
- Spring MVC
- Spring Security
- Spring Data JPA
- Hibernate ORM
- PostgreSQL
- JWT Authentication
- Bean Validation
- Maven
- Lombok

## Frontend

- React.js
- Vite
- JavaScript (ES6+)
- HTML5
- CSS3
- Tailwind CSS
- Axios
- React Router DOM
- React Hook Form
- Context API

## AI & Machine Learning

- Python
- FastAPI
- Large Language Models (LLMs)
- Qwen2.5-7B-Instruct (planned local LLM)
- Resume Parsing
- ATS Resume Scoring
- AI Candidate Ranking
- AI Job Matching
- AI Decision Support

> The AI engine is planned as a separate service. The Spring Boot
> backend will communicate with the AI engine, validate AI results, and
> persist final results in PostgreSQL. The AI engine will not directly
> access PostgreSQL.

## Database

- PostgreSQL

## Tools

- IntelliJ IDEA
- Visual Studio Code
- Postman
- Git
- GitHub
- pgAdmin

## Deployment

- AWS
- AWS EC2
- Docker (planned)
- AWS deployment architecture for frontend, backend, database, and AI
  services

------------------------------------------------------------------------

# 📡 REST APIs

## Authentication APIs

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| POST | `/auth/register` | Register user |
| POST | `/auth/register/recruiter` | Register recruiter |
| POST | `/auth/login` | Login |
| POST | `/auth/refresh` | Refresh access token |

## User APIs

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| GET | `/users/me` | Get current user |
| GET | `/users/{id}` | Get user by ID |
| PUT | `/users/me` | Update current user |

## Recruiter Profile APIs

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| POST | `/recruiter/profile` | Create recruiter profile |
| GET | `/recruiter/profile` | Get logged-in recruiter's profile |

## Job Management APIs

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| POST | `/jobs` | Create a job |
| GET | `/jobs` | Get logged-in recruiter's jobs |
| GET | `/jobs/{jobId}` | Get a recruiter-owned job |
| PUT | `/jobs/{jobId}` | Update a job |
| PATCH | `/jobs/{jobId}/close` | Close a job |

All Job Management endpoints are protected with:

```java
@PreAuthorize("hasRole('RECRUITER')")
```

------------------------------------------------------------------------

# 🏗 Architecture

## Current Backend Architecture

```text
React Frontend
        │
        ▼
REST APIs
        │
        ▼
Spring Boot Controllers
        │
        ▼
Service Layer
        │
        ▼
Repository Layer
        │
        ▼
Hibernate / JPA
        │
        ▼
PostgreSQL
```

## Planned AI Architecture

```text
                    FRONTEND
                       │
                       ▼
                SPRING BOOT
                       │
             ┌─────────┴─────────┐
             │                   │
          FAST AI             SLOW AI
             │                   │
             ▼                   ▼
        Match Engine        Resume Parser
        Decision Engine     Long LLM Tasks
             │                   │
             ▼                   │
        Spring Boot ◄──────── AI Callback
             │
             ▼
         PostgreSQL
```

- AI engine runs as a separate service
- Backend communicates with the AI engine
- AI engine does not directly access PostgreSQL
- Fast AI operations use synchronous communication
- Long-running AI operations use asynchronous processing
- AI responses are validated by the backend before persistence
- JSON API contracts use camelCase

------------------------------------------------------------------------

# 🔒 Security Features

- JWT Authentication
- Stateless Authentication
- BCrypt Password Encryption
- Role-Based Authorization
- Custom Authentication Filter
- Custom Authentication Entry Point
- Method-Level Security with `@PreAuthorize`
- Recruiter-only Job Management APIs
- Global Exception Handling
- Ownership-based Job Access

------------------------------------------------------------------------

# 🧪 API Testing

Completed Authentication, Recruiter Profile, and Job Management APIs
were manually tested using **Postman**.

## Authentication Testing

| Test | Endpoint | Result |
| :--- | :--- | :--- |
| Recruiter Registration | `POST /auth/register/recruiter` | ✅ Passed |
| Recruiter Login | `POST /auth/login` | ✅ Passed |

Verified:
- Recruiter account creation
- `ROLE_RECRUITER` assignment
- JWT access token
- Refresh token
- Successful authentication

## Recruiter Profile Testing

| Test | Endpoint | Result |
| :--- | :--- | :--- |
| Create Recruiter Profile | `POST /recruiter/profile` | ✅ Passed |
| Get Recruiter Profile | `GET /recruiter/profile` | ✅ Passed |

Verified:
- JWT authentication
- Current recruiter identification
- Profile creation
- Profile retrieval
- Company information persistence

## Job Management Testing

| Test | Endpoint | Result |
| :--- | :--- | :--- |
| Create Job | `POST /jobs` | ✅ Passed |
| Get My Jobs | `GET /jobs` | ✅ Passed |
| Get Job by ID | `GET /jobs/{jobId}` | ✅ Passed |
| Update Job | `PUT /jobs/{jobId}` | ✅ Passed |
| Close Job | `PATCH /jobs/{jobId}/close` | ✅ Passed |

### Job Testing Flow

```text
Recruiter Registration
        ↓
Recruiter Login
        ↓
Create Recruiter Profile
        ↓
Get Recruiter Profile
        ↓
Create Job
        ↓
Get My Jobs
        ↓
Get Job by ID
        ↓
Update Job
        ↓
Close Job
        ↓
Verify Job Status = CLOSED
```

### Issue Found During Testing

While testing `GET /jobs`, the API initially returned a misleading
`401 Unauthorized`. The actual backend exception was a Hibernate
lazy-loading error:

```text
failed to lazily initialize a collection of role:
com.vionsys.hireai.job.entity.Job.skills
could not initialize proxy - no Session
```

The issue was caused by the `skills` collection being accessed during
JSON serialization after the Hibernate session had closed.

### Fix

`JobMapper` was updated to convert the Hibernate collection into a
normal Java list:

```java
.skills(job.getSkills() != null
        ? new ArrayList<>(job.getSkills())
        : null)
```

After the fix:

```text
GET /jobs
→ 200 OK
```

### Final Job Lifecycle Verification

After closing the job:

```text
PATCH /jobs/{jobId}/close
→ 204 No Content
```

A final:

```text
GET /jobs/{jobId}
```

confirmed:

```json
{
  "status": "CLOSED"
}
```

This verified the complete Job Management lifecycle.

------------------------------------------------------------------------

# 📅 Development Roadmap

## ✅ Sprint 1 - Authentication & Authorization

- User Registration
- Recruiter Registration
- User Login
- JWT Authentication
- Access & Refresh Tokens
- Spring Security
- Role Management
- Exception Handling

## ✅ Sprint 2 - User Management

- Get Current User
- Get User by ID
- Update User Profile

## ✅ Sprint 3 - Recruiter Profile

- Create Recruiter Profile
- Get Recruiter Profile
- Company Information
- Recruiter Information
- Recruiter Ownership

## ✅ Sprint 4 - Job Management

- Job Entity
- Job Enums
- Job Request / Response DTOs
- Job Mapper
- Job Repository
- Job Service
- Job Controller
- Recruiter Authorization
- Create Job
- Get Recruiter's Jobs
- Get Job by ID
- Update Job
- Close Job
- Postman API Testing
- Hibernate Lazy Loading Issue Resolution

## 🚧 Sprint 5 - Candidate Profile

- Candidate Profile
- Skills
- Education
- Experience
- Portfolio
- LinkedIn
- GitHub

## 🚧 Sprint 6 - Application Management

- Apply for Jobs
- Application Tracking
- Application Status
- Shortlisting
- Interview Scheduling

## 🚧 Sprint 7 - Resume Management

- Resume Upload
- Resume Storage
- Resume Versioning
- Resume Processing

## 🚧 Sprint 8 - AI Resume Analysis

- Resume Parsing
- ATS Resume Score
- Candidate-Job Matching
- Resume Analysis
- AI Candidate Ranking
- AI Recommendations

## 🚧 Sprint 9 - AI Decision & Recruitment Assistance

- AI Decision Engine
- AI Interview Questions
- Recruitment Chatbot
- Recruitment Insights

## 🚧 Sprint 10 - Admin Dashboard

- User Management
- Recruiter Verification
- Job Moderation
- Analytics Dashboard
- Reports
- System Monitoring

------------------------------------------------------------------------

# 📌 Current Progress

| Module | Status |
| :--- | :--- |
| Authentication | ✅ Completed & Tested |
| Authorization | ✅ Completed & Tested |
| User Management | ✅ Completed |
| Recruiter Profile | ✅ Completed & Tested |
| Job Management | ✅ Completed & Tested |
| Candidate Module | 🚧 Upcoming |
| Application Module | ⏳ Planned |
| Resume Module | ⏳ Planned |
| AI Module | ⏳ Planned |
| Admin Module | ⏳ Planned |

------------------------------------------------------------------------

# 🚀 Getting Started

## Clone Repository

```bash
git clone https://github.com/justmemohit052002/hireai.git
cd hireai
```

## Backend

```bash
cd backend
mvn clean install
mvn spring-boot:run
```

## Frontend

```bash
cd frontend
npm install
npm run dev
```

------------------------------------------------------------------------

# 🔮 Future AI Architecture

The AI layer will be implemented as a dedicated service instead of
connecting the application directly to external AI APIs.

```text
Frontend
   │
   ▼
Spring Boot Backend
   │
   ▼
Python FastAPI AI Engine
   │
   ▼
Local LLM
   │
   ├── Resume Parser
   ├── Match Engine
   ├── Decision Engine
   └── Recruitment Assistant
   │
   ▼
Spring Boot Backend
   │
   ▼
PostgreSQL
```

The backend remains responsible for authentication, authorization,
business logic, validation, and persistence.

------------------------------------------------------------------------

# 📌 Project Status

HireAI is currently under active development.

The core **authentication, authorization, user management, recruiter
profile, and Job Management foundations are implemented and tested**.
Candidate, application, resume, and AI modules are planned for upcoming
development sprints.
