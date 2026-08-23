import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'catalog' },

  {
    path: 'login',
    loadComponent: () =>
      import('./features/auth/login/login.component').then((m) => m.LoginComponent),
  },
  {
    path: 'register',
    loadComponent: () =>
      import('./features/auth/register/register.component').then((m) => m.RegisterComponent),
  },

  // Public certificate verification
  {
    path: 'verify',
    loadComponent: () =>
      import('./features/verify/verify.component').then((m) => m.VerifyComponent),
  },
  {
    path: 'verify/:code',
    loadComponent: () =>
      import('./features/verify/verify.component').then((m) => m.VerifyComponent),
  },

  // Public catalog
  {
    path: 'catalog',
    loadComponent: () =>
      import('./features/catalog/course-list/course-list.component').then((m) => m.CourseListComponent),
  },
  {
    path: 'catalog/:uuid',
    loadComponent: () =>
      import('./features/catalog/course-detail/course-detail.component').then((m) => m.CourseDetailComponent),
  },

  // Student
  {
    path: 'dashboard',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/dashboard/student-dashboard/student-dashboard.component').then((m) => m.StudentDashboardComponent),
  },
  {
    path: 'certificates',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/certificates/my-certificates.component').then((m) => m.MyCertificatesComponent),
  },
  {
    path: 'results',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/results/my-results.component').then((m) => m.MyResultsComponent),
  },
  {
    path: 'assessments/:uuid/take',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/assessment/take-assessment/take-assessment.component').then((m) => m.TakeAssessmentComponent),
  },

  // Course player (enrolled learners)
  {
    path: 'learn/:uuid',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/learn/course-player/course-player.component').then((m) => m.CoursePlayerComponent),
  },

  // Admin / instructor
  {
    path: 'admin',
    canActivate: [roleGuard],
    data: { roles: ['ADMIN', 'INSTRUCTOR'] },
    loadComponent: () =>
      import('./features/admin/admin-home/admin-home.component').then((m) => m.AdminHomeComponent),
  },
  {
    path: 'admin/courses',
    canActivate: [roleGuard],
    data: { roles: ['ADMIN', 'INSTRUCTOR'] },
    loadComponent: () =>
      import('./features/admin/course-manage/course-manage.component').then((m) => m.CourseManageComponent),
  },
  {
    path: 'admin/certificates',
    canActivate: [roleGuard],
    data: { roles: ['ADMIN', 'INSTRUCTOR'] },
    loadComponent: () =>
      import('./features/admin/issue-certificate/issue-certificate.component').then((m) => m.IssueCertificateComponent),
  },
  {
    path: 'admin/grading',
    canActivate: [roleGuard],
    data: { roles: ['ADMIN', 'INSTRUCTOR'] },
    loadComponent: () =>
      import('./features/admin/grading/grade-list.component').then((m) => m.GradeListComponent),
  },
  {
    path: 'admin/grading/:uuid',
    canActivate: [roleGuard],
    data: { roles: ['ADMIN', 'INSTRUCTOR'] },
    loadComponent: () =>
      import('./features/admin/grading/grade-attempt.component').then((m) => m.GradeAttemptComponent),
  },
  {
    path: 'admin/assessments',
    canActivate: [roleGuard],
    data: { roles: ['ADMIN', 'INSTRUCTOR'] },
    loadComponent: () =>
      import('./features/admin/assessment-manage/assessment-manage.component').then((m) => m.AssessmentManageComponent),
  },
  {
    path: 'admin/assessments/:uuid',
    canActivate: [roleGuard],
    data: { roles: ['ADMIN', 'INSTRUCTOR'] },
    loadComponent: () =>
      import('./features/admin/assessment-manage/assessment-edit.component').then((m) => m.AssessmentEditComponent),
  },
  {
    path: 'admin/users',
    canActivate: [roleGuard],
    data: { roles: ['ADMIN'] },
    loadComponent: () =>
      import('./features/admin/system-users/system-users.component').then((m) => m.SystemUsersComponent),
  },
  {
    path: 'admin/cohorts',
    canActivate: [roleGuard],
    data: { roles: ['ADMIN', 'INSTRUCTOR'] },
    loadComponent: () =>
      import('./features/admin/cohorts/cohorts-manage.component').then((m) => m.CohortsManageComponent),
  },
  {
    path: 'admin/reporting',
    canActivate: [roleGuard],
    data: { roles: ['ADMIN', 'FINANCE_OFFICER'] },
    loadComponent: () =>
      import('./features/admin/reporting/reporting-dashboard.component').then((m) => m.ReportingDashboardComponent),
  },
  {
    path: 'admin/organizations',
    canActivate: [roleGuard],
    data: { roles: ['ADMIN'] },
    loadComponent: () =>
      import('./features/admin/organizations/organizations-manage.component').then((m) => m.OrganizationsManageComponent),
  },
  {
    path: 'pay/:courseUuid',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/payment/payment.component').then((m) => m.PaymentComponent),
  },
  {
    path: 'admin/billing',
    canActivate: [roleGuard],
    data: { roles: ['ADMIN', 'FINANCE_OFFICER'] },
    loadComponent: () =>
      import('./features/admin/billing/billing-admin.component').then((m) => m.BillingAdminComponent),
  },

  { path: '**', redirectTo: 'catalog' },
];
