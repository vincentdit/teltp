import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DatePipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatChipsModule } from '@angular/material/chips';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { CatalogService } from '../../../core/services/catalog.service';
import { EnrollmentService } from '../../../core/services/enrollment.service';
import { CourseResponse } from '../../../core/models/catalog.model';
import { CohortResponse } from '../../../core/models/enrollment.model';
import { OrganizationService } from '../../../core/services/organization.service';
import { UserService } from '../../../core/services/user.service';
import { OrganizationResponse } from '../../../core/models/organization.model';
import { UserResponse } from '../../../core/models/user.model';

@Component({
  selector: 'app-cohorts-manage',
  standalone: true,
  imports: [
    RouterLink, DatePipe, ReactiveFormsModule, MatCardModule, MatFormFieldModule, MatInputModule,
    MatSelectModule, MatChipsModule, MatDatepickerModule, MatNativeDateModule, MatButtonModule, MatIconModule,
    MatProgressSpinnerModule,
  ],
  template: `
    <div class="page">
      <a class="muted back" routerLink="/admin"><mat-icon>arrow_back</mat-icon> Administration</a>
      <h1 class="page-title">Enrolments &amp; cohorts</h1>
      <p class="page-subtitle">Create scheduled cohorts (intakes) for a course and track their capacity.</p>

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
          <mat-card class="surface-card">
            <h3>Cohorts</h3>
            @if (loadingList()) {
              <p class="muted">Loading…</p>
            } @else if (cohorts().length === 0) {
              <p class="muted">No cohorts yet for this course.</p>
            } @else {
              <div class="stack">
                @for (c of cohorts(); track c.uuid) {
                  <div class="cohort">
                    <div>
                      <strong>{{ c.name }}</strong>
                      <span class="muted">
                        @if (c.startDate) { {{ c.startDate | date:'mediumDate' }} }
                        @if (c.startDate && c.endDate) { – }
                        @if (c.endDate) { {{ c.endDate | date:'mediumDate' }} }
                        @if (!c.startDate && !c.endDate) { No dates set }
                      </span>
                    </div>
                    <span class="spacer"></span>
                    <span class="chip">{{ c.activeCount }}@if (c.capacity) { / {{ c.capacity }} } enrolled</span>
                  </div>
                }
              </div>
            }
          </mat-card>

          <mat-card class="surface-card form-card">
            <h3>New cohort</h3>
            <form [formGroup]="form" (ngSubmit)="create()" class="stack">
              <mat-form-field appearance="outline" class="full-width">
                <mat-label>Name</mat-label>
                <input matInput formControlName="name" placeholder="e.g. January 2026 Intake" />
              </mat-form-field>
              <div class="two">
                <mat-form-field appearance="outline">
                  <mat-label>Start date</mat-label>
                  <input matInput [matDatepicker]="sp" formControlName="startDate" />
                  <mat-datepicker-toggle matIconSuffix [for]="sp"></mat-datepicker-toggle>
                  <mat-datepicker #sp></mat-datepicker>
                </mat-form-field>
                <mat-form-field appearance="outline">
                  <mat-label>End date</mat-label>
                  <input matInput [matDatepicker]="ep" formControlName="endDate" />
                  <mat-datepicker-toggle matIconSuffix [for]="ep"></mat-datepicker-toggle>
                  <mat-datepicker #ep></mat-datepicker>
                </mat-form-field>
              </div>
              <mat-form-field appearance="outline" class="full-width">
                <mat-label>Capacity (optional)</mat-label>
                <input matInput type="number" formControlName="capacity" min="1" />
              </mat-form-field>
              <button mat-flat-button color="primary" type="submit" [disabled]="form.invalid || busy()">
                Create cohort
              </button>
            </form>
          </mat-card>
        </div>

        <mat-card class="surface-card form-card assign-card">
          <h3>Assign learners (corporate)</h3>
          <p class="muted small">Bulk-enrol an organization's learners into this course, optionally into a cohort.</p>
          <div class="two">
            <mat-form-field appearance="outline">
              <mat-label>Organization</mat-label>
              <mat-select [(value)]="assignOrg">
                @for (o of orgs(); track o.uuid) { <mat-option [value]="o.uuid">{{ o.name }}</mat-option> }
              </mat-select>
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>Cohort (optional)</mat-label>
              <mat-select [(value)]="assignCohort">
                <mat-option [value]="null">— none —</mat-option>
                @for (c of cohorts(); track c.uuid) { <mat-option [value]="c.uuid">{{ c.name }}</mat-option> }
              </mat-select>
            </mat-form-field>
          </div>
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Learners</mat-label>
            <mat-select [(value)]="assignStudents" multiple>
              @for (s of students(); track s.uuid) {
                <mat-option [value]="s.uuid">{{ s.fullName || s.username }} ({{ s.email }})</mat-option>
              }
            </mat-select>
          </mat-form-field>
          @if (students().length === 0) { <p class="muted small">No student accounts found to assign.</p> }
          <button mat-flat-button color="primary" (click)="assign()"
                  [disabled]="!assignOrg || assignStudents.length === 0 || assigning()">
            Assign {{ assignStudents.length || '' }} learner(s)
          </button>
        </mat-card>
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
    .cohort { display: flex; align-items: center; gap: 12px; padding: 12px 14px; border: 1px solid var(--teltp-line); border-radius: 10px; }
    .cohort div { display: flex; flex-direction: column; }
    .cohort .muted { font-size: 0.85rem; }
    .assign-card { margin-top: 20px; }
    .small { font-size: 0.85rem; margin-top: -4px; }
    @media (max-width: 900px) { .cols, .two { grid-template-columns: 1fr; } }
  `],
})
export class CohortsManageComponent {
  private readonly catalog = inject(CatalogService);
  private readonly enrollment = inject(EnrollmentService);
  private readonly fb = inject(FormBuilder);
  private readonly orgApi = inject(OrganizationService);
  private readonly userApi = inject(UserService);
  private readonly snack = inject(MatSnackBar);

  readonly courses = signal<CourseResponse[]>([]);
  readonly courseUuid = signal<string>('');
  readonly cohorts = signal<CohortResponse[]>([]);
  readonly loadingList = signal(false);
  readonly busy = signal(false);
  readonly assigning = signal(false);
  readonly orgs = signal<OrganizationResponse[]>([]);
  readonly students = signal<UserResponse[]>([]);
  assignOrg: string | null = null;
  assignCohort: string | null = null;
  assignStudents: string[] = [];

  readonly form = this.fb.group({
    name: ['', Validators.required],
    startDate: [null as Date | null],
    endDate: [null as Date | null],
    capacity: [null as number | null],
  });

  constructor() {
    this.catalog.publishedCourses(0, 100).subscribe({ next: (p) => this.courses.set(p.content) });
    this.orgApi.list(0, 200).subscribe({ next: (p) => this.orgs.set(p.content) });
    this.userApi.list(0, 200).subscribe({
      next: (p) => this.students.set(p.content.filter((u) => u.roles.includes('STUDENT'))),
    });
  }

  selectCourse(uuid: string): void { this.courseUuid.set(uuid); this.loadList(); }

  private loadList(): void {
    this.loadingList.set(true);
    this.enrollment.listCohorts(this.courseUuid()).subscribe({
      next: (list) => { this.cohorts.set(list); this.loadingList.set(false); },
      error: () => this.loadingList.set(false),
    });
  }

  private toIso(d: Date | null): string | undefined {
    return d ? new Date(d).toISOString().slice(0, 10) : undefined;
  }

  create(): void {
    if (this.form.invalid || !this.courseUuid()) return;
    const v = this.form.getRawValue();
    this.busy.set(true);
    this.enrollment.createCohort({
      courseUuid: this.courseUuid(),
      name: v.name!,
      startDate: this.toIso(v.startDate),
      endDate: this.toIso(v.endDate),
      capacity: v.capacity ?? undefined,
    }).subscribe({
      next: (c) => {
        this.busy.set(false);
        this.snack.open('Cohort created.', 'Dismiss', { duration: 2500 });
        this.form.reset();
        this.cohorts.update((list) => [...list, c]);
      },
      error: (e) => { this.busy.set(false); this.snack.open(e?.error?.message || 'Could not create cohort.', 'Dismiss', { duration: 4000 }); },
    });
  }

  assign(): void {
    if (!this.assignOrg || this.assignStudents.length === 0 || !this.courseUuid()) return;
    this.assigning.set(true);
    this.enrollment.adminAssign({
      courseUuid: this.courseUuid(),
      organizationUuid: this.assignOrg,
      cohortUuid: this.assignCohort ?? undefined,
      studentUuids: this.assignStudents,
    }).subscribe({
      next: (created) => {
        this.assigning.set(false);
        this.snack.open(`Assigned ${created.length} learner(s).`, 'Dismiss', { duration: 3000 });
        this.assignStudents = [];
        if (this.assignCohort) this.loadList(); // refresh cohort counts
      },
      error: (e) => { this.assigning.set(false); this.snack.open(e?.error?.message || 'Could not assign.', 'Dismiss', { duration: 4000 }); },
    });
  }
}
