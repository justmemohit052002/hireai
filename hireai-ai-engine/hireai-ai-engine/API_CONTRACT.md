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
- IDs: strings (e.g. `"cand_1029"`, `"job_44""`) — not raw database integers,
  so the ID format can change later without breaking the frontend
- Missing/unknown values: `null` — never an empty string standing in for
  "no data", and never omit a key entirely
- Timestamps: ISO 8601 strings, e.g. `"2026-08-08T14:30:00Z"`
- Every error response uses the same shape (see bottom of this doc)

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

Request:
```json
{
  "candidate_id": "cand_1029",
  "job_id": "job_118"
}
```
(The AI engine looks up the already-parsed resume and JD internally by ID —
frontend doesn't need to resend that data.)

Response:
```json
{
  "candidate_id": "cand_1029",
  "job_id": "job_118",
  "match_score": 92,
  "matched_skills": ["Node.js", "PostgreSQL"],
  "missing_skills": ["Kubernetes"],
  "rationale": "Strong overlap on core backend stack; no cloud orchestration experience listed.",
  "auto_action": "shortlist"
}
```

`auto_action` is one of: `"shortlist"` (score ≥ 80), `"reject"` (score <
40), or `"review"` (needs a human to decide) — this drives whether the
candidate row shows up in the recruiter's queue at all.

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

## What to lock in today's session

1. Do these field names/shapes look right to backend and frontend, or does
   anything need to be added/renamed before code gets written against it?
2. Confirm the base URL and auth approach (API key header? JWT? none yet
   for prototype?)
3. Agree that any future change to this file needs a quick heads-up to both
   other teams before it's implemented — not discovered during integration
