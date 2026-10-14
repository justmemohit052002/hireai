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


def get_specific_tools() -> set[str]:
    """Returns set of specific technologies/tools that require exact or alias match."""
    data = load_skill_knowledge()
    tools = data.get("specific_tools", [])
    alias_map = get_alias_map()
    result = set()
    for t in tools:
        t_low = t.lower()
        result.add(t_low)
        canon = alias_map.get(t_low)
        if canon:
            result.add(canon.lower())
    return result


def get_related_skills_map() -> dict[str, list[str]]:
    """Returns mapping of skill -> list of related-but-different skills."""
    data = load_skill_knowledge()
    return data.get("related_skills", {})


def is_category_requirement(job_skill: str, base_concept: str = "") -> tuple[bool, Optional[str]]:
    """
    Determines if a job requirement is a CATEGORY requirement (e.g. 'database management systems',
    'cloud platforms', 'version control', 'testing frameworks') vs a SPECIFIC technology (e.g. 'Docker', 'Kubernetes', 'TypeScript').
    Returns (is_category, category_name).
    """
    specific_tools = get_specific_tools()
    cat_map = get_category_map()

    req_clean = job_skill.strip()
    req_norm = normalize_skill(req_clean).lower()
    base_clean = base_concept.strip() if base_concept else ""
    base_norm = normalize_skill(base_clean).lower() if base_clean else req_norm

    # If the exact requirement is a specific tool, it is NOT a category requirement
    # unless it has parenthetical examples or explicit category wording
    has_category_wording = any(
        w in req_clean.lower() for w in ("systems", "platforms", "frameworks", "technologies", "methodologies", "e.g.", "such as")
    )
    if req_norm in specific_tools and not has_category_wording:
        return False, None

    # Check if base_concept or normalized requirement matches a known category
    candidate_keys = [base_clean.lower(), base_norm, req_clean.lower(), req_norm]
    for k in candidate_keys:
        if k and k in cat_map:
            return True, k

    for k in cat_map:
        if k and (k in base_clean.lower() or k in req_clean.lower()):
            return True, k

    return False, None


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


def split_outside_parens(s: str) -> list[str]:
    """
    Splits text by comma or semicolon, but only when not enclosed within parentheses.
    Prevents splitting parenthetical examples like '(e.g. MySQL, MongoDB)'.
    """
    if not s or not str(s).strip():
        return []
    tokens = re.split(r"[,;]\s*(?![^()]*\))", str(s).strip())
    return [t.strip() for t in tokens if t.strip()]


def repair_and_clean_job_skills(skills: list[str]) -> list[str]:
    """
    Repairs broken requirements caused by naive comma splitting inside parentheses.
    E.g. ['Database management systems (e.g. MySQL', 'MongoDB)'] -> ['Database management systems (e.g. MySQL, MongoDB)']
    Also cleans stray unclosed or unopened parentheses.
    """
    repaired = []
    i = 0
    clean_list = [str(s).strip() for s in (skills or []) if str(s).strip()]
    while i < len(clean_list):
        item = clean_list[i]
        # Open paren without close paren, followed by close paren without open paren
        if "(" in item and ")" not in item and i + 1 < len(clean_list):
            next_item = clean_list[i + 1]
            if ")" in next_item and "(" not in next_item:
                merged = f"{item}, {next_item}"
                repaired.append(merged)
                i += 2
                continue
        # Clean stray trailing close paren if no open paren
        if item.endswith(")") and "(" not in item:
            item = item[:-1].strip()
        # Clean stray leading open paren if no close paren
        if item.startswith("(") and ")" not in item:
            item = item[1:].strip()
        if item:
            repaired.append(item)
        i += 1
    return repaired


def decompose_requirement(job_skill: str) -> dict[str, Any]:
    """
    Decomposes a descriptive requirement into its base concept, examples, alternatives,
    and bundled sub-requirements.
    Examples:
    - "Cloud computing platforms (e.g. AWS)" -> concept: "cloud computing platforms", examples: ["aws"]
    - "PostgreSQL / MySQL" -> alternatives: ["postgresql", "mysql"]
    - "Version control systems (e.g. Git, SVN), Spring Framework experience, Azure" -> bundled items
    """
    text = job_skill.strip()
    # Check for stray unbalanced close paren
    if text.endswith(")") and "(" not in text:
        text = text[:-1].strip()

    # Check if this single requirement bundles multiple distinct sub-requirements separated by commas outside parens
    bundled_items = split_outside_parens(text)
    is_bundled = len(bundled_items) > 1

    examples = []
    alternatives = []

    # 1. Check for parenthesized examples "(e.g. AWS)", "(such as Docker, K8s)"
    eg_pattern = re.search(r"\((?:e\.?g\.?|eg:?|such as|including)\s*([^)]+)(?:\)|$)", text, flags=re.IGNORECASE)
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
        "is_bundled": is_bundled,
        "bundled_items": bundled_items if is_bundled else [text],
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


def find_skill_evidence(skill_name: str, resume_text: str) -> str:
    """
    Identifies sections in the resume where the skill or its aliases appear as evidence.
    Returns e.g. "Spring Boot (Skills, Experience, Projects)".
    """
    if not skill_name or not resume_text:
        return skill_name or ""

    lower_text = resume_text.lower()
    alias_map = get_alias_map()
    variants = [skill_name.lower()]
    for v, c in alias_map.items():
        if c.lower() == skill_name.lower():
            variants.append(v)

    sections = []
    section_patterns = [
        ("Skills", r"(?:technical skills|skills|technologies|proficiencies)[\s\S]*?(?=(?:experience|projects|education|certifications|extracurricular|$))"),
        ("Experience", r"(?:experience|work history|employment)[\s\S]*?(?=(?:projects|education|certifications|extracurricular|$))"),
        ("Projects", r"(?:projects|personal projects|key projects)[\s\S]*?(?=(?:education|certifications|extracurricular|$))"),
        ("Summary", r"(?:summary|objective|profile)[\s\S]*?(?=(?:technical skills|skills|experience|$))"),
        ("Education", r"(?:education|academic background)[\s\S]*?(?=(?:certifications|extracurricular|$))"),
        ("Certifications", r"(?:certifications|certificates)[\s\S]*?(?=(?:extracurricular|$))"),
    ]

    for sec_name, pattern in section_patterns:
        match = re.search(pattern, lower_text)
        if match:
            sec_content = match.group(0)
            if any(token_matches(v, sec_content) for v in variants):
                sections.append(sec_name)

    if sections:
        return f"{skill_name} ({', '.join(sections)})"
    return skill_name


def find_skill_verbatim_evidence(skill_name: str, resume_text: str) -> tuple[bool, Optional[str], Optional[str]]:
    """
    Finds exact line/sentence in resume_text containing the skill or any alias,
    and identifies which section it belongs to.
    Returns (is_found, verbatim_snippet, section_name).
    """
    if not skill_name or not resume_text:
        return False, None, None

    alias_map = get_alias_map()
    s_clean = skill_name.lower().strip()
    variants = [s_clean]
    for v, c in alias_map.items():
        if c.lower() == s_clean or v == s_clean:
            variants.append(v)
            variants.append(c.lower())
    variants = list(dict.fromkeys(variants))

    lines = [line.strip() for line in resume_text.split("\n") if line.strip()]

    current_section = "summary"
    section_headers = {
        "summary": ["summary", "objective", "profile", "professional summary", "about"],
        "skills": ["technical skills", "skills", "technologies", "proficiencies", "technical competencies", "core skills"],
        "experience": ["experience", "work history", "employment", "professional experience", "work experience"],
        "project": ["projects", "personal projects", "academic projects", "key projects"],
        "education": ["education", "academic background", "qualifications"],
        "certification": ["certifications", "certificates", "courses", "licenses"],
    }

    for line in lines:
        lower_line = line.strip().lower()
        for sec, headers in section_headers.items():
            if any(lower_line == h or lower_line.startswith(h + ":") or lower_line.startswith(h + " -") for h in headers):
                current_section = sec
                break

        for v in variants:
            if len(v) <= 2 and v not in ("c#", "go", "js", "ts", "r"):
                continue
            if token_matches(v, line):
                return True, line, current_section

    return False, None, None


def is_skill_grounded_in_text(skill_name: str, resume_text: str) -> tuple[bool, Optional[str], Optional[str]]:
    """
    Checks if a skill or any of its canonical aliases appears in resume_text
    with strict word-boundary awareness.
    Returns (is_grounded, verbatim_snippet, section).
    """
    return find_skill_verbatim_evidence(skill_name, resume_text)


def filter_grounded_skills(skills: list[str], resume_text: Optional[str]) -> list[str]:
    """
    Filters a list of skills to only those grounded in resume_text (if resume_text is non-empty).
    If resume_text is None or blank, returns the skills as-is.
    """
    if not resume_text or not resume_text.strip() or not skills:
        return skills or []

    grounded = []
    seen = set()
    for s in skills:
        clean = str(s).strip()
        if not clean:
            continue
        canon = normalize_skill(clean)
        is_grounded, _, _ = is_skill_grounded_in_text(clean, resume_text)
        if is_grounded:
            if canon not in seen:
                seen.add(canon)
                grounded.append(clean)
    return grounded
