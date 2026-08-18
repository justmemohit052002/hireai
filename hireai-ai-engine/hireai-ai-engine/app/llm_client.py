"""
Single entry point for every LLM call in the AI engine.

Every module calls call_llm(prompt) and never talks to Ollama/Gemini/Groq
directly - switching providers is a config change (app/config.py or .env),
not a code change in six different files.

Includes automatic retry with backoff: local LLMs occasionally fail on a
cold start or a transient hiccup, so a single failure doesn't need to
bubble all the way up to the caller.
"""

import time
import json
import requests

from app.config import (
    LLM_PROVIDER, OLLAMA_MODEL, OLLAMA_BASE_URL,
    GEMINI_API_KEY, GROQ_API_KEY,
    LLM_TIMEOUT_SECONDS, LLM_MAX_RETRIES, LLM_RETRY_BACKOFF_SECONDS,
    LLM_CALL_LOG_PATH,
)
from app.logger import get_logger

logger = get_logger(__name__)


class LLMCallError(Exception):
    """Raised when the configured LLM provider fails after all retries."""


def call_llm(prompt: str, task_type: str = "general") -> str:
    last_error = None

    for attempt in range(1, LLM_MAX_RETRIES + 2):  # e.g. 2 retries = 3 total attempts
        start = time.time()
        try:
            if LLM_PROVIDER == "ollama":
                text = _call_ollama(prompt)
            elif LLM_PROVIDER == "gemini":
                text = _call_gemini(prompt)
            elif LLM_PROVIDER == "groq":
                text = _call_groq(prompt)
            else:
                raise LLMCallError(f"Unknown LLM_PROVIDER: {LLM_PROVIDER}")

            latency = round(time.time() - start, 2)
            logger.info(f"[{task_type}] {LLM_PROVIDER} call succeeded in {latency}s (attempt {attempt})")
            _log_call(prompt, text, task_type, latency, attempt)
            return text

        except Exception as e:
            last_error = e
            logger.warning(f"[{task_type}] {LLM_PROVIDER} call failed on attempt {attempt}: {e}")
            if attempt <= LLM_MAX_RETRIES:
                time.sleep(LLM_RETRY_BACKOFF_SECONDS * attempt)  # simple linear backoff

    logger.error(f"[{task_type}] All {LLM_MAX_RETRIES + 1} attempts failed. Last error: {last_error}")
    raise LLMCallError(f"LLM call failed after {LLM_MAX_RETRIES + 1} attempts: {last_error}")


def _call_ollama(prompt: str) -> str:
    try:
        response = requests.post(
            f"{OLLAMA_BASE_URL}/api/generate",
            json={"model": OLLAMA_MODEL, "prompt": prompt, "stream": False},
            timeout=LLM_TIMEOUT_SECONDS,
        )
        response.raise_for_status()
        return response.json()["response"]
    except requests.exceptions.ConnectionError:
        raise LLMCallError(
            f"Could not reach Ollama at {OLLAMA_BASE_URL}. Is 'ollama serve' running?"
        )
    except requests.exceptions.Timeout:
        raise LLMCallError(f"Ollama did not respond within {LLM_TIMEOUT_SECONDS}s.")


def _call_gemini(prompt: str) -> str:
    if not GEMINI_API_KEY:
        raise LLMCallError("GEMINI_API_KEY not set - required when LLM_PROVIDER=gemini.")
    url = (
        "https://generativelanguage.googleapis.com/v1beta/models/"
        f"gemini-1.5-flash:generateContent?key={GEMINI_API_KEY}"
    )
    response = requests.post(
        url, json={"contents": [{"parts": [{"text": prompt}]}]}, timeout=60
    )
    response.raise_for_status()
    return response.json()["candidates"][0]["content"]["parts"][0]["text"]


def _call_groq(prompt: str) -> str:
    if not GROQ_API_KEY:
        raise LLMCallError("GROQ_API_KEY not set - required when LLM_PROVIDER=groq.")
    response = requests.post(
        "https://api.groq.com/openai/v1/chat/completions",
        headers={"Authorization": f"Bearer {GROQ_API_KEY}"},
        json={"model": "llama-3.1-8b-instant", "messages": [{"role": "user", "content": prompt}]},
        timeout=60,
    )
    response.raise_for_status()
    return response.json()["choices"][0]["message"]["content"]


def check_ollama_reachable() -> bool:
    """Used by the /health endpoint to report real status, not just 'server is up'."""
    try:
        response = requests.get(f"{OLLAMA_BASE_URL}/api/tags", timeout=5)
        return response.status_code == 200
    except requests.exceptions.RequestException:
        return False


def _log_call(prompt: str, response_text: str, task_type: str, latency: float, attempt: int) -> None:
    entry = {
        "task_type": task_type,
        "provider": LLM_PROVIDER,
        "latency_seconds": latency,
        "attempt": attempt,
        "prompt_preview": prompt[:200],
        "response_preview": response_text[:200],
    }
    with open(LLM_CALL_LOG_PATH, "a") as f:
        f.write(json.dumps(entry) + "\n")
