import pytest
from fastapi.testclient import TestClient
from main import app
from app.config import INTERNAL_API_KEY

client = TestClient(app)

def test_non_admin_endpoints_reject_missing_internal_key():
    """
    Every /api/v1 non-admin endpoint must reject requests missing the X-Internal-Key header with 401.
    """
    # 1. JD Generator
    res = client.post("/api/v1/jd/generate", json={"jobTitle": "Backend Engineer", "requiredSkills": ["Java"]})
    assert res.status_code == 401
    assert "Invalid or missing internal API key" in res.json().get("message", "")

    # 2. Resume Parser
    res = client.post("/api/v1/resumes/parse", json={"resumeText": "Experienced engineer", "candidateId": "c1"})
    assert res.status_code == 401
    assert "Invalid or missing internal API key" in res.json().get("message", "")

    # 3. Resume Job Status
    res = client.get("/api/v1/resumes/status/job-test-123")
    assert res.status_code == 401
    assert "Invalid or missing internal API key" in res.json().get("message", "")

    # 4. Match Score Engine
    res = client.post("/api/v1/match/score", json={"resumeSkills": ["Python"], "jobSkills": ["Python", "FastAPI"]})
    assert res.status_code == 401
    assert "Invalid or missing internal API key" in res.json().get("message", "")

    # 5. AI Chatbot
    res = client.post("/api/v1/chatbot/message", json={"conversationHistory": [], "newMessage": "Hello"})
    assert res.status_code == 401
    assert "Invalid or missing internal API key" in res.json().get("message", "")

    # 6. Interview Questions
    res = client.post("/api/v1/interview/questions", json={"skills": ["Python"]})
    assert res.status_code == 401
    assert "Invalid or missing internal API key" in res.json().get("message", "")

    # 7. Interview Evaluate
    res = client.post("/api/v1/interview/evaluate", json={"answers": []})
    assert res.status_code == 401
    assert "Invalid or missing internal API key" in res.json().get("message", "")

    # 8. Decision Engine
    res = client.post("/api/v1/decision/finalize", json={"resumeScore": 80, "interviewScore": 85, "chatbotSignalScore": 90})
    assert res.status_code == 401
    assert "Invalid or missing internal API key" in res.json().get("message", "")


def test_non_admin_endpoints_reject_invalid_internal_key():
    """
    Requests with an incorrect X-Internal-Key must be rejected with 401.
    """
    bad_headers = {"X-Internal-Key": "unauthorized-client-key-attempt"}
    res = client.post(
        "/api/v1/match/score",
        json={"resumeSkills": ["Python"], "jobSkills": ["Python"]},
        headers=bad_headers,
    )
    assert res.status_code == 401
    assert "Invalid or missing internal API key" in res.json().get("message", "")


def test_non_admin_endpoints_accept_valid_internal_key():
    """
    Non-admin endpoints succeed when the legitimate X-Internal-Key header is supplied.
    """
    valid_headers = {"X-Internal-Key": INTERNAL_API_KEY}

    # Match score (deterministic math engine, does not require external LLM)
    res = client.post(
        "/api/v1/match/score",
        json={"resumeSkills": ["Python", "Docker"], "jobSkills": ["Python", "Docker", "AWS"]},
        headers=valid_headers,
    )
    assert res.status_code == 200
    data = res.json()
    assert "matchScore" in data
    assert "matchedSkills" in data

    # Decision engine (deterministic formula, does not require external LLM)
    res = client.post(
        "/api/v1/decision/finalize",
        json={"resumeScore": 85, "interviewScore": 90, "chatbotSignalScore": 80},
        headers=valid_headers,
    )
    assert res.status_code == 200
    data = res.json()
    assert "finalScore" in data
    assert "classification" in data


def test_public_health_and_root_endpoints_require_no_key():
    """
    The /health check and root / endpoints must remain public without requiring authentication.
    """
    res = client.get("/health")
    assert res.status_code == 200
    data = res.json()
    assert "status" in data

    res = client.get("/")
    assert res.status_code == 200
    data = res.json()
    assert data.get("service") == "HireAI AI Engine"
