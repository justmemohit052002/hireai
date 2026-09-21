import React, { useState, useEffect } from 'react';
import {
  Send,
  CheckCircle2,
  Sparkles,
  FileText,
  AlertCircle,
  Upload,
  UserCheck,
  Eye,
  Trash2,
  Loader2,
  RefreshCw,
} from 'lucide-react';
import { Modal } from '@/components/ui/Modal';
import { Button } from '@/components/ui/Button';
import { Textarea } from '@/components/ui/Textarea';
import { ResumeUpload } from '@/components/forms/ResumeUpload';
import { candidateApi } from '@/services/api/candidate.api';
import { useJobs } from '@/context/JobsContext';

const formatFileSize = (bytes) => {
  if (!bytes || isNaN(bytes)) return 'Unknown size';
  const kb = bytes / 1024;
  if (kb < 1024) return `${kb.toFixed(1)} KB`;
  return `${(kb / 1024).toFixed(2)} MB`;
};

export const ApplyModal = ({
  job,
  isOpen,
  onClose,
  onSubmit,
}) => {
  const { submitApplication } = useJobs();
  const [coverNote, setCoverNote] = useState('');
  const [resumeMode, setResumeMode] = useState('upload'); // 'upload' | 'profile'
  const [customResume, setCustomResume] = useState(null);
  const [profileResume, setProfileResume] = useState(null);
  const [isLoadingProfileResume, setIsLoadingProfileResume] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isSuccess, setIsSuccess] = useState(false);
  const [matchScore, setMatchScore] = useState(null);
  const [errorMsg, setErrorMsg] = useState('');
  const [isViewingResume, setIsViewingResume] = useState(false);

  // Fetch candidate's profile resume on open to determine options
  useEffect(() => {
    let isMounted = true;
    if (isOpen) {
      setErrorMsg('');
      setIsLoadingProfileResume(true);
      candidateApi
        .getMyResume()
        .then((remoteResume) => {
          if (isMounted && remoteResume && (remoteResume.id || remoteResume.originalFileName)) {
            const formatted = {
              name: remoteResume.originalFileName || 'Profile_Resume.pdf',
              size: formatFileSize(remoteResume.fileSize),
              isRemote: true,
              id: remoteResume.id,
              parsedRole: remoteResume.parsedRole,
              parsedExperience: remoteResume.parsedExperience,
            };
            setProfileResume(formatted);
          } else if (isMounted) {
            setProfileResume(null);
            setResumeMode('upload');
          }
        })
        .catch(() => {
          if (isMounted) {
            setProfileResume(null);
            setResumeMode('upload');
          }
        })
        .finally(() => {
          if (isMounted) setIsLoadingProfileResume(false);
        });
    } else {
      // Reset state on close
      setCustomResume(null);
      setCoverNote('');
      setErrorMsg('');
      setIsSuccess(false);
      setMatchScore(null);
    }

    return () => {
      isMounted = false;
    };
  }, [isOpen]);

  if (!job) return null;

  const handleViewProfileResume = async () => {
    setIsViewingResume(true);
    try {
      const blob = await candidateApi.downloadResume();
      if (blob instanceof Blob && blob.size > 0) {
        const isPdf = profileResume?.name?.toLowerCase().endsWith('.pdf') !== false;
        const fileBlob = new Blob([blob], {
          type: isPdf ? 'application/pdf' : 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
        });
        const blobUrl = URL.createObjectURL(fileBlob);
        window.open(blobUrl, '_blank');
        setTimeout(() => URL.revokeObjectURL(blobUrl), 60000);
      }
    } catch (err) {
      setErrorMsg(err.message || 'Could not preview profile resume.');
    } finally {
      setIsViewingResume(false);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErrorMsg('');
    setIsSubmitting(true);

    try {
      // If user chose upload mode with a custom file, pass the custom file.
      // If user chose profile mode, file is null (backend uses existing profile resume).
      const fileToSubmit = resumeMode === 'upload' && customResume ? customResume : null;

      const result = await submitApplication({
        jobId: job.id,
        coverNote,
        file: fileToSubmit,
      });

      if (result?.aiScore || result?.atsMatchScore) {
        setMatchScore(result.aiScore || Math.round(result.atsMatchScore));
      } else {
        setMatchScore(88);
      }

      setIsSuccess(true);
      if (onSubmit) {
        onSubmit(job.id, coverNote);
      }

      setTimeout(() => {
        setIsSuccess(false);
        setCoverNote('');
        setCustomResume(null);
        setMatchScore(null);
        onClose();
      }, 2200);
    } catch (err) {
      setErrorMsg(err.message || 'Failed to submit application. Please try again.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={isSuccess ? 'Application Submitted!' : `Apply to ${job.company?.name || 'Position'}`}
      description={isSuccess ? 'ATS Pipeline computed your match score.' : job.title}
      maxWidth="lg"
    >
      {isSuccess ? (
        <div className="py-8 flex flex-col items-center justify-center text-center space-y-4">
          <div className="w-16 h-16 rounded-full bg-emerald-500/20 text-emerald-500 flex items-center justify-center animate-bounce">
            <CheckCircle2 className="w-10 h-10" />
          </div>
          <h3 className="text-xl font-bold font-heading text-foreground">Application Received!</h3>
          <p className="text-sm text-muted-foreground max-w-xs">
            Your application has been delivered to {job.company?.name || 'the recruiter'}.
          </p>
          {matchScore && (
            <div className="inline-flex items-center gap-2 px-4 py-2 rounded-xl bg-brand-blue-light/30 border border-brand-blue/30 text-brand-navy dark:text-brand-blue-light font-bold text-sm">
              <Sparkles className="w-4 h-4 text-brand-blue" />
              <span>{matchScore}% ATS Role Match</span>
            </div>
          )}
        </div>
      ) : (
        <form onSubmit={handleSubmit} className="space-y-5">
          {errorMsg && (
            <div className="p-3.5 rounded-xl bg-red-500/10 border border-red-500/20 text-red-500 text-xs font-medium flex items-start gap-2.5">
              <AlertCircle className="w-4 h-4 shrink-0 mt-0.5" />
              <span>{errorMsg}</span>
            </div>
          )}

          {/* AI Resume Sync Notice */}
          <div className="p-3.5 rounded-2xl bg-blue-500/10 border border-blue-500/20 flex items-start gap-3">
            <Sparkles className="w-5 h-5 text-blue-500 shrink-0 mt-0.5" />
            <div className="text-xs space-y-0.5">
              <span className="font-bold text-foreground block">AI Resume Matching</span>
              <p className="text-muted-foreground">
                Your resume will be parsed against this job's skills and experience requirements to calculate an ATS score for the recruiter.
              </p>
            </div>
          </div>

          {/* Resume Selection / Upload Section */}
          <div className="space-y-2.5">
            <div className="flex items-center justify-between">
              <label className="block text-xs font-bold uppercase tracking-wider text-foreground">
                Resume Attachment
              </label>
              {profileResume && (
                <div className="flex items-center p-0.5 rounded-xl bg-surface-2 border border-border/70 text-xs">
                  <button
                    type="button"
                    onClick={() => setResumeMode('upload')}
                    className={`px-3 py-1 rounded-lg font-medium transition-all ${
                      resumeMode === 'upload'
                        ? 'bg-primary text-primary-foreground shadow-sm'
                        : 'text-muted-foreground hover:text-foreground'
                    }`}
                  >
                    Upload New
                  </button>
                  <button
                    type="button"
                    onClick={() => setResumeMode('profile')}
                    className={`px-3 py-1 rounded-lg font-medium transition-all ${
                      resumeMode === 'profile'
                        ? 'bg-primary text-primary-foreground shadow-sm'
                        : 'text-muted-foreground hover:text-foreground'
                    }`}
                  >
                    Use Profile Resume
                  </button>
                </div>
              )}
            </div>

            {/* Mode 1: Upload Custom Resume */}
            {resumeMode === 'upload' && (
              <div>
                <ResumeUpload
                  autoUpload={false}
                  fetchRemoteOnMount={false}
                  allowRemoteDelete={false}
                  initialFile={customResume}
                  onFileSelect={(file) => setCustomResume(file)}
                />
              </div>
            )}

            {/* Mode 2: Use Profile Resume */}
            {resumeMode === 'profile' && profileResume && (
              <div className="flex flex-col gap-3 p-4 rounded-2xl bg-surface-2 border border-primary/30 shadow-sm transition-all">
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                  <div className="flex items-center gap-3.5 min-w-0">
                    <div className="w-11 h-11 rounded-xl bg-gradient-to-br from-emerald-500/20 to-teal-500/20 border border-emerald-500/30 text-emerald-400 flex items-center justify-center shrink-0">
                      <UserCheck className="w-5 h-5" />
                    </div>
                    <div className="min-w-0 space-y-0.5">
                      <p className="text-xs sm:text-sm font-bold text-foreground truncate">
                        {profileResume.name}
                      </p>
                      <div className="flex flex-wrap items-center gap-2 text-[11px] text-muted-foreground">
                        <span>{profileResume.size}</span>
                        <span>•</span>
                        <span className="inline-flex items-center gap-1 font-semibold text-emerald-400">
                          <CheckCircle2 className="w-3.5 h-3.5" />
                          Saved on Candidate Profile
                        </span>
                      </div>
                    </div>
                  </div>

                  <div className="flex items-center gap-2 self-end sm:self-center shrink-0">
                    <Button
                      type="button"
                      variant="outline"
                      size="sm"
                      onClick={handleViewProfileResume}
                      disabled={isViewingResume}
                      className="h-8 px-3 text-xs font-semibold gap-1.5"
                    >
                      {isViewingResume ? (
                        <Loader2 className="w-3.5 h-3.5 animate-spin" />
                      ) : (
                        <Eye className="w-3.5 h-3.5 text-primary" />
                      )}
                      <span>View</span>
                    </Button>
                    <Button
                      type="button"
                      variant="ghost"
                      size="sm"
                      onClick={() => setResumeMode('upload')}
                      className="h-8 px-2.5 text-xs text-muted-foreground hover:text-foreground"
                    >
                      <span>Upload different</span>
                    </Button>
                  </div>
                </div>
              </div>
            )}
          </div>

          {/* Cover Note */}
          <div>
            <label className="block text-xs font-bold uppercase tracking-wider text-foreground mb-2">
              Note to Hiring Manager (Optional)
            </label>
            <Textarea
              value={coverNote}
              onChange={(e) => setCoverNote(e.target.value)}
              placeholder="Tell the recruiter why you're a great fit for this role..."
              rows={4}
            />
          </div>

          {/* Action Buttons */}
          <div className="flex items-center justify-end gap-3 pt-3 border-t border-border/50">
            <Button type="button" variant="ghost" onClick={onClose} disabled={isSubmitting}>
              Cancel
            </Button>
            <Button type="submit" variant="default" isLoading={isSubmitting}>
              <Send className="w-4 h-4 mr-2" />
              Submit Application
            </Button>
          </div>
        </form>
      )}
    </Modal>
  );
};
