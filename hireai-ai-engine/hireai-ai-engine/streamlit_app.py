"""
HireAI AI Engine - Streamlit Interactive LLM Dashboard.

Run with:
    streamlit run streamlit_app.py
"""
import os
os.environ["TRANSFORMERS_VERBOSITY"] = "error"
os.environ["TOKENIZERS_PARALLELISM"] = "false"

import streamlit as st
import json
import time
from app.config import OLLAMA_MODEL, OLLAMA_BASE_URL, LLM_PROVIDER, GEMINI_API_KEY, GROQ_API_KEY, GROQ_MODEL
from app.llm_client import check_ollama_reachable, set_active_provider, get_active_provider_info
from app.document_loader import extract_document_info
from app.document_analyzer import analyze_document, ask_document_question, run_custom_prompt_test
from app.jd_generator import generate_jd
from app.resume_parser import parse_resume
from app.match_engine import calculate_match_score
from app.chatbot import handle_message
from app.interview_ai import generate_questions, evaluate_answers
from app.decision_engine import finalize_decision

# ----------------- Page Configuration -----------------
st.set_page_config(
    page_title="HireAI — AI Engine Playground",
    page_icon="🤖",
    layout="wide",
    initial_sidebar_state="expanded",
)

# ----------------- Styling -----------------
st.markdown(
    """
    <style>
    .main-title {
        font-size: 2.2rem;
        font-weight: 700;
        color: #1E3A8A;
        margin-bottom: 0.2rem;
    }
    .sub-title {
        font-size: 1rem;
        color: #64748B;
        margin-bottom: 1.5rem;
    }
    .card {
        background-color: #F8FAFC;
        padding: 1.2rem;
        border-radius: 10px;
        border: 1px solid #E2E8F0;
        margin-bottom: 1rem;
    }
    .doc-stat-card {
        background-color: #FFFFFF;
        padding: 0.9rem 1.2rem;
        border-radius: 8px;
        border: 1px solid #E2E8F0;
        box-shadow: 0 1px 3px rgba(0,0,0,0.05);
        text-align: center;
    }
    .badge {
        display: inline-block;
        padding: 0.25rem 0.6rem;
        border-radius: 9999px;
        font-size: 0.8rem;
        font-weight: 600;
    }
    .badge-primary { background-color: #DBEAFE; color: #1D4ED8; }
    .badge-success { background-color: #DCFCE7; color: #166534; }
    .badge-warning { background-color: #FEF3C7; color: #92400E; }
    .badge-danger { background-color: #FEE2E2; color: #991B1B; }
    .badge-info { background-color: #E0E7FF; color: #3730A3; }
    .chip {
        display: inline-block;
        background-color: #F1F5F9;
        color: #334155;
        padding: 0.25rem 0.6rem;
        margin: 0.2rem;
        border-radius: 6px;
        font-size: 0.85rem;
        font-weight: 500;
        border: 1px solid #CBD5E1;
    }
    .stButton>button {
        border-radius: 8px;
        font-weight: 600;
    }
    </style>
    """,
    unsafe_allow_html=True,
)

# ----------------- Sample Documents -----------------
SAMPLE_RESUME_DOC = """ALEX MORGAN
Email: alex.morgan@techmail.io | Phone: +1 (555) 432-8765 | Location: San Francisco, CA
LinkedIn: linkedin.com/in/alexmorgan-dev | GitHub: github.com/alexm-code

EXECUTIVE SUMMARY:
Principal Software Architect and Full Stack Lead with 8+ years of experience designing high-throughput microservices, distributed cloud architectures, and AI-assisted workflows. Led cross-functional teams of 12+ engineers to deliver scalable enterprise platforms.

TECHNICAL SKILLS:
- Core Languages: Java 17/21, Python 3.11, TypeScript, SQL
- Frameworks: Spring Boot 3.x, FastAPI, Next.js, Angular, PyTorch
- Cloud & DevOps: AWS (EKS, Lambda, S3, RDS), Docker, Kubernetes, Terraform, GitHub Actions, Helm
- Databases & Messaging: PostgreSQL, MongoDB, Redis, Apache Kafka, RabbitMQ
- Testing & Tools: JUnit 5, Mockito, Testcontainers, Postman, Datadog, Prometheus

PROFESSIONAL EXPERIENCE:
Lead Solutions Architect — NextGen FinTech Solutions (2021 – Present)
- Architected event-driven microservices platform processing $40M+ in daily transaction volume with 99.99% availability.
- Mentored 8 software engineers on Spring Boot best practices, TDD, and modular architecture.
- Optimized PostgreSQL queries and introduced Redis caching layer, slashing P99 API latency from 450ms to 42ms.
- Spearheaded Kubernetes migration on AWS EKS reducing infrastructure hosting costs by 28%.

Senior Backend Engineer — CloudScale Labs (2018 – 2021)
- Built resilient RESTful and gRPC APIs powering enterprise analytics dashboard.
- Implemented real-time streaming pipeline using Apache Kafka and Spring Cloud Stream.
- Automated CI/CD deployment pipelines using GitLab CI, Docker, and AWS CodeDeploy.

EDUCATION & CERTIFICATIONS:
- Master of Science in Computer Science — Stanford University (2016 – 2018)
- Bachelor of Science in Information Technology — University of Michigan (2012 – 2016)
- AWS Certified Solutions Architect – Professional (2023)
"""

SAMPLE_JD_DOC = """JOB DESCRIPTION: Senior Full Stack Java / AI Platform Engineer
Location: Remote (US / Canada)
Department: Core Engineering
Employment Type: Full-Time

ROLE OVERVIEW:
We are seeking an experienced Senior Full Stack Java Engineer to lead the design and development of our next-generation AI-powered recruitment engine. You will build high-performance microservices, integrate cutting-edge LLMs and vector embeddings, and collaborate with cross-functional product teams.

KEY RESPONSIBILITIES:
- Architect, build, and maintain robust microservices using Java 17/21, Spring Boot, and PostgreSQL.
- Develop and integrate REST/gRPC APIs with Python-based AI engine and local LLM services.
- Design scalable event-driven messaging pipelines using Apache Kafka.
- Containerize and deploy services using Docker and Kubernetes on AWS.
- Participate in code reviews, architectural planning, and mentoring junior/mid-level engineers.

REQUIRED QUALIFICATIONS:
- 5+ years of software engineering experience with Java and Spring Boot.
- Deep expertise in relational databases (PostgreSQL/MySQL) and SQL performance tuning.
- Hands-on experience with Docker, Kubernetes, and AWS cloud infrastructure.
- Solid understanding of distributed systems, REST APIs, and microservice design patterns.
- Strong problem-solving skills and passion for clean, testable code.

NICE TO HAVE:
- Experience integrating AI/LLM models (Ollama, HuggingFace, OpenAI, or Gemini).
- Knowledge of Vector Databases (Pinecone, ChromaDB, PGVector) or sentence-transformers.
- Experience with frontend frameworks like React or Angular.
"""

SAMPLE_POLICY_DOC = """HIREAI ENTERPRISE TALENT ACQUISITION POLICY & CANDIDATE EVALUATION CRITERIA
Document Version: 2.4 | Effective Date: August 2026

1. CANDIDATE EVALUATION MATRIX:
Every applicant undergoes a standardized multi-stage evaluation:
- Stage 1: Automated Resume Semantic Match (40% weight). Minimum threshold: 65% match score.
- Stage 2: Technical Assessment & Coding Exercise (30% weight). Evaluates algorithm efficiency, design patterns, and code maintainability.
- Stage 3: Behavioral & Culture Fit Interview (30% weight). Evaluates communication, leadership, and team collaboration.

2. DECISION THRESHOLDS:
- Shortlist / Direct Offer: Combined weighted score >= 75%
- Manual Recruiter Review (Hold): Combined weighted score between 40% and 74%
- Rejection: Combined weighted score < 40%

3. FAIR HIRING & DATA PRIVACY:
- All resume parsing and AI evaluations must be anonymized to eliminate gender, ethnic, or age bias.
- Candidates have the right to request manual recruiter review of any automated AI screening recommendation.
"""

# ----------------- Sidebar -----------------
with st.sidebar:
    st.image("https://img.icons8.com/isometric/100/artificial-intelligence.png", width=60)
    st.title("HireAI Engine")
    st.caption("Local LLM & Matching Engine Testing Dashboard")

    st.divider()

    # System & Model Status
    st.subheader("⚙️ Active LLM Engine")
    provider_options = [
        "⚡ Groq Cloud (openai/gpt-oss-20b) ~1s",
        "💻 Ollama Local (llama3.2:3b) ~8s",
        "💻 Ollama Local (llama3.1:8b) ~90s",
        "🌐 Gemini Cloud (gemini-1.5-flash) ~2s",
    ]
    default_idx = 0 if LLM_PROVIDER == "groq" else (1 if OLLAMA_MODEL == "llama3.2:3b" else 2)
    selected_option = st.selectbox(
        "Switch Model at Runtime:",
        provider_options,
        index=default_idx,
        help="Compare response speed and accuracy between Groq Cloud and Local Ollama live!",
    )

    if "Groq" in selected_option:
        set_active_provider("groq", GROQ_MODEL)
    elif "llama3.2:3b" in selected_option:
        set_active_provider("ollama", "llama3.2:3b")
    elif "llama3.1:8b" in selected_option:
        set_active_provider("ollama", "llama3.1:8b")
    elif "Gemini" in selected_option:
        set_active_provider("gemini", "gemini-1.5-flash")

    active_prov, active_mdl = get_active_provider_info()

    is_ollama_up = check_ollama_reachable()
    if active_prov == "ollama":
        if is_ollama_up:
            st.success(f"🟢 Ollama Local Online")
        else:
            st.error(f"🔴 Ollama Offline ({OLLAMA_BASE_URL})")
            st.caption("Run `ollama serve` in your terminal.")
    elif active_prov == "gemini":
        st.info("🟢 Gemini Active" if GEMINI_API_KEY else "⚠️ Gemini Key Missing")
    elif active_prov == "groq":
        st.info("🟢 Groq Active" if GROQ_API_KEY else "⚠️ Groq Key Missing")

    st.markdown(f"**Provider:** `{active_prov.upper()}`")
    st.markdown(f"**Model:** `{active_mdl}`")
    st.markdown(f"**Embeddings:** `all-MiniLM-L6-v2`")

    st.divider()
    page = st.radio(
        "Select Feature to Test:",
        [
            "📂 Document Upload & LLM Testing",
            "📄 Resume Parser",
            "📝 JD Generator",
            "🎯 Match Engine (Vector Sim)",
            "💬 AI Recruiter Chatbot",
            "🎙️ Interview AI",
            "⚖️ Decision Engine",
        ],
    )

# Header
st.markdown(f"<div class='main-title'>HireAI — AI Engine Playground</div>", unsafe_allow_html=True)
st.markdown(
    f"<div class='sub-title'>Interactive testing interface for Local LLM ({OLLAMA_MODEL}), Document Intelligence, and Vector Match Engine.</div>",
    unsafe_allow_html=True,
)

# ----------------------------------------------------
# 0. DOCUMENT UPLOAD & LLM TESTING (NEW FEATURE)
# ----------------------------------------------------
if page == "📂 Document Upload & LLM Testing":
    st.header("📂 Document Upload & LLM Model Testing Hub")
    st.caption(
        "Upload real documents (PDF, DOCX, TXT, MD, JSON) to test text extraction, prompt responses, "
        "document Q&A, and LLM classification."
    )

    # File Upload & Preset Selection
    upload_col, preset_col = st.columns([2, 1])

    with upload_col:
        uploaded_file = st.file_uploader(
            "Upload a document to test with LLM:",
            type=["pdf", "docx", "doc", "txt", "md", "json", "csv"],
            help="Supported formats: PDF, DOCX, TXT, Markdown, JSON, CSV",
        )

    with preset_col:
        st.markdown("**Or load a sample document:**")
        sample_choice = st.selectbox(
            "Quick test samples:",
            ["None", "Candidate Resume (Alex Morgan)", "Job Description (Senior Java / AI)", "Hiring Policy Document"],
        )

    # Determine Active Document Content
    doc_info = None
    if uploaded_file is not None:
        with st.spinner("Extracting text and metadata from uploaded file..."):
            doc_info = extract_document_info(uploaded_file)
    elif sample_choice == "Candidate Resume (Alex Morgan)":
        doc_info = extract_document_info(SAMPLE_RESUME_DOC.encode("utf-8"), filename="alex_morgan_resume.txt")
    elif sample_choice == "Job Description (Senior Java / AI)":
        doc_info = extract_document_info(SAMPLE_JD_DOC.encode("utf-8"), filename="senior_java_engineer_jd.txt")
    elif sample_choice == "Hiring Policy Document":
        doc_info = extract_document_info(SAMPLE_POLICY_DOC.encode("utf-8"), filename="talent_acquisition_policy.txt")

    if doc_info and doc_info.get("text"):
        st.divider()

        # Display Document Metadata Summary
        m1, m2, m3, m4, m5 = st.columns(5)
        with m1:
            st.markdown(
                f"<div class='doc-stat-card'><small>File Name</small><br><b>{doc_info['filename']}</b></div>",
                unsafe_allow_html=True,
            )
        with m2:
            ext_label = doc_info["extension"].upper() or "TEXT"
            st.markdown(
                f"<div class='doc-stat-card'><small>Format</small><br><span class='badge badge-primary'>{ext_label}</span></div>",
                unsafe_allow_html=True,
            )
        with m3:
            st.markdown(
                f"<div class='doc-stat-card'><small>Total Characters</small><br><b>{doc_info['char_count']:,}</b></div>",
                unsafe_allow_html=True,
            )
        with m4:
            st.markdown(
                f"<div class='doc-stat-card'><small>Word Count</small><br><b>{doc_info['word_count']:,}</b></div>",
                unsafe_allow_html=True,
            )
        with m5:
            st.markdown(
                f"<div class='doc-stat-card'><small>Est. Tokens</small><br><b>~{doc_info['estimated_tokens']:,}</b></div>",
                unsafe_allow_html=True,
            )

        if doc_info.get("page_count"):
            st.caption(f"📑 PDF Page Count: {doc_info['page_count']} page(s)")

        # Document Text Viewer / Editor
        with st.expander("📄 View / Edit Extracted Document Content", expanded=False):
            active_doc_text = st.text_area(
                "Document Text (You can edit before running LLM tests):",
                value=doc_info["text"],
                height=220,
                key="active_doc_text_area",
            )
        active_text = active_doc_text if "active_doc_text_area" in st.session_state else doc_info["text"]

        st.divider()
        st.subheader("🧪 Choose LLM Test Mode")

        tab_analysis, tab_qa, tab_prompt, tab_pipeline = st.tabs(
            [
                "📊 1. Executive Analysis & Entity Extraction",
                "❓ 2. Interactive Document Q&A",
                "🧪 3. Custom Prompt Sandbox",
                "🚀 4. One-Click Pipeline Routing",
            ]
        )

        # Tab 1: Executive Analysis
        with tab_analysis:
            st.markdown("##### Extract high-level insights, summary, skills, and classification from document.")
            if st.button("✨ Run Comprehensive LLM Document Analysis", type="primary", key="btn_doc_analyze"):
                with st.spinner("Analyzing document with LLM model..."):
                    start_t = time.time()
                    try:
                        analysis_res = analyze_document(active_text)
                        elapsed = round(time.time() - start_t, 2)
                        st.success(f"Analysis completed in {elapsed}s!")

                        # Render Structured Results
                        col_c1, col_c2 = st.columns([1, 1])
                        with col_c1:
                            st.markdown(f"**Document Category:** <span class='badge badge-info'>{analysis_res.get('document_category', 'General')}</span>", unsafe_allow_html=True)
                            st.markdown(f"**Subject / Title:** `{analysis_res.get('title_or_subject', 'N/A')}`")
                            st.markdown("##### 📝 Executive Summary")
                            st.info(analysis_res.get("executive_summary", "No summary provided."))

                        with col_c2:
                            st.markdown("##### 📌 Key Highlights")
                            for h in analysis_res.get("key_highlights", []):
                                st.markdown(f"- {h}")

                        st.divider()
                        col_s1, col_s2 = st.columns(2)
                        with col_s1:
                            st.markdown("##### 🛠️ Identified Skills & Technologies")
                            skills = analysis_res.get("identified_skills_and_technologies", [])
                            if skills:
                                chips_html = " ".join([f"<span class='chip'>{s}</span>" for s in skills])
                                st.markdown(chips_html, unsafe_allow_html=True)
                            else:
                                st.caption("No specific skills identified.")

                            st.markdown("##### 🏢 Key Entities & Organizations")
                            entities = analysis_res.get("key_entities", [])
                            if entities:
                                for ent in entities:
                                    st.markdown(f"- **{ent}**")
                            else:
                                st.caption("No specific entities identified.")

                        with col_s2:
                            st.markdown("##### 🌟 Strengths & Positive Highlights")
                            for p in analysis_res.get("strengths_or_pros", []):
                                st.markdown(f"- ✅ {p}")

                            st.markdown("##### ⚠️ Gaps & Potential Concerns")
                            gaps = analysis_res.get("potential_red_flags_or_gaps", [])
                            if gaps:
                                for g in gaps:
                                    st.markdown(f"- ⚠️ {g}")
                            else:
                                st.caption("None detected.")

                        with st.expander("🔍 View Raw JSON Response"):
                            st.json(analysis_res)

                    except Exception as e:
                        st.error(f"Error during document analysis: {e}")

        # Tab 2: Interactive Document Q&A
        with tab_qa:
            st.markdown("##### Ask any question grounded strictly in this document's context.")
            
            # Quick suggestion buttons
            st.caption("Quick query ideas:")
            q_cols = st.columns(3)
            with q_cols[0]:
                if st.button("📋 What are the primary qualifications/requirements?", key="quick_q1"):
                    st.session_state.doc_qa_input = "What are the primary qualifications, skills, and requirements mentioned in this document?"
            with q_cols[1]:
                if st.button("🎯 Summarize key achievements or responsibilities", key="quick_q2"):
                    st.session_state.doc_qa_input = "What are the main achievements, responsibilities, or deliverables described?"
            with q_cols[2]:
                if st.button("❓ Are there any missing prerequisites or gaps?", key="quick_q3"):
                    st.session_state.doc_qa_input = "Identify any missing information, experience gaps, or prerequisites that are not met."

            user_doc_question = st.text_input(
                "Your Question about this Document:",
                value=st.session_state.get("doc_qa_input", "What are the key technical skills and years of experience mentioned in this document?"),
                key="doc_qa_query_box",
            )

            if st.button("🔍 Ask LLM", type="primary", key="btn_ask_doc"):
                if not user_doc_question.strip():
                    st.warning("Please enter a question.")
                else:
                    with st.spinner("Generating grounded answer with LLM..."):
                        start_t = time.time()
                        try:
                            answer = ask_document_question(active_text, user_doc_question)
                            elapsed = round(time.time() - start_t, 2)
                            st.markdown(f"<span class='badge badge-success'>Response received in {elapsed}s</span>", unsafe_allow_html=True)
                            st.markdown("### 💡 Answer:")
                            st.markdown(answer)
                        except Exception as e:
                            st.error(f"Error answering question: {e}")

        # Tab 3: Custom Prompt Sandbox
        with tab_prompt:
            st.markdown("##### Test custom prompts and inspect LLM behavior on the document context.")
            
            prompt_presets = {
                "Custom Prompt": "Extract the top 5 key takeaways from this document in concise bullet points:\n\n{document_text}",
                "Candidate Evaluation": "Evaluate this candidate's fit for a Senior Staff Engineer role. Provide a score from 1-10 and detailed pros and cons:\n\n{document_text}",
                "Role & Skill Matrix": "Extract all technical tools, databases, programming languages, and frameworks into a clean markdown table with categorized columns:\n\n{document_text}",
                "Document Grammar & Tone Review": "Critique the tone, clarity, and grammatical precision of this document. Provide 3 specific recommendations for improvement:\n\n{document_text}",
            }

            preset_sel = st.selectbox("Prompt Template Preset:", list(prompt_presets.keys()))
            default_prompt = prompt_presets[preset_sel]

            custom_prompt_val = st.text_area(
                "Prompt (Use `{document_text}` where document should be inserted):",
                value=default_prompt,
                height=150,
            )

            if st.button("🚀 Execute Custom Prompt", type="primary", key="btn_custom_prompt"):
                with st.spinner("Executing prompt with LLM..."):
                    try:
                        prompt_res = run_custom_prompt_test(active_text, custom_prompt_val)
                        st.success(f"Execution completed in {prompt_res['latency_seconds']}s")
                        
                        st.markdown("### 🤖 LLM Response:")
                        st.markdown(prompt_res["response"])

                        with st.expander("🔍 View Full Injected Prompt"):
                            st.text(prompt_res["prompt"])
                    except Exception as e:
                        st.error(f"Custom prompt error: {e}")

        # Tab 4: Pipeline Routing
        with tab_pipeline:
            st.markdown("##### Route this uploaded document directly into HireAI's processing modules:")
            pipe_col1, pipe_col2 = st.columns(2)

            with pipe_col1:
                st.markdown("#### 📄 Parse as Candidate Resume")
                st.caption("Runs HireAI zero-shot resume extraction pipeline on this document.")
                if st.button("Run Resume Parser Pipeline", type="primary", key="btn_pipe_resume"):
                    with st.spinner("Parsing resume with Ollama..."):
                        try:
                            resume_res = parse_resume(active_text)
                            st.success("Resume parsed successfully!")
                            st.json(resume_res)
                        except Exception as e:
                            st.error(f"Error parsing: {e}")

            with pipe_col2:
                st.markdown("#### 🎙️ Generate Interview Questions")
                st.caption("Generates targeted technical interview questions based on document content.")
                if st.button("Generate Interview Questions", key="btn_pipe_interview"):
                    with st.spinner("Analyzing document skills & generating questions..."):
                        try:
                            # Quick skill extract
                            skills_to_use = ["Java", "Spring Boot", "Microservices", "Docker", "Kubernetes", "PostgreSQL"]
                            q_res = generate_questions(skills_to_use)
                            st.success("Questions generated!")
                            for i, q in enumerate(q_res.get("questions", []), 1):
                                st.markdown(f"**Q{i}:** {q.get('question_text', q.get('question', ''))}")
                                st.caption(f"Target Skill: `{q.get('skill', 'General')}`")
                        except Exception as e:
                            st.error(f"Error: {e}")

    else:
        st.info("👆 Please upload a document (PDF, DOCX, TXT) or select a sample document above to start testing.")

# ----------------------------------------------------
# 1. RESUME PARSER (ENHANCED WITH FILE UPLOAD)
# ----------------------------------------------------
elif page == "📄 Resume Parser":
    st.header("📄 Resume Parser")
    st.caption("Extracts structured candidate details, skills, experience, and education from documents or raw text.")

    input_mode = st.radio("Input Method:", ["📤 Upload Document (PDF / DOCX / TXT)", "✍️ Paste Raw Text"], horizontal=True)

    resume_input_text = ""
    candidate_id = st.text_input("Candidate ID", value="CAND-001")

    if input_mode == "📤 Upload Document (PDF / DOCX / TXT)":
        uploaded_resume = st.file_uploader(
            "Upload Candidate Resume:",
            type=["pdf", "docx", "doc", "txt"],
            key="resume_file_uploader",
        )
        if uploaded_resume is not None:
            doc_res = extract_document_info(uploaded_resume)
            if doc_res.get("error"):
                st.error(doc_res["error"])
            else:
                resume_input_text = doc_res["text"]
                st.success(f"Loaded `{doc_res['filename']}` ({doc_res['word_count']} words, ~{doc_res['estimated_tokens']} tokens)")
                with st.expander("Preview Extracted Resume Text", expanded=False):
                    st.text(resume_input_text)
        else:
            st.info("Upload a PDF/DOCX/TXT resume or use sample below.")
            if st.button("Load Sample Resume"):
                resume_input_text = SAMPLE_RESUME_DOC
                st.session_state.pasted_resume_text = SAMPLE_RESUME_DOC
    else:
        default_resume = st.session_state.get("pasted_resume_text", SAMPLE_RESUME_DOC)
        resume_input_text = st.text_area("Resume Text Content", value=default_resume, height=240)

    if st.button("🚀 Parse Resume with LLM", type="primary"):
        if not resume_input_text.strip():
            st.warning("Please provide resume text or upload a resume document.")
        else:
            with st.spinner("Extracting structured candidate data using LLM..."):
                try:
                    result = parse_resume(resume_input_text)
                    result["candidateId"] = candidate_id
                    st.success("Resume parsed successfully!")

                    col1, col2 = st.columns(2)
                    with col1:
                        st.markdown("### 👤 Personal Info")
                        st.write(f"**Name:** {result.get('name', 'N/A')}")
                        st.write(f"**Email:** {result.get('email', 'N/A')}")
                        st.write(f"**Phone:** {result.get('phone', 'N/A')}")
                        st.write(f"**Total Experience:** {result.get('years_of_experience', result.get('total_years_experience', 'N/A'))} years")

                    with col2:
                        st.markdown("### 🛠️ Extracted Skills")
                        skills = result.get("skills", [])
                        if skills:
                            st.write(", ".join([f"`{s}`" for s in skills]))
                        else:
                            st.write("No skills found")

                    st.markdown("### 💼 Work Experience")
                    st.json(result.get("experience", result.get("work_experience", [])))

                    st.markdown("### 🎓 Education")
                    st.json(result.get("education", []))

                    with st.expander("🔍 View Raw JSON"):
                        st.json(result)
                except Exception as e:
                    st.error(f"Error parsing resume: {e}")

# ----------------------------------------------------
# 2. JD GENERATOR
# ----------------------------------------------------
elif page == "📝 JD Generator":
    st.header("📝 Job Description Generator")
    st.caption("Generates a complete, structured Job Description using local Llama 3.1 LLM.")

    col1, col2 = st.columns([1, 1])
    with col1:
        job_title = st.text_input("Job Title", value="Senior Java Backend Developer")
        skills_input = st.text_area("Required Skills (comma separated)", value="Java, Spring Boot, PostgreSQL, Microservices, Docker, Kafka")
        exp_level = st.selectbox("Experience Level", ["Entry Level (0-2 yrs)", "Mid Level (3-5 yrs)", "Senior Level (5+ yrs)", "Lead / Principal (8+ yrs)"], index=2)
        generate_btn = st.button("✨ Generate Job Description", type="primary", use_container_width=True)

    with col2:
        if generate_btn:
            skills = [s.strip() for s in skills_input.split(",") if s.strip()]
            with st.spinner("Calling Ollama to generate Job Description..."):
                try:
                    result = generate_jd(job_title, skills, exp_level)
                    st.success("JD Generated Successfully!")

                    st.subheader("📋 Overview")
                    st.write(result.get("description", ""))

                    st.subheader("🎯 Key Responsibilities")
                    for resp in result.get("responsibilities", []):
                        st.markdown(f"- {resp}")

                    st.subheader("🛠️ Must-Have Skills")
                    st.write(", ".join([f"`{s}`" for s in result.get("must_have_skills", [])]))

                    st.subheader("💡 Nice-To-Have Skills")
                    st.write(", ".join([f"`{s}`" for s in result.get("nice_to_have_skills", [])]))

                    st.subheader("❓ Suggested Interview Questions")
                    for q in result.get("interview_questions", []):
                        st.markdown(f"1. {q}")

                    with st.expander("🔍 View Raw JSON Output"):
                        st.json(result)
                except Exception as e:
                    st.error(f"Error: {e}")

# ----------------------------------------------------
# 3. MATCH ENGINE (VECTOR SIMILARITY & RESUME PARSING)
# ----------------------------------------------------
elif page == "🎯 Match Engine (Vector Sim)":
    st.header("🎯 Candidate-to-Job Match Engine")
    st.caption("Automatically parses uploaded candidate resumes, matches against job requirements, and calculates match score.")

    col1, col2 = st.columns(2)
    with col1:
        st.subheader("📋 1. Job Requirements (Recruiter)")
        job_doc_upload = st.file_uploader("Upload Job Description (PDF/DOCX/TXT):", type=["pdf", "docx", "txt"], key="jd_match_file")
        
        default_job_skills = "Java, JavaScript, SQL, Spring Boot, Spring MVC, Spring Security, Spring Data JPA, Hibernate, JDBC, REST APIs, MySQL, PostgreSQL, Docker, AWS, Git"
        if job_doc_upload:
            jd_info = extract_document_info(job_doc_upload)
            st.caption(f"📄 Loaded JD: `{jd_info['filename']}`")

        job_skills_input = st.text_area(
            "Target Required Skills (comma-separated):",
            value=default_job_skills,
            height=130,
            key="match_job_skills_input",
            help="Enter the skills you are hiring for. Candidates with these skills will be shortlisted.",
        )

    with col2:
        st.subheader("📄 2. Candidate Resume (Automated)")
        cand_doc_upload = st.file_uploader(
            "Upload Candidate Resume (PDF / DOCX / TXT):",
            type=["pdf", "docx", "doc", "txt"],
            key="resume_match_file",
            help="Upload the candidate's resume. Skills will be automatically parsed by the AI model.",
        )

        use_sample_resume = False
        if not cand_doc_upload:
            st.info("👆 Upload a resume above or check the box to test with sample resume.")
            use_sample_resume = st.checkbox("Use Sample Resume (Alex Morgan — Senior Java/AI Lead)", value=False)

    st.markdown("<br>", unsafe_allow_html=True)
    if st.button("⚡ Run AI Candidate-to-Job Match", type="primary", use_container_width=True):
        j_skills = [s.strip() for s in job_skills_input.split(",") if s.strip()]

        if not j_skills:
            st.warning("⚠️ Please specify at least one required job skill.")
        elif not cand_doc_upload and not use_sample_resume:
            st.warning("⚠️ Please upload a candidate resume (PDF/DOCX/TXT) or check 'Use Sample Resume'.")
        else:
            # 1. Extract Resume Text
            resume_text = ""
            resume_filename = "sample_resume.txt"
            if cand_doc_upload:
                with st.spinner("Extracting text from uploaded resume..."):
                    cand_info = extract_document_info(cand_doc_upload)
                    resume_text = cand_info.get("text", "")
                    resume_filename = cand_info.get("filename", "uploaded_resume")
            elif use_sample_resume:
                resume_text = SAMPLE_RESUME_DOC
                resume_filename = "alex_morgan_resume.txt"

            if not resume_text.strip():
                st.error("Could not extract any readable text from the uploaded resume.")
            else:
                # 2. Automatically Parse Candidate Skills with LLM
                with st.spinner(f"AI parsing candidate skills from `{resume_filename}`..."):
                    try:
                        parsed_candidate = parse_resume(resume_text)
                        c_skills = parsed_candidate.get("skills", [])
                    except Exception as e:
                        st.error(f"Error parsing candidate resume: {e}")
                        c_skills = []

                if not c_skills:
                    st.warning("No skills could be identified in the resume. Please check the document content.")
                else:
                    # 3. Calculate Match Score
                    result = calculate_match_score(c_skills, j_skills)
                    score = result["match_score"]

                    st.divider()
                    
                    # Top Metric Summary
                    col_m1, col_m2, col_m3, col_m4 = st.columns(4)
                    with col_m1:
                        st.metric("Skill Match Score", f"{score}%")
                        st.progress(score / 100)
                    with col_m2:
                        action = result.get("auto_action", "MANUAL_REVIEW").upper()
                        badge_class = "badge-success" if "SHORTLIST" in action else ("badge-warning" if "REVIEW" in action else "badge-danger")
                        st.markdown(f"**Recommendation:**")
                        st.markdown(f"<span class='badge {badge_class}' style='font-size:1rem;'>{action}</span>", unsafe_allow_html=True)
                    with col_m3:
                        st.metric("Required Skills Matched", f"{len(result.get('matched_skills', []))} / {len(j_skills)}")
                    with col_m4:
                        st.metric("Total Skills on Resume", len(c_skills))

                    # Candidate Info Banner
                    if parsed_candidate.get("name") or parsed_candidate.get("currentRole"):
                        cand_name = parsed_candidate.get("name") or "Candidate"
                        cand_role = parsed_candidate.get("currentRole") or "Professional"
                        cand_exp = parsed_candidate.get("yearsExperience") or "N/A"
                        st.info(f"👤 **Candidate Profile:** {cand_name} | **Role:** {cand_role} | **Experience:** {cand_exp} years")

                    # Detailed Skills Breakdown
                    col_r1, col_r2 = st.columns(2)
                    with col_r1:
                        st.subheader(f"✅ Matched Required Skills ({len(result.get('matched_skills', []))})")
                        m_skills = result.get("matched_skills", [])
                        if m_skills:
                            chips_html = " ".join([f"<span class='chip' style='background:#DCFCE7; color:#166534;'>{s}</span>" for s in m_skills])
                            st.markdown(chips_html, unsafe_allow_html=True)
                        else:
                            st.info("None")

                    with col_r2:
                        st.subheader(f"❌ Missing Required Skills ({len(result.get('missing_skills', []))})")
                        miss_skills = result.get("missing_skills", [])
                        if miss_skills:
                            chips_html = " ".join([f"<span class='chip' style='background:#FEE2E2; color:#991B1B;'>{s}</span>" for s in miss_skills])
                            st.markdown(chips_html, unsafe_allow_html=True)
                        else:
                            st.success("🎉 All required skills satisfied! (100% Match)")

                    # Bonus Extra Skills
                    bonus_s = result.get("bonus_skills", [])
                    if bonus_s:
                        st.subheader(f"🌟 Candidate Bonus / Extra Skills ({len(bonus_s)})")
                        st.caption("Extra capabilities the candidate brings beyond what was asked for:")
                        bonus_html = " ".join([f"<span class='chip' style='background:#DBEAFE; color:#1D4ED8;'>{s}</span>" for s in bonus_s])
                        st.markdown(bonus_html, unsafe_allow_html=True)

                    with st.expander("🔍 View Parsed Resume Data & Raw Results"):
                        st.json({
                            "parsed_candidate": parsed_candidate,
                            "match_results": result
                        })

# ----------------------------------------------------
# 4. AI RECRUITER CHATBOT
# ----------------------------------------------------
elif page == "💬 AI Recruiter Chatbot":
    st.header("💬 AI Recruiter Candidate Screening Bot")
    st.caption("Stateless conversational screening bot powered by Llama 3.1 that extracts candidate qualifications on the fly.")

    if "chat_history" not in st.session_state:
        st.session_state.chat_history = [
            {"role": "assistant", "text": "Hi! Thanks for applying to HireAI. Could you briefly tell me how many years of experience you have with Java and Spring Boot?"}
        ]
    if "extracted_fields" not in st.session_state:
        st.session_state.extracted_fields = {}

    col_chat, col_info = st.columns([2, 1])

    with col_info:
        st.subheader("📋 Extracted Profile Data")
        if st.session_state.extracted_fields:
            st.json(st.session_state.extracted_fields)
        else:
            st.info("No candidate fields extracted yet. As the candidate chats, extracted data will appear here.")

        if st.button("🔄 Reset Conversation"):
            st.session_state.chat_history = [
                {"role": "assistant", "text": "Hi! Thanks for applying to HireAI. Could you briefly tell me how many years of experience you have with Java and Spring Boot?"}
            ]
            st.session_state.extracted_fields = {}
            st.rerun()

    with col_chat:
        for msg in st.session_state.chat_history:
            with st.chat_message(msg["role"]):
                st.write(msg["text"])

        user_input = st.chat_input("Type candidate response here...")
        if user_input:
            st.session_state.chat_history.append({"role": "user", "text": user_input})
            with st.chat_message("user"):
                st.write(user_input)

            with st.spinner("AI is thinking..."):
                try:
                    response = handle_message(st.session_state.chat_history, user_input)
                    bot_reply = response.get("bot_reply", "")
                    new_fields = response.get("extracted_fields", {})
                    st.session_state.extracted_fields.update(new_fields)

                    st.session_state.chat_history.append({"role": "assistant", "text": bot_reply})
                    with st.chat_message("assistant"):
                        st.write(bot_reply)

                    if response.get("conversation_complete", False):
                        st.balloons()
                        st.success("🎉 Screening interview completed!")
                except Exception as e:
                    st.error(f"Chatbot error: {e}")

# ----------------------------------------------------
# 5. INTERVIEW AI
# ----------------------------------------------------
elif page == "🎙️ Interview AI":
    st.header("🎙️ Technical Interview AI")
    tab1, tab2 = st.tabs(["1️⃣ Generate Interview Questions", "2️⃣ Evaluate Candidate Answers"])

    with tab1:
        st.subheader("Generate Targeted Technical Questions")
        skills_q = st.text_input("Candidate & Job Skills", value="Java, Spring Boot, Microservices, Kafka")
        if st.button("Generate Questions", type="primary"):
            skills_list = [s.strip() for s in skills_q.split(",") if s.strip()]
            with st.spinner("Generating questions with Llama 3.1..."):
                try:
                    q_res = generate_questions(skills_list)
                    st.session_state.generated_questions = q_res.get("questions", [])
                    st.success("Questions generated!")
                except Exception as e:
                    st.error(f"Error: {e}")

        if "generated_questions" in st.session_state:
            for i, q in enumerate(st.session_state.generated_questions, 1):
                st.markdown(f"**Q{i}:** {q.get('question_text', q.get('question', ''))}")
                st.caption(f"Target Skill: `{q.get('skill', 'General')}` | Expected concept: {q.get('expected_concepts', q.get('expected_concept', 'N/A'))}")

    with tab2:
        st.subheader("Evaluate Answers")
        q_text = st.text_area("Question", value="How do you handle distributed transactions across microservices in Spring Boot?")
        ans_text = st.text_area("Candidate Answer", value="We use the Saga pattern with choreography or orchestration, utilizing Apache Kafka for event-driven compensation transactions when a step fails.")

        if st.button("Evaluate Answer", type="primary"):
            with st.spinner("Evaluating candidate answer..."):
                try:
                    eval_res = evaluate_answers([{"question_id": "Q1", "answer_text": ans_text}])
                    st.success("Evaluation complete!")
                    st.json(eval_res)
                except Exception as e:
                    st.error(f"Evaluation error: {e}")

# ----------------------------------------------------
# 6. DECISION ENGINE
# ----------------------------------------------------
elif page == "⚖️ Decision Engine":
    st.header("⚖️ Final Candidate Decision Engine")
    st.caption("Aggregates resume match, technical score, and interview ratings using auditable deterministic weights.")

    col1, col2 = st.columns(2)
    with col1:
        cand_id = st.text_input("Candidate ID", value="CAND-001")
        match_score = st.slider("Resume Match Score (%)", 0, 100, 85)
        tech_score = st.slider("Technical Assessment Score (%)", 0, 100, 80)
    with col2:
        interview_score = st.slider("Interview Score (%)", 0, 100, 75)
        notes = st.text_area("Interviewer Notes", value="Candidate demonstrated strong knowledge of microservices and clean architecture.")

    if st.button("📊 Finalize Recommendation", type="primary", use_container_width=True):
        try:
            dec = finalize_decision(match_score, interview_score, tech_score)
            st.subheader("🎯 Final Decision Summary")
            
            col_d1, col_d2 = st.columns([1, 1])
            with col_d1:
                st.metric("Final Weighted Score", f"{dec['final_score']}%")
                cls = dec["classification"].upper()
                badge_style = "badge-success" if cls == "SHORTLIST" else ("badge-warning" if cls == "HOLD" else "badge-danger")
                st.markdown(f"**Classification:** <span class='badge {badge_style}'>{cls}</span>", unsafe_allow_html=True)
                st.info(dec.get("explanation", ""))

            with col_d2:
                st.markdown("##### Score Breakdown")
                st.json(dec.get("breakdown", {}))

            with st.expander("🔍 View Raw Output"):
                st.json(dec)
        except Exception as e:
            st.error(f"Error finalizing decision: {e}")
