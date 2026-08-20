import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar } from '@angular/material/snack-bar';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [
    ReactiveFormsModule, RouterLink, MatCardModule, MatFormFieldModule, MatInputModule,
    MatButtonModule, MatCheckboxModule, MatProgressBarModule,
  ],
  template: `
    <div class="auth-wrap">
      <mat-card class="auth-card surface-card">
        @if (loading()) { <mat-progress-bar mode="indeterminate" /> }
        <h1 class="page-title">Create your account</h1>
        <p class="muted">Register to enrol in courses and earn certificates.</p>

        <form [formGroup]="form" (ngSubmit)="submit()" class="stack">
          <div class="two-col">
            <mat-form-field appearance="outline">
              <mat-label>First name</mat-label>
              <input matInput formControlName="firstName" />
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>Last name</mat-label>
              <input matInput formControlName="lastName" />
            </mat-form-field>
          </div>

          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Username</mat-label>
            <input matInput formControlName="username" autocomplete="username" />
          </mat-form-field>

          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Email</mat-label>
            <input matInput type="email" formControlName="email" autocomplete="email" />
          </mat-form-field>

          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Profession (optional)</mat-label>
            <input matInput formControlName="profession" />
          </mat-form-field>

          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Password</mat-label>
            <input matInput type="password" formControlName="password" autocomplete="new-password" />
            @if (form.controls.password.hasError('minlength')) {
              <mat-error>At least 8 characters</mat-error>
            }
          </mat-form-field>

          <mat-checkbox formControlName="dataProcessingConsent">
            I consent to the processing of my personal data (PDPA)
          </mat-checkbox>

          <button mat-flat-button color="primary" type="submit"
                  [disabled]="form.invalid || loading()">Create account</button>
        </form>

        <p class="muted switch">Already registered? <a routerLink="/login">Sign in</a></p>
      </mat-card>
    </div>
  `,
  styles: [`
    .auth-wrap { max-width: 520px; margin: 48px auto; padding: 0 20px; }
    .auth-card { padding: 28px; }
    .two-col { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
    .switch { margin-top: 18px; }
    mat-progress-bar { margin: -28px -28px 20px; width: calc(100% + 56px); }
    @media (max-width: 520px) { .two-col { grid-template-columns: 1fr; } }
  `],
})
export class RegisterComponent {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly snack = inject(MatSnackBar);

  readonly loading = signal(false);

  readonly form = this.fb.nonNullable.group({
    firstName: [''],
    lastName: [''],
    username: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    profession: [''],
    password: ['', [Validators.required, Validators.minLength(8)]],
    dataProcessingConsent: [false, Validators.requiredTrue],
  });

  submit(): void {
    if (this.form.invalid) return;
    this.loading.set(true);
    this.auth.register(this.form.getRawValue()).subscribe({
      next: () => {
        this.snack.open('Account created — please sign in.', 'OK', { duration: 4000 });
        this.router.navigate(['/login']);
      },
      error: () => this.loading.set(false),
    });
  }
}
