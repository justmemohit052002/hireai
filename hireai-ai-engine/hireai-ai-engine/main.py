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

from fastapi import FastAPI, HTTPException, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse

from app.config import CORS_ALLOWED_ORIGINS, LLM_PROVIDER, OLLAMA_MODEL
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
    logger.error(f"LLM call error on {request.url.path}: {exc}")
    return JSONResponse(
        status_code=502,
        content={"error": True, "errorCode": "LLM_CALL_FAILED", "message": str(exc)},
    )


@app.exception_handler(ValueError)
def handle_value_error(request: Request, exc: ValueError):
    logger.error(f"Invalid LLM response on {request.url.path}: {exc}")
    return JSONResponse(
        status_code=502,
        content={"error": True, "errorCode": "LLM_RESPONSE_INVALID", "message": str(exc)},
    )


@app.exception_handler(Exception)
def handle_unexpected_error(request: Request, exc: Exception):
    logger.error(f"Unexpected error on {request.url.path}: {exc}")
    return JSONResponse(
        status_code=500,
        content={"error": True, "errorCode": "INTERNAL_ERROR", "message": str(exc)},
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


@app.get("/")
def root():
    return {
        "service": "HireAI AI Engine",
        "docs": "/docs",
        "health": "/health",
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
