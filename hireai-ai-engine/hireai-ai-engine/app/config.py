"""
Centralized configuration. All environment-dependent values live here,
loaded from a .env file (see .env.example). Nothing else in the codebase
should call os.getenv() directly - import from here instead, so every
setting is documented and discoverable in one place.
"""

import os
from dotenv import load_dotenv

load_dotenv()  # reads .env if present, does nothing if it doesn't exist

# ---- LLM provider ----
LLM_PROVIDER = os.getenv("LLM_PROVIDER", "ollama")  # "ollama" | "gemini" | "groq"
OLLAMA_MODEL = os.getenv("OLLAMA_MODEL", "llama3.2:3b")
OLLAMA_BASE_URL = os.getenv("OLLAMA_BASE_URL", "http://localhost:11434")
GEMINI_API_KEY = os.getenv("GEMINI_API_KEY")
GROQ_API_KEY = os.getenv("GROQ_API_KEY")
GROQ_MODEL = os.getenv("GROQ_MODEL", "openai/gpt-oss-20b")

# ---- LLM call reliability ----
LLM_TIMEOUT_SECONDS = int(os.getenv("LLM_TIMEOUT_SECONDS", "240"))
LLM_MAX_RETRIES = int(os.getenv("LLM_MAX_RETRIES", "2"))
LLM_RETRY_BACKOFF_SECONDS = float(os.getenv("LLM_RETRY_BACKOFF_SECONDS", "2"))

# ---- Server ----
PORT = int(os.getenv("PORT", "8000"))
LOG_LEVEL = os.getenv("LOG_LEVEL", "INFO")
LOG_FILE_PATH = os.getenv("LOG_FILE_PATH", "logs/app.log")
LLM_CALL_LOG_PATH = os.getenv("LLM_CALL_LOG_PATH", "logs/llm_call_log.jsonl")

# ---- Error handling & debugging ----
# When false (default), API clients receive safe generic error messages without stack traces.
# Full error details and stack traces are always logged server-side for debugging.
SHOW_ERROR_DETAILS = os.getenv("SHOW_ERROR_DETAILS", "false").lower() in ("true", "1", "yes")

# ---- Security & Internal Service Authentication ----
INTERNAL_API_KEY = os.getenv("INTERNAL_API_KEY")
APP_ENV = os.getenv("APP_ENV", os.getenv("ENVIRONMENT", "development")).lower()

# Startup security validation:
# If APP_ENV is not "development" and the key is missing, empty, or equals the old default, raise an error.
OLD_DEFAULT_KEY = "hireai-internal-secret-key"
if APP_ENV != "development":
    if not INTERNAL_API_KEY or not str(INTERNAL_API_KEY).strip() or str(INTERNAL_API_KEY).strip() == OLD_DEFAULT_KEY:
        raise ValueError(
            "Security Error: INTERNAL_API_KEY must be configured with a secure, non-default secret in non-development environments."
        )

# ---- Admin Metrics Configuration ----
GEMINI_DAILY_LIMIT = int(os.getenv("GEMINI_DAILY_LIMIT", "250"))
ADMIN_TIMEZONE = os.getenv("ADMIN_TIMEZONE", os.getenv("TIMEZONE", "Asia/Kolkata"))
GEMINI_MODEL = os.getenv("GEMINI_MODEL", "gemini-1.5-flash")

# ---- CORS ----
# During local development, frontend may call this service directly, so default local ports are allowed.
# Outside development, CORS_ALLOWED_ORIGINS defaults to an empty list (no wildcard).
cors_env = os.getenv("CORS_ALLOWED_ORIGINS")
if cors_env is not None:
    CORS_ALLOWED_ORIGINS = [
        origin.strip() for origin in cors_env.split(",") if origin.strip() and origin.strip() != "*"
    ]
elif APP_ENV == "development":
    CORS_ALLOWED_ORIGINS = [
        "http://localhost:3000",
        "http://localhost:5173",
        "http://localhost:8080",
        "http://localhost:4200",
    ]
else:
    CORS_ALLOWED_ORIGINS = []

