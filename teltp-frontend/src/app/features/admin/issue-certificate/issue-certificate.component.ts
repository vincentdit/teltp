import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { CatalogService } from '../../../core/services/catalog.service';
import { CertificationService } from '../../../core/services/certification.service';
import { CourseResponse } from '../../../core/models/catalog.model';
import { CertificateResponse } from '../../../core/models/certification.model';

@Component({
  selector: 'app-issue-certificate',
  standalone: true,
  imports: [
    FormsModule, RouterLink, MatCardModule, MatFormFieldModule, MatInputModule,
    MatSelectModule, MatButtonModule, MatIconModule,
  ],
  template: `
    <div class="page narrow">
      <a class="muted back" routerLink="/admin"><mat-icon>arrow_back</mat-icon> Administration</a>
      <h1 class="page-title">Issue a certificate</h1>
      <p class="page-subtitle">Issue a credential to a student who has completed a course.</p>

      <mat-card class="surface-card form">
        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Course</mat-label>
          <mat-select [(ngModel)]="courseUuid">
            @for (c of courses(); track c.uuid) {
              <mat-option [value]="c.uuid">{{ c.title }} — {{ c.referenceNumber }}</mat-option>
            }
          </mat-select>
        </mat-form-field>

        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Student UUID</mat-label>
          <input matInput [(ngModel)]="studentUuid" placeholder="the learner's user uuid" />
          <mat-hint>The student must have completed this course.</mat-hint>
        </mat-form-field>

        <div class="two">
          <mat-form-field appearance="outline">
            <mat-label>Accrediting body (optional)</mat-label>
            <input matInput [(ngModel)]="accreditingBody" placeholder="e.g. NACTE" />
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Accreditation level (optional)</mat-label>
            <input matInput [(ngModel)]="accreditationLevel" />
          </mat-form-field>
        </div>

        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Expires on (optional)</mat-label>
          <input matInput type="date" [(ngModel)]="expiresOn" />
        </mat-form-field>

        @if (error()) { <p class="msg bad"><mat-icon>error</mat-icon> {{ error() }}</p> }

        <button mat-flat-button color="primary" (click)="submit()" [disabled]="busy() || !courseUuid || !studentUuid.trim()">
          <mat-icon>workspace_premium</mat-icon> Issue certificate
        </button>
      </mat-card>

      @if (issued(); as c) {
        <mat-card class="surface-card result">
          <div class="row"><mat-icon class="ok">check_circle</mat-icon><strong>Certificate issued</strong></div>
          <dl class="meta">
            <div><dt>Recipient</dt><dd>{{ c.recipientName }}</dd></div>
            <div><dt>Course</dt><dd>{{ c.courseTitle }}</dd></div>
            <div><dt>Reference</dt><dd class="mono">{{ c.referenceNumber }}</dd></div>
            <div><dt>Verification code</dt><dd class="mono">{{ c.verificationCode }}</dd></div>
          </dl>
          <a mat-stroked-button [routerLink]="['/verify', c.verificationCode]">Open verification page</a>
        </mat-card>
      }
    </div>
  `,
  styles: [`
    .narrow { max-width: 720px; }
    .back { display: inline-flex; align-items: center; gap: 4px; font-size: 0.9rem; text-decoration: none; margin-bottom: 8px; }
    .back mat-icon { font-size: 18px; height: 18px; width: 18px; }
    .form { display: flex; flex-direction: column; padding: 24px; gap: 4px; }
    .two { display: grid; grid-template-columns: 1fr 1fr; gap: 0 16px; }
    .msg { display: flex; align-items: center; gap: 6px; font-size: 0.9rem; margin: 4px 0 12px; }
    .msg.bad { color: #a33; }
    .msg mat-icon { font-size: 18px; height: 18px; width: 18px; }
    .result { margin-top: 20px; padding: 24px; display: flex; flex-direction: column; gap: 12px; border-top: 3px solid var(--teltp-brand); }
    .result .ok { color: var(--teltp-brand); }
    .mono { font-family: 'Spline Sans Mono', ui-monospace, monospace; }
    .meta { margin: 0; display: grid; gap: 6px; }
    .meta div { display: flex; justify-content: space-between; gap: 12px; font-size: 0.9rem; }
    .meta dt { color: var(--teltp-muted); margin: 0; }
    .meta dd { margin: 0; text-align: right; }
    @media (max-width: 640px) { .two { grid-template-columns: 1fr; } }
  `],
})
export class IssueCertificateComponent {
  private readonly catalog = inject(CatalogService);
  private readonly certification = inject(CertificationService);

  readonly courses = signal<CourseResponse[]>([]);
  courseUuid = '';
  studentUuid = '';
  accreditingBody = '';
  accreditationLevel = '';
  expiresOn = '';

  readonly busy = signal(false);
  readonly error = signal<string | null>(null);
  readonly issued = signal<CertificateResponse | null>(null);

  constructor() {
    this.catalog.publishedCourses(0, 100).subscribe({
      next: (p) => this.courses.set(p.content),
    });
  }

  submit(): void {
    this.busy.set(true);
    this.error.set(null);
    this.issued.set(null);
    this.certification.issue({
      studentUuid: this.studentUuid.trim(),
      courseUuid: this.courseUuid,
      accreditingBody: this.accreditingBody.trim() || undefined,
      accreditationLevel: this.accreditationLevel.trim() || undefined,
      expiresOn: this.expiresOn || undefined,
    }).subscribe({
      next: (c) => { this.issued.set(c); this.busy.set(false); },
      error: (e) => { this.error.set(e?.error?.message || 'Could not issue the certificate.'); this.busy.set(false); },
    });
  }
}
