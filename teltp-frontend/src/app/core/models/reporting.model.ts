export interface PlatformKpis {
  activeLearners: number;
  publishedCourses: number;
  certificatesIssued: number;
  corporateClients: number;
  confirmedRevenue: number;
  currency: string;
}

export interface RevenueByChannel {
  channel: string;
  amount: number;
  transactions: number;
}

export interface RevenueDashboard {
  totalConfirmed: number;
  currency: string;
  byChannel: RevenueByChannel[];
}

export interface CompletionRow {
  courseUuid: string;
  courseTitle: string;
  enrolled: number;
  completed: number;
  completionRate: number;
}
export interface CompletionDashboard { rows: CompletionRow[]; }

export interface TrainerRow {
  instructorUuid: string;
  instructorName: string;
  coursesAuthored: number;
  learnersTaught: number;
}
export interface TrainerDashboard { rows: TrainerRow[]; }
