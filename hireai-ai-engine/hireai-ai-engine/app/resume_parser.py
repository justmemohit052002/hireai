"""
Module 2: Resume Parser.
Zero-shot extraction - no training data needed. The LLM already knows what
resumes look like; we just tell it what shape to return data in.
"""

import os
from app.llm_client import call_llm
from app.utils import extract_json

PROMPT_PATH = os.path.join(os.path.dirname(__file__), "..", "prompts", "resume_parser_prompt.txt")


def parse_resume(resume_text: str) -> dict:
    prompt_file = PROMPT_PATH if os.path.exists(PROMPT_PATH) else "prompts/resume_parser_prompt.txt"
    with open(prompt_file, encoding="utf-8") as f:
        template = f.read()

    prompt = template.replace("{resume_text}", resume_text)
    raw_text = call_llm(prompt, task_type="resume_parse")

    parsed = extract_json(raw_text)
    if parsed is None or not isinstance(parsed, dict):
        raise ValueError(f"Model did not return a valid JSON object:\n{raw_text}")

    # Standardize & normalize keys for consistent downstream integration
    normalized = {
        "name": parsed.get("name") or parsed.get("candidate_name") or parsed.get("fullName"),
        "email": parsed.get("email") or parsed.get("contact_email"),
        "phone": parsed.get("phone") or parsed.get("contact_number"),
        "skills": parsed.get("skills", []),
        "yearsExperience": parsed.get("yearsExperience", parsed.get("years_of_experience", parsed.get("total_years_experience", 0))),
        "education": parsed.get("education", []),
        "projects": parsed.get("projects", []),
        "experience": parsed.get("experience", parsed.get("work_experience", [])),
        "domain": parsed.get("domain", "General"),
        "currentRole": parsed.get("currentRole", parsed.get("current_role", parsed.get("title"))),
    }

    # Ensure skills is always a list of clean strings
    if isinstance(normalized["skills"], list):
        normalized["skills"] = [str(s).strip() for s in normalized["skills"] if s]
    elif isinstance(normalized["skills"], str):
        normalized["skills"] = [s.strip() for s in normalized["skills"].split(",") if s.strip()]

    return normalized
