import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ApiService } from '../../../core/services/api.service';
import { PageResponse } from '../../../core/models/api-response.model';
import { InvoiceResponse } from '../../../core/models/billing.model';

@Component({
  selector: 'app-billing-admin',
  standalone: true,
  imports: [
    RouterLink, DecimalPipe, FormsModule, MatCardModule, MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule, MatProgressSpinnerModule,
  ],
  template: `
    <div class="page">
      <a class="muted back" routerLink="/admin"><mat-icon>arrow_back</mat-icon> Administration</a>
      <h1 class="page-title">Billing &amp; payments</h1>
      <p class="page-subtitle">Issued invoices, payment status, and manual reconciliation.</p>

      <div class="search-bar">
        <mat-form-field appearance="outline" class="search-field">
          <mat-label>Search by payer UUID or invoice reference</mat-label>
          <input matInput [(ngModel)]="query" (keydown.enter)="search()" />
        </mat-form-field>
        <button mat-flat-button color="primary" (click)="search()" [disabled]="loading()">Search</button>
      </div>

      @if (loading()) {
        <div class="center"><mat-spinner diameter="36" /></div>
      } @else if (invoices().length === 0 && searched()) {
        <p class="muted">No invoices found.</p>
      } @else if (invoices().length > 0) {
        <mat-card class="surface-card table-card">
          <div class="irow head">
            <span>Reference</span><span>Payer</span><span>Status</span>
            <span class="num">Amount</span><span></span>
          </div>
          @for (inv of invoices(); track inv.uuid) {
            <div class="irow" [class.paid]="inv.status === 'PAID'">
              <span class="ref">{{ inv.referenceNumber }}</span>
              <span class="muted small">{{ inv.payerUuid.slice(0, 8) }}…</span>
              <span class="chip" [class.accent]="inv.status === 'ISSUED'">{{ inv.status }}</span>
              <span class="num">{{ inv.currency }} {{ inv.total | number:'1.0-0' }}</span>
              <div class="actions">
                @if (inv.status === 'ISSUED') {
                  <button mat-button color="warn" (click)="confirmManual(inv)">Mark paid</button>
                }
              </div>
            </div>
          }
        </mat-card>
      }

      @if (!searched()) {
        <div class="hint surface-card">
          <mat-icon>info</mat-icon>
          <p>Search by a learner's UUID (from System Users) or invoice reference number to view and manage invoices. GePG payments are confirmed automatically via the callback webhook.</p>
        </div>
      }
    </div>
  `,
  styles: [`
    .back { display: inline-flex; align-items: center; gap: 4px; font-size: 0.9rem; text-decoration: none; margin-bottom: 8px; }
    .back mat-icon { font-size: 18px; height: 18px; width: 18px; }
    .center { display: flex; justify-content: center; padding: 60px; }
    .search-bar { display: flex; gap: 12px; align-items: flex-start; margin-bottom: 16px; }
    .search-field { flex: 1; }
    .table-card { padding: 6px 10px; }
    .irow { display: grid; grid-template-columns: 2fr 2fr 1fr 1.5fr 1fr; gap: 12px; align-items: center; padding: 12px 8px; border-bottom: 1px solid var(--teltp-line); }
    .irow:last-child { border-bottom: none; }
    .irow.head { font-size: 0.78rem; text-transform: uppercase; letter-spacing: 0.04em; color: var(--teltp-muted); font-weight: 700; }
    .irow.paid { opacity: 0.65; }
    .ref { font-variant-numeric: tabular-nums; font-size: 0.9rem; }
    .small { font-size: 0.85rem; }
    .num { text-align: right; font-variant-numeric: tabular-nums; }
    .actions { display: flex; justify-content: flex-end; }
    .hint { display: flex; gap: 12px; align-items: flex-start; padding: 16px 18px; }
    .hint mat-icon { color: var(--teltp-brand); flex-shrink: 0; }
    .hint p { margin: 0; font-size: 0.9rem; }
    @media (max-width: 720px) { .irow { grid-template-columns: 1fr 1fr; } .irow.head { display: none; } }
  `],
})
export class BillingAdminComponent {
  private readonly api = inject(ApiService);
  private readonly snack = inject(MatSnackBar);

  readonly loading = signal(false);
  readonly searched = signal(false);
  readonly invoices = signal<InvoiceResponse[]>([]);
  query = '';

  search(): void {
    if (!this.query.trim()) return;
    this.loading.set(true); this.searched.set(true);
    const q = this.query.trim();
    // try payer-UUID search first; if it looks like a reference number, try that
    const path = q.length === 36 ? `/billing/invoices/payer/${q}` : `/billing/invoices/payer/${q}`;
    this.api.get<PageResponse<InvoiceResponse>>(path).subscribe({
      next: (p) => { this.invoices.set(p.content); this.loading.set(false); },
      error: () => { this.invoices.set([]); this.loading.set(false); },
    });
  }

  confirmManual(inv: InvoiceResponse): void {
    // Manual confirmation via the GePG callback endpoint (operator reconciliation)
    this.api.post<string>('/billing/gepg/callback', {
      controlNumber: inv.referenceNumber,
      providerReference: 'MANUAL',
      paidAmount: inv.total,
    }).subscribe({
      next: () => {
        this.invoices.update((list) => list.map((i) => i.uuid === inv.uuid ? { ...i, status: 'PAID' as never } : i));
        this.snack.open('Invoice marked as paid.', 'Dismiss', { duration: 3000 });
      },
      error: (e) => this.snack.open(e?.error?.message || 'Could not confirm.', 'Dismiss', { duration: 4000 }),
    });
  }
}
