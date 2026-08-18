"""
Module 4: AI Chatbot.
Stateless on our side - backend sends the full conversation history each
time (the LLM has no memory of its own), we return the next reply plus
whatever fields we've extracted so far.
"""

from app.llm_client import call_llm
from app.utils import extract_json

PROMPT_PATH = "prompts/chatbot_prompt.txt"


def handle_message(conversation_history: list[dict], new_message: str) -> dict:
    with open(PROMPT_PATH) as f:
        template = f.read()

    history_text = "\n".join(
        f"{turn['role']}: {turn['text']}" for turn in conversation_history
    )

    prompt = template.replace("{conversation_history}", history_text or "(none yet)").replace(
        "{new_message}", new_message
    )
    raw_text = call_llm(prompt, task_type="chatbot_message")

    parsed = extract_json(raw_text)
    if parsed is None:
        raise ValueError(f"Model did not return valid JSON:\n{raw_text}")
    return parsed
