# 🚀 HireAI - AI Powered Recruitment Platform

<div align="center">

![Java](https://img.shields.io/badge/Java-21-orange?style=for-the-badge&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.x-6DB33F?style=for-the-badge&logo=springboot)
![React](https://img.shields.io/badge/React.js-19-61DAFB?style=for-the-badge&logo=react)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-18-4169E1?style=for-the-badge&logo=postgresql)
![JWT](https://img.shields.io/badge/JWT-Authentication-black?style=for-the-badge&logo=jsonwebtokens)
![License](https://img.shields.io/badge/License-MIT-blue?style=for-the-badge)

**An Enterprise AI-Powered Recruitment Platform built with Spring Boot, React.js, PostgreSQL and Large Language Models (LLMs).**

</div>

---

# 📖 Overview

HireAI is a modern AI-powered recruitment platform designed to simplify and automate the hiring process for **Candidates**, **Recruiters**, and **Administrators**.

The platform combines enterprise-grade backend architecture with an interactive React frontend and Artificial Intelligence to provide intelligent hiring solutions such as:

- 🤖 ATS Resume Score
- 📄 Resume Parsing
- 🎯 AI Candidate Ranking
- 💼 Job Recommendation
- 🧠 LLM Powered Resume Analysis
- 📊 Recruiter Dashboard
- 👤 Candidate Profile Management

The project follows an **Enterprise Software Development Lifecycle (SDLC)** using Sprint-based development.

---

# 🎯 Objectives

- Build an enterprise-level Recruitment Platform
- Learn Spring Boot Architecture
- Implement Secure JWT Authentication
- Develop scalable REST APIs
- Integrate AI-powered Resume Analysis
- Build modern React Frontend
- Follow Clean Architecture and Best Practices

---

# ✨ Features

## ✅ Authentication & Authorization

- User Registration
- User Login
- JWT Authentication
- Access Token
- Refresh Token
- BCrypt Password Encryption
- Spring Security
- Role-Based Authorization
- Custom Authentication Filter
- Global Exception Handling

---

## ✅ User Management

- View Current User Profile
- View User by ID
- Update User Profile

---

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

### Recruiter Module

- Recruiter Profile
- Company Management
- Recruiter Verification

### Job Module

- Create Job
- Update Job
- Delete Job
- Search Jobs

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

### Admin Module

- User Management
- Recruiter Verification
- Job Moderation
- Analytics Dashboard
- Reports
- System Monitoring

---

# 🛠 Tech Stack

## Backend

- Java 21
- Spring Boot 3
- Spring Security
- Spring Data JPA
- Hibernate ORM
- PostgreSQL
- JWT Authentication
- Maven
- Lombok

---

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

---

## AI & Machine Learning

- Python
- Large Language Models (LLMs)
- Resume Parsing
- ATS Resume Scoring
- AI Candidate Ranking
- AI Job Recommendation

---

## Database

- PostgreSQL

---

## Tools

- IntelliJ IDEA
- Visual Studio Code
- Postman
- Git
- GitHub
- pgAdmin

---

# 📂 Project Structure

```text
hireai
│
├── backend
│   │
│   ├── authentication
│   ├── security
│   ├── user
│   ├── role
│   ├── candidate
│   ├── recruiter
│   ├── company
│   ├── job
│   ├── application
│   ├── ai
│   ├── exception
│   ├── configuration
│   └── utils
│
├── frontend
│   │
│   ├── assets
│   ├── components
│   ├── pages
│   ├── layouts
│   ├── hooks
│   ├── context
│   ├── services
│   ├── routes
│   └── utils
│
└── README.md
```

---

# 🗄 Database Design

### Current Tables

- Users
- Roles

### Upcoming Tables

- Candidate_Profile
- Recruiter_Profile
- Company
- Job
- Job_Application
- Resume
- Education
- Experience
- Skills
- Notification

---

# 🔐 Authentication Flow

```text
Client
   │
   ▼
Login / Register
   │
   ▼
Spring Security
   │
   ▼
Authentication Manager
   │
   ▼
JWT Generation
   │
   ▼
Access Token
   │
   ▼
Protected REST APIs
```

---

# 📡 REST APIs

## Authentication APIs

| Method | Endpoint |
|---------|----------|
| POST | `/auth/register` |
| POST | `/auth/login` |

---

## User APIs

| Method | Endpoint |
|---------|----------|
| GET | `/users/me` |
| GET | `/users/{id}` |
| PUT | `/users/me` |

---

# 🏗 Architecture

```text
React Frontend
        │
        ▼
REST APIs
        │
        ▼
Spring Boot
        │
        ▼
Service Layer
        │
        ▼
Repository Layer
        │
        ▼
PostgreSQL
```

---

# 🔒 Security Features

- JWT Authentication
- Stateless Authentication
- BCrypt Password Encryption
- Role-Based Authorization
- Custom Authentication Filter
- Custom Authentication Entry Point
- Global Exception Handling

---

# 📅 Development Roadmap

## ✅ Sprint 1

Authentication & Authorization

- User Registration
- User Login
- JWT Authentication
- Spring Security
- Role Management
- Exception Handling

---

## ✅ Sprint 2

User Management

- Get Current User
- Get User by ID
- Update User Profile

---

## 🚧 Sprint 3

Candidate Profile

---

## 🚧 Sprint 4

Recruiter Profile

---

## 🚧 Sprint 5

Company Management

---

## 🚧 Sprint 6

Job Management

---

## 🚧 Sprint 7

Application Management

---

## 🚧 Sprint 8

Resume Management

---

## 🚧 Sprint 9

AI Resume Analysis

---

## 🚧 Sprint 10

Admin Dashboard

---

# 🚀 Getting Started

## Clone Repository

```bash
git clone https://github.com/your-username/hireai.git
```

---

## Backend

```bash
cd backend
mvn clean install
mvn spring-boot:run
```

---

## Frontend

```bash
cd frontend
npm install
npm run dev
```

---

# 📌 Current Progress

| Module | Status |
|---------|--------|
| Authentication | ✅ Completed |
| Authorization | ✅ Completed |
| User Management | ✅ Completed |
| Candidate Module | 🚧 In Progress |
| Recruiter Module | ⏳ Planned |
| Company Module | ⏳ Planned |
| Job Module | ⏳ Planned |
| AI Module | ⏳ Planned |

---
