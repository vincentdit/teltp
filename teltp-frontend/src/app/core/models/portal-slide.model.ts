export interface PortalSlide {
  uuid: string;
  title: string;
  body: string | null;
  linkUrl: string | null;
  sortOrder: number;
  active: boolean;
  hasImage: boolean;
  imageVersion?: number;
}
