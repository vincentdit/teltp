import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { AuthService } from '../../../core/services/auth.service';
import { AuthFrameComponent } from '../auth-frame/auth-frame.component';
import { PASSWORD_HINT, passwordLabel, passwordScore, passwordsMatch, strongPassword } from '../password-rules';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [
    ReactiveFormsModule, RouterLink, MatFormFieldModule, MatInputModule, MatButtonModule,
    MatIconModule, MatCheckboxModule, MatProgressBarModule, AuthFrameComponent,
  ],
  template: `
    <app-auth-frame title="Create your account" lead="Join the TIRDO training hub. It takes a minute.">
      @if (loading()) { <mat-progress-bar mode="indeterminate" /> }
      @if (error()) { <p class="form-error" role="alert">{{ error() }}</p> }

      <form [formGroup]="form" (ngSubmit)="submit()" class="stack">
        <div class="two-col">
          <mat-form-field appearance="outline">
            <mat-label>First name</mat-label>
            <input matInput formControlName="firstName" autocomplete="given-name" />
            @if (form.controls.firstName.touched && form.controls.firstName.hasError('required')) {
              <mat-error>Enter your first name.</mat-error>
            }
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Last name</mat-label>
            <input matInput formControlName="lastName" autocomplete="family-name" />
            @if (form.controls.lastName.touched && form.controls.lastName.hasError('required')) {
              <mat-error>Enter your last name.</mat-error>
            }
          </mat-form-field>
        </div>

        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Username</mat-label>
          <input matInput formControlName="username" autocomplete="username" />
          <mat-hint>You can sign in with this or your email.</mat-hint>
          @if (form.controls.username.touched && form.controls.username.hasError('required')) {
            <mat-error>Choose a username.</mat-error>
          }
        </mat-form-field>

        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Email</mat-label>
          <input matInput type="email" formControlName="email" autocomplete="email" />
          @if (form.controls.email.touched && form.controls.email.invalid) {
            <mat-error>Enter a valid email address.</mat-error>
          }
        </mat-form-field>

        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Profession (optional)</mat-label>
          <input matInput formControlName="profession" />
        </mat-form-field>

        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Password</mat-label>
          <input matInput [type]="showPassword() ? 'text' : 'password'" formControlName="password"
                 autocomplete="new-password" />
          <button mat-icon-button matSuffix type="button" (click)="showPassword.set(!showPassword())"
                  [attr.aria-label]="showPassword() ? 'Hide password' : 'Show password'">
            <mat-icon>{{ showPassword() ? 'visibility_off' : 'visibility' }}</mat-icon>
          </button>
          <mat-hint>{{ hint }}</mat-hint>
          @if (form.controls.password.touched && form.controls.password.hasError('strong')) {
            <mat-error>{{ hint }}</mat-error>
          }
        </mat-form-field>

        @if (form.controls.password.value) {
          <div class="meter" [attr.data-score]="score()" aria-live="polite">
            <span></span>
            <em>{{ label() }}</em>
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

        <mat-checkbox formControlName="dataProcessingConsent">
          I agree to the Terms and Conditions and consent to the processing of my personal data.
        </mat-checkbox>
        @if (form.controls.dataProcessingConsent.touched && form.controls.dataProcessingConsent.invalid) {
          <p class="form-error" role="alert">Accept the terms to create an account.</p>
        }

        <button mat-flat-button color="primary" type="submit" [disabled]="form.invalid || loading()">
          {{ loading() ? 'Creating account…' : 'Create account' }}
        </button>
      </form>

      <p class="muted switch">Already registered? <a routerLink="/login">Sign in</a></p>
    </app-auth-frame>
  `,
  styles: [`
    .two-col { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
    .form-error { margin: 0 0 12px; padding: 10px 12px; border-radius: 10px; background: #fdecec; color: #8a1f1f; }
    .switch { margin: 16px 0 0; }
    .meter { display: flex; align-items: center; gap: 10px; margin-top: -4px; }
    .meter span {
      flex: 1; height: 6px; border-radius: 99px; background: #e7e4da; position: relative; overflow: hidden;
    }
    .meter span::after {
      content: ""; position: absolute; inset: 0 auto 0 0; width: 25%; background: #b42318;
    }
    .meter[data-score="2"] span::after { width: 50%; background: #b54708; }
    .meter[data-score="3"] span::after { width: 75%; background: #b08900; }
    .meter[data-score="4"] span::after { width: 100%; background: #067647; }
    .meter em { font-style: normal; font-size: 0.85rem; color: var(--teltp-muted); min-width: 4.5rem; }
    @media (max-width: 520px) { .two-col { grid-template-columns: 1fr; } }
  `],
})
export class RegisterComponent {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly hint = PASSWORD_HINT;
  readonly loading = signal(false);
  readonly error = signal('');
  readonly showPassword = signal(false);
  readonly showConfirm = signal(false);

  readonly form = this.fb.nonNullable.group({
    firstName: ['', Validators.required],
    lastName: ['', Validators.required],
    username: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    profession: [''],
    password: ['', [Validators.required, strongPassword]],
    confirm: ['', Validators.required],
    dataProcessingConsent: [false, Validators.requiredTrue],
  }, { validators: passwordsMatch });

  score(): number {
    return passwordScore(this.form.controls.password.value);
  }

  label(): string {
    return passwordLabel(this.score());
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.loading.set(true);
    this.error.set('');
    const raw = this.form.getRawValue();
    this.auth.register({
      username: raw.username.trim(),
      email: raw.email.trim(),
      password: raw.password,
      firstName: raw.firstName.trim(),
      lastName: raw.lastName.trim(),
      profession: raw.profession.trim() || undefined,
      dataProcessingConsent: raw.dataProcessingConsent,
    }).subscribe({
      next: (user) => {
        this.router.navigate(['/login'], { queryParams: { created: user.emailVerified ? 'ready' : 'confirm' } });
      },
      error: (err: HttpErrorResponse) => {
        this.loading.set(false);
        this.error.set(err.error?.message ?? 'Could not create the account. Check the form and try again.');
      },
    });
  }
}
