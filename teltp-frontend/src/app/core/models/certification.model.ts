export interface IssueCertificateRequest {
  studentUuid: string;
  courseUuid: string;
  accreditingBody?: string;
  accreditationLevel?: string;
  expiresOn?: string; // ISO date (yyyy-MM-dd)
}

export interface CertificateResponse {
  uuid: string;
  referenceNumber: string;
  verificationCode: string;
  recipientName: string;
  courseTitle: string;
  issuedOn: string;
  expiresOn?: string;
  revoked: boolean;
  accreditingBody?: string;
  accreditationLevel?: string;
}

export interface VerificationResult {
  valid: boolean;
  status: string;
  recipientName?: string;
  courseTitle?: string;
  issuedOn?: string;
  expiresOn?: string;
  accreditingBody?: string;
}
