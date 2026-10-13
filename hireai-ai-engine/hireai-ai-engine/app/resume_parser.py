"""
Module 2: Resume Parser.
Zero-shot LLM extraction combined with deterministic dictionary fallback extraction.
Ensures obvious technical skills (e.g. MySQL, Git, OOP, DSA) are never lost.
"""

import os
from app.llm_client import call_llm
from app.utils import extract_json
from app.skill_normalizer import extract_skills_from_text, normalize_skill

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

    # 1. Standardize keys from LLM response
    llm_skills = parsed.get("skills", [])
    if isinstance(llm_skills, str):
        llm_skills = [s.strip() for s in llm_skills.split(",") if s.strip()]
    elif not isinstance(llm_skills, list):
        llm_skills = []

    # 2. Run deterministic fallback extractor over the raw resume text
    deterministic_skills = extract_skills_from_text(resume_text)

    # 3. Union & deduplicate: keep all LLM skills plus any verified deterministic skills
    combined_skills = []
    seen_canonical = set()

    for s in llm_skills:
        clean = str(s).strip()
        if clean:
            norm = normalize_skill(clean)
            if norm not in seen_canonical:
                seen_canonical.add(norm)
                combined_skills.append(clean)

    for det in deterministic_skills:
        norm = normalize_skill(det)
        if norm not in seen_canonical:
            seen_canonical.add(norm)
            combined_skills.append(det)

    normalized = {
        "name": parsed.get("name") or parsed.get("candidate_name") or parsed.get("fullName"),
        "email": parsed.get("email") or parsed.get("contact_email"),
        "phone": parsed.get("phone") or parsed.get("contact_number"),
        "skills": combined_skills,
        "softSkills": parsed.get("softSkills") or parsed.get("soft_skills") or [],
        "yearsExperience": parsed.get("yearsExperience", parsed.get("years_of_experience", parsed.get("total_years_experience", 0))),
        "education": parsed.get("education", []),
        "projects": parsed.get("projects", []),
        "experience": parsed.get("experience", parsed.get("work_experience", [])),
        "domain": parsed.get("domain", "General"),
        "currentRole": parsed.get("currentRole", parsed.get("current_role", parsed.get("title"))),
    }

    return normalized
