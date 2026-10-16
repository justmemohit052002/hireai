import { formatSalary, formatDate, getJobTypeLabel, getExperienceLevelLabel } from '@/utils';

/**
 * Curated, versioned role profile catalog fallback for client-side generation
 * and offline resilience, strictly synchronized with the Python AI Engine catalog.
 */
export const ROLE_CATALOG_FALLBACK = {
  java_backend: {
    name: 'Java / Backend Developer',
    matches: ['java', 'spring', 'spring boot', 'jvm', 'kotlin backend', 'backend java'],
    levels: {
      entry: {
        required: ['Java', 'Object-Oriented Programming', 'Data Structures and Algorithms', 'SQL', 'Spring Boot', 'REST APIs', 'JUnit', 'Maven', 'Git', 'Postman'],
        preferred: ['Docker', 'Linux', 'Hibernate', 'Agile Methodologies', 'Mockito', 'AWS'],
        education: "Bachelor's degree in Computer Science, Information Technology, or equivalent practical background",
        experienceYears: { min: 0, max: 2 },
      },
      mid: {
        required: ['Java', 'Spring Boot', 'Spring Security', 'Hibernate', 'PostgreSQL', 'REST APIs', 'Microservices', 'JUnit', 'Mockito', 'Docker', 'Git', 'CI/CD'],
        preferred: ['Redis', 'Kafka', 'AWS', 'Linux', 'Performance Tuning', 'Agile Methodologies'],
        education: "Bachelor's degree in Computer Science, Software Engineering, or related technical field",
        experienceYears: { min: 2, max: 5 },
      },
      senior: {
        required: ['Java', 'Spring Boot', 'Microservices', 'Distributed Systems', 'System Design', 'PostgreSQL', 'Redis', 'Kafka', 'Spring Security', 'Docker', 'Kubernetes', 'AWS', 'Mentoring'],
        preferred: ['CI/CD', 'Observability', 'NoSQL', 'Performance Optimization', 'Database Tuning', 'Technical Leadership'],
        education: "Bachelor's or Master's degree in Computer Science, Software Engineering, or equivalent experience",
        experienceYears: { min: 5, max: 8 },
      },
      lead: {
        required: ['Java', 'Spring Boot', 'System Design', 'Distributed Systems', 'Architecture Ownership', 'Team Leadership', 'Microservices', 'PostgreSQL', 'Kafka', 'Code Review', 'Delivery Management', 'Mentoring'],
        preferred: ['AWS', 'Kubernetes', 'Technical Strategy', 'CI/CD', 'Observability', 'Agile Coaching'],
        education: "Bachelor's or Master's degree with demonstrable engineering leadership track record",
        experienceYears: { min: 8, max: 12 },
      },
      executive: {
        required: ['Technology Strategy', 'Engineering Leadership', 'Technology Governance', 'System Architecture', 'Budgeting', 'Hiring', 'Vendor Management', 'Enterprise Security', 'Roadmap Planning', 'Stakeholder Management'],
        preferred: ['Cloud Strategy', 'Distributed Systems', 'Software Quality Governance', 'Digital Transformation', 'Risk Management'],
        education: "Bachelor's or Master's degree with executive credentials",
        experienceYears: { min: 10, max: 20 },
      },
    },
  },
  full_stack_java_react: {
    name: 'Full Stack Developer (Java + React)',
    matches: ['full stack', 'fullstack', 'react java', 'java react'],
    levels: {
      entry: {
        required: ['Java', 'JavaScript', 'React', 'HTML5', 'CSS3', 'SQL', 'Spring Boot', 'REST APIs', 'Git', 'Data Structures and Algorithms'],
        preferred: ['TypeScript', 'JUnit', 'Postman', 'Maven', 'Tailwind CSS', 'Docker'],
        education: "Bachelor's degree in Computer Science, Information Technology, or practical web experience",
        experienceYears: { min: 0, max: 2 },
      },
      mid: {
        required: ['Java', 'React', 'TypeScript', 'Spring Boot', 'Hibernate', 'PostgreSQL', 'REST APIs', 'Tailwind CSS', 'Redux', 'Docker', 'Git', 'Jest'],
        preferred: ['Microservices', 'CI/CD', 'Redis', 'AWS', 'JUnit', 'Agile Methodologies'],
        education: "Bachelor's degree in Computer Science or Software Engineering",
        experienceYears: { min: 2, max: 5 },
      },
      senior: {
        required: ['Java', 'Spring Boot', 'React', 'TypeScript', 'System Design', 'Microservices', 'Distributed Systems', 'PostgreSQL', 'State Management', 'Docker', 'AWS', 'CI/CD', 'Mentoring'],
        preferred: ['Redis', 'Kafka', 'Kubernetes', 'GraphQL', 'Next.js', 'Web Performance', 'Testing Strategies'],
        education: "Bachelor's or Master's degree in Computer Science or equivalent",
        experienceYears: { min: 5, max: 8 },
      },
      lead: {
        required: ['Java', 'React', 'TypeScript', 'System Design', 'Full Stack Architecture', 'Team Leadership', 'Microservices', 'PostgreSQL', 'Code Review', 'Delivery Management', 'Mentoring', 'Architecture Ownership'],
        preferred: ['Cloud Architecture', 'CI/CD', 'Kafka', 'Technical Strategy', 'Observability', 'Agile Coaching'],
        education: "Bachelor's or Master's degree with engineering lead experience",
        experienceYears: { min: 8, max: 12 },
      },
      executive: {
        required: ['Technology Strategy', 'Engineering Leadership', 'System Architecture', 'Roadmap Planning', 'Technology Governance', 'Hiring', 'Budgeting', 'Stakeholder Management', 'Vendor Management', 'Enterprise Security'],
        preferred: ['Cloud Governance', 'Product Strategy', 'Engineering Culture', 'Operational Excellence', 'Scalability Strategy'],
        education: "Bachelor's or Master's degree with executive track record",
        experienceYears: { min: 10, max: 20 },
      },
    },
  },
  frontend: {
    name: 'Frontend Developer',
    matches: ['frontend', 'front end', 'react', 'vue', 'angular', 'ui developer', 'javascript developer', 'web developer'],
    levels: {
      entry: {
        required: ['JavaScript', 'React', 'HTML5', 'CSS3', 'Data Structures and Algorithms', 'REST APIs', 'Git', 'Responsive Web Design', 'Postman', 'Browser Developer Tools'],
        preferred: ['TypeScript', 'Tailwind CSS', 'Jest', 'Webpack', 'Vite', 'Figma'],
        education: "Bachelor's degree in Computer Science, Software Engineering, or digital portfolio",
        experienceYears: { min: 0, max: 2 },
      },
      mid: {
        required: ['JavaScript', 'TypeScript', 'React', 'HTML5', 'CSS3', 'Tailwind CSS', 'State Management', 'REST APIs', 'Jest', 'Vite', 'Git', 'Web Performance'],
        preferred: ['Next.js', 'Redux', 'GraphQL', 'CI/CD', 'Responsive Web Design', 'Accessibility'],
        education: "Bachelor's degree in Computer Science or Design Technology",
        experienceYears: { min: 2, max: 5 },
      },
      senior: {
        required: ['JavaScript', 'TypeScript', 'React', 'Next.js', 'State Management', 'Frontend Architecture', 'Web Performance', 'Core Web Vitals', 'Testing Library', 'CI/CD', 'Design Systems', 'REST APIs', 'Mentoring'],
        preferred: ['GraphQL', 'Micro Frontends', 'WebSockets', 'Accessibility', 'Docker', 'Tailwind CSS'],
        education: "Bachelor's or Master's degree in Computer Science",
        experienceYears: { min: 5, max: 8 },
      },
      lead: {
        required: ['JavaScript', 'TypeScript', 'React', 'Frontend Architecture', 'Design Systems', 'Team Leadership', 'Web Performance', 'Code Review', 'Delivery Management', 'Mentoring', 'Testing Strategy', 'Technical Strategy'],
        preferred: ['Micro Frontends', 'CI/CD', 'Next.js', 'Accessibility', 'Cross-Browser Compatibility', 'Stakeholder Management'],
        education: "Bachelor's or Master's degree with frontend leadership background",
        experienceYears: { min: 8, max: 12 },
      },
      executive: {
        required: ['Technology Strategy', 'Engineering Leadership', 'Frontend Platform Strategy', 'User Experience Strategy', 'Technology Governance', 'Hiring', 'Budgeting', 'Stakeholder Management', 'Vendor Management', 'Engineering Culture'],
        preferred: ['Product Alignment', 'Digital Transformation', 'Web Standards', 'Operational Excellence', 'Performance Governance'],
        education: "Bachelor's or Master's degree with executive credentials",
        experienceYears: { min: 10, max: 20 },
      },
    },
  },
  python_backend: {
    name: 'Python / Backend Developer',
    matches: ['python', 'django', 'fastapi', 'flask'],
    levels: {
      entry: {
        required: ['Python', 'Object-Oriented Programming', 'Data Structures and Algorithms', 'SQL', 'FastAPI', 'REST APIs', 'PyTest', 'Git', 'Postman', 'Linux'],
        preferred: ['Django', 'PostgreSQL', 'Docker', 'Redis', 'Virtualenv', 'Agile Methodologies'],
        education: "Bachelor's degree in Computer Science, Software Engineering, or equivalent",
        experienceYears: { min: 0, max: 2 },
      },
      mid: {
        required: ['Python', 'FastAPI', 'Django', 'PostgreSQL', 'SQLAlchemy', 'REST APIs', 'PyTest', 'Docker', 'Redis', 'Git', 'Microservices', 'CI/CD'],
        preferred: ['Celery', 'Kafka', 'AWS', 'Linux', 'Performance Profiling', 'Asynchronous Programming'],
        education: "Bachelor's degree in Computer Science, Information Systems, or related field",
        experienceYears: { min: 2, max: 5 },
      },
      senior: {
        required: ['Python', 'FastAPI', 'Microservices', 'Distributed Systems', 'System Design', 'PostgreSQL', 'Redis', 'Celery', 'Kafka', 'Docker', 'AWS', 'CI/CD', 'Mentoring'],
        preferred: ['Kubernetes', 'SQLAlchemy', 'Asynchronous Programming', 'NoSQL', 'Database Optimization', 'Observability'],
        education: "Bachelor's or Master's degree in Computer Science or Software Engineering",
        experienceYears: { min: 5, max: 8 },
      },
      lead: {
        required: ['Python', 'FastAPI', 'System Design', 'Distributed Systems', 'Architecture Ownership', 'Team Leadership', 'Microservices', 'PostgreSQL', 'Kafka', 'Code Review', 'Delivery Management', 'Mentoring'],
        preferred: ['AWS', 'Kubernetes', 'Technical Strategy', 'CI/CD', 'Observability', 'Agile Coaching'],
        education: "Bachelor's or Master's degree with technical leadership experience",
        experienceYears: { min: 8, max: 12 },
      },
      executive: {
        required: ['Technology Strategy', 'Engineering Leadership', 'System Architecture', 'Roadmap Planning', 'Technology Governance', 'Hiring', 'Budgeting', 'Stakeholder Management', 'Enterprise Security', 'Vendor Management'],
        preferred: ['Cloud Strategy', 'Data Governance', 'Operational Excellence', 'Technical Due Diligence', 'Scalability Strategy'],
        education: "Bachelor's or Master's degree with executive technical background",
        experienceYears: { min: 10, max: 20 },
      },
    },
  },
  devops_cloud: {
    name: 'DevOps / Cloud Engineer',
    matches: ['devops', 'cloud', 'sre', 'site reliability', 'infrastructure', 'platform engineer', 'kubernetes', 'aws'],
    levels: {
      entry: {
        required: ['Linux', 'Bash Scripting', 'Git', 'Python', 'Networking Basics', 'Docker', 'CI/CD Basics', 'Cloud Fundamentals', 'Virtualization', 'Postman'],
        preferred: ['AWS', 'Terraform', 'GitHub Actions', 'Nginx', 'YAML', 'Monitoring Basics'],
        education: "Bachelor's degree in Computer Science, Information Technology, or Network Engineering",
        experienceYears: { min: 0, max: 2 },
      },
      mid: {
        required: ['Linux', 'Docker', 'Kubernetes', 'AWS', 'Terraform', 'CI/CD', 'Bash Scripting', 'Python', 'Git', 'Prometheus', 'Grafana', 'Infrastructure as Code'],
        preferred: ['Ansible', 'Helm', 'GitHub Actions', 'GCP', 'Azure', 'Security Best Practices'],
        education: "Bachelor's degree in Computer Science, Systems Engineering, or certification equivalent",
        experienceYears: { min: 2, max: 5 },
      },
      senior: {
        required: ['Kubernetes', 'Cloud Architecture', 'AWS', 'Terraform', 'Infrastructure as Code', 'CI/CD', 'Prometheus', 'Grafana', 'Observability', 'Security and Compliance', 'Distributed Systems', 'System Design', 'Mentoring'],
        preferred: ['Helm', 'GitOps', 'Cost Optimization', 'Disaster Recovery', 'Linux', 'Azure', 'Site Reliability Engineering'],
        education: "Bachelor's or Master's degree in Computer Science or significant cloud track record",
        experienceYears: { min: 5, max: 8 },
      },
      lead: {
        required: ['Cloud Architecture', 'Kubernetes', 'Terraform', 'Infrastructure as Code', 'Site Reliability Engineering', 'Team Leadership', 'Security and Compliance', 'Architecture Ownership', 'Disaster Recovery', 'Cost Optimization', 'Mentoring', 'Code Review'],
        preferred: ['CI/CD', 'GitOps', 'Observability', 'Technical Strategy', 'AWS', 'Stakeholder Management'],
        education: "Bachelor's or Master's degree with infrastructure leadership credentials",
        experienceYears: { min: 8, max: 12 },
      },
      executive: {
        required: ['Cloud Strategy', 'Infrastructure Governance', 'Engineering Leadership', 'Security and Compliance', 'Technology Governance', 'Budgeting', 'Vendor Management', 'Disaster Recovery Strategy', 'Hiring', 'Stakeholder Management'],
        preferred: ['Site Reliability Engineering', 'Cost Optimization', 'Multi-Cloud Strategy', 'Operational Excellence', 'Risk Management'],
        education: "Bachelor's or Master's degree with executive platform credentials",
        experienceYears: { min: 10, max: 20 },
      },
    },
  },
  qa_automation: {
    name: 'QA / Automation Engineer',
    matches: ['qa', 'sdet', 'test engineer', 'automation engineer', 'quality assurance', 'tester'],
    levels: {
      entry: {
        required: ['Software Testing Fundamentals', 'Test Case Design', 'Java', 'Selenium WebDriver', 'Test Execution', 'Bug Tracking', 'JIRA', 'SQL', 'Git', 'Postman'],
        preferred: ['TestNG', 'JUnit', 'Python', 'API Testing', 'Agile Methodologies', 'CI/CD Basics'],
        education: "Bachelor's degree in Computer Science, Software Engineering, or related technical field",
        experienceYears: { min: 0, max: 2 },
      },
      mid: {
        required: ['Test Automation', 'Selenium WebDriver', 'Java', 'TestNG', 'API Testing', 'Postman', 'REST Assured', 'SQL', 'JIRA', 'Git', 'CI/CD', 'Test Execution'],
        preferred: ['Cypress', 'Playwright', 'Cucumber', 'Python', 'Docker', 'Performance Testing Basics'],
        education: "Bachelor's degree in Computer Science or Software Engineering",
        experienceYears: { min: 2, max: 5 },
      },
      senior: {
        required: ['Test Automation Architecture', 'Framework Design', 'Selenium WebDriver', 'Playwright', 'API Testing', 'REST Assured', 'CI/CD Integration', 'Performance Testing', 'JMeter', 'SQL', 'Test Strategy', 'Git', 'Mentoring'],
        preferred: ['Cypress', 'Docker', 'Security Testing', 'Cross-Browser Testing', 'TestNG', 'Agile Methodologies'],
        education: "Bachelor's or Master's degree in Computer Science or extensive SDET experience",
        experienceYears: { min: 5, max: 8 },
      },
      lead: {
        required: ['Quality Engineering Leadership', 'Test Strategy', 'Test Automation Architecture', 'Framework Design', 'Team Leadership', 'CI/CD Integration', 'Delivery Management', 'Code Review', 'Mentoring', 'Defect Management', 'Performance Testing', 'Stakeholder Communication'],
        preferred: ['Playwright', 'Security Testing', 'Test Metrics', 'Agile Coaching', 'Cloud Testing', 'Technical Strategy'],
        education: "Bachelor's or Master's degree with QA / SDET leadership track record",
        experienceYears: { min: 8, max: 12 },
      },
      executive: {
        required: ['Quality Governance', 'Engineering Leadership', 'Technology Strategy', 'Software Quality Standards', 'Risk Management', 'Hiring', 'Budgeting', 'Stakeholder Management', 'Vendor Management', 'Technology Governance'],
        preferred: ['Continuous Testing Strategy', 'Shift-Left Adoption', 'Test Operations', 'Compliance', 'Operational Excellence'],
        education: "Bachelor's or Master's degree with executive quality governance background",
        experienceYears: { min: 10, max: 20 },
      },
    },
  },
  data_ml: {
    name: 'Data / Machine Learning Engineer',
    matches: ['data science', 'machine learning', 'ml engineer', 'ai engineer', 'data engineer', 'deep learning', 'nlp'],
    levels: {
      entry: {
        required: ['Python', 'SQL', 'Data Structures and Algorithms', 'Pandas', 'NumPy', 'Scikit-Learn', 'Mathematics and Statistics', 'Git', 'Exploratory Data Analysis', 'Data Visualization'],
        preferred: ['Matplotlib', 'Jupyter', 'PyTorch', 'Docker', 'Feature Engineering', 'REST APIs'],
        education: "Bachelor's or Master's degree in Data Science, Computer Science, or Mathematics",
        experienceYears: { min: 0, max: 2 },
      },
      mid: {
        required: ['Python', 'SQL', 'Pandas', 'NumPy', 'Scikit-Learn', 'PyTorch', 'Machine Learning', 'Feature Engineering', 'Model Evaluation', 'Docker', 'Git', 'Data Pipelines'],
        preferred: ['TensorFlow', 'MLOps', 'FastAPI', 'MLflow', 'AWS', 'PostgreSQL'],
        education: "Bachelor's or Master's degree in Data Science, Computer Science, or quantitative field",
        experienceYears: { min: 2, max: 5 },
      },
      senior: {
        required: ['Python', 'Machine Learning', 'Deep Learning', 'PyTorch', 'MLOps', 'Model Deployment', 'Feature Engineering', 'Distributed Computing', 'System Design', 'SQL', 'Docker', 'Cloud Platforms', 'Mentoring'],
        preferred: ['MLflow', 'Kubeflow', 'Apache Spark', 'Large Language Models', 'Data Pipelines', 'Observability'],
        education: "Master's or Ph.D. in Computer Science or Artificial Intelligence",
        experienceYears: { min: 5, max: 8 },
      },
      lead: {
        required: ['Python', 'Machine Learning', 'Deep Learning', 'MLOps', 'AI Strategy', 'Architecture Ownership', 'Team Leadership', 'Data Governance', 'Model Deployment', 'Code Review', 'Mentoring', 'Delivery Management'],
        preferred: ['Large Language Models', 'Cloud Platforms', 'Distributed Computing', 'Stakeholder Management', 'Kubeflow', 'Ethical AI'],
        education: "Master's or Ph.D. with track record of leading applied ML / AI systems in production",
        experienceYears: { min: 8, max: 12 },
      },
      executive: {
        required: ['AI Strategy', 'Data Strategy', 'Engineering Leadership', 'Technology Governance', 'Ethical AI', 'Hiring', 'Budgeting', 'Roadmap Planning', 'Stakeholder Management', 'Vendor Management'],
        preferred: ['Enterprise AI Adoption', 'Data Governance', 'Research Direction', 'Operational Excellence', 'Cloud Cost Optimization'],
        education: "Advanced quantitative degree with executive data leadership credentials",
        experienceYears: { min: 10, max: 20 },
      },
    },
  },
  mobile: {
    name: 'Mobile Application Developer',
    matches: ['mobile', 'android', 'ios', 'flutter', 'react native', 'swift', 'kotlin'],
    levels: {
      entry: {
        required: ['Mobile Application Development', 'Kotlin', 'Java', 'Android SDK', 'Object-Oriented Programming', 'Data Structures and Algorithms', 'REST APIs', 'Git', 'UI Layouts', 'Postman'],
        preferred: ['Swift', 'Flutter', 'SQLite', 'Firebase', 'Android Studio', 'Agile Methodologies'],
        education: "Bachelor's degree in Computer Science, Mobile Computing, or app portfolio",
        experienceYears: { min: 0, max: 2 },
      },
      mid: {
        required: ['Kotlin', 'Android SDK', 'Flutter', 'Dart', 'REST APIs', 'State Management', 'SQLite', 'Git', 'Firebase', 'Unit Testing', 'App Store Deployment', 'Clean Architecture'],
        preferred: ['Swift', 'iOS SDK', 'CI/CD', 'Jetpack Compose', 'GraphQL', 'Push Notifications'],
        education: "Bachelor's degree in Computer Science or Mobile Engineering",
        experienceYears: { min: 2, max: 5 },
      },
      senior: {
        required: ['Mobile Architecture', 'Kotlin', 'Swift', 'Flutter', 'State Management', 'Performance Optimization', 'Offline Synchronization', 'CI/CD', 'App Store Deployment', 'Automated Testing', 'REST APIs', 'Security Best Practices', 'Mentoring'],
        preferred: ['Jetpack Compose', 'SwiftUI', 'Reactive Programming', 'GraphQL', 'Firebase', 'Memory Management'],
        education: "Bachelor's or Master's degree in Computer Science or significant mobile production history",
        experienceYears: { min: 5, max: 8 },
      },
      lead: {
        required: ['Mobile Architecture', 'Cross-Platform Architecture', 'Team Leadership', 'App Lifecycle Management', 'Code Review', 'Delivery Management', 'Mentoring', 'Performance Optimization', 'Security Best Practices', 'App Store Compliance', 'Release Management', 'Technical Strategy'],
        preferred: ['Kotlin', 'Swift', 'Flutter', 'CI/CD', 'Stakeholder Management', 'Agile Coaching'],
        education: "Bachelor's or Master's degree with mobile leadership track record",
        experienceYears: { min: 8, max: 12 },
      },
      executive: {
        required: ['Mobile Strategy', 'Technology Strategy', 'Engineering Leadership', 'Mobile Platform Governance', 'App Ecosystem Strategy', 'Hiring', 'Budgeting', 'Stakeholder Management', 'Vendor Management', 'Security and Compliance'],
        preferred: ['Product Alignment', 'User Experience Governance', 'Digital Transformation', 'Operational Excellence', 'Release Governance'],
        education: "Bachelor's or Master's degree with executive mobile credentials",
        experienceYears: { min: 10, max: 20 },
      },
    },
  },
  generic: {
    name: 'Software Engineer (General)',
    matches: ['software engineer', 'software developer', 'sde', 'developer', 'engineer', 'programmer'],
    levels: {
      entry: {
        required: ['Programming Fundamentals', 'Object-Oriented Programming', 'Data Structures and Algorithms', 'SQL', 'REST APIs', 'Version Control', 'Git', 'Software Testing', 'Problem Solving', 'Command Line'],
        preferred: ['Relational Databases', 'Agile Methodologies', 'Postman', 'CI/CD Basics', 'Docker Basics', 'Code Quality'],
        education: "Bachelor's degree in Computer Science, Information Technology, or practical experience",
        experienceYears: { min: 0, max: 2 },
      },
      mid: {
        required: ['Software Engineering', 'Object-Oriented Programming', 'Relational Databases', 'SQL', 'REST APIs', 'Microservices Basics', 'Unit Testing', 'Git', 'Docker', 'CI/CD', 'Code Review', 'Problem Solving'],
        preferred: ['Cloud Platforms', 'Caching', 'Linux', 'Database Optimization', 'Agile Methodologies', 'Software Architecture'],
        education: "Bachelor's degree in Computer Science, Software Engineering, or related field",
        experienceYears: { min: 2, max: 5 },
      },
      senior: {
        required: ['System Design', 'Software Architecture', 'Distributed Systems', 'Microservices', 'Relational Databases', 'NoSQL', 'API Design', 'Security Best Practices', 'CI/CD', 'Docker', 'Cloud Computing', 'Code Review', 'Mentoring'],
        preferred: ['Observability', 'Performance Optimization', 'Messaging Systems', 'Database Tuning', 'High Availability', 'Kubernetes', 'Agile Methodologies'],
        education: "Bachelor's or Master's degree in Computer Science or equivalent",
        experienceYears: { min: 5, max: 8 },
      },
      lead: {
        required: ['System Architecture', 'System Design', 'Distributed Systems', 'Architecture Ownership', 'Team Leadership', 'Code Review', 'Delivery Management', 'Mentoring', 'Cross-Team Collaboration', 'Technical Strategy', 'Software Quality Standards', 'Stakeholder Communication'],
        preferred: ['Cloud Platforms', 'Microservices', 'CI/CD', 'Cost Optimization', 'Agile Coaching', 'Risk Management'],
        education: "Bachelor's or Master's degree with engineering leadership track record",
        experienceYears: { min: 8, max: 12 },
      },
      executive: {
        required: ['Technology Strategy', 'Engineering Leadership', 'System Architecture', 'Roadmap Planning', 'Technology Governance', 'Hiring', 'Budgeting', 'Stakeholder Management', 'Vendor Management', 'Enterprise Security'],
        preferred: ['Cloud Governance', 'Organizational Design', 'Technical Due Diligence', 'Digital Transformation', 'Operational Excellence'],
        education: "Bachelor's or Master's degree with executive engineering credentials",
        experienceYears: { min: 10, max: 20 },
      },
    },
  },
};

export function detectRoleFamilyFrontend(title = '', department = '') {
  const query = `${title} ${department}`.toLowerCase();
  for (const [key, family] of Object.entries(ROLE_CATALOG_FALLBACK)) {
    if (family.matches.some((m) => query.includes(m))) {
      return key;
    }
  }
  return 'generic';
}

function formatWorkplaceModel(type) {
  if (type === 'on_site' || type === 'on-site') return 'On-Site';
  if (type === 'remote') return 'Remote';
  return 'Hybrid';
}

function normalizeLevelKey(level) {
  if (!level) return 'mid';
  const l = String(level).toLowerCase().replace(/[-\s]/g, '_');
  if (l.includes('fresher') || l.includes('entry') || l.includes('junior')) return 'entry';
  if (l.includes('lead') || l.includes('staff') || l.includes('principal')) return 'lead';
  if (l.includes('executive') || l.includes('director') || l.includes('vp')) return 'executive';
  if (l.includes('senior') || l.includes('sr')) return 'senior';
  return 'mid';
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
  existingPreferredSkills = '',
  backendData = null,
}) {
  const cleanTitle = title.trim() || 'Software Engineer';
  const cleanDept = department.trim() || 'Engineering';
  const jobTypeLabel = getJobTypeLabel(jobType) || 'Full-time';
  const workplaceLabel = formatWorkplaceModel(workplaceType);
  const normLevel = normalizeLevelKey(level);
  const levelLabel = getExperienceLevelLabel(level) || 'Mid Level';

  const roleFamKey = detectRoleFamilyFrontend(cleanTitle, cleanDept);
  const familyData = ROLE_CATALOG_FALLBACK[roleFamKey] || ROLE_CATALOG_FALLBACK.generic;
  const levelBaseline = familyData.levels[normLevel] || familyData.levels.mid;

  const hasUserSkills = Boolean(existingSkills && existingSkills.trim().length > 0);
  const hasUserPrefSkills = Boolean(existingPreferredSkills && existingPreferredSkills.trim().length > 0);

  // 1. Required Skills
  let requiredList = [];
  if (hasUserSkills) {
    requiredList = existingSkills
      .split(/,\s*(?![^()]*\))/)
      .map((s) => s.trim())
      .filter(Boolean);
  } else if (backendData?.required?.length || backendData?.mustHaveSkills?.length || backendData?.must_have_skills?.length) {
    const raw = backendData.required || backendData.mustHaveSkills || backendData.must_have_skills || [];
    requiredList = Array.from(new Set(raw.map((s) => (typeof s === 'string' ? s.trim() : '')).filter(Boolean)));
  } else {
    requiredList = [...levelBaseline.required];
  }

  // 2. Preferred Skills
  let preferredList = [];
  if (hasUserPrefSkills) {
    preferredList = existingPreferredSkills
      .split(/,\s*(?![^()]*\))/)
      .map((s) => s.trim())
      .filter(Boolean);
  } else if (backendData?.preferred?.length || backendData?.niceToHaveSkills?.length || backendData?.nice_to_have_skills?.length) {
    const raw = backendData.preferred || backendData.niceToHaveSkills || backendData.nice_to_have_skills || [];
    preferredList = Array.from(new Set(raw.map((s) => (typeof s === 'string' ? s.trim() : '')).filter(Boolean)));
  } else {
    preferredList = [...levelBaseline.preferred];
  }

  // Deduplicate preferred from required
  const reqLower = new Set(requiredList.map((s) => s.toLowerCase()));
  preferredList = preferredList.filter((p) => !reqLower.has(p.toLowerCase()));

  // 3. Education
  const education = backendData?.education || levelBaseline.education;

  // 4. Perks & Benefits based on work model and type
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

  // 5. Structured Comprehensive Job Description
  const formattedSalary = (salaryMin || salaryMax)
    ? formatSalary({ min: salaryMin, max: salaryMax, currency, period: 'year' })
    : 'Competitive Market Compensation (Negotiable)';

  const locationSnippet = location?.trim()
    ? `${workplaceLabel} | ${location.trim()}`
    : `${workplaceLabel}`;

  const deadlineSnippet = deadline?.trim()
    ? formatDate(deadline)
    : 'Open until filled (Immediate Hiring)';

  // Build responsibilities
  const responsibilities = (backendData?.responsibilities?.length ? backendData.responsibilities : null) || [
    `Lead the architecture, implementation, and optimization of core ${cleanTitle} initiatives.`,
    `Collaborate with cross-functional product, design, and operations teams to translate strategic goals into technical deliverables.`,
    `Ensure robust test coverage, performance benchmarking, and clean code documentation adhering to ${normLevel} engineering standards.`,
    `Continuously innovate and incorporate modern industry best practices into our development workflows.`
  ];

  const respBullets = responsibilities.map((r) => `• ${r}`).join('\n');
  const skillsDisplay = requiredList.slice(0, 8).join(', ');

  const generatedDescription = backendData?.description || `### Role Overview
We are looking for a skilled and motivated **${cleanTitle}** (${levelLabel}) to join our growing **${cleanDept}** division on a **${jobTypeLabel}** basis (${locationSnippet}).

In this role, you will collaborate with cross-functional talent to design, build, and deliver high-quality solutions, solve complex challenges, and contribute directly to our product roadmap and business growth.

### Key Responsibilities
${respBullets}

### What We Are Looking For
• Proven hands-on experience as a ${cleanTitle} (${levelLabel} level) or in a directly related role.
• Strong practical proficiency in core required technologies: ${skillsDisplay}.
• Preferred experience in: ${preferredList.slice(0, 6).join(', ')}.
• Demonstrated analytical thinking, problem-solving mindset, and clear communication skills.
• Ability to thrive in a fast-paced, collaborative team environment and take ownership of deliverables.

### Compensation & Work Model
• **Compensation:** ${formattedSalary}
• **Employment Type:** ${jobTypeLabel}
• **Workplace Model:** ${locationSnippet}
• **Application Deadline:** ${deadlineSnippet}

If you are eager to make a tangible impact and grow with a forward-thinking organization, we encourage you to submit your application!`;

  return {
    skills: requiredList.join(', '),
    preferredSkills: preferredList.join(', '),
    requiredList,
    preferredList,
    education,
    benefits: generatedBenefitsText,
    description: generatedDescription,
    roleFamilyName: familyData.name,
    levelLabel,
    experienceYears: backendData?.experienceYears || backendData?.experience_years || levelBaseline.experienceYears,
  };
}
