"""
Skill Normalizer and Canonicalization Module.
Provides deterministic parsing, alias resolution, requirement decomposition,
token boundary matching, and dictionary-based skill extraction.
"""

import json
import os
import re
from functools import lru_cache
from typing import Any, Optional

DATA_PATH = os.path.join(os.path.dirname(__file__), "data", "skill_aliases.json")


@lru_cache(maxsize=1)
def load_skill_knowledge() -> dict[str, Any]:
    """Loads skill aliases and categories from the static JSON resource."""
    if os.path.exists(DATA_PATH):
        with open(DATA_PATH, "r", encoding="utf-8") as f:
            return json.load(f)
    return {"aliases": {}, "categories": {}}


@lru_cache(maxsize=1)
def get_alias_map() -> dict[str, str]:
    """Inverted lookup map: variant -> canonical skill name."""
    data = load_skill_knowledge()
    alias_dict = data.get("aliases", {})
    lookup: dict[str, str] = {}
    for canonical, variants in alias_dict.items():
        lookup[canonical.lower()] = canonical
        for v in variants:
            lookup[v.lower()] = canonical
    return lookup


@lru_cache(maxsize=1)
def get_canonical_variants_map() -> dict[str, set[str]]:
    """Maps canonical skill (lower) -> set of all lower-case variants and aliases."""
    data = load_skill_knowledge()
    alias_dict = data.get("aliases", {})
    mapping: dict[str, set[str]] = {}
    for canonical, variants in alias_dict.items():
        c_low = canonical.lower()
        if c_low not in mapping:
            mapping[c_low] = set()
        mapping[c_low].add(c_low)
        for v in variants:
            mapping[c_low].add(v.lower())
    return mapping


@lru_cache(maxsize=1)
def get_category_map() -> dict[str, set[str]]:
    """Category name -> set of member skills in canonical and lowercase forms."""
    data = load_skill_knowledge()
    categories = data.get("categories", {})
    alias_map = get_alias_map()
    cat_map: dict[str, set[str]] = {}
    for cat_name, members in categories.items():
        canon_members: set[str] = set()
        for m in members:
            m_low = m.lower()
            canon = alias_map.get(m_low, m_low)
            canon_members.add(canon)
            canon_members.add(canon.lower())
            canon_members.add(m_low)
        cat_map[cat_name.lower()] = canon_members
    return cat_map


@lru_cache(maxsize=1)
def get_specific_tools() -> set[str]:
    """Returns set of specific technologies/tools that require exact or alias match."""
    data = load_skill_knowledge()
    tools = data.get("specific_tools", [])
    alias_map = get_alias_map()
    result: set[str] = set()
    for t in tools:
        t_low = t.lower()
        result.add(t_low)
        canon = alias_map.get(t_low)
        if canon:
            result.add(canon.lower())
    return result


@lru_cache(maxsize=1)
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

    req_clean = str(job_skill or "").strip()
    req_norm = normalize_skill(req_clean).lower()
    base_clean = str(base_concept or "").strip()
    base_norm = normalize_skill(base_clean).lower() if base_clean else req_norm

    # If the exact requirement is a specific tool, it is NOT a category requirement
    # unless it has parenthetical examples or explicit category wording
    has_category_wording = any(
        w in req_clean.lower()
        for w in (
            "systems",
            "platforms",
            "frameworks",
            "technologies",
            "methodologies",
            "ecosystem",
            "e.g.",
            "such as",
            "including",
        )
    )
    if req_norm in specific_tools and not has_category_wording:
        return False, None

    # Check if base_concept or normalized requirement matches a known category directly
    candidate_keys = [base_clean.lower(), base_norm, req_clean.lower(), req_norm]
    for k in candidate_keys:
        if k and k in cat_map:
            return True, k

    # Check longer category keys first to avoid greedy false matches
    for k in sorted(cat_map.keys(), key=len, reverse=True):
        if not k:
            continue
        # Avoid treating single umbrella words (like 'spring') as category requirement
        # for specific child tech (like 'Spring Security' or 'Spring MVC') without category wording
        if k in ("spring", "agile") and not has_category_wording and req_norm != k:
            continue
        if k in base_clean.lower() or k in req_clean.lower():
            return True, k

    return False, None


FILLER_PATTERNS = [
    re.compile(
        r"^(?:proficient\s+(?:in|with)|proficiency\s+in|hands[- ]on\s+experience\s+(?:with|in)|"
        r"(?:strong|proven|demonstrated|solid|prior|good|extensive|deep|sound|thorough)\s+(?:experience|knowledge|understanding)\s+(?:with|in|of)|"
        r"experience\s+(?:with|in)|knowledge\s+of|working\s+knowledge\s+of|expertise\s+in|"
        r"familiar\s+with|familiarity\s+with|understanding\s+of|skilled\s+in|exposure\s+to|"
        r"ability\s+to\s+work\s+with)\s+",
        re.IGNORECASE,
    ),
    re.compile(
        r"\s+(?:programming\s+language|language|frameworks?|concepts?|technologies?|tools?|"
        r"platforms?|architecture|ecosystem|libraries|library|stacks?)$",
        re.IGNORECASE,
    ),
]


def clean_skill_string(s: str) -> str:
    """Removes leading/trailing filler text and collapses whitespace."""
    if not s:
        return ""
    cleaned = str(s).strip()
    for _ in range(5):
        prev = cleaned
        for pattern in FILLER_PATTERNS:
            cleaned = pattern.sub("", cleaned).strip()
        if cleaned == prev or not cleaned:
            break
    return cleaned if cleaned else str(s).strip()


def normalize_skill(skill: str) -> str:
    """
    Normalizes a skill name into its canonical representation.
    Preserves valid symbols (c++, c#, .net, node.js, ci/cd).
    """
    if not skill or not str(skill).strip():
        return ""

    raw_str = str(skill).strip()
    alias_map = get_alias_map()
    lower_raw = raw_str.lower()

    # 1. Exact alias match on raw lowercase string (preserves skills containing words like "architecture", "framework", etc.)
    if lower_raw in alias_map:
        return alias_map[lower_raw]

    # 2. Check parenthesized annotations e.g. "Java (Core Java, Java 8+)" -> check base "Java"
    paren_match = re.match(r"^([^(]+)\s*\((.*?)\)$", raw_str)
    if paren_match:
        base_raw = paren_match.group(1).strip().lower()
        if base_raw in alias_map:
            return alias_map[base_raw]
        base_clean = clean_skill_string(base_raw).lower()
        if base_clean in alias_map:
            return alias_map[base_clean]
        if base_clean:
            return base_clean

    # 3. Clean filler words and check alias map
    cleaned = clean_skill_string(raw_str).lower()
    if cleaned in alias_map:
        return alias_map[cleaned]

    # 4. Collapse whitespace
    collapsed = re.sub(r"\s+", " ", cleaned)
    if collapsed in alias_map:
        return alias_map[collapsed]

    return collapsed or raw_str.lower()


def split_outside_parens(s: str) -> list[str]:
    """
    Splits text by comma or semicolon, but only when not enclosed within parentheses or brackets.
    Handles nested parentheses without regex lookahead limitations.
    """
    if not s or not str(s).strip():
        return []

    parts: list[str] = []
    current: list[str] = []
    paren_depth = 0
    bracket_depth = 0

    for char in str(s).strip():
        if char == "(":
            paren_depth += 1
            current.append(char)
        elif char == ")":
            if paren_depth > 0:
                paren_depth -= 1
            current.append(char)
        elif char == "[":
            bracket_depth += 1
            current.append(char)
        elif char == "]":
            if bracket_depth > 0:
                bracket_depth -= 1
            current.append(char)
        elif char in (",", ";") and paren_depth == 0 and bracket_depth == 0:
            token = "".join(current).strip()
            if token:
                parts.append(token)
            current = []
        else:
            current.append(char)

    token = "".join(current).strip()
    if token:
        parts.append(token)

    return parts


def repair_and_clean_job_skills(skills: list[str]) -> list[str]:
    """
    Repairs broken requirements caused by naive comma splitting inside parentheses.
    E.g. ['Database management systems (e.g. MySQL', 'MongoDB)'] -> ['Database management systems (e.g. MySQL, MongoDB)']
    Also merges requirements split across 3+ items and cleans stray unclosed or unopened parentheses.
    """
    if not skills:
        return []

    repaired: list[str] = []
    clean_list = [str(s).strip() for s in skills if str(s).strip()]
    i = 0
    n = len(clean_list)

    while i < n:
        item = clean_list[i]

        # Clean stray leading open paren if no matching close paren before merging
        while item.startswith("(") and item.count("(") > item.count(")"):
            item = item[1:].strip()

        # Clean stray trailing close paren if no matching open paren before merging
        while item.endswith(")") and item.count(")") > item.count("("):
            item = item[:-1].strip()

        open_count = item.count("(")
        close_count = item.count(")")

        # If open parentheses exceed close parentheses, merge with subsequent items
        # until balanced or list is exhausted
        while open_count > close_count and i + 1 < n:
            i += 1
            next_item = clean_list[i]
            item = f"{item}, {next_item}"
            open_count = item.count("(")
            close_count = item.count(")")

        # Clean stray trailing close paren if no matching open paren
        while item.endswith(")") and item.count(")") > item.count("("):
            item = item[:-1].strip()

        # Clean stray leading open paren if no matching close paren
        while item.startswith("(") and item.count("(") > item.count(")"):
            item = item[1:].strip()

        # Balance trailing unclosed paren if still open
        if item.count("(") > item.count(")"):
            item = item + (")" * (item.count("(") - item.count(")")))

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
    text = str(job_skill or "").strip()
    # Check for stray unbalanced close paren
    while text.endswith(")") and text.count(")") > text.count("("):
        text = text[:-1].strip()

    # Check if this single requirement bundles multiple distinct sub-requirements separated by commas outside parens
    bundled_items = split_outside_parens(text)
    is_bundled = len(bundled_items) > 1

    examples: list[str] = []
    alternatives: list[str] = []

    # 1. Check for parenthesized examples "(e.g. AWS)", "(such as Docker, K8s)"
    eg_pattern = re.search(r"\((?:e\.?g\.?|eg:?|such as|including)\s*([^)]+)(?:\)|$)", text, flags=re.IGNORECASE)
    base_text = text
    if eg_pattern:
        raw_examples = eg_pattern.group(1)
        examples = [clean_skill_string(x) for x in re.split(r"[,;/]|\s+or\s+", raw_examples) if clean_skill_string(x)]
        base_text = text[:eg_pattern.start()] + text[eg_pattern.end():]
        base_text = clean_skill_string(base_text)

    # 2. Check for alternatives "A / B" or "A or B"
    # Guard against breaking compound skills with slashes like "CI/CD", "PL/SQL", "TCP/IP"
    alias_map = get_alias_map()
    base_lower = base_text.lower().strip()
    if base_lower not in alias_map:
        if re.search(r"\b(?:or|and/or)\b", base_text, flags=re.IGNORECASE):
            parts = re.split(r"\b(?:or|and/or)\b", base_text, flags=re.IGNORECASE)
            alts = []
            for p in parts:
                cleaned_p = clean_skill_string(p)
                if cleaned_p:
                    alts.append(cleaned_p)
            if len(alts) > 1:
                alternatives = alts
        elif "/" in base_text:
            # Check if this is an alternative slash (e.g. "PostgreSQL / MySQL") rather than a single term
            parts = [clean_skill_string(p) for p in base_text.split("/") if clean_skill_string(p)]
            # Only treat as alternatives if not a single protected compound skill
            if len(parts) > 1 and base_lower not in ("ci/cd", "pl/sql", "tcp/ip", "i/o"):
                alternatives = parts

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
    - "go" does NOT match "mongodb", "django", "algorithm", or English idioms ("go-to", "go live")
    - "sql" does NOT match "nosql"
    - "r" does NOT match "R&D"
    - "c" does NOT match "(c)" copyright
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
    if p_clean in ("c++", "cpp"):
        regex = rf"(?<![a-zA-Z0-9_]){escaped}(?![a-zA-Z0-9_+])"
    elif p_clean == "c#":
        regex = rf"(?<![a-zA-Z0-9_]){escaped}(?![a-zA-Z0-9_#])"
    else:
        regex = rf"(?<![a-zA-Z0-9_]){escaped}(?![a-zA-Z0-9_])"

    # Specific negative guard for SQL vs NoSQL
    if p_clean == "sql" and "nosql" in t_clean:
        t_without_nosql = re.sub(r"\bnosql\b", "", t_clean, flags=re.IGNORECASE)
        return bool(re.search(regex, t_without_nosql))

    # Specific negative guard for R language vs R&D
    if p_clean == "r" and ("r&d" in t_clean or "r & d" in t_clean or "r and d" in t_clean):
        t_without_rd = re.sub(r"\br\s*(?:&|and)\s*d\b", "", t_clean, flags=re.IGNORECASE)
        return bool(re.search(regex, t_without_rd))

    # Specific negative guard for C language vs copyright (c) or c/o
    if p_clean == "c" and ("(c)" in t_clean or "c/o" in t_clean):
        t_without_c = re.sub(r"\(\s*c\s*\)|\bc/o\b", "", t_clean, flags=re.IGNORECASE)
        return bool(re.search(regex, t_without_c))

    # Specific negative guard for Go vs English idioms ("go-to", "go live", "on the go")
    if p_clean == "go" and any(idiom in t_clean for idiom in ("go-to", "go to", "go live", "go-live", "on the go")):
        t_without_idioms = re.sub(r"\b(?:go[- ]to|go[- ]live|on[- ]the[- ]go)\b", "", t_clean, flags=re.IGNORECASE)
        return bool(re.search(regex, t_without_idioms))

    return bool(re.search(regex, t_clean))


# Allowed short 1-2 char tech acronyms
ALLOWED_SHORT_VARIANTS = {
    "c", "c#", "go", "js", "ts", "r", "ui", "ux", "qa", "ci", "cd", "ai", "ml", "db"
}


def extract_skills_from_text(text: str) -> list[str]:
    """
    Scans raw text using the alias dictionary to deterministically identify
    mentioned skills without relying on an LLM call.
    Includes fast substring pre-filtering for maximum performance.
    """
    if not text:
        return []

    alias_map = get_alias_map()
    found_canonicals: set[str] = set()
    text_lower = text.lower()

    for variant, canonical in alias_map.items():
        # Avoid short ambiguous 1-2 char words unless explicit
        if len(variant) <= 2 and variant not in ALLOWED_SHORT_VARIANTS:
            continue
        # Fast substring check before running regex token boundary matching
        if variant not in text_lower:
            continue
        if token_matches(variant, text):
            found_canonicals.add(canonical)

    return sorted(list(found_canonicals))


def get_all_skill_variants(skill_name: str) -> list[str]:
    """
    Resolves a skill name into all of its search variants and aliases,
    including canonical name, aliases from knowledge base, base name without parens,
    and decomposed inner tokens.
    """
    if not skill_name:
        return []

    raw = str(skill_name).strip()
    s_clean = clean_skill_string(raw).lower()
    canon = normalize_skill(raw).lower()

    variants: set[str] = {raw.lower(), s_clean, canon}

    # Extract base and items inside parens
    paren_match = re.match(r"^([^(]+)\s*\((.*?)\)$", raw)
    if paren_match:
        base = clean_skill_string(paren_match.group(1)).lower()
        if base:
            variants.add(base)
            base_canon = normalize_skill(base).lower()
            if base_canon:
                variants.add(base_canon)
        for part in re.split(r"[,;/]|\s+or\s+", paren_match.group(2)):
            p_clean = clean_skill_string(part).lower()
            if p_clean:
                variants.add(p_clean)

    # Add all known aliases for the canonical and base forms
    variants_map = get_canonical_variants_map()
    if canon in variants_map:
        variants.update(variants_map[canon])
    if s_clean in variants_map:
        variants.update(variants_map[s_clean])

    return [v for v in variants if v]


def parse_resume_sections(resume_text: str) -> list[tuple[str, str]]:
    """
    Parses resume text line by line, tagging each line with its detected section.
    Accurately handles markdown headers (## Experience, **Skills:**).
    Returns list of (line_text, section_name).
    """
    if not resume_text:
        return []

    lines = [line.strip() for line in resume_text.split("\n") if line.strip()]
    section_headers = {
        "summary": ["summary", "objective", "profile", "professional summary", "about", "about me"],
        "skills": ["technical skills", "skills", "technologies", "proficiencies", "technical competencies", "core skills", "tools"],
        "experience": ["experience", "work history", "employment", "professional experience", "work experience"],
        "project": ["projects", "personal projects", "academic projects", "key projects"],
        "education": ["education", "academic background", "qualifications"],
        "certification": ["certifications", "certificates", "courses", "licenses"],
    }

    current_section = "summary"
    annotated: list[tuple[str, str]] = []

    for line in lines:
        lower_line = line.strip().lower()
        # Clean markdown characters (#, *, -, _, :) from start and end of line for header detection
        header_candidate = re.sub(r"^[#*\-_\s\d.]+|[#*\-_\s:]+$", "", lower_line).strip()

        for sec, headers in section_headers.items():
            if header_candidate in headers or any(header_candidate.startswith(h + ":") or header_candidate.startswith(h + " -") for h in headers):
                current_section = sec
                break

        annotated.append((line, current_section))

    return annotated


def find_skill_evidence(skill_name: str, resume_text: str) -> str:
    """
    Identifies sections in the resume where the skill or its aliases appear as evidence.
    Returns e.g. "Spring Boot (Skills, Experience, Projects)".
    """
    if not skill_name or not resume_text:
        return skill_name or ""

    variants = get_all_skill_variants(skill_name)
    annotated_lines = parse_resume_sections(resume_text)

    matched_sections: set[str] = set()
    section_display_names = {
        "skills": "Skills",
        "experience": "Experience",
        "project": "Projects",
        "summary": "Summary",
        "education": "Education",
        "certification": "Certifications",
    }

    for line, sec in annotated_lines:
        for v in variants:
            if len(v) <= 2 and v not in ALLOWED_SHORT_VARIANTS:
                continue
            if token_matches(v, line):
                matched_sections.add(section_display_names.get(sec, sec.capitalize()))
                break

    if matched_sections:
        # Preserve standard section order
        ordered = [s for s in ("Skills", "Experience", "Projects", "Summary", "Education", "Certifications") if s in matched_sections]
        return f"{skill_name} ({', '.join(ordered)})"
    return skill_name


def find_skill_verbatim_evidence(skill_name: str, resume_text: str) -> tuple[bool, Optional[str], Optional[str]]:
    """
    Finds exact line/sentence in resume_text containing the skill or any alias,
    and identifies which section it belongs to.
    Returns (is_found, verbatim_snippet, section_name).
    """
    if not skill_name or not resume_text:
        return False, None, None

    variants = get_all_skill_variants(skill_name)
    annotated_lines = parse_resume_sections(resume_text)

    for line, sec in annotated_lines:
        for v in variants:
            if len(v) <= 2 and v not in ALLOWED_SHORT_VARIANTS:
                continue
            if token_matches(v, line):
                return True, line, sec

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

    grounded: list[str] = []
    seen: set[str] = set()

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
