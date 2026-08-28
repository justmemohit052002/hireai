# 🚀 HireAI - Enterprise AI-Powered Recruitment Platform

<div align="center">

![Java](https://img.shields.io/badge/Java-21-orange?style=for-the-badge&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5.5-6DB33F?style=for-the-badge&logo=springboot)
![Python](https://img.shields.io/badge/Python-3.11+-3776AB?style=for-the-badge&logo=python)
![FastAPI](https://img.shields.io/badge/FastAPI-0.110+-009688?style=for-the-badge&logo=fastapi)
![React](https://img.shields.io/badge/React.js-19-61DAFB?style=for-the-badge&logo=react)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-18-4169E1?style=for-the-badge&logo=postgresql)
![JWT](https://img.shields.io/badge/JWT-Stateless_Auth-black?style=for-the-badge&logo=jsonwebtokens)
![Swagger](https://img.shields.io/badge/OpenAPI_3.0-Swagger_UI-85EA2D?style=for-the-badge&logo=swagger)

**An Enterprise-Grade, Multi-Service AI Recruitment Platform built with Spring Boot 3.5, FastAPI, React 19, PostgreSQL 18, and Large Language Models (LLMs).**

[📖 API Documentation](http://localhost:8080/swagger-ui/index.html) • [🧪 Testing Manual](docs/HireAI_Backend_API_Testing_and_Debugging_Manual.md) • [🧠 AI Specification](docs/HireAI_AI_Engine_Architecture_and_Integration_Specification.md)

</div>

---

## 📑 Table of Contents

- [📖 Overview](#-overview)
- [🎯 Core Value Proposition & Capabilities](#-core-value-proposition--capabilities)
- [🏗 System Architecture](#-system-architecture)
- [✨ Implemented Feature Modules](#-implemented-feature-modules)
  - [1. Authentication, Authorization & Password Recovery](#1-authentication-authorization--password-recovery)
  - [2. User Management](#2-user-management)
  - [3. Recruiter Company Profile & Branding](#3-recruiter-company-profile--branding)
  - [4. Candidate Profile Management & Directory](#4-candidate-profile-management--directory)
  - [5. Job Postings & Lifecycle Management](#5-job-postings--lifecycle-management)
  - [6. Job Applications & Live ATS Scoring Pipeline](#6-job-applications--live-ats-scoring-pipeline)
  - [7. Resume Storage & Apache Tika Document Extraction](#7-resume-storage--apache-tika-document-extraction)
  - [8. Recruiter AI Suite](#8-recruiter-ai-suite)
  - [9. Candidate AI Suite & Interview Assessment](#9-candidate-ai-suite--interview-assessment)
  - [10. Automated HTML Email Notification Subsystem](#10-automated-html-email-notification-subsystem)
- [🧠 Standalone AI Engine (`hireai-ai-engine`)](#-standalone-ai-engine-hireai-ai-engine)
- [🛠 Tech Stack](#-tech-stack)
- [📡 Complete REST API Inventory](#-complete-rest-api-inventory)
- [🧪 Testing & Quality Assurance](#-testing--quality-assurance)
- [📅 Development Roadmap & Sprint Progress](#-development-roadmap--sprint-progress)
- [🚀 Quick-Start & Installation Guide](#-quick-start--installation-guide)
- [📂 Project Directory Structure](#-project-directory-structure)

---

# 📖 Overview

**HireAI** is a modern, full-lifecycle intelligent recruitment ecosystem engineered to automate candidate screening, candidate-job matching, technical pre-screening, and hiring workflow management for **Candidates**, **Recruiters**, and **Administrators**.

By decoupling heavy LLM inferences into an independent, high-performance Python FastAPI microservice while keeping transactional persistence, business logic, role-based security, and document parsing in an enterprise Spring Boot backend, HireAI delivers sub-second response times for recruiters and intelligent automated evaluations for candidates.

---

# 🎯 Core Value Proposition & Capabilities

- 🤖 **Real-Time ATS Match Scoring:** Calculates skill overlap and semantic match percentages immediately when candidates submit job applications.
- 📄 **Zero-Loss Resume Parsing:** Dual-layer resume ingestion using **Apache Tika 2.9** for PDF/DOCX text extraction and asynchronous LLM structured JSON parsing.
- 🎯 **Recruiter Candidate Ranking:** Recruiter dashboard automatically lists all applicants ranked from highest to lowest ATS match score.
- 📝 **AI Job Description Generator:** Instant generation of comprehensive job descriptions, required vs nice-to-have skills, and technical screening questions from role parameters.
- 💬 **Interactive AI Pre-Screening Chatbot:** Multi-turn conversational interview assistant that screens candidate technical backgrounds before recruiter review.
- 🎯 **Automated Interview Evaluation:** Dynamically generates technical interview questions per job posting and grades candidate submitted answers with detailed scoring.
- ⚖️ **Multi-Factor Decision Engine:** Computes a composite hiring decision score combining resume score, interview score, and chatbot behavioral signals.
- 📧 **Automated Email Pipeline:** HTML formatted transactional emails triggered on registration, login, application submission, recruiter alerts, and pipeline stage changes.

---

# 🏗 System Architecture

```text
                                  ┌─────────────────────────────┐
                                  │      React 19 Frontend      │
                                  │  (Vite + Glassmorphic UI)   │
                                  └──────────────┬──────────────┘
                                                 │ HTTP / JSON & Multipart
                                                 ▼
               ┌─────────────────────────────────────────────────────────────────┐
               │              Spring Boot 3.5.5 Backend (Port 8080)              │
               │  ┌───────────────────────────────────────────────────────────┐  │
               │  │ Spring Security • Stateless JJWT • RBAC (Roles & Filters) │  │
               │  └───────────────────────────────────────────────────────────┘  │
               │  ┌───────────────────────────────────────────────────────────┐  │
               │  │ REST Controllers • Services • Repositories • JavaMail     │  │
               │  └───────────────────────────────────────────────────────────┘  │
               │  ┌───────────────────────────────────────────────────────────┐  │
               │  │ Apache Tika 2.9 Parser • Profile Photo Storage Service    │  │
               │  └───────────────────────────────────────────────────────────┘  │
               └──────────────┬───────────────────────────────┬──────────────────┘
                              │                               │
            JDBC / JPA        │                               │ HTTP REST (camelCase)
                              ▼                               ▼
               ┌──────────────────────────────┐ ┌────────────────────────────────┐
               │    PostgreSQL 18 Database    │ │   FastAPI AI Engine (Port 8000)│
               │                              │ │ ┌────────────────────────────┐ │
               │ • Users & Roles              │ │ │ Ollama (Llama 3.1:8b)      │ │
               │ • Recruiter Profiles         │ │ │ Groq / Gemini / OpenAI     │ │
               │ • Candidate Profiles         │ │ ├────────────────────────────┤ │
               │ • Job Postings & Skills      │ │ │ Match Engine (Embeddings)  │ │
               │ • Applications & ATS Scores  │ │ │ Decision Engine (Weighted) │ │
               │ • Resumes & Binary Metadata  │ │ │ Async Resume Parser Queue  │ │
               │ • Chat & Interview Sessions  │ │ └────────────────────────────┘ │
               └──────────────────────────────┘ └────────────────────────────────┘
```

### Architectural Principles:
1. **Stateless AI Processing:** The FastAPI AI microservice does not connect directly to PostgreSQL. The Spring Boot backend acts as the single source of truth, orchestrating AI calls and persisting validated outputs.
2. **Text Extraction Boundary:** Spring Boot handles multipart binary uploads and extracts raw text via Apache Tika before sending payload strings to the AI microservice.
3. **Synchronous vs Asynchronous Workflows:**
   - **Synchronous:** JD Generator, Match Engine, Decision Engine, Chatbot, and Interview AI evaluate instantly in real time.
   - **Asynchronous Polling:** Heavy LLM resume parsing runs in a background thread with an immediate `jobId` return and status polling (`GET /api/v1/resumes/status/{jobId}`).

---

# ✨ Implemented Feature Modules

## 1. Authentication, Authorization & Password Recovery
- **Dual Role Registration:** Dedicated registration flows for `ROLE_CANDIDATE` and `ROLE_RECRUITER`.
- **JWT Stateless Security:** Access tokens and refresh tokens signed with HS512 via JJWT 0.12.7.
- **BCrypt Password Encryption:** Industrial-strength 10-round salted password hashing.
- **Self-Service Password Reset Workflow:**
  - `POST /auth/forgot-password`: Generates secure 15-minute time-limited tokens and emails HTML reset links.
  - `GET /auth/verify-reset-token`: Validates token status and expiration.
  - `POST /auth/reset-password`: Resets password and atomically invalidates used tokens.
- **Security Login Alerts:** Sends automated security notifications to user emails upon login.

## 2. User Management
- `GET /users/me`: Fetch authenticated user profile and roles.
- `GET /users/{userId}`: Retrieve user identity by UUID.
- `PUT /users/me`: Update first name, last name, and contact phone number.

## 3. Recruiter Company Profile & Branding
- Complete company profile management: Company Name, Designation, Website, Email, Phone, Company Size, Industry, Location, and Description.
- **Recruiter Avatar & Logo Storage:** Multi-format image upload (`/photo`) with MIME detection and binary streaming.

## 4. Candidate Profile Management & Directory
- **Candidate Profile:** Experience (years), Current CTC, Expected CTC, Notice Period (days), Preferred Location, Skills list, Education, Experience records, GitHub, LinkedIn, and Portfolio links.
- **Candidate Profile Photo:** Multipart upload, view, stream, and delete endpoints.
- **Recruiter Candidate Directory Search:** Full multi-field filtering (`firstName`, `lastName`, `email`, `phone`, `location`, `candidateStatus`, `experience`, `skill`) with server-side pagination and sorting.

## 5. Job Postings & Lifecycle Management
- **Job Creation & Ownership:** Recruiters create and manage jobs with Title, Description, Required Skills, Salary Range, Experience Level, Employment Type, Location, Remote Support, and Application Deadline.
- **Public & Authenticated Feeds:** Candidate open job listings (`GET /jobs/open`) and recruiter-specific job management (`GET /jobs`).
- **Job Lifecycle Controls:** Update job postings and close postings (`PATCH /jobs/{jobId}/close`) to prevent further applications.

## 6. Job Applications & Live ATS Scoring Pipeline
- **One-Click Application with Instant Resume Attachment:** Candidates apply using either stored profile credentials or by uploading a new resume (`POST /jobs/{jobId}/apply`).
- **Automated ATS Score Computation:** Instantly compares job requirements against candidate skill sets to compute an ATS match score (0–100%).
- **Applicant Ranking Pipeline:** Recruiters view applicants automatically sorted from highest to lowest ATS match score (`GET /jobs/{jobId}/applications`).
- **Recruiter Stage Tracking:** Transition candidates across pipeline stages (`APPLIED` ➔ `SCREENING` ➔ `SHORTLISTED` ➔ `INTERVIEW_SCHEDULED` ➔ `OFFERED` ➔ `REJECTED`) with recruiter notes.
- **Candidate Live Application Dashboard:** Candidates view active application statuses, stage histories, and feedback in real time (`GET /candidate/applications`).

## 7. Resume Storage & Apache Tika Document Extraction
- **Resume Upload & Parsing:** Ingests PDF and DOCX files, extracts raw text with Apache Tika 2.9, stores files safely in local uploads, and queues background AI parsing.
- **Candidate Self-Service:** Upload, retrieve metadata, download original file, or delete resume.
- **Recruiter/Admin Access:** Direct download and parsing review for any candidate's resume.

## 8. Recruiter AI Suite
- **AI Job Description Generator (`/recruiter/ai/jd/generate`):** Produces role overviews, structured responsibilities, must-have/nice-to-have skill breakdowns, and interview questions.
- **On-Demand ATS Semantic Match (`/recruiter/ai/match/{jobId}/{candidateId}`):** Computes deep vector semantic match scores between candidate skills and job specifications.
- **AI Decision Engine (`/recruiter/ai/applications/{applicationId}/decision`):** Computes a composite weighted hiring recommendation (`HIRE`, `SHORTLIST`, `REJECT`, `NEEDS_REVIEW`) combining ATS match, interview performance, and pre-screening signals.

## 9. Candidate AI Suite & Interview Assessment
- **Pre-Screening Chatbot (`/candidate/chat/message`, `/candidate/chat/history`):** Stateful multi-turn conversational AI evaluating candidate background, qualifications, and domain knowledge.
- **AI Technical Interview Questions (`/candidate/applications/{applicationId}/interview`):** Generates targeted technical interview questions tailored to the job's required skills.
- **Automated Answer Evaluation (`/candidate/applications/{applicationId}/interview/submit`):** Grades candidate answers out of 100 with comprehensive constructive feedback.

## 10. Automated HTML Email Notification Subsystem
- ✉️ **Candidate Application Confirmation:** Notifies candidates that their application has been received.
- ✉️ **Recruiter New Applicant Alert:** Alerts recruiters immediately when a new candidate applies to their job.
- ✉️ **Pipeline Stage Update Notification:** Sends email updates to candidates when moved across recruitment stages.
- ✉️ **Welcome Candidate & Recruiter Emails:** Onboarding emails sent immediately upon registration.
- ✉️ **Security & Password Recovery Emails:** Login alerts and time-limited password reset links.

---

# 🧠 Standalone AI Engine (`hireai-ai-engine`)

The AI Engine is a dedicated Python FastAPI microservice providing 6 modular AI capabilities:

```text
hireai-ai-engine/
├── app/
│   ├── jd_generator.py      # Module 1: Job Description Generator
│   ├── resume_parser.py     # Module 2: Async LLM Resume Parser
│   ├── match_engine.py      # Module 3: Vector & Skill Match Engine
│   ├── chatbot.py           # Module 4: Pre-Screening Conversational Agent
│   ├── interview_ai.py      # Module 5: Technical Question Generator & Grader
│   ├── decision_engine.py   # Module 6: Weighted Decision Engine
│   ├── llm_client.py        # Multi-provider LLM Adapter (Ollama, Gemini, Groq, OpenAI)
│   ├── jobs.py              # Background Job Queue for Async Parsing
│   └── models.py            # Pydantic Schemas & DTO Contracts
├── streamlit_app.py         # Interactive Visual AI Testing Playground
├── main.py                  # FastAPI Application Entry Point
└── requirements.txt         # Python Dependencies
```

### Supported LLM Providers:
- **Local LLM (Default):** Ollama running `llama3.1:8b` or `qwen2.5:7b` (zero API cost, privacy-first)
- **Cloud Providers:** Google Gemini API (`gemini-1.5-flash`), Groq (`llama-3.1-70b-versatile`), or OpenAI (`gpt-4o-mini`)

---

# 🛠 Tech Stack

## Backend
| Technology | Version | Purpose |
| :--- | :--- | :--- |
| **Java** | 21 / 25 | Core Backend Runtime |
| **Spring Boot** | 3.5.5 | Backend Application Framework |
| **Spring Security** | 6.x | Authentication & Role Authorization |
| **PostgreSQL** | 18 | Relational Database & Persistence |
| **Hibernate / Spring Data JPA** | Latest | ORM & Entity Mappings |
| **Apache Tika** | 2.9.2 | PDF / DOCX Resume Text Extraction |
| **JJWT** | 0.12.7 | Stateless JSON Web Tokens |
| **SpringDoc OpenAPI** | 2.8.5 | Swagger UI & OpenAPI Specification |
| **JavaMailSender** | Spring Starter | Asynchronous HTML Email Dispatch |
| **MapStruct & Lombok** | Latest | DTO Mapping & Boilerplate Reduction |

## AI Microservice
| Technology | Version | Purpose |
| :--- | :--- | :--- |
| **Python** | 3.11+ | AI Microservice Runtime |
| **FastAPI & Uvicorn** | 0.110+ | High-Performance Asynchronous API Server |
| **Sentence Transformers** | `all-MiniLM-L6-v2` | Dense Vector Semantic Embeddings |
| **Ollama / Llama 3.1 / Qwen 2.5** | Latest | Local Large Language Model Inferences |
| **Pydantic** | v2 | Request/Response Validation & Serialization |
| **Streamlit** | Latest | Visual AI Testing & Prompt Inspection Studio |

## Frontend
| Technology | Version | Purpose |
| :--- | :--- | :--- |
| **React** | 19 | Frontend Single Page Application |
| **Vite** | Latest | Fast Modern Bundler & Development Server |
| **Axios** | Latest | HTTP Client with Interceptors |
| **CSS3** | Vanilla CSS | Custom Glassmorphic Dark-Mode Design System |
| **React Testing Hub** | Built-in | Interactive API Debugger & E2E Flow Orchestrator |

---

# 📡 Complete REST API Inventory

## 🔐 1. Authentication & Security (`/auth`, `/test`)

| Method | Endpoint | Auth Required | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/auth/register/candidate` | Public | Register new candidate account |
| `POST` | `/auth/register/recruiter` | Public | Register new recruiter account |
| `POST` | `/auth/login` | Public | Login and receive JWT access + refresh tokens |
| `POST` | `/auth/forgot-password` | Public | Generate 15-minute password reset token and send email |
| `GET` | `/auth/verify-reset-token` | Public | Verify reset token validity |
| `POST` | `/auth/reset-password` | Public | Reset password using valid token |
| `GET` | `/test/authenticated` | Any User | Validate active JWT session |
| `GET` | `/test/recruiter` | `ROLE_RECRUITER` | Recruiter-only access validation |
| `GET` | `/test/candidate` | `ROLE_CANDIDATE` | Candidate-only access validation |

## 👤 2. User Management (`/users`)

| Method | Endpoint | Auth Required | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/users/me` | Authenticated | Get current authenticated user profile |
| `GET` | `/users/{userId}` | Authenticated | Get user profile by UUID |
| `PUT` | `/users/me` | Authenticated | Update current user's personal details |

## 🏢 3. Recruiter Company Profile (`/recruiter/profile`)

| Method | Endpoint | Auth Required | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/recruiter/profile` | `ROLE_RECRUITER` | Create recruiter company profile |
| `GET` | `/recruiter/profile` | `ROLE_RECRUITER` | Get logged-in recruiter's profile |
| `GET` | `/recruiter/profile/{userId}` | Authenticated | Get recruiter profile by User UUID |
| `PUT` | `/recruiter/profile` | `ROLE_RECRUITER` | Update recruiter company profile |
| `POST` | `/recruiter/profile/photo` | `ROLE_RECRUITER` | Upload recruiter profile/company logo |
| `GET` | `/recruiter/profile/photo` | `ROLE_RECRUITER` | Stream current recruiter profile photo |
| `GET` | `/recruiter/profile/{userId}/photo`| Authenticated | Stream recruiter profile photo by user ID |
| `DELETE`| `/recruiter/profile/photo` | `ROLE_RECRUITER` | Delete recruiter profile photo |

## 👨‍💼 4. Candidate Profile & Directory (`/candidate/profile`, `/candidates`)

| Method | Endpoint | Auth Required | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/candidate/profile` | `ROLE_CANDIDATE` | Create authenticated candidate profile |
| `GET` | `/candidate/profile` | `ROLE_CANDIDATE` | Get authenticated candidate profile |
| `PUT` | `/candidate/profile` | `ROLE_CANDIDATE` | Update candidate profile details & skills |
| `POST` | `/candidate/profile/photo` | `ROLE_CANDIDATE` | Upload candidate avatar image |
| `GET` | `/candidate/profile/photo` | `ROLE_CANDIDATE` | Stream candidate avatar image |
| `GET` | `/candidates/{id}/profile/photo` | Authenticated | Stream candidate avatar by candidate ID |
| `DELETE`| `/candidate/profile/photo` | `ROLE_CANDIDATE` | Delete candidate avatar image |
| `POST` | `/candidates` | `ROLE_RECRUITER`, `ROLE_ADMIN` | Create candidate entry |
| `GET` | `/candidates/{candidateId}` | Authenticated | Get candidate details by ID |
| `GET` | `/candidates` | `ROLE_RECRUITER`, `ROLE_ADMIN` | Search & paginate candidates with filter params |
| `PUT` | `/candidates/{candidateId}` | `ROLE_RECRUITER`, `ROLE_ADMIN` | Update candidate entry |
| `DELETE`| `/candidates/{candidateId}` | `ROLE_RECRUITER`, `ROLE_ADMIN` | Delete candidate entry |

## 💼 5. Job Management (`/jobs`)

| Method | Endpoint | Auth Required | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/jobs` | `ROLE_RECRUITER` | Create a new job posting |
| `GET` | `/jobs` | `ROLE_RECRUITER` | Get all jobs owned by logged-in recruiter |
| `GET` | `/jobs/open` | `ROLE_CANDIDATE`, `ROLE_RECRUITER` | Browse active open job listings |
| `GET` | `/jobs/{jobId}` | `ROLE_RECRUITER` | Get recruiter-owned job by ID |
| `PUT` | `/jobs/{jobId}` | `ROLE_RECRUITER` | Update job posting |
| `PATCH` | `/jobs/{jobId}/close` | `ROLE_RECRUITER` | Close job posting |

## 🎯 6. Job Applications & ATS Pipeline (`/jobs/{jobId}/apply`, `/applications`)

| Method | Endpoint | Auth Required | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/jobs/{jobId}/apply` | `ROLE_CANDIDATE` | Apply with optional cover note & resume upload (calculates ATS score) |
| `GET` | `/candidate/applications` | `ROLE_CANDIDATE` | Get all submitted applications and current stages |
| `GET` | `/jobs/{jobId}/applications` | `ROLE_RECRUITER` | Get applicants for job ranked by ATS score (highest first) |
| `PATCH` | `/applications/{id}/status` | `ROLE_RECRUITER` | Update recruitment stage & send email alert |
| `GET` | `/applications/{id}` | Authenticated | Get application details & ATS match breakdown |
| `GET` | `/applications/{id}/resume/download`| Authenticated | Download attached applicant resume |

## 📄 7. Resume Management (`/candidate/resume`, `/candidates/{id}/resume`)

| Method | Endpoint | Auth Required | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/candidate/resume/upload` | `ROLE_CANDIDATE` | Upload resume, parse with Tika & trigger AI extraction |
| `GET` | `/candidate/resume` | `ROLE_CANDIDATE` | Get parsed resume metadata |
| `GET` | `/candidate/resume/download` | `ROLE_CANDIDATE` | Download candidate's own resume binary |
| `DELETE`| `/candidate/resume` | `ROLE_CANDIDATE` | Delete candidate's resume |
| `POST` | `/candidates/{id}/resume` | `ROLE_RECRUITER`, `ROLE_ADMIN` | Upload resume for candidate |
| `GET` | `/candidates/{id}/resume` | `ROLE_RECRUITER`, `ROLE_ADMIN` | Get candidate's parsed resume metadata |
| `GET` | `/candidates/{id}/resume/status` | `ROLE_RECRUITER`, `ROLE_ADMIN` | Check resume async AI parsing status |
| `GET` | `/candidates/{id}/resume/download` | `ROLE_RECRUITER`, `ROLE_ADMIN` | Download candidate resume binary |
| `DELETE`| `/candidates/{id}/resume` | `ROLE_RECRUITER`, `ROLE_ADMIN` | Delete candidate resume |

## 🤖 8. AI Recruiter Services (`/recruiter/ai`)

| Method | Endpoint | Auth Required | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/recruiter/ai/jd/generate` | `ROLE_RECRUITER`, `ROLE_ADMIN` | AI Job Description generation |
| `POST` | `/recruiter/ai/match/{jobId}/{candidateId}` | `ROLE_RECRUITER`, `ROLE_ADMIN` | On-demand ATS vector match calculation |
| `POST` | `/recruiter/ai/applications/{id}/decision` | `ROLE_RECRUITER`, `ROLE_ADMIN` | AI Decision Engine composite score & decision |

## 💬 9. Candidate AI & Interview Assessment (`/candidate`)

| Method | Endpoint | Auth Required | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/candidate/chat/message` | `ROLE_CANDIDATE` | Send message to AI Pre-Screening Chatbot |
| `GET` | `/candidate/chat/history` | `ROLE_CANDIDATE` | Fetch chatbot conversation history |
| `POST` | `/candidate/chat/reset` | `ROLE_CANDIDATE` | Reset chat session |
| `GET` | `/candidate/applications/{id}/interview` | `ROLE_CANDIDATE` | Generate/fetch AI technical interview questions |
| `POST` | `/candidate/applications/{id}/interview/submit` | `ROLE_CANDIDATE` | Submit interview answers for AI evaluation |

---

# 🧪 Testing & Quality Assurance

The HireAI platform underwent rigorous automated integration testing using **Postman**, **Newman CLI**, and our built-in **React Testing Hub**.

### 📊 Automated Test Execution Summary

| Test Suite Module | Total Tests | Passed | Failed | Success Rate |
| :--- | :---: | :---: | :---: | :---: |
| **1. Authentication & Security** | 8 | 8 | 0 | 100.0% |
| **2. Password Reset Workflow** | 4 | 4 | 0 | 100.0% |
| **3. User Management** | 3 | 3 | 0 | 100.0% |
| **4. Recruiter Profile & Photo Upload** | 6 | 6 | 0 | 100.0% |
| **5. Candidate Profile & Photo Upload** | 6 | 6 | 0 | 100.0% |
| **6. Candidate Search & Pagination** | 4 | 4 | 0 | 100.0% |
| **7. Job Postings & Lifecycle** | 6 | 6 | 0 | 100.0% |
| **8. Applications & ATS Scoring Pipeline** | 5 | 5 | 0 | 100.0% |
| **9. AI Microservice & Decision Engine** | 4 | 4 | 0 | 100.0% |
| **TOTAL** | **46** | **46** | **0** | **100.0%** |

Detailed testing documentation, curl commands, and root-cause analysis (RCA) records are available in:
- 📄 [HireAI Backend API Testing and Debugging Manual](docs/HireAI_Backend_API_Testing_and_Debugging_Manual.md)
- 📄 [HireAI AI Engine Architecture & Specification](docs/HireAI_AI_Engine_Architecture_and_Integration_Specification.md)

---

# 📅 Development Roadmap & Sprint Progress

```
Sprint 1: Authentication & Authorization             [████████████████████] 100% (Completed)
Sprint 2: User Self-Service Management               [████████████████████] 100% (Completed)
Sprint 3: Recruiter Profile & Company Branding       [████████████████████] 100% (Completed)
Sprint 4: Job Postings & Lifecycle Management        [████████████████████] 100% (Completed)
Sprint 5: Candidate Profile & Directory Search       [████████████████████] 100% (Completed)
Sprint 6: Job Application & Live ATS Match Engine    [████████████████████] 100% (Completed)
Sprint 7: Resume Storage & Apache Tika Extraction    [████████████████████] 100% (Completed)
Sprint 8: FastAPI AI Engine & LLM Microservice       [████████████████████] 100% (Completed)
Sprint 9: AI Decision Engine & Chatbot Screening     [████████████████████] 100% (Completed)
Sprint 10: Automated Email Notification Subsystem    [████████████████████] 100% (Completed)
Sprint 11: Production React Frontend Dashboards      [██████████░░░░░░░░░░]  50% (In Progress)
Sprint 12: AWS Cloud Deployment (EC2, RDS, Docker)   [░░░░░░░░░░░░░░░░░░░░]   0% (Planned)
```

---

# 🚀 Quick-Start & Installation Guide

### Prerequisites
- **Java JDK 21+**
- **Maven 3.9+**
- **Python 3.11+**
- **Node.js 20+ & npm**
- **PostgreSQL 16+** (Running on port `5432` with database `hireai_db`)
- **Ollama** (Optional for local LLM inference: `ollama run llama3.1:8b`)

---

### 1. Database Setup
Create PostgreSQL database:
```sql
CREATE DATABASE hireai_db;
```

---

### 2. Start the AI Microservice (FastAPI)
```bash
cd hireai-ai-engine/hireai-ai-engine
python -m venv venv

# Windows
.\venv\Scripts\activate

# Linux / macOS
# source venv/bin/activate

pip install -r requirements.txt
uvicorn main:app --host 0.0.0.0 --port 8000 --reload
```
- **API Docs:** `http://localhost:8000/docs`
- **Streamlit Playground:** `streamlit run streamlit_app.py`

---

### 3. Start the Backend Service (Spring Boot)
Configure your database & email credentials in `hireai-backend/src/main/resources/application.properties`, then start the service:
```bash
cd hireai-backend
mvn clean install -DskipTests
mvn spring-boot:run
```
- **Backend Host:** `http://localhost:8080`
- **Swagger UI:** `http://localhost:8080/swagger-ui/index.html`
- **OpenAPI JSON Spec:** `http://localhost:8080/v3/api-docs`

---

### 4. Start the Frontend Application (React)
```bash
cd hireai-frontend
npm install
npm run dev
```
- **Frontend App:** `http://localhost:5173`

---

# 📂 Project Directory Structure

```text
Vionsys HireAI/
├── docs/                                                 # Comprehensive Specifications & Manuals
│   ├── HireAI_Backend_API_Testing_and_Debugging_Manual.md
│   ├── HireAI_Backend_API_Testing_and_Debugging_Manual.pdf
│   ├── HireAI_AI_Engine_Architecture_and_Integration_Specification.md
│   ├── HireAI_AI_Engine_Architecture_and_Integration_Specification.pdf
│   └── screenshots/                                      # Test run & DB verification screenshots
├── hireai-backend/                                       # Spring Boot Enterprise Microservice
│   ├── src/main/java/com/vionsys/hireai/
│   │   ├── ai/                                           # AI Client & Decision Services
│   │   ├── application/                                  # Job Application & ATS Scoring
│   │   ├── auth/                                         # Authentication & Password Recovery
│   │   ├── candidate/                                    # Candidate Profiles & Resumes
│   │   ├── common/                                       # Unified API Response & Utilities
│   │   ├── config/                                       # Security & Web Configuration
│   │   ├── email/                                        # Automated HTML Email Dispatcher
│   │   ├── exception/                                    # Global Exception Handling
│   │   ├── job/                                          # Job Postings & Status Management
│   │   ├── recruiter/                                    # Recruiter Profile & Company Info
│   │   ├── role/                                         # Role Definitions & Enum
│   │   ├── security/                                     # JWT Filters & Custom UserDetails
│   │   └── user/                                         # User Self-Service APIs
│   └── pom.xml
├── hireai-ai-engine/                                     # Standalone Python AI Microservice
│   └── hireai-ai-engine/
│       ├── app/                                          # JD, Match, Resume, Chatbot, Interview AI
│       ├── main.py                                       # FastAPI Entry Point
│       ├── streamlit_app.py                              # Visual AI Testing Studio
│       └── requirements.txt
├── hireai-frontend/                                      # React 19 Frontend
│   ├── src/
│   │   ├── testing/                                      # Interactive API Testing & Debugging Hub
│   │   ├── App.jsx / main.jsx
│   │   └── styles.css
│   └── package.json
└── README.md
```

---

<div align="center">

**HireAI** is designed and maintained by **Vionsys IT Solutions**.  
Licensed under the [MIT License](LICENSE).

</div>
