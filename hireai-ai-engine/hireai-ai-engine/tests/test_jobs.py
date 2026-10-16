import time
from app.jobs import start_job, get_job, _jobs, _jobs_lock, MAX_JOBS_STORED


def test_job_execution_success():
    def dummy_task(x, y):
        return {"sum": x + y}

    job_id = start_job(dummy_task, (3, 7))
    assert job_id is not None

    # Wait for completion
    for _ in range(50):
        info = get_job(job_id)
        if info and info["status"] == "complete":
            break
        time.sleep(0.05)

    info = get_job(job_id)
    assert info is not None
    assert info["status"] == "complete"
    assert info["result"] == {"sum": 10}


def test_job_execution_failure():
    def failing_task():
        raise ValueError("Simulated worker error")

    job_id = start_job(failing_task, ())
    for _ in range(50):
        info = get_job(job_id)
        if info and info["status"] == "failed":
            break
        time.sleep(0.05)

    info = get_job(job_id)
    assert info is not None
    assert info["status"] == "failed"
    assert "Simulated worker error" in info["result"]["error"]


def test_job_retention_capacity():
    with _jobs_lock:
        initial_count = len(_jobs)

    def quick_task():
        return True

    job_id = start_job(quick_task, ())
    assert get_job(job_id) is not None
