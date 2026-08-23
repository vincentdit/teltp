import { Component, OnInit, inject, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FormArray, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { AssessmentService } from '../../../core/services/assessment.service';
import { AssessmentView, QuestionType } from '../../../core/models/assessment.model';

@Component({
  selector: 'app-assessment-edit',
  standalone: true,
  imports: [
    RouterLink, ReactiveFormsModule, MatCardModule, MatFormFieldModule, MatInputModule,
    MatSelectModule, MatButtonModule, MatIconModule, MatCheckboxModule, MatProgressSpinnerModule,
  ],
  template: `
    <div class="page narrow">
      <a class="muted back" routerLink="/admin/assessments"><mat-icon>arrow_back</mat-icon> Assessments</a>
      @if (loading()) {
        <div class="center"><mat-spinner diameter="36" /></div>
      } @else if (assessment(); as a) {
        <h1 class="page-title">{{ a.title }}</h1>
        <p class="page-subtitle">{{ a.type }} · pass mark {{ a.passMark }}% · {{ a.questions.length }} question(s)</p>

        <!-- existing questions -->
        @if (a.questions.length) {
          <mat-card class="surface-card">
            <h3>Questions</h3>
            <div class="stack">
              @for (q of a.questions; track q.uuid; let i = $index) {
                <div class="q-item">
                  <p class="q-prompt"><span class="q-num">Q{{ i + 1 }}.</span> {{ q.prompt }}
                    <span class="muted">({{ q.type.replace('_', ' ').toLowerCase() }} · {{ q.points }} pts)</span></p>
                  @if (q.options.length) {
                    <ul class="opts">
                      @for (o of q.options; track o.uuid) { <li>{{ o.text }}</li> }
                    </ul>
                  }
                </div>
              }
            </div>
          </mat-card>
        }

        <!-- add question -->
        <mat-card class="surface-card form-card">
          <h3>Add a question</h3>
          <form [formGroup]="form" (ngSubmit)="add()" class="stack">
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Prompt</mat-label>
              <textarea matInput formControlName="prompt" rows="2"></textarea>
            </mat-form-field>
            <div class="two">
              <mat-form-field appearance="outline">
                <mat-label>Type</mat-label>
                <mat-select formControlName="type" (selectionChange)="onTypeChange()">
                  <mat-option value="MULTIPLE_CHOICE">Multiple choice (auto-graded)</mat-option>
                  <mat-option value="ESSAY">Essay (manual)</mat-option>
                  <mat-option value="CASE_STUDY">Case study (manual)</mat-option>
                  <mat-option value="PRACTICAL_TASK">Practical task (manual)</mat-option>
                </mat-select>
              </mat-form-field>
              <mat-form-field appearance="outline">
                <mat-label>Points</mat-label>
                <input matInput type="number" formControlName="points" min="1" />
              </mat-form-field>
            </div>

            @if (form.controls.type.value === 'MULTIPLE_CHOICE') {
              <div class="options-block">
                <div class="opts-head">
                  <strong>Options</strong>
                  <span class="muted">Tick the correct one(s)</span>
                </div>
                @for (opt of options.controls; track opt; let i = $index) {
                  <div class="opt-row" [formGroup]="asGroup(opt)">
                    <mat-checkbox formControlName="correct"></mat-checkbox>
                    <mat-form-field appearance="outline" class="opt-text">
                      <mat-label>Option {{ i + 1 }}</mat-label>
                      <input matInput formControlName="text" />
                    </mat-form-field>
                    <button mat-icon-button type="button" (click)="removeOption(i)" [disabled]="options.length <= 2">
                      <mat-icon>close</mat-icon>
                    </button>
                  </div>
                }
                <button mat-stroked-button type="button" (click)="addOption()"><mat-icon>add</mat-icon> Add option</button>
              </div>
            }

            @if (error()) { <p class="msg bad"><mat-icon>error</mat-icon> {{ error() }}</p> }

            <button mat-flat-button color="primary" type="submit" [disabled]="busy()">Add question</button>
          </form>
        </mat-card>
      } @else {
        <p class="muted">Assessment not found.</p>
      }
    </div>
  `,
  styles: [`
    .narrow { max-width: 760px; }
    .back { display: inline-flex; align-items: center; gap: 4px; font-size: 0.9rem; text-decoration: none; margin-bottom: 8px; }
    .back mat-icon { font-size: 18px; height: 18px; width: 18px; }
    .center { display: flex; justify-content: center; padding: 60px; }
    .form-card { padding: 20px 22px; }
    .full-width { width: 100%; }
    .two { display: grid; grid-template-columns: 1fr 1fr; gap: 0 16px; }
    .q-item { padding: 12px 14px; border: 1px solid var(--teltp-line); border-radius: 10px; }
    .q-prompt { margin: 0; font-weight: 600; }
    .q-num { color: var(--teltp-brand); margin-right: 4px; }
    .opts { margin: 8px 0 0; padding-left: 20px; color: var(--teltp-muted); }
    .options-block { border: 1px dashed var(--teltp-line); border-radius: 10px; padding: 14px; display: flex; flex-direction: column; gap: 8px; }
    .opts-head { display: flex; justify-content: space-between; align-items: baseline; }
    .opt-row { display: flex; align-items: center; gap: 10px; }
    .opt-text { flex: 1; }
    .msg { display: flex; align-items: center; gap: 6px; font-size: 0.9rem; }
    .msg.bad { color: #a33; }
    .msg mat-icon { font-size: 18px; height: 18px; width: 18px; }
    @media (max-width: 640px) { .two { grid-template-columns: 1fr; } }
  `],
})
export class AssessmentEditComponent implements OnInit {
  readonly uuid = input.required<string>();

  private readonly assessmentApi = inject(AssessmentService);
  private readonly fb = inject(FormBuilder);
  private readonly snack = inject(MatSnackBar);

  readonly loading = signal(true);
  readonly busy = signal(false);
  readonly error = signal<string | null>(null);
  readonly assessment = signal<AssessmentView | null>(null);

  readonly form = this.fb.group({
    prompt: ['', Validators.required],
    type: ['MULTIPLE_CHOICE', Validators.required],
    points: [10, [Validators.required, Validators.min(1)]],
    options: this.fb.array([this.optionGroup(), this.optionGroup()]),
  });

  get options(): FormArray { return this.form.get('options') as FormArray; }
  asGroup(c: unknown): FormGroup { return c as FormGroup; }

  ngOnInit(): void { this.load(); }

  private load(): void {
    this.loading.set(true);
    this.assessmentApi.view(this.uuid()).subscribe({
      next: (v) => { this.assessment.set(v); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }

  private optionGroup(): FormGroup {
    return this.fb.group({ text: ['', Validators.required], correct: [false] });
  }

  onTypeChange(): void { this.error.set(null); }
  addOption(): void { this.options.push(this.optionGroup()); }
  removeOption(i: number): void { if (this.options.length > 2) this.options.removeAt(i); }

  add(): void {
    this.error.set(null);
    const type = this.form.controls.type.value as QuestionType;
    const prompt = this.form.controls.prompt.value?.trim();
    const points = this.form.controls.points.value;
    if (!prompt || !points || points < 1) { this.error.set('Prompt and a positive point value are required.'); return; }

    let options: { text: string; correct: boolean }[] = [];
    if (type === 'MULTIPLE_CHOICE') {
      options = this.options.getRawValue()
        .map((o: { text: string; correct: boolean }) => ({ text: (o.text || '').trim(), correct: !!o.correct }))
        .filter((o) => o.text.length > 0);
      if (options.length < 2) { this.error.set('Multiple-choice needs at least two options.'); return; }
      if (!options.some((o) => o.correct)) { this.error.set('Mark at least one option correct.'); return; }
    }

    this.busy.set(true);
    this.assessmentApi.addQuestion({ assessmentUuid: this.uuid(), prompt, type, points, options }).subscribe({
      next: () => {
        this.busy.set(false);
        this.snack.open('Question added.', 'Dismiss', { duration: 2500 });
        this.form.reset({ type, points: 10 });
        this.options.clear(); this.options.push(this.optionGroup()); this.options.push(this.optionGroup());
        this.load();
      },
      error: (e) => { this.busy.set(false); this.error.set(e?.error?.message || 'Could not add question.'); },
    });
  }
}
