"""
Module 3: Match Engine v2.
Deterministic, multi-tier skill matching with semantic cosine similarity fallback.
Fast, explainable, zero LLM cost, robust against false positives and formatting variances.
"""

import os
import re
from typing import Optional, Any

from app.config import ATS_SHORTLIST_THRESHOLD, ATS_REJECT_THRESHOLD
from app.logger import get_logger
from app.skill_normalizer import (
    normalize_skill,
    decompose_requirement,
    repair_and_clean_job_skills,
    get_category_map,
    token_matches,
    extract_skills_from_text,
    find_skill_evidence,
)

logger = get_logger(__name__)

# Lazy-loaded embedding model reference
_embedding_model = None
_model_load_attempted = False
_model_available = False


def get_embedding_model():
    """
    Lazy-loads the sentence-transformers model on first use.
    If unavailable or fails to load, gracefully falls back to deterministic-only scoring.
    """
    global _embedding_model, _model_load_attempted, _model_available
    if not _model_load_attempted:
        _model_load_attempted = True
        try:
            os.environ["TRANSFORMERS_VERBOSITY"] = "error"
            os.environ["TOKENIZERS_PARALLELISM"] = "false"
            from sentence_transformers import SentenceTransformer
            logger.info("Initializing embedding model 'all-MiniLM-L6-v2' for Match Engine...")
            _embedding_model = SentenceTransformer("all-MiniLM-L6-v2")
            _model_available = True
            logger.info("Match Engine embedding model successfully initialized.")
        except Exception as ex:
            logger.warning(
                f"SentenceTransformer model unavailable ({ex}). Match Engine running in deterministic-only mode."
            )
            _embedding_model = None
            _model_available = False
    return _embedding_model


def is_semantic_model_available() -> bool:
    """Returns True if the sentence-transformers model is active."""
    get_embedding_model()
    return _model_available


def evaluate_single_requirement(
    req: str,
    resume_canonical_map: dict[str, str],
    clean_resume_skills: list[str],
    category_map: dict[str, set[str]],
    model: Any,
    resume_text: Optional[str] = None,
) -> tuple[bool, Optional[str], Optional[str], float, float, Optional[str]]:
    """
    Evaluates a single requirement through tiers:
      1. Exact canonical equality
      2. Alias canonical equivalence
      3. Example / alternative decomposition (e.g. "X (e.g. Y)", "A / B")
      4. Category membership (e.g. "Version control" satisfied by "Git", "Spring Framework" by "Spring Boot")
      5. Strict token-boundary match
      6. Per-skill semantic cosine similarity (threshold 0.72)
      7. Whole resume text evidence
    Returns: (match_found, matched_item, matched_type, match_credit, match_confidence, evidence_label)
    """
    j_norm = normalize_skill(req)
    decomp = decompose_requirement(req)
    base_concept = decomp["base_concept"]
    base_norm = normalize_skill(base_concept)
    examples = decomp["examples"]
    alternatives = decomp["alternatives"]

    match_found = False
    matched_item = None
    matched_type = None
    match_credit = 0.0
    match_confidence = 0.0

    # Tier 1 & 2: Exact or Alias Canonical Match
    for r_orig, r_norm in resume_canonical_map.items():
        if j_norm and r_norm and j_norm == r_norm:
            match_found = True
            matched_item = r_orig
            matched_type = "exact" if req.lower() == r_orig.lower() else "alias"
            match_credit = 1.0
            match_confidence = 1.0
            break
        if base_norm and r_norm and base_norm == r_norm:
            match_found = True
            matched_item = r_orig
            matched_type = "alias"
            match_credit = 1.0
            match_confidence = 1.0
            break

    # Tier 3: Decomposed Examples or Alternatives Match
    if not match_found and (examples or alternatives):
        candidates_to_check = examples + alternatives
        for cand in candidates_to_check:
            cand_norm = normalize_skill(cand)
            for r_orig, r_norm in resume_canonical_map.items():
                if (cand_norm and r_norm and cand_norm == r_norm) or token_matches(cand, r_orig):
                    match_found = True
                    matched_item = r_orig
                    matched_type = "example" if cand in examples else "alternative"
                    match_credit = 1.0
                    match_confidence = 1.0
                    break
            if match_found:
                break

    # Tier 4: Category Membership Match
    if not match_found:
        cat_keys = [base_concept.lower(), base_norm.lower(), req.lower(), j_norm.lower()]
        for cat_k in cat_keys:
            if cat_k in category_map:
                members = category_map[cat_k]
                for r_orig, r_norm in resume_canonical_map.items():
                    if r_norm in members or r_orig.lower() in members:
                        match_found = True
                        matched_item = r_orig
                        matched_type = "category"
                        match_credit = 0.95
                        match_confidence = 0.95
                        break
            if match_found:
                break

    # Tier 5: Strict Token-Boundary Match (avoids "java" in "javascript")
    if not match_found:
        for r_orig in clean_resume_skills:
            target_word = base_norm if base_norm else j_norm
            if target_word and len(target_word) > 2:
                if token_matches(target_word, r_orig) or token_matches(r_orig, req):
                    match_found = True
                    matched_item = r_orig
                    matched_type = "token"
                    match_credit = 0.90
                    match_confidence = 0.90
                    break

    # Tier 6: Per-Skill Semantic Cosine Similarity
    if not match_found and model is not None and clean_resume_skills:
        try:
            from sentence_transformers import util
            j_embed = model.encode(base_concept if base_concept else req, convert_to_tensor=True)
            r_embeds = model.encode(clean_resume_skills, convert_to_tensor=True)
            cos_sims = util.cos_sim(j_embed, r_embeds)[0]
            best_idx = int(cos_sims.argmax().item())
            best_sim = float(cos_sims[best_idx].item())

            if best_sim >= 0.72:
                match_found = True
                matched_item = clean_resume_skills[best_idx]
                matched_type = "semantic"
                match_credit = round(min(0.90, best_sim), 2)
                match_confidence = round(best_sim, 2)
        except Exception as ex:
            logger.debug(f"Per-skill semantic similarity skipped: {ex}")

    # Tier 7: Resume Text Evidence (search across whole resume sections)
    if not match_found and resume_text and resume_text.strip():
        evidence_targets = [base_norm if base_norm else j_norm] + examples + alternatives
        # Also check category members against resume text
        cat_keys = [base_concept.lower(), base_norm.lower(), req.lower(), j_norm.lower()]
        for cat_k in cat_keys:
            if cat_k in category_map:
                evidence_targets.extend(list(category_map[cat_k]))

        for target in evidence_targets:
            if target and len(target) > 2 and token_matches(target, resume_text):
                match_found = True
                matched_item = target
                matched_type = "text_evidence"
                match_credit = 0.85
                match_confidence = 0.85
                break

    evidence_label = None
    if match_found and matched_item:
        evidence_label = find_skill_evidence(matched_item, resume_text) if resume_text else matched_item

    return (match_found, matched_item, matched_type, match_credit, match_confidence, evidence_label)


def calculate_match_score(
    resume_skills: list[str],
    job_skills: list[str],
    nice_to_have_skills: Optional[list[str]] = None,
    resume_text: Optional[str] = None,
    candidate_experience_years: Optional[float] = None,
    required_experience_years: Optional[float] = None,
    shortlist_threshold: Optional[float] = None,
) -> dict[str, Any]:
    """
    Computes deterministic, accurate ATS match score between candidate skills/resume and job requirements.
    - Repairs broken parenthetical splits in job requirements.
    - Decomposes bundled requirements ("A, B, C") into atomic skills with proportional credit.
    - Searches whole resume text as evidence.
    - Evaluates required skills across exact, alias, example, category, token, and semantic tiers.
    """
    raw_job_skills = [str(s).strip() for s in (job_skills or []) if str(s).strip()]
    # Repair broken comma splits inside parentheses
    clean_job_skills = repair_and_clean_job_skills(raw_job_skills)

    clean_resume_skills = [str(s).strip() for s in (resume_skills or []) if str(s).strip()]
    clean_nice_skills = [str(s).strip() for s in (nice_to_have_skills or []) if str(s).strip()]

    # If raw resume text is provided, extract all verified skills across entire resume
    if resume_text and resume_text.strip():
        text_skills = extract_skills_from_text(resume_text)
        existing_canonical = {normalize_skill(s) for s in clean_resume_skills}
        for ts in text_skills:
            ts_norm = normalize_skill(ts)
            if ts_norm not in existing_canonical:
                existing_canonical.add(ts_norm)
                clean_resume_skills.append(ts)

    # Guard: No job requirements
    if not clean_job_skills:
        return {
            "match_score": 100 if clean_resume_skills else 0,
            "auto_action": "shortlist" if clean_resume_skills else "review",
            "matched_skills": [],
            "missing_skills": [],
            "bonus_skills": list(clean_resume_skills),
            "match_details": [],
            "skill_coverage_percent": 100.0 if clean_resume_skills else 0.0,
            "experience_match_percent": None,
            "score_breakdown": {
                "skill_score": 100 if clean_resume_skills else 0,
                "experience_score": None,
            },
            "rationale": "No specific required skills specified for this job posting.",
            "engine_version": "match-v2",
            "semantic_model_available": is_semantic_model_available(),
        }

    # Guard: Empty candidate skills and no text
    if not clean_resume_skills and not (resume_text and resume_text.strip()):
        return {
            "match_score": 0,
            "auto_action": "reject",
            "matched_skills": [],
            "missing_skills": list(clean_job_skills),
            "bonus_skills": [],
            "match_details": [
                {
                    "required_skill": j,
                    "status": "missing",
                    "matched_with": None,
                    "match_type": None,
                    "confidence": 0.0,
                    "evidence": None,
                }
                for j in clean_job_skills
            ],
            "skill_coverage_percent": 0.0,
            "experience_match_percent": None,
            "score_breakdown": {
                "skill_score": 0,
                "experience_score": None,
            },
            "rationale": f"Candidate profile has no listed skills. 0 of {len(clean_job_skills)} required skills matched.",
            "engine_version": "match-v2",
            "semantic_model_available": is_semantic_model_available(),
        }

    # Pre-compute canonical representations for resume skills
    resume_canonical_map = {}
    for r in clean_resume_skills:
        norm = normalize_skill(r)
        resume_canonical_map[r] = norm

    category_map = get_category_map()
    model = get_embedding_model()

    matched_job_skills = []
    missing_job_skills = []
    matched_details = []
    matched_resume_skills = set()
    total_credit = 0.0

    # Evaluate each required job skill
    for j_orig in clean_job_skills:
        decomp = decompose_requirement(j_orig)

        # Handle bundled requirements (e.g. "Git, SVN, Spring Framework, Azure")
        if decomp.get("is_bundled") and len(decomp.get("bundled_items", [])) > 1:
            sub_items = decomp["bundled_items"]
            sub_matched_names = []
            sub_missing_names = []
            sub_credit_sum = 0.0

            for sub_req in sub_items:
                m_found, m_item, m_type, m_cred, m_conf, m_ev = evaluate_single_requirement(
                    sub_req, resume_canonical_map, clean_resume_skills, category_map, model, resume_text
                )
                if m_found:
                    sub_matched_names.append(sub_req)
                    sub_credit_sum += (m_cred / len(sub_items))
                    if m_item:
                        matched_resume_skills.add(m_item)
                    matched_details.append(
                        {
                            "required_skill": sub_req,
                            "status": "matched",
                            "matched_with": m_item,
                            "match_type": m_type,
                            "confidence": m_conf,
                            "evidence": m_ev,
                        }
                    )
                else:
                    sub_missing_names.append(sub_req)
                    matched_details.append(
                        {
                            "required_skill": sub_req,
                            "status": "missing",
                            "matched_with": None,
                            "match_type": None,
                            "confidence": 0.0,
                            "evidence": None,
                        }
                    )

            if sub_matched_names:
                matched_job_skills.extend(sub_matched_names)
                total_credit += sub_credit_sum
            if sub_missing_names:
                missing_job_skills.extend(sub_missing_names)

        else:
            # Single atomic requirement
            m_found, m_item, m_type, m_cred, m_conf, m_ev = evaluate_single_requirement(
                j_orig, resume_canonical_map, clean_resume_skills, category_map, model, resume_text
            )
            if m_found:
                matched_job_skills.append(j_orig)
                if m_item:
                    matched_resume_skills.add(m_item)
                total_credit += m_cred
                matched_details.append(
                    {
                        "required_skill": j_orig,
                        "status": "matched",
                        "matched_with": m_item,
                        "match_type": m_type,
                        "confidence": m_conf,
                        "evidence": m_ev,
                    }
                )
            else:
                missing_job_skills.append(j_orig)
                matched_details.append(
                    {
                        "required_skill": j_orig,
                        "status": "missing",
                        "matched_with": None,
                        "match_type": None,
                        "confidence": 0.0,
                        "evidence": None,
                    }
                )

    # Deduplicate matched and missing lists
    matched_job_skills = list(dict.fromkeys(matched_job_skills))
    missing_job_skills = list(dict.fromkeys(missing_job_skills))

    # Calculate bonus skills (candidate skills not used to fulfill required skills)
    bonus_skills = [
        r for r in clean_resume_skills if r not in matched_resume_skills
    ]

    # Calculate nice-to-have bonus (capped at +5)
    nice_bonus = 0
    for nice in clean_nice_skills:
        nice_norm = normalize_skill(nice)
        for r_orig, r_norm in resume_canonical_map.items():
            if nice_norm == r_norm or token_matches(nice, r_orig):
                nice_bonus += 1
                break
    nice_bonus = min(5, nice_bonus)

    # Coverage ratio (0 - 100)
    skill_coverage = (total_credit / len(clean_job_skills)) * 100.0
    skill_score = min(100, round(skill_coverage + nice_bonus))

    # Experience scoring (if both values supplied and requirement > 0)
    exp_score = None
    final_score = skill_score
    if (
        candidate_experience_years is not None
        and required_experience_years is not None
        and required_experience_years > 0
    ):
        exp_ratio = min(1.0, max(0.0, candidate_experience_years / required_experience_years))
        exp_score = round(exp_ratio * 100)
        # Weighted composite: 85% skills + 15% experience
        composite = (skill_score * 0.85) + (exp_score * 0.15)
        final_score = max(0, min(100, round(composite)))

    # Determine autoAction using configured thresholds
    cutoff = shortlist_threshold if shortlist_threshold is not None else ATS_SHORTLIST_THRESHOLD
    if final_score >= cutoff:
        auto_action = "shortlist"
    elif final_score < ATS_REJECT_THRESHOLD:
        auto_action = "reject"
    else:
        auto_action = "review"

    # Construct human-readable rationale
    matched_count = len(matched_job_skills)
    total_count = len(clean_job_skills)
    if not missing_job_skills:
        rationale = f"Matched all required skills ({skill_coverage:.1f}% coverage)."
    else:
        missing_preview = ", ".join(missing_job_skills[:3])
        if len(missing_job_skills) > 3:
            missing_preview += f" (+{len(missing_job_skills) - 3} more)"
        rationale = f"Matched {matched_count} skills ({skill_coverage:.1f}% coverage). Missing: {missing_preview}."

    return {
        "match_score": final_score,
        "auto_action": auto_action,
        "matched_skills": matched_job_skills,
        "missing_skills": missing_job_skills,
        "bonus_skills": bonus_skills,
        "match_details": matched_details,
        "skill_coverage_percent": round(skill_coverage, 1),
        "experience_match_percent": float(exp_score) if exp_score is not None else None,
        "score_breakdown": {
            "skill_score": skill_score,
            "experience_score": exp_score,
        },
        "rationale": rationale,
        "engine_version": "match-v2",
        "semantic_model_available": is_semantic_model_available(),
    }
