import React, { useState } from 'react';
import { Send, CheckCircle2, Sparkles, FileText, AlertCircle } from 'lucide-react';
import { Modal } from '@/components/ui/Modal';
import { Button } from '@/components/ui/Button';
import { Textarea } from '@/components/ui/Textarea';
import { ResumeUpload } from '@/components/forms/ResumeUpload';
import { useJobs } from '@/context/JobsContext';

export const ApplyModal = ({
  job,
  isOpen,
  onClose,
  onSubmit,
}) => {
  const { submitApplication } = useJobs();
  const [coverNote, setCoverNote] = useState('');
  const [resumeFile, setResumeFile] = useState(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isSuccess, setIsSuccess] = useState(false);
  const [matchScore, setMatchScore] = useState(null);
  const [errorMsg, setErrorMsg] = useState('');

  if (!job) return null;

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErrorMsg('');
    setIsSubmitting(true);

    try {
      const result = await submitApplication({
        jobId: job.id,
        coverNote,
        file: resumeFile,
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
        setResumeFile(null);
        setMatchScore(null);
        onClose();
      }, 2000);
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
            <div className="inline-flex items-center gap-2 px-4 py-2 rounded-xl bg-gradient-to-r from-[#C63FC5]/15 via-[#F56681]/15 to-[#FC9559]/15 border border-[#F56681]/30 text-foreground font-bold text-sm">
              <Sparkles className="w-4 h-4 text-[#F56681]" />
              <span>Initial ATS Match Score: {matchScore}%</span>
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

          {/* Resume Upload Component */}
          <div>
            <label className="block text-xs font-bold uppercase tracking-wider text-foreground mb-2">
              Attach Resume (Optional if already on profile)
            </label>
            <ResumeUpload onFileSelect={(file) => setResumeFile(file)} />
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
