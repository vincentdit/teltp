import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { AuthService } from '../../../core/services/auth.service';
import { AuthFrameComponent } from '../auth-frame/auth-frame.component';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [
    ReactiveFormsModule, RouterLink, MatFormFieldModule, MatInputModule,
    MatButtonModule, MatProgressBarModule, AuthFrameComponent,
  ],
  template: `
    <app-auth-frame title="Reset your password" lead="We will email a link if this address is on an account. The reply is the same either way.">
      @if (loading()) { <mat-progress-bar mode="indeterminate" /> }
      @if (sent()) {
        <p class="notice" role="status">If that email is registered, a reset link has been sent. It expires in 30 minutes.</p>
      } @else {
        @if (error()) { <p class="form-error" role="alert">{{ error() }}</p> }
        <form [formGroup]="form" (ngSubmit)="submit()" class="stack">
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Email</mat-label>
            <input matInput type="email" formControlName="email" autocomplete="email" />
            @if (form.controls.email.touched && form.controls.email.invalid) {
              <mat-error>Enter the email on your account.</mat-error>
            }
          </mat-form-field>
          <button mat-flat-button color="primary" type="submit" [disabled]="form.invalid || loading()">
            {{ loading() ? 'Sending…' : 'Send reset link' }}
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
export class ForgotPasswordComponent {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  readonly loading = signal(false);
  readonly sent = signal(false);
  readonly error = signal('');
  readonly form = this.fb.nonNullable.group({ email: ['', [Validators.required, Validators.email]] });

  submit(): void {
    if (this.form.invalid) return;
    this.loading.set(true);
    this.error.set('');
    this.auth.forgotPassword(this.form.controls.email.value.trim()).subscribe({
      next: () => { this.loading.set(false); this.sent.set(true); },
      error: (err: HttpErrorResponse) => {
        this.loading.set(false);
        this.error.set(err.error?.message ?? 'Could not send the reset link. Try again in a minute.');
      },
    });
  }
}
