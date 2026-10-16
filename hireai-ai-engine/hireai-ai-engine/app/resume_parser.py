"""
Module 2: Evidence-Based Resume Parser.
- Strict Pydantic schema validation.
- Zero-temperature LLM extraction with automatic retries on invalid JSON.
- Deterministic regex/section-based fallback parser (never fails open).
- Deterministic grounding check: drops any LLM-hallucinated skills not present in resume text.
- Every extracted skill carries: canonical name, verbatim evidence snippet, and section.
- Accurately computes experience from date ranges, treating internships appropriately.
"""

import os
import re
from typing import Optional, Any, Union
from pydantic import BaseModel, Field, ValidationError, field_validator

from app.config import LLM_MAX_RETRIES
from app.llm_client import call_llm
from app.logger import get_logger
from app.utils import extract_json
from app.skill_normalizer import (
    normalize_skill,
    extract_skills_from_text,
    find_skill_verbatim_evidence,
    is_skill_grounded_in_text,
    token_matches,
)

logger = get_logger(__name__)

PROMPT_PATH = os.path.join(os.path.dirname(__file__), "..", "prompts", "resume_parser_prompt.txt")

MONTH_MAP = {
    "jan": 1, "feb": 2, "mar": 3, "apr": 4, "may": 5, "jun": 6,
    "jul": 7, "aug": 8, "sep": 9, "oct": 10, "nov": 11, "dec": 12,
}


# ---------------------------------------------------------
# Pydantic Schemas for Validation
# ---------------------------------------------------------

class ParsedEducation(BaseModel):
    degree: Optional[str] = None
    institution: Optional[str] = None
    year: Optional[Union[str, int]] = None

    @field_validator("year", mode="before")
    @classmethod
    def coerce_year(cls, v: Any) -> Optional[str]:
        return str(v) if v is not None else None


class ParsedProject(BaseModel):
    name: Optional[str] = None
    technologies: list[str] = Field(default_factory=list)
    description: Optional[str] = None


class ParsedExperience(BaseModel):
    company: Optional[str] = None
    role: Optional[str] = None
    duration: Optional[str] = None
    is_internship: bool = False
    highlights: list[str] = Field(default_factory=list)


class SkillDetail(BaseModel):
    name: str
    canonical_name: str
    evidence_snippet: str
    section: str


class LlmResumeParseResult(BaseModel):
    skills: list[str] = Field(default_factory=list)
    soft_skills: list[str] = Field(default_factory=list, alias="softSkills")
    years_experience: Optional[float] = Field(default=0.0, alias="yearsExperience")
    education: list[ParsedEducation] = Field(default_factory=list)
    projects: list[ParsedProject] = Field(default_factory=list)
    experience: list[ParsedExperience] = Field(default_factory=list)
    certifications: list[str] = Field(default_factory=list)
    domain: Optional[str] = "General"
    current_role: Optional[str] = Field(default=None, alias="currentRole")

    @field_validator("years_experience", mode="before")
    @classmethod
    def coerce_exp(cls, v: Any) -> float:
        if v is None:
            return 0.0
        try:
            return float(v)
        except (ValueError, TypeError):
            return 0.0


# ---------------------------------------------------------
# Deterministic Fallback Parser
# ---------------------------------------------------------

def deterministic_extract_sections(resume_text: str) -> dict[str, str]:
    """Splits raw text into standard resume sections."""
    section_patterns = [
        ("summary", r"(?:^|\n)\s*(?:SUMMARY|OBJECTIVE|PROFILE|PROFESSIONAL SUMMARY)\s*(?:\n|:)"),
        ("skills", r"(?:^|\n)\s*(?:TECHNICAL SKILLS|SKILLS|TECHNOLOGIES|PROFICIENCIES)\s*(?:\n|:)"),
        ("experience", r"(?:^|\n)\s*(?:EXPERIENCE|WORK HISTORY|EMPLOYMENT|PROFESSIONAL EXPERIENCE)\s*(?:\n|:)"),
        ("projects", r"(?:^|\n)\s*(?:PROJECTS|PERSONAL PROJECTS|KEY PROJECTS)\s*(?:\n|:)"),
        ("education", r"(?:^|\n)\s*(?:EDUCATION|ACADEMIC BACKGROUND|ACADEMICS)\s*(?:\n|:)"),
        ("certifications", r"(?:^|\n)\s*(?:CERTIFICATIONS|CERTIFICATES|COURSES)\s*(?:\n|:)"),
        ("extracurricular", r"(?:^|\n)\s*(?:EXTRACURRICULAR|ACHIEVEMENTS|HONORS)\s*(?:\n|:)"),
    ]

    # Find start indices of each section
    matches = []
    for sec_name, pattern in section_patterns:
        m = re.search(pattern, resume_text, re.IGNORECASE)
        if m:
            matches.append((m.start(), sec_name))

    matches.sort()
    sections = {}
    if not matches:
        sections["full"] = resume_text
        return sections

    # Content before first section header is header/summary
    if matches[0][0] > 0:
        sections["header"] = resume_text[:matches[0][0]].strip()

    for i in range(len(matches)):
        start_idx = matches[i][0]
        sec_name = matches[i][1]
        end_idx = matches[i + 1][0] if i + 1 < len(matches) else len(resume_text)
        sections[sec_name] = resume_text[start_idx:end_idx].strip()

    return sections


def calculate_experience_from_text(experience_text: str) -> float:
    """Computes total experience in years from date spans within the Experience section."""
    if not experience_text:
        return 0.0

    # Match "Dec 2025 – Jun 2026", "Aug 2023 – Mar 2024", "May 2022 - Present", etc.
    date_pattern = re.compile(
        r"([A-Za-z]{3,9})\s+(\d{4})\s*[–\-—to\s]+\s*([A-Za-z]{3,9}|present|current)\s*(\d{4})?",
        re.IGNORECASE,
    )

    total_months = 0
    lines = experience_text.split("\n")
    for line in lines:
        matches = date_pattern.findall(line)
        for m in matches:
            start_month_str, start_year_str, end_month_str, end_year_str = m
            s_m = MONTH_MAP.get(start_month_str[:3].lower(), 1)
            try:
                s_y = int(start_year_str)
            except ValueError:
                continue

            if end_month_str.lower() in ("present", "current"):
                # Approximate to current time
                e_m = 10
                e_y = 2026
            else:
                e_m = MONTH_MAP.get(end_month_str[:3].lower(), 12)
                try:
                    e_y = int(end_year_str) if end_year_str else s_y
                except ValueError:
                    e_y = s_y

            diff = (e_y - s_y) * 12 + (e_m - s_m) + 1
            if 0 < diff <= 120:
                # Internships count towards hands-on experience
                total_months += diff

    if total_months == 0:
        # Fallback: check for explicit "X years" or "X+ years"
        exp_match = re.search(r"(\d+(?:\.\d+)?)\+?\s*(?:years|yrs)", experience_text, re.IGNORECASE)
        if exp_match:
            try:
                return float(exp_match.group(1))
            except ValueError:
                pass
        return 0.0

    # Treat internships and early roles accurately (e.g. 15 months = 1.0 - 1.2 years)
    years = total_months / 12.0
    return round(years, 1)


def deterministic_parse_resume(resume_text: str) -> dict[str, Any]:
    """
    Deterministic rule- and regex-based parser that executes when LLM is unavailable
    or fails validation. Extracts technical skills, project tech stacks, education,
    certifications, and experience without hallucinating.
    """
    sections = deterministic_extract_sections(resume_text)
    header_text = sections.get("header", "")
    lines = [l.strip() for l in resume_text.split("\n") if l.strip()]

    # Candidate Name & Role from header
    candidate_name = None
    current_role = None
    if lines:
        # First line is almost always candidate full name
        candidate_name = lines[0]
        if len(lines) > 1 and ("|" in lines[1] or "developer" in lines[1].lower() or "engineer" in lines[1].lower()):
            role_part = lines[1].split("|")[0].strip()
            current_role = role_part

    # Contact info
    email_match = re.search(r"[\w\.-]+@[\w\.-]+\.\w+", resume_text)
    email = email_match.group(0) if email_match else None

    phone_match = re.search(r"\+?\d[\d\s\-]{8,15}\d", resume_text)
    phone = phone_match.group(0) if phone_match else None

    # Deterministically extract skills from text
    extracted_canonical = extract_skills_from_text(resume_text)

    # Experience section parsing
    exp_text = sections.get("experience", "")
    years_experience = calculate_experience_from_text(exp_text)
    if years_experience == 0.0:
        # Check summary for experience statement
        summary_text = sections.get("summary", "")
        years_experience = calculate_experience_from_text(summary_text)

    # Project parsing: extract project headers with tech lists
    projects = []
    proj_text = sections.get("projects", "")
    if proj_text:
        proj_blocks = re.split(r"\n(?=[A-Z][A-Za-z0-9\s]{2,40}\s*\|)", proj_text)
        for pb in proj_blocks:
            pb_lines = [pl.strip() for pl in pb.split("\n") if pl.strip()]
            if not pb_lines:
                continue
            first_line = pb_lines[0]
            if "|" in first_line:
                parts = first_line.split("|")
                p_name = parts[0].strip()
                tech_part = parts[1].strip() if len(parts) > 1 else ""
                # Strip date from tech_part if present e.g. "Jul 2026"
                tech_part = re.sub(r"\b(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\s+\d{4}\b", "", tech_part).strip()
                techs = [t.strip() for t in tech_part.split(",") if t.strip()]
                desc = " ".join(pb_lines[1:])
                projects.append({"name": p_name, "technologies": techs, "description": desc[:300]})

    # Education parsing
    education = []
    edu_text = sections.get("education", "")
    if edu_text:
        edu_lines = [el.strip() for el in edu_text.split("\n") if el.strip()]
        if len(edu_lines) >= 2:
            inst = edu_lines[1]
            deg = edu_lines[2] if len(edu_lines) > 2 else edu_lines[1]
            education.append({"degree": deg, "institution": inst, "year": ""})

    # Certifications
    certifications = []
    cert_text = sections.get("certifications", "")
    if cert_text:
        for cl in cert_text.split("\n"):
            cl_clean = cl.strip()
            if cl_clean and not cl_clean.lower().startswith("certifications"):
                certifications.append(cl_clean)

    return {
        "candidate_name": candidate_name,
        "email": email,
        "phone": phone,
        "current_role": current_role,
        "domain": "Software Engineering",
        "years_experience": years_experience,
        "skills": extracted_canonical,
        "education": education,
        "projects": projects,
        "experience": [],
        "certifications": certifications,
    }


# ---------------------------------------------------------
# Main Resume Parsing Pipeline
# ---------------------------------------------------------

def parse_resume(resume_text: str) -> dict[str, Any]:
    """
    Parses a resume into structured, evidence-grounded JSON.
    1. Attempts LLM parsing with temperature 0.0 and Pydantic validation.
    2. Retries up to LLM_MAX_RETRIES times on invalid JSON.
    3. Falls back to deterministic parser on final failure.
    4. Deterministic grounding check: DROPS any LLM-extracted skill whose canonical
       name or alias does NOT appear in resume_text with word boundaries.
    5. Returns canonical skills, rich skillDetails (with verbatim snippets and sections),
       computed experience, education, projects, and domain.
    """
    if not resume_text or not resume_text.strip():
        return {
            "name": None,
            "email": None,
            "phone": None,
            "skills": [],
            "skillDetails": [],
            "softSkills": [],
            "yearsExperience": 0.0,
            "education": [],
            "projects": [],
            "experience": [],
            "certifications": [],
            "domain": "General",
            "currentRole": None,
            "parseStatus": "empty",
        }

    prompt_file = PROMPT_PATH if os.path.exists(PROMPT_PATH) else "prompts/resume_parser_prompt.txt"
    try:
        with open(prompt_file, encoding="utf-8") as f:
            template = f.read()
    except Exception as ex:
        logger.warning(f"Could not read prompt template ({ex}). Using default template.")
        template = "Parse this resume and return valid JSON with skills, education, projects, yearsExperience:\n\n{resume_text}"

    prompt = template.replace("{resume_text}", resume_text)

    parsed_llm_dict = None
    llm_attempts = 0
    max_parse_attempts = min(3, LLM_MAX_RETRIES + 1)

    while llm_attempts < max_parse_attempts:
        llm_attempts += 1
        try:
            raw_response = call_llm(prompt, task_type="resume_parse", temperature=0.0)
            json_candidate = extract_json(raw_response)
            if json_candidate and isinstance(json_candidate, dict):
                # Validate with Pydantic
                validated = LlmResumeParseResult(**json_candidate)
                parsed_llm_dict = validated.model_dump(by_alias=True)
                logger.info(f"LLM resume parsing succeeded and passed schema validation on attempt {llm_attempts}")
                break
            else:
                logger.warning(f"Resume parser attempt {llm_attempts}: Response was not valid JSON.")
        except (ValidationError, Exception) as ex:
            logger.warning(f"Resume parser attempt {llm_attempts} failed: {ex}")

    # Fall back to deterministic parser if LLM failed
    if not parsed_llm_dict:
        logger.info("Falling back to deterministic resume parser.")
        deterministic_data = deterministic_parse_resume(resume_text)
        candidate_skills_pool = deterministic_data.get("skills", [])
        years_exp = deterministic_data.get("years_experience", 0.0)
        candidate_name = deterministic_data.get("candidate_name")
        email = deterministic_data.get("email")
        phone = deterministic_data.get("phone")
        current_role = deterministic_data.get("current_role")
        education = deterministic_data.get("education", [])
        projects = deterministic_data.get("projects", [])
        experience = deterministic_data.get("experience", [])
        certifications = deterministic_data.get("certifications", [])
        domain = deterministic_data.get("domain", "Software Engineering")
        soft_skills = []
        parse_status = "fallback_deterministic"
    else:
        candidate_skills_pool = parsed_llm_dict.get("skills", [])
        years_exp = parsed_llm_dict.get("yearsExperience", 0.0)
        candidate_name = parsed_llm_dict.get("name")
        email = parsed_llm_dict.get("email")
        phone = parsed_llm_dict.get("phone")
        current_role = parsed_llm_dict.get("currentRole")
        education = parsed_llm_dict.get("education", [])
        projects = parsed_llm_dict.get("projects", [])
        experience = parsed_llm_dict.get("experience", [])
        certifications = parsed_llm_dict.get("certifications", [])
        domain = parsed_llm_dict.get("domain", "Software Engineering")
        soft_skills = parsed_llm_dict.get("softSkills", [])
        parse_status = "llm_grounded"

    if not candidate_name and lines:
        candidate_name = lines[0].strip()

    # Also extract deterministic skills from text to ensure no obvious skills are missed
    deterministic_skills = extract_skills_from_text(resume_text)
    for det_skill in deterministic_skills:
        if det_skill not in candidate_skills_pool:
            candidate_skills_pool.append(det_skill)

    # ---------------------------------------------------------
    # Mandatory Deterministic Grounding Check
    # ---------------------------------------------------------
    verified_skills = []
    skill_details = []
    seen_canonical = set()
    dropped_skills = []

    for raw_skill in candidate_skills_pool:
        if not raw_skill or not str(raw_skill).strip():
            continue
        skill_str = str(raw_skill).strip()
        canon = normalize_skill(skill_str)

        # Grounding check: verify that this skill or its alias is present in the resume text
        is_grounded, snippet, section = is_skill_grounded_in_text(skill_str, resume_text)
        if not is_grounded and canon:
            is_grounded, snippet, section = is_skill_grounded_in_text(canon, resume_text)

        if not is_grounded:
            dropped_skills.append(skill_str)
            logger.warning(
                f"Grounding check dropped ungrounded skill '{skill_str}' (not found in resume text)."
            )
            continue

        if canon not in seen_canonical:
            seen_canonical.add(canon)
            verified_skills.append(canon)
            skill_details.append(
                {
                    "name": skill_str,
                    "canonical_name": canon,
                    "evidence_snippet": snippet[:150] if snippet else skill_str,
                    "section": section or "skills",
                }
            )

    if dropped_skills:
        logger.info(f"Resume parsing grounding check dropped {len(dropped_skills)} ungrounded skills: {dropped_skills}")

    # Fallback experience calculation if LLM returned 0 or None
    if (years_exp is None or years_exp == 0.0) and sections_present(resume_text):
        sections = deterministic_extract_sections(resume_text)
        exp_text = sections.get("experience", "") or sections.get("summary", "")
        calculated_exp = calculate_experience_from_text(exp_text)
        if calculated_exp > 0.0:
            years_exp = calculated_exp

    result = {
        "name": candidate_name,
        "email": email,
        "phone": phone,
        "skills": verified_skills,
        "skillDetails": skill_details,
        "softSkills": soft_skills,
        "yearsExperience": float(years_exp) if years_exp is not None else 0.0,
        "education": education,
        "projects": projects,
        "experience": experience,
        "certifications": certifications,
        "domain": domain,
        "currentRole": current_role,
        "parseStatus": parse_status,
        "droppedUngroundedSkills": dropped_skills,
    }

    return result


def sections_present(text: str) -> bool:
    return any(h in text.lower() for h in ("experience", "work history", "summary", "projects"))
