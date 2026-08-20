import { Component, OnDestroy, OnInit, computed, inject, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatRadioModule } from '@angular/material/radio';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { AssessmentService } from '../../../core/services/assessment.service';
import {
  AssessmentView, AttemptEligibility, AttemptResponse, SubmittedAnswer,
} from '../../../core/models/assessment.model';

type Phase = 'intro' | 'taking' | 'result';

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
      } @else if (phase() === 'result' && result(); as r) {
        <!-- ---- result screen ---- -->
        <div class="result-card surface-card" [class.pass]="r.passed" [class.fail]="r.passed === false">
          <mat-icon class="big">{{ r.passed ? 'emoji_events' : (r.status === 'AWAITING_MANUAL_GRADING' ? 'hourglass_top' : 'sentiment_dissatisfied') }}</mat-icon>
          @if (r.status === 'AWAITING_MANUAL_GRADING') {
            <h1 class="page-title">Submitted for grading</h1>
            <p class="muted">Some questions need manual grading. Your result will appear once an instructor reviews it.</p>
          } @else if (r.status === 'EXPIRED') {
            <h1 class="page-title">Time expired</h1>
            <p class="muted">This attempt ran out of time before it was submitted.</p>
          } @else {
            <h1 class="page-title">{{ r.passed ? 'Passed' : 'Not passed' }}</h1>
            <div class="score" [class.pass]="r.passed" [class.fail]="!r.passed">{{ r.scorePercent }}%</div>
            <p class="muted">Pass mark for this assessment is {{ view()?.passMark }}%.</p>
          }
          <div class="row actions">
            <a mat-flat-button color="primary" [routerLink]="['/learn', courseUuid()]">Back to course</a>
            <a mat-stroked-button routerLink="/results">My results</a>
            @if (r.passed === false || r.status === 'EXPIRED') {
              <button mat-stroked-button (click)="backToIntro()">Try again</button>
            }
          </div>
        </div>
      } @else if (phase() === 'intro') {
        <!-- ---- intro / eligibility screen ---- -->
        <a class="muted back" [routerLink]="['/learn', courseUuid()]"><mat-icon>arrow_back</mat-icon> Back to course</a>
        @if (view(); as a) {
          <h1 class="page-title">{{ a.title }}</h1>
          <p class="page-subtitle">
            {{ a.type === 'EXAM' ? 'Exam' : 'Quiz' }} · {{ a.questions.length }} questions · pass mark {{ a.passMark }}%
          </p>

          <mat-card class="surface-card intro">
            <ul class="facts">
              @if (a.timeLimitMinutes) {
                <li><mat-icon>timer</mat-icon> Time limit: <strong>{{ a.timeLimitMinutes }} min</strong> (auto-submits when time runs out)</li>
              } @else {
                <li><mat-icon>timer_off</mat-icon> No time limit</li>
              }
              @if (eligibility(); as e) {
                @if (e.maxAttempts != null) {
                  <li><mat-icon>replay</mat-icon> Attempts: <strong>{{ e.attemptsUsed }} / {{ e.maxAttempts }}</strong> used
                    @if (e.attemptsRemaining != null) { <span class="muted">({{ e.attemptsRemaining }} left)</span> }
                  </li>
                } @else {
                  <li><mat-icon>all_inclusive</mat-icon> Unlimited attempts</li>
                }
              }
            </ul>

            @if (error()) { <p class="msg bad"><mat-icon>error</mat-icon> {{ error() }}</p> }

            @if (eligibility(); as e) {
              @if (e.hasActiveAttempt) {
                <div class="cta">
                  <p class="muted">You have an attempt in progress.</p>
                  <button mat-flat-button color="primary" (click)="start()" [disabled]="starting()">
                    @if (starting()) { <mat-spinner diameter="18" /> } @else { Resume attempt }
                  </button>
                </div>
              } @else if (e.canAttempt) {
                <div class="cta">
                  <button mat-flat-button color="primary" (click)="start()" [disabled]="starting()">
                    @if (starting()) { <mat-spinner diameter="18" /> } @else { Start attempt }
                  </button>
                </div>
              } @else {
                <div class="cta">
                  <p class="msg blocked"><mat-icon>lock</mat-icon> {{ e.reason || 'This assessment is not available right now.' }}</p>
                  @if (e.alreadyPassed) {
                    <a mat-stroked-button routerLink="/results">View your result</a>
                  }
                </div>
              }
            }
          </mat-card>
        } @else {
          <p class="muted">Assessment not found.</p>
        }
      } @else if (view(); as a) {
        <!-- ---- taking screen ---- -->
        <div class="taking-head">
          <div>
            <h1 class="page-title">{{ a.title }}</h1>
            <p class="page-subtitle">{{ a.type === 'EXAM' ? 'Exam' : 'Quiz' }} · {{ a.questions.length }} questions · pass mark {{ a.passMark }}%</p>
          </div>
          @if (remainingMs() != null) {
            <div class="timer" [class.low]="isLowTime()">
              <mat-icon>timer</mat-icon><span>{{ remainingLabel() }}</span>
            </div>
          }
        </div>

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
      }
    </div>
  `,
  styles: [`
    .narrow { max-width: 760px; }
    .center { display: flex; justify-content: center; padding: 60px; }
    .back { display: inline-flex; align-items: center; gap: 4px; font-size: 0.9rem; text-decoration: none; margin-bottom: 8px; }
    .back mat-icon { font-size: 18px; height: 18px; width: 18px; }
    .intro { padding: 20px 22px; }
    .facts { list-style: none; padding: 0; margin: 0 0 8px; display: flex; flex-direction: column; gap: 10px; }
    .facts li { display: flex; align-items: center; gap: 8px; }
    .facts mat-icon { color: var(--teltp-brand); font-size: 20px; height: 20px; width: 20px; }
    .cta { margin-top: 14px; display: flex; flex-direction: column; align-items: flex-start; gap: 10px; }
    .taking-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; }
    .timer { display: inline-flex; align-items: center; gap: 6px; font-variant-numeric: tabular-nums;
             font-weight: 700; padding: 8px 12px; border-radius: 999px; background: var(--teltp-surface, #eef);
             border: 1px solid var(--teltp-line); position: sticky; top: 12px; white-space: nowrap; }
    .timer mat-icon { font-size: 18px; height: 18px; width: 18px; }
    .timer.low { color: #a33; border-color: #a33; }
    .question { padding: 18px 20px; margin-bottom: 14px; }
    .q-prompt { margin: 0 0 12px; font-weight: 600; line-height: 1.5; }
    .q-num { color: var(--teltp-brand); margin-right: 4px; }
    .pts { font-weight: 400; font-size: 0.85rem; }
    .options { display: flex; flex-direction: column; gap: 8px; }
    .essay { width: 100%; font: inherit; padding: 10px; border: 1px solid var(--teltp-line); border-radius: 8px; resize: vertical; }
    .msg { display: flex; align-items: center; gap: 6px; font-size: 0.9rem; }
    .msg.bad { color: #a33; }
    .msg.blocked { color: #b9892f; }
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
    .actions { gap: 12px; margin-top: 16px; justify-content: center; flex-wrap: wrap; }
  `],
})
export class TakeAssessmentComponent implements OnInit, OnDestroy {
  readonly uuid = input.required<string>();          // /assessments/:uuid/take
  readonly courseUuid = input<string>('');           // ?courseUuid= for back-links

  private readonly assessments = inject(AssessmentService);

  readonly loading = signal(true);
  readonly starting = signal(false);
  readonly submitting = signal(false);
  readonly phase = signal<Phase>('intro');
  readonly view = signal<AssessmentView | null>(null);
  readonly eligibility = signal<AttemptEligibility | null>(null);
  readonly result = signal<AttemptResponse | null>(null);
  readonly error = signal<string | null>(null);
  readonly now = signal<number>(Date.now());
  answers: Record<string, string> = {};

  private expiresMs: number | null = null;
  private timerId: ReturnType<typeof setInterval> | null = null;
  private autoSubmitted = false;

  readonly answeredCount = computed(() =>
    (this.view()?.questions ?? []).filter((q) => !!this.answers[q.uuid]).length);

  readonly remainingMs = computed<number | null>(() =>
    this.expiresMs == null ? null : Math.max(0, this.expiresMs - this.now()));

  readonly isLowTime = computed(() => {
    const ms = this.remainingMs();
    return ms != null && ms <= 60_000;
  });

  readonly remainingLabel = computed(() => {
    const ms = this.remainingMs();
    if (ms == null) return '';
    const total = Math.floor(ms / 1000);
    const m = Math.floor(total / 60);
    const s = total % 60;
    return `${m}:${s.toString().padStart(2, '0')}`;
  });

  ngOnInit(): void { this.load(); }
  ngOnDestroy(): void { this.clearTimer(); }

  private load(): void {
    this.loading.set(true);
    this.error.set(null);
    forkJoin({
      view: this.assessments.view(this.uuid()),
      elig: this.assessments.eligibility(this.uuid()),
    }).subscribe({
      next: ({ view, elig }) => {
        this.view.set(view);
        this.eligibility.set(elig);
        this.phase.set('intro');
        this.loading.set(false);
      },
      error: (e) => { this.error.set(e?.error?.message || 'Could not load the assessment.'); this.loading.set(false); },
    });
  }

  /** Start a fresh attempt or resume an in-progress one (same endpoint). */
  start(): void {
    this.starting.set(true);
    this.error.set(null);
    this.assessments.start(this.uuid()).subscribe({
      next: (a) => {
        this.autoSubmitted = false;
        this.beginTiming(a.expiresAt);
        this.phase.set('taking');
        this.starting.set(false);
        window.scrollTo(0, 0);
      },
      error: (e) => { this.error.set(e?.error?.message || 'Could not start the assessment.'); this.starting.set(false); },
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
      next: (r) => {
        this.clearTimer();
        this.result.set(r);
        this.phase.set('result');
        this.submitting.set(false);
        window.scrollTo(0, 0);
      },
      error: (e) => {
        this.submitting.set(false);
        // If the window elapsed, the server expires the attempt; reflect that as a result.
        const msg = e?.error?.message || 'Could not submit.';
        if (/expired|time limit/i.test(msg)) {
          this.clearTimer();
          this.result.set({
            uuid: '', assessmentUuid: this.uuid(), studentUuid: '', status: 'EXPIRED',
          });
          this.phase.set('result');
        } else {
          this.error.set(msg);
        }
      },
    });
  }

  /** Return from a result screen to the intro so the student can re-check eligibility. */
  backToIntro(): void {
    this.answers = {};
    this.result.set(null);
    this.load();
  }

  private beginTiming(expiresAt?: string): void {
    this.clearTimer();
    if (!expiresAt) { this.expiresMs = null; return; }
    this.expiresMs = new Date(expiresAt).getTime();
    this.now.set(Date.now());
    this.timerId = setInterval(() => {
      this.now.set(Date.now());
      if (this.phase() === 'taking' && !this.autoSubmitted && (this.remainingMs() ?? 1) <= 0) {
        this.autoSubmitted = true;
        const a = this.view();
        if (a) this.submit(a);
      }
    }, 1000);
  }

  private clearTimer(): void {
    if (this.timerId != null) { clearInterval(this.timerId); this.timerId = null; }
  }
}
