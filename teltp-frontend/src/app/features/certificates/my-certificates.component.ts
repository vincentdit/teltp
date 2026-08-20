import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { EnrollmentService } from '../../core/services/enrollment.service';
import { CertificationService } from '../../core/services/certification.service';
import { CertificateResponse } from '../../core/models/certification.model';

@Component({
  selector: 'app-my-certificates',
  standalone: true,
  imports: [RouterLink, MatCardModule, MatButtonModule, MatIconModule, MatProgressSpinnerModule],
  template: `
    <div class="page">
      <h1 class="page-title">My certificates</h1>
      <p class="page-subtitle">Your earned credentials. Each can be verified publicly with its code.</p>

      @if (loading()) {
        <div class="center"><mat-spinner diameter="36" /></div>
      } @else if (certs().length === 0) {
        <div class="surface-card empty">
          <mat-icon>workspace_premium</mat-icon>
          <p class="muted">No certificates yet. Complete a course to earn one.</p>
          <a mat-flat-button color="primary" routerLink="/dashboard">My learning</a>
        </div>
      } @else {
        <div class="card-grid">
          @for (c of certs(); track c.uuid) {
            <mat-card class="surface-card cert">
              <div class="row between">
                <span class="chip" [class.accent]="c.revoked">{{ c.revoked ? 'Revoked' : 'Valid' }}</span>
                <mat-icon class="seal">workspace_premium</mat-icon>
              </div>
              <h3 class="title">{{ c.courseTitle }}</h3>
              <p class="ref mono muted">{{ c.referenceNumber }}</p>
              <dl class="meta">
                <div><dt>Recipient</dt><dd>{{ c.recipientName }}</dd></div>
                <div><dt>Issued</dt><dd>{{ c.issuedOn }}</dd></div>
                @if (c.expiresOn) { <div><dt>Expires</dt><dd>{{ c.expiresOn }}</dd></div> }
                <div><dt>Code</dt><dd class="mono">{{ c.verificationCode }}</dd></div>
              </dl>
              <div class="row actions">
                <button mat-flat-button color="primary" (click)="download(c)" [disabled]="downloading() === c.uuid">
                  <mat-icon>download</mat-icon> PDF
                </button>
                <a mat-stroked-button [routerLink]="['/verify', c.verificationCode]">Verify</a>
              </div>
            </mat-card>
          }
        </div>
      }
    </div>
  `,
  styles: [`
    .center { display: flex; justify-content: center; padding: 60px; }
    .empty { text-align: center; padding: 48px; display: flex; flex-direction: column; align-items: center; gap: 12px; }
    .empty mat-icon { font-size: 40px; height: 40px; width: 40px; color: var(--teltp-muted); }
    .cert { display: flex; flex-direction: column; gap: 8px; border-top: 3px solid var(--teltp-brand); }
    .between { justify-content: space-between; }
    .seal { color: var(--teltp-accent); }
    .title { margin: 4px 0 0; }
    .ref { font-size: 0.8rem; margin: 0; }
    .mono { font-family: 'Spline Sans Mono', ui-monospace, monospace; }
    .meta { margin: 8px 0 4px; display: grid; gap: 6px; }
    .meta div { display: flex; justify-content: space-between; gap: 12px; font-size: 0.88rem; }
    .meta dt { color: var(--teltp-muted); margin: 0; }
    .meta dd { margin: 0; text-align: right; }
    .actions { margin-top: auto; gap: 10px; padding-top: 8px; }
  `],
})
export class MyCertificatesComponent {
  private readonly enrollment = inject(EnrollmentService);
  private readonly certification = inject(CertificationService);
  private readonly snack = inject(MatSnackBar);

  readonly loading = signal(true);
  readonly downloading = signal<string | null>(null);
  readonly certs = signal<CertificateResponse[]>([]);

  constructor() {
    // The JWT carries the username, not the student uuid; derive it from enrollments.
    this.enrollment.myEnrollments(0, 1).subscribe({
      next: (p) => {
        const studentUuid = p.content[0]?.studentUuid;
        if (!studentUuid) { this.loading.set(false); return; }
        this.certification.forStudent(studentUuid).subscribe({
          next: (list) => { this.certs.set(list); this.loading.set(false); },
          error: () => this.loading.set(false),
        });
      },
      error: () => this.loading.set(false),
    });
  }

  download(c: CertificateResponse): void {
    this.downloading.set(c.uuid);
    this.certification.downloadPdf(c.uuid).subscribe({
      next: (blob) => {
        this.downloading.set(null);
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `${c.referenceNumber}.pdf`;
        a.click();
        URL.revokeObjectURL(url);
      },
      error: () => {
        this.downloading.set(null);
        this.snack.open('Could not download the certificate.', 'Dismiss', { duration: 4000 });
      },
    });
  }
}
