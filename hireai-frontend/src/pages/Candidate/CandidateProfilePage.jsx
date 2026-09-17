import React, { useState, useEffect, useMemo } from 'react';
import {
  User,
  MapPin,
  Briefcase,
  Award,
  Save,
  Sparkles,
  Phone,
  Mail,
  Globe,
  Code2,
  Link2,
  Loader2,
  CheckCircle2,
  AlertCircle,
  TrendingUp,
  ChevronDown,
  ChevronUp,
  Check,
  Plus,
  X,
  Eye,
  ExternalLink,
  DollarSign,
  Clock,
  ShieldCheck,
  FileText
} from 'lucide-react';
import { useAuth } from '@/context/AuthContext';
import { Card } from '@/components/ui/Card';
import { Input } from '@/components/ui/Input';
import { Textarea } from '@/components/ui/Textarea';
import { Button } from '@/components/ui/Button';
import { Avatar } from '@/components/ui/Avatar';
import { Badge } from '@/components/ui/Badge';
import { ResumeUpload } from '@/components/forms/ResumeUpload';
import { candidateApi } from '@/services/api/candidate.api';
import { formatSalary } from '@/utils';

const SUGGESTED_SKILLS = [
  'React',
  'TypeScript',
  'Node.js',
  'Spring Boot',
  'Java',
  'Python',
  'PostgreSQL',
  'Docker',
  'Kubernetes',
  'AWS',
  'GraphQL',
  'TailwindCSS',
  'Next.js',
  'Microservices',
];

const NOTICE_PERIOD_PRESETS = [
  { label: 'Immediate', days: 0 },
  { label: '15 Days', days: 15 },
  { label: '30 Days', days: 30 },
  { label: '60 Days', days: 60 },
  { label: '90 Days', days: 90 },
];

export const CandidateProfilePage = () => {
  const { user } = useAuth();
  const [activeTab, setActiveTab] = useState('edit'); // 'edit' | 'preview'
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [successMsg, setSuccessMsg] = useState('');
  const [errorMsg, setErrorMsg] = useState('');
  const [showBreakdown, setShowBreakdown] = useState(false);

  // Form Fields - clean defaults without hardcoded mock data
  const [designation, setDesignation] = useState('');
  const [location, setLocation] = useState('');
  const [experienceYears, setExperienceYears] = useState('');
  const [currentCtc, setCurrentCtc] = useState('');
  const [expectedCtc, setExpectedCtc] = useState('');
  const [noticePeriodDays, setNoticePeriodDays] = useState('');
  const [skillsList, setSkillsList] = useState([]);
  const [skillInput, setSkillInput] = useState('');
  const [bio, setBio] = useState('');
  const [githubUrl, setGithubUrl] = useState('');
  const [linkedinUrl, setLinkedinUrl] = useState('');
  const [resumeFile, setResumeFile] = useState(null);
  const [hasExistingResume, setHasExistingResume] = useState(false);

  useEffect(() => {
    async function loadProfile() {
      setIsLoading(true);
      try {
        const profile = await candidateApi.getMyProfile();
        if (profile) {
          if (profile.currentDesignation || profile.designation) {
            setDesignation(profile.currentDesignation || profile.designation);
          }
          if (profile.location) setLocation(profile.location);
          if (profile.experience != null || profile.experienceYears != null) {
            setExperienceYears(profile.experience ?? profile.experienceYears);
          }
          if (profile.currentCtc != null) setCurrentCtc(profile.currentCtc);
          if (profile.expectedCtc != null) setExpectedCtc(profile.expectedCtc);
          if (profile.noticePeriod != null || profile.noticePeriodDays != null) {
            setNoticePeriodDays(profile.noticePeriod ?? profile.noticePeriodDays);
          }
          if (profile.skills) {
            const arr = Array.isArray(profile.skills)
              ? profile.skills
              : profile.skills.split(',').map((s) => s.trim()).filter(Boolean);
            setSkillsList(arr);
          }
          if (profile.bio) setBio(profile.bio);
          if (profile.githubUrl) setGithubUrl(profile.githubUrl);
          if (profile.linkedinUrl) setLinkedinUrl(profile.linkedinUrl);
          if (profile.resumeId || profile.resumeUrl) {
            setHasExistingResume(true);
          }
        }
      } catch (err) {
        console.warn('Could not load remote profile:', err.message);
      } finally {
        setIsLoading(false);
      }
    }
    loadProfile();
  }, []);

  // Skill Management Helpers
  const handleAddSkill = (skillToAdd) => {
    const trimmed = (skillToAdd || skillInput).trim();
    if (trimmed && !skillsList.some((s) => s.toLowerCase() === trimmed.toLowerCase())) {
      setSkillsList([...skillsList, trimmed]);
      setSkillInput('');
    }
  };

  const handleRemoveSkill = (skillToRemove) => {
    setSkillsList(skillsList.filter((s) => s !== skillToRemove));
  };

  const handleSkillKeyDown = (e) => {
    if (e.key === 'Enter' || e.key === ',') {
      e.preventDefault();
      handleAddSkill();
    }
  };

  // Dynamic real-time calculation of AI Profile Score and recommendations
  const { score, completedItems, pendingSuggestions } = useMemo(() => {
    let totalScore = 0;
    const completed = [];
    const pending = [];

    // 1. Current Designation (10 pts)
    if (designation?.trim()) {
      totalScore += 10;
      completed.push({ label: 'Current Designation added', points: 10 });
    } else {
      pending.push({
        label: 'Add your Current Title / Designation',
        points: 10,
        tip: 'Helps AI match you with relevant seniority levels',
      });
    }

    // 2. Location (10 pts)
    if (location?.trim()) {
      totalScore += 10;
      completed.push({ label: 'Location specified', points: 10 });
    } else {
      pending.push({
        label: 'Add your Location / City',
        points: 10,
        tip: 'Used for remote and local matching filters',
      });
    }

    // 3. Experience Years (10 pts)
    if (experienceYears !== '' && Number(experienceYears) >= 0) {
      totalScore += 10;
      completed.push({ label: `${experienceYears} yrs experience listed`, points: 10 });
    } else {
      pending.push({
        label: 'Specify your Years of Experience',
        points: 10,
        tip: 'Required for seniority indexing',
      });
    }

    // 4. Compensation & Notice Period (10 pts)
    const hasCtc = expectedCtc !== '' && Number(expectedCtc) > 0;
    const hasNotice = noticePeriodDays !== '' && Number(noticePeriodDays) >= 0;
    if (hasCtc && hasNotice) {
      totalScore += 10;
      completed.push({ label: 'Expected CTC & Notice period defined', points: 10 });
    } else {
      pending.push({
        label: 'Provide Expected CTC and Notice Period',
        points: 10,
        tip: 'Enables quick recruiter outreach & budget alignment',
      });
    }

    // 5. Technical Skills (20 pts max)
    if (skillsList.length >= 5) {
      totalScore += 20;
      completed.push({ label: `${skillsList.length} Technical Skills listed`, points: 20 });
    } else if (skillsList.length >= 2) {
      totalScore += 12;
      completed.push({ label: `${skillsList.length} Skills listed (partial)`, points: 12 });
      pending.push({
        label: `Add ${5 - skillsList.length} more technical skills (minimum 5 recommended)`,
        points: 8,
        tip: 'Broader skill coverage increases ATS match ranking',
      });
    } else if (skillsList.length === 1) {
      totalScore += 6;
      completed.push({ label: '1 Skill listed', points: 6 });
      pending.push({
        label: 'Add at least 5 technical skills',
        points: 14,
        tip: 'AI uses skills to compute exact match scores against job postings',
      });
    } else {
      pending.push({
        label: 'Add your primary technical skills (e.g. React, Python, AWS)',
        points: 20,
        tip: 'Primary ranking signal for recruiter searches',
      });
    }

    // 6. GitHub Profile (10 pts)
    if (githubUrl?.trim() && (githubUrl.includes('github.com') || githubUrl.startsWith('http'))) {
      totalScore += 10;
      completed.push({ label: 'GitHub profile connected', points: 10 });
    } else {
      pending.push({
        label: 'Link your GitHub profile URL',
        points: 10,
        tip: 'Shows code proof and open-source projects',
      });
    }

    // 7. LinkedIn Profile (10 pts)
    if (linkedinUrl?.trim() && (linkedinUrl.includes('linkedin.com') || linkedinUrl.startsWith('http'))) {
      totalScore += 10;
      completed.push({ label: 'LinkedIn profile connected', points: 10 });
    } else {
      pending.push({
        label: 'Link your LinkedIn profile URL',
        points: 10,
        tip: 'Allows recruiters to verify career history',
      });
    }

    // 8. Biography (10 pts)
    const bioTrimmed = bio?.trim() || '';
    if (bioTrimmed.length >= 40) {
      totalScore += 10;
      completed.push({ label: 'Detailed professional bio', points: 10 });
    } else if (bioTrimmed.length > 0) {
      totalScore += 5;
      completed.push({ label: 'Brief bio (partial)', points: 5 });
      pending.push({
        label: 'Expand professional bio (at least 40 characters)',
        points: 5,
        tip: 'Summarize key achievements, tech domains, and career goals',
      });
    } else {
      pending.push({
        label: 'Write a professional biography',
        points: 10,
        tip: 'Give hiring managers an executive summary of your background',
      });
    }

    // 9. Resume (10 pts)
    const hasResume = !!resumeFile || hasExistingResume;
    if (hasResume) {
      totalScore += 10;
      completed.push({ label: 'Resume attached & parsed', points: 10 });
    } else {
      pending.push({
        label: 'Upload your latest Resume (PDF or DOCX)',
        points: 10,
        tip: 'Enables automatic AI parsing and 1-click job applications',
      });
    }

    return {
      score: Math.min(100, totalScore),
      completedItems: completed,
      pendingSuggestions: pending,
    };
  }, [
    designation,
    location,
    experienceYears,
    expectedCtc,
    noticePeriodDays,
    skillsList,
    githubUrl,
    linkedinUrl,
    bio,
    resumeFile,
    hasExistingResume,
  ]);

  const handleSave = async (e) => {
    e.preventDefault();
    setErrorMsg('');
    setIsSaving(true);

    const expNum = experienceYears !== '' && !isNaN(Number(experienceYears)) ? Number(experienceYears) : null;
    const curCtcNum = currentCtc !== '' && !isNaN(Number(currentCtc)) ? Number(currentCtc) : null;
    const expCtcNum = expectedCtc !== '' && !isNaN(Number(expectedCtc)) ? Number(expectedCtc) : null;
    const noticeNum = noticePeriodDays !== '' && !isNaN(Number(noticePeriodDays)) ? parseInt(noticePeriodDays, 10) : null;

    const payload = {
      currentDesignation: designation?.trim() || null,
      designation: designation?.trim() || null,
      location: location?.trim() || null,
      experience: expNum,
      experienceYears: expNum,
      currentCtc: curCtcNum,
      expectedCtc: expCtcNum,
      noticePeriod: noticeNum,
      noticePeriodDays: noticeNum,
      skills: skillsList,
      bio: bio?.trim() || null,
      githubUrl: githubUrl?.trim() || null,
      linkedinUrl: linkedinUrl?.trim() || null,
    };

    try {
      await candidateApi.updateProfile(payload);
      setSuccessMsg('Profile updated successfully!');
      setTimeout(() => setSuccessMsg(''), 3500);
    } catch (err) {
      try {
        await candidateApi.createProfile(payload);
        setSuccessMsg('Profile created successfully!');
        setTimeout(() => setSuccessMsg(''), 3500);
      } catch (createErr) {
        setErrorMsg(createErr.message || 'Failed to save profile.');
      }
    } finally {
      setIsSaving(false);
    }
  };

  const displayName = `${user?.firstName || 'Alex'} ${user?.lastName || 'Rivera'}`;

  const getScoreTheme = (val) => {
    if (val >= 80) {
      return {
        badgeBg: 'bg-emerald-500/15 border-emerald-500/30 text-emerald-400',
        progressBar: 'from-emerald-500 via-teal-400 to-cyan-400',
        ratingLabel: 'All-Star Candidate',
      };
    }
    if (val >= 50) {
      return {
        badgeBg: 'bg-amber-500/15 border-amber-500/30 text-amber-400',
        progressBar: 'from-amber-500 via-orange-400 to-yellow-400',
        ratingLabel: 'Good Profile',
      };
    }
    return {
      badgeBg: 'bg-rose-500/15 border-rose-500/30 text-rose-400',
      progressBar: 'from-rose-500 via-pink-400 to-red-400',
      ratingLabel: 'Needs Completion',
    };
  };

  const scoreTheme = getScoreTheme(score);

  // Quick INR / LPA Formatter helper for inputs
  const formatLpa = (amount) => {
    const num = Number(amount);
    if (!num || isNaN(num)) return '';
    if (num >= 10000000) return `₹${(num / 10000000).toFixed(2)} Cr`;
    if (num >= 100000) return `₹${(num / 100000).toFixed(1)} LPA`;
    return `₹${num.toLocaleString('en-IN')}`;
  };

  return (
    <div className="max-w-5xl mx-auto space-y-6 pb-16">
      {/* Toast Alerts */}
      {errorMsg && (
        <div className="p-4 rounded-2xl bg-red-500/10 border border-red-500/20 text-red-500 text-xs font-medium flex items-start gap-2.5 shadow-sm">
          <AlertCircle className="w-4 h-4 shrink-0 mt-0.5" />
          <span>{errorMsg}</span>
        </div>
      )}

      {successMsg && (
        <div className="p-4 rounded-2xl bg-emerald-500/10 border border-emerald-500/20 text-emerald-500 text-xs font-medium flex items-start gap-2.5 shadow-sm">
          <CheckCircle2 className="w-4 h-4 shrink-0 mt-0.5" />
          <span>{successMsg}</span>
        </div>
      )}

      {/* Hero Profile Header Banner */}
      <div className="relative overflow-hidden rounded-3xl glass border border-border/70 p-6 md:p-8 shadow-md">
        <div className="absolute top-0 inset-x-0 h-1.5 bg-brand-blue" />
        
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-6">
          <div className="flex items-center gap-5">
            <div className="relative">
              <Avatar name={displayName} size="xl" />
              <span className="absolute -bottom-1 -right-1 w-5 h-5 rounded-full bg-emerald-500 border-2 border-background flex items-center justify-center text-[10px] text-foreground font-bold" title="Verified Candidate">
                ✓
              </span>
            </div>

            <div className="space-y-1">
              <div className="flex items-center gap-2.5 flex-wrap">
                <h1 className="text-2xl sm:text-3xl font-bold font-heading text-foreground">{displayName}</h1>
                <span className="text-[11px] font-semibold px-2.5 py-0.5 rounded-full bg-primary/10 border border-primary/20 text-primary">
                  {designation || 'Candidate'}
                </span>
              </div>
              <p className="text-xs sm:text-sm text-muted-foreground flex items-center gap-2">
                <Mail className="w-3.5 h-3.5" />
                <span>{user?.email || 'alex@example.com'}</span>
                {location && (
                  <>
                    <span>•</span>
                    <span className="flex items-center gap-1">
                      <MapPin className="w-3.5 h-3.5 text-primary" /> {location}
                    </span>
                  </>
                )}
              </p>
            </div>
          </div>

          {/* AI Score Badge & View Mode Toggle */}
          <div className="flex flex-wrap items-center gap-3 self-start md:self-center">
            <button
              type="button"
              onClick={() => setShowBreakdown((prev) => !prev)}
              className={`inline-flex items-center gap-2 px-3.5 py-2 rounded-2xl border text-xs font-bold font-mono transition-all duration-300 shadow-xs cursor-pointer ${scoreTheme.badgeBg}`}
            >
              <Sparkles className="w-4 h-4" /> AI Profile Score: {score}/100
              <ChevronDown className={`w-3.5 h-3.5 transition-transform ${showBreakdown ? 'rotate-180' : ''}`} />
            </button>

            <div className="flex rounded-xl bg-surface-2 p-1 border border-border/70">
              <button
                type="button"
                onClick={() => setActiveTab('edit')}
                className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-all ${
                  activeTab === 'edit'
                    ? 'bg-background text-foreground shadow-xs'
                    : 'text-muted-foreground hover:text-foreground'
                }`}
              >
                Edit Profile
              </button>
              <button
                type="button"
                onClick={() => setActiveTab('preview')}
                className={`px-3 py-1.5 rounded-lg text-xs font-semibold flex items-center gap-1 transition-all ${
                  activeTab === 'preview'
                    ? 'bg-background text-foreground shadow-xs'
                    : 'text-muted-foreground hover:text-foreground'
                }`}
              >
                <Eye className="w-3.5 h-3.5" /> Recruiter View
              </button>
            </div>
          </div>
        </div>

        {/* Collapsible AI Score Breakdown Drawer */}
        {showBreakdown && (
          <div className="mt-6 pt-6 border-t border-border/60 space-y-4">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
              <div className="flex items-center gap-2">
                <TrendingUp className="w-4 h-4 text-primary" />
                <span className="text-xs font-bold uppercase tracking-wider text-foreground">AI Optimization Breakdown</span>
                <span className={`text-[10px] font-mono font-bold px-2 py-0.5 rounded-md ${scoreTheme.badgeBg}`}>
                  {scoreTheme.ratingLabel}
                </span>
              </div>
              <span className="text-xs text-muted-foreground font-mono">{score} / 100 Points</span>
            </div>

            <div className="w-full bg-background/80 h-2 rounded-full overflow-hidden border border-border/50">
              <div
                className={`h-full bg-gradient-to-r ${scoreTheme.progressBar} transition-all duration-500 rounded-full`}
                style={{ width: `${Math.max(5, score)}%` }}
              />
            </div>

            {pendingSuggestions.length > 0 ? (
              <div className="grid grid-cols-1 md:grid-cols-2 gap-2 pt-1">
                {pendingSuggestions.map((item, idx) => (
                  <div
                    key={idx}
                    className="p-3 rounded-xl bg-background/60 border border-border/60 flex items-start justify-between gap-3 text-xs"
                  >
                    <div>
                      <p className="font-semibold text-foreground">{item.label}</p>
                      <p className="text-[11px] text-muted-foreground">{item.tip}</p>
                    </div>
                    <span className="shrink-0 font-mono font-bold px-2 py-0.5 rounded-md bg-primary/10 text-primary border border-primary/20 text-[11px]">
                      +{item.points} pts
                    </span>
                  </div>
                ))}
              </div>
            ) : (
              <div className="p-3 rounded-xl bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 text-xs font-medium flex items-center gap-2">
                <CheckCircle2 className="w-4 h-4 shrink-0" />
                <span>🎉 All-Star Profile! 100% complete and fully optimized for top recruiter discovery & ATS rankings.</span>
              </div>
            )}
          </div>
        )}
      </div>

      {/* Mode Switch Content */}
      {activeTab === 'preview' ? (
        /* =================== PUBLIC RECRUITER VIEW PREVIEW =================== */
        <div className="space-y-6">
          <div className="p-3.5 rounded-2xl bg-blue-500/10 border border-blue-500/20 text-xs text-blue-400 flex items-center justify-between">
            <span className="flex items-center gap-2">
              <Eye className="w-4 h-4" /> This is an interactive live preview of how hiring managers view your profile card in talent search.
            </span>
            <Button size="sm" variant="outline" onClick={() => setActiveTab('edit')}>
              Return to Edit
            </Button>
          </div>

          <Card glass className="p-8 space-y-6">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-6 border-b border-border/50">
              <div className="flex items-center gap-4">
                <Avatar name={displayName} size="xl" />
                <div>
                  <h2 className="text-xl sm:text-2xl font-bold font-heading text-foreground">{displayName}</h2>
                  <p className="text-sm font-semibold text-primary">{designation || 'Software Engineer'}</p>
                  <p className="text-xs text-muted-foreground flex items-center gap-2 mt-1">
                    <MapPin className="w-3.5 h-3.5" /> {location || 'Remote'} • {experienceYears} Years Experience
                  </p>
                </div>
              </div>

              <div className="flex items-center gap-2">
                {githubUrl && (
                  <a
                    href={githubUrl}
                    target="_blank"
                    rel="noreferrer"
                    className="p-2.5 rounded-xl bg-surface-2 hover:bg-surface-3 border border-border text-foreground transition-colors"
                    title="GitHub Profile"
                  >
                    <Code2 className="w-4 h-4" />
                  </a>
                )}
                {linkedinUrl && (
                  <a
                    href={linkedinUrl}
                    target="_blank"
                    rel="noreferrer"
                    className="p-2.5 rounded-xl bg-surface-2 hover:bg-surface-3 border border-border text-foreground transition-colors"
                    title="LinkedIn Profile"
                  >
                    <Link2 className="w-4 h-4" />
                  </a>
                )}
              </div>
            </div>

            {/* Quick Metrics Strip */}
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 p-4 rounded-2xl bg-surface-2 border border-border/60 text-xs">
              <div>
                <span className="text-muted-foreground block text-[11px] uppercase font-bold tracking-wider">Experience</span>
                <span className="font-bold text-foreground text-sm">{experienceYears} Years</span>
              </div>
              <div>
                <span className="text-muted-foreground block text-[11px] uppercase font-bold tracking-wider">Expected CTC</span>
                <span className="font-bold text-foreground text-sm">{formatLpa(expectedCtc) || 'Disclosed'}</span>
              </div>
              <div>
                <span className="text-muted-foreground block text-[11px] uppercase font-bold tracking-wider">Notice Period</span>
                <span className="font-bold text-foreground text-sm">{noticePeriodDays === 0 ? 'Immediate' : `${noticePeriodDays} Days`}</span>
              </div>
              <div>
                <span className="text-muted-foreground block text-[11px] uppercase font-bold tracking-wider">AI Readiness</span>
                <span className="font-bold text-emerald-400 text-sm">{score}/100</span>
              </div>
            </div>

            {/* Bio */}
            <div className="space-y-2">
              <h3 className="text-xs font-bold uppercase tracking-wider text-muted-foreground">About Candidate</h3>
              <p className="text-sm text-foreground leading-relaxed whitespace-pre-line">{bio || 'No bio provided yet.'}</p>
            </div>

            {/* Verified Skills */}
            <div className="space-y-2.5">
              <h3 className="text-xs font-bold uppercase tracking-wider text-muted-foreground">Technical Capabilities</h3>
              <div className="flex flex-wrap gap-2">
                {skillsList.map((skill, idx) => (
                  <span
                    key={idx}
                    className="px-3 py-1 rounded-xl bg-surface-2 border border-border/70 text-xs font-semibold text-foreground font-mono"
                  >
                    {skill}
                  </span>
                ))}
              </div>
            </div>
          </Card>
        </div>
      ) : (
        /* =================== EDIT PROFILE FORM =================== */
        <Card glass className="p-6 sm:p-8 space-y-6">
          <form onSubmit={handleSave} className="space-y-6">
            {/* Row 1: Title & Location */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label className="block text-xs font-bold uppercase tracking-wider text-muted-foreground mb-1.5">
                  Current Title / Designation <span className="text-primary font-mono text-[10px]">(+10 pts)</span>
                </label>
                <Input
                  value={designation}
                  onChange={(e) => setDesignation(e.target.value)}
                  icon={<Briefcase className="w-4 h-4" />}
                  placeholder="e.g. Senior Frontend Engineer"
                />
              </div>

              <div>
                <label className="block text-xs font-bold uppercase tracking-wider text-muted-foreground mb-1.5">
                  Location / City <span className="text-primary font-mono text-[10px]">(+10 pts)</span>
                </label>
                <Input
                  value={location}
                  onChange={(e) => setLocation(e.target.value)}
                  icon={<MapPin className="w-4 h-4" />}
                  placeholder="e.g. Bengaluru, India"
                />
              </div>
            </div>

            {/* Row 2: Experience, CTC & Notice Period */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              <div>
                <label className="block text-xs font-bold uppercase tracking-wider text-muted-foreground mb-1.5">
                  Years of Experience <span className="text-primary font-mono text-[10px]">(+10 pts)</span>
                </label>
                <Input
                  type="number"
                  value={experienceYears}
                  onChange={(e) => setExperienceYears(e.target.value)}
                  placeholder="4"
                  min="0"
                />
              </div>

              <div>
                <div className="flex items-center justify-between mb-1.5">
                  <label className="block text-xs font-bold uppercase tracking-wider text-muted-foreground">
                    Expected CTC (INR) <span className="text-primary font-mono text-[10px]">(+5 pts)</span>
                  </label>
                  {expectedCtc > 0 && (
                    <span className="text-[11px] font-mono font-bold text-primary">
                      {formatLpa(expectedCtc)}
                    </span>
                  )}
                </div>
                <Input
                  type="number"
                  value={expectedCtc}
                  onChange={(e) => setExpectedCtc(e.target.value)}
                  placeholder="2500000"
                />
              </div>

              <div>
                <label className="block text-xs font-bold uppercase tracking-wider text-muted-foreground mb-1.5">
                  Notice Period (Days) <span className="text-primary font-mono text-[10px]">(+5 pts)</span>
                </label>
                <Input
                  type="number"
                  value={noticePeriodDays}
                  onChange={(e) => setNoticePeriodDays(Number(e.target.value))}
                  placeholder="30"
                  min="0"
                />
              </div>
            </div>

            {/* Quick Notice Period Presets */}
            <div className="flex items-center gap-2 flex-wrap pt-1">
              <span className="text-xs text-muted-foreground font-semibold">Quick select notice:</span>
              {NOTICE_PERIOD_PRESETS.map((preset) => (
                <button
                  key={preset.label}
                  type="button"
                  onClick={() => setNoticePeriodDays(preset.days)}
                  className={`text-xs px-2.5 py-1 rounded-lg border transition-all ${
                    noticePeriodDays === preset.days
                      ? 'bg-primary/15 border-primary text-primary font-bold'
                      : 'bg-surface-2 border-border/70 text-muted-foreground hover:text-foreground'
                  }`}
                >
                  {preset.label}
                </button>
              ))}
            </div>

            {/* Interactive Skill Tag Manager */}
            <div className="space-y-2.5">
              <div className="flex items-center justify-between">
                <label className="block text-xs font-bold uppercase tracking-wider text-muted-foreground">
                  Technical Stack & Skills <span className="text-primary font-mono text-[10px]">(Up to +20 pts)</span>
                </label>
                <span className="text-[11px] font-mono text-muted-foreground">
                  {skillsList.length} skills added (5+ recommended)
                </span>
              </div>

              {/* Tag Input Box */}
              <div className="p-3 rounded-2xl bg-surface-2/80 border border-border/80 focus-within:border-primary/50 transition-colors space-y-2.5">
                <div className="flex flex-wrap items-center gap-2">
                  {skillsList.map((skill) => (
                    <span
                      key={skill}
                      className="inline-flex items-center gap-1.5 px-3 py-1 rounded-xl bg-background border border-border/70 text-xs font-semibold text-foreground font-mono group"
                    >
                      <span>{skill}</span>
                      <button
                        type="button"
                        onClick={() => handleRemoveSkill(skill)}
                        className="text-muted-foreground hover:text-red-400 transition-colors"
                      >
                        <X className="w-3.5 h-3.5" />
                      </button>
                    </span>
                  ))}
                  
                  <div className="flex items-center gap-2 flex-1 min-w-[160px]">
                    <input
                      type="text"
                      value={skillInput}
                      onChange={(e) => setSkillInput(e.target.value)}
                      onKeyDown={handleSkillKeyDown}
                      placeholder="Type a skill and press Enter..."
                      className="bg-transparent border-none text-xs text-foreground placeholder:text-muted-foreground focus:outline-none w-full"
                    />
                    <Button
                      type="button"
                      size="sm"
                      variant="ghost"
                      onClick={() => handleAddSkill()}
                      disabled={!skillInput.trim()}
                      className="h-7 px-2 text-xs"
                    >
                      <Plus className="w-3.5 h-3.5 mr-0.5" /> Add
                    </Button>
                  </div>
                </div>
              </div>

              {/* Quick-Add Suggestions */}
              <div className="flex flex-wrap items-center gap-1.5 pt-1">
                <span className="text-[11px] text-muted-foreground font-semibold">Suggested skills:</span>
                {SUGGESTED_SKILLS.filter((s) => !skillsList.some((ex) => ex.toLowerCase() === s.toLowerCase()))
                  .slice(0, 8)
                  .map((suggested) => (
                    <button
                      key={suggested}
                      type="button"
                      onClick={() => handleAddSkill(suggested)}
                      className="inline-flex items-center gap-1 text-[11px] px-2 py-0.5 rounded-md bg-surface-3 hover:bg-primary/15 hover:text-primary border border-border/60 text-muted-foreground transition-all cursor-pointer"
                    >
                      <Plus className="w-3 h-3" /> {suggested}
                    </button>
                  ))}
              </div>
            </div>

            {/* Row 3: GitHub & LinkedIn */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label className="block text-xs font-bold uppercase tracking-wider text-muted-foreground mb-1.5">
                  GitHub Profile URL <span className="text-primary font-mono text-[10px]">(+10 pts)</span>
                </label>
                <Input
                  value={githubUrl}
                  onChange={(e) => setGithubUrl(e.target.value)}
                  icon={<Code2 className="w-4 h-4" />}
                  placeholder="https://github.com/username"
                />
              </div>

              <div>
                <label className="block text-xs font-bold uppercase tracking-wider text-muted-foreground mb-1.5">
                  LinkedIn Profile URL <span className="text-primary font-mono text-[10px]">(+10 pts)</span>
                </label>
                <Input
                  value={linkedinUrl}
                  onChange={(e) => setLinkedinUrl(e.target.value)}
                  icon={<Link2 className="w-4 h-4" />}
                  placeholder="https://linkedin.com/in/username"
                />
              </div>
            </div>

            {/* Professional Biography */}
            <div className="space-y-1.5">
              <div className="flex items-center justify-between">
                <label className="block text-xs font-bold uppercase tracking-wider text-muted-foreground">
                  Professional Biography <span className="text-primary font-mono text-[10px]">(+10 pts)</span>
                </label>
                <span className="text-[11px] font-mono text-muted-foreground">
                  {bio?.length || 0} characters (40+ recommended)
                </span>
              </div>
              <Textarea
                value={bio}
                onChange={(e) => setBio(e.target.value)}
                rows={3}
                placeholder="Highlight your key engineering specialties, major achievements, and target roles..."
              />
            </div>

            {/* Resume & AI Skills Parsing */}
            <div className="space-y-2">
              <label className="block text-xs font-bold uppercase tracking-wider text-muted-foreground">
                Resume & AI Skills Parsing <span className="text-primary font-mono text-[10px]">(+10 pts)</span>
              </label>
              <ResumeUpload
                autoUpload
                onFileSelect={(file) => {
                  setResumeFile(file);
                  if (!file) setHasExistingResume(false);
                }}
              />
            </div>

            {/* Submit Button */}
            <div className="pt-4 border-t border-border/60 flex items-center justify-between">
              <span className="text-xs text-muted-foreground">
                All changes automatically sync with AI recruiter search.
              </span>
              <Button type="submit" variant="gradient" size="lg" isLoading={isSaving} className="font-bold shadow-xl">
                <Save className="w-4 h-4 mr-2" />
                Save Profile Changes
              </Button>
            </div>
          </form>
        </Card>
      )}
    </div>
  );
};
