import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar } from '@angular/material/snack-bar';
import { CatalogService } from '../../../core/services/catalog.service';
import { AssessmentService } from '../../../core/services/assessment.service';
import { CourseResponse } from '../../../core/models/catalog.model';
import { AssessmentSummary } from '../../../core/models/assessment.model';

@Component({
  selector: 'app-assessment-manage',
  standalone: true,
  imports: [
    RouterLink, ReactiveFormsModule, MatCardModule, MatFormFieldModule, MatInputModule,
    MatSelectModule, MatButtonModule, MatIconModule,
  ],
  template: `
    <div class="page">
      <a class="muted back" routerLink="/admin"><mat-icon>arrow_back</mat-icon> Administration</a>
      <h1 class="page-title">Assessment authoring</h1>
      <p class="page-subtitle">Create quizzes and exams for a course, then add questions.</p>

      <mat-card class="surface-card form-card">
        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Course</mat-label>
          <mat-select [value]="courseUuid()" (selectionChange)="selectCourse($event.value)">
            @for (c of courses(); track c.uuid) {
              <mat-option [value]="c.uuid">{{ c.title }} — {{ c.referenceNumber }}</mat-option>
            }
          </mat-select>
        </mat-form-field>
      </mat-card>

      @if (courseUuid()) {
        <div class="cols">
          <!-- existing assessments -->
          <mat-card class="surface-card">
            <h3>Assessments for this course</h3>
            @if (loadingList()) {
              <p class="muted">Loading…</p>
            } @else if (assessments().length === 0) {
              <p class="muted">None yet. Create one on the right.</p>
            } @else {
              <div class="stack">
                @for (a of assessments(); track a.uuid) {
                  <a class="row-item" [routerLink]="['/admin/assessments', a.uuid]">
                    <div>
                      <strong>{{ a.title }}</strong>
                      <span class="muted">{{ a.type }} · pass {{ a.passMark }}%{{ a.timeLimitMinutes ? ' · ' + a.timeLimitMinutes + ' min' : '' }}</span>
                    </div>
                    <span class="spacer"></span>
                    <span class="edit">Add questions <mat-icon>chevron_right</mat-icon></span>
                  </a>
                }
              </div>
            }
          </mat-card>

          <!-- new assessment -->
          <mat-card class="surface-card form-card">
            <h3>New assessment</h3>
            <form [formGroup]="form" (ngSubmit)="create()" class="stack">
              <mat-form-field appearance="outline" class="full-width">
                <mat-label>Title</mat-label>
                <input matInput formControlName="title" />
              </mat-form-field>
              <div class="two">
                <mat-form-field appearance="outline">
                  <mat-label>Type</mat-label>
                  <mat-select formControlName="type">
                    <mat-option value="QUIZ">Quiz</mat-option>
                    <mat-option value="EXAM">Exam (certifying)</mat-option>
                  </mat-select>
                </mat-form-field>
                <mat-form-field appearance="outline">
                  <mat-label>Pass mark (%)</mat-label>
                  <input matInput type="number" formControlName="passMark" min="0" max="100" />
                </mat-form-field>
              </div>
              <div class="two">
                <mat-form-field appearance="outline">
                  <mat-label>Time limit (min, optional)</mat-label>
                  <input matInput type="number" formControlName="timeLimitMinutes" min="1" />
                </mat-form-field>
                <mat-form-field appearance="outline">
                  <mat-label>Max attempts (optional)</mat-label>
                  <input matInput type="number" formControlName="maxAttempts" min="1" />
                </mat-form-field>
              </div>
              <mat-form-field appearance="outline" class="full-width">
                <mat-label>Cooldown between attempts (min, optional)</mat-label>
                <input matInput type="number" formControlName="cooldownMinutes" min="0" />
              </mat-form-field>
              @if (form.controls.type.value === 'EXAM') {
                <p class="hint muted"><mat-icon>info</mat-icon> Exams gate course completion — a learner must pass to finish the course.</p>
              }
              <button mat-flat-button color="primary" type="submit" [disabled]="form.invalid || busy()">
                Create assessment
              </button>
            </form>
          </mat-card>
        </div>
      }
    </div>
  `,
  styles: [`
    .back { display: inline-flex; align-items: center; gap: 4px; font-size: 0.9rem; text-decoration: none; margin-bottom: 8px; }
    .back mat-icon { font-size: 18px; height: 18px; width: 18px; }
    .form-card { padding: 20px 22px; }
    .full-width { width: 100%; }
    .two { display: grid; grid-template-columns: 1fr 1fr; gap: 0 16px; }
    .cols { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; align-items: start; }
    .row-item { display: flex; align-items: center; gap: 12px; text-decoration: none; color: inherit;
      padding: 12px 14px; border: 1px solid var(--teltp-line); border-radius: 10px; }
    .row-item:hover { border-color: var(--teltp-brand); }
    .row-item div { display: flex; flex-direction: column; }
    .row-item .muted { font-size: 0.85rem; }
    .edit { display: inline-flex; align-items: center; gap: 2px; color: var(--teltp-brand); font-weight: 600; font-size: 0.9rem; }
    .hint { display: flex; align-items: center; gap: 6px; font-size: 0.85rem; margin: 0; }
    .hint mat-icon { font-size: 18px; height: 18px; width: 18px; }
    @media (max-width: 900px) { .cols, .two { grid-template-columns: 1fr; } }
  `],
})
export class AssessmentManageComponent {
  private readonly catalog = inject(CatalogService);
  private readonly assessmentApi = inject(AssessmentService);
  private readonly fb = inject(FormBuilder);
  private readonly snack = inject(MatSnackBar);

  readonly courses = signal<CourseResponse[]>([]);
  readonly courseUuid = signal<string>('');
  readonly assessments = signal<AssessmentSummary[]>([]);
  readonly loadingList = signal(false);
  readonly busy = signal(false);

  readonly form = this.fb.group({
    title: ['', Validators.required],
    type: ['QUIZ', Validators.required],
    passMark: [60, [Validators.required, Validators.min(0), Validators.max(100)]],
    timeLimitMinutes: [null as number | null],
    maxAttempts: [null as number | null],
    cooldownMinutes: [null as number | null],
  });

  constructor() {
    this.catalog.publishedCourses(0, 100).subscribe({ next: (p) => this.courses.set(p.content) });
  }

  selectCourse(uuid: string): void {
    this.courseUuid.set(uuid);
    this.loadList();
  }

  private loadList(): void {
    this.loadingList.set(true);
    this.assessmentApi.forCourse(this.courseUuid()).subscribe({
      next: (list) => { this.assessments.set(list); this.loadingList.set(false); },
      error: () => this.loadingList.set(false),
    });
  }

  create(): void {
    if (this.form.invalid || !this.courseUuid()) return;
    const v = this.form.getRawValue();
    this.busy.set(true);
    this.assessmentApi.createAssessment({
      courseUuid: this.courseUuid(),
      title: v.title!,
      type: v.type as 'QUIZ' | 'EXAM',
      passMark: v.passMark!,
      timeLimitMinutes: v.timeLimitMinutes ?? undefined,
      maxAttempts: v.maxAttempts ?? undefined,
      cooldownMinutes: v.cooldownMinutes ?? undefined,
    }).subscribe({
      next: (a) => {
        this.busy.set(false);
        this.snack.open('Assessment created — now add questions.', 'Dismiss', { duration: 3000 });
        this.form.reset({ type: 'QUIZ', passMark: 60 });
        this.assessments.update((list) => [a, ...list]);
      },
      error: (e) => { this.busy.set(false); this.snack.open(e?.error?.message || 'Could not create.', 'Dismiss', { duration: 4000 }); },
    });
  }
}
