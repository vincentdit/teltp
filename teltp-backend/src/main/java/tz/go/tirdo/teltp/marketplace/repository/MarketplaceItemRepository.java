package tz.go.tirdo.teltp.marketplace.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import tz.go.tirdo.teltp.marketplace.entity.MarketplaceItem;

import java.util.Optional;

public interface MarketplaceItemRepository extends JpaRepository<MarketplaceItem, Long> {
    Optional<MarketplaceItem> findByUuid(String uuid);
    Page<MarketplaceItem> findByPublishedTrue(Pageable pageable);
}
