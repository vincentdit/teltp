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

  { path: '**', redirectTo: 'catalog' },
];
