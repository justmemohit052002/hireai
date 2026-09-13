import React, { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { Sparkles, Eye, Building2, Calendar, Loader2, CheckCircle2, AlertCircle, ArrowLeft, Users } from 'lucide-react';
import { Card } from '@/components/ui/Card';
import { Input } from '@/components/ui/Input';
import { Textarea } from '@/components/ui/Textarea';
import { Select } from '@/components/ui/Select';
import { CurrencySelect, detectUserCurrency } from '@/components/ui/CurrencySelect';
import { Button } from '@/components/ui/Button';
import { Badge } from '@/components/ui/Badge';
import { JOB_TYPES, EXPERIENCE_LEVELS, ROUTES } from '@/constants';
import { formatSalary } from '@/utils';
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
  const [education, setEducation] = useState('');
  const [benefits, setBenefits] = useState('');
  const [openings, setOpenings] = useState(1);
  const [deadline, setDeadline] = useState(defaultFutureDeadline);
  const [description, setDescription] = useState('');

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
        setEducation(jobToEdit.education || (Array.isArray(jobToEdit.educationRequirements) ? jobToEdit.educationRequirements.join(', ') : ''));
        setDescription(jobToEdit.description || '');
        setOpenings(jobToEdit.openings || 1);
        setDeadline(jobToEdit.applicationDeadline || defaultFutureDeadline);
      }
    }
  }, [id, jobs]);

  // AI JD Generation based on all entered parameters
  const handleGenerateWithAi = async () => {
    if (!title.trim()) {
      setErrorMsg('Please enter a Job Title before generating with AI.');
      return;
    }

    setErrorMsg('');
    setIsAiGenerating(true);
    let backendAiData = null;

    try {
      backendAiData = await jobsApi.generateAiJd({
        jobTitle: title,
        requiredSkills: skills,
        experienceLevel: level,
      });
    } catch (err) {
      console.warn('Backend AI generation endpoint note:', err.message);
    }

    try {
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
        backendData: backendAiData,
      });

      if (generated) {
        if (generated.skills) setSkills(generated.skills);
        if (generated.education) setEducation(generated.education);
        if (generated.benefits) setBenefits(generated.benefits);
        if (generated.description) setDescription(generated.description);

        setSuccessMsg('AI generated job requirements, skills, perks, and description tailored to your job details & compensation!');
        setTimeout(() => setSuccessMsg(''), 4000);
      }
    } catch (genErr) {
      setErrorMsg('Failed to generate job description. Please try again.');
    } finally {
      setIsAiGenerating(false);
    }
  };

  const handlePublish = async (e) => {
    e.preventDefault();
    setErrorMsg('');

    if (!title.trim()) {
      setErrorMsg('Job title is required.');
      return;
    }

    if (!description.trim()) {
      setErrorMsg('Job description is required.');
      return;
    }

    const skillArray = skills.split(',').map((s) => s.trim()).filter(Boolean);
    if (skillArray.length === 0) {
      setErrorMsg('Please enter at least one required skill (e.g. Java, Python, React).');
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
      education: education?.trim() || '',
      educationRequirements: education?.trim() ? [education.trim()] : [],
      benefits: benefitArray,
      openings: Number(openings) > 0 ? Number(openings) : 1,
      applicationDeadline: deadline || null,
      description: description.trim(),
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

  return (
    <div className="max-w-4xl mx-auto space-y-6 pb-12">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <Button variant="ghost" size="sm" onClick={() => navigate(ROUTES.RECRUITER_JOBS)}>
            <ArrowLeft className="w-4 h-4 mr-1" /> Back
          </Button>
          <div>
            <h2 className="text-2xl font-bold font-heading text-foreground">
              {id ? 'Edit Job Posting' : 'Create New Job Opening'}
            </h2>
            <p className="text-xs text-muted-foreground">
              Configure parameters, salary range, and let AI polish requirements.
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

      <form onSubmit={handlePublish} className="space-y-6">
        <Card glass className="p-6 space-y-5">
          <div className="flex items-center justify-between pb-3 border-b border-border/50">
            <h3 className="text-base font-bold text-foreground">Job Details</h3>
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={handleGenerateWithAi}
              disabled={isAiGenerating}
              className="border-[#F56681]/30 hover:bg-[#F56681]/10 text-foreground"
            >
              {isAiGenerating ? (
                <Loader2 className="w-3.5 h-3.5 mr-1.5 animate-spin text-[#F56681]" />
              ) : (
                <Sparkles className="w-3.5 h-3.5 mr-1.5 text-[#F56681]" />
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
                onChange={(val) => setLevel(val)}
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
        <Card glass className="p-6 space-y-5">
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

        {/* Skills & Description */}
        <Card glass className="p-6 space-y-5">
          <h3 className="text-base font-bold text-foreground pb-3 border-b border-border/50">
            Requirements & Description
          </h3>

          <div>
            <label className="block text-xs font-bold uppercase tracking-wider text-muted-foreground mb-1.5">
              Required Skills * (Comma-separated for ATS Matching)
            </label>
            <Input
              required
              value={skills}
              onChange={(e) => setSkills(e.target.value)}
              placeholder="e.g. React, Node.js, Spring Boot, Java, PostgreSQL, Docker"
            />
          </div>

          <div>
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
              rows={6}
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Detail key responsibilities, team structure, tech stack, and role requirements..."
            />
          </div>
        </Card>

        <div className="flex items-center justify-end gap-4 pt-2">
          <Button type="button" variant="ghost" onClick={() => navigate(ROUTES.RECRUITER_JOBS)}>
            Cancel
          </Button>
          <Button type="submit" variant="gradient" size="lg" isLoading={isSubmitting} className="font-bold shadow-xl">
            {id ? 'Update Job Posting' : 'Publish Job Opening'}
          </Button>
        </div>
      </form>
    </div>
  );
};
