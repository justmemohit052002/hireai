// ─────────────────────────────────────────
// Route Constants
// ─────────────────────────────────────────

export const ROUTES = {
  // Public
  HOME: '/',
  LOGIN: '/login',
  SIGNUP: '/signup',

  // Candidate
  CANDIDATE_JOBS: '/candidate/jobs',
  CANDIDATE_APPLICATIONS: '/candidate/applications',
  CANDIDATE_INBOX: '/candidate/inbox',
  CANDIDATE_PROFILE: '/candidate/profile',
  CANDIDATE_SETTINGS: '/candidate/settings',

  // Recruiter
  RECRUITER_DASHBOARD: '/recruiter/dashboard',
  RECRUITER_JOBS: '/recruiter/jobs',
  RECRUITER_JOBS_CREATE: '/recruiter/jobs/create',
  RECRUITER_LEADERBOARD: '/recruiter/leaderboard',
  RECRUITER_INBOX: '/recruiter/inbox',
  RECRUITER_PROFILE: '/recruiter/profile',
  RECRUITER_SETTINGS: '/recruiter/settings',
};

// ─────────────────────────────────────────
// Navigation Items
// ─────────────────────────────────────────

export const CANDIDATE_NAV_ITEMS = [
  { label: 'Browse Jobs', href: ROUTES.CANDIDATE_JOBS, icon: 'Briefcase' },
  { label: 'Applications', href: ROUTES.CANDIDATE_APPLICATIONS, icon: 'FileText' },
  { label: 'Inbox', href: ROUTES.CANDIDATE_INBOX, icon: 'MessageSquare' },
  { label: 'Profile', href: ROUTES.CANDIDATE_PROFILE, icon: 'User' },
  { label: 'Settings', href: ROUTES.CANDIDATE_SETTINGS, icon: 'Settings' },
];

export const RECRUITER_NAV_ITEMS = [
  { label: 'Dashboard', href: ROUTES.RECRUITER_DASHBOARD, icon: 'LayoutDashboard' },
  { label: 'Jobs', href: ROUTES.RECRUITER_JOBS, icon: 'Briefcase' },
  { label: 'Leaderboard', href: ROUTES.RECRUITER_LEADERBOARD, icon: 'Trophy' },
  { label: 'Inbox', href: ROUTES.RECRUITER_INBOX, icon: 'MessageSquare' },
  { label: 'Profile', href: ROUTES.RECRUITER_PROFILE, icon: 'Building2' },
  { label: 'Settings', href: ROUTES.RECRUITER_SETTINGS, icon: 'Settings' },
];

// ─────────────────────────────────────────
// App Constants
// ─────────────────────────────────────────

export const APP_NAME = 'HireAI';
export const APP_TAGLINE = 'The AI-Powered Recruitment Platform';

export const JOB_TYPES = [
  { value: 'full-time', label: 'Full-time' },
  { value: 'part-time', label: 'Part-time' },
  { value: 'contract', label: 'Contract' },
  { value: 'internship', label: 'Internship' },
  { value: 'remote', label: 'Remote' },
];

export const EXPERIENCE_LEVELS = [
  { value: 'entry', label: 'Entry Level' },
  { value: 'mid', label: 'Mid Level' },
  { value: 'senior', label: 'Senior' },
  { value: 'lead', label: 'Lead' },
  { value: 'executive', label: 'Executive' },
];

export const COMPANY_SIZES = [
  { value: '1-10', label: '1-10 employees' },
  { value: '11-50', label: '11-50 employees' },
  { value: '51-200', label: '51-200 employees' },
  { value: '201-500', label: '201-500 employees' },
  { value: '501-1000', label: '501-1000 employees' },
  { value: '1000+', label: '1000+ employees' },
];

export const APPLICATION_STATUSES = [
  { value: 'applied', label: 'Applied', color: 'info' },
  { value: 'screening', label: 'Screening', color: 'warning' },
  { value: 'interview', label: 'Interview', color: 'secondary' },
  { value: 'technical', label: 'Technical', color: 'primary' },
  { value: 'offer', label: 'Offer', color: 'accent' },
  { value: 'rejected', label: 'Rejected', color: 'danger' },
  { value: 'withdrawn', label: 'Withdrawn', color: 'muted' },
];

export const POPULAR_SKILLS = [
  'React', 'TypeScript', 'Python', 'Node.js', 'AWS', 'Docker', 'Kubernetes',
  'Machine Learning', 'SQL', 'GraphQL', 'Next.js', 'Go', 'Rust', 'Flutter',
  'Swift', 'Kotlin', 'Java', 'C++', 'TensorFlow', 'PyTorch',
];

export const CURRENCIES = [
  { value: 'INR', symbol: '₹', label: 'Indian Rupee' },
  { value: 'USD', symbol: '$', label: 'US Dollar' },
  { value: 'EUR', symbol: '€', label: 'Euro' },
  { value: 'GBP', symbol: '£', label: 'British Pound' },
  { value: 'AED', symbol: 'د.إ', label: 'UAE Dirham' },
  { value: 'CAD', symbol: 'C$', label: 'Canadian Dollar' },
  { value: 'AUD', symbol: 'A$', label: 'Australian Dollar' },
  { value: 'SGD', symbol: 'S$', label: 'Singapore Dollar' },
];

// ─────────────────────────────────────────
// Animation Variants (Framer Motion)
// ─────────────────────────────────────────

export const FADE_IN = {
  initial: { opacity: 0 },
  animate: { opacity: 1 },
  exit: { opacity: 0 },
};

export const FADE_UP = {
  initial: { opacity: 0, y: 20 },
  animate: { opacity: 1, y: 0 },
  exit: { opacity: 0, y: -20 },
};

export const SLIDE_IN_RIGHT = {
  initial: { opacity: 0, x: 40 },
  animate: { opacity: 1, x: 0 },
  exit: { opacity: 0, x: 40 },
};

export const SCALE_IN = {
  initial: { opacity: 0, scale: 0.95 },
  animate: { opacity: 1, scale: 1 },
  exit: { opacity: 0, scale: 0.95 },
};

export const STAGGER_CONTAINER = {
  animate: {
    transition: {
      staggerChildren: 0.08,
    },
  },
};

export const TRANSITION_SPRING = {
  type: 'spring',
  stiffness: 300,
  damping: 30,
};

export const TRANSITION_SMOOTH = {
  duration: 0.3,
  ease: [0.4, 0, 0.2, 1],
};
