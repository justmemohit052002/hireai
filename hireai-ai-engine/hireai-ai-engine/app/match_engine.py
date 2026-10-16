"""
Module 3: Match Engine v2 (Hardened & Evidence-Based).
- Deterministic, explainable matching across strict tiers:
    1. Exact / canonical alias match (OOP, DSA, Postgres, REST, etc.) -> 1.0 credit.
    2. Decomposed parenthetical examples and alternatives -> 1.0 credit.
    3. Category requirement match (ONLY for category concepts like database systems,
       version control, cloud platforms, testing frameworks) -> 1.0 credit.
    4. Related but different technologies (e.g. JavaScript for TypeScript, AWS for Azure,
       Docker for Kubernetes) NEVER count as Matched Skills. They receive small, clearly labeled
       partial credit (default 0.25) and appear in 'related_skills'.
    5. Specific tools (Docker, Kubernetes, TypeScript, Kafka, Azure) REQUIRE exact/alias evidence.
       Semantic similarity never marks a match by itself.
- Deterministic score formula:
    Composite = (required_skills * W_req) + (preferred_skills * W_pref) + (experience_fit * W_exp) + (role_fit * W_role)
- Shortlist threshold decision is strictly compared against configurable cutoff.
"""

import os
import re
from typing import Optional, Any

from app.config import (
    ATS_SHORTLIST_THRESHOLD,
    ATS_REJECT_THRESHOLD,
    ATS_WEIGHT_REQUIRED_SKILLS,
    ATS_WEIGHT_PREFERRED_SKILLS,
    ATS_WEIGHT_EXPERIENCE,
    ATS_WEIGHT_ROLE,
    ATS_RELATED_SKILL_CREDIT,
)
from app.logger import get_logger
from app.skill_normalizer import (
    normalize_skill,
    decompose_requirement,
    repair_and_clean_job_skills,
    get_category_map,
    get_specific_tools,
    get_related_skills_map,
    is_category_requirement,
    filter_grounded_skills,
    token_matches,
    extract_skills_from_text,
    find_skill_verbatim_evidence,
    find_skill_evidence,
)

logger = get_logger(__name__)

# Lazy-loaded embedding model reference (used only to suggest candidate skills for verification)
_embedding_model = None
_model_load_attempted = False
_model_available = False


def get_embedding_model():
    """Lazy-loads the sentence-transformers model if available."""
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
    """Returns True if the sentence-transformers model is loaded."""
    global _model_available
    return _model_available


def evaluate_single_requirement(
    req: str,
    resume_canonical_map: dict[str, str],
    category_map: dict[str, set[str]],
    specific_tools: set[str],
    related_skills_map: dict[str, list[str]],
    resume_text: Optional[str] = None,
) -> tuple[str, Optional[str], Optional[str], float, float, Optional[str], Optional[str]]:
    """
    Evaluates a single requirement through strict, ranked tiers:
      1. Exact or canonical alias match -> status="matched", credit=1.0
      2. Decomposed parenthetical examples or alternatives -> status="matched", credit=1.0
      3. Category requirement match (ONLY if requirement is a category concept) -> status="matched", credit=1.0
      4. Related but different technology -> status="related", credit=ATS_RELATED_SKILL_CREDIT (default 0.25).
         NEVER counted in matched_skills!
      5. Otherwise -> status="missing", credit=0.0

    Returns:
      (status, matched_item, match_type, credit, confidence, evidence_label, related_item)
    """
    req_clean = req.strip()
    j_norm = normalize_skill(req_clean)
    decomp = decompose_requirement(req_clean)
    base_concept = decomp["base_concept"]
    base_norm = normalize_skill(base_concept) if base_concept else j_norm
    examples = decomp["examples"]
    alternatives = decomp["alternatives"]
    is_cat, cat_name = is_category_requirement(req_clean, base_concept)

    # -------------------------------------------------------------
    # Tier 1 & 2: Exact or Alias Match
    # -------------------------------------------------------------
    for r_orig, r_norm in resume_canonical_map.items():
        if (j_norm and r_norm and j_norm == r_norm) or (base_norm and r_norm and base_norm == r_norm):
            is_exact = (req_clean.lower() == r_orig.lower().strip())
            ev = find_skill_evidence(r_orig, resume_text) if resume_text else r_orig
            return ("matched", r_orig, "exact" if is_exact else "alias", 1.0, 1.0, ev, None)

    # -------------------------------------------------------------
    # Tier 3: Decomposed Examples or Alternatives Match
    # -------------------------------------------------------------
    if examples or alternatives:
        for cand in (examples + alternatives):
            cand_norm = normalize_skill(cand)
            for r_orig, r_norm in resume_canonical_map.items():
                if (cand_norm and r_norm and cand_norm == r_norm) or token_matches(cand, r_orig):
                    m_type = "example" if cand in examples else "alternative"
                    ev = find_skill_evidence(r_orig, resume_text) if resume_text else r_orig
                    return ("matched", r_orig, m_type, 1.0, 1.0, ev, None)

    # -------------------------------------------------------------
    # Tier 4: Category Requirement Match (ONLY for Category Concepts)
    # -------------------------------------------------------------
    if is_cat:
        cat_keys = [cat_name, base_concept.lower(), base_norm.lower(), req_clean.lower(), j_norm.lower()]
        for ck in cat_keys:
            if ck and ck in category_map:
                members = category_map[ck]
                for r_orig, r_norm in resume_canonical_map.items():
                    if r_norm in members or r_orig.lower() in members:
                        ev = find_skill_evidence(r_orig, resume_text) if resume_text else r_orig
                        return ("matched", r_orig, "category", 1.0, 0.95, ev, None)

    # -------------------------------------------------------------
    # Tier 5: Related but Different Technology (Partial Credit)
    # -------------------------------------------------------------
    req_key = (base_norm if base_norm else j_norm).lower()
    if req_key in related_skills_map:
        related_targets = related_skills_map[req_key]
        for r_orig, r_norm in resume_canonical_map.items():
            if r_norm.lower() in related_targets or r_orig.lower() in related_targets:
                ev = find_skill_evidence(r_orig, resume_text) if resume_text else r_orig
                return (
                    "related",
                    r_orig,
                    "related",
                    ATS_RELATED_SKILL_CREDIT,
                    0.75,
                    ev,
                    r_orig,
                )

    # -------------------------------------------------------------
    # Tier 6: Unmatched / Skill Gap
    # -------------------------------------------------------------
    return ("missing", None, None, 0.0, 0.0, None, None)


def calculate_match_score(
    resume_skills: list[str],
    job_skills: list[str],
    nice_to_have_skills: Optional[list[str]] = None,
    resume_text: Optional[str] = None,
    candidate_experience_years: Optional[float] = None,
    required_experience_years: Optional[float] = None,
    candidate_role: Optional[str] = None,
    job_title: Optional[str] = None,
    domain: Optional[str] = None,
    shortlist_threshold: Optional[float] = None,
) -> dict[str, Any]:
    """
    Computes deterministic, explainable ATS match score.
    1. Pre-processes and cleans job requirements.
    2. GROUNDS candidate resume skills: drops any profile skills not verified in resume_text.
    3. Evaluates required and nice-to-have skills using ranked exact, alias, example, category tiers.
    4. Separates genuine Matched Skills from Skill Gaps and Related Experience.
    5. Computes composite deterministic score with configurable weights.
    """
    raw_job_skills = [str(s).strip() for s in (job_skills or []) if str(s).strip()]
    clean_job_skills = repair_and_clean_job_skills(raw_job_skills)

    raw_resume_skills = [str(s).strip() for s in (resume_skills or []) if str(s).strip()]
    clean_nice_skills = repair_and_clean_job_skills([str(s).strip() for s in (nice_to_have_skills or []) if str(s).strip()])

    # -------------------------------------------------------------
    # Strict Grounding Check for Resume Skills
    # -------------------------------------------------------------
    if resume_text and resume_text.strip():
        # Drop any ungrounded skills from DB profile
        grounded_profile_skills = filter_grounded_skills(raw_resume_skills, resume_text)
        # Add deterministically extracted skills from text
        text_skills = extract_skills_from_text(resume_text)
        combined_skills = []
        seen_canonical = set()
        for s in grounded_profile_skills + text_skills:
            canon = normalize_skill(s)
            if canon not in seen_canonical:
                seen_canonical.add(canon)
                combined_skills.append(s)
        clean_resume_skills = combined_skills
    else:
        clean_resume_skills = raw_resume_skills

    # Guard: No job requirements specified
    if not clean_job_skills:
        return {
            "match_score": 100 if clean_resume_skills else 0,
            "auto_action": "shortlist" if clean_resume_skills else "review",
            "matched_skills": [],
            "missing_skills": [],
            "related_skills": [],
            "bonus_skills": list(clean_resume_skills),
            "match_details": [],
            "skill_coverage_percent": 100.0 if clean_resume_skills else 0.0,
            "experience_match_percent": None,
            "role_match_percent": None,
            "score_breakdown": {
                "skill_score": 100 if clean_resume_skills else 0,
                "required_skill_score": 100 if clean_resume_skills else 0,
                "preferred_skill_score": None,
                "experience_score": None,
                "role_score": None,
            },
            "rationale": "No specific required skills specified for this job posting.",
            "engine_version": "match-v2",
            "semantic_model_available": is_semantic_model_available(),
        }

    # Guard: No resume skills and no resume text
    if not clean_resume_skills and not (resume_text and resume_text.strip()):
        return {
            "match_score": 0,
            "auto_action": "reject",
            "matched_skills": [],
            "missing_skills": list(clean_job_skills),
            "related_skills": [],
            "bonus_skills": [],
            "match_details": [
                {
                    "required_skill": j,
                    "status": "missing",
                    "matched_with": None,
                    "match_type": None,
                    "confidence": 0.0,
                    "credit": 0.0,
                    "evidence": None,
                    "method": "none",
                    "related_skill": None,
                }
                for j in clean_job_skills
            ],
            "skill_coverage_percent": 0.0,
            "experience_match_percent": None,
            "role_match_percent": None,
            "score_breakdown": {
                "skill_score": 0,
                "required_skill_score": 0,
                "preferred_skill_score": None,
                "experience_score": None,
                "role_score": None,
            },
            "rationale": f"Candidate profile has no listed skills. 0 of {len(clean_job_skills)} required skills matched.",
            "engine_version": "match-v2",
            "semantic_model_available": is_semantic_model_available(),
        }

    # Pre-compute lookup maps
    resume_canonical_map = {r: normalize_skill(r) for r in clean_resume_skills}
    category_map = get_category_map()
    specific_tools = get_specific_tools()
    related_skills_map = get_related_skills_map()

    matched_job_skills = []
    missing_job_skills = []
    related_skills_list = []
    matched_details = []
    matched_resume_skills = set()

    total_required_credit = 0.0
    total_required_units = 0.0

    # -------------------------------------------------------------
    # Evaluate Required Job Skills
    # -------------------------------------------------------------
    for j_orig in clean_job_skills:
        decomp = decompose_requirement(j_orig)

        # Handle bundled requirements (e.g. "Git, SVN, Spring Framework, Azure")
        if decomp.get("is_bundled") and len(decomp.get("bundled_items", [])) > 1:
            sub_items = decomp["bundled_items"]
            for sub_req in sub_items:
                total_required_units += 1.0
                status, m_item, m_type, cred, conf, ev, rel = evaluate_single_requirement(
                    sub_req, resume_canonical_map, category_map, specific_tools, related_skills_map, resume_text
                )
                if status == "matched":
                    matched_job_skills.append(sub_req)
                    total_required_credit += cred
                    if m_item:
                        matched_resume_skills.add(m_item)
                    matched_details.append(
                        {
                            "required_skill": sub_req,
                            "status": "matched",
                            "matched_with": m_item,
                            "match_type": m_type,
                            "confidence": conf,
                            "credit": cred,
                            "evidence": ev,
                            "method": m_type,
                            "related_skill": None,
                        }
                    )
                elif status == "related":
                    missing_job_skills.append(sub_req)
                    related_skills_list.append(f"{sub_req} (via {rel}, {int(cred * 100)}% credit)")
                    total_required_credit += cred
                    matched_details.append(
                        {
                            "required_skill": sub_req,
                            "status": "related",
                            "matched_with": None,
                            "match_type": "related",
                            "confidence": conf,
                            "credit": cred,
                            "evidence": ev,
                            "method": "related_skill",
                            "related_skill": rel,
                        }
                    )
                else:
                    missing_job_skills.append(sub_req)
                    matched_details.append(
                        {
                            "required_skill": sub_req,
                            "status": "missing",
                            "matched_with": None,
                            "match_type": None,
                            "confidence": 0.0,
                            "credit": 0.0,
                            "evidence": None,
                            "method": "none",
                            "related_skill": None,
                        }
                    )

        else:
            # Atomic single requirement
            total_required_units += 1.0
            status, m_item, m_type, cred, conf, ev, rel = evaluate_single_requirement(
                j_orig, resume_canonical_map, category_map, specific_tools, related_skills_map, resume_text
            )
            if status == "matched":
                matched_job_skills.append(j_orig)
                total_required_credit += cred
                if m_item:
                    matched_resume_skills.add(m_item)
                matched_details.append(
                    {
                        "required_skill": j_orig,
                        "status": "matched",
                        "matched_with": m_item,
                        "match_type": m_type,
                        "confidence": conf,
                        "credit": cred,
                        "evidence": ev,
                        "method": m_type,
                        "related_skill": None,
                    }
                )
            elif status == "related":
                missing_job_skills.append(j_orig)
                related_skills_list.append(f"{j_orig} (via {rel}, {int(cred * 100)}% credit)")
                total_required_credit += cred
                matched_details.append(
                    {
                        "required_skill": j_orig,
                        "status": "related",
                        "matched_with": None,
                        "match_type": "related",
                        "confidence": conf,
                        "credit": cred,
                        "evidence": ev,
                        "method": "related_skill",
                        "related_skill": rel,
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
                        "credit": 0.0,
                        "evidence": None,
                        "method": "none",
                        "related_skill": None,
                    }
                )

    # Deduplicate display lists while preserving ordering
    matched_job_skills = list(dict.fromkeys(matched_job_skills))
    missing_job_skills = list(dict.fromkeys(missing_job_skills))
    related_skills_list = list(dict.fromkeys(related_skills_list))

    # Bonus skills (candidate skills that were not used to fulfill required skills)
    bonus_skills = [r for r in clean_resume_skills if r not in matched_resume_skills]

    # -------------------------------------------------------------
    # Evaluate Preferred / Nice-To-Have Skills
    # -------------------------------------------------------------
    preferred_credit = 0.0
    preferred_score = None
    if clean_nice_skills:
        for nice_req in clean_nice_skills:
            st, mi, mt, cr, cf, ev, rl = evaluate_single_requirement(
                nice_req, resume_canonical_map, category_map, specific_tools, related_skills_map, resume_text
            )
            if st == "matched":
                preferred_credit += cr
            elif st == "related":
                preferred_credit += (cr * 0.5)
        preferred_score = round((preferred_credit / len(clean_nice_skills)) * 100.0)

    # -------------------------------------------------------------
    # Experience Fit Scoring
    # -------------------------------------------------------------
    exp_score = None
    if (
        candidate_experience_years is not None
        and required_experience_years is not None
        and required_experience_years > 0
    ):
        exp_ratio = min(1.0, max(0.0, candidate_experience_years / required_experience_years))
        exp_score = round(exp_ratio * 100)

    # -------------------------------------------------------------
    # Role / Designation Relevance Scoring
    # -------------------------------------------------------------
    role_score = None
    if candidate_role and job_title:
        c_words = set(re.findall(r"\w+", candidate_role.lower()))
        j_words = set(re.findall(r"\w+", job_title.lower()))
        overlap = c_words.intersection(j_words)
        if overlap:
            role_score = min(100, 70 + len(overlap) * 15)
        else:
            role_score = 60
    elif candidate_role or job_title:
        role_score = None

    # -------------------------------------------------------------
    # Composite Score Calculation (Deterministic)
    # -------------------------------------------------------------
    required_coverage = (
        (total_required_credit / total_required_units) * 100.0
        if total_required_units > 0
        else 100.0
    )

    w_req = ATS_WEIGHT_REQUIRED_SKILLS
    w_pref = ATS_WEIGHT_PREFERRED_SKILLS
    w_exp = ATS_WEIGHT_EXPERIENCE
    w_role = ATS_WEIGHT_ROLE

    # Reweight if no preferred skills specified
    if preferred_score is None:
        w_req += w_pref
        w_pref = 0.0

    # Reweight if no experience requirement specified
    if exp_score is None:
        w_req += w_exp
        w_exp = 0.0

    # Reweight if no role relevance specified
    if role_score is None:
        w_req += w_role
        w_role = 0.0

    composite = (
        (required_coverage * w_req)
        + ((preferred_score or 0) * w_pref)
        + ((exp_score or 0) * w_exp)
        + ((role_score or 0) * w_role)
    )
    final_score = max(0, min(100, round(composite)))

    # Shortlist threshold decision
    cutoff = shortlist_threshold if shortlist_threshold is not None else ATS_SHORTLIST_THRESHOLD
    if final_score >= cutoff:
        auto_action = "shortlist"
    elif final_score < ATS_REJECT_THRESHOLD:
        auto_action = "reject"
    else:
        auto_action = "review"

    # Human-readable rationale
    matched_count = len(matched_job_skills)
    total_count = int(total_required_units)
    if not missing_job_skills:
        rationale = f"Matched all {total_count} required skills ({required_coverage:.1f}% coverage)."
    else:
        missing_preview = ", ".join(missing_job_skills[:3])
        if len(missing_job_skills) > 3:
            missing_preview += f" (+{len(missing_job_skills) - 3} more)"
        rationale = f"Matched {matched_count} of {total_count} skills ({required_coverage:.1f}% coverage). Missing: {missing_preview}."
        if related_skills_list:
            rationale += f" Related credit: {', '.join(related_skills_list[:2])}."

    return {
        "match_score": final_score,
        "auto_action": auto_action,
        "matched_skills": matched_job_skills,
        "missing_skills": missing_job_skills,
        "related_skills": related_skills_list,
        "bonus_skills": bonus_skills,
        "match_details": matched_details,
        "skill_coverage_percent": round(required_coverage, 1),
        "experience_match_percent": float(exp_score) if exp_score is not None else None,
        "role_match_percent": float(role_score) if role_score is not None else None,
        "score_breakdown": {
            "skill_score": round(required_coverage),
            "required_skill_score": round(required_coverage),
            "preferred_skill_score": preferred_score,
            "experience_score": exp_score,
            "role_score": role_score,
        },
        "rationale": rationale,
        "engine_version": "match-v2",
        "semantic_model_available": is_semantic_model_available(),
    }
