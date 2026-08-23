import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from './api.service';
import { PageResponse } from '../models/api-response.model';
import { InitiatePaymentRequest, InvoiceResponse, PaymentResponse } from '../models/billing.model';

@Injectable({ providedIn: 'root' })
export class BillingService {
  private readonly api = inject(ApiService);

  myInvoices(page = 0, size = 20): Observable<PageResponse<InvoiceResponse>> {
    return this.api.get<PageResponse<InvoiceResponse>>('/billing/invoices/mine', { page, size });
  }

  getInvoice(uuid: string): Observable<InvoiceResponse> {
    return this.api.get<InvoiceResponse>(`/billing/invoices/${uuid}`);
  }

  initiatePayment(req: InitiatePaymentRequest): Observable<PaymentResponse> {
    return this.api.post<PaymentResponse>('/billing/payments/initiate', req);
  }

  confirmInvoice(uuid: string): Observable<InvoiceResponse> {
    return this.api.post<InvoiceResponse>(`/billing/invoices/${uuid}/confirm`, {});
  }
}
