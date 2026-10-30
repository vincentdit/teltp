export type RoleName =
  | 'ADMIN' | 'INSTRUCTOR' | 'STUDENT' | 'CORPORATE_CLIENT' | 'FINANCE_OFFICER';

export interface LoginRequest { username: string; password: string; }

export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
  firstName?: string;
  lastName?: string;
  phoneNumber?: string;
  profession?: string;
  dataProcessingConsent: boolean;
}

export interface TokenResponse {
  accessToken: string | null;
  refreshToken: string | null;
  tokenType: string;
  expiresIn: number;
  mfaRequired?: boolean;
  mfaChallenge?: string | null;
}

export interface UserResponse {
  uuid: string;
  username: string;
  email: string;
  fullName: string;
  profession?: string;
  organizationUuid?: string;
  active: boolean;
  roles: RoleName[];
}
