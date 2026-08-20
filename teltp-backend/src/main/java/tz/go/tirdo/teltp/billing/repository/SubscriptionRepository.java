package tz.go.tirdo.teltp.billing.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tz.go.tirdo.teltp.billing.entity.Subscription;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    Optional<Subscription> findByUuid(String uuid);
    List<Subscription> findBySubscriberUuid(String subscriberUuid);
    List<Subscription> findByActiveTrueAndAutoRenewTrueAndCurrentPeriodEndBefore(LocalDate date);
}
