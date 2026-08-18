# HireAI — AI Engine

Standalone AI processing service for the HireAI recruitment platform.
Runs entirely free using local Ollama by default; swappable to a paid
provider later via one config change.

**New here?** Start with `SETUP_AND_TESTING_GUIDE.md` — it has full
step-by-step instructions for getting this running and testing every
endpoint, written for frontend/backend teammates who haven't touched
this project before.

---

## What's in this project

```
hireai-ai-engine/
├── main.py                          ← FastAPI app: routes, CORS, error handling
├── requirements.txt
├── .env.example                     ← copy to .env, holds all config
├── .gitignore
├── API_CONTRACT.md                  ← exact request/response shape per endpoint
├── SETUP_AND_TESTING_GUIDE.md       ← full setup guide for the team
├── HireAI_Postman_Collection.json   ← import into Postman for one-click testing
├── prompts/                         ← every LLM instruction, plain text, edit freely
└── app/
    ├── config.py                     ← all settings, loaded from .env
    ├── logger.py                      ← shared logging setup
    ├── llm_client.py                   ← single point every LLM call routes through
    │                                     (retry logic, provider switching, call logging)
    ├── jobs.py                          ← background job tracker (polling pattern)
    ├── utils.py                          ← shared JSON-extraction helper
    ├── models.py                          ← request/response shapes, camelCase handling
    ├── resume_parser.py                    ← Module 2 (async/polling)
    ├── match_engine.py                      ← Module 3 (no LLM - embeddings + math)
    ├── jd_generator.py                       ← Module 1 (sync)
    ├── chatbot.py                             ← Module 4 (sync)
    ├── interview_ai.py                         ← Module 5 (sync)
    └── decision_engine.py                       ← Module 6 (no LLM - pure formula)
```

---

## What makes this "industry standard" rather than a prototype script

- **Config, not hardcoding** — every setting (model name, timeouts, ports,
  allowed origins) lives in `.env`, not scattered through code
- **Retry logic** — LLM calls automatically retry with backoff on
  transient failures instead of failing on the first hiccup
- **Structured logging** — every request, job, and error is logged with a
  timestamp to `logs/app.log`, not scattered `print()` statements
- **Consistent error shape** — every failure, anywhere in the service,
  returns the same `{error, errorCode, message}` JSON shape, so calling
  code (Spring Boot or frontend) can handle errors generically
- **Real health checks** — `/health` actually verifies Ollama is
  reachable, not just "the server process is alive"
- **CORS configured** — frontend can test against this service directly
  during development, not just through backend
- **Separation of concerns** — config, logging, job tracking, and each
  business module are separate files with one clear responsibility each

## What's intentionally still simple (documented, not hidden)

- Job status (`app/jobs.py`) lives in memory — resets on restart, won't
  work across multiple server instances. Fine for a prototype; a real
  deployment would use Redis or a database table instead.
- No authentication between backend and this service yet.
- Only `LLM_PROVIDER=ollama` is exercised in daily use — Gemini/Groq code
  paths exist and are ready, but need an API key to actually test.

---

## Quick start

```bash
ollama serve                         # terminal 1, leave running
python -m venv venv && source venv/bin/activate   # terminal 2
pip install -r requirements.txt
cp .env.example .env
uvicorn main:app --reload
```
Then visit `http://localhost:8000/health` and `http://localhost:8000/docs`.

Full details, troubleshooting, and what each team needs to know:
see `SETUP_AND_TESTING_GUIDE.md`.
