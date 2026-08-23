export type OrganizationType = 'GOVERNMENT' | 'INDUSTRIAL' | 'ACADEMIC' | 'DEVELOPMENT_PARTNER';

export interface OrganizationResponse {
  uuid: string;
  name: string;
  type: OrganizationType;
  subType?: OrganizationSubType;
  contactEmail?: string;
  contactPhone?: string;
  region?: string;
  district?: string;
  tin?: string;
}

export interface CreateOrganizationRequest {
  name: string;
  type: OrganizationType;
  subType?: OrganizationSubType;
  contactEmail?: string;
  contactPhone?: string;
  region?: string;
  district?: string;
  tin?: string;
}

export type OrganizationSubType =
  | 'MINISTRY' | 'AGENCY' | 'LOCAL_GOVERNMENT_AUTHORITY'
  | 'MANUFACTURER' | 'SME' | 'INDUSTRIAL_PARK'
  | 'UNIVERSITY' | 'COLLEGE' | 'TVET_INSTITUTION' | 'OTHER';

export const ORG_TYPES: OrganizationType[] = ['GOVERNMENT', 'INDUSTRIAL', 'ACADEMIC', 'DEVELOPMENT_PARTNER'];
export const ORG_SUBTYPES: OrganizationSubType[] = [
  'MINISTRY', 'AGENCY', 'LOCAL_GOVERNMENT_AUTHORITY',
  'MANUFACTURER', 'SME', 'INDUSTRIAL_PARK',
  'UNIVERSITY', 'COLLEGE', 'TVET_INSTITUTION', 'OTHER',
];
