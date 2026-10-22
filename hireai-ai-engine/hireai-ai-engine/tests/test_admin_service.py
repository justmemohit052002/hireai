import json
import os
import tempfile
import pytest
from app.admin_service import JsonlLogSource, get_llm_statistics, query_call_logs, get_single_log
from app.llm_client import sanitize_error_message, mask_key

def test_sanitize_error_message():
    # Test stripping of API keys and bearer tokens
    err1 = "Failed to call Gemini with key AIzaSyD1234567890abcdef and token Bearer eyJhbGciOi"
    sanitized1 = sanitize_error_message(err1)
    assert "AIza" not in sanitized1
    assert "Bearer" not in sanitized1
    assert "[REDACTED_SECRET]" in sanitized1 or "[REDACTED_BEARER]" in sanitized1

    # Test length truncation at 500 chars
    long_err = "Error: " + ("x" * 600)
    sanitized_long = sanitize_error_message(long_err)
    assert len(sanitized_long) <= 500
    assert sanitized_long.endswith("...")

def test_mask_key():
    # Long key > 12 chars: returns Configured without exposing partial characters
    long_key = "AIzaSyD1234567890abcdef"
    masked = mask_key(long_key)
    assert masked == "Configured"

    # Short key <= 12 chars
    short_key = "12345678"
    assert mask_key(short_key) == "Configured"

    # Empty key
    assert mask_key("") == "Not Configured"
    assert mask_key(None) == "Not Configured"

def test_jsonl_log_source_with_malformed_and_missing_timestamps():
    with tempfile.NamedTemporaryFile(mode="w", delete=False, suffix=".jsonl") as f:
        # Line 1: Normal line
        f.write(json.dumps({
            "id": "log-1",
            "timestamp": "2026-10-10T12:00:00+05:30",
            "task_type": "jd_generation",
            "provider": "ollama",
            "model": "llama3.2:3b",
            "status": "SUCCESS",
            "latency_seconds": 1.25,
            "prompt_tokens": 100,
            "completion_tokens": 50,
            "total_tokens": 150,
            "prompt_preview": "Generate JD",
            "response_preview": "JD Response",
            "error_message": None
        }) + "\n")
        
        # Line 2: Corrupt / malformed JSON
        f.write("{this is not valid json}\n")
        
        # Line 3: Missing timestamp
        f.write(json.dumps({
            "id": "log-2",
            "task_type": "resume_parsing",
            "provider": "gemini",
            "model": "gemini-2.5-flash",
            "status": "FAILED",
            "latency_seconds": 2.5,
            "prompt_tokens": 500,
            "completion_tokens": 0,
            "total_tokens": 500,
            "prompt_preview": "Parse Resume",
            "response_preview": "",
            "error_message": "Rate limit exceeded"
        }) + "\n")
        
        # Line 4: Invalid timestamp string
        f.write(json.dumps({
            "id": "log-3",
            "timestamp": "invalid-timestamp",
            "task_type": "interview_questions",
            "provider": "groq",
            "model": "llama-3.3-70b-versatile",
            "status": "SUCCESS",
            "latency_seconds": 0.8,
            "prompt_tokens": 200,
            "completion_tokens": 100,
            "total_tokens": 300,
            "prompt_preview": "Interview questions",
            "response_preview": "Questions",
            "error_message": None
        }) + "\n")
        
        temp_path = f.name

    try:
        source = JsonlLogSource(temp_path)
        entries = list(source.stream_entries())
        
        # Should parse 3 valid json entries, skipping the corrupt line
        assert len(entries) == 3
        assert entries[0]["id"] == "log-1"
        assert entries[1]["id"] == "log-2"
        assert entries[2]["id"] == "log-3"
    finally:
        if os.path.exists(temp_path):
            os.remove(temp_path)


def test_get_system_health_structure():
    from app.admin_service import get_system_health
    health = get_system_health()

    assert health["service"] == "HireAI AI Engine"
    assert "overallStatus" in health
    assert "checkedAt" in health
    assert len(health["modules"]) == 6

    expected_ids = {"jd_generator", "resume_parser", "match_engine", "chatbot", "interview_ai", "decision_engine"}
    found_ids = {m["id"] for m in health["modules"]}
    assert found_ids == expected_ids

    for m in health["modules"]:
        assert "taskType" in m
        assert "status" in m
        assert "error" in m
        assert m["status"] in ("HEALTHY", "DEGRADED", "DOWN")


def test_get_system_health_provider_degradation(monkeypatch):
    from app import admin_service

    # Simulate Ollama being unreachable
    monkeypatch.setattr(admin_service, "check_ollama_reachable", lambda: False)
    monkeypatch.setattr(admin_service, "get_active_provider_info", lambda: ("ollama", "llama3.2:3b"))
    monkeypatch.setattr(admin_service, "GEMINI_API_KEY", "")
    monkeypatch.setattr(admin_service, "GROQ_API_KEY", "")

    health = admin_service.get_system_health()
    assert health["overallStatus"] in ("DEGRADED", "DOWN")

    modules_by_id = {m["id"]: m for m in health["modules"]}

    # Non-LLM deterministic modules stay HEALTHY
    assert modules_by_id["match_engine"]["status"] == "HEALTHY"
    assert modules_by_id["decision_engine"]["status"] == "HEALTHY"

    # LLM modules show DEGRADED or DOWN with error reason
    assert modules_by_id["jd_generator"]["status"] in ("DEGRADED", "DOWN")
    assert "Ollama unreachable" in str(modules_by_id["jd_generator"]["error"])


def test_get_llm_statistics_empty_window(monkeypatch):
    from app import admin_service
    # Point log source to empty temporary file
    with tempfile.NamedTemporaryFile(mode="w", delete=False, suffix=".jsonl") as f:
        temp_path = f.name

    try:
        source = JsonlLogSource(temp_path)
        monkeypatch.setattr(admin_service, "log_source", source)
        monkeypatch.setattr(admin_service, "_stats_cache", {})
        monkeypatch.setattr(admin_service, "_stats_cache_expiry", 0.0)

        stats = get_llm_statistics(scope="today")
        assert stats["totalCalls"] == 0
        assert stats["todayCalls"] == 0
        assert stats["avgLatencySeconds"] is None
        assert stats["p50LatencySeconds"] is None
        assert stats["p95LatencySeconds"] is None
        assert stats["successRate"] is None
        assert stats["range"] == "today"
    finally:
        if os.path.exists(temp_path):
            os.remove(temp_path)


def test_get_llm_statistics_percentiles_and_window(monkeypatch):
    from datetime import datetime, timezone
    from app import admin_service

    now_iso = datetime.now(timezone.utc).isoformat()
    old_iso = "2020-01-01T00:00:00+00:00"

    with tempfile.NamedTemporaryFile(mode="w", delete=False, suffix=".jsonl") as f:
        # Write 10 calls with increasing latencies: 1.0, 2.0, ... 10.0
        for i in range(1, 11):
            f.write(json.dumps({
                "id": f"call-{i}",
                "timestamp": now_iso,
                "task_type": "jd_generation",
                "provider": "ollama",
                "status": "SUCCESS" if i <= 9 else "FAILED",
                "latency_seconds": float(i),
                "prompt_preview": "Test prompt",
                "response_preview": "Test response"
            }) + "\n")
        # Write 1 very old call that should not be in 'today' or '7d'
        f.write(json.dumps({
            "id": "old-call",
            "timestamp": old_iso,
            "task_type": "jd_generation",
            "provider": "ollama",
            "status": "SUCCESS",
            "latency_seconds": 99.0
        }) + "\n")
        temp_path = f.name

    try:
        source = JsonlLogSource(temp_path)
        monkeypatch.setattr(admin_service, "log_source", source)
        monkeypatch.setattr(admin_service, "_stats_cache", {})
        monkeypatch.setattr(admin_service, "_stats_cache_expiry", 0.0)

        # Scoped to 'today': 10 calls, old-call excluded
        stats = get_llm_statistics(scope="today")
        assert stats["totalCalls"] == 10
        # Avg of 1..10 = 55 / 10 = 5.5s
        assert stats["avgLatencySeconds"] == 5.5
        # p50 rank = ceil(10 * 0.50) = 5 -> value 5.0
        assert stats["p50LatencySeconds"] == 5.0
        # p95 rank = ceil(10 * 0.95) = 10 -> value 10.0
        assert stats["p95LatencySeconds"] == 10.0
        # 9 success out of 10 = 90.0%
        assert stats["successRate"] == 90.0

        # All-time scope: includes old-call (11 calls)
        monkeypatch.setattr(admin_service, "_stats_cache", {})
        monkeypatch.setattr(admin_service, "_stats_cache_expiry", 0.0)
        stats_all = get_llm_statistics(scope="all")
        assert stats_all["totalCalls"] == 11
    finally:
        if os.path.exists(temp_path):
            os.remove(temp_path)


def test_query_call_logs_preview_truncation_and_dual_keys(monkeypatch):
    from app import admin_service

    long_prompt = "P" * 500
    long_response = "R" * 500

    with tempfile.NamedTemporaryFile(mode="w", delete=False, suffix=".jsonl") as f:
        f.write(json.dumps({
            "id": "log-long",
            "timestamp": "2026-10-21T12:00:00Z",
            "task_type": "interview_ai",
            "provider": "ollama",
            "status": "SUCCESS",
            "latency_seconds": 1.5,
            "prompt_preview": long_prompt,
            "response_preview": long_response
        }) + "\n")
        temp_path = f.name

    try:
        source = JsonlLogSource(temp_path)
        monkeypatch.setattr(admin_service, "log_source", source)

        res = query_call_logs(page=1, size=10)
        assert res["totalElements"] == 1
        assert len(res["items"]) == 1

        item = res["items"][0]
        # Verify dual-case keys
        assert item["task_type"] == "interview_ai"
        assert item["taskType"] == "interview_ai"
        assert item["latency_seconds"] == 1.5
        assert item["latencySeconds"] == 1.5

        # Verify preview truncation to 200 chars max
        assert len(item["prompt_preview"]) <= 200
        assert item["prompt_preview"] == item["promptPreview"]
        assert len(item["response_preview"]) <= 200
        assert item["response_preview"] == item["responsePreview"]
    finally:
        if os.path.exists(temp_path):
            os.remove(temp_path)



