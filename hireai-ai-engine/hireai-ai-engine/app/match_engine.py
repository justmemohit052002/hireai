"""
Module 3: Match Engine.
No LLM call - uses a small local embedding model (sentence-transformers)
to convert skill lists into vectors, then measures similarity. Instant,
free, no rate limits, runs forever locally.
"""

import os
import logging
os.environ["TRANSFORMERS_VERBOSITY"] = "error"
os.environ["TOKENIZERS_PARALLELISM"] = "false"
logging.getLogger("transformers").setLevel(logging.ERROR)

from sentence_transformers import SentenceTransformer, util
import transformers
transformers.logging.set_verbosity_error()

from app.logger import get_logger

logger = get_logger(__name__)

# Loaded once when the server starts, reused for every request.
logger.info("Loading embedding model 'all-MiniLM-L6-v2' for Match Engine...")
_model = SentenceTransformer("all-MiniLM-L6-v2")
logger.info("Match Engine embedding model ready.")


def calculate_match_score(resume_skills: list[str], job_skills: list[str]) -> dict:
    if not job_skills:
        return {
            "match_score": 0,
            "matched_skills": [],
            "missing_skills": [],
            "bonus_skills": list(resume_skills),
            "auto_action": "review",
        }

    if not resume_skills:
        return {
            "match_score": 0,
            "matched_skills": [],
            "missing_skills": list(job_skills),
            "bonus_skills": [],
            "auto_action": "reject",
        }

    resume_clean = [s.strip() for s in resume_skills if s.strip()]
    job_clean = [s.strip() for s in job_skills if s.strip()]

    matched = []
    missing = []
    matched_resume_indices = set()

    for j_orig in job_clean:
        j_lower = j_orig.lower()
        j_norm = j_lower.replace(" ", "").replace("-", "").replace(".", "")
        matched_found = False

        for idx, r_orig in enumerate(resume_clean):
            r_lower = r_orig.lower()
            r_norm = r_lower.replace(" ", "").replace("-", "").replace(".", "")

            if (
                j_lower == r_lower
                or j_norm == r_norm
                or j_lower in r_lower
                or r_lower in j_lower
                or (len(j_norm) > 2 and j_norm in r_norm)
                or (len(r_norm) > 2 and r_norm in j_norm)
            ):
                matched_found = True
                matched_resume_indices.add(idx)
                break

        if matched_found:
            matched.append(j_orig)
        else:
            missing.append(j_orig)

    # Extra skills candidate possesses beyond the job requirements
    bonus_skills = [
        resume_clean[i] for i in range(len(resume_clean)) if i not in matched_resume_indices
    ]

    coverage_ratio = len(matched) / len(job_clean)

    # If candidate has ALL required skills (plus any number of extra skills), score is 100%!
    if len(missing) == 0:
        score = 100
    else:
        resume_text = ", ".join(resume_clean)
        job_text = ", ".join(job_clean)

        resume_embedding = _model.encode(resume_text, convert_to_tensor=True)
        job_embedding = _model.encode(job_text, convert_to_tensor=True)
        similarity = max(0.0, min(1.0, util.cos_sim(resume_embedding, job_embedding).item()))

        # 70% based on direct requirement coverage + 30% semantic vector similarity
        raw_score = (coverage_ratio * 70) + (similarity * 30)

        # Bonus reward: extra skills can give up to +5 points on partial match
        if bonus_skills:
            raw_score += min(5, len(bonus_skills))

        score = max(0, min(100, round(raw_score)))

    if score >= 75:
        auto_action = "shortlist"
    elif score < 40:
        auto_action = "reject"
    else:
        auto_action = "review"

    return {
        "match_score": score,
        "matched_skills": matched,
        "missing_skills": missing,
        "bonus_skills": bonus_skills,
        "auto_action": auto_action,
    }
