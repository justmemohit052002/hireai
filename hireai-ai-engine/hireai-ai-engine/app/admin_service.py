"""
Admin Service for HireAI AI Engine.
Provides module health evaluation, LLM usage & cost analytics, call log audit queries,
and provider/key status indicators (read-only, sanitized).
"""

import json
import os
import time
from abc import ABC, abstractmethod
from datetime import datetime, timezone, timedelta
from typing import Dict, Any, List, Optional, Iterator
from zoneinfo import ZoneInfo

from app.config import (
    LLM_PROVIDER,
    OLLAMA_MODEL,
    OLLAMA_BASE_URL,
    GEMINI_API_KEY,
    GROQ_API_KEY,
    GROQ_MODEL,
    GEMINI_MODEL,
    LLM_CALL_LOG_PATH,
    LLM_TIMEOUT_SECONDS,
    LLM_MAX_RETRIES,
    GEMINI_DAILY_LIMIT,
    ADMIN_TIMEZONE,
)
from app.llm_client import check_ollama_reachable, get_active_provider_info, mask_key
from app.logger import get_logger

logger = get_logger(__name__)

# Explicit mapping from logged task_type values to the 6 system modules
TASK_TYPE_TO_MODULE: Dict[str, str] = {
    "jd_generate": "jd_generator",
    "resume_parse": "resume_parser",
    "match_score": "match_engine",
    "chatbot_message": "chatbot",
    "chatbot": "chatbot",
    "interview_questions": "interview_ai",
    "interview_eval": "interview_ai",
    "interview_evaluate": "interview_ai",
    "decision_finalize": "decision_engine",
    "decision_engine": "decision_engine",
    "document_analysis": "document_analyzer",
    "document_qa": "document_analyzer",
    "custom_prompt_test": "document_analyzer",
}


# ==========================================
# Log Source Abstraction & JSONL Reader
# ==========================================

class LogSource(ABC):
    """Abstract interface for reading AI invocation audit records."""

    @abstractmethod
    def stream_entries(self) -> Iterator[Dict[str, Any]]:
        """Yields parsed log entries from oldest to newest."""
        pass

    @abstractmethod
    def read_all(self) -> List[Dict[str, Any]]:
        """Returns list of valid log entries."""
        pass

    @abstractmethod
    def get_entry_by_id(self, log_id: str) -> Optional[Dict[str, Any]]:
        """Returns a single log entry by its UUID/ID."""
        pass


class JsonlLogSource(LogSource):
    """Streams and parses records from llm_call_log.jsonl with error tolerance."""

    def __init__(self, file_path: str = LLM_CALL_LOG_PATH):
        self.file_path = file_path

    def stream_entries(self) -> Iterator[Dict[str, Any]]:
        if not os.path.exists(self.file_path):
            return

        try:
            with open(self.file_path, "r", encoding="utf-8") as f:
                for idx, line in enumerate(f):
                    line = line.strip()
                    if not line:
                        continue
                    try:
                        data = json.loads(line)
                        if not isinstance(data, dict):
                            continue

                        # Ensure valid structure
                        if "id" not in data:
                            data["id"] = f"legacy-log-{idx}"
                        if "status" not in data:
                            data["status"] = "SUCCESS"
                        if "prompt_preview" in data and isinstance(data["prompt_preview"], str):
                            data["prompt_preview"] = data["prompt_preview"][:200]
                        if "response_preview" in data and isinstance(data["response_preview"], str):
                            data["response_preview"] = data["response_preview"][:200]

                        yield data
                    except Exception as parse_err:
                        # Skip malformed lines without crashing
                        logger.debug(f"Skipping malformed JSONL line {idx}: {parse_err}")
                        continue
        except Exception as e:
            logger.error(f"Failed streaming from {self.file_path}: {e}")

    def read_all(self, max_entries: int = 10000) -> List[Dict[str, Any]]:
        entries = list(self.stream_entries())
        if len(entries) > max_entries:
            return entries[-max_entries:]
        return entries

    def get_entry_by_id(self, log_id: str) -> Optional[Dict[str, Any]]:
        if not log_id:
            return None
        clean_id = str(log_id).strip().lower()
        for entry in self.stream_entries():
            if str(entry.get("id", "")).lower() == clean_id:
                return entry
        return None


# Default LogSource instance (swappable for DB table later)
log_source: LogSource = JsonlLogSource()


# ==========================================
# In-Memory Cache for Computed Statistics
# ==========================================

_stats_cache: Dict[str, Any] = {}
_stats_cache_expiry: float = 0.0
STATS_CACHE_TTL_SECONDS = 60


def _parse_iso_datetime(ts_str: Optional[str]) -> Optional[datetime]:
    """Safely parses ISO timestamp strings to timezone-aware UTC datetimes."""
    if not ts_str:
        return None
    try:
        clean = ts_str.replace("Z", "+00:00")
        dt = datetime.fromisoformat(clean)
        if dt.tzinfo is None:
            dt = dt.replace(tzinfo=timezone.utc)
        return dt
    except Exception:
        return None


def get_system_health() -> Dict[str, Any]:
    """
    Evaluates real operational reachability and health of all 6 AI Engine modules
    based on provider status and recent call outcomes from the log.
    """
    ollama_reachable = check_ollama_reachable()
    active_prov, active_model = get_active_provider_info()

    gemini_configured = bool(GEMINI_API_KEY and len(str(GEMINI_API_KEY).strip()) > 5)
    groq_configured = bool(GROQ_API_KEY and len(str(GROQ_API_KEY).strip()) > 5)

    # Determine global engine operational status
    if active_prov == "ollama":
        engine_status = "HEALTHY" if ollama_reachable else "DEGRADED"
    elif active_prov == "gemini":
        engine_status = "HEALTHY" if gemini_configured else "DOWN"
    elif active_prov == "groq":
        engine_status = "HEALTHY" if groq_configured else "DOWN"
    else:
        engine_status = "HEALTHY"

    # Analyze recent log calls (last 20 calls per module) to reflect real outcomes
    all_logs = log_source.read_all()
    logs_by_module: Dict[str, List[Dict[str, Any]]] = {}

    for entry in all_logs:
        task = entry.get("task_type", "general")
        mod_id = TASK_TYPE_TO_MODULE.get(task, task)
        logs_by_module.setdefault(mod_id, []).append(entry)

    def calculate_module_telemetry(mod_id: str, requires_llm: bool) -> Dict[str, Any]:
        mod_logs = logs_by_module.get(mod_id, [])
        recent_calls = mod_logs[-20:] if len(mod_logs) > 20 else mod_logs

        recent_failures = [c for c in recent_calls if c.get("status") == "FAILED"]
        recent_failure_count = len(recent_failures)

        last_success_at = None
        last_failure_at = None

        for c in reversed(mod_logs):
            ts = c.get("timestamp")
            if c.get("status") == "SUCCESS" and not last_success_at:
                last_success_at = ts
            elif c.get("status") == "FAILED" and not last_failure_at:
                last_failure_at = ts
            if last_success_at and last_failure_at:
                break

        # Status determination logic:
        if not requires_llm:
            # Deterministic math/rule modules stay HEALTHY unless recently failed
            status = "HEALTHY" if recent_failure_count == 0 else "DEGRADED"
        else:
            provider_ok = (
                (active_prov == "ollama" and ollama_reachable)
                or (active_prov == "gemini" and gemini_configured)
                or (active_prov == "groq" and groq_configured)
            )
            last_call_failed = bool(recent_calls and recent_calls[-1].get("status") == "FAILED")

            if not provider_ok and last_call_failed:
                status = "DOWN"
            elif not provider_ok or (len(recent_calls) >= 5 and (recent_failure_count / len(recent_calls)) > 0.5):
                status = "DEGRADED"
            else:
                status = "HEALTHY"

        return {
            "status": status,
            "lastSuccessAt": last_success_at,
            "lastFailureAt": last_failure_at,
            "recentFailureCount": recent_failure_count,
        }

    # 6 Standard Modules
    module_definitions = [
        {
            "id": "jd_generator",
            "name": "Job Description Generator",
            "category": "Synchronous LLM",
            "endpoint": "/api/v1/jd/generate",
            "provider": active_prov,
            "model": active_model,
            "requiresLlm": True,
            "description": "Generates structured job requirements, tech stack, and responsibilities.",
        },
        {
            "id": "resume_parser",
            "name": "Resume Parser (Async Worker)",
            "category": "Asynchronous Polling LLM",
            "endpoint": "/api/v1/resumes/parse",
            "provider": active_prov,
            "model": active_model,
            "requiresLlm": True,
            "description": "Background thread parsing of candidate CVs into structured profile JSON.",
        },
        {
            "id": "match_engine",
            "name": "Match Engine (Vector Similarity)",
            "category": "Deterministic Vector / TF-IDF",
            "endpoint": "/api/v1/match/score",
            "provider": "Vector Engine (Local)",
            "model": "Cosine Semantic Matcher",
            "requiresLlm": False,
            "description": "Computes weighted overlap and ATS vector similarity score between resume and JD.",
        },
        {
            "id": "chatbot",
            "name": "Pre-Screening Chatbot",
            "category": "Conversational LLM",
            "endpoint": "/api/v1/chatbot/message",
            "provider": active_prov,
            "model": active_model,
            "requiresLlm": True,
            "description": "Multi-turn pre-screening conversational agent assessing candidate experience.",
        },
        {
            "id": "interview_ai",
            "name": "Interview AI (Q&A & Evaluator)",
            "category": "Evaluator LLM",
            "endpoint": "/api/v1/interview/questions & /evaluate",
            "provider": active_prov,
            "model": active_model,
            "requiresLlm": True,
            "description": "Generates technical questions and rubric-evaluates candidate response answers.",
        },
        {
            "id": "decision_engine",
            "name": "Decision Engine (Composite)",
            "category": "Weighted Composite Rule Engine",
            "endpoint": "/api/v1/decision/finalize",
            "provider": "Rule Engine (Deterministic)",
            "model": "Weighted Scoring Model",
            "requiresLlm": False,
            "description": "Aggregates resume match (40%), interview (40%), and chatbot signal (20%).",
        },
    ]

    enriched_modules = []
    for m in module_definitions:
        telemetry = calculate_module_telemetry(m["id"], m["requiresLlm"])
        enriched_modules.append({**m, **telemetry})

    providers = {
        "activeProvider": active_prov,
        "activeModel": active_model,
        "ollama": {
            "name": "Ollama (Local LLM)",
            "baseUrl": OLLAMA_BASE_URL,
            "model": OLLAMA_MODEL,
            "reachable": ollama_reachable,
            "isConfigured": True,
        },
        "gemini": {
            "name": "Google Gemini API",
            "model": GEMINI_MODEL or "gemini-1.5-flash",
            "isConfigured": gemini_configured,
            "keyMasked": mask_key(GEMINI_API_KEY) if gemini_configured else "Not Configured",
        },
        "groq": {
            "name": "Groq Cloud API",
            "model": GROQ_MODEL,
            "isConfigured": groq_configured,
            "keyMasked": mask_key(GROQ_API_KEY) if groq_configured else "Not Configured",
        },
    }

    return {
        "service": "HireAI AI Engine",
        "overallStatus": engine_status,
        "checkedAt": datetime.now(timezone.utc).isoformat(),
        "modules": enriched_modules,
        "providers": providers,
        "environment": {
            "timeoutSeconds": LLM_TIMEOUT_SECONDS,
            "maxRetries": LLM_MAX_RETRIES,
        },
    }


def get_llm_statistics(range_param: str = "all") -> Dict[str, Any]:
    """
    Computes aggregated LLM usage, latency, and quota metrics with a 60s TTL cache.
    `range_param` accepts 'today', 'week', or 'all' (default).
    """
    global _stats_cache, _stats_cache_expiry
    now_ts = time.time()
    cache_key = f"stats_{range_param.lower()}"

    if cache_key in _stats_cache and now_ts < _stats_cache_expiry:
        return _stats_cache[cache_key]

    # Resolve configured timezone
    try:
        admin_tz = ZoneInfo(ADMIN_TIMEZONE)
    except Exception:
        try:
            admin_tz = ZoneInfo("Asia/Kolkata")
        except Exception:
            admin_tz = timezone(timedelta(hours=5, minutes=30))

    now_tz = datetime.now(admin_tz)
    today_date_str = now_tz.strftime("%Y-%m-%d")
    one_week_ago_tz = now_tz - timedelta(days=7)

    logs = log_source.read_all()
    total_calls = len(logs)
    today_calls = 0
    week_calls = 0
    failed_calls = 0
    unknown_time_count = 0

    gemini_today_requests = 0

    # Range-scoped collectors
    scope_provider_counts: Dict[str, int] = {}
    scope_provider_failures: Dict[str, int] = {}
    scope_provider_latencies: Dict[str, List[float]] = {}

    scope_module_counts: Dict[str, int] = {}
    scope_module_failures: Dict[str, int] = {}
    scope_module_latencies: Dict[str, List[float]] = {}

    all_latencies: List[float] = []

    for entry in logs:
        ts_dt = _parse_iso_datetime(entry.get("timestamp"))
        prov = str(entry.get("provider", "unknown")).lower()
        task = str(entry.get("task_type", "general"))
        lat = float(entry.get("latency_seconds", 0.0))
        status = str(entry.get("status", "SUCCESS")).upper()

        is_failed = (status == "FAILED")
        if is_failed:
            failed_calls += 1

        all_latencies.append(lat)

        # Date categorization in configured timezone
        in_today = False
        in_week = False

        if ts_dt is not None:
            entry_tz = ts_dt.astimezone(admin_tz)
            if entry_tz.strftime("%Y-%m-%d") == today_date_str:
                today_calls += 1
                in_today = True
                if prov == "gemini":
                    gemini_today_requests += 1
            if entry_tz >= one_week_ago_tz:
                week_calls += 1
                in_week = True
        else:
            # Missing or unparseable timestamp: do NOT count as today
            unknown_time_count += 1

        # Check if entry falls in selected scope
        in_scope = True
        if range_param == "today":
            in_scope = in_today
        elif range_param == "week":
            in_scope = in_week

        if in_scope:
            scope_provider_counts[prov] = scope_provider_counts.get(prov, 0) + 1
            if is_failed:
                scope_provider_failures[prov] = scope_provider_failures.get(prov, 0) + 1
            scope_provider_latencies.setdefault(prov, []).append(lat)

            scope_module_counts[task] = scope_module_counts.get(task, 0) + 1
            if is_failed:
                scope_module_failures[task] = scope_module_failures.get(task, 0) + 1
            scope_module_latencies.setdefault(task, []).append(lat)

    avg_latency = round(sum(all_latencies) / len(all_latencies), 2) if all_latencies else 0.0
    scope_total = sum(scope_module_counts.values())

    module_breakdown = []
    for mod, count in scope_module_counts.items():
        lats = scope_module_latencies.get(mod, [])
        mod_avg_lat = round(sum(lats) / len(lats), 2) if lats else 0.0
        module_breakdown.append({
            "module": mod,
            "callCount": count,
            "failedCount": scope_module_failures.get(mod, 0),
            "avgLatencySeconds": mod_avg_lat,
            "percentage": round((count / scope_total * 100), 1) if scope_total > 0 else 0.0,
        })

    provider_breakdown = []
    for p_name, count in scope_provider_counts.items():
        p_lats = scope_provider_latencies.get(p_name, [])
        p_avg_lat = round(sum(p_lats) / len(p_lats), 2) if p_lats else 0.0
        provider_breakdown.append({
            "provider": p_name,
            "callCount": count,
            "failedCount": scope_provider_failures.get(p_name, 0),
            "avgLatencySeconds": p_avg_lat,
            "percentage": round((count / scope_total * 100), 1) if scope_total > 0 else 0.0,
        })

    success_rate = round(((total_calls - failed_calls) / total_calls * 100), 1) if total_calls > 0 else 100.0

    result = {
        "totalCalls": total_calls,
        "todayCalls": today_calls,
        "weekCalls": week_calls,
        "failedCalls": failed_calls,
        "unknownTimeCount": unknown_time_count,
        "successRate": success_rate,
        "avgLatencySeconds": avg_latency,
        "quota": {
            "provider": "gemini",
            "tier": "Free Tier",
            "dailyLimit": GEMINI_DAILY_LIMIT,
            "usedToday": gemini_today_requests,
            "remainingToday": max(0, GEMINI_DAILY_LIMIT - gemini_today_requests),
            "percentageUsed": round((gemini_today_requests / GEMINI_DAILY_LIMIT * 100), 1),
        },
        "providerDistribution": provider_breakdown,
        "moduleBreakdown": module_breakdown,
    }

    _stats_cache[cache_key] = result
    _stats_cache_expiry = now_ts + STATS_CACHE_TTL_SECONDS
    return result


def query_call_logs(
    query: Optional[str] = None,
    task_type: Optional[str] = None,
    module: Optional[str] = None,
    status: Optional[str] = None,
    provider: Optional[str] = None,
    from_date: Optional[str] = None,
    to_date: Optional[str] = None,
    correlation_id: Optional[str] = None,
    candidate_ref: Optional[str] = None,
    page: int = 1,
    size: int = 20,
) -> Dict[str, Any]:
    """
    Search and filter audit call logs with pagination and correlation tracking.
    """
    effective_task = module or task_type
    effective_correlation = correlation_id or candidate_ref
    effective_page = max(1, page)
    effective_size = min(100, max(1, size))

    from_dt = _parse_iso_datetime(from_date)
    to_dt = _parse_iso_datetime(to_date)

    logs = log_source.read_all()
    # Reverse chronological (newest first)
    logs.reverse()

    filtered = []
    q = (query or "").lower().strip()

    for item in logs:
        # Search query across prompt, response, id, task_type, request_id, candidate_ref
        if q:
            p_text = str(item.get("prompt_preview", "")).lower()
            r_text = str(item.get("response_preview", "")).lower()
            t_type = str(item.get("task_type", "")).lower()
            p_prov = str(item.get("provider", "")).lower()
            l_id = str(item.get("id", "")).lower()
            req_id = str(item.get("request_id", "")).lower()
            cand_ref = str(item.get("candidate_ref", "")).lower()
            if (
                q not in p_text
                and q not in r_text
                and q not in t_type
                and q not in p_prov
                and q not in l_id
                and q not in req_id
                and q not in cand_ref
            ):
                continue

        if effective_task and effective_task != "ALL":
            item_task = str(item.get("task_type", "")).lower()
            mapped_mod = TASK_TYPE_TO_MODULE.get(item_task, item_task)
            target_task = effective_task.lower()
            if item_task != target_task and mapped_mod != target_task:
                continue

        if status and status != "ALL":
            if str(item.get("status", "SUCCESS")).upper() != status.upper():
                continue

        if provider and provider != "ALL":
            if str(item.get("provider", "")).lower() != provider.lower():
                continue

        if effective_correlation:
            req_id = str(item.get("request_id", "")).lower()
            cand_ref = str(item.get("candidate_ref", "")).lower()
            target_corr = effective_correlation.lower()
            if target_corr not in req_id and target_corr not in cand_ref:
                continue

        # Date filtering
        if from_dt or to_dt:
            item_dt = _parse_iso_datetime(item.get("timestamp"))
            if item_dt:
                if from_dt and item_dt < from_dt:
                    continue
                if to_dt and item_dt > to_dt:
                    continue
            else:
                if from_dt or to_dt:
                    continue

        filtered.append(item)

    total_elements = len(filtered)
    start_idx = (effective_page - 1) * effective_size
    end_idx = start_idx + effective_size
    page_items = filtered[start_idx:end_idx]

    return {
        "items": page_items,
        "totalElements": total_elements,
        "page": effective_page,
        "size": effective_size,
        "totalPages": (total_elements + effective_size - 1) // effective_size if effective_size > 0 else 1,
    }


def get_single_log(log_id: str) -> Optional[Dict[str, Any]]:
    """Retrieves a single audit log entry by ID."""
    return log_source.get_entry_by_id(log_id)
