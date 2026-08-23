import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { EnrollmentService } from '../../../core/services/enrollment.service';
import { TokenService } from '../../../core/services/token.service';
import { EnrollmentResponse } from '../../../core/models/enrollment.model';

interface QuickAction { label: string; desc: string; icon: string; link: string; }

@Component({
  selector: 'app-student-dashboard',
  standalone: true,
  imports: [RouterLink, MatCardModule, MatButtonModule, MatIconModule, MatProgressSpinnerModule],
  template: `
    <header class="hero">
      <div class="hero-inner">
        <span class="hero-badge"><img src="/tirdo-logo.png" alt="TIRDO" /></span>
        <div class="hero-copy">
          <p class="eyebrow">TIRDO Training Hub</p>
          <h1>Karibu, {{ username() }}</h1>
          <p class="lede">National Industrial Skills &amp; Innovation Training Hub</p>
          <div class="roles">
            @for (r of roles(); track r) { <span class="role-chip">{{ prettyRole(r) }}</span> }
          </div>
        </div>
      </div>
    </header>

    <div class="page">
      @if (isStaff()) {
        <section>
          <h2 class="section-title">Administration</h2>
          <div class="action-grid">
            @for (a of adminActions; track a.link) {
              <a class="action surface-card" [routerLink]="a.link">
                <span class="ic"><mat-icon>{{ a.icon }}</mat-icon></span>
                <span class="label">{{ a.label }}</span>
                <span class="desc muted">{{ a.desc }}</span>
              </a>
            }
          </div>
        </section>
      }

      <section>
        <h2 class="section-title">Quick links</h2>
        <div class="action-grid">
          @for (a of generalActions(); track a.link) {
            <a class="action surface-card" [routerLink]="a.link">
              <span class="ic"><mat-icon>{{ a.icon }}</mat-icon></span>
              <span class="label">{{ a.label }}</span>
              <span class="desc muted">{{ a.desc }}</span>
            </a>
          }
        </div>
      </section>

      @if (isStudent()) {
      <section>
        <h2 class="section-title">My learning</h2>
        @if (loading()) {
          <div class="center"><mat-spinner diameter="32" /></div>
        } @else if (enrollments().length === 0) {
          <div class="surface-card empty">
            <mat-icon>school</mat-icon>
            <p class="muted">You are not enrolled in any courses yet.</p>
            <a mat-flat-button color="primary" routerLink="/catalog">Browse the catalogue</a>
          </div>
        } @else {
          <div class="card-grid">
            @for (e of enrollments(); track e.uuid) {
              <mat-card class="surface-card enrol">
                <span class="chip" [class.accent]="e.status !== 'ACTIVE' && e.status !== 'COMPLETED'">
                  {{ statusLabel(e.status) }}
                </span>
                <p class="course-ref muted">Course {{ e.courseUuid.slice(0, 8) }}…</p>
                <div class="row foot">
                  <span class="spacer"></span>
                  @if (e.status === 'ACTIVE' || e.status === 'COMPLETED') {
                    <a mat-flat-button color="primary" [routerLink]="['/learn', e.courseUuid]">
                      {{ e.status === 'COMPLETED' ? 'Review' : 'Continue' }}
                    </a>
                  } @else {
                    <a mat-button color="primary" [routerLink]="['/catalog', e.courseUuid]">View</a>
                  }
                </div>
              </mat-card>
            }
          </div>
        }
      </section>
      }
    </div>
  `,
  styles: [`
    .hero {
      background:
        radial-gradient(1200px 260px at 12% -40%, rgba(230,163,0,0.18), transparent 60%),
        linear-gradient(135deg, var(--teltp-brand-dark), var(--teltp-brand));
      color: #fff;
      padding: 40px 20px 46px;
    }
    .hero-inner { max-width: var(--teltp-maxw); margin: 0 auto; display: flex; align-items: center; gap: 22px; }
    .hero-badge {
      flex: none; display: inline-flex; align-items: center; justify-content: center;
      background: #fff; border-radius: 16px; padding: 12px; box-shadow: 0 8px 24px rgba(0,0,0,0.25);
    }
    .hero-badge img { display: block; height: 68px; width: auto; }
    .hero-copy { min-width: 0; }
    .eyebrow { margin: 0 0 2px; font-size: 0.72rem; letter-spacing: 0.18em; text-transform: uppercase; color: var(--teltp-accent); font-weight: 700; }
    .hero h1 { margin: 0; font-family: 'Spectral', Georgia, serif; font-size: 2.1rem; line-height: 1.1; }
    .lede { margin: 6px 0 0; opacity: 0.85; }
    .roles { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 12px; }
    .role-chip {
      font-size: 0.72rem; font-weight: 600; letter-spacing: 0.03em;
      background: rgba(255,255,255,0.14); border: 1px solid rgba(255,255,255,0.28);
      padding: 3px 10px; border-radius: 999px;
    }

    section { margin-top: 28px; }
    .section-title { font-family: 'Spectral', Georgia, serif; font-size: 1.3rem; margin: 0 0 14px; }

    .action-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(220px, 1fr)); gap: 14px; }
    .action {
      display: flex; flex-direction: column; gap: 6px; padding: 18px; text-decoration: none; color: inherit;
      border-top: 3px solid transparent; transition: border-color 0.15s, transform 0.12s, box-shadow 0.15s;
    }
    .action:hover { border-top-color: var(--teltp-accent); transform: translateY(-2px); box-shadow: 0 8px 22px rgba(16,49,95,0.12); }
    .action .ic {
      width: 42px; height: 42px; border-radius: 11px; display: inline-flex; align-items: center; justify-content: center;
      background: rgba(26,77,152,0.10); color: var(--teltp-brand); margin-bottom: 4px;
    }
    .action .label { font-weight: 700; font-size: 1.02rem; }
    .action .desc { font-size: 0.85rem; }

    .center { display: flex; justify-content: center; padding: 40px; }
    .empty { text-align: center; padding: 40px; display: flex; flex-direction: column; align-items: center; gap: 12px; }
    .empty mat-icon { font-size: 40px; height: 40px; width: 40px; color: var(--teltp-muted); }
    .enrol { display: flex; flex-direction: column; gap: 10px; }
    .course-ref { font-size: 0.85rem; font-variant-numeric: tabular-nums; }
    .foot { margin-top: auto; }

    @media (max-width: 620px) {
      .hero-inner { flex-direction: column; align-items: flex-start; gap: 16px; }
      .hero h1 { font-size: 1.7rem; }
    }
  `],
})
export class StudentDashboardComponent {
  private readonly enrollment = inject(EnrollmentService);
  private readonly tokens = inject(TokenService);

  readonly username = this.tokens.username;
  readonly roles = this.tokens.roles;
  readonly isStaff = computed(() => this.tokens.hasAnyRole(['ADMIN', 'INSTRUCTOR'] as never));
  readonly isStudent = computed(() => this.tokens.hasAnyRole(['STUDENT'] as never));

  readonly loading = signal(true);
  readonly enrollments = signal<EnrollmentResponse[]>([]);

  readonly adminActions: QuickAction[] = [
    { label: 'Manage Courses', desc: 'Author and publish courses', icon: 'menu_book', link: '/admin/courses' },
    { label: 'Assessment Authoring', desc: 'Build quizzes and exams', icon: 'edit_note', link: '/admin/assessments' },
    { label: 'Assessment Grading', desc: 'Grade written answers', icon: 'grading', link: '/admin/grading' },
    { label: 'Issue Certificates', desc: 'Award learner credentials', icon: 'workspace_premium', link: '/admin/certificates' },
    { label: 'System Users', desc: 'Manage accounts and roles', icon: 'manage_accounts', link: '/admin/users' },
    { label: 'Enrolments & Cohorts', desc: 'Cohorts and bulk enrolment', icon: 'groups', link: '/admin/cohorts' },
    { label: 'Organizations', desc: 'Corporate and institutional clients', icon: 'domain', link: '/admin/organizations' },
    { label: 'Billing & Payments', desc: 'Invoices and payment management', icon: 'receipt_long', link: '/admin/billing' },
  ];
  private readonly allGeneral: (QuickAction & { student?: boolean })[] = [
    { label: 'Course Catalogue', desc: 'Browse available training', icon: 'grid_view', link: '/catalog' },
    { label: 'My Results', desc: 'Your assessment attempts', icon: 'assignment_turned_in', link: '/results', student: true },
    { label: 'My Certificates', desc: 'View and download credentials', icon: 'verified', link: '/certificates', student: true },
  ];
  readonly generalActions = computed<QuickAction[]>(() =>
    this.allGeneral.filter((a) => !a.student || this.isStudent()));

  constructor() {
    if (this.isStudent()) {
      this.enrollment.myEnrollments().subscribe({
        next: (p) => { this.enrollments.set(p.content); this.loading.set(false); },
        error: () => this.loading.set(false),
      });
    } else {
      this.loading.set(false);
    }
  }

  prettyRole(r: string): string {
    return r.replace('_', ' ').toLowerCase().replace(/\b\w/g, (c) => c.toUpperCase());
  }

  statusLabel(s: string): string {
    return {
      PENDING_PAYMENT: 'Payment due', ACTIVE: 'Active', WAITLISTED: 'Waitlisted',
      COMPLETED: 'Completed', CANCELLED: 'Cancelled',
    }[s] ?? s;
  }
}
