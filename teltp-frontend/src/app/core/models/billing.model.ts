export type PaymentChannel = 'GEPG' | 'MOBILE_MONEY' | 'BANK_TRANSFER';
export type InvoiceStatus = 'DRAFT' | 'ISSUED' | 'PARTIALLY_PAID' | 'PAID' | 'CANCELLED';
export type PaymentStatus = 'INITIATED' | 'PENDING' | 'CONFIRMED' | 'FAILED';

export interface LineItemResponse {
  description: string;
  itemType: string;
  itemUuid?: string;
  quantity: number;
  unitPrice: number;
}

export interface InvoiceResponse {
  uuid: string;
  referenceNumber: string;
  payerUuid: string;
  payerType: string;
  status: InvoiceStatus;
  total: number;
  currency: string;
  lineItems: LineItemResponse[];
}

export interface InitiatePaymentRequest {
  invoiceUuid: string;
  channel: PaymentChannel;
  payerName?: string;
  payerPhone?: string;
  payerEmail?: string;
}

export interface PaymentResponse {
  uuid: string;
  referenceNumber: string;
  invoiceUuid: string;
  channel: PaymentChannel;
  status: PaymentStatus;
  amount: number;
  controlNumber?: string;
  instructions?: string;
}

export const PAYMENT_CHANNELS: { value: PaymentChannel; label: string; icon: string }[] = [
  { value: 'GEPG', label: 'GePG (Government Portal)', icon: 'account_balance' },
  { value: 'MOBILE_MONEY', label: 'Mobile Money (M-Pesa / Tigo / Airtel)', icon: 'phone_android' },
  { value: 'BANK_TRANSFER', label: 'Bank Transfer (EFT)', icon: 'account_balance_wallet' },
];
