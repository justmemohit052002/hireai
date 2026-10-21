import React from 'react';
import { Shield, Lock, FileText, Eye, Database, Bell } from 'lucide-react';

export const PrivacyPolicyPage = () => {
  return (
    <div className="pt-28 pb-12 sm:pt-36 sm:pb-20 max-w-4xl mx-auto px-4 sm:px-6">
      {/* Header */}
      <div className="border-b border-border/80 pb-8 mb-10">
        <div className="inline-flex items-center gap-2 px-3 py-1 rounded-md bg-brand-blue/10 text-brand-blue text-xs font-mono font-bold uppercase mb-4">
          <Shield className="w-3.5 h-3.5" />
          Data Protection and Trust
        </div>
        <h1 className="text-3xl sm:text-4xl font-extrabold font-heading text-foreground tracking-tight">
          Privacy Policy
        </h1>
        <p className="text-sm text-muted-foreground mt-2 font-sans">
          Effective Date: October 13, 2026. Last updated: October 13, 2026.
        </p>
      </div>

      {/* Main Legal Content */}
      <div className="space-y-10 text-sm text-foreground/90 font-sans leading-relaxed">
        {/* Section 1 */}
        <section className="space-y-3">
          <h2 className="text-xl font-bold font-heading text-foreground flex items-center gap-2">
            <Eye className="w-5 h-5 text-brand-blue" />
            1. Information We Collect
          </h2>
          <p className="text-muted-foreground">
            HireAI collects only the data necessary to provide automated recruitment, applicant screening, and direct communication services.
          </p>
          <ul className="list-disc pl-5 space-y-2 text-muted-foreground">
            <li>
              <strong className="text-foreground">Candidate Data:</strong> Name, contact details, uploaded resume files (.pdf, .docx), portfolio links, self-reported skills, and employment history.
            </li>
            <li>
              <strong className="text-foreground">Recruiter Data:</strong> Work email address, company name, job posting specifications, evaluation criteria, and hiring decisions.
            </li>
            <li>
              <strong className="text-foreground">Technical Metadata:</strong> IP address, device telemetry, browser information, and authenticated session tokens via secure HTTP-only cookies.
            </li>
          </ul>
        </section>

        {/* Section 2 */}
        <section className="space-y-3">
          <h2 className="text-xl font-bold font-heading text-foreground flex items-center gap-2">
            <Database className="w-5 h-5 text-brand-blue" />
            2. How We Process Resumes and Candidate Data
          </h2>
          <p className="text-muted-foreground">
            When you upload a resume to HireAI, our parsing service processes the document to extract structured skill competencies, years of experience, and role history. This data is compared deterministically and semantically against recruiter job requirements.
          </p>
          <p className="text-muted-foreground">
            We do not sell, rent, or monetize your resume or candidate profile to third-party data brokers. Candidate profiles are only accessible to verified recruiters posting jobs on the platform.
          </p>
        </section>

        {/* Section 3 */}
        <section className="space-y-3">
          <h2 className="text-xl font-bold font-heading text-foreground flex items-center gap-2">
            <Lock className="w-5 h-5 text-brand-blue" />
            3. Security and Storage Architecture
          </h2>
          <p className="text-muted-foreground">
            All user data, resumes, and communications are encrypted in transit via TLS 1.3 and encrypted at rest using AES-256. Access to backend databases is strictly segregated through role-based access control and token-based authentication.
          </p>
        </section>

        {/* Section 4 */}
        <section className="space-y-3">
          <h2 className="text-xl font-bold font-heading text-foreground flex items-center gap-2">
            <FileText className="w-5 h-5 text-brand-blue" />
            4. User Rights and Data Deletion
          </h2>
          <p className="text-muted-foreground">
            Regardless of your geographic location, you retain full ownership of your data on HireAI:
          </p>
          <ul className="list-disc pl-5 space-y-2 text-muted-foreground">
            <li>
              <strong className="text-foreground">Access and Export:</strong> You may request an export of all candidate profile information and submission history.
            </li>
            <li>
              <strong className="text-foreground">Permanent Deletion:</strong> You may request the full deletion of your account and associated resumes at any time from your settings or by contacting our security team.
            </li>
            <li>
              <strong className="text-foreground">Opt-Out:</strong> Candidates can toggle their profile visibility to private to pause recruiter discovery.
            </li>
          </ul>
        </section>

        {/* Section 5 */}
        <section className="space-y-3">
          <h2 className="text-xl font-bold font-heading text-foreground flex items-center gap-2">
            <Bell className="w-5 h-5 text-brand-blue" />
            5. Contact Information
          </h2>
          <p className="text-muted-foreground">
            If you have questions regarding this Privacy Policy or wish to exercise your data rights, please contact our Data Protection Office at:
          </p>
          <div className="p-4 rounded-xl bg-surface border border-border text-xs font-mono text-muted-foreground space-y-1">
            <p className="text-foreground font-bold">HireAI Data Governance</p>
            <p>Email: privacy@hireai.app</p>
            <p>Vionsys HireAI Engineering, Pune, India</p>
          </div>
        </section>
      </div>
    </div>
  );
};
