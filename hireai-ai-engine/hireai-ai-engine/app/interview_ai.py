"""Module 5: Interview AI. Generates questions, then evaluates answers."""

from app.llm_client import call_llm
from app.utils import extract_json

QUESTIONS_PROMPT_PATH = "prompts/interview_questions_prompt.txt"
EVAL_PROMPT_PATH = "prompts/interview_eval_prompt.txt"


def generate_questions(skills: list[str]) -> dict:
    with open(QUESTIONS_PROMPT_PATH) as f:
        template = f.read()

    prompt = template.replace("{skills}", ", ".join(skills))
    raw_text = call_llm(prompt, task_type="interview_questions")

    parsed = extract_json(raw_text)
    if parsed is None:
        raise ValueError(f"Model did not return valid JSON:\n{raw_text}")
    return parsed


def evaluate_answers(answers: list[dict]) -> dict:
    with open(EVAL_PROMPT_PATH) as f:
        template = f.read()

    qa_text = "\n".join(
        f"{a['question_id']}: {a['answer_text']}" for a in answers
    )
    prompt = template.replace("{qa_pairs}", qa_text)
    raw_text = call_llm(prompt, task_type="interview_eval")

    parsed = extract_json(raw_text)
    if parsed is None:
        raise ValueError(f"Model did not return valid JSON:\n{raw_text}")
    return parsed
