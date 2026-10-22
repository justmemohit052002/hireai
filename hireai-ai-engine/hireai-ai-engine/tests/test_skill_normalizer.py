"""
Unit tests for skill_normalizer.py module.
Validates normalization, alias resolution, parenthetical handling,
requirement decomposition, token boundary checks, and resume grounding.
"""

import pytest
from app.skill_normalizer import (
    clean_skill_string,
    decompose_requirement,
    extract_skills_from_text,
    filter_grounded_skills,
    find_skill_evidence,
    find_skill_verbatim_evidence,
    get_alias_map,
    get_category_map,
    get_specific_tools,
    is_category_requirement,
    normalize_skill,
    repair_and_clean_job_skills,
    split_outside_parens,
    token_matches,
)


def test_clean_skill_string_prefixes_and_suffixes():
    """Validates stripping of common recruiter/candidate filler text."""
    cases = [
        ("proficient in Python", "Python"),
        ("Proficiency in Java", "Java"),
        ("hands-on experience with Docker", "Docker"),
        ("Hands on experience in Kubernetes", "Kubernetes"),
        ("strong experience in C++", "C++"),
        ("solid understanding of DSA", "DSA"),
        ("prior experience with AWS", "AWS"),
        ("demonstrated experience with React", "React"),
        ("knowledge of PostgreSQL", "PostgreSQL"),
        ("working knowledge of Linux", "Linux"),
        ("familiar with Git", "Git"),
        ("Spring Boot framework", "Spring Boot"),
        ("Java programming language", "Java"),
        ("Microservices Architecture", "Microservices"),
        ("JavaScript technologies", "JavaScript"),
        ("Python libraries", "Python"),
        ("MERN stack", "MERN"),
        ("", ""),
        (None, ""),
    ]
    for raw, expected in cases:
        assert clean_skill_string(raw) == expected


def test_normalize_skill_aliases_and_cases():
    """Validates canonical mapping for various spellings and tech aliases."""
    cases = [
        ("k8s", "Kubernetes"),
        ("Kubernetes", "Kubernetes"),
        ("Postgres", "PostgreSQL"),
        ("postgresql", "PostgreSQL"),
        ("NodeJS", "Node.js"),
        ("node.js", "Node.js"),
        ("cpp", "C++"),
        ("C++", "C++"),
        ("c#", "C#"),
        (".net", ".NET"),
        ("ci/cd", "CI/CD"),
        ("DSA", "Data structures and algorithms"),
        ("OOP", "Object-oriented programming"),
        ("DBMS", "Database management systems"),
        ("Software Architecture", "architecture ownership"),
        ("Java (Core Java, Java 8+)", "Java"),
        ("FastAPI (REST API)", "fastapi"),
        ("", ""),
        (None, ""),
    ]
    for raw, expected in cases:
        assert normalize_skill(raw).lower() == expected.lower()


def test_split_outside_parens():
    """Validates splitting on commas/semicolons without splitting parenthetical examples."""
    s1 = "Version control systems (e.g. Git, SVN), Spring Framework experience, Azure"
    assert split_outside_parens(s1) == [
        "Version control systems (e.g. Git, SVN)",
        "Spring Framework experience",
        "Azure",
    ]

    s2 = "Java; Python; Go"
    assert split_outside_parens(s2) == ["Java", "Python", "Go"]

    s3 = "Database systems (e.g. MySQL, PostgreSQL, MongoDB)"
    assert split_outside_parens(s3) == [
        "Database systems (e.g. MySQL, PostgreSQL, MongoDB)"
    ]

    s4 = "Nested (A (B, C), D), Standalone"
    assert split_outside_parens(s4) == ["Nested (A (B, C), D)", "Standalone"]

    assert split_outside_parens("") == []
    assert split_outside_parens(None) == []


def test_repair_and_clean_job_skills():
    """Validates multi-part parenthetical splits and stray paren balancing."""
    # 2-part split
    input_2 = ["Database management systems (e.g. MySQL", "MongoDB)"]
    assert repair_and_clean_job_skills(input_2) == [
        "Database management systems (e.g. MySQL, MongoDB)"
    ]

    # 3-part split
    input_3 = ["Database systems (e.g. MySQL", "PostgreSQL", "MongoDB)"]
    assert repair_and_clean_job_skills(input_3) == [
        "Database systems (e.g. MySQL, PostgreSQL, MongoDB)"
    ]

    # Stray parens
    input_stray = ["Java)", "(Python", "AWS (EC2"]
    assert repair_and_clean_job_skills(input_stray) == [
        "Java",
        "Python",
        "AWS (EC2)",
    ]


def test_decompose_requirement():
    """Validates decomposing concept, parenthetical examples, and alternative branches."""
    # Parenthetical example (platforms is cleaned as filler)
    decomp1 = decompose_requirement("Cloud computing platforms (e.g. AWS, GCP)")
    assert "Cloud computing" in decomp1["base_concept"]
    assert "AWS" in decomp1["examples"]
    assert "GCP" in decomp1["examples"]

    # Alternatives with /
    decomp2 = decompose_requirement("PostgreSQL / MySQL")
    assert "PostgreSQL" in decomp2["alternatives"]
    assert "MySQL" in decomp2["alternatives"]

    # Protected compound skill with / (must NOT split into CI and CD)
    decomp3 = decompose_requirement("CI/CD")
    assert decomp3["alternatives"] == []
    assert decomp3["base_concept"] == "CI/CD"

    # Bundled requirement
    decomp4 = decompose_requirement("Git, Docker, Kubernetes")
    assert decomp4["is_bundled"] is True
    assert decomp4["bundled_items"] == ["Git", "Docker", "Kubernetes"]


def test_token_matches_strict_boundaries():
    """Validates strict token matching to prevent false positives."""
    # Java vs JavaScript
    assert token_matches("java", "Proficient in Java and Python") is True
    assert token_matches("java", "Proficient in JavaScript") is False

    # SQL vs NoSQL
    assert token_matches("sql", "Experience with PostgreSQL and SQL") is True
    assert token_matches("sql", "Experience with NoSQL and MongoDB") is False

    # Go vs MongoDB / idioms
    assert token_matches("go", "Skilled in Go and Python") is True
    assert token_matches("go", "Skilled in MongoDB") is False
    assert token_matches("go", "Skilled in Django") is False
    assert token_matches("go", "Skilled in Algorithm design") is False
    assert token_matches("go", "He was the go-to person on the team") is False

    # R language vs R&D
    assert token_matches("r", "Data science with R and Python") is True
    assert token_matches("r", "Worked in R&D department") is False
    assert token_matches("r", "Worked in R & D") is False

    # C language vs (c) copyright
    assert token_matches("c", "Systems programming in C and C++") is True
    assert token_matches("c", "Copyright (c) 2026 Acme Corp") is False

    # Punctuation tech
    assert token_matches("c++", "C++ Developer") is True
    assert token_matches("c#", "C# / .NET Developer") is True
    assert token_matches(".net", "C# / .NET Developer") is True


def test_extract_skills_from_text():
    """Validates deterministic dictionary skill extraction from plain text."""
    text = """
    Software Engineer with 4 years experience in Java, Spring Boot, MySQL, Docker, and AWS.
    Built frontend using React.js and Tailwind CSS. Familiar with Git, CI/CD pipelines.
    """
    extracted = [s.lower() for s in extract_skills_from_text(text)]
    assert "java" in extracted
    assert "spring boot" in extracted
    assert "mysql" in extracted
    assert "docker" in extracted
    assert any("react" in s for s in extracted)
    assert "git" in extracted
    assert "ci/cd" in extracted
    # Must NOT extract skills not present
    assert "kubernetes" not in extracted
    assert "ruby" not in extracted


def test_find_skill_evidence_and_sections():
    """Validates evidence section tagging and verbatim snippet location."""
    resume_text = """
    ## Summary
    Full stack developer passionate about distributed systems.
    
    ## Experience
    Developed scalable microservices using Java and Spring Boot.
    Managed deployments on AWS EC2 instances.
    
    ## Projects
    HireAI: Automated recruitment platform built with Python, FastAPI, and PostgreSQL.
    
    ## Skills
    Java, Python, Spring Boot, PostgreSQL, Docker, Git
    """

    # Java evidence sections
    ev_java = find_skill_evidence("Java", resume_text)
    assert "Skills" in ev_java
    assert "Experience" in ev_java

    # Verbatim snippet for FastAPI
    is_found, snippet, section = find_skill_verbatim_evidence("FastAPI", resume_text)
    assert is_found is True
    assert "FastAPI" in snippet
    assert section == "project"


def test_filter_grounded_skills_with_aliases_and_parens():
    """Validates that candidate skills with annotations remain grounded when base appears."""
    resume_text = """
    TECHNICAL SKILLS
    Languages: Java, Python, SQL
    Databases: PostgreSQL
    Tools: Git, AWS (EC2)
    """
    candidate_skills = [
        "Java (Core Java, Java 8+)",
        "Postgres",
        "AWS",
        "Docker",  # Not in resume
    ]

    grounded = filter_grounded_skills(candidate_skills, resume_text)
    assert "Java (Core Java, Java 8+)" in grounded
    assert "Postgres" in grounded
    assert "AWS" in grounded
    assert "Docker" not in grounded


def test_is_category_requirement():
    """Validates distinction between broad categories and specific technologies."""
    # Broad categories
    is_cat, cat = is_category_requirement("Database management systems (e.g. MySQL)")
    assert is_cat is True
    assert "database" in cat

    is_cat2, cat2 = is_category_requirement("Version control systems (e.g. Git)")
    assert is_cat2 is True
    assert "version control" in cat2

    # Specific tools in specific_tools must NOT be category requirements
    is_cat3, _ = is_category_requirement("Docker")
    assert is_cat3 is False

    is_cat4, _ = is_category_requirement("Kubernetes")
    assert is_cat4 is False

    is_cat5, _ = is_category_requirement("Spring Boot")
    assert is_cat5 is False

    is_cat6, _ = is_category_requirement("TypeScript")
    assert is_cat6 is False
