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
    GEMINI_API_KEY, GROQ_API_KEY, GROQ_MODEL,
    LLM_TIMEOUT_SECONDS, LLM_MAX_RETRIES, LLM_RETRY_BACKOFF_SECONDS,
    LLM_CALL_LOG_PATH,
)
from app.logger import get_logger

logger = get_logger(__name__)


class LLMCallError(Exception):
    """Raised when the configured LLM provider fails after all retries."""


ACTIVE_PROVIDER = None
ACTIVE_MODEL = None


def set_active_provider(provider: str, model: str = None) -> None:
    """Dynamically switch active provider and model at runtime (e.g. from UI)."""
    global ACTIVE_PROVIDER, ACTIVE_MODEL
    ACTIVE_PROVIDER = provider
    ACTIVE_MODEL = model


def get_active_provider_info() -> tuple[str, str]:
    """Returns the currently active (provider, model)."""
    prov = ACTIVE_PROVIDER or LLM_PROVIDER
    if prov == "ollama":
        mdl = ACTIVE_MODEL or OLLAMA_MODEL
    elif prov == "groq":
        mdl = ACTIVE_MODEL or GROQ_MODEL
    else:
        mdl = "gemini-1.5-flash"
    return prov, mdl


import uuid
from datetime import datetime, timezone

def call_llm(prompt: str, task_type: str = "general", provider_override: str = None, model_override: str = None) -> str:
    last_error = None
    provider = provider_override or ACTIVE_PROVIDER or LLM_PROVIDER
    model = model_override or ACTIVE_MODEL

    for attempt in range(1, LLM_MAX_RETRIES + 2):  # e.g. 2 retries = 3 total attempts
        start = time.time()
        try:
            if provider == "ollama":
                text = _call_ollama(prompt, model=model)
            elif provider == "gemini":
                text = _call_gemini(prompt)
            elif provider == "groq":
                text = _call_groq(prompt, model=model)
            else:
                raise LLMCallError(f"Unknown LLM_PROVIDER: {provider}")

            latency = round(time.time() - start, 2)
            logger.info(f"[{task_type}] {provider} ({model or 'default'}) call succeeded in {latency}s (attempt {attempt})")
            _log_call(prompt, text, task_type, latency, attempt, provider=provider, status="SUCCESS")
            return text

        except Exception as e:
            last_error = e
            logger.warning(f"[{task_type}] {provider} call failed on attempt {attempt}: {e}")
            if attempt <= LLM_MAX_RETRIES:
                time.sleep(LLM_RETRY_BACKOFF_SECONDS * attempt)  # simple linear backoff

    latency = round(time.time() - start, 2)
    logger.error(f"[{task_type}] All {LLM_MAX_RETRIES + 1} attempts failed. Last error: {last_error}")
    _log_call(
        prompt=prompt,
        response_text="",
        task_type=task_type,
        latency=latency,
        attempt=LLM_MAX_RETRIES + 1,
        provider=provider,
        status="FAILED",
        error_message=str(last_error),
    )
    raise LLMCallError(f"LLM call failed after {LLM_MAX_RETRIES + 1} attempts: {last_error}")


def _call_ollama(prompt: str, model: str = None) -> str:
    use_model = model or OLLAMA_MODEL
    
    # Detect if prompt requests structured JSON output
    is_json_request = "json" in prompt.lower() or "{" in prompt

    payload = {
        "model": use_model,
        "prompt": prompt,
        "stream": False,
        "options": {
            "temperature": 0.1 if is_json_request else 0.3,
            "top_p": 0.9,
            "num_ctx": 4096,
            "num_predict": 1024,
            "repeat_penalty": 1.1,
        },
    }
    if is_json_request:
        payload["format"] = "json"

    try:
        response = requests.post(
            f"{OLLAMA_BASE_URL}/api/generate",
            json=payload,
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


def _call_groq(prompt: str, model: str = None) -> str:
    if not GROQ_API_KEY:
        raise LLMCallError("GROQ_API_KEY not set - required when LLM_PROVIDER=groq.")
    use_model = model or GROQ_MODEL
    response = requests.post(
        "https://api.groq.com/openai/v1/chat/completions",
        headers={"Authorization": f"Bearer {GROQ_API_KEY}"},
        json={"model": use_model, "messages": [{"role": "user", "content": prompt}]},
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


def mask_key(key: str | None) -> str:
    if not key:
        return ""
    if len(key) <= 8:
        return "****"
    return f"{key[:3]}...{key[-4:]}"


def _log_call(
    prompt: str,
    response_text: str,
    task_type: str,
    latency: float,
    attempt: int,
    provider: str = None,
    status: str = "SUCCESS",
    error_message: str = None,
) -> None:
    entry = {
        "id": str(uuid.uuid4()),
        "timestamp": datetime.now(timezone.utc).isoformat(),
        "task_type": task_type,
        "provider": provider or LLM_PROVIDER,
        "latency_seconds": latency,
        "attempt": attempt,
        "status": status,
        "error_message": error_message,
        "prompt_preview": prompt[:200] if prompt else "",
        "response_preview": response_text[:200] if response_text else "",
    }
    try:
        with open(LLM_CALL_LOG_PATH, "a", encoding="utf-8") as f:
            f.write(json.dumps(entry) + "\n")
    except Exception as e:
        logger.error(f"Failed to append to LLM call log: {e}")
