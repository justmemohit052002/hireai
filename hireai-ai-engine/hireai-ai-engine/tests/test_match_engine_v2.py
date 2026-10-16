"""
Unit and integration tests for Match Engine v2.
Validates regression fixture, anti-false-positive boundaries, alias resolution,
requirement decomposition, edge cases, input limits, and contract serialization.
"""

import pytest
from fastapi.testclient import TestClient

from app.match_engine import calculate_match_score
from app.config import INTERNAL_API_KEY
from main import app


@pytest.fixture
def client():
    return TestClient(app)


@pytest.fixture
def auth_headers():
    return {"X-Internal-Key": INTERNAL_API_KEY}


def test_regression_fixture_real_case():
    """
    Candidate with standard full-stack Java background applying to 8 requirements.
    Expected: 7 of 8 matched, only 'MongoDB' missing, score >= 85, autoAction == 'shortlist'.
    """
    resume_skills = [
        "Java (Core Java, Java 8+)", "JavaScript", "SQL", "Spring Boot", "Spring MVC",
        "Spring Security", "Spring Data JPA", "Hibernate", "JDBC", "REST APIs",
        "JWT", "OAuth", "MySQL", "PostgreSQL", "React.js", "HTML5", "CSS3",
        "Tailwind CSS", "JUnit", "Mockito", "API Testing", "Git", "GitHub",
        "Maven", "Postman", "Swagger/OpenAPI", "AWS (EC2)", "IntelliJ IDEA",
        "VS Code", "OOP", "Data Structures and Algorithms (DSA)", "DBMS",
        "Operating Systems", "Collections", "Streams", "Exception Handling",
        "Multithreading", "Microservices Architecture", "Design Patterns",
        "SDLC", "Agile"
    ]

    job_skills = [
        "Java programming language",
        "Cloud computing platforms (e.g. AWS)",
        "Java-based testing frameworks (e.g. JUnit)",
        "Object-Oriented Programming (OOP) concepts",
        "Data structures and algorithms",
        "Database management systems (e.g. MySQL)",
        "MongoDB",
        "Version control systems (e.g. Git)"
    ]

    result = calculate_match_score(resume_skills, job_skills)

    assert len(result["matched_skills"]) == 7
    assert result["missing_skills"] == ["MongoDB"]
    assert result["match_score"] >= 85
    assert result["auto_action"] == "shortlist"
    assert "Java programming language" in result["matched_skills"]
    assert "Cloud computing platforms (e.g. AWS)" in result["matched_skills"]
    assert "Java-based testing frameworks (e.g. JUnit)" in result["matched_skills"]
    assert "Object-Oriented Programming (OOP) concepts" in result["matched_skills"]
    assert "Data structures and algorithms" in result["matched_skills"]
    assert "Database management systems (e.g. MySQL)" in result["matched_skills"]
    assert "Version control systems (e.g. Git)" in result["matched_skills"]


def test_anti_false_positive_word_boundaries():
    """
    Ensures strict token boundaries prevent raw substring false positives:
    - Job 'Java' must NOT match resume ['JavaScript']
    - Job 'Go' must NOT match resume ['MongoDB']
    - Job 'SQL' must NOT match resume ['NoSQL', 'MongoDB']
    """
    res1 = calculate_match_score(["JavaScript"], ["Java"])
    assert "Java" in res1["missing_skills"]
    assert res1["match_score"] == 0

    res2 = calculate_match_score(["MongoDB"], ["Go"])
    assert "Go" in res2["missing_skills"]
    assert res2["match_score"] == 0

    res3 = calculate_match_score(["NoSQL", "MongoDB"], ["SQL"])
    assert "SQL" in res3["missing_skills"]
    assert res3["match_score"] == 0


def test_alias_canonical_resolution():
    """Validates alias dictionary mapping across tech stacks."""
    cases = [
        (["DSA"], ["Data structures and algorithms"]),
        (["OOP"], ["Object-oriented programming"]),
        (["OOPS"], ["Object-oriented programming"]),
        (["DBMS"], ["Database management systems"]),
        (["Postgres"], ["PostgreSQL"]),
        (["K8s"], ["Kubernetes"]),
        (["Node.js"], ["NodeJS"]),
        (["NodeJS"], ["Node.js"]),
        (["cpp"], ["C++"]),
        (["C++"], ["cpp"]),
        (["REST"], ["RESTful APIs"]),
    ]

    for resume, job in cases:
        out = calculate_match_score(resume, job)
        assert len(out["matched_skills"]) == 1, f"Failed matching {resume} to {job}"
        assert out["missing_skills"] == [], f"Expected 0 missing for {resume} to {job}"


def test_requirement_decomposition():
    """
    Decomposes descriptive concepts and alternatives:
    - 'Cloud computing platforms (e.g. AWS)' matched by ['EC2'] or ['AWS']
    - 'PostgreSQL / MySQL' matched by either one
    """
    res1 = calculate_match_score(["EC2"], ["Cloud computing platforms (e.g. AWS)"])
    assert len(res1["matched_skills"]) == 1

    res2 = calculate_match_score(["MySQL"], ["PostgreSQL / MySQL"])
    assert len(res2["matched_skills"]) == 1

    res3 = calculate_match_score(["PostgreSQL"], ["PostgreSQL / MySQL"])
    assert len(res3["matched_skills"]) == 1


def test_edge_cases_robustness():
    """Validates empty inputs, blanks, duplicates, unicode, and determinism."""
    # Empty job skills
    res_empty_job = calculate_match_score(["Java", "SQL"], [])
    assert res_empty_job["matched_skills"] == []
    assert "Java" in res_empty_job["bonus_skills"]

    # Empty resume skills
    res_empty_resume = calculate_match_score([], ["Java", "SQL"])
    assert res_empty_resume["match_score"] == 0
    assert res_empty_resume["auto_action"] == "reject"
    assert len(res_empty_resume["missing_skills"]) == 2

    # Duplicates & blanks
    res_dups = calculate_match_score(["Java", "  ", "Java", "SQL"], ["Java", "SQL", ""])
    assert len(res_dups["matched_skills"]) == 2
    assert len(res_dups["missing_skills"]) == 0

    # Determinism: identical runs give identical outputs
    run_a = calculate_match_score(["Java", "Git"], ["Java", "Python"])
    run_b = calculate_match_score(["Java", "Git"], ["Java", "Python"])
    assert run_a == run_b


def test_api_contract_and_model_serialization(client, auth_headers):
    """
    Validates API endpoint POST /api/v1/match/score:
    - Accepts camelCase payload
    - Returns exact camelCase keys required by API contract
    - bonusSkills is retained (not stripped)
    """
    payload = {
        "resumeSkills": ["Java", "Spring Boot", "Git", "Docker"],
        "jobSkills": ["Java", "Kubernetes"],
        "niceToHaveSkills": ["Docker"],
        "candidateExperienceYears": 3.0,
        "requiredExperienceYears": 2.0
    }

    response = client.post("/api/v1/match/score", json=payload, headers=auth_headers)
    assert response.status_code == 200

    data = response.json()
    assert "matchScore" in data
    assert "autoAction" in data
    assert "matchedSkills" in data
    assert "missingSkills" in data
    assert "bonusSkills" in data
    assert "matchDetails" in data
    assert "skillCoveragePercent" in data
    assert "experienceMatchPercent" in data
    assert "scoreBreakdown" in data
    assert "rationale" in data
    assert "engineVersion" in data
    assert "semanticModelAvailable" in data

    # Verify bonusSkills retained
    assert "Spring Boot" in data["bonusSkills"]
    assert "Git" in data["bonusSkills"]
    assert data["matchedSkills"] == ["Java"]
    assert data["missingSkills"] == ["Kubernetes"]


def test_input_validation_oversized(client, auth_headers):
    """Oversized inputs (e.g. > 120 chars per skill) must return 422 Unprocessable Entity."""
    oversized_skill = "A" * 150
    payload = {
        "resumeSkills": [oversized_skill],
        "jobSkills": ["Java"]
    }

    response = client.post("/api/v1/match/score", json=payload, headers=auth_headers)
    assert response.status_code == 422


def test_fallback_when_embedding_model_disabled(monkeypatch):
    """Model-dependent calculations must still pass deterministically when semantic model is unavailable."""
    from app import match_engine
    monkeypatch.setattr(match_engine, "get_embedding_model", lambda: None)
    monkeypatch.setattr(match_engine, "is_semantic_model_available", lambda: False)

    res = match_engine.calculate_match_score(["Java", "Git"], ["Java programming language", "Git"])
    assert len(res["matched_skills"]) == 2
    assert res["match_score"] == 100
    assert res["semantic_model_available"] is False


def test_real_candidate_resume_match():
    """
    Validates exact sample resume against the real job requirements:
    - Core Java, OOP, DSA, DBMS (MySQL, PostgreSQL), Git, Spring Framework, Agile are MATCHED.
    - Score reflects strong coverage well above 70% threshold.
    """
    resume_skills = [
        "Core Java", "OOP", "Collections", "Multithreading", "Spring Boot",
        "Spring MVC", "Spring Security", "Spring Data JPA", "Hibernate", "REST APIs",
        "JWT", "OAuth", "MySQL", "PostgreSQL", "React.js", "JUnit", "Mockito",
        "Git", "GitHub", "Maven", "Postman", "AWS (EC2)", "DSA", "DBMS", "OS",
        "Microservices", "Design Patterns", "SDLC", "Agile Methodologies"
    ]

    job_skills = [
        "Java programming language",
        "Cloud computing platforms (e.g. AWS)",
        "Java-based testing frameworks (e.g. JUnit)",
        "Object-Oriented Programming (OOP) concepts",
        "Data structures and algorithms",
        "Database management systems (e.g. MySQL, MongoDB)",
        "Version control systems (e.g. Git, SVN)",
        "Spring Framework experience",
        "Agile development methodologies (e.g. Scrum, Kanban)"
    ]

    result = calculate_match_score(resume_skills, job_skills)

    assert result["match_score"] >= 90
    assert result["auto_action"] == "shortlist"
    assert len(result["missing_skills"]) == 0
    assert "Java programming language" in result["matched_skills"]
    assert "Object-Oriented Programming (OOP) concepts" in result["matched_skills"]
    assert "Data structures and algorithms" in result["matched_skills"]
    assert "Database management systems (e.g. MySQL, MongoDB)" in result["matched_skills"]
    assert "Version control systems (e.g. Git, SVN)" in result["matched_skills"]
    assert "Spring Framework experience" in result["matched_skills"]
    assert "Agile development methodologies (e.g. Scrum, Kanban)" in result["matched_skills"]


def test_bundled_requirements_splitting_and_partial_credit():
    """
    When a requirement bundles multiple skills ('Git, SVN, Spring Framework, Azure'),
    candidate with Git and Spring Boot matches those two, while 'Azure' is identified as the only gap.
    """
    resume_skills = ["Git", "Spring Boot", "MySQL"]
    job_skills = [
        "Version control systems (e.g. Git, SVN), Spring Framework experience, Azure"
    ]

    result = calculate_match_score(resume_skills, job_skills)

    # 2 of 3 sub-skills matched -> ~67% credit
    assert result["match_score"] >= 60
    assert "Azure" in result["missing_skills"]
    # Neither Git nor Spring Framework should be in missing skills
    assert "Version control systems (e.g. Git, SVN)" not in result["missing_skills"]
    assert "Spring Framework experience" not in result["missing_skills"]


def test_repaired_broken_parentheses_from_naive_splits():
    """
    When naive comma splitting previously broke 'Database management systems (e.g. MySQL, MongoDB)',
    into two items with unmatched parens, repair_and_clean_job_skills merges them.
    """
    resume_skills = ["MySQL", "PostgreSQL", "Java"]
    broken_job_skills = [
        "Database management systems (e.g. MySQL",
        "MongoDB)"
    ]

    result = calculate_match_score(resume_skills, broken_job_skills)
    assert result["match_score"] >= 90
    assert len(result["missing_skills"]) == 0


def test_negative_case_unrelated_resume():
    """A resume with unrelated skills (e.g. Graphic Designer) must score 0% against technical Java requirements."""
    resume_skills = ["Photoshop", "Illustrator", "Figma", "Typography"]
    job_skills = [
        "Java programming language",
        "Spring Boot",
        "PostgreSQL",
        "Docker",
        "Kubernetes"
    ]

    result = calculate_match_score(resume_skills, job_skills)
    assert result["match_score"] == 0
    assert result["auto_action"] == "reject"
    assert len(result["missing_skills"]) == 5


def test_mohit_backend_developer_real_case_no_false_matches():
    """
    Verifies the real evidence from the user prompt:
    Mohit's resume PDF contains:
    Java, JavaScript, SQL, Spring Boot, Spring MVC, Spring Security, Spring Data JPA,
    Hibernate, JDBC, REST APIs, JWT, OAuth, MySQL, PostgreSQL, React.js, JUnit,
    Mockito, Git, GitHub, Maven, Postman, AWS, Python (in HireAI project), FastAPI,
    Microservices, DSA, OOP, etc.
    The resume does NOT contain: Docker, Kubernetes, TypeScript.
    
    Job: 'Backend Developer'
    Required: Java, Python, JavaScript, SQL, Git, Docker, AWS, Kubernetes, TypeScript, RESTful APIs, Microservices
    
    MUST NOT match: Docker, Kubernetes, TypeScript in matched_skills.
    TypeScript may have related credit under related_skills (via JavaScript), but NOT in matched_skills.
    Docker and Kubernetes MUST be in missing_skills.
    Total matched: exactly 8 skills (Java, Python, JavaScript, SQL, Git, AWS, RESTful APIs, Microservices).
    Match score must not be 100%, and auto_action must NOT be shortlist if under threshold.
    """
    resume_skills = [
        "Core Java", "OOP", "Collections", "Multithreading", "Spring Boot",
        "Spring MVC", "Spring Security", "Spring Data JPA", "Hibernate", "REST APIs",
        "JWT", "OAuth", "MySQL", "PostgreSQL", "React.js", "JUnit", "Mockito",
        "Git", "GitHub", "Maven", "Postman", "AWS (EC2)", "DSA", "DBMS", "OS",
        "Microservices", "Design Patterns", "SDLC", "Agile Methodologies",
        "Python", "FastAPI", "JavaScript", "SQL"
    ]
    
    resume_text = """
    MOHIT SINGH CHOUHAN
    Java Developer | Spring Boot | REST APIs
    TECHNICAL SKILLS
    Languages: Java (Core Java, Java 8+), JavaScript, SQL
    Backend: Spring Boot, Spring MVC, Spring Security, Spring Data JPA, Hibernate, JDBC, REST APIs, JWT, OAuth
    Databases: MySQL, PostgreSQL
    Frontend: React.js, HTML5, CSS3, Tailwind CSS
    Testing: JUnit, Mockito, API Testing
    Tools: Git, GitHub, Maven, Postman, Swagger/OpenAPI, AWS (EC2), IntelliJ IDEA, VS Code
    Concepts: OOP, Data Structures & Algorithms (DSA), DBMS, OS, Collections, Streams, Exception Handling, Multithreading,
    Microservices Architecture, Design Patterns, SDLC, Agile Methodologies
    PROJECTS
    HireAI | Java, Spring Boot, Spring Security, PostgreSQL, React.js, Tailwind CSS, Python, FastAPI, LLM, AWS Jul 2026
    """

    job_skills = [
        "Java", "Python", "JavaScript", "SQL", "Git", "Docker", "AWS",
        "Kubernetes", "TypeScript", "RESTful APIs", "Microservices"
    ]

    result = calculate_match_score(
        resume_skills=resume_skills,
        job_skills=job_skills,
        resume_text=resume_text,
        candidate_experience_years=3.5,
        required_experience_years=3.0,
        candidate_role="Java Developer",
        job_title="Backend Developer"
    )

    # 1. Verify exact 8 matched skills
    expected_matches = {"Java", "Python", "JavaScript", "SQL", "Git", "AWS", "RESTful APIs", "Microservices"}
    assert set(result["matched_skills"]) == expected_matches

    # 2. Strict anti-false-positives: Docker, Kubernetes, TypeScript MUST NOT be in matched_skills
    assert "Docker" not in result["matched_skills"]
    assert "Kubernetes" not in result["matched_skills"]
    assert "TypeScript" not in result["matched_skills"]

    # 3. Docker, Kubernetes, TypeScript MUST be in missing_skills
    assert "Docker" in result["missing_skills"]
    assert "Kubernetes" in result["missing_skills"]
    assert "TypeScript" in result["missing_skills"]

    # 4. Related skills list records TypeScript via JavaScript with partial credit
    assert any("TypeScript" in r and "JavaScript" in r for r in result["related_skills"])

    # 5. Score is explainable and strictly not 100%
    assert result["match_score"] <= 80
    assert result["match_score"] >= 65

