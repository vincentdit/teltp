import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSelectModule } from '@angular/material/select';
import { MatChipsModule } from '@angular/material/chips';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { UserService } from '../../../core/services/user.service';
import { TokenService } from '../../../core/services/token.service';
import { ALL_ROLES, RoleName, UserResponse } from '../../../core/models/user.model';

@Component({
  selector: 'app-system-users',
  standalone: true,
  imports: [
    RouterLink, FormsModule, ReactiveFormsModule, MatCardModule, MatButtonModule, MatIconModule,
    MatSlideToggleModule, MatSelectModule, MatChipsModule, MatFormFieldModule, MatInputModule,
    MatCheckboxModule, MatPaginatorModule, MatProgressSpinnerModule, MatTooltipModule,
  ],
  template: `
    <div class="page">
      <a class="muted back" routerLink="/admin"><mat-icon>arrow_back</mat-icon> Administration</a>
      <h1 class="page-title">System users</h1>
      <p class="page-subtitle">Search accounts, create new ones, manage roles, active status, and passwords.</p>

      <div class="toolbar">
        <mat-form-field appearance="outline" class="search-field">
          <mat-label>Search by name, username, or email</mat-label>
          <input matInput [(ngModel)]="query" (keydown.enter)="search()" />
          <button mat-icon-button matSuffix (click)="search()"><mat-icon>search</mat-icon></button>
        </mat-form-field>
        <button mat-flat-button color="primary" (click)="toggleCreate()">
          <mat-icon>{{ creating() ? 'close' : 'person_add' }}</mat-icon>
          {{ creating() ? 'Cancel' : 'Add user' }}
        </button>
      </div>

      @if (creating()) {
        <mat-card class="surface-card form-card">
          <h3>New user</h3>
          <form [formGroup]="form" (ngSubmit)="create()" class="stack">
            <div class="two">
              <mat-form-field appearance="outline">
                <mat-label>Username</mat-label>
                <input matInput formControlName="username" />
              </mat-form-field>
              <mat-form-field appearance="outline">
                <mat-label>Email</mat-label>
                <input matInput type="email" formControlName="email" />
              </mat-form-field>
            </div>
            <div class="two">
              <mat-form-field appearance="outline">
                <mat-label>First name</mat-label>
                <input matInput formControlName="firstName" />
              </mat-form-field>
              <mat-form-field appearance="outline">
                <mat-label>Last name</mat-label>
                <input matInput formControlName="lastName" />
              </mat-form-field>
            </div>
            <div class="two">
              <mat-form-field appearance="outline">
                <mat-label>Phone (optional)</mat-label>
                <input matInput formControlName="phoneNumber" />
              </mat-form-field>
              <mat-form-field appearance="outline">
                <mat-label>Profession (optional)</mat-label>
                <input matInput formControlName="profession" />
              </mat-form-field>
            </div>
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Temporary password</mat-label>
              <input matInput type="text" formControlName="password" />
              <mat-hint>At least 8 characters. Share this with the user so they can log in and change it.</mat-hint>
            </mat-form-field>
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Roles</mat-label>
              <mat-select formControlName="roles" multiple>
                @for (r of allRoles; track r) { <mat-option [value]="r">{{ pretty(r) }}</mat-option> }
              </mat-select>
              <mat-hint>Defaults to Student if none selected.</mat-hint>
            </mat-form-field>
            <mat-checkbox formControlName="dataProcessingConsent">
              User has consented to data processing (required)
            </mat-checkbox>
            @if (createError()) { <p class="msg bad"><mat-icon>error</mat-icon> {{ createError() }}</p> }
            <button mat-flat-button color="primary" type="submit" [disabled]="form.invalid || creatingBusy()">
              Create user
            </button>
          </form>
        </mat-card>
      }

      @if (resetTarget(); as ru) {
        <mat-card class="surface-card form-card reset-card">
          @if (resetRevealed(); as revealed) {
            <h3>Password reset — {{ ru.fullName || ru.username }}</h3>
            <p class="muted small">Share this password with the user directly — there is no automatic email delivery. They should change it after signing in.</p>
            <div class="pwd-reveal">
              <code>{{ revealed }}</code>
              <button mat-icon-button (click)="copyPassword(revealed)" matTooltip="Copy to clipboard">
                <mat-icon>content_copy</mat-icon>
              </button>
            </div>
            <button mat-flat-button color="primary" (click)="closeReset()">Done</button>
          } @else {
            <h3>Reset password — {{ ru.fullName || ru.username }}</h3>
            <p class="muted small">Set a new temporary password for this account. It replaces their current password immediately.</p>
            <div class="pwd-edit">
              <mat-form-field appearance="outline" class="pwd-field">
                <mat-label>New temporary password</mat-label>
                <input matInput [(ngModel)]="resetDraft" [ngModelOptions]="{ standalone: true }" />
                <mat-hint>At least 8 characters.</mat-hint>
              </mat-form-field>
              <button mat-icon-button type="button" (click)="regenerate()" matTooltip="Generate a new one">
                <mat-icon>casino</mat-icon>
              </button>
            </div>
            @if (resetError()) { <p class="msg bad"><mat-icon>error</mat-icon> {{ resetError() }}</p> }
            <div class="row">
              <button mat-flat-button color="primary" (click)="confirmReset(ru)"
                      [disabled]="resetDraft.length < 8 || resetBusy()">Set new password</button>
              <button mat-button (click)="closeReset()">Cancel</button>
            </div>
          }
        </mat-card>
      }

      @if (loading()) {
        <div class="center"><mat-spinner diameter="36" /></div>
      } @else if (users().length === 0) {
        <p class="muted">No users found{{ query ? ' for “' + query + '”' : '' }}.</p>
      } @else {
        <mat-card class="surface-card table-card">
          <div class="urow head">
            <span class="c-user">User</span>
            <span class="c-roles">Roles</span>
            <span class="c-active">Active</span>
            <span class="c-pwd">Password</span>
          </div>
          @for (u of users(); track u.uuid) {
            <div class="urow">
              <div class="c-user">
                <strong>{{ u.fullName || u.username }}@if (isSelf(u)) { <span class="you">you</span> }</strong>
                <span class="muted">{{ u.email }}</span>
              </div>
              <div class="c-roles">
                @if (editing() === u.uuid) {
                  <mat-select multiple [(ngModel)]="draftRoles" class="role-select">
                    @for (r of allRoles; track r) { <mat-option [value]="r">{{ pretty(r) }}</mat-option> }
                  </mat-select>
                  <div class="edit-actions">
                    <button mat-flat-button color="primary" (click)="saveRoles(u)" [disabled]="saving()">Save</button>
                    <button mat-button (click)="cancelEdit()">Cancel</button>
                  </div>
                } @else {
                  <div class="chips">
                    @for (r of u.roles; track r) { <span class="chip">{{ pretty(r) }}</span> }
                    @if (!u.roles.length) { <span class="muted">—</span> }
                  </div>
                  <button mat-button color="primary" (click)="startEdit(u)"><mat-icon>edit</mat-icon> Roles</button>
                }
              </div>
              <div class="c-active">
                <span [matTooltip]="isSelf(u) ? 'You cannot deactivate your own account' : ''"
                      [matTooltipDisabled]="!isSelf(u)">
                  <mat-slide-toggle [checked]="u.active" [disabled]="isSelf(u)"
                                    (change)="toggleActive(u, $event.checked)"></mat-slide-toggle>
                </span>
              </div>
              <div class="c-pwd">
                <button mat-icon-button (click)="startReset(u)" matTooltip="Reset password">
                  <mat-icon>key</mat-icon>
                </button>
              </div>
            </div>
          }
          <mat-paginator [length]="total()" [pageSize]="pageSize" [pageIndex]="pageIndex()"
                         [pageSizeOptions]="[10, 25, 50]" (page)="onPage($event)"></mat-paginator>
        </mat-card>
      }
    </div>
  `,
  styles: [`
    .back { display: inline-flex; align-items: center; gap: 4px; font-size: 0.9rem; text-decoration: none; margin-bottom: 8px; }
    .back mat-icon { font-size: 18px; height: 18px; width: 18px; }
    .center { display: flex; justify-content: center; padding: 60px; }
    .toolbar { display: flex; gap: 12px; align-items: flex-start; margin-bottom: 8px; }
    .search-field { flex: 1; max-width: 420px; }
    .form-card { padding: 20px 22px; margin-bottom: 20px; }
    .full-width { width: 100%; }
    .two { display: grid; grid-template-columns: 1fr 1fr; gap: 0 16px; }
    .small { font-size: 0.85rem; }
    .msg { display: flex; align-items: center; gap: 6px; font-size: 0.9rem; }
    .msg.bad { color: #a33; }
    .msg mat-icon { font-size: 18px; height: 18px; width: 18px; }
    .table-card { padding: 6px 8px; }
    .urow { display: grid; grid-template-columns: 2fr 3fr 80px 90px; gap: 14px; align-items: center; padding: 14px 12px; border-bottom: 1px solid var(--teltp-line); }
    .urow:last-child { border-bottom: none; }
    .urow.head { font-weight: 700; font-size: 0.8rem; text-transform: uppercase; letter-spacing: 0.04em; color: var(--teltp-muted); padding: 8px 12px; }
    .c-user { display: flex; flex-direction: column; }
    .c-user .muted { font-size: 0.85rem; }
    .you { margin-left: 6px; font-size: 0.66rem; font-weight: 700; text-transform: uppercase; letter-spacing: 0.05em; color: var(--teltp-brand); background: rgba(26,77,152,0.10); padding: 1px 6px; border-radius: 999px; vertical-align: middle; }
    .c-roles { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
    .c-pwd { display: flex; justify-content: center; }
    .chips { display: flex; gap: 6px; flex-wrap: wrap; }
    .role-select { min-width: 240px; }
    .edit-actions { display: flex; gap: 6px; }
    .reset-card .pwd-edit { display: flex; align-items: center; gap: 8px; }
    .reset-card .pwd-field { flex: 1; max-width: 320px; }
    .pwd-reveal { display: flex; align-items: center; gap: 10px; margin: 10px 0 16px; }
    .pwd-reveal code { font-size: 1.15rem; font-weight: 700; letter-spacing: 0.06em; background: rgba(26,77,152,0.08); color: var(--teltp-brand); padding: 8px 16px; border-radius: 8px; }
    @media (max-width: 720px) { .urow { grid-template-columns: 1fr; gap: 8px; } .urow.head { display: none; } .two { grid-template-columns: 1fr; } }
  `],
})
export class SystemUsersComponent {
  private readonly userApi = inject(UserService);
  private readonly tokens = inject(TokenService);
  private readonly fb = inject(FormBuilder);
  private readonly snack = inject(MatSnackBar);

  readonly allRoles = ALL_ROLES;
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly users = signal<UserResponse[]>([]);
  readonly editing = signal<string | null>(null);
  draftRoles: RoleName[] = [];

  readonly total = signal(0);
  readonly pageIndex = signal(0);
  readonly pageSize = 25;
  query = '';
  private activeQuery = '';

  readonly creating = signal(false);
  readonly creatingBusy = signal(false);
  readonly createError = signal<string | null>(null);
  readonly form = this.fb.group({
    username: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    firstName: [''],
    lastName: [''],
    phoneNumber: [''],
    profession: [''],
    password: ['', [Validators.required, Validators.minLength(8)]],
    roles: [['STUDENT'] as RoleName[]],
    dataProcessingConsent: [false, Validators.requiredTrue],
  });

  readonly resetTarget = signal<UserResponse | null>(null);
  readonly resetBusy = signal(false);
  readonly resetError = signal<string | null>(null);
  readonly resetRevealed = signal<string | null>(null);
  resetDraft = '';

  constructor() { this.load(); }

  private load(): void {
    this.loading.set(true);
    this.userApi.list(this.pageIndex(), this.pageSize, this.activeQuery || undefined).subscribe({
      next: (p) => {
        this.users.set(p.content);
        this.total.set(p.totalElements);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  search(): void {
    this.activeQuery = this.query.trim();
    this.pageIndex.set(0);
    this.load();
  }

  onPage(e: PageEvent): void {
    this.pageIndex.set(e.pageIndex);
    this.load();
  }

  toggleCreate(): void {
    this.creating.update((v) => !v);
    this.createError.set(null);
    if (this.creating()) this.form.reset({ roles: ['STUDENT'], dataProcessingConsent: false });
  }

  create(): void {
    if (this.form.invalid) return;
    this.createError.set(null);
    this.creatingBusy.set(true);
    const v = this.form.getRawValue();
    const roles = (v.roles && v.roles.length ? v.roles : ['STUDENT']) as RoleName[];
    this.userApi.createUser({
      username: v.username!, email: v.email!, password: v.password!,
      firstName: v.firstName || undefined, lastName: v.lastName || undefined,
      phoneNumber: v.phoneNumber || undefined, profession: v.profession || undefined,
      dataProcessingConsent: v.dataProcessingConsent!,
    }, roles).subscribe({
      next: () => {
        this.creatingBusy.set(false);
        this.creating.set(false);
        this.snack.open('User created.', 'Dismiss', { duration: 3000 });
        this.pageIndex.set(0);
        this.load();
      },
      error: (e) => {
        this.creatingBusy.set(false);
        this.createError.set(e?.error?.message || 'Could not create user.');
      },
    });
  }

  isSelf(u: UserResponse): boolean { return u.username === this.tokens.username(); }

  startEdit(u: UserResponse): void { this.editing.set(u.uuid); this.draftRoles = [...u.roles]; }
  cancelEdit(): void { this.editing.set(null); }

  saveRoles(u: UserResponse): void {
    if (this.isSelf(u) && !this.draftRoles.includes('ADMIN')) {
      this.snack.open('You cannot remove your own ADMIN role.', 'Dismiss', { duration: 4000 });
      return;
    }
    this.saving.set(true);
    this.userApi.assignRoles({ userUuid: u.uuid, roles: this.draftRoles }).subscribe({
      next: (updated) => {
        this.saving.set(false); this.editing.set(null);
        this.users.update((list) => list.map((x) => (x.uuid === u.uuid ? updated : x)));
        this.snack.open('Roles updated.', 'Dismiss', { duration: 2500 });
      },
      error: (e) => { this.saving.set(false); this.snack.open(e?.error?.message || 'Could not update roles.', 'Dismiss', { duration: 4000 }); },
    });
  }

  toggleActive(u: UserResponse, active: boolean): void {
    this.userApi.setActive(u.uuid, active).subscribe({
      next: (updated) => {
        this.users.update((list) => list.map((x) => (x.uuid === u.uuid ? updated : x)));
        this.snack.open(active ? 'Account activated.' : 'Account deactivated.', 'Dismiss', { duration: 2500 });
      },
      error: (e) => {
        this.snack.open(e?.error?.message || 'Could not update.', 'Dismiss', { duration: 4000 });
        this.load(); // revert toggle to server truth
      },
    });
  }

  startReset(u: UserResponse): void {
    this.resetTarget.set(u);
    this.resetRevealed.set(null);
    this.resetError.set(null);
    this.resetDraft = this.generatePassword();
  }

  closeReset(): void {
    this.resetTarget.set(null);
    this.resetRevealed.set(null);
    this.resetDraft = '';
  }

  regenerate(): void { this.resetDraft = this.generatePassword(); }

  private generatePassword(): string {
    const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789';
    let out = '';
    for (let i = 0; i < 12; i++) out += chars[Math.floor(Math.random() * chars.length)];
    return out;
  }

  confirmReset(u: UserResponse): void {
    if (this.resetDraft.length < 8) return;
    this.resetError.set(null);
    this.resetBusy.set(true);
    this.userApi.resetPassword(u.uuid, { newPassword: this.resetDraft }).subscribe({
      next: () => {
        this.resetBusy.set(false);
        this.resetRevealed.set(this.resetDraft);
      },
      error: (e) => {
        this.resetBusy.set(false);
        this.resetError.set(e?.error?.message || 'Could not reset password.');
      },
    });
  }

  copyPassword(pwd: string): void {
    navigator.clipboard?.writeText(pwd).then(
      () => this.snack.open('Password copied.', 'Dismiss', { duration: 2000 }),
      () => this.snack.open('Could not copy — select and copy manually.', 'Dismiss', { duration: 3000 }),
    );
  }

  pretty(r: string): string {
    return r.replace('_', ' ').toLowerCase().replace(/\b\w/g, (c) => c.toUpperCase());
  }
}
