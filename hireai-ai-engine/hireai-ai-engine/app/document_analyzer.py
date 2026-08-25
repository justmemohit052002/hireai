"""
HireAI AI Engine - Document Analyzer & Testing Hub.

Enables interactive LLM model testing on uploaded documents:
- Executive Document Analysis & Classification
- Document Q&A (Context-grounded answering)
- Custom Prompt Sandbox & Evaluation
"""

import time
from typing import Dict, Any
from app.llm_client import call_llm
from app.utils import extract_json


DOCUMENT_ANALYSIS_PROMPT = """You are an advanced AI document analyzer for HireAI recruitment and enterprise platform.
Analyze the following document text thoroughly and return a valid JSON object ONLY.

Document Text:
\"\"\"{document_text}\"\"\"

Return a single JSON object with EXACTLY this structure:
{{
    "document_category": "Resume | Job Description | Technical Specification | Policy | Interview Transcript | Other",
    "title_or_subject": "Brief subject or candidate/job title",
    "executive_summary": "2-3 sentence high-level summary of the document",
    "key_highlights": [
        "Key point 1",
        "Key point 2",
        "Key point 3"
    ],
    "identified_skills_and_technologies": [
        "Skill 1",
        "Skill 2"
    ],
    "key_entities": [
        "Companies, institutions, or notable names mentioned"
    ],
    "strengths_or_pros": [
        "Identified strength or positive highlight"
    ],
    "potential_red_flags_or_gaps": [
        "Missing info, gaps, or areas needing clarification"
    ]
}}

No markdown explanation, no preamble, output valid JSON only.
"""

DOCUMENT_QA_PROMPT = """You are an intelligent assistant analyzing a specific document.
Answer the user's question accurately and strictly based on the provided document context.
If the answer cannot be found in the document, state clearly that the document does not contain that information.

DOCUMENT CONTEXT:
\"\"\"{document_text}\"\"\"

USER QUESTION:
{question}

ANSWER:"""


def analyze_document(document_text: str) -> Dict[str, Any]:
    """
    Runs comprehensive structured analysis on the provided document using the LLM.
    """
    # Truncate text if excessively long to prevent token overflow for local models
    max_chars = 12000
    trimmed_text = document_text[:max_chars]

    prompt = DOCUMENT_ANALYSIS_PROMPT.format(document_text=trimmed_text)
    raw_response = call_llm(prompt, task_type="document_analysis")

    parsed = extract_json(raw_response)
    if parsed is not None:
        return parsed

    # Fallback structure if LLM returned freeform text instead of JSON
    return {
        "document_category": "Analyzed Document",
        "title_or_subject": "Document Summary",
        "executive_summary": raw_response.strip()[:600],
        "key_highlights": ["See full summary above"],
        "identified_skills_and_technologies": [],
        "key_entities": [],
        "strengths_or_pros": [],
        "potential_red_flags_or_gaps": [],
        "raw_response": raw_response,
    }


def ask_document_question(document_text: str, question: str) -> str:
    """
    Answers a user question grounded in the document context.
    """
    max_chars = 12000
    trimmed_text = document_text[:max_chars]

    prompt = DOCUMENT_QA_PROMPT.format(document_text=trimmed_text, question=question)
    return call_llm(prompt, task_type="document_qa")


def run_custom_prompt_test(document_text: str, custom_prompt: str) -> Dict[str, Any]:
    """
    Executes a custom prompt against the document text for testing LLM model behaviors.
    """
    if "{document_text}" in custom_prompt:
        final_prompt = custom_prompt.replace("{document_text}", document_text)
    else:
        final_prompt = f"{custom_prompt}\n\n--- DOCUMENT CONTENT ---\n{document_text}"

    start_time = time.time()
    response_text = call_llm(final_prompt, task_type="custom_prompt_test")
    latency = round(time.time() - start_time, 2)

    return {
        "prompt": final_prompt,
        "response": response_text,
        "latency_seconds": latency,
    }
