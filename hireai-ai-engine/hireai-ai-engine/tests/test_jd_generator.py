"""
Unit tests for AI Job Description & Required Skills Generation.
Validates:
- Role profile catalog loading, versioning, and structure
- Role family detection across titles and descriptions
- Experience level normalization
- Level count ranges (Entry, Mid, Senior, Lead, Executive)
- Level-sanity filtering (prohibited skills like Kubernetes/System Design for Entry)
- Atomic canonical skill cleaning and deduplication
- Deterministic catalog baseline fallback on LLM failure
- ATS match engine weighting of required (80%) vs preferred (20%) skills
"""

import pytest
from unittest.mock import patch

from app.jd_generator import (
    load_role_catalog,
    normalize_experience_level,
    detect_role_family,
    get_catalog_baseline,
    clean_atomic_skills,
    enforce_level_sanity,
    generate_jd,
)
from app.match_engine import calculate_match_score


# ---------------------------------------------------------------------------
# 1. Role Profile Catalog Loader & Structure Tests
# ---------------------------------------------------------------------------
def test_catalog_loader_and_metadata():
    """Validates catalog is loaded, has versioning metadata and all required role families."""
    catalog = load_role_catalog()
    assert "version" in catalog
    assert "last_reviewed" in catalog
    assert "role_families" in catalog
    assert "level_ranges" in catalog
    assert "prohibited_by_level" in catalog

    roles = catalog["role_families"]
    expected_families = [
        "java_backend",
        "full_stack_java_react",
        "frontend",
        "python_backend",
        "data_ml",
        "devops_cloud",
        "qa_automation",
        "mobile",
        "generic",
    ]
    for fam in expected_families:
        assert fam in roles, f"Catalog missing role family: {fam}"
        fam_levels = roles[fam].get("levels", {})
        for lvl in ["entry", "mid", "senior", "lead", "executive"]:
            assert lvl in fam_levels, f"Role family {fam} missing level {lvl}"
            assert "required" in fam_levels[lvl]
            assert "preferred" in fam_levels[lvl]
            assert len(fam_levels[lvl]["required"]) > 0


# ---------------------------------------------------------------------------
# 2. Role Family Detection Tests
# ---------------------------------------------------------------------------
@pytest.mark.parametrize(
    "title,desc,expected_family",
    [
        ("Java Backend Developer", "Building scalable Spring Boot microservices", "java_backend"),
        ("Full Stack Developer (Java + React)", "Java backend with React UI", "full_stack_java_react"),
        ("Senior React Frontend Engineer", "Modern web app with React, TypeScript, Redux", "frontend"),
        ("Python Backend Developer", "FastAPI, Django, PostgreSQL", "python_backend"),
        ("Machine Learning Engineer", "PyTorch, NLP, model deployment", "data_ml"),
        ("Cloud & DevOps Engineer", "AWS, Terraform, Docker, CI/CD", "devops_cloud"),
        ("QA Automation Engineer", "Selenium, Cypress, TestNG, API testing", "qa_automation"),
        ("Android / iOS Mobile Developer", "Flutter, Swift, Kotlin mobile apps", "mobile"),
        ("Quantum Computing Researcher", "Theoretical physics and simulation", "generic"),
    ],
)
def test_detect_role_family(title, desc, expected_family):
    family = detect_role_family(title, desc)
    assert family == expected_family


# ---------------------------------------------------------------------------
# 3. Experience Level Normalization Tests
# ---------------------------------------------------------------------------
@pytest.mark.parametrize(
    "raw_input,expected_level",
    [
        ("Entry Level", "ENTRY"),
        ("Junior", "ENTRY"),
        ("entry", "ENTRY"),
        ("0-2 years", "ENTRY"),
        ("Mid Level", "MID"),
        ("Associate", "MID"),
        ("mid", "MID"),
        ("Senior Level", "SENIOR"),
        ("sr", "SENIOR"),
        ("senior", "SENIOR"),
        ("Lead", "LEAD"),
        ("Staff Engineer", "LEAD"),
        ("Principal", "LEAD"),
        ("Executive", "EXECUTIVE"),
        ("Director of Engineering", "EXECUTIVE"),
        ("VP Engineering", "EXECUTIVE"),
        ("CTO", "EXECUTIVE"),
    ],
)
def test_normalize_experience_level(raw_input, expected_level):
    assert normalize_experience_level(raw_input) == expected_level


# ---------------------------------------------------------------------------
# 4. Atomic Canonical Skill Cleaning & Deduplication Tests
# ---------------------------------------------------------------------------
def test_atomic_skill_cleaning_and_dedup():
    raw_skills = [
        "Java (Core Java, Java 8+)",
        "REST APIs (JSON / Swagger)",
        "Spring Boot",
        "spring boot",  # Duplicate alias/canonical
        "Git (Version Control)",
        "Docker / Containerization",
        "Data structures and algorithms",
        "OOP concepts (Object Oriented Programming)",
        "JUnit testing framework",
        "",
        "   ",
    ]
    cleaned = clean_atomic_skills(raw_skills)

    # Must be deduplicated canonical names
    assert "Spring Boot" in cleaned
    assert cleaned.count("Spring Boot") == 1
    assert "REST APIs" in cleaned
    assert "Git" in cleaned
    assert "Docker" in cleaned
    assert "Data Structures and Algorithms" in cleaned
    assert "Object-Oriented Programming" in cleaned
    assert "JUnit" in cleaned
    # Ensure no empty strings or uncleaned parentheses
    for s in cleaned:
        assert s.strip() != ""
        assert "(" not in s
        assert ")" not in s


# ---------------------------------------------------------------------------
# 5. Level-Sanity Filtering & Count Range Enforcement
# ---------------------------------------------------------------------------
def test_level_sanity_entry_prohibited_skills():
    """
    Validates that prohibited skills (Kubernetes, System Design, Distributed Systems,
    Architecture Ownership, Mentoring, Team Leadership) are NEVER placed in ENTRY required skills.
    """
    prohibited_candidates = [
        "Java",
        "Spring Boot",
        "SQL",
        "Git",
        "REST APIs",
        "JUnit",
        "Maven",
        "Object-Oriented Programming",
        "Kubernetes",  # Prohibited in Entry required
        "System Design",  # Prohibited in Entry required
        "Distributed Systems",  # Prohibited in Entry required
        "Architecture Ownership",  # Prohibited in Entry required
    ]
    preferred_candidates = ["Docker", "Linux", "Postman", "Agile Methodologies", "Mockito"]

    req_out, pref_out = enforce_level_sanity(
        "ENTRY", "java_backend", prohibited_candidates, preferred_candidates
    )

    # Required must NOT contain prohibited skills
    assert "Kubernetes" not in req_out
    assert "System Design" not in req_out
    assert "Distributed Systems" not in req_out
    assert "Architecture Ownership" not in req_out

    # Count must be within Entry range (8-10 required, 5-6 preferred)
    assert 8 <= len(req_out) <= 10
    assert 5 <= len(pref_out) <= 6


# ---------------------------------------------------------------------------
# 6. Catalog Baseline Fixtures: Java Backend Developer (All 5 Levels)
# ---------------------------------------------------------------------------
def test_java_backend_developer_fixtures_all_levels():
    title = "Java Backend Developer"

    # ENTRY
    entry_baseline = get_catalog_baseline(title, "ENTRY")
    req_entry = entry_baseline["required_skills"]
    pref_entry = entry_baseline["preferred_skills"]
    assert 8 <= len(req_entry) <= 10, f"Entry count out of range: {len(req_entry)}"
    assert 5 <= len(pref_entry) <= 6, f"Entry pref out of range: {len(pref_entry)}"
    # Required must include fundamentals
    for fund in ["Java", "Object-Oriented Programming", "Data Structures and Algorithms", "SQL", "Git", "REST APIs", "JUnit"]:
        assert fund in req_entry, f"Entry baseline missing fundamental: {fund}"
    # Required must NOT include advanced tier
    assert "Kubernetes" not in req_entry
    assert "System Design" not in req_entry
    assert "Distributed Systems" not in req_entry
    assert "Technical Leadership" not in req_entry

    # MID
    mid_baseline = get_catalog_baseline(title, "MID")
    req_mid = mid_baseline["required_skills"]
    pref_mid = mid_baseline["preferred_skills"]
    assert 10 <= len(req_mid) <= 13, f"Mid count out of range: {len(req_mid)}"
    assert 5 <= len(pref_mid) <= 7, f"Mid pref out of range: {len(pref_mid)}"
    assert "Spring Security" in req_mid
    assert "Docker" in req_mid
    assert "Hibernate" in req_mid or "JPA" in req_mid

    # SENIOR
    senior_baseline = get_catalog_baseline(title, "SENIOR")
    req_senior = senior_baseline["required_skills"]
    pref_senior = senior_baseline["preferred_skills"]
    assert 11 <= len(req_senior) <= 15, f"Senior count out of range: {len(req_senior)}"
    assert 5 <= len(pref_senior) <= 8, f"Senior pref out of range: {len(pref_senior)}"
    assert "System Design" in req_senior
    assert "Distributed Systems" in req_senior
    assert "Microservices" in req_senior

    # LEAD
    lead_baseline = get_catalog_baseline(title, "LEAD")
    req_lead = lead_baseline["required_skills"]
    pref_lead = lead_baseline["preferred_skills"]
    assert 10 <= len(req_lead) <= 14, f"Lead count out of range: {len(req_lead)}"
    assert 5 <= len(pref_lead) <= 8, f"Lead pref out of range: {len(pref_lead)}"
    assert "Architecture Ownership" in req_lead
    assert "Team Leadership" in req_lead or "Technical Leadership" in req_lead

    # EXECUTIVE
    exec_baseline = get_catalog_baseline(title, "EXECUTIVE")
    req_exec = exec_baseline["required_skills"]
    pref_exec = exec_baseline["preferred_skills"]
    assert 8 <= len(req_exec) <= 12, f"Exec count out of range: {len(req_exec)}"
    assert 4 <= len(pref_exec) <= 6, f"Exec pref out of range: {len(pref_exec)}"
    assert "Engineering Leadership" in req_exec
    assert "Technology Strategy" in req_exec


# ---------------------------------------------------------------------------
# 7. Catalog Baseline Fixtures: Full Stack Developer (Entry vs Senior)
# ---------------------------------------------------------------------------
def test_full_stack_fixtures_entry_vs_senior():
    title = "Full Stack Developer (Java + React)"

    entry = get_catalog_baseline(title, "ENTRY")
    req_entry = entry["required_skills"]
    assert 8 <= len(req_entry) <= 10
    assert "Java" in req_entry
    assert "React" in req_entry
    assert "JavaScript" in req_entry
    assert "SQL" in req_entry
    assert "Git" in req_entry
    assert "REST APIs" in req_entry
    assert "Kubernetes" not in req_entry
    assert "System Design" not in req_entry

    senior = get_catalog_baseline(title, "SENIOR")
    req_senior = senior["required_skills"]
    assert 11 <= len(req_senior) <= 15
    assert "System Design" in req_senior
    assert "Distributed Systems" in req_senior
    assert "Microservices" in req_senior
    assert "Mentoring" in req_senior
    assert "Java" in req_senior
    assert "React" in req_senior


# ---------------------------------------------------------------------------
# 8. Deterministic Fallback on LLM Failure
# ---------------------------------------------------------------------------
def test_llm_failure_deterministic_fallback():
    """
    When the LLM fails (e.g. timeout, invalid JSON, or offline Ollama),
    generate_jd must gracefully fall back to the curated catalog baseline
    and return complete, level-compliant skills (never empty or tiny).
    """
    with patch("app.jd_generator.check_ollama_reachable", return_value=False), \
         patch("app.jd_generator.call_llm", side_effect=Exception("LLM offline")):
        result = generate_jd("Java Backend Developer", experience_level="Entry Level")
        assert result is not None
        assert "required_skills" in result
        assert "preferred_skills" in result
        assert "role_family" in result
        assert result["role_family"] == "java_backend"
        assert 8 <= len(result["required_skills"]) <= 10
        assert 5 <= len(result["preferred_skills"]) <= 6
        assert "OOP" in result["required_skills"] or "Object-Oriented Programming" in result["required_skills"]
        assert "Kubernetes" not in result["required_skills"]


# ---------------------------------------------------------------------------
# 9. ATS Match Weighting: Required (80%) vs Preferred (20%)
# ---------------------------------------------------------------------------
def test_ats_match_weighting_required_vs_preferred():
    """
    Verifies that:
    1. A candidate with 100% required skills but 0% preferred skills gets 80% of the skill component.
    2. Missing preferred skills do NOT count as hard gaps in missing_skills.
    3. A candidate with 100% required + 100% preferred gets 100% of the skill component.
    4. A job without preferred skills allocates 100% of skill weight to required skills.
    """
    required_skills = ["Java", "Spring Boot", "SQL", "Git", "REST APIs"]
    preferred_skills = ["Docker", "AWS", "Kafka"]

    # Candidate 1: Has all required skills, none of the preferred skills
    resume_candidate_1 = ["Java", "Spring Boot", "SQL", "Git", "REST APIs"]
    result_1 = calculate_match_score(
        resume_skills=resume_candidate_1,
        job_skills=required_skills,
        nice_to_have_skills=preferred_skills,
    )

    # Missing preferred skills must NOT be in missing_skills!
    assert result_1["missing_skills"] == []
    # Matched skills should contain all 5 required skills
    assert len(result_1["matched_skills"]) == 5
    # Required skill score is 100, preferred skill score is 0
    assert result_1["score_breakdown"]["required_skill_score"] == 100
    assert result_1["score_breakdown"]["preferred_skill_score"] == 0
    # Composite score with 80/20 skill weighting: 100% required matches gives 85 without exp/role fit
    assert result_1["match_score"] == 85

    # Candidate 2: Has all required skills AND all preferred skills
    resume_candidate_2 = ["Java", "Spring Boot", "SQL", "Git", "REST APIs", "Docker", "AWS", "Kafka"]
    result_2 = calculate_match_score(
        resume_skills=resume_candidate_2,
        job_skills=required_skills,
        nice_to_have_skills=preferred_skills,
    )
    assert result_2["match_score"] == 100
    assert result_2["score_breakdown"]["required_skill_score"] == 100
    assert result_2["score_breakdown"]["preferred_skill_score"] == 100

    # Legacy Job: No preferred skills provided
    result_legacy = calculate_match_score(
        resume_skills=resume_candidate_1,
        job_skills=required_skills,
        nice_to_have_skills=None,
    )
    # Full 100% weight goes to required skills
    assert result_legacy["match_score"] == 100
    assert result_legacy["score_breakdown"]["preferred_skill_score"] is None
