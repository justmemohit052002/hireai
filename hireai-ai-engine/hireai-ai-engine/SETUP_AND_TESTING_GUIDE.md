# HireAI AI Engine — Setup & Testing Guide

**Who this is for:** Frontend and backend team members who need to run this
service locally to test their own integration against it. You don't need
to understand AI/LLMs to follow this — just follow each step in order.

**What this service does:** It's a standalone AI processing service. It
receives requests (resume text, job details, etc.), processes them using
a local AI model, and returns structured JSON results. It never touches
the database directly — that's the backend's job.

---

## Part 1 — One-time setup (do this once)

### Step 1.1 — Install Python
Skip if you already have Python 3.11 or newer.
- Download from https://www.python.org/downloads/
- **Windows users:** during install, tick "Add Python to PATH"
- Verify: open a terminal and run `python --version`

### Step 1.2 — Install Ollama (the free local AI model runner)
- Download from https://ollama.com/download
- Verify: `ollama --version`
- Download the model this project uses (one-time, ~5GB):
  ```bash
  ollama pull llama3.1:8b
  ```

### Step 1.3 — Get the project files
Unzip `hireai-ai-engine.zip` somewhere on your computer. Open that folder
in a terminal (or in VS Code, then open its terminal).

### Step 1.4 — Create a virtual environment
```bash
python -m venv venv
```
Activate it:
```bash
# Mac/Linux
source venv/bin/activate
# Windows
venv\Scripts\activate
```
Your terminal prompt should now start with `(venv)`. **You'll need to do
this activation step every time you open a new terminal for this project**
— it's not a one-time thing.

### Step 1.5 — Install dependencies
```bash
pip install -r requirements.txt
```
This takes a few minutes the first time. It also downloads a small
(~90MB) AI model used for matching resumes to jobs — that's normal and
only happens once.

### Step 1.6 — Set up your configuration file
```bash
# Mac/Linux
cp .env.example .env
# Windows
copy .env.example .env
```
Open the new `.env` file — the defaults are already correct for local
testing, you don't need to change anything unless told otherwise.

**You're done with one-time setup.** Steps below are what you'll repeat
every time you want to run and test the service.

---

## Part 2 — Running the service (do this every time)

You need **two terminals open at the same time**, both staying open while
you work.

### Terminal 1 — start Ollama
```bash
ollama serve
```
Leave this running. If it says something like "address already in use,"
that means Ollama is already running in the background — that's fine,
just leave it as is.

### Terminal 2 — start the AI engine
Navigate to the project folder, activate the venv (Step 1.4 above), then:
```bash
uvicorn main:app --reload
```
You should see output ending in:
```
Uvicorn running on http://127.0.0.1:8000
```
Leave this running too.

### Confirm it's working
Open a browser and visit:
```
http://localhost:8000/health
```
You should see something like:
```json
{
  "status": "ok",
  "llmProvider": "ollama",
  "ollamaModel": "llama3.1:8b",
  "ollamaReachable": true
}
```
If `ollamaReachable` is `false`, go back to Terminal 1 and make sure
`ollama serve` is actually running without errors.

---

## Part 3 — Testing the endpoints

You have two options. Pick whichever you're more comfortable with.

### Option A — Browser-based testing (no extra tools needed)
Go to `http://localhost:8000/docs`. This is an interactive page — click
any endpoint, click "Try it out," edit the example values, click
"Execute," see the response immediately below.

### Option B — Postman (if your team already uses it)
1. Open Postman
2. Click **Import**, select `HireAI_Postman_Collection.json` from the
   project folder
3. All 8 endpoints appear pre-configured with example request bodies —
   just click **Send** on any of them

---

## Part 4 — What frontend needs to know

- Base URL for local testing: `http://localhost:8000`
- All request and response fields use **camelCase** (e.g. `candidateId`,
  `matchScore`) — matches what you'd expect from a typical JS/JSON API
- **Resume Parser works differently from the others** — it's a two-step
  process because it's slow (30–90 seconds on a local machine):
  1. `POST /api/v1/resumes/parse` → returns `{jobId, status: "processing"}`
     immediately
  2. Poll `GET /api/v1/resumes/status/{jobId}` every few seconds until
     `status` becomes `"complete"` — the result is inside the `result` field
- Every other endpoint responds directly and quickly (under a few seconds)
- CORS is already enabled for common local dev ports
  (`localhost:3000`, `:5173`, `:8080`, `:4200`) — if your dev server runs
  on a different port, tell the AI engine owner to add it to `.env`

## Part 5 — What backend needs to know

- This service is meant to be called **by Spring Boot**, not the other
  way around — it never writes to PostgreSQL directly
- Every error response (regardless of what went wrong) comes back in this
  same shape, so you can handle errors generically:
  ```json
  {"error": true, "errorCode": "LLM_CALL_FAILED", "message": "..."}
  ```
- For the Resume Parser's async pattern specifically: your Spring Boot
  code should not hold an HTTP connection open waiting — start the job,
  store the `jobId`, and check back later (a scheduled task or a
  short-interval poll works)
- Full request/response shapes for every endpoint are documented in
  `API_CONTRACT.md`

---

## Troubleshooting

| Problem | Likely cause | Fix |
|---|---|---|
| `ollamaReachable: false` on `/health` | Ollama not running | Check Terminal 1, run `ollama serve` |
| Browser shows connection refused at `localhost:8000` | AI engine not running | Check Terminal 2 for errors, re-run `uvicorn main:app --reload` |
| `ModuleNotFoundError` | Dependencies not installed, or venv not active | Check your prompt shows `(venv)`; re-run `pip install -r requirements.txt` |
| CORS error in browser console | Your frontend's port isn't in the allowed list | Ask the AI engine owner to add your port to `CORS_ALLOWED_ORIGINS` in `.env` |
| Resume parser stuck on `"processing"` for over ~3 minutes | Ollama may be overloaded or stuck | Check Terminal 1 for errors; try restarting `ollama serve` |
| `{"error": true, "errorCode": "LLM_RESPONSE_INVALID", ...}` | The AI model returned something unexpected | This is a known area still being refined — report it to the AI engine owner with the exact request you sent |

---

## Where to look for more detail

- **`API_CONTRACT.md`** — exact field names and types for every endpoint
- **`logs/app.log`** — full activity log, useful if something breaks and
  you need to see what actually happened
- **`logs/llm_call_log.jsonl`** — every individual AI call made, with
  timing — useful for reporting slow or failed calls
