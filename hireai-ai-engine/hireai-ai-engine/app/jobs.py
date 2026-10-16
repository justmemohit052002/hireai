"""
Background-job tracker used for slow modules (Resume Parser, Chatbot).
Uses a bounded ThreadPoolExecutor and thread-safe bounded LRU-style retention
to prevent unbounded thread growth and memory leaks under production traffic.
"""

import uuid
import threading
from collections import OrderedDict
from concurrent.futures import ThreadPoolExecutor

from app.logger import get_logger

logger = get_logger(__name__)

MAX_JOBS_STORED = 1000
_jobs_lock = threading.Lock()
_jobs: OrderedDict[str, dict] = OrderedDict()
_executor = ThreadPoolExecutor(max_workers=8, thread_name_prefix="ai-job-worker")


def start_job(target, args: tuple) -> str:
    job_id = str(uuid.uuid4())
    with _jobs_lock:
        if len(_jobs) >= MAX_JOBS_STORED:
            # Evict oldest job
            _jobs.popitem(last=False)
        _jobs[job_id] = {"status": "processing", "result": None}
    logger.info(f"Job {job_id} scheduled on worker pool")

    _executor.submit(_run, job_id, target, args)
    return job_id


def _run(job_id: str, target, args: tuple) -> None:
    try:
        result = target(*args)
        with _jobs_lock:
            if job_id in _jobs:
                _jobs[job_id] = {"status": "complete", "result": result}
        logger.info(f"Job {job_id} completed successfully")
    except Exception as e:
        with _jobs_lock:
            if job_id in _jobs:
                _jobs[job_id] = {"status": "failed", "result": {"error": str(e)}}
        logger.error(f"Job {job_id} failed: {e}")


def get_job(job_id: str) -> dict | None:
    with _jobs_lock:
        job = _jobs.get(job_id)
        return dict(job) if job is not None else None
