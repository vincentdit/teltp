import { Component, OnInit, computed, inject, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatRadioModule } from '@angular/material/radio';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { FormsModule } from '@angular/forms';
import { AssessmentService } from '../../../core/services/assessment.service';
import { AssessmentView, AttemptResponse, SubmittedAnswer } from '../../../core/models/assessment.model';

@Component({
  selector: 'app-take-assessment',
  standalone: true,
  imports: [
    RouterLink, FormsModule, MatCardModule, MatButtonModule, MatIconModule,
    MatRadioModule, MatProgressSpinnerModule,
  ],
  template: `
    <div class="page narrow">
      @if (loading()) {
        <div class="center"><mat-spinner diameter="36" /></div>
      } @else if (result(); as r) {
        <!-- ---- result screen ---- -->
        <div class="result-card surface-card" [class.pass]="r.passed" [class.fail]="r.passed === false">
          <mat-icon class="big">{{ r.passed ? 'emoji_events' : (r.status === 'AWAITING_MANUAL_GRADING' ? 'hourglass_top' : 'sentiment_dissatisfied') }}</mat-icon>
          @if (r.status === 'AWAITING_MANUAL_GRADING') {
            <h1 class="page-title">Submitted for grading</h1>
            <p class="muted">Some questions need manual grading. Your result will be available once an instructor reviews it.</p>
          } @else {
            <h1 class="page-title">{{ r.passed ? 'Passed' : 'Not passed' }}</h1>
            <div class="score" [class.pass]="r.passed" [class.fail]="!r.passed">{{ r.scorePercent }}%</div>
            <p class="muted">Pass mark for this assessment is {{ view()?.passMark }}%.</p>
          }
          <div class="row actions">
            <a mat-flat-button color="primary" [routerLink]="['/learn', courseUuid()]">Back to course</a>
            @if (r.passed === false) {
              <button mat-stroked-button (click)="retake()">Retake</button>
            }
          </div>
        </div>
      } @else if (view(); as a) {
        <!-- ---- taking screen ---- -->
        <a class="muted back" [routerLink]="['/learn', courseUuid()]"><mat-icon>arrow_back</mat-icon> Back to course</a>
        <h1 class="page-title">{{ a.title }}</h1>
        <p class="page-subtitle">
          {{ a.type === 'EXAM' ? 'Exam' : 'Quiz' }} · {{ a.questions.length }} questions · pass mark {{ a.passMark }}%
        </p>

        @for (q of a.questions; track q.uuid; let i = $index) {
          <mat-card class="surface-card question">
            <p class="q-prompt"><span class="q-num">Q{{ i + 1 }}.</span> {{ q.prompt }} <span class="pts muted">({{ q.points }} pts)</span></p>
            @if (q.type === 'MULTIPLE_CHOICE') {
              <mat-radio-group [(ngModel)]="answers[q.uuid]" class="options">
                @for (o of q.options; track o.uuid) {
                  <mat-radio-button [value]="o.uuid">{{ o.text }}</mat-radio-button>
                }
              </mat-radio-group>
            } @else {
              <textarea class="essay" rows="4" [(ngModel)]="answers[q.uuid]"
                        placeholder="Type your answer… (graded by an instructor)"></textarea>
            }
          </mat-card>
        }

        @if (error()) { <p class="msg bad"><mat-icon>error</mat-icon> {{ error() }}</p> }

        <div class="row submit-bar">
          <span class="muted">{{ answeredCount() }} of {{ a.questions.length }} answered</span>
          <span class="spacer"></span>
          <button mat-flat-button color="primary" (click)="submit(a)" [disabled]="submitting()">
            @if (submitting()) { <mat-spinner diameter="18" /> } @else { Submit assessment }
          </button>
        </div>
      } @else {
        <p class="muted">Assessment not found.</p>
      }
    </div>
  `,
  styles: [`
    .narrow { max-width: 760px; }
    .center { display: flex; justify-content: center; padding: 60px; }
    .back { display: inline-flex; align-items: center; gap: 4px; font-size: 0.9rem; text-decoration: none; margin-bottom: 8px; }
    .back mat-icon { font-size: 18px; height: 18px; width: 18px; }
    .question { padding: 18px 20px; margin-bottom: 14px; }
    .q-prompt { margin: 0 0 12px; font-weight: 600; line-height: 1.5; }
    .q-num { color: var(--teltp-brand); margin-right: 4px; }
    .pts { font-weight: 400; font-size: 0.85rem; }
    .options { display: flex; flex-direction: column; gap: 8px; }
    .essay { width: 100%; font: inherit; padding: 10px; border: 1px solid var(--teltp-line); border-radius: 8px; resize: vertical; }
    .msg { display: flex; align-items: center; gap: 6px; font-size: 0.9rem; }
    .msg.bad { color: #a33; }
    .msg mat-icon { font-size: 18px; height: 18px; width: 18px; }
    .submit-bar { position: sticky; bottom: 0; background: var(--teltp-bg); padding: 14px 0; border-top: 1px solid var(--teltp-line); margin-top: 8px; }
    .result-card { text-align: center; padding: 48px 32px; display: flex; flex-direction: column; align-items: center; gap: 8px; }
    .result-card.pass { border-top: 4px solid var(--teltp-brand); }
    .result-card.fail { border-top: 4px solid #b9892f; }
    .result-card .big { font-size: 56px; height: 56px; width: 56px; }
    .result-card.pass .big { color: var(--teltp-brand); }
    .result-card.fail .big { color: #b9892f; }
    .score { font-family: 'Spectral', Georgia, serif; font-size: 3rem; font-weight: 700; line-height: 1; }
    .score.pass { color: var(--teltp-brand); }
    .score.fail { color: #a33; }
    .actions { gap: 12px; margin-top: 16px; justify-content: center; }
  `],
})
export class TakeAssessmentComponent implements OnInit {
  readonly uuid = input.required<string>();          // /assessments/:uuid/take
  readonly courseUuid = input<string>('');           // ?courseUuid= for back-links

  private readonly assessments = inject(AssessmentService);

  readonly loading = signal(true);
  readonly submitting = signal(false);
  readonly view = signal<AssessmentView | null>(null);
  readonly result = signal<AttemptResponse | null>(null);
  readonly error = signal<string | null>(null);
  answers: Record<string, string> = {};

  readonly answeredCount = computed(() =>
    (this.view()?.questions ?? []).filter((q) => !!this.answers[q.uuid]).length);

  ngOnInit(): void { this.load(); }

  private load(): void {
    this.loading.set(true);
    this.assessments.view(this.uuid()).subscribe({
      next: (v) => { this.view.set(v); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }

  submit(a: AssessmentView): void {
    this.error.set(null);
    const answers: SubmittedAnswer[] = a.questions.map((q) => {
      const val = this.answers[q.uuid];
      return q.type === 'MULTIPLE_CHOICE'
        ? { questionUuid: q.uuid, selectedOptionUuid: val }
        : { questionUuid: q.uuid, response: val };
    });
    this.submitting.set(true);
    this.assessments.submit({ assessmentUuid: this.uuid(), answers }).subscribe({
      next: (r) => { this.result.set(r); this.submitting.set(false); window.scrollTo(0, 0); },
      error: (e) => { this.error.set(e?.error?.message || 'Could not submit.'); this.submitting.set(false); },
    });
  }

  retake(): void {
    this.answers = {};
    this.result.set(null);
    this.load();
  }
}
