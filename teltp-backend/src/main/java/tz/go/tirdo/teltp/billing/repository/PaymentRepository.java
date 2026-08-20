package tz.go.tirdo.teltp.billing.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tz.go.tirdo.teltp.billing.entity.Payment;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByUuid(String uuid);
    Optional<Payment> findByControlNumber(String controlNumber);
    List<Payment> findByInvoiceUuid(String invoiceUuid);
}
