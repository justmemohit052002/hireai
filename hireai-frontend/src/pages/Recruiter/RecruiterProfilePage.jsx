import React, { useState, useEffect } from 'react';
import {
  Building2,
  Globe,
  MapPin,
  Save,
  Mail,
  Phone,
  Users,
  CheckCircle2,
  AlertCircle,
  Loader2,
  Eye,
  ExternalLink,
  ShieldCheck,
  Briefcase,
  Layers,
  Sparkles,
  Link2,
  Check
} from 'lucide-react';
import { useAuth } from '@/context/AuthContext';
import { Card } from '@/components/ui/Card';
import { Input } from '@/components/ui/Input';
import { PhoneInput } from '@/components/ui/PhoneInput';
import { Textarea } from '@/components/ui/Textarea';
import { Button } from '@/components/ui/Button';
import { Badge } from '@/components/ui/Badge';
import { recruiterApi } from '@/services/api/recruiter.api';

const COMPANY_SIZE_OPTIONS = [
  { label: '1-10 (Seed)', value: 10 },
  { label: '11-50 (Startup)', value: 50 },
  { label: '51-200 (Growth)', value: 200 },
  { label: '201-500 (Scale-up)', value: 500 },
  { label: '500+ (Enterprise)', value: 1000 },
];

const getCompanySizeLabel = (val) => {
  if (val === null || val === undefined || val === '') return 'Not set';
  const num = Number(val);
  const matched = COMPANY_SIZE_OPTIONS.find((opt) => opt.value === num);
  if (matched) return matched.label;
  return `${val} employees`;
};

const INDUSTRY_PRESETS = [
  'Artificial Intelligence',
  'Fintech',
  'Enterprise SaaS',
  'Cloud & DevOps',
  'Cybersecurity',
  'E-Commerce',
  'HealthTech',
];

export const RecruiterProfilePage = () => {
  const { user } = useAuth();
  const [activeTab, setActiveTab] = useState('edit'); // 'edit' | 'preview'
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [successMsg, setSuccessMsg] = useState('');
  const [errorMsg, setErrorMsg] = useState('');

  // Profile fields - initialize ONLY with real registration data, everything else empty
  const [companyName, setCompanyName] = useState(user?.companyName || '');
  const [designation, setDesignation] = useState('');
  const [industry, setIndustry] = useState('');
  const [companyLocation, setCompanyLocation] = useState('');
  const [companyWebsite, setCompanyWebsite] = useState('');
  const [companyEmail, setCompanyEmail] = useState(user?.email || '');
  const [companyPhone, setCompanyPhone] = useState(user?.phone || '');
  const [companySize, setCompanySize] = useState(null);
  const [companyDescription, setCompanyDescription] = useState('');

  // Sync initial registration values if user object arrives asynchronously
  useEffect(() => {
    if (user?.companyName && !companyName) setCompanyName(user.companyName);
    if (user?.email && !companyEmail) setCompanyEmail(user.email);
    if (user?.phone && !companyPhone) setCompanyPhone(user.phone);
  }, [user]);

  useEffect(() => {
    async function loadProfile() {
      setIsLoading(true);
      try {
        const profile = await recruiterApi.getMyProfile();
        if (profile) {
          if (profile.companyName) setCompanyName(profile.companyName);
          if (profile.designation) setDesignation(profile.designation);
          if (profile.industry) setIndustry(profile.industry);
          if (profile.city || profile.state || profile.country || profile.address) {
            const loc = [profile.city, profile.state, profile.country].filter(Boolean).join(', ');
            setCompanyLocation(loc || profile.address || '');
          }
          if (profile.companyWebsite) setCompanyWebsite(profile.companyWebsite);
          if (profile.companyEmail) setCompanyEmail(profile.companyEmail);
          if (profile.companyPhone) setCompanyPhone(profile.companyPhone);
          if (profile.companySize != null) setCompanySize(Number(profile.companySize));
          if (profile.companyDescription) setCompanyDescription(profile.companyDescription);
        }
      } catch (err) {
        console.warn('Could not load remote recruiter profile:', err.message);
      } finally {
        setIsLoading(false);
      }
    }
    loadProfile();
  }, []);

  const handleSave = async (e) => {
    e.preventDefault();
    setErrorMsg('');
    setIsSaving(true);

    let city = null;
    let state = null;
    let country = null;
    if (companyLocation && companyLocation.trim()) {
      const parts = companyLocation.split(',').map((p) => p.trim()).filter(Boolean);
      if (parts.length >= 3) {
        city = parts[0];
        state = parts[1];
        country = parts.slice(2).join(', ');
      } else if (parts.length === 2) {
        city = parts[0];
        state = parts[1];
      } else if (parts.length === 1) {
        city = parts[0];
      }
    }

    let parsedSize = null;
    if (typeof companySize === 'number' && Number.isFinite(companySize)) {
      parsedSize = companySize;
    } else if (typeof companySize === 'string' && companySize.trim() !== '') {
      if (companySize.includes('1-10')) parsedSize = 10;
      else if (companySize.includes('11-50')) parsedSize = 50;
      else if (companySize.includes('51-200')) parsedSize = 200;
      else if (companySize.includes('201-500')) parsedSize = 500;
      else if (companySize.includes('500+')) parsedSize = 1000;
      else {
        const n = parseInt(companySize.replace(/\D/g, ''), 10);
        parsedSize = isNaN(n) ? null : n;
      }
    }

    const payload = {
      companyName: companyName?.trim(),
      designation: designation?.trim() || null,
      industry: industry?.trim() || null,
      city: city,
      state: state,
      country: country,
      address: companyLocation?.trim() || null,
      companyWebsite: companyWebsite?.trim() || null,
      companyEmail: companyEmail?.trim() || null,
      companyPhone: companyPhone?.trim() || null,
      companySize: parsedSize,
      companyDescription: companyDescription?.trim() || null,
    };

    try {
      await recruiterApi.updateProfile(payload);
      setSuccessMsg('Employer profile updated successfully!');
      setTimeout(() => setSuccessMsg(''), 3500);
    } catch (err) {
      try {
        await recruiterApi.createProfile(payload);
        setSuccessMsg('Employer profile created successfully!');
        setTimeout(() => setSuccessMsg(''), 3500);
      } catch (createErr) {
        setErrorMsg(createErr.message || 'Failed to save employer profile.');
      }
    } finally {
      setIsSaving(false);
    }
  };

  const companyInitial = companyName ? companyName[0].toUpperCase() : (user?.companyName ? user.companyName[0].toUpperCase() : 'C');
  const recruiterName = `${user?.firstName || ''} ${user?.lastName || ''}`.trim() || 'Recruiter';

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

      {/* Hero Header Banner */}
      <div className="relative overflow-hidden rounded-3xl surface-nested border border-border/70 p-6 md:p-8 shadow-md">
        <div className="absolute top-0 inset-x-0 h-1.5 bg-gradient-to-r from-[#C63FC5] via-[#F56681] to-[#FC9559]" />

        <div className="flex flex-col md:flex-row md:items-center justify-between gap-6">
          <div className="flex items-center gap-5">
            <div className="w-16 h-16 rounded-2xl bg-gradient-to-br from-[#C63FC5]/20 via-[#F56681]/20 to-[#FC9559]/20 border border-[#F56681]/30 flex items-center justify-center font-bold text-2xl text-foreground shrink-0 shadow-md">
              {companyInitial}
            </div>

            <div className="space-y-1">
              <div className="flex items-center gap-2.5 flex-wrap">
                <h1 className="text-2xl sm:text-3xl font-bold font-heading text-foreground">
                  {companyName || 'Company Profile'}
                </h1>
                <span className="inline-flex items-center gap-1 text-[11px] font-bold px-2.5 py-0.5 rounded-full bg-emerald-500/15 text-emerald-400 border border-emerald-500/30">
                  <ShieldCheck className="w-3.5 h-3.5" /> Verified Employer
                </span>
              </div>
              <p className="text-xs sm:text-sm text-muted-foreground flex items-center gap-2">
                {industry && (
                  <>
                    <span>{industry}</span>
                    <span>•</span>
                  </>
                )}
                <span className="flex items-center gap-1">
                  <MapPin className="w-3.5 h-3.5 text-accent" /> {companyLocation || 'Location not set'}
                </span>
                <span>•</span>
                <span>{jobs.length} Active Positions</span>
              </p>
            </div>
          </div>

          {/* Tab Mode Switcher */}
          <div className="flex rounded-xl bg-surface-2 p-1 border border-border/70 self-start md:self-center">
            <button
              type="button"
              onClick={() => setActiveTab('edit')}
              className={`px-3.5 py-1.5 rounded-lg text-xs font-semibold transition-all ${
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
              className={`px-3.5 py-1.5 rounded-lg text-xs font-semibold flex items-center gap-1.5 transition-all ${
                activeTab === 'preview'
                  ? 'bg-background text-foreground shadow-xs'
                  : 'text-muted-foreground hover:text-foreground'
              }`}
            >
              <Eye className="w-3.5 h-3.5" /> Candidate View
            </button>
          </div>
        </div>
      </div>

      {/* Tab Switch Content */}
      {activeTab === 'preview' ? (
        /* =================== CANDIDATE VIEW PREVIEW =================== */
        <div className="space-y-6">
          <div className="p-3.5 rounded-2xl bg-blue-500/10 border border-blue-500/20 text-xs text-blue-400 flex items-center justify-between">
            <span className="flex items-center gap-2">
              <Eye className="w-4 h-4" /> This is a live preview of how job applicants and candidates see your company profile card.
            </span>
            <Button size="sm" variant="outline" onClick={() => setActiveTab('edit')}>
              Return to Edit
            </Button>
          </div>

          <Card surface-nested className="p-8 space-y-6">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-6 border-b border-border/50">
              <div className="flex items-center gap-4">
                <div className="w-16 h-16 rounded-2xl bg-gradient-to-br from-[#C63FC5]/20 to-[#F56681]/20 border border-[#F56681]/30 flex items-center justify-center font-bold text-3xl text-[#F56681]">
                  {companyInitial}
                </div>
                <div>
                  <h2 className="text-2xl font-bold font-heading text-foreground">{companyName || 'Your Company Name'}</h2>
                  <p className="text-xs sm:text-sm text-muted-foreground flex items-center gap-2 mt-0.5">
                    <span>{industry || 'Industry not specified'}</span>
                    <span>•</span>
                    <span>{companyLocation || 'Location not specified'}</span>
                  </p>
                </div>
              </div>

              {companyWebsite && (
                <a
                  href={companyWebsite.startsWith('http') ? companyWebsite : `https://${companyWebsite}`}
                  target="_blank"
                  rel="noreferrer"
                  className="inline-flex items-center gap-1.5 px-3.5 py-2 rounded-xl bg-surface-2 hover:bg-surface-3 border border-border text-xs font-semibold text-foreground transition-colors"
                >
                  <Globe className="w-3.5 h-3.5 text-accent" />
                  <span>Visit Careers Page</span>
                  <ExternalLink className="w-3 h-3 text-muted-foreground ml-0.5" />
                </a>
              )}
            </div>

            {/* Quick Metrics Strip */}
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 p-4 rounded-2xl bg-surface-2 border border-border/60 text-xs">
              <div>
                <span className="text-muted-foreground block text-[11px] uppercase font-bold tracking-wider">Company Size</span>
                <span className="font-bold text-foreground text-sm">{getCompanySizeLabel(companySize)}</span>
              </div>
              <div>
                <span className="text-muted-foreground block text-[11px] uppercase font-bold tracking-wider">Active Openings</span>
                <span className="font-bold text-foreground text-sm">{jobs.length} Positions</span>
              </div>
              <div>
                <span className="text-muted-foreground block text-[11px] uppercase font-bold tracking-wider">Talent Partner</span>
                <span className="font-bold text-foreground text-sm">{recruiterName}</span>
              </div>
              <div>
                <span className="text-muted-foreground block text-[11px] uppercase font-bold tracking-wider">Trust Rating</span>
                <span className="font-bold text-emerald-400 text-sm">Verified Tier-1</span>
              </div>
            </div>

            {/* Company Bio */}
            <div className="space-y-2">
              <h3 className="text-xs font-bold uppercase tracking-wider text-muted-foreground">About the Company & Culture</h3>
              <p className="text-sm text-foreground leading-relaxed whitespace-pre-line">
                {companyDescription || 'No company description provided yet. Use the edit tab to describe your organization, culture, and benefits.'}
              </p>
            </div>

            {/* Contact Information */}
            <div className="p-4 rounded-2xl bg-surface-2/60 border border-border/50 flex flex-wrap items-center justify-between gap-4 text-xs text-muted-foreground">
              {companyEmail && (
                <span className="flex items-center gap-2">
                  <Mail className="w-4 h-4 text-accent" /> {companyEmail}
                </span>
              )}
              {companyPhone && (
                <span className="flex items-center gap-2">
                  <Phone className="w-4 h-4 text-accent" /> {companyPhone}
                </span>
              )}
              {companyLocation && (
                <span className="flex items-center gap-2">
                  <MapPin className="w-4 h-4 text-accent" /> {companyLocation}
                </span>
              )}
            </div>
          </Card>
        </div>
      ) : (
        /* =================== EDIT PROFILE FORM =================== */
        <Card surface-nested className="p-6 sm:p-8 space-y-6">
          <form onSubmit={handleSave} className="space-y-6">
            {/* Section 1: Company Info */}
            <div className="space-y-4">
              <h3 className="text-xs font-bold uppercase tracking-wider text-muted-foreground border-b border-border/50 pb-2">
                Company & Organization Details
              </h3>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-bold uppercase tracking-wider text-muted-foreground mb-1.5">
                    Company Name
                  </label>
                  <Input
                    required
                    value={companyName}
                    onChange={(e) => setCompanyName(e.target.value)}
                    icon={<Building2 className="w-4 h-4" />}
                    placeholder="e.g. Acme Corp"
                  />
                </div>

                <div>
                  <label className="block text-xs font-bold uppercase tracking-wider text-muted-foreground mb-1.5">
                    Company Headquarters / Location
                  </label>
                  <Input
                    value={companyLocation}
                    onChange={(e) => setCompanyLocation(e.target.value)}
                    icon={<MapPin className="w-4 h-4" />}
                    placeholder="e.g. Pune, Maharashtra"
                  />
                </div>
              </div>

              {/* Industry Selector with Quick-Pills */}
              <div className="space-y-2">
                <label className="block text-xs font-bold uppercase tracking-wider text-muted-foreground">
                  Industry Domain
                </label>
                <Input
                  value={industry}
                  onChange={(e) => setIndustry(e.target.value)}
                  placeholder="e.g. Artificial Intelligence, Fintech, Cloud SaaS"
                />
                <div className="flex flex-wrap items-center gap-1.5 pt-1">
                  <span className="text-[11px] text-muted-foreground font-semibold">Quick select:</span>
                  {INDUSTRY_PRESETS.map((ind) => (
                    <button
                      key={ind}
                      type="button"
                      onClick={() => setIndustry(ind)}
                      className={`text-[11px] px-2.5 py-0.5 rounded-md border transition-all ${
                        industry.toLowerCase() === ind.toLowerCase()
                          ? 'bg-accent/15 border-accent text-accent font-bold'
                          : 'bg-surface-2 border-border/60 text-muted-foreground hover:text-foreground'
                      }`}
                    >
                      {ind}
                    </button>
                  ))}
                </div>
              </div>

              {/* Company Size with Interactive Chips */}
              <div className="space-y-2">
                <label className="block text-xs font-bold uppercase tracking-wider text-muted-foreground">
                  Company Headcount / Size
                </label>
                <div className="grid grid-cols-2 sm:grid-cols-5 gap-2">
                  {COMPANY_SIZE_OPTIONS.map((opt) => (
                    <button
                      key={opt.value}
                      type="button"
                      onClick={() => setCompanySize(opt.value)}
                      className={`p-2.5 rounded-xl border text-xs text-center transition-all ${
                        Number(companySize) === opt.value
                          ? 'bg-accent/15 border-accent text-accent font-bold shadow-xs'
                          : 'bg-surface-2 border-border/60 text-muted-foreground hover:text-foreground'
                      }`}
                    >
                      {opt.label}
                    </button>
                  ))}
                </div>
              </div>

              {/* Company Website */}
              <div>
                <div className="flex items-center justify-between mb-1.5">
                  <label className="block text-xs font-bold uppercase tracking-wider text-muted-foreground">
                    Company Website / Careers URL
                  </label>
                  {companyWebsite && (
                    <a
                      href={companyWebsite.startsWith('http') ? companyWebsite : `https://${companyWebsite}`}
                      target="_blank"
                      rel="noreferrer"
                      className="text-[11px] text-accent hover:underline inline-flex items-center gap-1"
                    >
                      Test Link <ExternalLink className="w-3 h-3" />
                    </a>
                  )}
                </div>
                <Input
                  value={companyWebsite}
                  onChange={(e) => setCompanyWebsite(e.target.value)}
                  icon={<Globe className="w-4 h-4" />}
                  placeholder="https://company.com"
                />
              </div>

              {/* Company Description */}
              <div className="space-y-1.5">
                <label className="block text-xs font-bold uppercase tracking-wider text-muted-foreground">
                  Company Mission & Work Culture
                </label>
                <Textarea
                  value={companyDescription}
                  onChange={(e) => setCompanyDescription(e.target.value)}
                  rows={3}
                  placeholder="Describe your engineering philosophy, growth opportunities, and workplace benefits..."
                />
              </div>
            </div>

            {/* Section 2: Recruiter Profile */}
            <div className="space-y-4 pt-4 border-t border-border/50">
              <h3 className="text-xs font-bold uppercase tracking-wider text-muted-foreground border-b border-border/50 pb-2">
                Recruiter Representative Details
              </h3>

              <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                <div>
                  <label className="block text-xs font-bold uppercase tracking-wider text-muted-foreground mb-1.5">
                    Your Role / Designation
                  </label>
                  <Input
                    value={designation}
                    onChange={(e) => setDesignation(e.target.value)}
                    icon={<Briefcase className="w-4 h-4" />}
                    placeholder="e.g. Lead Talent Acquisition"
                  />
                </div>

                <div>
                  <label className="block text-xs font-bold uppercase tracking-wider text-muted-foreground mb-1.5">
                    Direct Email Address
                  </label>
                  <Input
                    type="email"
                    value={companyEmail}
                    onChange={(e) => setCompanyEmail(e.target.value)}
                    icon={<Mail className="w-4 h-4" />}
                    placeholder="recruiter@company.com"
                  />
                </div>

                <div>
                  <label className="block text-xs font-bold uppercase tracking-wider text-muted-foreground mb-1.5">
                    Phone / WhatsApp Contact
                  </label>
                  <PhoneInput
                    value={companyPhone}
                    onChange={(val) => setCompanyPhone(val)}
                  />
                </div>
              </div>
            </div>

            {/* Submit Action */}
            <div className="pt-4 border-t border-border/60 flex items-center justify-between">
              <span className="text-xs text-muted-foreground">
                Updated details appear across all candidate job postings.
              </span>
              <Button type="submit" variant="gradient" size="lg" isLoading={isSaving} className="font-bold shadow-xl">
                <Save className="w-4 h-4 mr-2" />
                Save Employer Profile
              </Button>
            </div>
          </form>
        </Card>
      )}
    </div>
  );
};
