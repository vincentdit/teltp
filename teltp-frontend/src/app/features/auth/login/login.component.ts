import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink, ActivatedRoute } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    ReactiveFormsModule, RouterLink, MatCardModule, MatFormFieldModule,
    MatInputModule, MatButtonModule, MatProgressBarModule,
  ],
  template: `
    <div class="auth-wrap">
      <mat-card class="auth-card surface-card">
        @if (loading()) { <mat-progress-bar mode="indeterminate" /> }
        <h1 class="page-title">Welcome back</h1>
        <p class="muted">Sign in to continue your training.</p>

        <form [formGroup]="form" (ngSubmit)="submit()" class="stack">
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Username</mat-label>
            <input matInput formControlName="username" autocomplete="username" />
          </mat-form-field>

          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Password</mat-label>
            <input matInput type="password" formControlName="password" autocomplete="current-password" />
          </mat-form-field>

          <button mat-flat-button color="primary" type="submit"
                  [disabled]="form.invalid || loading()">Sign in</button>
        </form>

        <p class="muted switch">No account? <a routerLink="/register">Create one</a></p>
      </mat-card>
    </div>
  `,
  styles: [`
    .auth-wrap { max-width: 440px; margin: 56px auto; padding: 0 20px; }
    .auth-card { padding: 28px; }
    .switch { margin-top: 18px; }
    mat-progress-bar { margin: -28px -28px 20px; width: calc(100% + 56px); }
  `],
})
export class LoginComponent {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  readonly loading = signal(false);

  readonly form = this.fb.nonNullable.group({
    username: ['', Validators.required],
    password: ['', Validators.required],
  });

  submit(): void {
    if (this.form.invalid) return;
    this.loading.set(true);
    this.auth.login(this.form.getRawValue()).subscribe({
      next: () => {
        const redirect = this.route.snapshot.queryParamMap.get('redirect') ?? '/dashboard';
        this.router.navigateByUrl(redirect);
      },
      error: () => this.loading.set(false),
    });
  }
}
