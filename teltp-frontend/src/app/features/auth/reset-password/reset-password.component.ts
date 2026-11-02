import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { AuthService } from '../../../core/services/auth.service';
import { AuthFrameComponent } from '../auth-frame/auth-frame.component';
import { PASSWORD_HINT, passwordLabel, passwordScore, passwordsMatch, strongPassword } from '../password-rules';

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [
    ReactiveFormsModule, RouterLink, MatFormFieldModule, MatInputModule, MatButtonModule,
    MatIconModule, MatProgressBarModule, AuthFrameComponent,
  ],
  template: `
    <app-auth-frame title="Choose a new password" [lead]="hint">
      @if (loading()) { <mat-progress-bar mode="indeterminate" /> }
      @if (error()) { <p class="form-error" role="alert">{{ error() }}</p> }
      <form [formGroup]="form" (ngSubmit)="submit()" class="stack">
        <mat-form-field appearance="outline" class="full-width">
          <mat-label>New password</mat-label>
          <input matInput [type]="showPassword() ? 'text' : 'password'" formControlName="password"
                 autocomplete="new-password" />
          <button mat-icon-button matSuffix type="button" (click)="showPassword.set(!showPassword())"
                  [attr.aria-label]="showPassword() ? 'Hide password' : 'Show password'">
            <mat-icon>{{ showPassword() ? 'visibility_off' : 'visibility' }}</mat-icon>
          </button>
          @if (form.controls.password.touched && form.controls.password.hasError('strong')) {
            <mat-error>{{ hint }}</mat-error>
          }
        </mat-form-field>
        @if (form.controls.password.value) {
          <div class="meter" [attr.data-score]="score()" aria-live="polite">
            <span></span><em>{{ label() }}</em>
          </div>
        }
        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Confirm password</mat-label>
          <input matInput [type]="showConfirm() ? 'text' : 'password'" formControlName="confirm"
                 autocomplete="new-password" />
          <button mat-icon-button matSuffix type="button" (click)="showConfirm.set(!showConfirm())"
                  [attr.aria-label]="showConfirm() ? 'Hide confirmation' : 'Show confirmation'">
            <mat-icon>{{ showConfirm() ? 'visibility_off' : 'visibility' }}</mat-icon>
          </button>
        </mat-form-field>
        @if (form.controls.confirm.touched && form.hasError('mismatch')) {
          <p class="form-error" role="alert">Passwords do not match.</p>
        }
        <button mat-flat-button color="primary" type="submit" [disabled]="form.invalid || loading() || !token">
          {{ loading() ? 'Updating…' : 'Update password' }}
        </button>
      </form>
      <p class="muted switch"><a routerLink="/login">Back to sign in</a></p>
    </app-auth-frame>
  `,
  styles: [`
    .form-error { margin: 0 0 12px; padding: 10px 12px; border-radius: 10px; background: #fdecec; color: #8a1f1f; }
    .switch { margin: 16px 0 0; }
    .meter { display: flex; align-items: center; gap: 10px; }
    .meter span { flex: 1; height: 6px; border-radius: 99px; background: #e7e4da; position: relative; overflow: hidden; }
    .meter span::after { content: ""; position: absolute; inset: 0 auto 0 0; width: 25%; background: #b42318; }
    .meter[data-score="2"] span::after { width: 50%; background: #b54708; }
    .meter[data-score="3"] span::after { width: 75%; background: #b08900; }
    .meter[data-score="4"] span::after { width: 100%; background: #067647; }
    .meter em { font-style: normal; font-size: 0.85rem; color: var(--teltp-muted); }
  `],
})
export class ResetPasswordComponent {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly hint = PASSWORD_HINT;
  readonly loading = signal(false);
  readonly error = signal('');
  readonly showPassword = signal(false);
  readonly showConfirm = signal(false);
  readonly token = this.route.snapshot.queryParamMap.get('token') ?? '';

  readonly form = this.fb.nonNullable.group({
    password: ['', [Validators.required, strongPassword]],
    confirm: ['', Validators.required],
  }, { validators: passwordsMatch });

  constructor() {
    if (!this.token) this.error.set('This reset link is missing a token. Request a new one.');
  }

  score(): number { return passwordScore(this.form.controls.password.value); }
  label(): string { return passwordLabel(this.score()); }

  submit(): void {
    if (this.form.invalid || !this.token) {
      this.form.markAllAsTouched();
      return;
    }
    this.loading.set(true);
    this.error.set('');
    this.auth.resetPassword(this.token, this.form.controls.password.value).subscribe({
      next: () => this.router.navigate(['/login'], { queryParams: { reset: '1' } }),
      error: (err: HttpErrorResponse) => {
        this.loading.set(false);
        this.error.set(err.error?.message ?? 'Could not update the password. Request a new link.');
      },
    });
  }
}
