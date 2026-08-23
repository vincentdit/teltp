import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { OrganizationService } from '../../../core/services/organization.service';
import { ORG_SUBTYPES, ORG_TYPES, OrganizationResponse } from '../../../core/models/organization.model';

@Component({
  selector: 'app-organizations-manage',
  standalone: true,
  imports: [
    RouterLink, ReactiveFormsModule, MatCardModule, MatFormFieldModule, MatInputModule,
    MatSelectModule, MatButtonModule, MatIconModule, MatProgressSpinnerModule,
  ],
  template: `
    <div class="page">
      <a class="muted back" routerLink="/admin"><mat-icon>arrow_back</mat-icon> Administration</a>
      <h1 class="page-title">Organizations</h1>
      <p class="page-subtitle">Corporate and institutional clients whose learners you can enrol in bulk.</p>

      <div class="cols">
        <mat-card class="surface-card">
          <h3>Registered organizations</h3>
          @if (loading()) {
            <div class="center"><mat-spinner diameter="32" /></div>
          } @else if (orgs().length === 0) {
            <p class="muted">None yet. Add one on the right.</p>
          } @else {
            <div class="stack">
              @for (o of orgs(); track o.uuid) {
                <div class="org">
                  <div>
                    <strong>{{ o.name }}</strong>
                    <span class="muted">{{ pretty(o.type) }}@if (o.region) { · {{ o.region }} }@if (o.tin) { · TIN {{ o.tin }} }</span>
                  </div>
                  <span class="spacer"></span>
                  @if (o.contactEmail) { <span class="chip">{{ o.contactEmail }}</span> }
                </div>
              }
            </div>
          }
        </mat-card>

        <mat-card class="surface-card form-card">
          <h3>New organization</h3>
          <form [formGroup]="form" (ngSubmit)="create()" class="stack">
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Name</mat-label>
              <input matInput formControlName="name" />
            </mat-form-field>
            <div class="two">
              <mat-form-field appearance="outline">
                <mat-label>Type</mat-label>
                <mat-select formControlName="type">
                  @for (t of orgTypes; track t) { <mat-option [value]="t">{{ pretty(t) }}</mat-option> }
                </mat-select>
              </mat-form-field>
              <mat-form-field appearance="outline">
                <mat-label>Sub-type (optional)</mat-label>
                <mat-select formControlName="subType">
                  <mat-option [value]="null">— none —</mat-option>
                  @for (st of orgSubTypes; track st) { <mat-option [value]="st">{{ pretty(st) }}</mat-option> }
                </mat-select>
              </mat-form-field>
            </div>
            <div class="two">
              <mat-form-field appearance="outline">
                <mat-label>Contact email</mat-label>
                <input matInput formControlName="contactEmail" type="email" />
              </mat-form-field>
              <mat-form-field appearance="outline">
                <mat-label>Contact phone</mat-label>
                <input matInput formControlName="contactPhone" />
              </mat-form-field>
            </div>
            <div class="two">
              <mat-form-field appearance="outline">
                <mat-label>Region</mat-label>
                <input matInput formControlName="region" />
              </mat-form-field>
              <mat-form-field appearance="outline">
                <mat-label>District</mat-label>
                <input matInput formControlName="district" />
              </mat-form-field>
            </div>
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>TIN (optional)</mat-label>
              <input matInput formControlName="tin" />
            </mat-form-field>
            <button mat-flat-button color="primary" type="submit" [disabled]="form.invalid || busy()">
              Add organization
            </button>
          </form>
        </mat-card>
      </div>
    </div>
  `,
  styles: [`
    .back { display: inline-flex; align-items: center; gap: 4px; font-size: 0.9rem; text-decoration: none; margin-bottom: 8px; }
    .back mat-icon { font-size: 18px; height: 18px; width: 18px; }
    .center { display: flex; justify-content: center; padding: 40px; }
    .form-card { padding: 20px 22px; }
    .full-width { width: 100%; }
    .two { display: grid; grid-template-columns: 1fr 1fr; gap: 0 16px; }
    .cols { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; align-items: start; }
    .org { display: flex; align-items: center; gap: 12px; padding: 12px 14px; border: 1px solid var(--teltp-line); border-radius: 10px; }
    .org div { display: flex; flex-direction: column; }
    .org .muted { font-size: 0.85rem; }
    @media (max-width: 900px) { .cols, .two { grid-template-columns: 1fr; } }
  `],
})
export class OrganizationsManageComponent {
  private readonly orgApi = inject(OrganizationService);
  private readonly fb = inject(FormBuilder);
  private readonly snack = inject(MatSnackBar);

  readonly orgTypes = ORG_TYPES;
  readonly orgSubTypes = ORG_SUBTYPES;
  readonly loading = signal(true);
  readonly busy = signal(false);
  readonly orgs = signal<OrganizationResponse[]>([]);

  readonly form = this.fb.group({
    name: ['', Validators.required],
    type: ['GOVERNMENT', Validators.required],
    subType: [null as string | null],
    contactEmail: ['', Validators.email],
    contactPhone: [''],
    region: [''],
    district: [''],
    tin: [''],
  });

  constructor() { this.load(); }

  private load(): void {
    this.loading.set(true);
    this.orgApi.list(0, 200).subscribe({
      next: (p) => { this.orgs.set(p.content); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }

  create(): void {
    if (this.form.invalid) return;
    const v = this.form.getRawValue();
    this.busy.set(true);
    this.orgApi.create({
      name: v.name!, type: v.type as never,
      subType: (v.subType as never) || undefined,
      contactEmail: v.contactEmail || undefined,
      contactPhone: v.contactPhone || undefined,
      region: v.region || undefined,
      district: v.district || undefined,
      tin: v.tin || undefined,
    }).subscribe({
      next: (o) => {
        this.busy.set(false);
        this.snack.open('Organization added.', 'Dismiss', { duration: 2500 });
        this.form.reset({ type: 'GOVERNMENT' });
        this.orgs.update((list) => [o, ...list]);
      },
      error: (e) => { this.busy.set(false); this.snack.open(e?.error?.message || 'Could not create.', 'Dismiss', { duration: 4000 }); },
    });
  }

  pretty(s: string): string {
    return s.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, (c) => c.toUpperCase());
  }
}
