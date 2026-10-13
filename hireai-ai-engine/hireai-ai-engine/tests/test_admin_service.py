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
    # Long key > 12 chars
    long_key = "AIzaSyD1234567890abcdef"
    masked = mask_key(long_key)
    assert masked == "...cdef"

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

