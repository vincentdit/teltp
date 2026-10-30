import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, MatCardModule, MatFormFieldModule, MatInputModule, MatButtonModule],
  template: `
    <div class="auth-wrap">
      <mat-card class="auth-card surface-card">
        <h1 class="page-title">Reset password</h1>
        <p class="muted">Enter the email on your account. If mail is configured, a link is sent. Otherwise ask an administrator.</p>
        @if (sent()) {
          <p>If that email is registered, a reset link has been sent.</p>
        } @else {
          <form [formGroup]="form" (ngSubmit)="submit()" class="stack">
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Email</mat-label>
              <input matInput type="email" formControlName="email" autocomplete="email" />
            </mat-form-field>
            <button mat-flat-button color="primary" type="submit" [disabled]="form.invalid">Send link</button>
          </form>
        }
        <p class="muted switch"><a routerLink="/login">Back to sign in</a></p>
      </mat-card>
    </div>
  `,
  styles: [`.auth-wrap { max-width: 440px; margin: 56px auto; padding: 0 20px; } .auth-card { padding: 28px; } .switch { margin-top: 18px; }`],
})
export class ForgotPasswordComponent {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  readonly sent = signal(false);
  readonly form = this.fb.nonNullable.group({ email: ['', [Validators.required, Validators.email]] });

  submit(): void {
    this.auth.forgotPassword(this.form.controls.email.value).subscribe(() => this.sent.set(true));
  }
}
