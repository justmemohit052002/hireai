import { formatSalary, formatDate, getJobTypeLabel, getExperienceLevelLabel } from '@/utils';

/**
 * Intelligent domain knowledge base for generating high-quality ATS skills,
 * role-specific responsibilities, education, and perks.
 */
const DOMAIN_TEMPLATES = [
  {
    matches: ['data analyst', 'business analyst', 'bi analyst', 'analytics', 'data analysis', 'bi developer', 'data specialist', 'analyst'],
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
    matches: ['data scientist', 'machine learning', 'ml engineer', 'ai engineer', 'nlp', 'computer vision', 'deep learning'],
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
    matches: ['frontend', 'react', 'vue', 'angular', 'web developer', 'ui developer', 'javascript developer'],
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
    matches: ['backend', 'java', 'spring', 'node', 'python developer', 'golang', 'c#', '.net', 'api developer', 'systems engineer'],
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
    matches: ['full stack', 'fullstack', 'software engineer', 'software developer', 'sde'],
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
    matches: ['devops', 'cloud', 'sre', 'infrastructure', 'platform engineer', 'kubernetes', 'aws'],
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
    matches: ['qa', 'test', 'sdet', 'quality assurance', 'automation engineer'],
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
    matches: ['product manager', 'product owner', 'tpm', 'program manager'],
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
    matches: ['ui', 'ux', 'designer', 'product designer', 'visual designer'],
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

  // 1. Generate Skills
  let skillsArray = [];
  if (backendData?.mustHaveSkills?.length || backendData?.must_have_skills?.length) {
    const raw = backendData.mustHaveSkills || backendData.must_have_skills || [];
    skillsArray.push(...raw);
  }
  if (backendData?.niceToHaveSkills?.length || backendData?.nice_to_have_skills?.length) {
    const raw = backendData.niceToHaveSkills || backendData.nice_to_have_skills || [];
    skillsArray.push(...raw);
  }
  if (skillsArray.length === 0) {
    if (matchedTemplate?.skills) {
      skillsArray = [...matchedTemplate.skills];
    } else {
      skillsArray = [
        'Problem Solving',
        'System Architecture',
        'Team Collaboration',
        'Agile Methodologies',
        'Git & Version Control',
        'Data Structures & Algorithms',
        'Continuous Integration'
      ];
    }
  }
  // If user had existing skills, merge them gracefully
  if (existingSkills?.trim()) {
    const existingList = existingSkills.split(',').map((s) => s.trim()).filter(Boolean);
    const set = new Set([...existingList, ...skillsArray]);
    skillsArray = Array.from(set);
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

  const generatedDescription = `### Role Overview
We are looking for a skilled and motivated **${cleanTitle}** (${levelLabel}) to join our growing **${cleanDept}** division on a **${jobTypeLabel}** basis (${locationSnippet}).

In this role, you will collaborate with cross-functional talent to design, build, and deliver high-quality solutions, solve complex challenges, and contribute directly to our product roadmap and business growth.

### Key Responsibilities
${respBullets}

### What We Are Looking For
• Proven hands-on experience as a ${cleanTitle} (${levelLabel} level) or in a directly related role.
• Strong practical proficiency in core technologies: ${skillsArray.slice(0, 6).join(', ')}.
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
