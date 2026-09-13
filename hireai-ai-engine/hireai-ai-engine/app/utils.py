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


def to_str_list(val: Any) -> list[str]:
    """Flattens and normalizes any nested list, dict, or string into a clean list of strings."""
    if val is None:
        return []
    if isinstance(val, str):
        if "\n" in val:
            lines = [re.sub(r"^[\s\-\*\•\d\.\)]+", "", line).strip() for line in val.split("\n")]
            return [line for line in lines if line]
        cleaned = val.strip()
        return [cleaned] if cleaned else []
    if isinstance(val, (list, tuple, set)):
        result = []
        for item in val:
            if isinstance(item, (list, tuple, set)):
                result.extend(to_str_list(item))
            elif isinstance(item, dict):
                text = (
                    item.get("question")
                    or item.get("text")
                    or item.get("description")
                    or item.get("title")
                    or item.get("name")
                    or item.get("skill")
                    or " ".join(str(v) for v in item.values() if isinstance(v, str))
                )
                if text:
                    result.append(str(text).strip())
            elif item is not None:
                s = str(item).strip()
                if s:
                    result.append(s)
        return result
    if isinstance(val, dict):
        return [str(v).strip() for v in val.values() if str(v).strip()]
    cleaned = str(val).strip()
    return [cleaned] if cleaned else []


def to_str(val: Any) -> str:
    """Normalizes any value (str, list, dict) to a single string."""
    if val is None:
        return ""
    if isinstance(val, str):
        return val.strip()
    if isinstance(val, (list, tuple)):
        return "\n".join(to_str_list(val))
    if isinstance(val, dict):
        return " ".join(str(v) for v in val.values() if isinstance(v, str)).strip()
    return str(val).strip()

