"""
Skill Normalizer and Canonicalization Module.
Provides deterministic parsing, alias resolution, requirement decomposition,
token boundary matching, and dictionary-based skill extraction.
"""

import json
import os
import re
from functools import lru_cache
from typing import Any

DATA_PATH = os.path.join(os.path.dirname(__file__), "data", "skill_aliases.json")


@lru_cache(maxsize=1)
def load_skill_knowledge() -> dict[str, Any]:
    """Loads skill aliases and categories from the static JSON resource."""
    if os.path.exists(DATA_PATH):
        with open(DATA_PATH, "r", encoding="utf-8") as f:
            return json.load(f)
    return {"aliases": {}, "categories": {}}


def get_alias_map() -> dict[str, str]:
    """Inverted lookup map: variant -> canonical skill name."""
    data = load_skill_knowledge()
    alias_dict = data.get("aliases", {})
    lookup = {}
    for canonical, variants in alias_dict.items():
        lookup[canonical.lower()] = canonical
        for v in variants:
            lookup[v.lower()] = canonical
    return lookup


def get_category_map() -> dict[str, set[str]]:
    """Category name -> set of member skills in canonical forms."""
    data = load_skill_knowledge()
    categories = data.get("categories", {})
    alias_map = get_alias_map()
    cat_map = {}
    for cat_name, members in categories.items():
        canon_members = set()
        for m in members:
            canon = alias_map.get(m.lower(), m.lower())
            canon_members.add(canon)
            canon_members.add(m.lower())
        cat_map[cat_name.lower()] = canon_members
    return cat_map


FILLER_PATTERNS = [
    re.compile(r"^(?:proficient in|hands[- ]on experience with|experience with|knowledge of|working knowledge of|expertise in|familiar with)\s+", re.IGNORECASE),
    re.compile(r"\s+(?:programming language|language|frameworks?|concepts?|technologies?|tools?|platforms?|architecture)$", re.IGNORECASE),
]


def clean_skill_string(s: str) -> str:
    """Removes leading/trailing filler text and collapses whitespace."""
    if not s:
        return ""
    cleaned = s.strip()
    for pattern in FILLER_PATTERNS:
        cleaned = pattern.sub("", cleaned).strip()
    return cleaned


def normalize_skill(skill: str) -> str:
    """
    Normalizes a skill name into its canonical representation.
    Preserves valid symbols (c++, c#, .net, node.js, ci/cd).
    """
    if not skill or not str(skill).strip():
        return ""

    raw = clean_skill_string(str(skill))
    lower = raw.lower().strip()

    # Exact alias match
    alias_map = get_alias_map()
    if lower in alias_map:
        return alias_map[lower]

    # Handle parenthesized annotations e.g. "Java (Core Java, Java 8+)" -> check base "Java"
    paren_match = re.match(r"^([^(]+)\s*\((.*?)\)$", raw)
    if paren_match:
        base = clean_skill_string(paren_match.group(1)).lower()
        if base in alias_map:
            return alias_map[base]

    # Unify dots / dashes for common tech without stripping valid syntax
    cleaned = re.sub(r"\s+", " ", lower)
    if cleaned in alias_map:
        return alias_map[cleaned]

    return cleaned


def decompose_requirement(job_skill: str) -> dict[str, Any]:
    """
    Decomposes a descriptive requirement into its base concept, examples, and alternatives.
    Examples:
    - "Cloud computing platforms (e.g. AWS)" -> concept: "cloud computing platforms", examples: ["aws"]
    - "PostgreSQL / MySQL" -> alternatives: ["postgresql", "mysql"]
    - "Java-based testing frameworks (e.g. JUnit)" -> concept: "java-based testing frameworks", examples: ["junit"]
    """
    text = job_skill.strip()
    examples = []
    alternatives = []

    # 1. Check for parenthesized examples "(e.g. AWS)", "(such as Docker, K8s)"
    eg_pattern = re.search(r"\((?:e\.?g\.?|eg:?|such as|including)\s*([^)]+)\)", text, flags=re.IGNORECASE)
    base_text = text
    if eg_pattern:
        raw_examples = eg_pattern.group(1)
        examples = [clean_skill_string(x) for x in re.split(r"[,;/]|\s+or\s+", raw_examples) if clean_skill_string(x)]
        base_text = text[:eg_pattern.start()] + text[eg_pattern.end():]
        base_text = clean_skill_string(base_text)

    # 2. Check for alternatives "A / B" or "A or B"
    if "/" in base_text or re.search(r"\b(?:or|and/or)\b", base_text, flags=re.IGNORECASE):
        parts = re.split(r"/|\b(?:or|and/or)\b", base_text, flags=re.IGNORECASE)
        alternatives = [clean_skill_string(p) for p in parts if clean_skill_string(p)]

    return {
        "original": job_skill,
        "base_concept": clean_skill_string(base_text),
        "examples": examples,
        "alternatives": alternatives,
    }


def token_matches(pattern: str, text: str) -> bool:
    """
    Checks if a pattern appears in text with strict word/token boundaries.
    Prevents false positives:
    - "java" does NOT match "javascript"
    - "go" does NOT match "mongodb"
    - "sql" does NOT match "nosql"
    """
    if not pattern or not text:
        return False

    p_clean = pattern.strip().lower()
    t_clean = text.strip().lower()

    if p_clean == t_clean:
        return True

    # Punctuation-sensitive special cases (c++, c#, .net)
    escaped = re.escape(p_clean)

    # Boundary before: start of string or non-word/non-alphanumeric character
    # Boundary after: end of string or non-word/non-alphanumeric character
    regex = rf"(?<![a-zA-Z0-9_]){escaped}(?![a-zA-Z0-9_])"

    # Specific negative guard for SQL vs NoSQL
    if p_clean == "sql" and "nosql" in t_clean:
        # Match only if sql exists outside of nosql
        t_without_nosql = re.sub(r"\bnosql\b", "", t_clean, flags=re.IGNORECASE)
        return bool(re.search(regex, t_without_nosql))

    # Specific negative guard for Go vs MongoDB / Algo / Django
    if p_clean == "go" and any(w in t_clean for w in ("mongodb", "django", "algorithm", "algorithms", "algo")):
        matches = re.finditer(regex, t_clean)
        for m in matches:
            start, end = m.start(), m.end()
            word_surrounding = t_clean[max(0, start - 5):min(len(t_clean), end + 5)]
            if not any(blocked in word_surrounding for blocked in ("mongo", "django", "algo")):
                return True
        return False

    return bool(re.search(regex, t_clean))


def extract_skills_from_text(text: str) -> list[str]:
    """
    Scans raw text using the alias dictionary to deterministically identify
    mentioned skills without relying on an LLM call.
    """
    if not text:
        return []

    alias_map = get_alias_map()
    found_canonicals = set()

    for variant, canonical in alias_map.items():
        # Avoid short ambiguous 1-2 char words unless explicit
        if len(variant) <= 2 and variant not in ("c#", "go", "js", "ts", "r"):
            continue
        if token_matches(variant, text):
            found_canonicals.add(canonical)

    return sorted(list(found_canonicals))
