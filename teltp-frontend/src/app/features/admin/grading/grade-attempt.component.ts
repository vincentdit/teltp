import { Component, OnInit, computed, inject, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { AssessmentService } from '../../../core/services/assessment.service';
import { AnswerToGrade, AttemptGradingView } from '../../../core/models/assessment.model';

@Component({
  selector: 'app-grade-attempt',
  standalone: true,
  imports: [
    RouterLink, FormsModule, MatCardModule, MatFormFieldModule, MatInputModule,
    MatButtonModule, MatIconModule, MatProgressSpinnerModule,
  ],
  template: `
    <div class="page narrow">
      <a class="muted back" routerLink="/admin/grading"><mat-icon>arrow_back</mat-icon> Grading queue</a>
      @if (loading()) {
        <div class="center"><mat-spinner diameter="36" /></div>
      } @else if (attempt(); as at) {
        <h1 class="page-title">{{ at.assessmentTitle }}</h1>
        <p class="page-subtitle">Student {{ at.studentUuid.slice(0, 8) }}… · pass mark {{ at.passMark }}%</p>

        <div class="status-bar surface-card">
          <span class="chip" [class.accent]="at.status !== 'GRADED'">{{ statusLabel(at.status) }}</span>
          <span class="spacer"></span>
          @if (at.status === 'GRADED') {
            <span class="score" [class.pass]="at.passed" [class.fail]="!at.passed">
              {{ at.scorePercent }}% · {{ at.passed ? 'Passed' : 'Not passed' }}
            </span>
          } @else {
            <span class="muted">{{ remaining() }} answer(s) left to grade</span>
          }
        </div>

        @for (ans of at.answers; track ans.questionUuid; let i = $index) {
          <mat-card class="surface-card answer">
            <p class="prompt"><span class="q-num">Q{{ i + 1 }}.</span> {{ ans.prompt }}
              <span class="pts muted">(max {{ ans.maxPoints }} pts)</span></p>

            @if (ans.autoGraded) {
              <p class="given"><span class="muted">Answer:</span> {{ ans.selectedOptionText || '—' }}</p>
              <p class="auto"><mat-icon>bolt</mat-icon> Auto-graded · {{ ans.awardedPoints }} / {{ ans.maxPoints }} pts</p>
            } @else {
              <div class="given essay-box">{{ ans.response || '(no answer provided)' }}</div>
              @if (ans.awardedPoints != null && !editing[ans.questionUuid]) {
                <div class="graded-row">
                  <span class="chip">Graded · {{ ans.awardedPoints }} / {{ ans.maxPoints }} pts</span>
                  @if (ans.graderFeedback) { <span class="muted fb">“{{ ans.graderFeedback }}”</span> }
                  <span class="spacer"></span>
                  <button mat-button (click)="edit(ans)">Edit</button>
                </div>
              } @else {
                <div class="grade-form">
                  <mat-form-field appearance="outline" class="pts-field">
                    <mat-label>Points</mat-label>
                    <input matInput type="number" min="0" [max]="ans.maxPoints" [(ngModel)]="points[ans.questionUuid]" />
                  </mat-form-field>
                  <mat-form-field appearance="outline" class="fb-field">
                    <mat-label>Feedback (optional)</mat-label>
                    <input matInput [(ngModel)]="feedback[ans.questionUuid]" />
                  </mat-form-field>
                  <button mat-flat-button color="primary" (click)="save(at, ans)"
                          [disabled]="saving() === ans.questionUuid || !validPoints(ans)">
                    Save
                  </button>
                </div>
              }
            }
          </mat-card>
        }
      } @else {
        <p class="muted">Attempt not found.</p>
      }
    </div>
  `,
  styles: [`
    .narrow { max-width: 780px; }
    .back { display: inline-flex; align-items: center; gap: 4px; font-size: 0.9rem; text-decoration: none; margin-bottom: 8px; }
    .back mat-icon { font-size: 18px; height: 18px; width: 18px; }
    .center { display: flex; justify-content: center; padding: 60px; }
    .status-bar { display: flex; align-items: center; padding: 12px 16px; margin-bottom: 16px; }
    .status-bar .score { font-weight: 700; }
    .status-bar .score.pass { color: var(--teltp-brand); }
    .status-bar .score.fail { color: #a33; }
    .answer { padding: 18px 20px; margin-bottom: 14px; }
    .prompt { margin: 0 0 10px; font-weight: 600; line-height: 1.5; }
    .q-num { color: var(--teltp-brand); margin-right: 4px; }
    .pts { font-weight: 400; font-size: 0.85rem; }
    .given { margin: 0 0 10px; }
    .essay-box { background: var(--teltp-bg); border: 1px solid var(--teltp-line); border-radius: 8px;
      padding: 12px 14px; white-space: pre-wrap; line-height: 1.6; }
    .auto { display: flex; align-items: center; gap: 6px; color: var(--teltp-muted); font-size: 0.9rem; margin: 0; }
    .auto mat-icon { font-size: 18px; height: 18px; width: 18px; color: var(--teltp-accent); }
    .graded-row { display: flex; align-items: center; gap: 12px; margin-top: 10px; }
    .graded-row .fb { font-style: italic; font-size: 0.88rem; }
    .grade-form { display: flex; gap: 12px; align-items: flex-start; margin-top: 12px; }
    .pts-field { width: 120px; }
    .fb-field { flex: 1; }
  `],
})
export class GradeAttemptComponent implements OnInit {
  readonly uuid = input.required<string>();

  private readonly assessments = inject(AssessmentService);
  private readonly snack = inject(MatSnackBar);

  readonly loading = signal(true);
  readonly saving = signal<string | null>(null);
  readonly attempt = signal<AttemptGradingView | null>(null);

  points: Record<string, number> = {};
  feedback: Record<string, string> = {};
  editing: Record<string, boolean> = {};

  readonly remaining = computed(() =>
    (this.attempt()?.answers ?? []).filter((a) => !a.autoGraded && a.awardedPoints == null).length);

  ngOnInit(): void { this.load(); }

  private load(): void {
    this.loading.set(true);
    this.assessments.gradingView(this.uuid()).subscribe({
      next: (v) => { this.attempt.set(v); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }

  edit(ans: AnswerToGrade): void {
    this.editing[ans.questionUuid] = true;
    this.points[ans.questionUuid] = ans.awardedPoints ?? 0;
    this.feedback[ans.questionUuid] = ans.graderFeedback ?? '';
  }

  validPoints(ans: AnswerToGrade): boolean {
    const p = this.points[ans.questionUuid];
    return p != null && p >= 0 && p <= ans.maxPoints;
  }

  save(at: AttemptGradingView, ans: AnswerToGrade): void {
    if (!this.validPoints(ans)) return;
    this.saving.set(ans.questionUuid);
    this.assessments.grade({
      attemptUuid: at.uuid,
      questionUuid: ans.questionUuid,
      awardedPoints: this.points[ans.questionUuid],
      feedback: this.feedback[ans.questionUuid] || undefined,
    }).subscribe({
      next: () => {
        this.saving.set(null);
        this.editing[ans.questionUuid] = false;
        this.snack.open('Grade saved.', 'Dismiss', { duration: 2500 });
        this.load(); // refresh awarded points + finalized status/score
      },
      error: (e) => {
        this.saving.set(null);
        this.snack.open(e?.error?.message || 'Could not save grade.', 'Dismiss', { duration: 4000 });
      },
    });
  }

  statusLabel(s: string): string {
    return {
      AWAITING_MANUAL_GRADING: 'Awaiting grading',
      GRADED: 'Graded',
      AUTO_GRADED: 'Auto-graded',
      IN_PROGRESS: 'In progress',
    }[s] ?? s;
  }
}
