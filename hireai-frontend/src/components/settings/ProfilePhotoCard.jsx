import React, { useState, useRef } from 'react';
import { Camera, Trash2, Upload, CheckCircle2, AlertCircle, Loader2 } from 'lucide-react';
import { Card } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Avatar } from '@/components/ui/Avatar';
import { useAuth } from '@/context/AuthContext';
import { candidateApi } from '@/services/api/candidate.api';
import { recruiterApi } from '@/services/api/recruiter.api';
import { authApi } from '@/services/api/auth.api';

const ALLOWED_TYPES = ['image/jpeg', 'image/png', 'image/webp', 'image/jpg'];
const MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024; // 5MB

export const ProfilePhotoCard = () => {
  const { user, role, updateUserAvatar } = useAuth();
  const fileInputRef = useRef(null);

  const [isUploading, setIsUploading] = useState(false);
  const [isDeleting, setIsDeleting] = useState(false);
  const [successMessage, setSuccessMessage] = useState('');
  const [errorMessage, setErrorMessage] = useState('');

  const displayName =
    user?.name ||
    (user?.firstName ? `${user.firstName} ${user.lastName || ''}`.trim() : user?.email?.split('@')[0] || 'User');

  const handleFileSelect = async (e) => {
    const file = e.target.files?.[0];
    if (!file) return;

    // Reset input so re-selecting same file triggers onChange
    e.target.value = '';

    setErrorMessage('');
    setSuccessMessage('');

    // Client-side validation: Type check
    if (!ALLOWED_TYPES.includes(file.type.toLowerCase())) {
      setErrorMessage('Invalid file format. Allowed formats: JPG, PNG, and WebP.');
      return;
    }

    // Client-side validation: Size check
    if (file.size > MAX_FILE_SIZE_BYTES) {
      setErrorMessage('File size exceeds the 5MB limit. Please choose a smaller image.');
      return;
    }

    setIsUploading(true);
    try {
      let response;
      if (role === 'candidate') {
        response = await candidateApi.uploadPhoto(file);
      } else if (role === 'recruiter') {
        response = await recruiterApi.uploadPhoto(file);
      } else {
        response = await authApi.uploadAvatar(file);
      }

      const photoUrl =
        response?.profilePhotoUrl ||
        response?.avatarUrl ||
        (role === 'candidate' ? '/candidate/profile/photo' : null) ||
        (role === 'recruiter' ? '/recruiter/profile/photo' : null) ||
        '/users/me/avatar';

      const timestampedUrl = photoUrl ? `${photoUrl}?t=${Date.now()}` : null;
      updateUserAvatar(timestampedUrl);

      setSuccessMessage('Profile photo updated successfully.');
    } catch (err) {
      console.error('Photo upload failed:', err);
      setErrorMessage(err.message || 'Failed to upload profile photo. Please try again.');
    } finally {
      setIsUploading(false);
    }
  };

  const handleDeletePhoto = async () => {
    setErrorMessage('');
    setSuccessMessage('');
    setIsDeleting(true);

    try {
      if (role === 'candidate') {
        await candidateApi.deletePhoto();
      } else if (role === 'recruiter') {
        await recruiterApi.deletePhoto();
      } else {
        await authApi.deleteAvatar();
      }

      updateUserAvatar(null);
      setSuccessMessage('Profile photo removed.');
    } catch (err) {
      console.error('Photo delete failed:', err);
      setErrorMessage(err.message || 'Failed to remove profile photo.');
    } finally {
      setIsDeleting(false);
    }
  };

  return (
    <Card glass className="p-6 sm:p-8 space-y-6">
      <div className="flex items-center gap-3 pb-4 border-b border-border/50">
        <div className="w-10 h-10 rounded-xl bg-brand-blue/10 border border-brand-blue/20 flex items-center justify-center text-brand-blue shrink-0">
          <Camera className="w-5 h-5" />
        </div>
        <div>
          <h3 className="text-lg font-bold font-heading text-foreground">Profile Picture</h3>
          <p className="text-xs text-muted-foreground">
            Upload an authentic photo to enhance your profile presence across HireAI.
          </p>
        </div>
      </div>

      {successMessage && (
        <div className="p-4 rounded-xl bg-emerald-500/10 border border-emerald-500/30 text-emerald-500 dark:text-emerald-400 text-xs font-medium flex items-start gap-2.5">
          <CheckCircle2 className="w-4 h-4 shrink-0 mt-0.5" />
          <span>{successMessage}</span>
        </div>
      )}

      {errorMessage && (
        <div className="p-4 rounded-xl bg-red-500/10 border border-red-500/30 text-red-500 dark:text-red-400 text-xs font-medium flex items-start gap-2.5">
          <AlertCircle className="w-4 h-4 shrink-0 mt-0.5" />
          <span>{errorMessage}</span>
        </div>
      )}

      <div className="flex flex-col sm:flex-row items-center gap-6">
        <div className="relative group shrink-0">
          <Avatar name={displayName} src={user?.avatarUrl} size="xl" className="w-24 h-24 text-2xl border-2" />
          {isUploading && (
            <div className="absolute inset-0 rounded-full bg-background/80 flex items-center justify-center">
              <Loader2 className="w-6 h-6 text-brand-blue animate-spin" />
            </div>
          )}
        </div>

        <div className="space-y-3 text-center sm:text-left flex-1">
          <div>
            <h4 className="text-sm font-bold text-foreground">{displayName}</h4>
            <p className="text-xs text-muted-foreground capitalize">
              {role} Account • JPG, PNG or WebP up to 5MB
            </p>
          </div>

          <div className="flex flex-wrap items-center justify-center sm:justify-start gap-3">
            <input
              type="file"
              ref={fileInputRef}
              onChange={handleFileSelect}
              accept=".jpg,.jpeg,.png,.webp"
              className="hidden"
            />

            <Button
              type="button"
              onClick={() => fileInputRef.current?.click()}
              disabled={isUploading || isDeleting}
              className="rounded-xl px-4 flex items-center gap-2"
            >
              {isUploading ? (
                <>
                  <Loader2 className="w-4 h-4 animate-spin" />
                  <span>Uploading...</span>
                </>
              ) : (
                <>
                  <Upload className="w-4 h-4" />
                  <span>{user?.avatarUrl ? 'Change Photo' : 'Upload Photo'}</span>
                </>
              )}
            </Button>

            {user?.avatarUrl && (
              <Button
                type="button"
                variant="outline"
                onClick={handleDeletePhoto}
                disabled={isUploading || isDeleting}
                className="rounded-xl px-4 text-red-500 hover:text-red-600 hover:bg-red-500/10 border-red-500/30 flex items-center gap-2"
              >
                {isDeleting ? (
                  <>
                    <Loader2 className="w-4 h-4 animate-spin" />
                    <span>Removing...</span>
                  </>
                ) : (
                  <>
                    <Trash2 className="w-4 h-4" />
                    <span>Remove Photo</span>
                  </>
                )}
              </Button>
            )}
          </div>
        </div>
      </div>
    </Card>
  );
};
