package tz.go.tirdo.teltp.billing.service;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.go.tirdo.teltp.billing.dto.BillingDtos.*;
import tz.go.tirdo.teltp.billing.entity.*;
import tz.go.tirdo.teltp.billing.repository.InvoiceRepository;
import tz.go.tirdo.teltp.billing.repository.PaymentRepository;
import tz.go.tirdo.teltp.common.Money;
import tz.go.tirdo.teltp.common.PageResponse;
import tz.go.tirdo.teltp.common.ReferenceNumberGenerator;
import tz.go.tirdo.teltp.common.exception.BusinessRuleException;
import tz.go.tirdo.teltp.common.exception.ResourceNotFoundException;
import tz.go.tirdo.teltp.integration.payment.PaymentInitiation;
import tz.go.tirdo.teltp.integration.payment.PaymentInitiationResult;
import tz.go.tirdo.teltp.integration.payment.PaymentMethodRegistry;

import java.time.Instant;

@Service
public class BillingService {

    private final InvoiceRepository invoices;
    private final PaymentRepository payments;
    private final PaymentMethodRegistry paymentMethods;
    private final ReferenceNumberGenerator refGen;

    public BillingService(InvoiceRepository invoices, PaymentRepository payments,
                          PaymentMethodRegistry paymentMethods, ReferenceNumberGenerator refGen) {
        this.invoices = invoices;
        this.payments = payments;
        this.paymentMethods = paymentMethods;
        this.refGen = refGen;
    }

    @Transactional
    public InvoiceResponse createInvoice(CreateInvoiceRequest req) {
        Invoice inv = new Invoice();
        inv.setReferenceNumber(refGen.next("INV"));
        inv.setPayerUuid(req.payerUuid());
        inv.setPayerType(req.payerType());
        inv.setStatus(InvoiceStatus.ISSUED);
        for (LineItemRequest li : req.lineItems()) {
            InvoiceLineItem item = new InvoiceLineItem();
            item.setInvoice(inv);
            item.setDescription(li.description());
            item.setItemType(li.itemType());
            item.setItemUuid(li.itemUuid());
            item.setQuantity(li.quantity());
            item.setUnitPrice(Money.tzs(li.unitPrice()));
            inv.getLineItems().add(item);
        }
        inv.recalculateTotal();
        return toInvoiceResponse(invoices.save(inv));
    }

    @Transactional
    public PaymentResponse initiatePayment(InitiatePaymentRequest req) {
        Invoice inv = requireInvoice(req.invoiceUuid());
        if (inv.getStatus() == InvoiceStatus.PAID)
            throw new BusinessRuleException("Invoice already paid");
        PaymentChannel channel = PaymentChannel.valueOf(req.channel());

        Payment payment = new Payment();
        payment.setReferenceNumber(refGen.next("PAY"));
        payment.setInvoiceUuid(inv.getUuid());
        payment.setChannel(channel);
        payment.setAmount(inv.getTotal());
        payment.setStatus(PaymentStatus.INITIATED);
        payment = payments.save(payment);

        PaymentInitiation init = new PaymentInitiation(payment.getUuid(), inv.getReferenceNumber(),
                inv.getTotal().amount(), inv.getTotal().currency(),
                req.payerName(), req.payerPhone(), req.payerEmail());
        PaymentInitiationResult result = paymentMethods.forChannel(channel).initiate(init);

        payment.setControlNumber(result.controlNumber());
        payment.setProviderReference(result.providerReference());
        payment.setStatus(PaymentStatus.PENDING);
        Payment saved = payments.save(payment);

        return new PaymentResponse(saved.getUuid(), saved.getReferenceNumber(), inv.getUuid(),
                channel.name(), saved.getStatus().name(), saved.getAmount().amount(),
                saved.getControlNumber(), result.instructions());
    }

    /**
     * Confirm a payment (called by the GePG callback handler or an operator reconciliation action).
     * Idempotent: re-confirming an already-confirmed payment is a no-op.
     */
    @Transactional
    public void confirmByControlNumber(String controlNumber, String providerReference) {
        Payment payment = payments.findByControlNumber(controlNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Payment(controlNumber)", controlNumber));
        if (payment.getStatus() == PaymentStatus.CONFIRMED) return;

        payment.setStatus(PaymentStatus.CONFIRMED);
        payment.setProviderReference(providerReference);
        payment.setConfirmedAt(Instant.now());
        payments.save(payment);

        Invoice inv = requireInvoice(payment.getInvoiceUuid());
        inv.setStatus(InvoiceStatus.PAID);
        invoices.save(inv);
    }

    @Transactional(readOnly = true)
    public PageResponse<InvoiceResponse> invoicesForPayer(String payerUuid, Pageable pageable) {
        return PageResponse.from(invoices.findByPayerUuid(payerUuid, pageable), this::toInvoiceResponse);
    }

    @Transactional(readOnly = true)
    public InvoiceResponse getInvoice(String uuid) {
        return toInvoiceResponse(requireInvoice(uuid));
    }

    private Invoice requireInvoice(String uuid) {
        return invoices.findByUuid(uuid).orElseThrow(() -> new ResourceNotFoundException("Invoice", uuid));
    }

    private InvoiceResponse toInvoiceResponse(Invoice inv) {
        var items = inv.getLineItems().stream()
                .map(li -> new LineItemResponse(li.getDescription(), li.getItemType(), li.getItemUuid(),
                        li.getQuantity(), li.getUnitPrice().amount()))
                .toList();
        return new InvoiceResponse(inv.getUuid(), inv.getReferenceNumber(), inv.getPayerUuid(),
                inv.getPayerType(), inv.getStatus().name(), inv.getTotal().amount(),
                inv.getTotal().currency(), items);
    }
}
