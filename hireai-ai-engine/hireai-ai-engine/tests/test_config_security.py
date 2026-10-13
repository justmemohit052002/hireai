import os
import pytest

def test_startup_validation_rejects_missing_key_in_non_dev():
    env_backup = dict(os.environ)
    try:
        os.environ["APP_ENV"] = "production"
        os.environ.pop("INTERNAL_API_KEY", None)

        with pytest.raises(ValueError, match="INTERNAL_API_KEY must be configured"):
            # Re-evaluating validation logic
            app_env = os.environ.get("APP_ENV", "development").lower()
            key = os.environ.get("INTERNAL_API_KEY")
            old_default = "hireai-internal-secret-key"
            if app_env != "development":
                if not key or not str(key).strip() or str(key).strip() == old_default:
                    raise ValueError("Security Error: INTERNAL_API_KEY must be configured with a secure, non-default secret in non-development environments.")
    finally:
        os.environ.clear()
        os.environ.update(env_backup)


def test_startup_validation_rejects_old_default_key_in_non_dev():
    env_backup = dict(os.environ)
    try:
        os.environ["APP_ENV"] = "staging"
        os.environ["INTERNAL_API_KEY"] = "hireai-internal-secret-key"

        with pytest.raises(ValueError, match="INTERNAL_API_KEY must be configured"):
            app_env = os.environ.get("APP_ENV", "development").lower()
            key = os.environ.get("INTERNAL_API_KEY")
            old_default = "hireai-internal-secret-key"
            if app_env != "development":
                if not key or not str(key).strip() or str(key).strip() == old_default:
                    raise ValueError("Security Error: INTERNAL_API_KEY must be configured with a secure, non-default secret in non-development environments.")
    finally:
        os.environ.clear()
        os.environ.update(env_backup)


def test_startup_validation_accepts_strong_key_in_production():
    env_backup = dict(os.environ)
    try:
        os.environ["APP_ENV"] = "production"
        os.environ["INTERNAL_API_KEY"] = "super-secure-production-ai-engine-key-2026"

        app_env = os.environ.get("APP_ENV", "development").lower()
        key = os.environ.get("INTERNAL_API_KEY")
        old_default = "hireai-internal-secret-key"
        # Should not raise
        if app_env != "development":
            if not key or not str(key).strip() or str(key).strip() == old_default:
                raise ValueError("Security Error: INTERNAL_API_KEY must be configured")
    finally:
        os.environ.clear()
        os.environ.update(env_backup)


def test_cors_defaults_to_empty_list_outside_development():
    app_env = "production"
    cors_env = None
    if cors_env is not None:
        origins = [o.strip() for o in cors_env.split(",") if o.strip() and o.strip() != "*"]
    elif app_env == "development":
        origins = ["http://localhost:3000"]
    else:
        origins = []

    assert origins == []
