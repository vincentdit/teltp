import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { AuthService } from '../../../core/services/auth.service';
import { AuthFrameComponent } from '../auth-frame/auth-frame.component';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    ReactiveFormsModule, RouterLink, MatFormFieldModule, MatInputModule, MatButtonModule,
    MatIconModule, MatCheckboxModule, MatProgressBarModule, AuthFrameComponent,
  ],
  template: `
    <app-auth-frame title="Welcome back" lead="Sign in with your username or email to continue your training.">
      @if (loading()) { <mat-progress-bar mode="indeterminate" /> }
      @if (notice()) { <p class="notice" role="status">{{ notice() }}</p> }
      @if (error()) { <p class="form-error" role="alert">{{ error() }}</p> }

      <form [formGroup]="form" (ngSubmit)="submit()" class="stack">
        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Username or email</mat-label>
          <input matInput formControlName="username" autocomplete="username" />
          @if (form.controls.username.touched && form.controls.username.hasError('required')) {
            <mat-error>Enter your username or email.</mat-error>
          }
        </mat-form-field>

        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Password</mat-label>
          <input matInput [type]="showPassword() ? 'text' : 'password'" formControlName="password"
                 autocomplete="current-password" />
          <button mat-icon-button matSuffix type="button" (click)="showPassword.set(!showPassword())"
                  [attr.aria-label]="showPassword() ? 'Hide password' : 'Show password'">
            <mat-icon>{{ showPassword() ? 'visibility_off' : 'visibility' }}</mat-icon>
          </button>
          @if (form.controls.password.touched && form.controls.password.hasError('required')) {
            <mat-error>Enter your password.</mat-error>
          }
        </mat-form-field>

        @if (mfaChallenge()) {
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Authenticator code</mat-label>
            <input matInput formControlName="code" inputmode="numeric" autocomplete="one-time-code" />
          </mat-form-field>
        }

        <div class="row between">
          <mat-checkbox formControlName="rememberMe">Remember this device</mat-checkbox>
          <a routerLink="/forgot-password">Forgot password?</a>
        </div>

        <button mat-flat-button color="primary" type="submit" [disabled]="form.invalid || loading()">
          {{ loading() ? 'Signing in…' : (mfaChallenge() ? 'Verify code' : 'Sign in') }}
        </button>
      </form>

      <p class="muted switch">New to TeLTP? <a routerLink="/register">Create an account</a></p>
    </app-auth-frame>
  `,
  styles: [`
    .notice, .form-error { margin: 0 0 12px; padding: 10px 12px; border-radius: 10px; }
    .notice { background: #eef6ee; color: #135c2a; }
    .form-error { background: #fdecec; color: #8a1f1f; }
    .between { justify-content: space-between; flex-wrap: wrap; }
    .switch { margin: 16px 0 0; }
  `],
})
export class LoginComponent {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  readonly loading = signal(false);
  readonly error = signal('');
  readonly showPassword = signal(false);
  readonly mfaChallenge = signal<string | null>(null);
  readonly notice = signal('');

  readonly form = this.fb.nonNullable.group({
    username: ['', Validators.required],
    password: ['', Validators.required],
    code: [''],
    rememberMe: [false],
  });

  constructor() {
    const created = this.route.snapshot.queryParamMap.get('created');
    if (created === 'ready') this.notice.set('Account created. Sign in to open your dashboard.');
    if (created === 'confirm') this.notice.set('Account created. Confirm the email we sent, then sign in.');
    if (this.route.snapshot.queryParamMap.get('reset') === '1') {
      this.notice.set('Password updated. Sign in with your new password.');
    }
  }

  submit(): void {
    if (this.form.invalid) return;
    this.loading.set(true);
    this.error.set('');
    const done = () => this.router.navigateByUrl(this.destination());
    const challenge = this.mfaChallenge();
    if (challenge) {
      this.auth.verifyMfa(challenge, this.form.controls.code.value, this.form.controls.rememberMe.value).subscribe({
        next: done,
        error: (err: HttpErrorResponse) => this.fail(err),
      });
      return;
    }
    const { username, password, rememberMe } = this.form.getRawValue();
    this.auth.login({ username: username.trim(), password, rememberMe }).subscribe({
      next: (t) => {
        if (t.mfaRequired && t.mfaChallenge) {
          this.mfaChallenge.set(t.mfaChallenge);
          this.loading.set(false);
          this.notice.set('Enter the 6-digit code from your authenticator app.');
          return;
        }
        done();
      },
      error: (err: HttpErrorResponse) => this.fail(err),
    });
  }

  private fail(err: HttpErrorResponse): void {
    this.loading.set(false);
    this.error.set(err.error?.message ?? 'Sign-in failed. Check your details and try again.');
  }

  private destination(): string {
    const redirect = this.route.snapshot.queryParamMap.get('redirect');
    if (redirect && redirect.startsWith('/') && !redirect.startsWith('//') && !redirect.startsWith('/login')) {
      return redirect;
    }
    const roles = this.auth.roles();
    if (roles.includes('ADMIN') || roles.includes('INSTRUCTOR')) return '/admin';
    if (roles.includes('FINANCE_OFFICER')) return '/admin/billing';
    return '/dashboard';
  }
}
