"""Module 1: JD Generator. Single prompt-in, JSON-out call."""

from app.llm_client import call_llm
from app.utils import extract_json

PROMPT_PATH = "prompts/jd_generator_prompt.txt"


def generate_jd(job_title: str, required_skills: list[str], experience_level: str) -> dict:
    with open(PROMPT_PATH) as f:
        template = f.read()

    prompt = (
        template.replace("{job_title}", job_title)
        .replace("{required_skills}", ", ".join(required_skills))
        .replace("{experience_level}", experience_level)
    )
    raw_text = call_llm(prompt, task_type="jd_generate")

    parsed = extract_json(raw_text)
    if parsed is None:
        raise ValueError(f"Model did not return valid JSON:\n{raw_text}")
    return parsed
