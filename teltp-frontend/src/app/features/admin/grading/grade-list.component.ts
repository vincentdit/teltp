import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DatePipe } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AssessmentService } from '../../../core/services/assessment.service';
import { AttemptSummary } from '../../../core/models/assessment.model';

@Component({
  selector: 'app-grade-list',
  standalone: true,
  imports: [RouterLink, DatePipe, MatCardModule, MatButtonModule, MatIconModule, MatProgressSpinnerModule],
  template: `
    <div class="page">
      <a class="muted back" routerLink="/admin"><mat-icon>arrow_back</mat-icon> Administration</a>
      <h1 class="page-title">Assessment grading</h1>
      <p class="page-subtitle">Attempts with written answers awaiting manual grading.</p>

      @if (loading()) {
        <div class="center"><mat-spinner diameter="36" /></div>
      } @else if (attempts().length === 0) {
        <div class="surface-card empty">
          <mat-icon>task_alt</mat-icon>
          <p class="muted">Nothing awaiting grading. All caught up.</p>
        </div>
      } @else {
        <div class="stack">
          @for (a of attempts(); track a.uuid) {
            <a class="surface-card row-item" [routerLink]="['/admin/grading', a.uuid]">
              <div class="who">
                <strong>{{ a.assessmentTitle }}</strong>
                <span class="muted">Student {{ a.studentUuid.slice(0, 8) }}… · submitted {{ a.submittedAt | date:'medium' }}</span>
              </div>
              <span class="spacer"></span>
              <span class="chip accent">Awaiting</span>
              <mat-icon>chevron_right</mat-icon>
            </a>
          }
        </div>
      }
    </div>
  `,
  styles: [`
    .back { display: inline-flex; align-items: center; gap: 4px; font-size: 0.9rem; text-decoration: none; margin-bottom: 8px; }
    .back mat-icon { font-size: 18px; height: 18px; width: 18px; }
    .center { display: flex; justify-content: center; padding: 60px; }
    .empty { text-align: center; padding: 48px; display: flex; flex-direction: column; align-items: center; gap: 12px; }
    .empty mat-icon { font-size: 40px; height: 40px; width: 40px; color: var(--teltp-brand); }
    .row-item { display: flex; align-items: center; gap: 12px; text-decoration: none; color: inherit; padding: 16px 18px; }
    .row-item:hover { border-color: var(--teltp-brand); }
    .who { display: flex; flex-direction: column; }
    .who .muted { font-size: 0.85rem; }
  `],
})
export class GradeListComponent {
  private readonly assessments = inject(AssessmentService);
  readonly loading = signal(true);
  readonly attempts = signal<AttemptSummary[]>([]);

  constructor() {
    this.assessments.pendingAttempts().subscribe({
      next: (list) => { this.attempts.set(list); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }
}
