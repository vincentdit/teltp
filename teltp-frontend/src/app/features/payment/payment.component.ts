import { Component, OnInit, inject, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { DecimalPipe } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatRadioModule } from '@angular/material/radio';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { BillingService } from '../../core/services/billing.service';
import { TokenService } from '../../core/services/token.service';
import {
  InvoiceResponse, PaymentChannel, PaymentResponse, PAYMENT_CHANNELS,
} from '../../core/models/billing.model';

@Component({
  selector: 'app-payment',
  standalone: true,
  imports: [
    RouterLink, FormsModule, DecimalPipe, MatCardModule, MatButtonModule, MatIconModule,
    MatRadioModule, MatFormFieldModule, MatInputModule, MatProgressSpinnerModule,
  ],
  template: `
    <div class="page narrow">
      <a class="muted back" [routerLink]="['/catalog', courseUuid()]">
        <mat-icon>arrow_back</mat-icon> Back to course
      </a>
      <h1 class="page-title">Course payment</h1>

      @if (loading()) {
        <div class="center"><mat-spinner diameter="36" /></div>
      } @else if (result(); as r) {
        <!-- success screen -->
        <mat-card class="surface-card success">
          <mat-icon class="big">check_circle</mat-icon>
          <h2>Payment initiated</h2>
          @if (r.channel === 'GEPG') {
            <p>Your GePG control number is:</p>
            <div class="control-number">{{ r.controlNumber }}</div>
            <p class="muted">Use this number to pay at any bank branch, mobile wallet, or the GePG self-service portal. Your enrolment activates once payment is confirmed.</p>
          } @else if (r.channel === 'MOBILE_MONEY') {
            <p class="muted">{{ r.instructions }}</p>
            <p class="muted">Reference: <strong>{{ r.referenceNumber }}</strong></p>
          } @else {
            <p class="muted">{{ r.instructions }}</p>
            <p class="muted">Bank reference: <strong>{{ r.referenceNumber }}</strong></p>
          }
          <a mat-flat-button color="primary" routerLink="/dashboard">Go to dashboard</a>
        </mat-card>
      } @else if (invoice(); as inv) {
        <!-- payment form -->
        <mat-card class="surface-card inv-card">
          <div class="inv-head">
            <div>
              <span class="muted">Invoice</span>
              <strong>{{ inv.referenceNumber }}</strong>
            </div>
            <div class="amount">
              <span class="muted">Amount due</span>
              <strong>{{ inv.currency }} {{ inv.total | number:'1.0-0' }}</strong>
            </div>
          </div>
          <div class="items">
            @for (li of inv.lineItems; track li.description) {
              <div class="li">
                <span>{{ li.description }}</span>
                <span class="num muted">{{ inv.currency }} {{ li.unitPrice * li.quantity | number:'1.0-0' }}</span>
              </div>
            }
          </div>
        </mat-card>

        <h2 class="section-title">Choose payment method</h2>
        <div class="channels">
          @for (ch of channels; track ch.value) {
            <label class="ch-card surface-card" [class.selected]="channel() === ch.value"
                   (click)="setChannel(ch.value)">
              <mat-icon>{{ ch.icon }}</mat-icon>
              <span>{{ ch.label }}</span>
              <span class="spacer"></span>
              <span class="radio" [class.active]="channel() === ch.value"></span>
            </label>
          }
        </div>

        @if (channel() === 'MOBILE_MONEY') {
          <mat-card class="surface-card form-card">
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Mobile number (e.g. 0712345678)</mat-label>
              <input matInput [(ngModel)]="payerPhone" type="tel" />
            </mat-form-field>
          </mat-card>
        }

        @if (error()) { <p class="msg bad"><mat-icon>error</mat-icon> {{ error() }}</p> }

        <div class="pay-bar">
          <span class="spacer"></span>
          <button mat-flat-button color="primary" (click)="pay(inv)"
                  [disabled]="!channel() || paying() || (channel() === 'MOBILE_MONEY' && !payerPhone)">
            @if (paying()) { <mat-spinner diameter="18" /> } @else {
              Pay {{ inv.currency }} {{ inv.total | number:'1.0-0' }} via {{ channelLabel() }}
            }
          </button>
        </div>
      } @else {
        <mat-card class="surface-card empty">
          <mat-icon>receipt_long</mat-icon>
          <p class="muted">No pending invoice found for this course.</p>
          <p class="muted">If you already paid, your enrolment will activate shortly. <a routerLink="/dashboard">Go to dashboard.</a></p>
        </mat-card>
      }
    </div>
  `,
  styles: [`
    .narrow { max-width: 680px; }
    .back { display: inline-flex; align-items: center; gap: 4px; font-size: 0.9rem; text-decoration: none; margin-bottom: 8px; }
    .back mat-icon { font-size: 18px; height: 18px; width: 18px; }
    .center { display: flex; justify-content: center; padding: 60px; }
    .section-title { font-family: 'Spectral', Georgia, serif; font-size: 1.2rem; margin: 24px 0 12px; }

    .inv-card { padding: 18px 20px; }
    .inv-head { display: flex; justify-content: space-between; margin-bottom: 14px; }
    .inv-head > div { display: flex; flex-direction: column; }
    .amount { text-align: right; }
    .amount strong { font-family: 'Spectral', Georgia, serif; font-size: 1.5rem; color: var(--teltp-brand); }
    .items { border-top: 1px solid var(--teltp-line); padding-top: 10px; display: flex; flex-direction: column; gap: 8px; }
    .li { display: flex; justify-content: space-between; font-size: 0.9rem; }
    .num { font-variant-numeric: tabular-nums; }

    .channels { display: flex; flex-direction: column; gap: 10px; }
    .ch-card { display: flex; align-items: center; gap: 14px; padding: 14px 18px; cursor: pointer; border: 2px solid var(--teltp-line); transition: border-color 0.15s; }
    .ch-card.selected { border-color: var(--teltp-brand); }
    .ch-card mat-icon { color: var(--teltp-brand); }
    .radio { width: 18px; height: 18px; border-radius: 50%; border: 2px solid var(--teltp-muted); flex-shrink: 0; }
    .radio.active { border-color: var(--teltp-brand); background: var(--teltp-brand); }

    .form-card { padding: 18px 20px; margin-top: 12px; }
    .full-width { width: 100%; }

    .msg { display: flex; align-items: center; gap: 6px; font-size: 0.9rem; margin-top: 8px; }
    .msg.bad { color: #a33; }
    .msg mat-icon { font-size: 18px; height: 18px; width: 18px; }

    .pay-bar { display: flex; align-items: center; margin-top: 20px; }
    .pay-bar mat-spinner { --mdc-circular-progress-active-indicator-color: #fff; }

    .success { text-align: center; padding: 48px 32px; display: flex; flex-direction: column; align-items: center; gap: 12px; }
    .success .big { font-size: 56px; height: 56px; width: 56px; color: #2e7d32; }
    .control-number { font-family: monospace; font-size: 2.2rem; font-weight: 700; letter-spacing: 0.12em;
      background: rgba(26,77,152,0.08); padding: 12px 24px; border-radius: 10px; color: var(--teltp-brand); }

    .empty { text-align: center; padding: 48px; display: flex; flex-direction: column; align-items: center; gap: 12px; }
    .empty mat-icon { font-size: 40px; height: 40px; width: 40px; color: var(--teltp-muted); }
  `],
})
export class PaymentComponent implements OnInit {
  readonly courseUuid = input.required<string>();

  private readonly billingApi = inject(BillingService);
  private readonly tokens = inject(TokenService);

  readonly loading = signal(true);
  readonly paying = signal(false);
  readonly invoice = signal<InvoiceResponse | null>(null);
  readonly result = signal<PaymentResponse | null>(null);
  readonly error = signal<string | null>(null);
  readonly channel = signal<PaymentChannel | null>(null);
  payerPhone = '';

  readonly channels = PAYMENT_CHANNELS;

  ngOnInit(): void {
    this.billingApi.myInvoices(0, 50).subscribe({
      next: (p) => {
        // find the ISSUED invoice whose line item matches this course
        const inv = p.content.find((i) =>
          i.status === 'ISSUED' &&
          i.lineItems.some((li) => li.itemUuid === this.courseUuid())
        ) ?? null;
        this.invoice.set(inv);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  setChannel(ch: PaymentChannel): void { this.channel.set(ch); this.error.set(null); }

  channelLabel(): string {
    return this.channels.find((c) => c.value === this.channel())?.label ?? '';
  }

  pay(inv: InvoiceResponse): void {
    const ch = this.channel(); if (!ch) return;
    this.error.set(null); this.paying.set(true);
    this.billingApi.initiatePayment({
      invoiceUuid: inv.uuid, channel: ch,
      payerName: this.tokens.username() ?? undefined,
      payerPhone: this.payerPhone || undefined,
    }).subscribe({
      next: (r) => { this.paying.set(false); this.result.set(r); window.scrollTo(0, 0); },
      error: (e) => { this.paying.set(false); this.error.set(e?.error?.message || 'Could not initiate payment.'); },
    });
  }
}
