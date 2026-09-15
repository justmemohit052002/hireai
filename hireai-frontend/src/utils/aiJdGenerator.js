import { formatSalary, formatDate, getJobTypeLabel, getExperienceLevelLabel } from '@/utils';

/**
 * Intelligent domain knowledge base for generating high-quality ATS skills,
 * role-specific responsibilities, education, and perks.
 */
const DOMAIN_TEMPLATES = [
  {
    matches: ['data analyst', 'business analyst', 'bi analyst', 'analytics', 'data analysis', 'bi developer', 'data specialist', 'analyst', 'reporting analyst'],
    skills: ['SQL', 'Python', 'Power BI', 'Tableau', 'Excel (Advanced)', 'Data Modeling', 'ETL Pipelines', 'Statistical Analysis', 'Dashboarding', 'JIRA'],
    education: "Bachelor's degree in Data Science, Statistics, Computer Science, Mathematics, or related quantitative field.",
    responsibilities: [
      'Extract, clean, and transform complex datasets from multiple relational and non-relational sources.',
      'Design, build, and maintain interactive business intelligence dashboards in Power BI/Tableau for leadership.',
      'Perform exploratory data analysis to identify operational trends, bottlenecks, and revenue opportunities.',
      'Collaborate closely with product, engineering, and operations stakeholders to translate business questions into actionable metrics.',
      'Establish standardized data governance, metric definitions, and automated reporting pipelines.'
    ],
  },
  {
    matches: ['data scientist', 'machine learning', 'ml engineer', 'ai engineer', 'nlp', 'computer vision', 'deep learning', 'ai specialist', 'artificial intelligence'],
    skills: ['Python', 'PyTorch', 'TensorFlow', 'Scikit-Learn', 'SQL', 'LLM Fine-Tuning', 'Feature Engineering', 'MLOps', 'Docker', 'FastAPI'],
    education: "Master's or Bachelor's degree in Computer Science, Artificial Intelligence, Data Science, or Mathematics.",
    responsibilities: [
      'Architect, train, evaluate, and deploy predictive models and deep learning pipelines into production.',
      'Design advanced feature engineering strategies and benchmark model accuracy against real-world metrics.',
      'Collaborate with backend engineers to integrate scalable machine learning inference APIs and microservices.',
      'Monitor model performance, data drift, and latency in live production environments.',
      'Keep abreast of emerging AI architectures, foundation models, and prompt engineering best practices.'
    ],
  },
  {
    matches: ['frontend', 'react', 'vue', 'angular', 'web developer', 'ui developer', 'javascript developer', 'frontend engineer', 'next.js'],
    skills: ['React.js', 'TypeScript', 'Next.js', 'TailwindCSS', 'JavaScript (ES6+)', 'REST APIs', 'State Management (Redux/Zustand)', 'HTML5/CSS3', 'Jest/RTL', 'Git'],
    education: "Bachelor's degree in Computer Science, Software Engineering, or equivalent practical web engineering experience.",
    responsibilities: [
      'Develop modern, ultra-responsive, and accessible user interfaces using modern frontend component frameworks.',
      'Optimize web application performance, bundle size, and Core Web Vitals across desktop and mobile devices.',
      'Collaborate with UI/UX designers to translate Figma designs and wireframes into pixel-perfect code.',
      'Integrate RESTful and GraphQL APIs with robust client-side state management and error handling.',
      'Write comprehensive unit and integration tests to ensure cross-browser compatibility and reliability.'
    ],
  },
  {
    matches: ['python developer', 'python engineer', 'python backend', 'django', 'fastapi', 'flask', 'python'],
    skills: ['Python', 'Django / FastAPI / Flask', 'RESTful APIs', 'PostgreSQL / MySQL', 'SQLAlchemy / ORM', 'Redis', 'Docker', 'Git', 'PyTest', 'Celery'],
    education: "Bachelor's or Master's degree in Computer Science, Information Technology, Software Engineering, or related discipline.",
    responsibilities: [
      'Design, develop, and maintain robust, scalable backend microservices and RESTful APIs in Python.',
      'Architect efficient database schemas, optimize queries with PostgreSQL/MySQL, and implement caching using Redis.',
      'Write clean, modular, and maintainable Python code with comprehensive unit and integration test coverage.',
      'Collaborate with cross-functional frontend, product, and DevOps teams to ship high-impact features.',
      'Implement asynchronous task processing with Celery/Redis and integrate third-party APIs seamlessly.'
    ],
  },
  {
    matches: ['backend', 'java', 'spring', 'node', 'golang', 'c#', '.net', 'api developer', 'systems engineer', 'backend engineer', 'go developer'],
    skills: ['Java', 'Spring Boot', 'Microservices', 'PostgreSQL', 'REST APIs', 'Redis', 'Docker', 'Kafka', 'System Design', 'Git'],
    education: "Bachelor's or Master's degree in Computer Science, Information Technology, or related technical discipline.",
    responsibilities: [
      'Design, develop, and maintain high-throughput, low-latency microservices and distributed backend systems.',
      'Architect scalable relational and NoSQL database schemas, indexing strategies, and query optimizations.',
      'Implement secure RESTful APIs, JWT authentication, role-based access control, and rate limiting.',
      'Collaborate with DevOps teams on containerization, CI/CD automated deployment pipelines, and observability.',
      'Conduct rigorous code reviews, unit tests, and performance profiling to ensure 99.99% uptime.'
    ],
  },
  {
    matches: ['full stack', 'fullstack', 'software engineer', 'software developer', 'sde', 'mern', 'mean'],
    skills: ['React.js', 'Node.js', 'TypeScript', 'Java / Spring Boot', 'PostgreSQL', 'Docker', 'RESTful APIs', 'AWS', 'TailwindCSS', 'Git'],
    education: "Bachelor's degree in Computer Science, Information Systems, Engineering, or equivalent practical experience.",
    responsibilities: [
      'Own end-to-end feature delivery from responsive frontend interfaces to scalable backend microservices.',
      'Build and maintain resilient APIs and database architectures supporting high-concurrency client applications.',
      'Participate actively in architectural design discussions, sprint planning, and agile standups.',
      'Automate unit, integration, and end-to-end testing across the full development lifecycle.',
      'Identify and remediate performance bottlenecks across both client and server layers.'
    ],
  },
  {
    matches: ['mobile', 'android', 'ios', 'flutter', 'react native', 'swift', 'kotlin', 'app developer'],
    skills: ['Flutter / React Native', 'Dart / JavaScript', 'iOS / Swift', 'Android / Kotlin', 'Mobile App Architecture', 'REST APIs', 'State Management (Bloc/Redux)', 'App Store & Play Store Deployment', 'Firebase', 'Git'],
    education: "Bachelor's degree in Computer Science, Mobile Computing, or equivalent practical app development portfolio.",
    responsibilities: [
      'Architect, develop, and publish high-performance native and cross-platform mobile applications for iOS and Android.',
      'Ensure smooth 60fps animations, responsive UI layouts, offline synchronization, and battery efficiency.',
      'Integrate push notifications, device sensors, biometric authentication, and in-app purchase systems.',
      'Manage app lifecycle, testing on diverse screen resolutions, and App Store / Google Play release pipelines.',
      'Collaborate with product managers and UX designers to deliver delightful touch-first mobile experiences.'
    ],
  },
  {
    matches: ['data engineer', 'big data', 'etl developer', 'data pipeline', 'spark', 'snowflake', 'databricks', 'hadoop'],
    skills: ['Apache Spark', 'Python', 'SQL (Advanced)', 'Data Warehousing (Snowflake / BigQuery)', 'ETL / ELT Pipelines', 'Airflow', 'Kafka', 'AWS / GCP Data Services', 'Docker', 'Git'],
    education: "Bachelor's or Master's degree in Computer Science, Data Engineering, Information Systems, or quantitative field.",
    responsibilities: [
      'Architect, build, and scale automated data ingestion and transformation pipelines across petabyte-scale data lakes.',
      'Optimize complex analytical SQL queries, data warehousing schemas, and distributed Spark jobs for efficiency.',
      'Implement resilient workflow orchestration using Apache Airflow and real-time streaming with Apache Kafka.',
      'Enforce data security, masking, compliance, and automated data quality validation at every pipeline stage.',
      'Partner with analytics and data science teams to deliver clean, production-ready feature tables and marts.'
    ],
  },
  {
    matches: ['devops', 'cloud', 'sre', 'infrastructure', 'platform engineer', 'kubernetes', 'aws', 'azure', 'gcp', 'site reliability'],
    skills: ['AWS / Cloud Infrastructure', 'Kubernetes', 'Docker', 'Terraform', 'CI/CD Pipelines (GitHub Actions)', 'Linux / Bash', 'Prometheus & Grafana', 'Helm', 'Security Best Practices'],
    education: "Bachelor's degree in Computer Science, Network Engineering, or equivalent cloud platform certifications (AWS/GCP/CKA).",
    responsibilities: [
      'Architect, provision, and maintain secure, multi-region cloud infrastructure using Terraform and Infrastructure as Code.',
      'Build and optimize automated CI/CD deployment pipelines ensuring rapid, zero-downtime releases.',
      'Manage container orchestration clusters using Kubernetes with robust scaling and self-healing policies.',
      'Implement observability, distributed tracing, alerting, and log aggregation using Prometheus, Grafana, and ELK.',
      'Perform disaster recovery rehearsals, vulnerability scans, and continuous infrastructure cost optimization.'
    ],
  },
  {
    matches: ['cybersecurity', 'security engineer', 'infosec', 'soc analyst', 'penetration tester', 'security analyst', 'information security'],
    skills: ['Network Security', 'Vulnerability Assessment & Pen Testing', 'SIEM & SOC Tools', 'Threat Modeling', 'OWASP Top 10', 'Incident Response', 'IAM & Zero Trust Architecture', 'Python / Bash Scripting', 'Compliance (SOC2 / ISO 27001)'],
    education: "Bachelor's degree in Cybersecurity, Information Security, Computer Science, or certifications (CISSP, CEH, CompTIA Security+).",
    responsibilities: [
      'Conduct regular vulnerability assessments, code audits, and penetration testing across cloud and application infrastructure.',
      'Configure and monitor Security Information and Event Management (SIEM) systems for intrusion detection.',
      'Lead incident response investigations, forensic analysis, and post-mortem mitigation strategies.',
      'Implement Zero Trust identity and access management (IAM) policies and encryption standards across all environments.',
      'Ensure adherence to global cybersecurity compliance frameworks including SOC 2, ISO 27001, and GDPR.'
    ],
  },
  {
    matches: ['qa', 'test', 'sdet', 'quality assurance', 'automation engineer', 'tester'],
    skills: ['Selenium WebDriver', 'Cypress / Playwright', 'Test Automation Frameworks', 'Java / Python / JS', 'API Testing (Postman/RestAssured)', 'JIRA', 'CI/CD Integration', 'SQL', 'Performance Testing (JMeter)'],
    education: "Bachelor's degree in Computer Science, Software Engineering, or related technical field.",
    responsibilities: [
      'Design, maintain, and execute comprehensive end-to-end automated test suites for web, mobile, and API layers.',
      'Participate in requirements grooming to identify test scenarios, edge cases, and acceptance criteria early.',
      'Integrate test automation suites into CI/CD release pipelines to prevent regressions before production deployment.',
      'Perform load, stress, and security testing to validate non-functional system benchmarks.',
      'Document detailed defect reports, collaborate with engineers on triage, and verify bug fixes.'
    ],
  },
  {
    matches: ['product manager', 'product owner', 'tpm', 'program manager', 'product lead'],
    skills: ['Product Roadmap & Strategy', 'Agile & Scrum Methodologies', 'User Story Mapping', 'JIRA & Confluence', 'Data Analytics (Mixpanel/GA)', 'Customer Discovery & User Research', 'A/B Testing', 'Stakeholder Management'],
    education: "Bachelor's or Master's degree in Business, Computer Science, Engineering, or equivalent product leadership experience.",
    responsibilities: [
      'Define product vision, strategic roadmap, and quarterly KPI milestones aligned with company objectives.',
      'Work closely with engineering, design, and business teams to prioritize backlog and deliver customer-centric features.',
      'Conduct user interviews, market research, and telemetry analysis to uncover unmet user needs.',
      'Write clear, concise product requirement documents (PRDs) and user stories with measurable acceptance criteria.',
      'Lead go-to-market launches and iterate rapidly based on user feedback and quantitative engagement metrics.'
    ],
  },
  {
    matches: ['ui', 'ux', 'designer', 'product designer', 'visual designer', 'ux researcher'],
    skills: ['Figma', 'UI/UX Design', 'Design Systems', 'Wireframing & Prototyping', 'User Research & Usability Testing', 'Interaction Design', 'Responsive Design', 'Design Thinking'],
    education: "Bachelor's degree in Design, Human-Computer Interaction (HCI), Fine Arts, or equivalent digital portfolio.",
    responsibilities: [
      'Create intuitive, aesthetically pleasing UI flows, wireframes, prototypes, and high-fidelity mockups in Figma.',
      'Maintain and evolve the scalable design system token library across dark and light modes.',
      'Conduct usability tests, user interviews, and cognitive walkthroughs to validate design hypotheses.',
      'Collaborate closely with frontend engineers to ensure design fidelity and fluid micro-interactions.',
      'Advocate for design accessibility (WCAG) and seamless user experience across mobile and desktop interfaces.'
    ],
  },
  {
    matches: ['recruiter', 'talent acquisition', 'hr', 'human resources', 'people operations', 'hr generalist'],
    skills: ['Talent Sourcing & Headhunting', 'Technical Screening & Interviewing', 'Applicant Tracking Systems (ATS)', 'Candidate Experience', 'Offer Negotiation', 'Employer Branding', 'HR Operations & Compliance', 'LinkedIn Recruiter'],
    education: "Bachelor's degree in Human Resources, Business Administration, Psychology, or relevant operational experience.",
    responsibilities: [
      'Manage full-cycle recruitment from sourcing and candidate screening to offer negotiation and onboarding.',
      'Partner closely with hiring managers to calibrate candidate profiles, interview loops, and evaluation criteria.',
      'Build robust talent pipelines across multiple sourcing channels, community networks, and platforms.',
      'Deliver an exemplary, transparent, and empathetic candidate experience throughout the entire hiring journey.',
      'Track recruitment metrics (time-to-hire, offer acceptance rate, source efficiency) to continuously improve workflows.'
    ],
  },
  {
    matches: ['marketing', 'digital marketing', 'growth', 'seo', 'content', 'social media', 'brand'],
    skills: ['Growth Marketing & Acquisition', 'SEO & SEM Strategies', 'Content Strategy & Copywriting', 'Google Analytics & Tag Manager', 'Social Media Marketing', 'Email Marketing & Automation', 'Campaign ROI Optimization', 'A/B Testing'],
    education: "Bachelor's degree in Marketing, Communications, Business, Journalism, or equivalent digital marketing background.",
    responsibilities: [
      'Develop and execute multi-channel inbound and outbound marketing campaigns to drive user acquisition and engagement.',
      'Optimize organic search engine rankings (SEO), content strategy, and paid acquisition funnels.',
      'Analyze customer funnels, conversion rates, and campaign performance using Google Analytics and BI dashboards.',
      'Collaborate with design and product teams to craft compelling copy, landing pages, and email nurturing sequences.',
      'Oversee social media channels, community engagement, and brand storytelling to build strong industry presence.'
    ],
  },
  {
    matches: ['sales', 'business development', 'bdr', 'sdr', 'account executive', 'account manager'],
    skills: ['B2B Sales & Prospecting', 'CRM Management (Salesforce / HubSpot)', 'Lead Qualification (BANT / MEDDIC)', 'Cold Outreach & Follow-up', 'Product Demonstrations & Pitching', 'Contract & Pricing Negotiation', 'Pipeline Management', 'Customer Relationship Building'],
    education: "Bachelor's degree in Business, Marketing, Communications, or demonstrable high-achievement sales track record.",
    responsibilities: [
      'Identify, qualify, and engage prospective enterprise clients through outbound prospecting and inbound follow-up.',
      'Deliver persuasive product demonstrations and value presentations addressing specific client pain points.',
      'Manage end-to-end sales cycle from initial discovery to contract closing, meeting or exceeding quota targets.',
      'Maintain rigorous CRM records and pipeline forecasts in Salesforce/HubSpot with up-to-date deal velocity notes.',
      'Collaborate with customer success and solutions engineering teams to ensure seamless client onboarding.'
    ],
  },
];

/**
 * Match best domain template based on title and department
 */
function findMatchingTemplate(title = '', department = '') {
  const query = `${title} ${department}`.toLowerCase();
  for (const t of DOMAIN_TEMPLATES) {
    if (t.matches.some((m) => query.includes(m))) {
      return t;
    }
  }
  return null;
}

/**
 * Format workplace label
 */
function formatWorkplaceModel(type) {
  if (type === 'on_site' || type === 'on-site') return 'On-Site';
  if (type === 'remote') return 'Remote';
  return 'Hybrid';
}

/**
 * Generates tailored, comprehensive Job Requirements, Skills, Education,
 * Benefits, and Job Description using all entered Job Details + Compensation & Timeline parameters.
 *
 * If skills are entered prior by the user, AI incorporates those specific skills into the description.
 * If skills are left blank, AI automatically deduces and generates the skills from the job role & department.
 */
export function generateRoleRequirements({
  title = '',
  department = '',
  jobType = 'full-time',
  workplaceType = 'hybrid',
  level = 'mid',
  location = '',
  currency = 'INR',
  salaryMin = '',
  salaryMax = '',
  deadline = '',
  existingSkills = '',
  backendData = null,
}) {
  const cleanTitle = title.trim() || 'Software Engineer';
  const cleanDept = department.trim() || 'Engineering';
  const jobTypeLabel = getJobTypeLabel(jobType) || 'Full-time';
  const workplaceLabel = formatWorkplaceModel(workplaceType);
  const levelLabel = getExperienceLevelLabel(level) || 'Mid Level';

  const matchedTemplate = findMatchingTemplate(cleanTitle, cleanDept);
  const hasUserSkills = Boolean(existingSkills && existingSkills.trim().length > 0);

  // 1. Determine Skills:
  // If user entered skills, honor and prioritize their exact entered skills.
  // If user left skills blank, deduce from backend AI data, matched domain template, or smart role inference.
  let skillsArray = [];

  if (hasUserSkills) {
    const userEnteredList = existingSkills
      .split(',')
      .map((s) => s.trim())
      .filter(Boolean);
    skillsArray = userEnteredList;
  } else {
    // Collect from backend AI first if available
    if (backendData?.mustHaveSkills?.length || backendData?.must_have_skills?.length) {
      const raw = backendData.mustHaveSkills || backendData.must_have_skills || [];
      skillsArray.push(...raw);
    }
    if (backendData?.niceToHaveSkills?.length || backendData?.nice_to_have_skills?.length) {
      const raw = backendData.niceToHaveSkills || backendData.nice_to_have_skills || [];
      skillsArray.push(...raw);
    }

    // If backend did not provide skills, use domain templates
    if (skillsArray.length === 0) {
      if (matchedTemplate?.skills) {
        skillsArray = [...matchedTemplate.skills];
      } else {
        // Smart fallback derived from title and foundational industry standards
        const titleWords = cleanTitle
          .split(/[\s/-]+/)
          .filter((w) => w.length > 2 && !['and', 'the', 'for', 'lead', 'senior', 'junior', 'staff', 'principal', 'intern'].includes(w.toLowerCase()));
        
        const dynamicTitleSkills = titleWords.map((w) => w.charAt(0).toUpperCase() + w.slice(1));
        const standardCoreSkills = [
          'Problem Solving',
          'System Architecture',
          'Team Collaboration',
          'Agile Methodologies',
          'Git & Version Control',
          'RESTful APIs',
          'Continuous Integration'
        ];

        skillsArray = Array.from(new Set([...dynamicTitleSkills, ...standardCoreSkills]));
      }
    }
  }

  const generatedSkillsText = skillsArray.join(', ');

  // 2. Generate Education Requirements
  let generatedEducation = matchedTemplate?.education ||
    `Bachelor's or Master's degree in Computer Science, Information Technology, Engineering, or equivalent practical industry experience.`;
  if (level === 'entry') {
    generatedEducation = `Bachelor's degree in Computer Science, Engineering, Mathematics, or equivalent freshly graduated / entry-level background.`;
  } else if (level === 'senior' || level === 'lead' || level === 'executive') {
    generatedEducation = `Bachelor's or Master's degree in relevant discipline with 5+ years of demonstrable track record in ${cleanTitle} leadership.`;
  }

  // 3. Generate Perks & Benefits based on work model and type
  const defaultPerks = [];
  if (workplaceType === 'remote') {
    defaultPerks.push('100% Remote Flexibility', 'Home Office Stipend', 'Flexible Working Hours');
  } else if (workplaceType === 'hybrid') {
    defaultPerks.push('Hybrid Work Flexibility (2-3 days remote/week)', 'Commuter Benefits', 'Collaborative Office Space');
  } else {
    defaultPerks.push('State-of-the-art Office Amenities', 'On-site Cafeteria & Wellness Hub', 'Commuter Allowance');
  }

  if (jobType === 'part-time' || jobType === 'contract') {
    defaultPerks.push('Flexible Scheduling', 'Project Milestone Performance Bonuses', 'Professional Skill Development Stipend');
  } else {
    defaultPerks.push('Comprehensive Health & Medical Insurance', 'Annual Performance Bonus', 'Annual Learning & Certification Budget', 'Generous Paid Time Off (PTO)');
  }
  const generatedBenefitsText = defaultPerks.join(', ');

  // 4. Generate Structured Comprehensive Job Description
  const formattedSalary = (salaryMin || salaryMax)
    ? formatSalary({ min: salaryMin, max: salaryMax, currency, period: 'year' })
    : 'Competitive Market Compensation (Negotiable)';

  const locationSnippet = location?.trim()
    ? `${workplaceLabel} — ${location.trim()}`
    : `${workplaceLabel}`;

  const deadlineSnippet = deadline?.trim()
    ? formatDate(deadline)
    : 'Open until filled (Immediate Hiring)';

  // Build responsibilities
  const responsibilities = (backendData?.responsibilities?.length ? backendData.responsibilities : null) ||
    matchedTemplate?.responsibilities || [
      `Lead the architecture, implementation, and optimization of core ${cleanTitle} initiatives.`,
      `Collaborate with cross-functional product, design, and operations teams to translate strategic goals into technical deliverables.`,
      `Ensure robust test coverage, performance benchmarking, and clean code documentation.`,
      `Continuously innovate and incorporate modern industry best practices into our development workflows.`
    ];

  const respBullets = responsibilities.map((r) => `• ${r}`).join('\n');

  const skillsDisplay = skillsArray.length > 0
    ? skillsArray.slice(0, 8).join(', ')
    : `${cleanTitle} core technologies & tools`;

  const generatedDescription = `### Role Overview
We are looking for a skilled and motivated **${cleanTitle}** (${levelLabel}) to join our growing **${cleanDept}** division on a **${jobTypeLabel}** basis (${locationSnippet}).

In this role, you will collaborate with cross-functional talent to design, build, and deliver high-quality solutions, solve complex challenges, and contribute directly to our product roadmap and business growth.

### Key Responsibilities
${respBullets}

### What We Are Looking For
• Proven hands-on experience as a ${cleanTitle} (${levelLabel} level) or in a directly related role.
• Strong practical proficiency in core technologies: ${skillsDisplay}.
• Demonstrated analytical thinking, problem-solving mindset, and clear communication skills.
• Ability to thrive in a fast-paced, collaborative team environment and take ownership of deliverables.

### Compensation & Work Model
• **Compensation:** ${formattedSalary}
• **Employment Type:** ${jobTypeLabel}
• **Workplace Model:** ${locationSnippet}
• **Application Deadline:** ${deadlineSnippet}

If you are eager to make a tangible impact and grow with a forward-thinking organization, we encourage you to submit your application!`;

  return {
    skills: generatedSkillsText,
    education: generatedEducation,
    benefits: generatedBenefitsText,
    description: generatedDescription,
  };
}
