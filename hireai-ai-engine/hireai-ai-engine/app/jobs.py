"""
Minimal background-job tracker used for slow modules (Resume Parser,
Chatbot) so Spring Boot never has to wait 90 seconds on an open HTTP
connection. Instead: start_job() returns a job_id immediately, the real
work runs on a separate thread, and Spring Boot polls get_job() until
status is "complete".

LIMITATION (know this): this dictionary lives in memory only. It resets
every time the server restarts, and won't work if you ever run more than
one copy of this service. Fine for a prototype - a real production version
would use Redis or a database table instead.
"""

import uuid
import threading

from app.logger import get_logger

logger = get_logger(__name__)

_jobs: dict[str, dict] = {}


def start_job(target, args: tuple) -> str:
    job_id = str(uuid.uuid4())
    _jobs[job_id] = {"status": "processing", "result": None}
    logger.info(f"Job {job_id} started")

    thread = threading.Thread(target=_run, args=(job_id, target, args))
    thread.start()

    return job_id


def _run(job_id: str, target, args: tuple) -> None:
    try:
        result = target(*args)
        _jobs[job_id] = {"status": "complete", "result": result}
        logger.info(f"Job {job_id} completed successfully")
    except Exception as e:
        _jobs[job_id] = {"status": "failed", "result": {"error": str(e)}}
        logger.error(f"Job {job_id} failed: {e}")


def get_job(job_id: str) -> dict | None:
    return _jobs.get(job_id)
