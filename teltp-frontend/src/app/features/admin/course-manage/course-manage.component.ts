import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatSnackBar } from '@angular/material/snack-bar';
import { CatalogService } from '../../../core/services/catalog.service';
import { CourseResponse, CourseStatus, DeliveryMode } from '../../../core/models/catalog.model';

@Component({
  selector: 'app-course-manage',
  standalone: true,
  imports: [
    ReactiveFormsModule, MatCardModule, MatFormFieldModule, MatInputModule,
    MatSelectModule, MatButtonModule, MatIconModule, MatChipsModule,
  ],
  template: `
    <div class="page">
      <h1 class="page-title">Course management</h1>
      <p class="page-subtitle">Create a course, build its curriculum, then publish. Publishing requires at least one module.</p>

      <mat-card class="surface-card form-card">
        <h3>New course</h3>
        <form [formGroup]="form" (ngSubmit)="create()" class="stack">
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Title</mat-label>
            <input matInput formControlName="title" />
          </mat-form-field>

          <div class="two-col">
            <mat-form-field appearance="outline">
              <mat-label>Delivery mode</mat-label>
              <mat-select formControlName="deliveryMode">
                <mat-option value="ONLINE">Online</mat-option>
                <mat-option value="IN_PERSON">In person</mat-option>
                <mat-option value="HYBRID">Hybrid</mat-option>
              </mat-select>
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>Duration (hours)</mat-label>
              <input matInput type="number" formControlName="durationHours" />
            </mat-form-field>
          </div>

          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Description</mat-label>
            <textarea matInput rows="3" formControlName="description"></textarea>
          </mat-form-field>

          <button mat-flat-button color="primary" type="submit" [disabled]="form.invalid || saving()">
            <mat-icon>add</mat-icon> Create course
          </button>
        </form>
      </mat-card>

      @if (created().length > 0) {
        <h3 class="section">Created this session</h3>
        <div class="stack">
          @for (c of created(); track c.uuid) {
            <mat-card class="surface-card row line">
              <div class="info">
                <strong>{{ c.title }}</strong>
                <span class="muted ref">{{ c.referenceNumber }}</span>
              </div>
              <span class="chip" [class.accent]="c.status !== 'PUBLISHED'">{{ c.status }}</span>
              <span class="spacer"></span>
              @if (c.status !== 'PUBLISHED') {
                <button mat-stroked-button (click)="transition(c, 'PUBLISHED')">Publish</button>
              } @else {
                <button mat-stroked-button (click)="transition(c, 'ARCHIVED')">Archive</button>
              }
            </mat-card>
          }
        </div>
        <p class="muted hint">
          Tip: publishing needs at least one module — add modules and lessons via the
          <code>/catalog/modules</code> and <code>/catalog/lessons</code> endpoints (curriculum UI is the next slice).
        </p>
      }
    </div>
  `,
  styles: [`
    .form-card { padding: 22px; margin-bottom: 28px; }
    .form-card h3 { margin-top: 0; }
    .two-col { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
    .section { margin: 8px 0 12px; }
    .line { padding: 14px 18px; gap: 14px; }
    .info { display: flex; flex-direction: column; }
    .ref { font-size: 0.76rem; font-variant-numeric: tabular-nums; }
    .hint { margin-top: 16px; font-size: 0.85rem; }
    @media (max-width: 520px) { .two-col { grid-template-columns: 1fr; } }
  `],
})
export class CourseManageComponent {
  private readonly fb = inject(FormBuilder);
  private readonly catalog = inject(CatalogService);
  private readonly snack = inject(MatSnackBar);

  readonly saving = signal(false);
  readonly created = signal<CourseResponse[]>([]);

  readonly form = this.fb.nonNullable.group({
    title: ['', Validators.required],
    deliveryMode: ['ONLINE' as DeliveryMode, Validators.required],
    durationHours: [null as number | null],
    description: [''],
  });

  create(): void {
    if (this.form.invalid) return;
    this.saving.set(true);
    const v = this.form.getRawValue();
    this.catalog.createCourse({
      title: v.title,
      deliveryMode: v.deliveryMode,
      durationHours: v.durationHours ?? undefined,
      description: v.description || undefined,
    }).subscribe({
      next: (c) => {
        this.created.update((list) => [c, ...list]);
        this.form.reset({ deliveryMode: 'ONLINE', durationHours: null, title: '', description: '' });
        this.saving.set(false);
        this.snack.open('Course created', 'OK', { duration: 3000 });
      },
      error: () => this.saving.set(false),
    });
  }

  transition(c: CourseResponse, target: CourseStatus): void {
    this.catalog.transition(c.uuid, { targetStatus: target }).subscribe({
      next: (updated) =>
        this.created.update((list) => list.map((x) => (x.uuid === updated.uuid ? updated : x))),
    });
  }
}
