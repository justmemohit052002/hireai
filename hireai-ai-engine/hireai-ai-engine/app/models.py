"""
All request/response shapes for the AI engine API, matching API_CONTRACT.md.

Every model inherits CamelModel, which automatically converts between
Python's snake_case (used inside our code) and JSON camelCase (used by
Spring Boot and the frontend) - e.g. candidate_id <-> candidateId.
"""

from pydantic import BaseModel, ConfigDict
from pydantic.alias_generators import to_camel


class CamelModel(BaseModel):
    model_config = ConfigDict(alias_generator=to_camel, populate_by_name=True)


# ---- 1. JD Generator ----
class GenerateJDRequest(CamelModel):
    job_title: str
    required_skills: list[str]
    experience_level: str


class GenerateJDResponse(CamelModel):
    description: str
    responsibilities: list[str]
    must_have_skills: list[str]
    nice_to_have_skills: list[str]
    interview_questions: list[str]


# ---- 2. Resume Parser (async/polling) ----
class ParseResumeRequest(CamelModel):
    candidate_id: str
    resume_text: str


class JobAcceptedResponse(CamelModel):
    job_id: str
    status: str


class JobStatusResponse(CamelModel):
    job_id: str
    status: str
    result: dict | None = None


# ---- 3. Match Engine ----
class MatchScoreRequest(CamelModel):
    resume_skills: list[str]
    job_skills: list[str]


class MatchScoreResponse(CamelModel):
    match_score: int
    matched_skills: list[str]
    missing_skills: list[str]
    auto_action: str


# ---- 4. AI Chatbot ----
class ChatTurn(CamelModel):
    role: str
    text: str


class ChatMessageRequest(CamelModel):
    candidate_id: str
    conversation_history: list[ChatTurn]
    new_message: str


class ChatMessageResponse(CamelModel):
    bot_reply: str
    extracted_fields: dict
    conversation_complete: bool


# ---- 5. Interview AI ----
class InterviewQuestionsRequest(CamelModel):
    job_id: str
    skills: list[str]


class InterviewQuestionsResponse(CamelModel):
    questions: list[dict]


class InterviewAnswer(CamelModel):
    question_id: str
    answer_text: str


class InterviewEvaluateRequest(CamelModel):
    candidate_id: str
    answers: list[InterviewAnswer]


class InterviewEvaluateResponse(CamelModel):
    interview_score: int
    evaluated_answers: list[dict]


# ---- 6. Decision Engine ----
class DecisionRequest(CamelModel):
    resume_score: float
    interview_score: float
    chatbot_signal_score: float


class DecisionResponse(CamelModel):
    final_score: int
    classification: str
    breakdown: dict
    explanation: str
