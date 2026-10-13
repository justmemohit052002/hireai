# HireAI - AI Engine

Standalone AI processing service for the HireAI recruitment platform.
Runs entirely free using local Ollama by default; swappable to a paid
provider later via one config change.

**New here?** Start with `SETUP_AND_TESTING_GUIDE.md` - it has full
step-by-step instructions for getting this running and testing every
endpoint, written for frontend and backend teammates who haven't touched
this project before.

---

## What's in this project

```text
hireai-ai-engine/
├── main.py                          <- FastAPI app: routes, CORS, error handling
├── requirements.txt
├── .env.example                     <- copy to .env, holds all config
├── .gitignore
├── API_CONTRACT.md                  <- exact request/response shape per endpoint
├── SETUP_AND_TESTING_GUIDE.md       <- full setup guide for the team
├── HireAI_Postman_Collection.json   <- import into Postman for one-click testing
├── prompts/                         <- every LLM instruction, plain text, edit freely
└── app/
    ├── config.py                     <- all settings, loaded from .env
    ├── logger.py                      <- shared logging setup
    ├── llm_client.py                   <- single point every LLM call routes through
    │                                     (retry logic, provider switching, call logging)
    ├── jobs.py                          <- background job tracker (polling pattern)
    ├── utils.py                          <- shared JSON-extraction helper
    ├── models.py                          <- request/response shapes, camelCase handling
    ├── resume_parser.py                    <- Module 2 (async/polling)
    ├── match_engine.py                      <- Module 3 (no LLM: embeddings + math)
    ├── jd_generator.py                       <- Module 1 (sync)
    ├── chatbot.py                             <- Module 4 (sync)
    ├── interview_ai.py                         <- Module 5 (sync)
    └── decision_engine.py                       <- Module 6 (no LLM: pure formula)
```

---

## What makes this "industry standard" rather than a prototype script

- **Config, not hardcoding** - every setting (model name, timeouts, ports,
  allowed origins) lives in `.env`, not scattered through code
- **Strict Service Authentication** - all internal `/api/v1/*` endpoints require
  a shared constant-time verified `X-Internal-Key` header matching `INTERNAL_API_KEY`.
  Fails closed at startup outside development if the key is missing or default.
- **Fail-safe CORS policy** - outside development, allowed origins defaults to an
  empty list (`[]`) with no wildcard access, securing backend-to-backend traffic.
- **Retry logic** - LLM calls automatically retry with backoff on
  transient failures instead of failing on the first hiccup
- **Structured logging** - every request, job, and error is logged with a
  timestamp to `logs/app.log`, not scattered `print()` statements
- **Consistent error shape** - every failure, anywhere in the service,
  returns the same `{error, errorCode, message}` JSON shape, so calling
  code (Spring Boot or frontend) can handle errors generically
- **Real health checks** - `/health` verifies Ollama is reachable and
  remains public without requiring auth headers
- **Separation of concerns** - config, logging, job tracking, and each
  business module are separate files with one clear responsibility each

## Architecture Notes

- Job status (`app/jobs.py`) lives in memory: resets on restart, suitable
  for local single-instance operation. Production deployments can connect
  Redis or database-backed job states.
- Spring Boot backend routes all business transactions to this service
  using authenticated REST calls with `X-Internal-Key`.
- Only `LLM_PROVIDER=ollama` is required for daily local development.
  Gemini and Groq adapters are ready and activate when API keys are configured.

---

## Quick start

```bash
ollama serve                                       # terminal 1, leave running
python -m venv venv && source venv/bin/activate    # terminal 2
pip install -r requirements.txt
cp .env.example .env
uvicorn main:app --reload
```

Then verify service health:
- Public Health Check: `http://localhost:8000/health`
- OpenAPI Documentation: `http://localhost:8000/docs`

For testing internal endpoints, pass the `X-Internal-Key` header configured in your `.env`.

Full details, troubleshooting, and testing instructions:
see `SETUP_AND_TESTING_GUIDE.md`.
