package tz.go.tirdo.teltp.billing.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import tz.go.tirdo.teltp.billing.entity.Invoice;
import tz.go.tirdo.teltp.billing.entity.InvoiceStatus;

import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    Optional<Invoice> findByUuid(String uuid);
    Optional<Invoice> findByReferenceNumber(String referenceNumber);
    Page<Invoice> findByPayerUuid(String payerUuid, Pageable pageable);
    List<Invoice> findByStatus(InvoiceStatus status);
}
