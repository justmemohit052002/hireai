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
OLLAMA_MODEL = os.getenv("OLLAMA_MODEL", "llama3.1:8b")
OLLAMA_BASE_URL = os.getenv("OLLAMA_BASE_URL", "http://localhost:11434")
GEMINI_API_KEY = os.getenv("GEMINI_API_KEY")
GROQ_API_KEY = os.getenv("GROQ_API_KEY")

# ---- LLM call reliability ----
LLM_TIMEOUT_SECONDS = int(os.getenv("LLM_TIMEOUT_SECONDS", "240"))
LLM_MAX_RETRIES = int(os.getenv("LLM_MAX_RETRIES", "2"))
LLM_RETRY_BACKOFF_SECONDS = float(os.getenv("LLM_RETRY_BACKOFF_SECONDS", "2"))

# ---- Server ----
PORT = int(os.getenv("PORT", "8000"))
LOG_LEVEL = os.getenv("LOG_LEVEL", "INFO")
LOG_FILE_PATH = os.getenv("LOG_FILE_PATH", "logs/app.log")
LLM_CALL_LOG_PATH = os.getenv("LLM_CALL_LOG_PATH", "logs/llm_call_log.jsonl")

# ---- CORS ----
# During team testing, frontend may call this service directly (not just
# through backend), so we allow common local dev ports. Tighten this list
# before any real deployment.
CORS_ALLOWED_ORIGINS = os.getenv(
    "CORS_ALLOWED_ORIGINS",
    "http://localhost:3000,http://localhost:5173,http://localhost:8080,http://localhost:4200",
).split(",")
