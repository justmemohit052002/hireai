import os
from app.llm_client import call_llm
from app.utils import extract_json, to_str_list

QUESTIONS_PROMPT_PATH = os.path.join(os.path.dirname(__file__), "..", "prompts", "interview_questions_prompt.txt")
EVAL_PROMPT_PATH = os.path.join(os.path.dirname(__file__), "..", "prompts", "interview_eval_prompt.txt")


def generate_questions(skills: list[str]) -> dict:
    prompt_file = QUESTIONS_PROMPT_PATH if os.path.exists(QUESTIONS_PROMPT_PATH) else "prompts/interview_questions_prompt.txt"
    if os.path.exists(prompt_file):
        with open(prompt_file, encoding="utf-8") as f:
            template = f.read()
    else:
        template = "Generate interview questions for skills: {skills}"

    skills_str = ", ".join(to_str_list(skills)) if skills else ""
    prompt = template.replace("{skills}", skills_str)
    raw_text = call_llm(prompt, task_type="interview_questions")

    parsed = extract_json(raw_text)
    if parsed is None:
        raise ValueError(f"Model did not return valid JSON:\n{raw_text}")

    if isinstance(parsed, list):
        return {"questions": parsed}
    return parsed


def evaluate_answers(answers: list[dict]) -> dict:
    with open(EVAL_PROMPT_PATH) as f:
        template = f.read()

    qa_text = "\n".join(
        f"{a.get('question_id', a.get('questionId', 'q'))}: {a.get('answer_text', a.get('answer', ''))}" for a in answers
    )
    prompt = template.replace("{qa_pairs}", qa_text)
    raw_text = call_llm(prompt, task_type="interview_eval")

    parsed = extract_json(raw_text)
    if parsed is None:
        raise ValueError(f"Model did not return valid JSON:\n{raw_text}")

    if isinstance(parsed, list):
        return {"evaluatedAnswers": parsed, "interviewScore": 75}
    return parsed
