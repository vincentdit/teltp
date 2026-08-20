package tz.go.tirdo.teltp.marketplace.service;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.go.tirdo.teltp.billing.dto.BillingDtos.*;
import tz.go.tirdo.teltp.billing.entity.PricingPlan;
import tz.go.tirdo.teltp.billing.service.BillingService;
import tz.go.tirdo.teltp.billing.service.PricingService;
import tz.go.tirdo.teltp.common.PageResponse;
import tz.go.tirdo.teltp.common.exception.ResourceNotFoundException;
import tz.go.tirdo.teltp.marketplace.dto.MarketplaceDtos.*;
import tz.go.tirdo.teltp.marketplace.entity.MarketplaceItem;
import tz.go.tirdo.teltp.marketplace.entity.MarketplaceItemType;
import tz.go.tirdo.teltp.marketplace.repository.MarketplaceItemRepository;

import java.util.List;

@Service
public class MarketplaceService {

    private final MarketplaceItemRepository items;
    private final PricingService pricing;
    private final BillingService billing;

    public MarketplaceService(MarketplaceItemRepository items, PricingService pricing, BillingService billing) {
        this.items = items;
        this.pricing = pricing;
        this.billing = billing;
    }

    @Transactional
    public ItemResponse create(CreateItemRequest req) {
        pricing.getEntity(req.pricingPlanUuid()); // validate
        MarketplaceItem item = new MarketplaceItem();
        item.setTitle(req.title());
        item.setDescription(req.description());
        item.setType(MarketplaceItemType.valueOf(req.type()));
        item.setPricingPlanUuid(req.pricingPlanUuid());
        item.setStorageKey(req.storageKey());
        return toResponse(items.save(item));
    }

    @Transactional
    public ItemResponse publish(String uuid, boolean published) {
        MarketplaceItem item = require(uuid);
        item.setPublished(published);
        return toResponse(items.save(item));
    }

    @Transactional(readOnly = true)
    public PageResponse<ItemResponse> listPublished(Pageable pageable) {
        return PageResponse.from(items.findByPublishedTrue(pageable), this::toResponse);
    }

    /** Purchase raises an invoice via the Billing engine; download unlocks on payment confirmation. */
    @Transactional
    public InvoiceResponse purchase(String buyerUuid, PurchaseRequest req) {
        MarketplaceItem item = require(req.itemUuid());
        PricingPlan plan = pricing.getEntity(item.getPricingPlanUuid());
        LineItemRequest line = new LineItemRequest(
                item.getTitle(), "MARKETPLACE", item.getUuid(), 1, plan.getPrice().amount());
        return billing.createInvoice(new CreateInvoiceRequest(buyerUuid, "USER", List.of(line)));
    }

    private MarketplaceItem require(String uuid) {
        return items.findByUuid(uuid).orElseThrow(() -> new ResourceNotFoundException("MarketplaceItem", uuid));
    }

    private ItemResponse toResponse(MarketplaceItem i) {
        return new ItemResponse(i.getUuid(), i.getTitle(), i.getDescription(), i.getType().name(),
                i.getPricingPlanUuid(), i.isPublished());
    }
}
