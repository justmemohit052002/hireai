import React, { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { Sparkles, Building2, Calendar, Loader2, CheckCircle2, AlertCircle, ArrowLeft, Users, Info, Plus, X, RefreshCw } from 'lucide-react';
import { Card } from '@/components/ui/Card';
import { Input } from '@/components/ui/Input';
import { Textarea } from '@/components/ui/Textarea';
import { Select } from '@/components/ui/Select';
import { CurrencySelect, detectUserCurrency } from '@/components/ui/CurrencySelect';
import { Button } from '@/components/ui/Button';
import { Badge } from '@/components/ui/Badge';
import { JOB_TYPES, EXPERIENCE_LEVELS, ROUTES } from '@/constants';
import { formatSalary, getExperienceLevelLabel } from '@/utils';
import { generateRoleRequirements } from '@/utils/aiJdGenerator';
import { useJobs } from '@/context/JobsContext';
import { jobsApi } from '@/services/api/jobs.api';

const WORKPLACE_TYPES = [
  { value: 'hybrid', label: 'Hybrid' },
  { value: 'on_site', label: 'On-Site' },
  { value: 'remote', label: 'Remote' },
];

export const CreateJobPage = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const { jobs, createOrUpdateJob } = useJobs();

  const detectedCurrency = detectUserCurrency();
  const defaultFutureDeadline = new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString().split('T')[0];

  // All fields clean & empty on initial creation
  const [title, setTitle] = useState('');
  const [department, setDepartment] = useState('');
  const [jobType, setJobType] = useState('full-time');
  const [level, setLevel] = useState('senior');
  const [location, setLocation] = useState('');
  const [salaryMin, setSalaryMin] = useState('');
  const [salaryMax, setSalaryMax] = useState('');
  const [currency, setCurrency] = useState(detectedCurrency);
  const [workplaceType, setWorkplaceType] = useState('hybrid');
  const [skills, setSkills] = useState('');
  const [preferredSkills, setPreferredSkills] = useState('');
  const [education, setEducation] = useState('');
  const [benefits, setBenefits] = useState('');
  const [openings, setOpenings] = useState(1);
  const [deadline, setDeadline] = useState(defaultFutureDeadline);
  const [description, setDescription] = useState('');

  const [roleFamilyMeta, setRoleFamilyMeta] = useState('');
  const [suggestedExpYears, setSuggestedExpYears] = useState(null);
  const [pendingLevelChange, setPendingLevelChange] = useState(null);
  const [showLevelChangeModal, setShowLevelChangeModal] = useState(false);
  const [newRequiredInput, setNewRequiredInput] = useState('');
  const [newPreferredInput, setNewPreferredInput] = useState('');

  const [isAiGenerating, setIsAiGenerating] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');
  const [successMsg, setSuccessMsg] = useState('');
  const [originalJob, setOriginalJob] = useState(null);

  // Load existing job details if editing
  useEffect(() => {
    if (id) {
      const jobToEdit = jobs.find((j) => j.id === id);
      if (jobToEdit) {
        setOriginalJob(jobToEdit);
        setTitle(jobToEdit.title || '');
        setDepartment(jobToEdit.department || 'Engineering');
        setJobType(jobToEdit.type || jobToEdit.jobType || 'full-time');
        setLevel(jobToEdit.level || jobToEdit.experienceLevel || 'senior');
        setLocation(jobToEdit.location || '');
        setSalaryMin(jobToEdit.salary?.min ?? jobToEdit.salaryMin ?? '');
        setSalaryMax(jobToEdit.salary?.max ?? jobToEdit.salaryMax ?? '');
        setCurrency(jobToEdit.salary?.currency || jobToEdit.currency || 'INR');
        setWorkplaceType(jobToEdit.workplaceType || 'hybrid');
        setSkills(Array.isArray(jobToEdit.skills) ? jobToEdit.skills.join(', ') : jobToEdit.skills || '');
        setPreferredSkills(Array.isArray(jobToEdit.preferredSkills) ? jobToEdit.preferredSkills.join(', ') : jobToEdit.preferredSkills || '');
        setEducation(jobToEdit.education || (Array.isArray(jobToEdit.educationRequirements) ? jobToEdit.educationRequirements.join(', ') : ''));
        setDescription(jobToEdit.description || '');
        setOpenings(jobToEdit.openings || 1);
        setDeadline(jobToEdit.applicationDeadline || defaultFutureDeadline);
      }
    }
  }, [id, jobs]);

  const splitSkillsSafely = (s) => (s ? s.split(/,\s*(?![^()]*\))/).map((x) => x.trim()).filter(Boolean) : []);
  const requiredChips = splitSkillsSafely(skills);
  const preferredChips = splitSkillsSafely(preferredSkills);

  const removeRequiredChip = (indexToRemove) => {
    const updated = requiredChips.filter((_, idx) => idx !== indexToRemove);
    setSkills(updated.join(', '));
  };

  const removePreferredChip = (indexToRemove) => {
    const updated = preferredChips.filter((_, idx) => idx !== indexToRemove);
    setPreferredSkills(updated.join(', '));
  };

  const addRequiredChip = () => {
    const trimmed = newRequiredInput.trim();
    if (!trimmed) return;
    const exists = requiredChips.some((s) => s.toLowerCase() === trimmed.toLowerCase());
    if (!exists) {
      const updated = [...requiredChips, trimmed];
      setSkills(updated.join(', '));
    }
    setNewRequiredInput('');
  };

  const addPreferredChip = () => {
    const trimmed = newPreferredInput.trim();
    if (!trimmed) return;
    const exists = preferredChips.some((s) => s.toLowerCase() === trimmed.toLowerCase());
    if (!exists) {
      const updated = [...preferredChips, trimmed];
      setPreferredSkills(updated.join(', '));
    }
    setNewPreferredInput('');
  };

  const handleLevelChange = (newLevel) => {
    if (newLevel === level) return;
    const hasManualSkills = requiredChips.length > 0 || preferredChips.length > 0;
    if (hasManualSkills) {
      setPendingLevelChange(newLevel);
      setShowLevelChangeModal(true);
    } else {
      setLevel(newLevel);
    }
  };

  const confirmLevelRegenerate = async () => {
    const newLvl = pendingLevelChange;
    setLevel(newLvl);
    setShowLevelChangeModal(false);
    setPendingLevelChange(null);
    await triggerAiGeneration(title, newLvl, true);
  };

  const keepCurrentSkillsOnLevelChange = () => {
    setLevel(pendingLevelChange);
    setShowLevelChangeModal(false);
    setPendingLevelChange(null);
  };

  const triggerAiGeneration = async (targetTitle, targetLevel, forceOverwrite = false) => {
    if (!targetTitle.trim()) {
      setErrorMsg('Please enter a Job Title before generating with AI.');
      return;
    }

    setErrorMsg('');
    setIsAiGenerating(true);
    let backendAiData = null;

    try {
      backendAiData = await jobsApi.generateAiJd({
        jobTitle: targetTitle,
        requiredSkills: forceOverwrite ? [] : skills,
        experienceLevel: targetLevel,
        description: description || '',
        domain: department || '',
      });
    } catch (err) {
      console.warn('Backend AI generation note:', err.message);
    }

    try {
      const generated = generateRoleRequirements({
        title: targetTitle,
        department,
        jobType,
        workplaceType,
        level: targetLevel,
        location,
        currency,
        salaryMin,
        salaryMax,
        deadline,
        existingSkills: forceOverwrite ? '' : skills,
        existingPreferredSkills: forceOverwrite ? '' : preferredSkills,
        backendData: backendAiData,
      });

      if (generated) {
        if (generated.skills) setSkills(generated.skills);
        if (generated.preferredSkills) setPreferredSkills(generated.preferredSkills);
        if (generated.education) setEducation(generated.education);
        if (generated.benefits) setBenefits(generated.benefits);
        if (generated.description) setDescription(generated.description);
        if (generated.roleFamilyName) setRoleFamilyMeta(generated.roleFamilyName);
        if (generated.experienceYears) setSuggestedExpYears(generated.experienceYears);

        const lvlName = getExperienceLevelLabel(targetLevel);
        setSuccessMsg(`Generated realistic ${lvlName} skills and requirements grounded in market expectations.`);
        setTimeout(() => setSuccessMsg(''), 4500);
      }
    } catch (genErr) {
      setErrorMsg('Failed to generate job description. Please try again.');
    } finally {
      setIsAiGenerating(false);
    }
  };

  const handleGenerateWithAi = () => {
    const hasManualSkills = requiredChips.length > 0 || preferredChips.length > 0;
    triggerAiGeneration(title, level, false);
  };

  const handleRegenerateSkillsOnly = () => {
    triggerAiGeneration(title, level, true);
  };

  const handlePublish = async (e) => {
    e.preventDefault();
    setErrorMsg('');

    if (!title.trim()) {
      setErrorMsg('Job title is required.');
      return;
    }

    let skillArray = splitSkillsSafely(skills);
    let preferredSkillArray = splitSkillsSafely(preferredSkills);

    if (skillArray.length === 0) {
      // Auto-generate skills as per job role data entered prior if user left it blank
      const generated = generateRoleRequirements({
        title,
        department,
        jobType,
        workplaceType,
        level,
        location,
        currency,
        salaryMin,
        salaryMax,
        deadline,
        existingSkills: '',
        existingPreferredSkills: '',
      });

      if (generated?.skills) {
        skillArray = splitSkillsSafely(generated.skills);
        setSkills(generated.skills);
      }
      if (generated?.preferredSkills) {
        preferredSkillArray = splitSkillsSafely(generated.preferredSkills);
        setPreferredSkills(generated.preferredSkills);
      }

      if (skillArray.length === 0) {
        skillArray = [title.trim(), 'Problem Solving', 'Team Collaboration'];
      }
    }

    let finalDescription = description.trim();
    if (!finalDescription) {
      const generated = generateRoleRequirements({
        title,
        department,
        jobType,
        workplaceType,
        level,
        location,
        currency,
        salaryMin,
        salaryMax,
        deadline,
        existingSkills: skills,
        existingPreferredSkills: preferredSkills,
      });
      if (generated?.description) {
        finalDescription = generated.description;
        setDescription(generated.description);
      }
    }

    if (!finalDescription) {
      setErrorMsg('Job description is required.');
      return;
    }

    setIsSubmitting(true);

    const benefitArray = benefits.split(',').map((b) => b.trim()).filter(Boolean);
    const minSal = salaryMin !== '' && !isNaN(Number(salaryMin)) ? Number(salaryMin) : null;
    const maxSal = salaryMax !== '' && !isNaN(Number(salaryMax)) ? Number(salaryMax) : null;

    const jobPayload = {
      ...(originalJob || {}),
      id: id || originalJob?.id,
      title: title.trim(),
      department: department?.trim() || 'Engineering',
      location: location?.trim() || 'Remote',
      jobType,
      workplaceType,
      experienceLevel: level,
      salaryMin: minSal,
      salaryMax: maxSal,
      minSalary: minSal,
      maxSalary: maxSal,
      currency,
      skills: skillArray,
      preferredSkills: preferredSkillArray,
      education: education?.trim() || '',
      educationRequirements: education?.trim() ? [education.trim()] : [],
      benefits: benefitArray,
      openings: Number(openings) > 0 ? Number(openings) : 1,
      applicationDeadline: deadline || null,
      description: finalDescription,
    };

    try {
      await createOrUpdateJob(jobPayload);
      navigate(ROUTES.RECRUITER_JOBS);
    } catch (err) {
      console.error('Job publication error:', err);
      const msg = err.data?.message || err.message || 'Failed to publish job. Please check all fields.';
      setErrorMsg(msg);
    } finally {
      setIsSubmitting(false);
    }
  };

  const levelLabel = getExperienceLevelLabel(level) || 'Mid Level';
  const roleDisplay = roleFamilyMeta || `${title.trim() || 'Software'} Role`;

  return (
    <div className="max-w-4xl mx-auto space-y-6 pb-12">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <Button variant="ghost" size="sm" onClick={() => navigate(ROUTES.RECRUITER_JOBS)} className="rounded-lg">
            <ArrowLeft className="w-4 h-4 mr-1" /> Back
          </Button>
          <div>
            <h2 className="text-2xl font-bold font-heading text-foreground">
              {id ? 'Edit Job Posting' : 'Create New Job Opening'}
            </h2>
            <p className="text-xs text-muted-foreground">
              Configure parameters, salary range, and level-specific skill matrix.
            </p>
          </div>
        </div>
      </div>

      {errorMsg && (
        <div className="p-3.5 rounded-xl bg-red-500/10 border border-red-500/20 text-red-500 text-xs font-medium flex items-start gap-2.5">
          <AlertCircle className="w-4 h-4 shrink-0 mt-0.5" />
          <span>{errorMsg}</span>
        </div>
      )}

      {successMsg && (
        <div className="p-3.5 rounded-xl bg-emerald-500/10 border border-emerald-500/20 text-emerald-500 text-xs font-medium flex items-start gap-2.5">
          <CheckCircle2 className="w-4 h-4 shrink-0 mt-0.5" />
          <span>{successMsg}</span>
        </div>
      )}

      {/* Level Change Confirmation Dialog */}
      {showLevelChangeModal && (
        <div className="p-4 rounded-xl bg-amber-500/10 border border-amber-500/20 text-foreground text-xs space-y-3">
          <div className="flex items-start gap-2.5">
            <AlertCircle className="w-4 h-4 text-amber-500 shrink-0 mt-0.5" />
            <div>
              <p className="font-semibold text-sm text-foreground">
                Update Skills for {getExperienceLevelLabel(pendingLevelChange)}?
              </p>
              <p className="text-xs text-muted-foreground mt-0.5">
                You changed the experience level. Would you like AI to regenerate realistic required and preferred skills calibrated for {getExperienceLevelLabel(pendingLevelChange)}?
              </p>
            </div>
          </div>
          <div className="flex items-center gap-2 pt-1 pl-6">
            <Button
              type="button"
              size="sm"
              variant="outline"
              onClick={confirmLevelRegenerate}
              className="border-amber-500/30 hover:bg-amber-500/10 text-foreground font-semibold rounded-lg"
            >
              Regenerate for {getExperienceLevelLabel(pendingLevelChange)}
            </Button>
            <Button
              type="button"
              size="sm"
              variant="ghost"
              onClick={keepCurrentSkillsOnLevelChange}
              className="text-muted-foreground hover:text-foreground rounded-lg"
            >
              Keep Current Skills
            </Button>
          </div>
        </div>
      )}

      <form onSubmit={handlePublish} className="space-y-6">
        <Card glass className="p-6 space-y-5 rounded-xl">
          <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-2.5 pb-3 border-b border-border/50">
            <h3 className="text-base font-bold text-foreground">Job Details</h3>
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={handleGenerateWithAi}
              disabled={isAiGenerating}
              className="border-brand-blue/30 hover:bg-brand-blue-light/10 text-foreground w-full sm:w-auto rounded-lg"
            >
              {isAiGenerating ? (
                <Loader2 className="w-3.5 h-3.5 mr-1.5 animate-spin text-brand-blue" />
              ) : (
                <Sparkles className="w-3.5 h-3.5 mr-1.5 text-brand-blue" />
              )}
              {isAiGenerating ? 'Generating with AI...' : 'Auto-Generate with AI Engine'}
            </Button>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-bold uppercase tracking-wider text-muted-foreground mb-1.5">
                Job Title *
              </label>
              <Input
                required
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                placeholder="e.g. Senior Software Engineer / Lead AI Specialist"
              />
            </div>

            <div>
              <label className="block text-xs font-bold uppercase tracking-wider text-muted-foreground mb-1.5">
                Department / Team
              </label>
              <Input
                value={department}
                onChange={(e) => setDepartment(e.target.value)}
                placeholder="e.g. Engineering, Product, AI Research"
              />
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div>
              <label className="block text-xs font-bold uppercase tracking-wider text-muted-foreground mb-1.5">
                Employment Type *
              </label>
              <Select
                value={jobType}
                onChange={(val) => setJobType(val)}
                options={JOB_TYPES.map((t) => ({ value: t.value, label: t.label }))}
              />
            </div>

            <div>
              <label className="block text-xs font-bold uppercase tracking-wider text-muted-foreground mb-1.5">
                Workplace Model *
              </label>
              <Select
                value={workplaceType}
                onChange={(val) => setWorkplaceType(val)}
                options={WORKPLACE_TYPES}
              />
            </div>

            <div>
              <label className="block text-xs font-bold uppercase tracking-wider text-muted-foreground mb-1.5">
                Experience Level *
              </label>
              <Select
                value={level}
                onChange={handleLevelChange}
                options={EXPERIENCE_LEVELS.map((l) => ({ value: l.value, label: l.label }))}
              />
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-bold uppercase tracking-wider text-muted-foreground mb-1.5">
                Office / Regional Location
              </label>
              <Input
                value={location}
                onChange={(e) => setLocation(e.target.value)}
                placeholder="e.g. Pune, India / Bengaluru, India / Remote"
              />
            </div>

            <div>
              <label className="block text-xs font-bold uppercase tracking-wider text-muted-foreground mb-1.5">
                Number of Openings *
              </label>
              <Input
                type="number"
                min="1"
                required
                value={openings}
                onChange={(e) => setOpenings(Math.max(1, parseInt(e.target.value) || 1))}
                placeholder="e.g. 1"
              />
            </div>
          </div>
        </Card>

        {/* Compensation & Timeline */}
        <Card glass className="p-6 space-y-5 rounded-xl">
          <h3 className="text-base font-bold text-foreground pb-3 border-b border-border/50">
            Compensation & Timeline
          </h3>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div>
              <label className="block text-xs font-bold uppercase tracking-wider text-muted-foreground mb-1.5">
                Currency
              </label>
              <CurrencySelect
                value={currency}
                onChange={(val) => setCurrency(val)}
              />
            </div>

            <div>
              <label className="block text-xs font-bold uppercase tracking-wider text-muted-foreground mb-1.5">
                Minimum Annual Salary
              </label>
              <Input
                type="number"
                value={salaryMin}
                onChange={(e) => setSalaryMin(e.target.value)}
                placeholder="e.g. 1000000"
              />
            </div>

            <div>
              <label className="block text-xs font-bold uppercase tracking-wider text-muted-foreground mb-1.5">
                Maximum Annual Salary
              </label>
              <Input
                type="number"
                value={salaryMax}
                onChange={(e) => setSalaryMax(e.target.value)}
                placeholder="e.g. 1800000"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-bold uppercase tracking-wider text-muted-foreground mb-1.5">
              Application Deadline
            </label>
            <Input
              type="date"
              value={deadline}
              onChange={(e) => setDeadline(e.target.value)}
            />
          </div>
        </Card>

        {/* Level-Aware Requirements & Description */}
        <Card glass className="p-6 space-y-5 rounded-xl">
          <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-2.5 pb-3 border-b border-border/50">
            <div>
              <h3 className="text-base font-bold text-foreground">
                Requirements & Skill Matrix
              </h3>
              <p className="text-xs text-muted-foreground">
                Calibrated against real job market baselines for {levelLabel} engineering roles.
              </p>
            </div>
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={handleRegenerateSkillsOnly}
              disabled={isAiGenerating}
              className="border-brand-blue/30 hover:bg-brand-blue-light/10 text-foreground text-xs font-semibold rounded-lg shrink-0"
            >
              <RefreshCw className={`w-3.5 h-3.5 mr-1.5 text-brand-blue ${isAiGenerating ? 'animate-spin' : ''}`} />
              Regenerate Level Skills
            </Button>
          </div>

          {/* Level Grounding Guidance Banner */}
          <div className="p-3.5 rounded-xl bg-brand-blue/5 border border-brand-blue/20 text-xs text-foreground flex flex-col sm:flex-row items-start sm:items-center justify-between gap-2">
            <div className="flex items-center gap-2">
              <Info className="w-4 h-4 text-brand-blue shrink-0" />
              <span>
                Suggested for <strong>{levelLabel}</strong> <strong>{roleDisplay}</strong>
                {suggestedExpYears ? ` (${suggestedExpYears.min}-${suggestedExpYears.max} years exp)` : ''}. You can edit before posting.
              </span>
            </div>
            <span className="text-[11px] font-medium text-brand-blue bg-brand-blue/10 px-2.5 py-0.5 rounded-md shrink-0">
              ATS 80% / 20% Weighted
            </span>
          </div>

          {/* 1. Required Skills Section */}
          <div className="space-y-2">
            <div className="flex items-center justify-between">
              <label className="block text-xs font-bold uppercase tracking-wider text-foreground">
                Required Skills * <span className="text-muted-foreground font-normal">(ATS Priority: 80% weight)</span>
              </label>
              <span className="text-[11px] text-muted-foreground">
                {requiredChips.length} required skills
              </span>
            </div>
            <p className="text-[11px] text-muted-foreground">
              Core non-negotiable competencies. Missing skills count as candidate gaps during evaluation.
            </p>

            {/* Editable chips */}
            {requiredChips.length > 0 && (
              <div className="flex flex-wrap gap-1.5 p-2.5 rounded-lg bg-surface/50 border border-border/60">
                {requiredChips.map((skill, idx) => (
                  <span
                    key={`req-${idx}`}
                    className="inline-flex items-center gap-1 px-2.5 py-1 rounded-lg bg-brand-blue/10 border border-brand-blue/20 text-foreground text-xs font-medium"
                  >
                    <span>{skill}</span>
                    <button
                      type="button"
                      onClick={() => removeRequiredChip(idx)}
                      className="text-muted-foreground hover:text-red-500 transition-colors ml-0.5"
                      title={`Remove ${skill}`}
                    >
                      <X className="w-3 h-3" />
                    </button>
                  </span>
                ))}
              </div>
            )}

            {/* Quick add + comma-separated input */}
            <div className="flex gap-2">
              <Input
                value={newRequiredInput}
                onChange={(e) => setNewRequiredInput(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === 'Enter') {
                    e.preventDefault();
                    addRequiredChip();
                  }
                }}
                placeholder="Type a skill and press Enter to add..."
                className="text-xs"
              />
              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={addRequiredChip}
                className="shrink-0 text-xs rounded-lg"
              >
                <Plus className="w-3.5 h-3.5 mr-1" /> Add
              </Button>
            </div>

            <div className="pt-1">
              <label className="block text-[11px] text-muted-foreground mb-1">
                Raw Comma-Separated Required Skills:
              </label>
              <Input
                value={skills}
                onChange={(e) => setSkills(e.target.value)}
                placeholder="e.g. Java, Spring Boot, PostgreSQL, REST APIs"
                className="text-xs font-mono"
              />
            </div>
          </div>

          {/* 2. Preferred / Nice-to-Have Skills Section */}
          <div className="space-y-2 pt-2 border-t border-border/40">
            <div className="flex items-center justify-between">
              <label className="block text-xs font-bold uppercase tracking-wider text-foreground">
                Preferred / Nice-to-Have Skills <span className="text-muted-foreground font-normal">(ATS Bonus: 20% weight)</span>
              </label>
              <span className="text-[11px] text-muted-foreground">
                {preferredChips.length} preferred skills
              </span>
            </div>
            <p className="text-[11px] text-muted-foreground">
              Complementary technical skills. Missing preferred skills do NOT count as hard gaps.
            </p>

            {/* Editable chips */}
            {preferredChips.length > 0 && (
              <div className="flex flex-wrap gap-1.5 p-2.5 rounded-lg bg-surface/50 border border-border/60">
                {preferredChips.map((skill, idx) => (
                  <span
                    key={`pref-${idx}`}
                    className="inline-flex items-center gap-1 px-2.5 py-1 rounded-lg bg-slate-500/10 border border-slate-500/20 text-foreground text-xs font-medium"
                  >
                    <span>{skill}</span>
                    <button
                      type="button"
                      onClick={() => removePreferredChip(idx)}
                      className="text-muted-foreground hover:text-red-500 transition-colors ml-0.5"
                      title={`Remove ${skill}`}
                    >
                      <X className="w-3 h-3" />
                    </button>
                  </span>
                ))}
              </div>
            )}

            {/* Quick add + comma-separated input */}
            <div className="flex gap-2">
              <Input
                value={newPreferredInput}
                onChange={(e) => setNewPreferredInput(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === 'Enter') {
                    e.preventDefault();
                    addPreferredChip();
                  }
                }}
                placeholder="Type a preferred skill and press Enter to add..."
                className="text-xs"
              />
              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={addPreferredChip}
                className="shrink-0 text-xs rounded-lg"
              >
                <Plus className="w-3.5 h-3.5 mr-1" /> Add
              </Button>
            </div>

            <div className="pt-1">
              <label className="block text-[11px] text-muted-foreground mb-1">
                Raw Comma-Separated Preferred Skills:
              </label>
              <Input
                value={preferredSkills}
                onChange={(e) => setPreferredSkills(e.target.value)}
                placeholder="e.g. Docker, Redis, Kafka, AWS, CI/CD"
                className="text-xs font-mono"
              />
            </div>
          </div>

          <div className="pt-2 border-t border-border/40">
            <label className="block text-xs font-bold uppercase tracking-wider text-muted-foreground mb-1.5">
              Education Requirements
            </label>
            <Input
              value={education}
              onChange={(e) => setEducation(e.target.value)}
              placeholder="e.g. Bachelor's in Computer Science, B.Tech, or equivalent practical experience"
            />
          </div>

          <div>
            <label className="block text-xs font-bold uppercase tracking-wider text-muted-foreground mb-1.5">
              Company Perks & Benefits (Comma-separated)
            </label>
            <Input
              value={benefits}
              onChange={(e) => setBenefits(e.target.value)}
              placeholder="e.g. Health Insurance, Annual Performance Bonus, Flexible Remote Hours, Learning Stipend"
            />
          </div>

          <div>
            <label className="block text-xs font-bold uppercase tracking-wider text-muted-foreground mb-1.5">
              Comprehensive Job Description *
            </label>
            <Textarea
              required
              rows={8}
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Detail key responsibilities, team structure, tech stack, and role requirements..."
              className="font-sans text-xs leading-relaxed"
            />
          </div>
        </Card>

        <div className="flex flex-col-reverse sm:flex-row items-stretch sm:items-center justify-end gap-3 sm:gap-4 pt-2">
          <Button type="button" variant="ghost" onClick={() => navigate(ROUTES.RECRUITER_JOBS)} className="w-full sm:w-auto rounded-lg">
            Cancel
          </Button>
          <Button type="submit" variant="gradient" size="lg" isLoading={isSubmitting} className="font-bold shadow-xl w-full sm:w-auto rounded-lg">
            {id ? 'Update Job Posting' : 'Publish Job Opening'}
          </Button>
        </div>
      </form>
    </div>
  );
};
