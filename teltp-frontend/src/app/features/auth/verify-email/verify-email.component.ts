import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { AuthService } from '../../../core/services/auth.service';
import { AuthFrameComponent } from '../auth-frame/auth-frame.component';

@Component({
  selector: 'app-verify-email',
  standalone: true,
  imports: [
    ReactiveFormsModule, RouterLink, MatFormFieldModule, MatInputModule,
    MatButtonModule, MatProgressBarModule, AuthFrameComponent,
  ],
  template: `
    <app-auth-frame title="Confirm your email" lead="This keeps the account in your name before you sign in.">
      @if (loading()) { <mat-progress-bar mode="indeterminate" /> }
      @if (done()) {
        <p class="notice" role="status">Email confirmed. <a routerLink="/login">Sign in</a></p>
      } @else {
        @if (error()) { <p class="form-error" role="alert">{{ error() }}</p> }
        <form [formGroup]="form" (ngSubmit)="resend()" class="stack">
          <p class="muted">Need another link? Enter your email. We send one only if the account is still unconfirmed.</p>
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Email</mat-label>
            <input matInput type="email" formControlName="email" autocomplete="email" />
          </mat-form-field>
          <button mat-flat-button color="primary" type="submit" [disabled]="form.invalid || loading()">
            {{ resent() ? 'Link sent if needed' : 'Resend confirmation' }}
          </button>
        </form>
      }
      <p class="muted switch"><a routerLink="/login">Back to sign in</a></p>
    </app-auth-frame>
  `,
  styles: [`
    .notice, .form-error { margin: 0 0 12px; padding: 10px 12px; border-radius: 10px; }
    .notice { background: #eef6ee; color: #135c2a; }
    .form-error { background: #fdecec; color: #8a1f1f; }
    .switch { margin: 16px 0 0; }
  `],
})
export class VerifyEmailComponent {
  private readonly auth = inject(AuthService);
  private readonly route = inject(ActivatedRoute);
  private readonly fb = inject(FormBuilder);

  readonly loading = signal(false);
  readonly done = signal(false);
  readonly resent = signal(false);
  readonly error = signal('');
  readonly form = this.fb.nonNullable.group({ email: ['', [Validators.required, Validators.email]] });

  constructor() {
    const token = this.route.snapshot.queryParamMap.get('token');
    if (!token) return;
    this.loading.set(true);
    this.auth.verifyEmail(token).subscribe({
      next: () => { this.loading.set(false); this.done.set(true); },
      error: (err: HttpErrorResponse) => {
        this.loading.set(false);
        this.error.set(err.error?.message ?? 'This confirmation link is invalid or has expired.');
      },
    });
  }

  resend(): void {
    if (this.form.invalid) return;
    this.loading.set(true);
    this.error.set('');
    this.auth.resendVerification(this.form.controls.email.value.trim()).subscribe({
      next: () => { this.loading.set(false); this.resent.set(true); },
      error: (err: HttpErrorResponse) => {
        this.loading.set(false);
        this.error.set(err.error?.message ?? 'Could not resend the link. Try again in a minute.');
      },
    });
  }
}
