import pytest
from fastapi.testclient import TestClient
from main import app
from app.config import INTERNAL_API_KEY

client = TestClient(app)

def test_admin_endpoints_require_internal_key():
    # 1. /api/v1/admin/health-summary without key -> 401
    res = client.get("/api/v1/admin/health-summary")
    assert res.status_code == 401
    assert "Invalid or missing internal API key" in res.json().get("message", "")

    # 2. /api/v1/admin/stats without key -> 401
    res = client.get("/api/v1/admin/stats")
    assert res.status_code == 401

    # 3. /api/v1/admin/logs without key -> 401
    res = client.get("/api/v1/admin/logs")
    assert res.status_code == 401

def test_admin_endpoints_reject_invalid_key():
    headers = {"X-Internal-Key": "wrong-secret-key"}
    res = client.get("/api/v1/admin/health-summary", headers=headers)
    assert res.status_code == 401
    assert "Invalid or missing internal API key" in res.json().get("message", "")

def test_admin_endpoints_accept_valid_key():
    headers = {"X-Internal-Key": INTERNAL_API_KEY}
    res = client.get("/api/v1/admin/health-summary", headers=headers)
    assert res.status_code == 200
    data = res.json()
    assert "overallStatus" in data
    assert "modules" in data
    assert isinstance(data["modules"], list)

def test_admin_stats_with_valid_key():
    headers = {"X-Internal-Key": INTERNAL_API_KEY}
    res = client.get("/api/v1/admin/stats?range=today", headers=headers)
    assert res.status_code == 200
    data = res.json()
    assert "totalCalls" in data
    assert "quota" in data
    assert "providerDistribution" in data
    assert "moduleBreakdown" in data
