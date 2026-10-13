"""
All request/response shapes for the AI engine API, matching API_CONTRACT.md.

Every model inherits CamelModel, which automatically converts between
Python's snake_case (used inside our code) and JSON camelCase (used by
Spring Boot and the frontend) - e.g. candidate_id <-> candidateId.
"""

from typing import Optional
from pydantic import BaseModel, ConfigDict, Field, field_validator
from pydantic.alias_generators import to_camel


class CamelModel(BaseModel):
    model_config = ConfigDict(alias_generator=to_camel, populate_by_name=True)


# ---- 1. JD Generator ----
class GenerateJDRequest(CamelModel):
    job_title: str
    required_skills: list[str] = []
    experience_level: str = "mid"


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
class MatchDetail(CamelModel):
    required_skill: str
    status: str  # "matched" | "missing"
    matched_with: Optional[str] = None
    match_type: Optional[str] = None  # "exact" | "alias" | "example" | "alternative" | "category" | "token" | "semantic" | "text_evidence"
    confidence: float = 1.0


class ScoreBreakdown(CamelModel):
    skill_score: int
    experience_score: Optional[int] = None


class MatchScoreRequest(CamelModel):
    resume_skills: list[str] = Field(default_factory=list, max_length=300)
    job_skills: list[str] = Field(default_factory=list, max_length=300)
    nice_to_have_skills: Optional[list[str]] = Field(default=None, max_length=300)
    resume_text: Optional[str] = Field(default=None, max_length=25000)
    candidate_experience_years: Optional[float] = None
    required_experience_years: Optional[float] = None

    @field_validator("resume_skills", "job_skills", "nice_to_have_skills", mode="after")
    @classmethod
    def validate_skill_lengths(cls, v: Optional[list[str]]) -> Optional[list[str]]:
        if v is None:
            return v
        for s in v:
            if s and len(s) > 120:
                raise ValueError(f"Skill entry exceeds maximum length of 120 characters: {s[:30]}...")
        return v


class MatchScoreResponse(CamelModel):
    match_score: int
    auto_action: str
    matched_skills: list[str]
    missing_skills: list[str]
    bonus_skills: list[str] = []
    match_details: list[MatchDetail] = []
    skill_coverage_percent: float = 0.0
    experience_match_percent: Optional[float] = None
    score_breakdown: ScoreBreakdown
    rationale: str = ""
    engine_version: str = "match-v2"
    semantic_model_available: bool = True


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
