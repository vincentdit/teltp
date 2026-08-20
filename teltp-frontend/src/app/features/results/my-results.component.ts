import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AssessmentService } from '../../core/services/assessment.service';
import { MyAttemptResult, MyAttemptSummary } from '../../core/models/assessment.model';

@Component({
  selector: 'app-my-results',
  standalone: true,
  imports: [DatePipe, MatCardModule, MatButtonModule, MatIconModule, MatProgressSpinnerModule],
  template: `
    <div class="page narrow">
      <h1 class="page-title">My results</h1>
      <p class="page-subtitle">Every quiz and exam attempt you've made.</p>

      @if (loading()) {
        <div class="center"><mat-spinner diameter="36" /></div>
      } @else if (error()) {
        <p class="msg bad"><mat-icon>error</mat-icon> {{ error() }}</p>
      } @else if (history(); as rows) {
        @if (rows.length === 0) {
          <mat-card class="surface-card empty">
            <mat-icon>fact_check</mat-icon>
            <p class="muted">You haven't taken any assessments yet.</p>
          </mat-card>
        } @else {
          @for (r of rows; track r.uuid) {
            <mat-card class="surface-card row-card">
              <div class="row head" (click)="toggle(r)">
                <div class="title">
                  <span class="badge type">{{ r.type === 'EXAM' ? 'Exam' : (r.type === 'QUIZ' ? 'Quiz' : 'Assessment') }}</span>
                  <span class="name">{{ r.assessmentTitle }}</span>
                </div>
                <span class="spacer"></span>
                @if (r.scorePercent != null) { <span class="score-chip" [class.pass]="r.passed" [class.fail]="r.passed === false">{{ r.scorePercent }}%</span> }
                <span class="badge status" [attr.data-s]="r.status">{{ statusLabel(r.status) }}</span>
                <mat-icon class="chev">{{ expanded() === r.assessmentUuid ? 'expand_less' : 'expand_more' }}</mat-icon>
              </div>
              <div class="sub muted">{{ r.submittedAt ? (r.submittedAt | date: 'medium') : 'Not submitted' }}</div>

              @if (expanded() === r.assessmentUuid) {
                <div class="detail">
                  @if (detailLoading()) {
                    <div class="center"><mat-spinner diameter="24" /></div>
                  } @else if (detail(); as d) {
                    <p class="muted small">Latest attempt breakdown · pass mark {{ d.passMark }}%
                      @if (d.scorePercent != null) { · scored <strong>{{ d.scorePercent }}%</strong> }
                    </p>
                    @for (ans of d.answers; track ans.questionUuid; let i = $index) {
                      <div class="ans">
                        <p class="q"><span class="q-num">Q{{ i + 1 }}.</span> {{ ans.prompt }}
                          <span class="pts muted">({{ ans.awardedPoints ?? '—' }}/{{ ans.maxPoints }})</span>
                        </p>
                        @if (ans.yourSelectedOptionText) { <p class="your"><span class="lbl">Your answer:</span> {{ ans.yourSelectedOptionText }}</p> }
                        @else if (ans.yourResponse) { <p class="your"><span class="lbl">Your answer:</span> {{ ans.yourResponse }}</p> }
                        @else { <p class="your muted">No answer given.</p> }
                        @if (ans.feedback) { <p class="fb"><mat-icon>rate_review</mat-icon> {{ ans.feedback }}</p> }
                      </div>
                    }
                  }
                </div>
              }
            </mat-card>
          }
        }
      }
    </div>
  `,
  styles: [`
    .narrow { max-width: 820px; }
    .center { display: flex; justify-content: center; padding: 40px; }
    .empty { padding: 40px; text-align: center; display: flex; flex-direction: column; align-items: center; gap: 8px; }
    .empty mat-icon { font-size: 40px; height: 40px; width: 40px; color: var(--teltp-brand); }
    .row-card { padding: 14px 18px; margin-bottom: 12px; }
    .head { display: flex; align-items: center; gap: 10px; cursor: pointer; }
    .title { display: flex; align-items: center; gap: 10px; min-width: 0; }
    .name { font-weight: 600; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .sub { font-size: 0.82rem; margin-top: 2px; }
    .badge { font-size: 0.72rem; padding: 2px 8px; border-radius: 999px; border: 1px solid var(--teltp-line); white-space: nowrap; }
    .badge.type { color: var(--teltp-brand); border-color: var(--teltp-brand); }
    .score-chip { font-weight: 700; font-variant-numeric: tabular-nums; }
    .score-chip.pass { color: var(--teltp-brand); }
    .score-chip.fail { color: #a33; }
    .chev { color: var(--teltp-muted, #888); }
    .detail { margin-top: 12px; padding-top: 12px; border-top: 1px solid var(--teltp-line); }
    .small { font-size: 0.85rem; }
    .ans { padding: 10px 0; border-bottom: 1px dashed var(--teltp-line); }
    .ans:last-child { border-bottom: none; }
    .q { margin: 0 0 4px; font-weight: 600; line-height: 1.4; }
    .q-num { color: var(--teltp-brand); margin-right: 4px; }
    .pts { font-weight: 400; font-size: 0.82rem; }
    .your { margin: 2px 0; }
    .your .lbl { color: var(--teltp-muted, #777); margin-right: 4px; }
    .fb { display: flex; align-items: center; gap: 6px; margin: 6px 0 0; font-size: 0.9rem; color: #4a6; }
    .fb mat-icon { font-size: 18px; height: 18px; width: 18px; }
    .msg { display: flex; align-items: center; gap: 6px; }
    .msg.bad { color: #a33; }
  `],
})
export class MyResultsComponent implements OnInit {
  private readonly assessments = inject(AssessmentService);

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly history = signal<MyAttemptSummary[] | null>(null);
  readonly expanded = signal<string | null>(null);
  readonly detail = signal<MyAttemptResult | null>(null);
  readonly detailLoading = signal(false);

  ngOnInit(): void {
    this.assessments.myHistory().subscribe({
      next: (rows) => { this.history.set(rows); this.loading.set(false); },
      error: (e) => { this.error.set(e?.error?.message || 'Could not load your results.'); this.loading.set(false); },
    });
  }

  toggle(row: MyAttemptSummary): void {
    if (this.expanded() === row.assessmentUuid) {
      this.expanded.set(null);
      this.detail.set(null);
      return;
    }
    this.expanded.set(row.assessmentUuid);
    this.detail.set(null);
    this.detailLoading.set(true);
    this.assessments.myResult(row.assessmentUuid).subscribe({
      next: (d) => { this.detail.set(d); this.detailLoading.set(false); },
      error: () => { this.detailLoading.set(false); },
    });
  }

  statusLabel(s: string): string {
    switch (s) {
      case 'AWAITING_MANUAL_GRADING': return 'Awaiting grading';
      case 'AUTO_GRADED': return 'Graded';
      case 'GRADED': return 'Graded';
      case 'IN_PROGRESS': return 'In progress';
      case 'EXPIRED': return 'Expired';
      case 'SUBMITTED': return 'Submitted';
      default: return s;
    }
  }
}
