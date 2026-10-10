"""
Admin Service for HireAI AI Engine.
Provides module health evaluation, LLM usage & cost analytics, call log audit queries,
and provider/key status indicators (read-only, sanitized).
"""

import json
import os
from datetime import datetime, timezone, timedelta
from typing import Dict, Any, List, Optional

from app.config import (
    LLM_PROVIDER,
    OLLAMA_MODEL,
    OLLAMA_BASE_URL,
    GEMINI_API_KEY,
    GROQ_API_KEY,
    GROQ_MODEL,
    LLM_CALL_LOG_PATH,
    LLM_TIMEOUT_SECONDS,
    LLM_MAX_RETRIES,
)
from app.llm_client import check_ollama_reachable, get_active_provider_info, mask_key
from app.logger import get_logger

logger = get_logger(__name__)


def get_system_health() -> Dict[str, Any]:
    """
    Evaluates reachability and operational status of all 6 AI Engine modules.
    """
    ollama_reachable = check_ollama_reachable()
    active_prov, active_model = get_active_provider_info()
    
    gemini_configured = bool(GEMINI_API_KEY and len(GEMINI_API_KEY.strip()) > 5)
    groq_configured = bool(GROQ_API_KEY and len(GROQ_API_KEY.strip()) > 5)
    
    # Determine global engine operational status
    if active_prov == "ollama":
        engine_status = "HEALTHY" if ollama_reachable else "DEGRADED"
    elif active_prov == "gemini":
        engine_status = "HEALTHY" if gemini_configured else "DOWN"
    elif active_prov == "groq":
        engine_status = "HEALTHY" if groq_configured else "DOWN"
    else:
        engine_status = "HEALTHY"

    # All 6 AI engine modules with their operational characteristics
    modules = [
        {
            "id": "jd_generator",
            "name": "Job Description Generator",
            "category": "Synchronous LLM",
            "endpoint": "/api/v1/jd/generate",
            "provider": active_prov,
            "model": active_model,
            "requiresLlm": True,
            "status": "HEALTHY" if (engine_status == "HEALTHY") else "DEGRADED",
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
            "status": "HEALTHY" if (engine_status == "HEALTHY") else "DEGRADED",
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
            "status": "HEALTHY",
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
            "status": "HEALTHY" if (engine_status == "HEALTHY") else "DEGRADED",
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
            "status": "HEALTHY" if (engine_status == "HEALTHY") else "DEGRADED",
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
            "status": "HEALTHY",
            "description": "Aggregates resume match (40%), interview (40%), and chatbot signal (20%).",
        },
    ]

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
            "model": "gemini-1.5-flash",
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
        "modules": modules,
        "providers": providers,
        "environment": {
            "timeoutSeconds": LLM_TIMEOUT_SECONDS,
            "maxRetries": LLM_MAX_RETRIES,
        },
    }


def _read_all_logs() -> List[Dict[str, Any]]:
    """Reads all entries from llm_call_log.jsonl safely."""
    if not os.path.exists(LLM_CALL_LOG_PATH):
        return []
    
    logs = []
    try:
        with open(LLM_CALL_LOG_PATH, "r", encoding="utf-8") as f:
            for idx, line in enumerate(f):
                line = line.strip()
                if not line:
                    continue
                try:
                    data = json.loads(line)
                    if "id" not in data:
                        data["id"] = f"legacy-log-{idx}"
                    if "timestamp" not in data:
                        # Fallback timestamp for legacy entries without timestamp
                        data["timestamp"] = datetime.now(timezone.utc).isoformat()
                    if "status" not in data:
                        data["status"] = "SUCCESS"
                    logs.append(data)
                except Exception:
                    continue
    except Exception as e:
        logger.error(f"Error reading {LLM_CALL_LOG_PATH}: {e}")
    return logs


def get_llm_statistics() -> Dict[str, Any]:
    """
    Computes aggregated LLM usage, cost estimates, latency metrics, and quota utilization.
    """
    logs = _read_all_logs()
    now = datetime.now(timezone.utc)
    today_date_str = now.strftime("%Y-%m-%d")
    one_week_ago = now - timedelta(days=7)

    total_calls = len(logs)
    today_calls = 0
    week_calls = 0
    failed_calls = 0

    provider_counts: Dict[str, int] = {}
    module_counts: Dict[str, int] = {}
    module_latencies: Dict[str, List[float]] = {}
    all_latencies: List[float] = []

    gemini_today_requests = 0
    GEMINI_DAILY_QUOTA = 250  # Gemini free tier daily RPM/RPD limit reference

    for entry in logs:
        ts_str = entry.get("timestamp", "")
        prov = entry.get("provider", "unknown").lower()
        task = entry.get("task_type", "general")
        lat = float(entry.get("latency_seconds", 0.0))
        status = entry.get("status", "SUCCESS")

        if status == "FAILED":
            failed_calls += 1

        all_latencies.append(lat)

        provider_counts[prov] = provider_counts.get(prov, 0) + 1
        module_counts[task] = module_counts.get(task, 0) + 1
        module_latencies.setdefault(task, []).append(lat)

        try:
            entry_dt = datetime.fromisoformat(ts_str.replace("Z", "+00:00"))
            if entry_dt.strftime("%Y-%m-%d") == today_date_str:
                today_calls += 1
                if prov == "gemini":
                    gemini_today_requests += 1
            if entry_dt >= one_week_ago:
                week_calls += 1
        except Exception:
            today_calls += 1
            week_calls += 1
            if prov == "gemini":
                gemini_today_requests += 1

    avg_latency = round(sum(all_latencies) / len(all_latencies), 2) if all_latencies else 0.0

    module_breakdown = []
    for mod, count in module_counts.items():
        lats = module_latencies.get(mod, [])
        mod_avg_lat = round(sum(lats) / len(lats), 2) if lats else 0.0
        module_breakdown.append({
            "module": mod,
            "callCount": count,
            "avgLatencySeconds": mod_avg_lat,
            "percentage": round((count / total_calls * 100), 1) if total_calls > 0 else 0.0,
        })

    provider_breakdown = []
    for p_name, count in provider_counts.items():
        provider_breakdown.append({
            "provider": p_name,
            "callCount": count,
            "percentage": round((count / total_calls * 100), 1) if total_calls > 0 else 0.0,
        })

    success_rate = round(((total_calls - failed_calls) / total_calls * 100), 1) if total_calls > 0 else 100.0

    return {
        "totalCalls": total_calls,
        "todayCalls": today_calls,
        "weekCalls": week_calls,
        "failedCalls": failed_calls,
        "successRate": success_rate,
        "avgLatencySeconds": avg_latency,
        "quota": {
            "provider": "gemini",
            "tier": "Free Tier",
            "dailyLimit": GEMINI_DAILY_QUOTA,
            "usedToday": gemini_today_requests,
            "remainingToday": max(0, GEMINI_DAILY_QUOTA - gemini_today_requests),
            "percentageUsed": round((gemini_today_requests / GEMINI_DAILY_QUOTA * 100), 1),
        },
        "providerDistribution": provider_breakdown,
        "moduleBreakdown": module_breakdown,
    }


def query_call_logs(
    query: Optional[str] = None,
    task_type: Optional[str] = None,
    status: Optional[str] = None,
    provider: Optional[str] = None,
    page: int = 1,
    size: int = 20,
) -> Dict[str, Any]:
    """
    Search and filter audit call logs with pagination.
    """
    logs = _read_all_logs()
    
    # Reverse chronological (newest first)
    logs.reverse()

    filtered = []
    q = (query or "").lower().strip()

    for item in logs:
        # Search query across prompt, response, id, task_type
        if q:
            p_text = str(item.get("prompt_preview", "")).lower()
            r_text = str(item.get("response_preview", "")).lower()
            t_type = str(item.get("task_type", "")).lower()
            p_prov = str(item.get("provider", "")).lower()
            l_id = str(item.get("id", "")).lower()
            if q not in p_text and q not in r_text and q not in t_type and q not in p_prov and q not in l_id:
                continue

        if task_type and task_type != "ALL":
            if item.get("task_type", "").lower() != task_type.lower():
                continue

        if status and status != "ALL":
            if item.get("status", "SUCCESS").upper() != status.upper():
                continue

        if provider and provider != "ALL":
            if item.get("provider", "").lower() != provider.lower():
                continue

        filtered.append(item)

    total_elements = len(filtered)
    start_idx = (page - 1) * size
    end_idx = start_idx + size
    page_items = filtered[start_idx:end_idx]

    return {
        "items": page_items,
        "totalElements": total_elements,
        "page": page,
        "size": size,
        "totalPages": (total_elements + size - 1) // size if size > 0 else 1,
    }
