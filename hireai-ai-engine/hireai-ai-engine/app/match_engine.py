"""
Module 3: Match Engine.
No LLM call - uses a small local embedding model (sentence-transformers)
to convert skill lists into vectors, then measures similarity. Instant,
free, no rate limits, runs forever locally.
"""

from sentence_transformers import SentenceTransformer, util
from app.logger import get_logger

logger = get_logger(__name__)

# Loaded once when the server starts, reused for every request.
logger.info("Loading embedding model 'all-MiniLM-L6-v2' for Match Engine...")
_model = SentenceTransformer("all-MiniLM-L6-v2")
logger.info("Match Engine embedding model ready.")


def calculate_match_score(resume_skills: list[str], job_skills: list[str]) -> dict:
    resume_text = ", ".join(resume_skills) if resume_skills else "no skills listed"
    job_text = ", ".join(job_skills) if job_skills else "no requirements listed"

    resume_embedding = _model.encode(resume_text, convert_to_tensor=True)
    job_embedding = _model.encode(job_text, convert_to_tensor=True)

    similarity = util.cos_sim(resume_embedding, job_embedding).item()
    score = max(0, min(100, round(similarity * 100)))

    resume_lower = [s.lower() for s in resume_skills]
    job_lower = [s.lower() for s in job_skills]

    matched = [s for s in job_skills if s.lower() in resume_lower]
    missing = [s for s in job_skills if s.lower() not in resume_lower]

    if score >= 80:
        auto_action = "shortlist"
    elif score < 40:
        auto_action = "reject"
    else:
        auto_action = "review"

    return {
        "match_score": score,
        "matched_skills": matched,
        "missing_skills": missing,
        "auto_action": auto_action,
    }
