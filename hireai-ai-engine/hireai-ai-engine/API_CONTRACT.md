# HireAI — AI Engine API Contract (v1)

**Purpose:** This is the agreed interface between the AI Engine (Aishwarya)
and the Frontend/Backend team. Frontend and Backend can build against these
exact shapes starting today, without waiting for the real AI modules to be
finished. When the AI Engine is ready, only the data source changes — these
shapes should not.

**Status:** Draft — finalize in today's team session, then treat this file
as the source of truth. Any change to a field name or type must be agreed
by both sides before either team changes their code.

---

## Conventions used across every endpoint

- All requests/responses are JSON. Base path: `/api/v1/`
- Field names: `snake_case`
- IDs: strings (e.g. `"cand_1029"`, `"job_44"`) - not raw database integers, so the ID format can change later without breaking callers
- Missing/unknown values: `null` - never an empty string standing in for "no data", and never omit a key entirely
- Timestamps: ISO 8601 strings, e.g. `"2026-08-08T14:30:00Z"`
- **Authentication**: All endpoints under `/api/v1/*` (jd, resumes, match, chatbot, interview, decision, admin) require the shared internal secret header:
  ```http
  X-Internal-Key: <INTERNAL_API_KEY>
  ```
  Requests missing this header or providing an invalid key receive HTTP 401 Unauthorized (`{"error": true, "errorCode": "HTTP_401", "message": "Invalid or missing internal API key"}`).
- **Public Endpoints**: `/health` (service health and Ollama reachability) and `/` (root service metadata) remain public and do not require `X-Internal-Key`.
- Every error response uses the same unified shape (see bottom of this doc)

---

## 1. JD Generator

**`POST /api/v1/jd/generate`**

Request:
```json
{
  "job_title": "Backend Engineer",
  "required_skills": ["Node.js", "PostgreSQL", "REST APIs"],
  "experience_level": "3-5 years"
}
```

Response:
```json
{
  "job_id": "job_118",
  "description": "Own service design for our order platform...",
  "responsibilities": [
    "Design and maintain REST APIs for the order management system",
    "Collaborate with frontend team on API contracts"
  ],
  "must_have_skills": ["Node.js", "PostgreSQL", "REST APIs"],
  "nice_to_have_skills": ["Docker", "Redis"],
  "interview_questions": [
    "Walk me through how you'd design a rate-limited API endpoint."
  ]
}
```

---

## 2. Resume Parser

**`POST /api/v1/resumes/parse`**

Request — backend has already extracted raw text from the uploaded PDF/DOCX
and sends it here (the AI engine does not handle file uploads directly):
```json
{
  "candidate_id": "cand_1029",
  "resume_text": "Rohan Mehta\nBackend Developer\n..."
}
```

Response:
```json
{
  "candidate_id": "cand_1029",
  "skills": ["Node.js", "Express", "PostgreSQL", "Docker"],
  "years_experience": 4,
  "education": [
    {"degree": "B.Tech Computer Science", "institution": "Pune Institute of Technology", "year": "2020"}
  ],
  "projects": [
    {"name": "Order Tracking Dashboard", "description": "Internal dashboard built with React and Node.js"}
  ],
  "domain": "e-commerce",
  "current_role": "Backend Developer",
  "parse_status": "success"
}
```

`parse_status` is always one of: `"success"`, `"partial"` (some fields
couldn't be extracted), or `"failed"` (send to manual review). Frontend
should branch on this field, not assume every response is clean.

---

## 3. Match Engine

**`POST /api/v1/match/score`**

Request (camelCase JSON):
```json
{
  "resumeSkills": ["Java (Core Java, Java 8+)", "Spring Boot", "AWS (EC2)", "MySQL", "Git"],
  "jobSkills": [
    "Java programming language",
    "Cloud computing platforms (e.g. AWS)",
    "Database management systems (e.g. MySQL)",
    "MongoDB",
    "Version control systems (e.g. Git)"
  ],
  "niceToHaveSkills": ["Docker", "Kubernetes"],
  "resumeText": "Optional raw text of the resume for token evidence extraction...",
  "candidateExperienceYears": 3.5,
  "requiredExperienceYears": 3.0
}
```

Response (camelCase JSON):
```json
{
  "matchScore": 88,
  "autoAction": "shortlist",
  "matchedSkills": [
    "Java programming language",
    "Cloud computing platforms (e.g. AWS)",
    "Database management systems (e.g. MySQL)",
    "Version control systems (e.g. Git)"
  ],
  "missingSkills": [
    "MongoDB"
  ],
  "bonusSkills": [
    "Spring Boot"
  ],
  "matchDetails": [
    {
      "requiredSkill": "Cloud computing platforms (e.g. AWS)",
      "status": "matched",
      "matchedWith": "AWS (EC2)",
      "matchType": "example",
      "confidence": 1.0
    }
  ],
  "skillCoveragePercent": 80.0,
  "experienceMatchPercent": 100.0,
  "scoreBreakdown": {
    "skillScore": 85,
    "experienceScore": 100
  },
  "rationale": "Matched 4 of 5 required skills (80.0% coverage). Missing: MongoDB.",
  "engineVersion": "match-v2",
  "semanticModelAvailable": true
}
```

`autoAction` is one of:
- `"shortlist"` (score >= 70, aligned with backend `ats.shortlist-threshold=70`)
- `"reject"` (score < 40)
- `"review"` (score 40 to 69, requiring recruiter review)

---

## 4. AI Chatbot

**`POST /api/v1/chatbot/message`**

This one is a running conversation — backend stores history and resends it
each turn, since the AI engine has no memory between calls.

Request:
```json
{
  "candidate_id": "cand_1029",
  "conversation_history": [
    {"role": "bot", "text": "Hi! What's your current notice period?"},
    {"role": "candidate", "text": "I can join immediately."}
  ],
  "new_message": "I can join immediately."
}
```

Response:
```json
{
  "bot_reply": "Great — and your expected CTC range?",
  "extracted_fields": {
    "current_ctc": null,
    "expected_ctc": null,
    "notice_period": "immediate",
    "availability": "immediate"
  },
  "conversation_complete": false
}
```

When `conversation_complete` becomes `true`, `extracted_fields` should have
every field filled (or explicitly `null` if the candidate refused to
answer) — this is the signal for backend to move the candidate to the next
stage automatically.

---

## 5. Interview AI

**`POST /api/v1/interview/questions`**
Request: `{"job_id": "job_118"}`
Response:
```json
{
  "questions": [
    {"question_id": "q1", "text": "Explain how you'd design a rate-limited API.", "type": "open_text"},
    {"question_id": "q2", "text": "Which of these is a valid HTTP idempotent method?", "type": "mcq", "options": ["POST", "PUT", "PATCH"]}
  ]
}
```

**`POST /api/v1/interview/evaluate`**
Request:
```json
{
  "candidate_id": "cand_1029",
  "answers": [
    {"question_id": "q1", "answer_text": "I'd use a token bucket algorithm..."},
    {"question_id": "q2", "answer_text": "PUT"}
  ]
}
```
Response:
```json
{
  "candidate_id": "cand_1029",
  "interview_score": 84,
  "evaluated_answers": [
    {"question_id": "q1", "score": 90, "feedback": "Correct approach, good depth."},
    {"question_id": "q2", "score": 100, "feedback": "Correct."}
  ]
}
```

---

## 6. Decision Engine

**`POST /api/v1/decision/finalize`**

Request:
```json
{
  "candidate_id": "cand_1029",
  "job_id": "job_118"
}
```
(Again, looks up resume score, chatbot signal, and interview score
internally by candidate ID.)

Response:
```json
{
  "candidate_id": "cand_1029",
  "final_score": 88,
  "classification": "shortlist",
  "breakdown": {
    "resume_score": 92,
    "resume_weight": 0.4,
    "interview_score": 84,
    "interview_weight": 0.3,
    "chatbot_signal_score": 85,
    "chatbot_weight": 0.3
  },
  "explanation": "Strong technical match with clear interview performance; recommended for next round."
}
```

`classification` is one of: `"shortlist"`, `"hold"`, `"reject"` — this is
literally deterministic math (see earlier discussion), no LLM call needed
for this endpoint.

---

## Shared error shape (every endpoint uses this on failure)

```json
{
  "error": true,
  "error_code": "PARSE_FAILED",
  "message": "Could not extract valid JSON from resume text after 2 retries.",
  "candidate_id": "cand_1029"
}
```

Frontend should always check for `"error": true` before reading the rest
of a response, rather than assuming success.

---

## 7. Internal Administration & Health Endpoints (Additive)

Administrative endpoints provide operational telemetry, LLM token quotas, call logging, and module health monitoring.

**Security:** Like all `/api/v1/*` endpoints, `/api/v1/admin/*` endpoints require the shared internal secret header:
```http
X-Internal-Key: <INTERNAL_API_KEY>
```
Requests without this header or with an invalid key receive HTTP 401 Unauthorized.

**Correlation Tracking:** All engine endpoints accept optional correlation headers:
- `X-Request-Id`: Unique UUID of the API request / backend transaction.
- `X-Candidate-Ref`: Opaque UUID reference of the candidate or application.

---

### 7.1 Health & Module Telemetry
**`GET /api/v1/admin/health-summary`**
- Returns the operational status of all 6 AI modules, model configuration, and key presence.

Response:
```json
{
  "service": "HireAI AI Engine",
  "overallStatus": "HEALTHY",
  "checkedAt": "2026-10-10T12:00:00Z",
  "modules": [
    {
      "id": "jd_generator",
      "name": "Job Description Generator",
      "taskType": "jd_generation",
      "status": "HEALTHY",
      "lastSuccessAt": "2026-10-10T11:58:20Z",
      "lastFailureAt": null,
      "recentFailureCount": 0
    }
  ],
  "providers": {
    "activeProvider": "ollama",
    "activeModel": "llama3.2:3b",
    "ollama": { "name": "Ollama (Local)", "isConfigured": true, "reachable": true },
    "gemini": { "name": "Google Gemini", "isConfigured": true, "keyMasked": "...a91f" },
    "groq": { "name": "Groq API", "isConfigured": false, "keyMasked": "Not Configured" }
  },
  "environment": {
    "appEnv": "development",
    "engineHost": "localhost",
    "enginePort": 8000
  }
}
```

---

### 7.2 LLM Usage & Quota Statistics
**`GET /api/v1/admin/stats?range={today|week|all}`**
- `range` (optional, default `all`): Filter telemetry scope by `today`, `week`, or `all`.

Response:
```json
{
  "totalCalls": 45,
  "todayCalls": 10,
  "weekCalls": 32,
  "failedCalls": 1,
  "successRate": 97.8,
  "avgLatencySeconds": 1.25,
  "unknownTimeCount": 0,
  "quota": {
    "provider": "gemini",
    "tier": "Free Tier",
    "dailyLimit": 250,
    "usedToday": 10,
    "remainingToday": 240,
    "percentageUsed": 4.0,
    "timezone": "Asia/Kolkata"
  },
  "providerDistribution": [
    { "provider": "ollama", "calls": 35, "percentage": 77.8, "avgLatencySeconds": 1.15, "failedCount": 0 },
    { "provider": "gemini", "calls": 10, "percentage": 22.2, "avgLatencySeconds": 1.60, "failedCount": 1 }
  ],
  "moduleBreakdown": [
    { "id": "jd_generator", "name": "Job Description Generator", "calls": 20, "percentage": 44.4, "avgLatencySeconds": 1.10, "failedCount": 0 }
  ]
}
```

---

### 7.3 Query Call Logs
**`GET /api/v1/admin/logs`**
- Query Parameters: `query`, `task_type` / `module`, `status`, `provider`, `from_date`, `to_date`, `correlation_id`, `candidate_ref`, `page` (default 1), `size` (default 20, max 100).

Response:
```json
{
  "items": [
    {
      "id": "673f1a23-4567-890a-bcde-f1234567890a",
      "timestamp": "2026-10-10T11:58:20Z",
      "taskType": "jd_generation",
      "provider": "ollama",
      "model": "llama3.2:3b",
      "status": "SUCCESS",
      "latencySeconds": 1.12,
      "promptTokens": 120,
      "completionTokens": 65,
      "totalTokens": 185,
      "promptPreview": "Generate Job Description for...",
      "responsePreview": "We are seeking an experienced...",
      "errorMessage": null,
      "requestId": "req-9912",
      "candidateRef": "cand_1029"
    }
  ],
  "totalElements": 1,
  "page": 1,
  "size": 20,
  "totalPages": 1
}
```

---

### 7.4 Single Call Log Lookup
**`GET /api/v1/admin/logs/{log_id}`**
- Returns the complete single log entry by ID, including sanitized error message.

---

## What to lock in today's session

1. Do these field names/shapes look right to backend and frontend, or does
   anything need to be added/renamed before code gets written against it?
2. Confirm the base URL and auth approach (Internal Key `X-Internal-Key` for admin microservice communication).
3. Agree that any future change to this file needs a quick heads-up to both
   other teams before it's implemented — not discovered during integration.
