"""
Module 2: Resume Parser.
Zero-shot extraction - no training data needed. The LLM already knows what
resumes look like; we just tell it what shape to return data in.
"""

from app.llm_client import call_llm
from app.utils import extract_json

PROMPT_PATH = "prompts/resume_parser_prompt.txt"


def parse_resume(resume_text: str) -> dict:
    with open(PROMPT_PATH) as f:
        template = f.read()

    prompt = template.replace("{resume_text}", resume_text)
    raw_text = call_llm(prompt, task_type="resume_parse")

    parsed = extract_json(raw_text)
    if parsed is None:
        raise ValueError(f"Model did not return valid JSON:\n{raw_text}")
    return parsed
