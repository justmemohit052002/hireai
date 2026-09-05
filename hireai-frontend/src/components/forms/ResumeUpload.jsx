import React, { useState, useEffect, useRef } from 'react';
import {
  Upload,
  FileText,
  CheckCircle2,
  X,
  Sparkles,
  Loader2,
  Eye,
  RefreshCw,
  Trash2,
  ExternalLink,
  Download
} from 'lucide-react';
import { Button } from '@/components/ui/Button';
import { candidateApi } from '@/services/api/candidate.api';

const formatFileSize = (bytes) => {
  if (!bytes || isNaN(bytes)) return 'Unknown size';
  const kb = bytes / 1024;
  if (kb < 1024) return `${kb.toFixed(1)} KB`;
  return `${(kb / 1024).toFixed(2)} MB`;
};

export const ResumeUpload = ({
  onFileSelect,
  initialFile = null,
  autoUpload = false,
  fetchRemoteOnMount = true,
}) => {
  const [file, setFile] = useState(initialFile);
  const [isUploading, setIsUploading] = useState(false);
  const [isViewing, setIsViewing] = useState(false);
  const [isDeleting, setIsDeleting] = useState(false);
  const [parsedData, setParsedData] = useState(null);
  const [error, setError] = useState('');
  const [isFetchingInitial, setIsFetchingInitial] = useState(false);
  const fileInputRef = useRef(null);

  // Sync initialFile if passed as prop
  useEffect(() => {
    if (initialFile) {
      setFile(initialFile);
    }
  }, [initialFile]);

  // Fetch candidate's existing resume on mount
  useEffect(() => {
    let isMounted = true;
    async function loadExistingResume() {
      if (!fetchRemoteOnMount || initialFile) return;
      setIsFetchingInitial(true);
      try {
        const remoteResume = await candidateApi.getMyResume();
        if (isMounted && remoteResume && (remoteResume.id || remoteResume.originalFileName)) {
          const formatted = {
            name: remoteResume.originalFileName || 'Resume.pdf',
            size: formatFileSize(remoteResume.fileSize),
            isRemote: true,
            id: remoteResume.id,
            resumeStatus: remoteResume.resumeStatus || 'PARSED',
            parsedRole: remoteResume.parsedRole,
            parsedDomain: remoteResume.parsedDomain,
            parsedExperience: remoteResume.parsedExperience,
            parsedDataJson: remoteResume.parsedDataJson,
          };
          setFile(formatted);
          if (onFileSelect) {
            onFileSelect(formatted);
          }
        }
      } catch (err) {
        // Resume not yet uploaded (404/empty) - expected for new profiles
        console.debug('No existing resume found on backend:', err.message);
      } finally {
        if (isMounted) setIsFetchingInitial(false);
      }
    }

    loadExistingResume();
    return () => {
      isMounted = false;
    };
  }, [fetchRemoteOnMount]);

  const handleFileChange = async (e) => {
    const selected = e.target.files?.[0];
    if (!selected) return;

    setError('');
    const fileObj = {
      name: selected.name,
      size: `${(selected.size / (1024 * 1024)).toFixed(2)} MB`,
      raw: selected,
      isRemote: false,
    };

    setFile(fileObj);
    if (onFileSelect) {
      onFileSelect(fileObj);
    }

    if (autoUpload) {
      setIsUploading(true);
      try {
        const result = await candidateApi.uploadResume(selected);
        setParsedData(result);
        const updated = {
          ...fileObj,
          isRemote: true,
          id: result?.id,
          name: result?.originalFileName || selected.name,
          resumeStatus: result?.resumeStatus || 'PARSED',
        };
        setFile(updated);
        if (onFileSelect) {
          onFileSelect(updated);
        }
      } catch (err) {
        setError(err.message || 'Failed to upload and parse resume.');
      } finally {
        setIsUploading(false);
      }
    }
  };

  const handleViewResume = async () => {
    if (!file) return;
    setError('');

    // If local file object is held in memory
    if (file.raw) {
      const localUrl = URL.createObjectURL(file.raw);
      window.open(localUrl, '_blank');
      return;
    }

    // If already uploaded on the server
    if (file.isRemote) {
      setIsViewing(true);
      try {
        const blob = await candidateApi.downloadResume();
        if (blob instanceof Blob) {
          // If the backend returned a binary octet/pdf stream
          const blobType = file.name.toLowerCase().endsWith('.pdf') ? 'application/pdf' : 'application/octet-stream';
          const fileBlob = new Blob([blob], { type: blobType });
          const blobUrl = URL.createObjectURL(fileBlob);
          window.open(blobUrl, '_blank');
        } else {
          // If download url is provided in response
          window.open('/api/v1/candidate/resume/download', '_blank');
        }
      } catch (err) {
        setError(err.message || 'Could not open resume preview.');
      } finally {
        setIsViewing(false);
      }
    }
  };

  const handleRemove = async () => {
    if (file?.isRemote) {
      setIsDeleting(true);
      try {
        await candidateApi.deleteResume();
      } catch (err) {
        console.warn('Could not delete remote resume:', err.message);
      } finally {
        setIsDeleting(false);
      }
    }

    setFile(null);
    setParsedData(null);
    setError('');
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
    if (onFileSelect) {
      onFileSelect(null);
    }
  };

  const triggerUploadClick = () => {
    if (fileInputRef.current) {
      fileInputRef.current.click();
    }
  };

  if (isFetchingInitial) {
    return (
      <div className="flex items-center justify-center p-6 rounded-2xl bg-surface-2/60 border border-border/60">
        <Loader2 className="w-5 h-5 text-primary animate-spin mr-2" />
        <span className="text-xs text-muted-foreground">Checking existing resume...</span>
      </div>
    );
  }

  return (
    <div className="w-full space-y-2">
      {file ? (
        <div className="flex flex-col gap-3 p-4 rounded-2xl bg-surface-2 border border-border/70 shadow-sm transition-all">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
            <div className="flex items-center gap-3.5 min-w-0">
              <div className="w-11 h-11 rounded-xl bg-gradient-to-br from-blue-500/20 to-indigo-500/20 border border-blue-500/30 text-blue-400 flex items-center justify-center shrink-0">
                {isUploading ? (
                  <Loader2 className="w-5 h-5 animate-spin" />
                ) : (
                  <FileText className="w-5 h-5" />
                )}
              </div>
              <div className="min-w-0 space-y-0.5">
                <p className="text-xs sm:text-sm font-bold text-foreground truncate">
                  {file.name || 'Candidate_Resume.pdf'}
                </p>
                <div className="flex flex-wrap items-center gap-2 text-[11px] text-muted-foreground">
                  <span>{file.size}</span>
                  <span>•</span>
                  <span className="inline-flex items-center gap-1 font-semibold text-emerald-400">
                    <CheckCircle2 className="w-3 h-3" />
                    {isUploading ? 'AI Engine parsing...' : 'Attached & Verified'}
                  </span>
                </div>
              </div>
            </div>

            {/* Action Buttons: View, Replace, Delete */}
            <div className="flex items-center gap-2 self-end sm:self-center shrink-0">
              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={handleViewResume}
                disabled={isViewing || isUploading}
                className="h-8 px-3 text-xs font-semibold gap-1.5"
              >
                {isViewing ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : <Eye className="w-3.5 h-3.5 text-primary" />}
                <span>View Resume</span>
              </Button>

              <Button
                type="button"
                variant="ghost"
                size="sm"
                onClick={triggerUploadClick}
                disabled={isUploading}
                title="Replace with a new file"
                className="h-8 px-2.5 text-xs text-muted-foreground hover:text-foreground"
              >
                <RefreshCw className="w-3.5 h-3.5" />
              </Button>

              <button
                type="button"
                onClick={handleRemove}
                disabled={isDeleting || isUploading}
                title="Remove resume"
                className="p-1.5 rounded-lg text-muted-foreground hover:text-red-400 hover:bg-red-500/10 transition-colors"
              >
                {isDeleting ? <Loader2 className="w-4 h-4 animate-spin" /> : <Trash2 className="w-4 h-4" />}
              </button>
            </div>
          </div>

          {/* AI Extracted Highlights (if present) */}
          {(parsedData || file.parsedRole || file.parsedExperience) && (
            <div className="p-3 rounded-xl bg-blue-500/5 border border-blue-500/20 text-xs space-y-1.5 mt-1">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-1.5 font-bold text-blue-400">
                  <Sparkles className="w-3.5 h-3.5" /> AI Parsed Profile Signals
                </div>
                {file.parsedExperience && (
                  <span className="text-[11px] font-mono text-muted-foreground">
                    {file.parsedExperience} Yrs Exp.
                  </span>
                )}
              </div>
              {file.parsedRole && (
                <p className="text-[11px] text-foreground font-medium">
                  Primary Domain / Role: <span className="text-primary font-bold">{file.parsedRole}</span>
                </p>
              )}
            </div>
          )}
        </div>
      ) : (
        <label className="flex flex-col items-center justify-center p-6 rounded-2xl border-2 border-dashed border-border/80 hover:border-primary/50 bg-background/50 hover:bg-surface-2/40 transition-colors cursor-pointer text-center">
          <Upload className="w-8 h-8 text-primary mb-2" />
          <span className="text-xs font-bold text-foreground">Click to upload or drag & drop</span>
          <span className="text-[10px] text-muted-foreground mt-0.5">PDF or DOCX up to 10MB</span>
        </label>
      )}

      {/* Hidden File Input */}
      <input
        ref={fileInputRef}
        type="file"
        accept=".pdf,.docx,application/pdf,application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        onChange={handleFileChange}
        className="hidden"
      />

      {error && <p className="text-xs text-red-500 font-medium">{error}</p>}
    </div>
  );
};
