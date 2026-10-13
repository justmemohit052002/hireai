import sys
import os

os.environ.setdefault("APP_ENV", "development")
os.environ.setdefault("INTERNAL_API_KEY", "test-internal-api-key-secure-2026")

# Add project root to sys.path
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

