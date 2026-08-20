import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { EnrollmentService } from '../../../core/services/enrollment.service';
import { AuthService } from '../../../core/services/auth.service';
import { EnrollmentResponse } from '../../../core/models/enrollment.model';

@Component({
  selector: 'app-student-dashboard',
  standalone: true,
  imports: [RouterLink, MatCardModule, MatButtonModule, MatIconModule, MatProgressSpinnerModule],
  template: `
    <div class="page">
      <h1 class="page-title">My learning</h1>
      <p class="page-subtitle">Signed in as {{ username() }}.</p>

      @if (loading()) {
        <div class="center"><mat-spinner diameter="36" /></div>
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
    </div>
  `,
  styles: [`
    .center { display: flex; justify-content: center; padding: 60px; }
    .empty { text-align: center; padding: 48px; display: flex; flex-direction: column; align-items: center; gap: 12px; }
    .empty mat-icon { font-size: 40px; height: 40px; width: 40px; color: var(--teltp-muted); }
    .enrol { display: flex; flex-direction: column; gap: 10px; }
    .course-ref { font-size: 0.85rem; font-variant-numeric: tabular-nums; }
    .foot { margin-top: auto; }
  `],
})
export class StudentDashboardComponent {
  private readonly enrollment = inject(EnrollmentService);
  private readonly auth = inject(AuthService);

  readonly username = this.auth.username;
  readonly loading = signal(true);
  readonly enrollments = signal<EnrollmentResponse[]>([]);

  constructor() {
    this.enrollment.myEnrollments().subscribe({
      next: (p) => { this.enrollments.set(p.content); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }

  statusLabel(s: string): string {
    return {
      PENDING_PAYMENT: 'Payment due', ACTIVE: 'Active', WAITLISTED: 'Waitlisted',
      COMPLETED: 'Completed', CANCELLED: 'Cancelled',
    }[s] ?? s;
  }
}
