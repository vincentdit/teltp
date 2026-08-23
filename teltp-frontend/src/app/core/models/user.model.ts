export type RoleName = 'ADMIN' | 'INSTRUCTOR' | 'STUDENT' | 'FINANCE_OFFICER' | 'CORPORATE_CLIENT';

export interface UserResponse {
  uuid: string;
  username: string;
  email: string;
  fullName?: string;
  profession?: string;
  organizationUuid?: string;
  active: boolean;
  roles: RoleName[];
}

export interface AssignRolesRequest {
  userUuid: string;
  roles: RoleName[];
}

export const ALL_ROLES: RoleName[] = ['ADMIN', 'INSTRUCTOR', 'STUDENT', 'FINANCE_OFFICER', 'CORPORATE_CLIENT'];

export interface CreateUserRequest {
  username: string;
  email: string;
  password: string;
  firstName?: string;
  lastName?: string;
  phoneNumber?: string;
  profession?: string;
  dataProcessingConsent: boolean;
}
