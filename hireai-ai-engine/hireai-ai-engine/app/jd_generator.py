"""
Module 1: Level-Aware JD & Skill Generator.
Grounds generation on a curated, maintainable role profile catalog,
refines via LLM with strict JSON schema validation, temperature 0.0,
enforces level sanity and count ranges, and falls back deterministically to catalog baselines.
"""

import json
import os
import re
from functools import lru_cache
from typing import Any, Optional

from pydantic import BaseModel, Field

from app.logger import get_logger
from app.llm_client import (
    call_llm,
    check_ollama_reachable,
    get_active_provider_info,
)
from app.utils import extract_json, to_str, to_str_list
from app.skill_normalizer import (
    clean_skill_string,
    decompose_requirement,
    split_outside_parens,
    normalize_skill,
)

logger = get_logger(__name__)

PROMPT_PATH = os.path.join(os.path.dirname(__file__), "..", "prompts", "jd_generator_prompt.txt")
CATALOG_PATH = os.path.join(os.path.dirname(__file__), "data", "role_catalog.json")


class RefinedJdSchema(BaseModel):
    description: Optional[str] = None
    responsibilities: list[str] = Field(default_factory=list)
    required: list[str] = Field(default_factory=list)
    preferred: list[str] = Field(default_factory=list)
    experience_years: dict[str, float] = Field(default_factory=dict)
    education: Optional[str] = None
    soft_skills: list[str] = Field(default_factory=list)
    interview_questions: list[str] = Field(default_factory=list)


@lru_cache(maxsize=1)
def load_role_catalog() -> dict[str, Any]:
    """Loads the versioned role catalog from static JSON resource."""
    if os.path.exists(CATALOG_PATH):
        try:
            with open(CATALOG_PATH, "r", encoding="utf-8") as f:
                return json.load(f)
        except Exception as e:
            logger.error(f"Failed to load role catalog from {CATALOG_PATH}: {e}")
    return {"role_families": {}, "level_ranges": {}, "prohibited_by_level": {}}


_CANONICAL_DISPLAY_MAP: dict[str, str] = {}


def get_canonical_display_map() -> dict[str, str]:
    """Returns a lookup dictionary mapping normalized lowercase skill strings to canonical display names."""
    global _CANONICAL_DISPLAY_MAP
    if not _CANONICAL_DISPLAY_MAP:
        # Standard curated map
        m = {
            "java": "Java",
            "spring boot": "Spring Boot",
            "rest apis": "REST APIs",
            "rest api": "REST APIs",
            "rest": "REST APIs",
            "object-oriented programming": "Object-Oriented Programming",
            "data structures and algorithms": "Data Structures and Algorithms",
            "sql": "SQL",
            "git": "Git",
            "docker": "Docker",
            "kubernetes": "Kubernetes",
            "system design": "System Design",
            "distributed systems": "Distributed Systems",
            "microservices": "Microservices",
            "junit": "JUnit",
            "mockito": "Mockito",
            "maven": "Maven",
            "gradle": "Gradle",
            "postman": "Postman",
            "swagger": "Swagger",
            "linux": "Linux",
            "agile methodologies": "Agile Methodologies",
            "agile": "Agile Methodologies",
            "spring security": "Spring Security",
            "hibernate": "Hibernate",
            "jpa": "Hibernate",
            "postgresql": "PostgreSQL",
            "mysql": "MySQL",
            "mongodb": "MongoDB",
            "redis": "Redis",
            "kafka": "Kafka",
            "rabbitmq": "RabbitMQ",
            "aws": "AWS",
            "azure": "Azure",
            "gcp": "GCP",
            "ci/cd": "CI/CD",
            "react": "React",
            "react.js": "React",
            "javascript": "JavaScript",
            "typescript": "TypeScript",
            "html5": "HTML5",
            "html": "HTML5",
            "css3": "CSS3",
            "css": "CSS3",
            "tailwind css": "Tailwind CSS",
            "python": "Python",
            "django": "Django",
            "fastapi": "FastAPI",
            "technical leadership": "Technical Leadership",
            "architecture ownership": "Architecture Ownership",
            "engineering leadership": "Engineering Leadership",
            "technology strategy": "Technology Strategy",
            "mentoring": "Technical Mentoring",
            "code review": "Code Review",
            "full stack architecture": "Full Stack Architecture",
        }
        # Enrich from role catalog
        catalog = load_role_catalog()
        for fam_data in catalog.get("role_families", {}).values():
            for lvl_data in fam_data.get("levels", {}).values():
                for s in lvl_data.get("required", []) + lvl_data.get("preferred", []):
                    norm = normalize_skill(s)
                    if norm and norm not in m:
                        m[norm] = s
        _CANONICAL_DISPLAY_MAP = m
    return _CANONICAL_DISPLAY_MAP


def normalize_experience_level(level_raw: Optional[str]) -> str:
    """Normalizes any level string into canonical uppercase: ENTRY, MID, SENIOR, LEAD, EXECUTIVE."""
    if not level_raw:
        return "MID"
    l_str = str(level_raw).strip().lower().replace("-", "_").replace(" ", "_")
    if any(k in l_str for k in ["fresher", "entry", "intern", "junior", "trainee", "0_2", "0-2"]):
        return "ENTRY"
    if any(k in l_str for k in ["lead", "staff", "principal", "tech_lead", "team_lead", "architect", "8+"]):
        return "LEAD"
    if any(k in l_str for k in ["executive", "director", "vp", "head", "c_level", "cto", "cio", "vp_engineering"]):
        return "EXECUTIVE"
    if any(k in l_str for k in ["senior", "sr", "5_8", "5-8"]):
        return "SENIOR"
    return "MID"


def detect_role_family(job_title: str, description: str = "", domain: str = "") -> str:
    """
    Detects the best-matching role family based on job title, description, and domain keywords.
    Prioritizes specific families (e.g. Full Stack vs Frontend vs Backend).
    """
    text = f"{job_title or ''} {description or ''} {domain or ''}".lower()
    title_low = (job_title or "").lower()

    # 1. Full Stack (Java + React or general full stack)
    if "full stack" in text or "fullstack" in text or "full-stack" in text:
        return "full_stack_java_react"

    if ("react" in title_low or "frontend" in title_low) and ("java" in title_low or "spring" in title_low or "backend" in title_low):
        return "full_stack_java_react"

    # 2. QA / Automation
    if any(k in text for k in ["qa ", "sdet", "test engineer", "automation engineer", "quality assurance", "tester"]):
        return "qa_automation"

    # 3. DevOps / Cloud / SRE
    if any(k in text for k in ["devops", "cloud engineer", "sre", "site reliability", "infrastructure", "platform engineer", "kubernetes engineer", "aws engineer"]):
        return "devops_cloud"

    # 4. Data / Machine Learning / AI
    if any(k in text for k in ["data science", "data scientist", "machine learning", "ml engineer", "ai engineer", "data engineer", "deep learning", "nlp", "computer vision"]):
        return "data_ml"

    # 5. Mobile
    if any(k in text for k in ["mobile developer", "android", "ios", "flutter", "react native", "swift developer", "mobile engineer"]):
        return "mobile"

    # 6. Frontend
    if any(k in text for k in ["frontend", "front end", "front-end", "react", "vue", "angular", "ui developer", "javascript developer", "web developer"]):
        return "frontend"

    # 7. Python Backend
    if any(k in text for k in ["python", "django", "fastapi", "flask"]):
        return "python_backend"

    # 8. Java Backend
    if any(k in text for k in ["java", "spring", "spring boot", "backend", "jvm", "kotlin backend"]):
        return "java_backend"

    return "generic"


def get_catalog_baseline(role_family: str, level: str) -> dict[str, Any]:
    """Retrieves baseline skills, ranges, and expectations from role catalog."""
    norm_level = normalize_experience_level(level)
    level_key = norm_level.lower()

    catalog = load_role_catalog()
    families = catalog.get("role_families", {})
    if role_family in families:
        fam_key = role_family
    else:
        fam_key = detect_role_family(role_family)

    fam_data = families.get(fam_key) or families.get("generic", {})
    levels = fam_data.get("levels", {})
    level_data = levels.get(level_key) or levels.get("mid", {})

    level_ranges = catalog.get("level_ranges", {}).get(
        level_key,
        {"min_required": 8, "max_required": 12, "min_preferred": 5, "max_preferred": 7, "experience_years": {"min": 2, "max": 5}},
    )
    prohibited = catalog.get("prohibited_by_level", {}).get(level_key, [])

    req_list = list(level_data.get("required", []))
    pref_list = list(level_data.get("preferred", []))

    return {
        "role_family": fam_key,
        "display_name": fam_data.get("display_name", "Software Engineer"),
        "level": norm_level,
        "required": req_list,
        "required_skills": req_list,
        "preferred": pref_list,
        "preferred_skills": pref_list,
        "experience_years": level_data.get("experience_years", level_ranges.get("experience_years", {"min": 2, "max": 5})),
        "education": level_data.get("education", "Bachelor's degree in Computer Science or related technical discipline"),
        "soft_skills": list(level_data.get("soft_skills", ["Problem Solving", "Team Collaboration"])),
        "min_required": level_ranges.get("min_required", 8),
        "max_required": level_ranges.get("max_required", 12),
        "min_preferred": level_ranges.get("min_preferred", 5),
        "max_preferred": level_ranges.get("max_preferred", 7),
        "prohibited": [p.lower() for p in prohibited],
    }


def clean_atomic_skills(skills: list[str]) -> list[str]:
    """
    Normalizes a list of skills into atomic canonical names without parentheses,
    slashes, or duplicate entries.
    """
    display_map = get_canonical_display_map()
    atomic = []
    seen = set()

    for s in skills:
        if not s or not isinstance(s, str) or not s.strip():
            continue

        for part in split_outside_parens(s):
            part_str = part.strip()
            if not part_str:
                continue

            # Strip parenthetical explanations if base exists
            m_paren = re.match(r"^([^\(]+)\s*\((.*?)\)$", part_str)
            if m_paren:
                base_cand = m_paren.group(1).strip()
                inner_cand = m_paren.group(2).strip()
                cand = base_cand if base_cand else inner_cand
            else:
                cand = part_str

            # Strip " / " options like "Docker / Containerization" -> "Docker" (preserve CI/CD)
            if " / " in cand and cand.lower() != "ci/cd":
                cand = cand.split(" / ")[0].strip()

            clean = clean_skill_string(cand)
            if not clean:
                continue

            norm_key = normalize_skill(clean)
            if not norm_key:
                norm_key = clean.lower()

            canonical_name = display_map.get(norm_key, clean)

            if norm_key not in seen:
                seen.add(norm_key)
                atomic.append(canonical_name)

    return atomic


def enforce_level_sanity(
    raw_required: Any,
    raw_preferred: Any,
    level: Any = "MID",
    baseline: Optional[dict[str, Any]] = None,
    user_specified_skills: Optional[list[str]] = None,
) -> tuple[list[str], list[str]]:
    """
    Validates skill counts, removes prohibited skills for the seniority level from required skills,
    and deduplicates while strictly preserving user-specified manual skills.
    Accepts both (required_list, preferred_list, level, baseline) and (level, role_fam, req_list, pref_list).
    """
    if isinstance(raw_required, str) and isinstance(raw_preferred, str):
        level_arg = raw_required
        fam_arg = raw_preferred
        cand_req = level if isinstance(level, list) else []
        cand_pref = baseline if isinstance(baseline, list) else []
        baseline_dict = get_catalog_baseline(fam_arg, level_arg)
        level_norm = normalize_experience_level(level_arg)
        return enforce_level_sanity(cand_req, cand_pref, level=level_norm, baseline=baseline_dict, user_specified_skills=user_specified_skills)

    level_norm = normalize_experience_level(level)
    if baseline is None or not isinstance(baseline, dict):
        baseline = get_catalog_baseline("generic", level_norm)

    prohibited = set(baseline.get("prohibited", []))
    min_req = baseline.get("min_required", 8)
    max_req = baseline.get("max_required", 12)
    min_pref = baseline.get("min_preferred", 5)
    max_pref = baseline.get("max_preferred", 7)
    base_req = baseline.get("required", [])
    base_pref = baseline.get("preferred", [])

    clean_req = clean_atomic_skills(raw_required if isinstance(raw_required, list) else [])
    clean_pref = clean_atomic_skills(raw_preferred if isinstance(raw_preferred, list) else [])
    user_clean = set(s.lower() for s in clean_atomic_skills(user_specified_skills or []))

    # 1. Filter out prohibited skills from required
    filtered_req = []
    for s in clean_req:
        s_low = s.lower()
        if s_low in prohibited and s_low not in user_clean:
            logger.info(f"Filtered out prohibited skill '{s}' from required skills for level '{level_norm}'")
            if s not in clean_pref:
                clean_pref.append(s)
        else:
            filtered_req.append(s)

    # 2. Pad or truncate required skills to meet count ranges
    if len(filtered_req) < min_req:
        for b in base_req:
            if b.lower() not in [r.lower() for r in filtered_req]:
                filtered_req.append(b)
            if len(filtered_req) >= min_req:
                break

    if len(filtered_req) > max_req:
        prio = [s for s in filtered_req if s.lower() in user_clean]
        others = [s for s in filtered_req if s.lower() not in user_clean]
        filtered_req = (prio + others)[:max_req]

    # 3. Ensure preferred skills do not duplicate required skills
    req_set = set(r.lower() for r in filtered_req)
    filtered_pref = [p for p in clean_pref if p.lower() not in req_set]

    # Pad or truncate preferred skills
    if len(filtered_pref) < min_pref:
        for bp in base_pref:
            if bp.lower() not in req_set and bp.lower() not in [p.lower() for p in filtered_pref]:
                filtered_pref.append(bp)
            if len(filtered_pref) >= min_pref:
                break

    if len(filtered_pref) > max_pref:
        filtered_pref = filtered_pref[:max_pref]

    return filtered_req, filtered_pref


def generate_jd(
    job_title: str,
    required_skills: Optional[list[str]] = None,
    experience_level: Optional[str] = "mid",
    description: Optional[str] = None,
    domain: Optional[str] = None,
    years_of_experience: Optional[float] = None,
) -> dict[str, Any]:
    """
    Main JD & Skill Generation entry point.
    Combines catalog baseline with LLM refinement and strict level sanity checks.
    """
    clean_title = (job_title or "").strip() or "Software Engineer"
    norm_level = normalize_experience_level(experience_level)
    role_fam = detect_role_family(clean_title, description or "", domain or "")
    baseline = get_catalog_baseline(role_fam, norm_level)

    has_manual_skills = bool(required_skills and len(to_str_list(required_skills)) > 0)
    user_skill_list = to_str_list(required_skills) if has_manual_skills else []

    # Prepare prompt template
    prompt_file = PROMPT_PATH if os.path.exists(PROMPT_PATH) else "prompts/jd_generator_prompt.txt"
    template = ""
    if os.path.exists(prompt_file):
        with open(prompt_file, encoding="utf-8") as f:
            template = f.read()

    parsed_llm = None
    if template:
        prov, _ = get_active_provider_info()
        can_call_llm = True
        if prov == "ollama" and not check_ollama_reachable():
            logger.info("Local Ollama service is not reachable; applying deterministic catalog baseline.")
            can_call_llm = False

        if can_call_llm:
            manual_skills_str = (
                ", ".join(user_skill_list)
                if has_manual_skills
                else "(None specified - generate level-appropriate skill requirements using catalog baseline)"
            )
            prompt = (
                template.replace("{job_title}", clean_title)
                .replace("{experience_level}", norm_level.capitalize())
                .replace("{role_family}", baseline["display_name"])
                .replace("{baseline_required}", ", ".join(baseline["required"]))
                .replace("{baseline_preferred}", ", ".join(baseline["preferred"]))
                .replace("{min_required}", str(baseline["min_required"]))
                .replace("{max_required}", str(baseline["max_required"]))
                .replace("{min_preferred}", str(baseline["min_preferred"]))
                .replace("{max_preferred}", str(baseline["max_preferred"]))
                .replace("{required_skills}", manual_skills_str)
                .replace("{job_description}", to_str(description or "None provided"))
            )

            try:
                raw_text = call_llm(prompt, task_type="jd_generate", temperature=0.0)
                raw_json = extract_json(raw_text)
                if raw_json and isinstance(raw_json, dict):
                    # Validate with Pydantic
                    parsed_llm = RefinedJdSchema(**raw_json)
            except Exception as ex:
                logger.warn(f"LLM call for JD generation encountered issue ({ex}). Falling back to catalog baseline.")

    # Determine required and preferred skills
    if parsed_llm and parsed_llm.required:
        candidate_required = parsed_llm.required
        candidate_preferred = parsed_llm.preferred
    else:
        candidate_required = baseline["required"]
        candidate_preferred = baseline["preferred"]

    # If recruiter entered manual skills, prepend and strictly retain them
    if has_manual_skills:
        candidate_required = user_skill_list + [s for s in candidate_required if s.lower() not in [u.lower() for u in user_skill_list]]

    # Enforce Level Sanity and count bounds
    final_required, final_preferred = enforce_level_sanity(
        candidate_required,
        candidate_preferred,
        norm_level,
        baseline,
        user_specified_skills=user_skill_list if has_manual_skills else None,
    )

    # Synthesize fallback responsibilities if missing
    responsibilities = (
        to_str_list(parsed_llm.responsibilities)
        if parsed_llm and parsed_llm.responsibilities
        else [
            f"Design, build, and maintain production-grade components and services for the {clean_title} role.",
            "Collaborate closely with cross-functional engineering, product, and design peers to deliver robust features.",
            "Write modular, well-tested, and maintainable code adhering to industry best practices.",
            "Identify bottlenecks, optimize performance, and participate in technical reviews.",
        ]
    )

    # Synthesize fallback description if missing
    final_desc = (
        to_str(parsed_llm.description)
        if parsed_llm and parsed_llm.description
        else f"We are seeking a talented and motivated {clean_title} ({norm_level.capitalize()}) to join our engineering organization. In this role, you will leverage {', '.join(final_required[:4])} to build high-impact, scalable solutions."
    )

    # Synthesize interview questions if missing
    interview_questions = (
        to_str_list(parsed_llm.interview_questions)
        if parsed_llm and parsed_llm.interview_questions
        else [
            f"Can you walk through your hands-on experience using {final_required[0] if final_required else clean_title} in production?",
            "How do you approach debugging, performance profiling, and reliability in your applications?",
            "Describe a complex technical challenge you faced recently and how you resolved it.",
        ]
    )

    exp_years = (
        parsed_llm.experience_years
        if parsed_llm and parsed_llm.experience_years
        else baseline["experience_years"]
    )

    education = (
        to_str(parsed_llm.education)
        if parsed_llm and parsed_llm.education
        else baseline["education"]
    )

    soft_skills = (
        to_str_list(parsed_llm.soft_skills)
        if parsed_llm and parsed_llm.soft_skills
        else baseline["soft_skills"]
    )

    result = {
        "job_title": clean_title,
        "experience_level": norm_level,
        "role_family": role_fam,
        "description": final_desc,
        "responsibilities": responsibilities,
        "must_have_skills": final_required,
        "nice_to_have_skills": final_preferred,
        "required": final_required,
        "preferred": final_preferred,
        "required_skills": final_required,
        "preferred_skills": final_preferred,
        "experience_years": exp_years,
        "education": education,
        "soft_skills": soft_skills,
        "interview_questions": interview_questions,
    }
    return result
