"""
Request-scoped context variables for tracking correlation IDs (X-Request-Id, X-Candidate-Ref)
across threads and async execution.
"""

from contextvars import ContextVar
from typing import Optional

_request_id_ctx: ContextVar[Optional[str]] = ContextVar("request_id", default=None)
_candidate_ref_ctx: ContextVar[Optional[str]] = ContextVar("candidate_ref", default=None)


def set_request_context(request_id: Optional[str] = None, candidate_ref: Optional[str] = None) -> None:
    """Sets correlation IDs for the current request scope."""
    _request_id_ctx.set(request_id)
    _candidate_ref_ctx.set(candidate_ref)


def get_current_request_id() -> Optional[str]:
    """Retrieves current request ID if present."""
    return _request_id_ctx.get()


def get_current_candidate_ref() -> Optional[str]:
    """Retrieves current candidate reference ID if present."""
    return _candidate_ref_ctx.get()


def clear_request_context() -> None:
    """Clears correlation IDs."""
    _request_id_ctx.set(None)
    _candidate_ref_ctx.set(None)
