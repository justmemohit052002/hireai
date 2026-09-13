import React, { useState, useEffect } from 'react';
import {
  X,
  Sparkles,
  User,
  Mail,
  Phone,
  FileText,
  Download,
  Calendar,
  CheckCircle2,
  XCircle,
  Clock,
  Briefcase,
  MapPin,
  RefreshCw,
  Loader2,
  ChevronRight,
  Filter,
} from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';
import { Button } from '@/components/ui/Button';
import { Badge } from '@/components/ui/Badge';
import { Avatar } from '@/components/ui/Avatar';
import { applicationsApi } from '@/services/api/applications.api';
import { useJobs } from '@/context/JobsContext';
import { formatSalary, formatShortDate, getJobTypeLabel } from '@/utils';

const STAGES = [
  { value: 'APPLIED', label: 'Applied', color: 'bg-blue-500/15 text-blue-500 border-blue-500/30' },
  { value: 'SCREENING', label: 'Screening', color: 'bg-amber-500/15 text-amber-500 border-amber-500/30' },
  { value: 'SHORTLISTED', label: 'Shortlisted', color: 'bg-emerald-500/15 text-emerald-500 border-emerald-500/30' },
  { value: 'INTERVIEW_SCHEDULED', label: 'Interview', color: 'bg-purple-500/15 text-purple-500 border-purple-500/30' },
  { value: 'OFFERED', label: 'Offered', color: 'bg-cyan-500/15 text-cyan-500 border-cyan-500/30' },
  { value: 'REJECTED', label: 'Rejected', color: 'bg-red-500/15 text-red-500 border-red-500/30' },
];

export const JobApplicantsDrawer = ({
  job,
  isOpen,
  onClose,
  onApplicationUpdated,
}) => {
  const { applications: contextApplications } = useJobs() || {};
  const [applicants, setApplicants] = useState([]);
  const [isLoading, setIsLoading] = useState(false);
  const [filterStage, setFilterStage] = useState('ALL');
  const [updatingAppId, setUpdatingAppId] = useState(null);
  const [downloadingAppId, setDownloadingAppId] = useState(null);
  const [resumeNotice, setResumeNotice] = useState(null);
  const [previewModal, setPreviewModal] = useState(null); // { url, name, filename, isPdf }

  const fetchApplicants = async () => {
    if (!job?.id) return;
    setIsLoading(true);
    try {
      const data = await applicationsApi.getJobApplications(job.id);
      let list = Array.isArray(data)
        ? data
        : Array.isArray(data?.data)
        ? data.data
        : Array.isArray(data?.content)
        ? data.content
        : [];

      // If backend returned empty array, check if any application for this job exists in memory/context
      if (list.length === 0 && Array.isArray(contextApplications)) {
        const matchingFromContext = contextApplications.filter(
          (a) => a.jobId === job.id || a.job?.id === job.id
        );
        if (matchingFromContext.length > 0) {
          list = matchingFromContext;
        }
      }

      // Sort highest ATS Match Score first
      const sorted = [...list].sort((a, b) => (b.atsMatchScore || 0) - (a.atsMatchScore || 0));
      setApplicants(sorted);
    } catch (err) {
      console.warn('Could not fetch applicants for job:', err);
      // Fallback to context applications if network request failed
      if (Array.isArray(contextApplications)) {
        const matchingFromContext = contextApplications.filter(
          (a) => a.jobId === job.id || a.job?.id === job.id
        );
        if (matchingFromContext.length > 0) {
          setApplicants(matchingFromContext);
        }
      }
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    if (isOpen && job?.id) {
      fetchApplicants();
    }
  }, [isOpen, job?.id]);

  const handleUpdateStatus = async (applicationId, newStatus) => {
    setUpdatingAppId(applicationId);
    try {
      await applicationsApi.updateApplicationStatus(applicationId, {
        status: newStatus,
        feedbackNotes: `Stage updated to ${newStatus} by recruiter`,
      });

      setApplicants((prev) =>
        prev.map((app) => (app.id === applicationId ? { ...app, status: newStatus } : app))
      );

      if (onApplicationUpdated) {
        onApplicationUpdated(applicationId, newStatus);
      }
    } catch (err) {
      console.error('Failed to update applicant status:', err);
    } finally {
      setUpdatingAppId(null);
    }
  };

  const handleDownloadResume = async (applicant) => {
    if (!applicant) return;
    setDownloadingAppId(applicant.id);
    setResumeNotice(null);

    try {
      let blob = null;

      // 1. Try application-level authenticated download
      try {
        blob = await applicationsApi.downloadApplicationResume(applicant.id);
      } catch (err1) {
        console.warn('Application resume endpoint not available, trying candidate resume endpoint...', err1);
      }

      // 2. Fallback to candidate-level resume download
      if (!blob && applicant.candidateId) {
        try {
          blob = await applicationsApi.downloadCandidateResume(applicant.candidateId);
        } catch (err2) {
          console.warn('Candidate resume endpoint failed:', err2);
        }
      }

      if (blob instanceof Blob && blob.size > 0) {
        const fileName = applicant.resumeFileName || `${applicant.candidateName || 'Candidate'}_Resume.pdf`;
        const isPdf = fileName.toLowerCase().endsWith('.pdf') || blob.type?.includes('pdf') || blob.type === 'application/octet-stream';

        const fileBlob = new Blob([blob], {
          type: isPdf ? 'application/pdf' : 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
        });
        const blobUrl = URL.createObjectURL(fileBlob);

        // Open in-app Preview Modal with download options
        setPreviewModal({
          url: blobUrl,
          name: applicant.candidateName || 'Candidate',
          filename: fileName,
          isPdf,
        });

        // Also try opening in a new tab if PDF
        if (isPdf) {
          const newTab = window.open(blobUrl, '_blank');
          if (!newTab) {
            console.info('Browser popup blocked; preview modal will display resume directly.');
          }
        }
      } else {
        setResumeNotice({
          appId: applicant.id,
          message: 'No uploaded resume document attached to this candidate application.',
        });
        setTimeout(() => setResumeNotice(null), 5000);
      }
    } catch (err) {
      console.error('Resume download error:', err);
      setResumeNotice({
        appId: applicant.id,
        message: err.message || 'Could not load resume document.',
      });
      setTimeout(() => setResumeNotice(null), 5000);
    } finally {
      setDownloadingAppId(null);
    }
  };

  const handleClosePreviewModal = () => {
    if (previewModal?.url) {
      URL.revokeObjectURL(previewModal.url);
    }
    setPreviewModal(null);
  };

  if (!isOpen || !job) return null;

  const filteredApplicants = applicants.filter((app) => {
    if (filterStage === 'ALL') return true;
    if (filterStage === 'SHORTLISTED') {
      return (
        app.status === 'SHORTLISTED' ||
        (app.atsMatchScore != null && Number(app.atsMatchScore) >= 70)
      );
    }
    if (filterStage === 'REJECTED') {
      return (
        app.status === 'REJECTED' ||
        (app.atsMatchScore != null && Number(app.atsMatchScore) < 70)
      );
    }
    return app.status === filterStage;
  });

  return (
    <AnimatePresence>
      <div className="fixed inset-0 z-50 flex justify-end">
        {/* Backdrop */}
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          onClick={onClose}
          className="fixed inset-0 bg-black/60 backdrop-blur-sm"
        />

        {/* Slide-in Drawer */}
        <motion.div
          initial={{ x: '100%' }}
          animate={{ x: 0 }}
          exit={{ x: '100%' }}
          transition={{ type: 'spring', damping: 28, stiffness: 300 }}
          className="relative z-10 w-full max-w-3xl h-full bg-background border-l border-border/80 shadow-2xl flex flex-col overflow-hidden"
        >
          {/* Drawer Header */}
          <div className="p-6 border-b border-border/60 bg-surface/50 backdrop-blur-md shrink-0 space-y-4">
            <div className="flex items-start justify-between gap-4">
              <div className="space-y-1 min-w-0">
                <div className="flex items-center gap-2 flex-wrap">
                  <h2 className="text-xl sm:text-2xl font-bold font-heading text-foreground tracking-tight">
                    {job.title}
                  </h2>
                  <Badge variant="default" className="text-[11px] font-mono capitalize">
                    {job.listingStatus || 'open'}
                  </Badge>
                  <span className="text-[11px] font-semibold px-2 py-0.5 rounded-md border border-border/60 bg-surface-2 text-muted-foreground capitalize">
                    {job.workplaceType || 'hybrid'}
                  </span>
                </div>
                <div className="flex items-center gap-3 text-xs text-muted-foreground font-medium flex-wrap">
                  <span>{getJobTypeLabel(job.type)}</span>
                  <span>•</span>
                  <span>{formatSalary(job.salary)}</span>
                  <span>•</span>
                  <span className="flex items-center gap-1">
                    <MapPin className="w-3.5 h-3.5 text-primary" />
                    {job.location || 'Remote'}
                  </span>
                  <span>•</span>
                  <span>Posted {formatShortDate(job.postedAt)}</span>
                </div>
              </div>

              <div className="flex items-center gap-2">
                <Button
                  size="sm"
                  variant="ghost"
                  onClick={fetchApplicants}
                  disabled={isLoading}
                  title="Refresh Applicants"
                  className="h-8 w-8 p-0 text-muted-foreground hover:text-foreground"
                >
                  <RefreshCw className={`w-4 h-4 ${isLoading ? 'animate-spin text-primary' : ''}`} />
                </Button>
                <button
                  onClick={onClose}
                  className="rounded-full p-2 text-muted-foreground hover:bg-muted hover:text-foreground transition-colors"
                >
                  <X className="w-5 h-5" />
                </button>
              </div>
            </div>

            {/* Quick Stats Summary */}
            <div className="grid grid-cols-3 gap-3">
              <div className="p-3 rounded-xl bg-surface-2 border border-border/50 text-center">
                <div className="text-[10px] uppercase font-bold tracking-wider text-muted-foreground">
                  Total Applicants
                </div>
                <div className="text-lg font-bold font-mono text-foreground mt-0.5">
                  {applicants.length}
                </div>
              </div>
              <div className="p-3 rounded-xl bg-emerald-500/10 border border-emerald-500/20 text-center">
                <div className="text-[10px] uppercase font-bold tracking-wider text-emerald-500">
                  AI Shortlisted (&gt;= 70%)
                </div>
                <div className="text-lg font-bold font-mono text-emerald-400 mt-0.5">
                  {applicants.filter((a) => (a.atsMatchScore || 0) >= 70).length}
                </div>
              </div>
              <div className="p-3 rounded-xl bg-[#F56681]/10 border border-[#F56681]/20 text-center">
                <div className="text-[10px] uppercase font-bold tracking-wider text-[#F56681]">
                  Avg ATS Score
                </div>
                <div className="text-lg font-bold font-mono text-[#F56681] mt-0.5">
                  {applicants.length > 0
                    ? `${Math.round(
                        applicants.reduce((acc, c) => acc + (c.atsMatchScore || 0), 0) /
                          applicants.length
                      )}%`
                    : '0%'}
                </div>
              </div>
            </div>

            {/* Stage Filter Tabs */}
            <div className="flex items-center gap-1.5 overflow-x-auto pb-1 pt-1 scrollbar-none text-xs">
              {['ALL', 'SHORTLISTED', 'REJECTED', 'INTERVIEW_SCHEDULED', 'OFFERED'].map((stage) => {
                const isActive = filterStage === stage;
                const label =
                  stage === 'ALL'
                    ? `All (${applicants.length})`
                    : stage === 'SHORTLISTED'
                    ? `Shortlisted (${applicants.filter((a) => (a.atsMatchScore || 0) >= 70).length})`
                    : stage === 'REJECTED'
                    ? `Rejected (${applicants.filter((a) => (a.atsMatchScore || 0) < 70).length})`
                    : stage.replace('_', ' ');

                return (
                  <button
                    key={stage}
                    onClick={() => setFilterStage(stage)}
                    className={`px-3 py-1 rounded-lg font-semibold whitespace-nowrap transition-all ${
                      isActive
                        ? 'bg-gradient-to-r from-[#C63FC5] to-[#F56681] text-white shadow-sm'
                        : 'bg-surface-2 text-muted-foreground hover:text-foreground border border-border/40'
                    }`}
                  >
                    {label}
                  </button>
                );
              })}
            </div>
          </div>

          {/* Drawer Body: Applicants List */}
          <div className="flex-1 overflow-y-auto p-6 space-y-4">
            {isLoading ? (
              <div className="py-20 flex flex-col items-center justify-center text-center space-y-3">
                <Loader2 className="w-8 h-8 text-primary animate-spin" />
                <p className="text-sm font-semibold text-foreground">Fetching Live Candidate Pipeline...</p>
                <p className="text-xs text-muted-foreground">Computing ATS Match rankings & AI skill graphs</p>
              </div>
            ) : filteredApplicants.length > 0 ? (
              filteredApplicants.map((applicant, idx) => {
                const matchScore = applicant.atsMatchScore != null ? Math.round(applicant.atsMatchScore) : 0;
                const isShortlisted = matchScore >= 70;
                const stageObj = STAGES.find((s) => s.value === String(applicant.status).toUpperCase()) || STAGES[0];
                const matchingSkills = Array.isArray(applicant.matchingSkills)
                  ? applicant.matchingSkills
                  : typeof applicant.matchingSkills === 'string' && applicant.matchingSkills
                  ? applicant.matchingSkills.split(',').map((s) => s.trim())
                  : [];
                const missingSkills = Array.isArray(applicant.missingSkills)
                  ? applicant.missingSkills
                  : typeof applicant.missingSkills === 'string' && applicant.missingSkills
                  ? applicant.missingSkills.split(',').map((s) => s.trim())
                  : [];

                return (
                  <div
                    key={applicant.id}
                    className="p-5 rounded-2xl glass border border-border/70 hover:border-primary/40 shadow-xs transition-all space-y-4"
                  >
                    {/* Top Row: Candidate details & Score Badge */}
                    <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                      <div className="flex items-center gap-3.5">
                        <Avatar
                          name={applicant.candidateName || 'Candidate'}
                          src={applicant.candidateProfilePhotoUrl}
                          size="lg"
                          className="border-2 border-primary/20"
                        />
                        <div className="space-y-0.5 min-w-0">
                          <div className="flex items-center gap-2">
                            <h4 className="font-bold text-base text-foreground truncate">
                              {applicant.candidateName || 'Candidate'}
                            </h4>
                            <span className="text-[10px] font-mono px-2 py-0.5 rounded-md bg-surface-2 text-muted-foreground border border-border/50">
                              #{idx + 1} Rank
                            </span>
                          </div>
                          <div className="flex items-center gap-3 text-xs text-muted-foreground flex-wrap">
                            {applicant.candidateEmail && (
                              <span className="flex items-center gap-1">
                                <Mail className="w-3.5 h-3.5 text-primary shrink-0" />
                                <span className="truncate max-w-[170px]">{applicant.candidateEmail}</span>
                              </span>
                            )}
                            {applicant.candidatePhone && (
                              <span className="flex items-center gap-1">
                                <Phone className="w-3.5 h-3.5 text-primary shrink-0" />
                                <span>{applicant.candidatePhone}</span>
                              </span>
                            )}
                          </div>
                        </div>
                      </div>

                      {/* ATS Score & AI Classification */}
                      <div className="flex items-center sm:flex-col items-end gap-1 shrink-0">
                        <div
                          className={`inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl border font-mono font-bold text-sm ${
                            isShortlisted
                              ? 'bg-emerald-500/15 border-emerald-500/30 text-emerald-400 shadow-sm shadow-emerald-500/10'
                              : 'bg-rose-500/15 border-rose-500/30 text-rose-400'
                          }`}
                        >
                          <Sparkles className="w-4 h-4" />
                          <span>{matchScore}% ATS Match</span>
                        </div>
                        <span className="text-[10px] font-semibold text-muted-foreground">
                          {isShortlisted ? '✓ Shortlist Threshold Met' : '✗ Below 70% Cutoff'}
                        </span>
                      </div>
                    </div>

                    {/* Skills Overlap Breakdown */}
                    {(matchingSkills.length > 0 || missingSkills.length > 0) && (
                      <div className="p-3 rounded-xl bg-surface-2/60 border border-border/40 text-xs space-y-2">
                        {matchingSkills.length > 0 && (
                          <div className="flex items-center gap-1.5 flex-wrap">
                            <span className="text-[11px] font-bold text-emerald-400 shrink-0">Matched Skills:</span>
                            {matchingSkills.slice(0, 6).map((skill, sIdx) => (
                              <span
                                key={sIdx}
                                className="text-[10px] font-medium px-2 py-0.5 rounded-md bg-emerald-500/10 border border-emerald-500/20 text-emerald-300"
                              >
                                {skill}
                              </span>
                            ))}
                          </div>
                        )}
                        {missingSkills.length > 0 && (
                          <div className="flex items-center gap-1.5 flex-wrap">
                            <span className="text-[11px] font-bold text-rose-400 shrink-0">Skill Gaps:</span>
                            {missingSkills.slice(0, 5).map((skill, sIdx) => (
                              <span
                                key={sIdx}
                                className="text-[10px] font-medium px-2 py-0.5 rounded-md bg-rose-500/10 border border-rose-500/20 text-rose-300"
                              >
                                {skill}
                              </span>
                            ))}
                          </div>
                        )}
                      </div>
                    )}

                    {/* Candidate Cover Note */}
                    {applicant.coverNote && (
                      <div className="text-xs p-3 rounded-xl bg-surface/80 border border-border/50 text-muted-foreground italic">
                        "{applicant.coverNote}"
                      </div>
                    )}

                    {/* Resume Error/Notice Banner */}
                    {resumeNotice?.appId === applicant.id && (
                      <div className="p-2.5 rounded-xl bg-amber-500/10 border border-amber-500/20 text-amber-400 text-xs flex items-center gap-2">
                        <XCircle className="w-4 h-4 shrink-0 text-amber-400" />
                        <span>{resumeNotice.message}</span>
                      </div>
                    )}

                    {/* Bottom Actions: Resume Download & Stage Changer */}
                    <div className="pt-3 border-t border-border/50 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                      <div className="flex items-center gap-2">
                        <Button
                          size="sm"
                          variant="outline"
                          onClick={() => handleDownloadResume(applicant)}
                          disabled={downloadingAppId === applicant.id}
                          className="h-8 text-xs font-semibold gap-1.5 hover:border-primary/50"
                        >
                          {downloadingAppId === applicant.id ? (
                            <Loader2 className="w-3.5 h-3.5 animate-spin" />
                          ) : (
                            <Download className="w-3.5 h-3.5 text-primary" />
                          )}
                          <span>View Resume</span>
                        </Button>
                        <span className="text-[11px] text-muted-foreground">
                          Applied {formatShortDate(applicant.appliedAt)}
                        </span>
                      </div>

                      {/* Pipeline Stage Buttons */}
                      <div className="flex items-center gap-1.5 flex-wrap">
                        {['SHORTLISTED', 'INTERVIEW_SCHEDULED', 'OFFERED', 'REJECTED'].map((stageVal) => {
                          const isCurrent = String(applicant.status).toUpperCase() === stageVal;
                          const isUpdating = updatingAppId === applicant.id;

                          return (
                            <button
                              key={stageVal}
                              disabled={isCurrent || isUpdating}
                              onClick={() => handleUpdateStatus(applicant.id, stageVal)}
                              className={`text-[11px] font-bold px-2.5 py-1 rounded-lg border transition-all cursor-pointer ${
                                isCurrent
                                  ? 'bg-primary text-white border-primary shadow-xs'
                                  : 'bg-surface-2 text-muted-foreground hover:text-foreground hover:bg-muted border-border/50'
                              } disabled:opacity-60 disabled:cursor-not-allowed`}
                            >
                              {stageVal === 'INTERVIEW_SCHEDULED' ? 'Interview' : stageVal.toLowerCase()}
                            </button>
                          );
                        })}
                      </div>
                    </div>
                  </div>
                );
              })
            ) : (
              <div className="py-16 flex flex-col items-center justify-center text-center space-y-3 p-6 rounded-2xl bg-surface-2/30 border border-dashed border-border/60">
                <div className="w-12 h-12 rounded-2xl bg-[#F56681]/15 text-[#F56681] flex items-center justify-center">
                  <User className="w-6 h-6" />
                </div>
                <h4 className="text-base font-bold text-foreground">No Applicants Found</h4>
                <p className="text-xs text-muted-foreground max-w-xs">
                  {filterStage === 'ALL'
                    ? 'No candidates have submitted an application for this position yet.'
                    : `No candidates currently in '${filterStage.toLowerCase()}' stage.`}
                </p>
                {filterStage !== 'ALL' && (
                  <Button size="sm" variant="outline" onClick={() => setFilterStage('ALL')} className="text-xs">
                    Show All Applicants
                  </Button>
                )}
              </div>
            )}
          </div>

          {/* Interactive Resume Preview Modal */}
          {previewModal && (
            <div className="fixed inset-0 z-60 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm animate-in fade-in duration-200">
              <div className="relative w-full max-w-4xl h-[88vh] bg-background border border-border rounded-2xl shadow-2xl flex flex-col overflow-hidden">
                {/* Modal Header */}
                <div className="flex items-center justify-between px-6 py-4 border-b border-border/80 bg-surface">
                  <div className="flex items-center gap-3">
                    <div className="w-9 h-9 rounded-xl bg-primary/10 border border-primary/20 flex items-center justify-center text-primary">
                      <FileText className="w-5 h-5" />
                    </div>
                    <div>
                      <h3 className="font-bold text-base text-foreground leading-tight">
                        {previewModal.name}'s Resume
                      </h3>
                      <p className="text-xs text-muted-foreground font-mono truncate max-w-sm">
                        {previewModal.filename}
                      </p>
                    </div>
                  </div>

                  <div className="flex items-center gap-2">
                    <a
                      href={previewModal.url}
                      download={previewModal.filename}
                      className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-surface-2 hover:bg-muted text-xs font-semibold text-foreground border border-border transition-colors"
                    >
                      <Download className="w-4 h-4 text-primary" />
                      <span>Download</span>
                    </a>
                    <button
                      onClick={() => window.open(previewModal.url, '_blank')}
                      className="inline-flex items-center gap-1 px-3 py-1.5 rounded-xl bg-surface-2 hover:bg-muted text-xs font-semibold text-foreground border border-border transition-colors"
                    >
                      <span>New Tab</span>
                    </button>
                    <button
                      onClick={handleClosePreviewModal}
                      className="p-1.5 rounded-xl text-muted-foreground hover:text-foreground hover:bg-surface-2 transition-colors ml-1"
                    >
                      <X className="w-5 h-5" />
                    </button>
                  </div>
                </div>

                {/* Modal Body */}
                <div className="flex-1 bg-surface-2/40 p-4 flex flex-col">
                  {previewModal.isPdf ? (
                    <iframe
                      src={previewModal.url}
                      title="Candidate Resume"
                      className="w-full flex-1 rounded-xl border border-border bg-white shadow-inner"
                    />
                  ) : (
                    <div className="flex-1 flex flex-col items-center justify-center text-center p-8 space-y-4 rounded-xl border border-dashed border-border bg-surface">
                      <div className="w-16 h-16 rounded-2xl bg-blue-500/10 text-blue-400 flex items-center justify-center">
                        <FileText className="w-8 h-8" />
                      </div>
                      <div className="space-y-1 max-w-sm">
                        <h4 className="text-base font-bold text-foreground">Document Ready for Download</h4>
                        <p className="text-xs text-muted-foreground">
                          This resume is a Microsoft Word document (.docx). Download it below to review full candidate credentials.
                        </p>
                      </div>
                      <a
                        href={previewModal.url}
                        download={previewModal.filename}
                        className="inline-flex items-center gap-2 px-5 py-2.5 rounded-xl bg-gradient-to-r from-[#C63FC5] to-[#F56681] text-white font-bold text-sm shadow-md hover:opacity-95 transition-opacity"
                      >
                        <Download className="w-4 h-4" />
                        <span>Download {previewModal.filename}</span>
                      </a>
                    </div>
                  )}
                </div>
              </div>
            </div>
          )}
        </motion.div>
      </div>
    </AnimatePresence>
  );
};
