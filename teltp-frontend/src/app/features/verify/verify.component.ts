import { Component, OnInit, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { CertificationService } from '../../core/services/certification.service';
import { VerificationResult } from '../../core/models/certification.model';

@Component({
  selector: 'app-verify',
  standalone: true,
  imports: [
    FormsModule, RouterLink, MatCardModule, MatFormFieldModule, MatInputModule,
    MatButtonModule, MatIconModule, MatProgressSpinnerModule,
  ],
  template: `
    <div class="verify-shell">
      <div class="brand">
        <span class="brand-mark">TeLTP</span>
        <span class="brand-sub">Certificate Verification</span>
      </div>

      <mat-card class="surface-card panel">
        <p class="muted intro">Enter a certificate's verification code to confirm it's authentic.</p>
        <div class="row form">
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Verification code</mat-label>
            <input matInput [(ngModel)]="code" (keyup.enter)="check()" placeholder="e.g. 3F9A1C2B7E4D8056" />
          </mat-form-field>
          <button mat-flat-button color="primary" (click)="check()" [disabled]="loading() || !code.trim()">
            Verify
          </button>
        </div>

        @if (loading()) {
          <div class="center"><mat-spinner diameter="32" /></div>
        } @else if (result(); as r) {
          <div class="result" [class.ok]="r.valid" [class.bad]="!r.valid">
            <mat-icon>{{ r.valid ? 'verified' : 'gpp_bad' }}</mat-icon>
            <div class="result-body">
              <strong>{{ r.valid ? 'Valid certificate' : 'Not valid' }}</strong>
              <span class="status muted">{{ statusLabel(r.status) }}</span>
              @if (r.valid) {
                <dl class="meta">
                  <div><dt>Recipient</dt><dd>{{ r.recipientName }}</dd></div>
                  <div><dt>Course</dt><dd>{{ r.courseTitle }}</dd></div>
                  <div><dt>Issued</dt><dd>{{ r.issuedOn }}</dd></div>
                  @if (r.expiresOn) { <div><dt>Expires</dt><dd>{{ r.expiresOn }}</dd></div> }
                  @if (r.accreditingBody) { <div><dt>Accredited by</dt><dd>{{ r.accreditingBody }}</dd></div> }
                </dl>
              }
            </div>
          </div>
        }
      </mat-card>

      <a class="muted home" routerLink="/catalog">&larr; Back to TeLTP</a>
    </div>
  `,
  styles: [`
    .verify-shell { max-width: 560px; margin: 0 auto; padding: 48px 20px; }
    .brand { text-align: center; margin-bottom: 20px; display: flex; flex-direction: column; gap: 2px; }
    .brand-mark { font-family: 'Spectral', Georgia, serif; font-weight: 700; font-size: 1.6rem; color: var(--teltp-brand-dark); }
    .brand-sub { font-size: 0.72rem; letter-spacing: 0.16em; text-transform: uppercase; color: var(--teltp-accent); }
    .panel { padding: 24px; }
    .intro { margin: 0 0 12px; }
    .form { gap: 10px; align-items: flex-start; }
    .center { display: flex; justify-content: center; padding: 24px; }
    .result { display: flex; gap: 14px; align-items: flex-start; padding: 16px; border-radius: var(--teltp-radius); margin-top: 8px; }
    .result.ok { background: rgba(46,107,65,.08); }
    .result.bad { background: rgba(185,70,50,.08); }
    .result mat-icon { font-size: 32px; height: 32px; width: 32px; }
    .result.ok mat-icon { color: var(--teltp-brand); }
    .result.bad mat-icon { color: #a33; }
    .result-body { display: flex; flex-direction: column; gap: 2px; flex: 1; }
    .status { font-size: 0.8rem; }
    .meta { margin: 10px 0 0; display: grid; gap: 6px; }
    .meta div { display: flex; justify-content: space-between; gap: 12px; font-size: 0.88rem; }
    .meta dt { color: var(--teltp-muted); margin: 0; }
    .meta dd { margin: 0; text-align: right; }
    .home { display: block; text-align: center; margin-top: 20px; font-size: 0.9rem; }
  `],
})
export class VerifyComponent implements OnInit {
  // optional route param /verify/:code — bound via withComponentInputBinding()
  readonly codeParam = input<string>('', { alias: 'code' });

  private readonly certification = inject(CertificationService);

  code = '';
  readonly loading = signal(false);
  readonly result = signal<VerificationResult | null>(null);

  ngOnInit(): void {
    const c = this.codeParam();
    if (c) { this.code = c; this.check(); }
  }

  check(): void {
    const code = this.code.trim();
    if (!code) return;
    this.loading.set(true);
    this.result.set(null);
    this.certification.verify(code).subscribe({
      next: (r) => { this.result.set(r); this.loading.set(false); },
      error: () => { this.result.set({ valid: false, status: 'NOT_FOUND' }); this.loading.set(false); },
    });
  }

  statusLabel(s: string): string {
    return {
      VALID: 'This certificate is active and authentic.',
      EXPIRED: 'This certificate has expired.',
      REVOKED: 'This certificate has been revoked.',
      NOT_FOUND: 'No certificate matches this code.',
    }[s] ?? s;
  }
}
