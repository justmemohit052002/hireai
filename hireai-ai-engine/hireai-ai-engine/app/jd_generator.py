"""Module 1: JD Generator. Single prompt-in, JSON-out call."""

import os
from app.llm_client import call_llm
from app.utils import extract_json, to_str, to_str_list

PROMPT_PATH = os.path.join(os.path.dirname(__file__), "..", "prompts", "jd_generator_prompt.txt")


def generate_jd(job_title: str, required_skills: list[str], experience_level: str) -> dict:
    prompt_file = PROMPT_PATH if os.path.exists(PROMPT_PATH) else "prompts/jd_generator_prompt.txt"
    if os.path.exists(prompt_file):
        with open(prompt_file, encoding="utf-8") as f:
            template = f.read()
    else:
        template = "Job title: {job_title}\nRequired skills: {required_skills}\nExperience level: {experience_level}"

    skills_str = (
        ", ".join(to_str_list(required_skills))
        if required_skills and len(to_str_list(required_skills)) > 0
        else "(None specified — automatically deduce and generate the top 4-6 core essential skills for this job title)"
    )
    prompt = (
        template.replace("{job_title}", str(job_title or ""))
        .replace("{required_skills}", skills_str)
        .replace("{experience_level}", str(experience_level or ""))
    )
    raw_text = call_llm(prompt, task_type="jd_generate")

    parsed = extract_json(raw_text)
    if parsed is None or not isinstance(parsed, dict):
        raise ValueError(f"Model did not return valid JSON:\n{raw_text}")

    req_skills_list = to_str_list(required_skills)
    normalized = {
        "job_title": str(job_title or ""),
        "experience_level": str(experience_level or ""),
        "description": to_str(parsed.get("description", "")),
        "responsibilities": to_str_list(parsed.get("responsibilities", [])),
        "must_have_skills": to_str_list(parsed.get("must_have_skills")) or req_skills_list,
        "nice_to_have_skills": to_str_list(parsed.get("nice_to_have_skills", [])),
        "interview_questions": to_str_list(parsed.get("interview_questions", [])),
    }
    return normalized

