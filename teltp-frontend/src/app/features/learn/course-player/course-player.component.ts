import { Component, OnInit, computed, inject, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { CatalogService } from '../../../core/services/catalog.service';
import { ProgressService } from '../../../core/services/progress.service';
import { CourseCurriculumResponse, CurriculumLesson } from '../../../core/models/catalog.model';
import { CourseProgressResponse } from '../../../core/models/enrollment.model';

@Component({
  selector: 'app-course-player',
  standalone: true,
  imports: [
    RouterLink, MatCardModule, MatButtonModule, MatIconModule,
    MatProgressBarModule, MatProgressSpinnerModule,
  ],
  template: `
    <div class="page">
      @if (loading()) {
        <div class="center"><mat-spinner diameter="36" /></div>
      } @else if (curriculum(); as cur) {
        <a class="muted back" routerLink="/dashboard"><mat-icon>arrow_back</mat-icon> My Learning</a>
        <h1 class="page-title">{{ cur.title }}</h1>

        @if (progress(); as p) {
          <div class="progress-wrap">
            <div class="row">
              <span class="pct">{{ p.percentComplete }}%</span>
              <span class="muted">{{ p.completedLessons }} of {{ p.mandatoryLessons }} required lessons</span>
              <span class="spacer"></span>
              @if (p.courseCompleted) { <span class="chip done"><mat-icon>verified</mat-icon> Completed</span> }
            </div>
            <mat-progress-bar mode="determinate" [value]="p.percentComplete" />
          </div>
          @if (p.courseCompleted) {
            <div class="surface-card banner">
              <mat-icon>workspace_premium</mat-icon>
              <div>
                <strong>Course complete.</strong>
                <p class="muted">You've finished all required lessons. A certificate can now be issued by your instructor.</p>
              </div>
            </div>
          }
        }

        <div class="layout">
          <!-- curriculum sidebar -->
          <aside class="surface-card outline">
            @for (m of cur.modules; track m.uuid) {
              <div class="module">
                <p class="module-title">{{ m.orderIndex }}. {{ m.title }}</p>
                @for (l of m.lessons; track l.uuid) {
                  <button class="lesson" [class.active]="selected()?.uuid === l.uuid"
                          [class.complete]="isComplete(l.uuid)" (click)="select(l)">
                    <mat-icon class="tick">{{ isComplete(l.uuid) ? 'check_circle' : 'radio_button_unchecked' }}</mat-icon>
                    <span class="lesson-title">{{ l.title }}</span>
                    @if (l.estimatedMinutes) { <span class="mins muted">{{ l.estimatedMinutes }}m</span> }
                  </button>
                }
              </div>
            }
          </aside>

          <!-- lesson content -->
          <section>
            @if (selected(); as l) {
              <mat-card class="surface-card content">
                <h2>{{ l.title }}</h2>
                <p class="lesson-body">{{ l.content || 'No content for this lesson yet.' }}</p>
                <div class="row actions">
                  <button mat-flat-button color="primary" (click)="markComplete(l)"
                          [disabled]="marking() || isComplete(l.uuid)">
                    <mat-icon>{{ isComplete(l.uuid) ? 'check' : 'done' }}</mat-icon>
                    {{ isComplete(l.uuid) ? 'Completed' : 'Mark as complete' }}
                  </button>
                  @if (nextLesson(l); as nxt) {
                    <button mat-stroked-button (click)="select(nxt)">Next lesson <mat-icon>arrow_forward</mat-icon></button>
                  }
                </div>
              </mat-card>
            } @else {
              <div class="surface-card empty">
                <mat-icon>menu_book</mat-icon>
                <p class="muted">Select a lesson to begin.</p>
              </div>
            }
          </section>
        </div>
      } @else {
        <p class="muted">Course not found.</p>
      }
    </div>
  `,
  styles: [`
    .center { display: flex; justify-content: center; padding: 60px; }
    .back { display: inline-flex; align-items: center; gap: 4px; font-size: 0.9rem; text-decoration: none; margin-bottom: 8px; }
    .back mat-icon { font-size: 18px; height: 18px; width: 18px; }
    .progress-wrap { margin: 8px 0 20px; }
    .progress-wrap .pct { font-weight: 700; font-size: 1.1rem; }
    .chip.done { display: inline-flex; align-items: center; gap: 4px; background: rgba(46,107,65,.12); color: var(--teltp-brand-dark); }
    .chip.done mat-icon { font-size: 16px; height: 16px; width: 16px; }
    .banner { display: flex; gap: 14px; align-items: center; margin: 16px 0 24px; border-left: 4px solid var(--teltp-brand); }
    .banner mat-icon { color: var(--teltp-accent); font-size: 32px; height: 32px; width: 32px; }
    .banner p { margin: 2px 0 0; }
    .layout { display: grid; grid-template-columns: 320px 1fr; gap: var(--teltp-gap); align-items: start; }
    .outline { padding: 12px; }
    .module + .module { margin-top: 6px; }
    .module-title { font-weight: 600; margin: 10px 8px 4px; font-size: 0.92rem; }
    .lesson { display: flex; align-items: center; gap: 8px; width: 100%; text-align: left; border: 0;
      background: transparent; padding: 8px 8px; border-radius: 8px; cursor: pointer; font: inherit; color: var(--teltp-ink); }
    .lesson:hover { background: rgba(0,0,0,.04); }
    .lesson.active { background: rgba(46,107,65,.10); }
    .lesson .tick { font-size: 18px; height: 18px; width: 18px; color: var(--teltp-muted); }
    .lesson.complete .tick { color: var(--teltp-brand); }
    .lesson-title { flex: 1; font-size: 0.9rem; }
    .mins { font-size: 0.78rem; font-variant-numeric: tabular-nums; }
    .content { padding: 24px; }
    .content h2 { margin: 0 0 12px; }
    .lesson-body { line-height: 1.7; white-space: pre-line; }
    .actions { gap: 12px; margin-top: 20px; flex-wrap: wrap; }
    .empty { text-align: center; padding: 48px; display: flex; flex-direction: column; align-items: center; gap: 10px; }
    .empty mat-icon { font-size: 40px; height: 40px; width: 40px; color: var(--teltp-muted); }
    @media (max-width: 860px) { .layout { grid-template-columns: 1fr; } }
  `],
})
export class CoursePlayerComponent implements OnInit {
  readonly uuid = input.required<string>();   // bound from the route param

  private readonly catalog = inject(CatalogService);
  private readonly progressApi = inject(ProgressService);

  readonly loading = signal(true);
  readonly marking = signal(false);
  readonly curriculum = signal<CourseCurriculumResponse | null>(null);
  readonly progress = signal<CourseProgressResponse | null>(null);
  readonly selected = signal<CurriculumLesson | null>(null);
  // Completed lesson UUIDs, seeded from the backend on load and updated as you mark lessons.
  private readonly completed = signal<Set<string>>(new Set());

  private readonly orderedLessons = computed<CurriculumLesson[]>(() =>
    (this.curriculum()?.modules ?? []).flatMap((m) => m.lessons));

  ngOnInit(): void {
    const id = this.uuid();
    this.catalog.curriculum(id).subscribe({
      next: (cur) => {
        this.curriculum.set(cur);
        this.selected.set(cur.modules[0]?.lessons[0] ?? null);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
    this.progressApi.courseProgress(id).subscribe({ next: (p) => this.progress.set(p) });
    this.progressApi.lessonProgress(id).subscribe({
      next: (rows) => this.completed.set(new Set(rows.filter((r) => r.completed).map((r) => r.lessonUuid))),
    });
  }

  isComplete(lessonUuid: string): boolean {
    return this.completed().has(lessonUuid);
  }

  select(lesson: CurriculumLesson): void {
    this.selected.set(lesson);
  }

  nextLesson(current: CurriculumLesson): CurriculumLesson | null {
    const all = this.orderedLessons();
    const i = all.findIndex((l) => l.uuid === current.uuid);
    return i >= 0 && i < all.length - 1 ? all[i + 1] : null;
  }

  markComplete(lesson: CurriculumLesson): void {
    if (this.isComplete(lesson.uuid)) return;
    this.marking.set(true);
    this.progressApi.markLesson({
      lessonUuid: lesson.uuid, courseUuid: this.uuid(), percentComplete: 100, completed: true,
    }).subscribe({
      next: (p) => {
        this.completed.update((set) => new Set(set).add(lesson.uuid));
        this.progress.set(p);
        this.marking.set(false);
        const nxt = this.nextLesson(lesson);
        if (nxt) this.selected.set(nxt);
      },
      error: () => this.marking.set(false),
    });
  }
}
