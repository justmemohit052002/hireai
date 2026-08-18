"""
Module 6: Decision Engine.
Deterministic weighted formula - no LLM call. Kept as plain math on
purpose: the PRD requires every automated decision to be explainable and
auditable, and a formula is easier to defend than "the model decided."
"""

RESUME_WEIGHT = 0.4
INTERVIEW_WEIGHT = 0.3
CHATBOT_WEIGHT = 0.3

SHORTLIST_THRESHOLD = 75
REJECT_THRESHOLD = 40


def finalize_decision(resume_score: float, interview_score: float, chatbot_signal_score: float) -> dict:
    final_score = round(
        resume_score * RESUME_WEIGHT
        + interview_score * INTERVIEW_WEIGHT
        + chatbot_signal_score * CHATBOT_WEIGHT
    )

    if final_score >= SHORTLIST_THRESHOLD:
        classification = "shortlist"
        explanation = "Strong combined performance across resume, interview, and screening."
    elif final_score < REJECT_THRESHOLD:
        classification = "reject"
        explanation = "Combined score falls below the minimum threshold for this role."
    else:
        classification = "hold"
        explanation = "Mixed signals - recommended for manual recruiter review."

    return {
        "final_score": final_score,
        "classification": classification,
        "breakdown": {
            "resumeScore": resume_score,
            "resumeWeight": RESUME_WEIGHT,
            "interviewScore": interview_score,
            "interviewWeight": INTERVIEW_WEIGHT,
            "chatbotSignalScore": chatbot_signal_score,
            "chatbotWeight": CHATBOT_WEIGHT,
        },
        "explanation": explanation,
    }
