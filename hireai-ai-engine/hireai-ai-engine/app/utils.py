"""
Shared helper used by every module that asks the LLM for JSON back.

LLMs sometimes wrap JSON in markdown fences, add stray text, or leave trailing
commas despite instructions not to. This extracts and repairs JSON defensively.
"""

import json
import re
from typing import Any, Union


def extract_json(text: str) -> Union[dict, list, None]:
    if not text:
        return None

    cleaned = text.strip()

    # Strip markdown code blocks
    cleaned = re.sub(r"^```(?:json)?\s*", "", cleaned, flags=re.IGNORECASE)
    cleaned = re.sub(r"\s*```$", "", cleaned)
    cleaned = cleaned.strip()

    # Try direct parse first
    try:
        return json.loads(cleaned)
    except Exception:
        pass

    # Find JSON Object {...} or Array [...]
    obj_start = cleaned.find("{")
    obj_end = cleaned.rfind("}")
    arr_start = cleaned.find("[")
    arr_end = cleaned.rfind("]")

    candidates = []
    if obj_start != -1 and obj_end != -1 and obj_end > obj_start:
        candidates.append(cleaned[obj_start : obj_end + 1])
    if arr_start != -1 and arr_end != -1 and arr_end > arr_start:
        candidates.append(cleaned[arr_start : arr_end + 1])

    for candidate in candidates:
        try:
            return json.loads(candidate)
        except Exception:
            pass

        # Try repairing trailing commas before } or ]
        repaired = re.sub(r",\s*([}\]])", r"\1", candidate)
        try:
            return json.loads(repaired)
        except Exception:
            pass

    return None
