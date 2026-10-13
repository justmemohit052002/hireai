"""
HireAI AI Engine - main entry point.

Run with: uvicorn main:app --reload
Docs at:  http://localhost:8000/docs

Architecture (agreed with backend team):
  Frontend -> Backend (Spring Boot) -> THIS SERVICE -> Backend -> PostgreSQL
This service never touches the database directly.

Sync vs async:
  - JD Generator, Match Engine, Decision Engine, Interview Questions/Eval,
    Chatbot: fast enough to respond directly (synchronous).
  - Resume Parser: slow on local Ollama (30-90+ seconds), so it uses the
    polling pattern - returns a jobId immediately, work happens on a
    background thread, caller polls /status/{job_id} until done.
"""

from typing import Optional, List, Dict, Any
from fastapi import FastAPI, HTTPException, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse

from app.config import CORS_ALLOWED_ORIGINS, LLM_PROVIDER, OLLAMA_MODEL, SHOW_ERROR_DETAILS
from app.logger import get_logger
from app.llm_client import LLMCallError, check_ollama_reachable
from app.models import (
    GenerateJDRequest, GenerateJDResponse,
    ParseResumeRequest, JobAcceptedResponse, JobStatusResponse,
    MatchScoreRequest, MatchScoreResponse,
    ChatMessageRequest, ChatMessageResponse,
    InterviewQuestionsRequest, InterviewQuestionsResponse,
    InterviewEvaluateRequest, InterviewEvaluateResponse,
    DecisionRequest, DecisionResponse,
)
from app import jobs
from app.jd_generator import generate_jd
from app.resume_parser import parse_resume
from app.match_engine import calculate_match_score
from app.chatbot import handle_message
from app.interview_ai import generate_questions, evaluate_answers
from app.decision_engine import finalize_decision

logger = get_logger(__name__)

app = FastAPI(
    title="HireAI AI Engine",
    version="1.0",
    description="Standalone AI processing service for the HireAI recruitment platform.",
)

# ---- CORS ----
# Allows the frontend team to call this service directly during testing,
# even though the real production flow routes through Spring Boot.
app.add_middleware(
    CORSMiddleware,
    allow_origins=CORS_ALLOWED_ORIGINS,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


# ---- Global error handling ----
# Every error, regardless of where it happens, comes back in the same
# shape (matches the error contract in API_CONTRACT.md), instead of each
# endpoint inventing its own error format.
@app.exception_handler(LLMCallError)
def handle_llm_error(request: Request, exc: LLMCallError):
    logger.error(f"LLM call error on {request.url.path}: {exc}", exc_info=True)
    msg = str(exc) if SHOW_ERROR_DETAILS else "The AI service is temporarily unavailable. Please try again later."
    return JSONResponse(
        status_code=502,
        content={"error": True, "errorCode": "LLM_CALL_FAILED", "message": msg},
    )


@app.exception_handler(ValueError)
def handle_value_error(request: Request, exc: ValueError):
    logger.error(f"Invalid LLM response or parameter on {request.url.path}: {exc}", exc_info=True)
    msg = str(exc) if SHOW_ERROR_DETAILS else "Failed to parse or process AI response. Please try again."
    return JSONResponse(
        status_code=502,
        content={"error": True, "errorCode": "LLM_RESPONSE_INVALID", "message": msg},
    )


@app.exception_handler(HTTPException)
def handle_http_exception(request: Request, exc: HTTPException):
    logger.warning(f"HTTP {exc.status_code} on {request.url.path}: {exc.detail}")
    return JSONResponse(
        status_code=exc.status_code,
        content={"error": True, "errorCode": f"HTTP_{exc.status_code}", "message": str(exc.detail)},
    )


@app.exception_handler(Exception)
def handle_unexpected_error(request: Request, exc: Exception):
    logger.error(f"Unexpected error on {request.url.path}: {exc}", exc_info=True)
    msg = str(exc) if SHOW_ERROR_DETAILS else "An unexpected internal error occurred. Please try again later."
    return JSONResponse(
        status_code=500,
        content={"error": True, "errorCode": "INTERNAL_ERROR", "message": msg},
    )


import secrets
from fastapi import Depends, Header, status
from app.config import INTERNAL_API_KEY, APP_ENV
from app.context import set_request_context, clear_request_context
from app import admin_service

# ---- Request Correlation Middleware ----
@app.middleware("http")
async def correlation_middleware(request: Request, call_next):
    request_id = request.headers.get("X-Request-Id")
    candidate_ref = request.headers.get("X-Candidate-Ref")
    set_request_context(request_id=request_id, candidate_ref=candidate_ref)
    try:
        response = await call_next(request)
        if request_id:
            response.headers["X-Request-Id"] = request_id
        return response
    finally:
        clear_request_context()


# ---- Security Dependency for Admin APIs ----
def verify_internal_key(x_internal_key: Optional[str] = Header(None, alias="X-Internal-Key")):
    """
    Authenticates internal administrative requests via constant-time comparison.
    Fails closed if the key is missing or unset.
    """
    if not INTERNAL_API_KEY:
        logger.error("INTERNAL_API_KEY is not configured on server. Admin requests are denied.")
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid or missing internal API key",
        )

    if not x_internal_key or not secrets.compare_digest(str(x_internal_key), str(INTERNAL_API_KEY)):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid or missing internal API key",
        )


# ---- Health check ----
@app.get("/health")
def health_check():
    ollama_ok = check_ollama_reachable()
    return {
        "status": "ok" if ollama_ok or LLM_PROVIDER != "ollama" else "degraded",
        "llmProvider": LLM_PROVIDER,
        "ollamaModel": OLLAMA_MODEL if LLM_PROVIDER == "ollama" else None,
        "ollamaReachable": ollama_ok,
    }


# ---- Admin APIs (Secured with X-Internal-Key) ----
@app.get("/api/v1/admin/health-summary", dependencies=[Depends(verify_internal_key)])
def admin_health_summary():
    return admin_service.get_system_health()


@app.get("/api/v1/admin/stats", dependencies=[Depends(verify_internal_key)])
def admin_stats(range: str = "all"):
    return admin_service.get_llm_statistics(range_param=range)


@app.get("/api/v1/admin/logs", dependencies=[Depends(verify_internal_key)])
def admin_call_logs(
    query: Optional[str] = None,
    task_type: Optional[str] = None,
    module: Optional[str] = None,
    status: Optional[str] = None,
    provider: Optional[str] = None,
    from_date: Optional[str] = None,
    to_date: Optional[str] = None,
    correlation_id: Optional[str] = None,
    candidate_ref: Optional[str] = None,
    page: int = 1,
    size: int = 20,
):
    return admin_service.query_call_logs(
        query=query,
        task_type=task_type,
        module=module,
        status=status,
        provider=provider,
        from_date=from_date,
        to_date=to_date,
        correlation_id=correlation_id,
        candidate_ref=candidate_ref,
        page=page,
        size=size,
    )


@app.get("/api/v1/admin/logs/{log_id}", dependencies=[Depends(verify_internal_key)])
def admin_get_log(log_id: str):
    entry = admin_service.get_single_log(log_id)
    if not entry:
        raise HTTPException(status_code=404, detail=f"Log entry {log_id} not found")
    return entry


@app.get("/")
def root():
    return {
        "service": "HireAI AI Engine",
        "docs": "/docs",
        "health": "/health",
        "adminHealth": "/api/v1/admin/health-summary",
    }


# ---- 1. JD Generator (sync) ----
@app.post("/api/v1/jd/generate", response_model=GenerateJDResponse)
def jd_generate(request: GenerateJDRequest):
    logger.info(f"JD generation requested for '{request.job_title}'")
    result = generate_jd(request.job_title, request.required_skills, request.experience_level)
    return GenerateJDResponse(**result)


# ---- 2. Resume Parser (async / polling) ----
@app.post("/api/v1/resumes/parse", response_model=JobAcceptedResponse)
def resumes_parse(request: ParseResumeRequest):
    def job_target(resume_text: str, candidate_id: str) -> dict:
        result = parse_resume(resume_text)
        result["candidateId"] = candidate_id
        return result

    job_id = jobs.start_job(job_target, (request.resume_text, request.candidate_id))
    logger.info(f"Resume parse job {job_id} started for candidate {request.candidate_id}")
    return JobAcceptedResponse(job_id=job_id, status="processing")


@app.get("/api/v1/resumes/status/{job_id}", response_model=JobStatusResponse)
def resumes_status(job_id: str):
    job = jobs.get_job(job_id)
    if job is None:
        raise HTTPException(status_code=404, detail=f"Job {job_id} not found")
    return JobStatusResponse(job_id=job_id, status=job["status"], result=job["result"])


# ---- 3. Match Engine (sync, no LLM) ----
@app.post("/api/v1/match/score", response_model=MatchScoreResponse)
def match_score(request: MatchScoreRequest):
    result = calculate_match_score(request.resume_skills, request.job_skills)
    return MatchScoreResponse(**result)


# ---- 4. AI Chatbot (sync) ----
@app.post("/api/v1/chatbot/message", response_model=ChatMessageResponse)
def chatbot_message(request: ChatMessageRequest):
    history = [{"role": t.role, "text": t.text} for t in request.conversation_history]
    result = handle_message(history, request.new_message)
    return ChatMessageResponse(**result)


# ---- 5. Interview AI (sync) ----
@app.post("/api/v1/interview/questions", response_model=InterviewQuestionsResponse)
def interview_questions(request: InterviewQuestionsRequest):
    result = generate_questions(request.skills)
    return InterviewQuestionsResponse(**result)


@app.post("/api/v1/interview/evaluate", response_model=InterviewEvaluateResponse)
def interview_evaluate(request: InterviewEvaluateRequest):
    answers = [{"question_id": a.question_id, "answer_text": a.answer_text} for a in request.answers]
    result = evaluate_answers(answers)
    return InterviewEvaluateResponse(**result)


# ---- 6. Decision Engine (sync, no LLM) ----
@app.post("/api/v1/decision/finalize", response_model=DecisionResponse)
def decision_finalize(request: DecisionRequest):
    result = finalize_decision(request.resume_score, request.interview_score, request.chatbot_signal_score)
    return DecisionResponse(**result)


if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)

