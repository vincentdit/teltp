import { Component, OnInit, inject, input, signal } from '@angular/core';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { CatalogService } from '../../../core/services/catalog.service';
import { EnrollmentService } from '../../../core/services/enrollment.service';
import { AuthService } from '../../../core/services/auth.service';
import { CourseResponse } from '../../../core/models/catalog.model';

@Component({
  selector: 'app-course-detail',
  standalone: true,
  imports: [MatCardModule, MatButtonModule, MatIconModule, MatProgressSpinnerModule],
  template: `
    <div class="page">
      @if (loading()) {
        <div class="center"><mat-spinner diameter="36" /></div>
      } @else if (course(); as c) {
        <div class="row gap top">
          <span class="chip">{{ label(c.deliveryMode) }}</span>
          @if (c.durationHours) { <span class="chip accent">{{ c.durationHours }} hours</span> }
        </div>
        <h1 class="page-title">{{ c.title }}</h1>
        <p class="muted ref">{{ c.referenceNumber }}</p>

        <mat-card class="surface-card body">
          <p>{{ c.description || 'No description provided for this course.' }}</p>
        </mat-card>

        <div class="row actions">
          <button mat-flat-button color="primary" (click)="enrol(c)" [disabled]="enrolling()">
            <mat-icon>how_to_reg</mat-icon> Enrol in this course
          </button>
          <button mat-stroked-button (click)="back()">Back to catalogue</button>
        </div>

        @if (c.pricingPlanUuid) {
          <p class="muted note">This is a paid course — enrolment creates an invoice payable via GePG, mobile money or bank transfer.</p>
        }
      } @else {
        <p class="muted">Course not found.</p>
      }
    </div>
  `,
  styles: [`
    .center { display: flex; justify-content: center; padding: 60px; }
    .top { margin-bottom: 4px; }
    .gap { gap: 6px; }
    .ref { font-size: 0.8rem; }
    .body { margin: 16px 0 24px; padding: 22px; line-height: 1.6; }
    .actions { gap: 12px; flex-wrap: wrap; }
    .note { margin-top: 16px; font-size: 0.85rem; }
  `],
})
export class CourseDetailComponent implements OnInit {
  // bound from the route param via withComponentInputBinding()
  readonly uuid = input.required<string>();

  private readonly catalog = inject(CatalogService);
  private readonly enrollment = inject(EnrollmentService);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly snack = inject(MatSnackBar);

  readonly loading = signal(true);
  readonly enrolling = signal(false);
  readonly course = signal<CourseResponse | null>(null);

  ngOnInit(): void {
    this.catalog.course(this.uuid()).subscribe({
      next: (c) => { this.course.set(c); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }

  enrol(c: CourseResponse): void {
    if (!this.auth.isAuthenticated()) {
      this.router.navigate(['/login'], { queryParams: { redirect: `/catalog/${c.uuid}` } });
      return;
    }
    this.enrolling.set(true);
    this.enrollment.selfEnroll({ courseUuid: c.uuid }).subscribe({
      next: (e) => {
        this.enrolling.set(false);
        const msg = e.status === 'PENDING_PAYMENT'
          ? 'Enrolled — payment required to activate.'
          : e.status === 'WAITLISTED' ? 'Added to the waitlist.' : 'You are enrolled!';
        this.snack.open(msg, 'My Learning', { duration: 5000 })
          .onAction().subscribe(() => this.router.navigate(['/dashboard']));
      },
      error: () => this.enrolling.set(false),
    });
  }

  back(): void { this.router.navigate(['/catalog']); }

  label(mode: string): string {
    return { ONLINE: 'Online', IN_PERSON: 'In person', HYBRID: 'Hybrid' }[mode] ?? mode;
  }
}
