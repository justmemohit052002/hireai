"""
Shared helper used by every module that asks the LLM for JSON back.

LLMs sometimes wrap JSON in markdown fences or add stray text despite
instructions not to. This extracts JSON defensively instead of assuming
a perfectly clean response.
"""

import json
import re


def extract_json(text: str) -> dict | None:
    text = text.strip()
    text = re.sub(r"^```(?:json)?", "", text)
    text = re.sub(r"```$", "", text)
    text = text.strip()

    try:
        return json.loads(text)
    except json.JSONDecodeError:
        pass

    start = text.find("{")
    end = text.rfind("}")
    if start != -1 and end != -1 and end > start:
        try:
            return json.loads(text[start : end + 1])
        except json.JSONDecodeError:
            return None
    return None
