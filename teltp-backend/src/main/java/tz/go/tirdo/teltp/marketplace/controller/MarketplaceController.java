package tz.go.tirdo.teltp.marketplace.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tz.go.tirdo.teltp.auth.service.UserService;
import tz.go.tirdo.teltp.billing.dto.BillingDtos.InvoiceResponse;
import tz.go.tirdo.teltp.common.ApiResponse;
import tz.go.tirdo.teltp.common.PageResponse;
import tz.go.tirdo.teltp.marketplace.dto.MarketplaceDtos.*;
import tz.go.tirdo.teltp.marketplace.service.MarketplaceService;
import tz.go.tirdo.teltp.security.CurrentUser;

@RestController
@RequestMapping("/marketplace")
public class MarketplaceController {

    private final MarketplaceService service;
    private final UserService users;

    public MarketplaceController(MarketplaceService service, UserService users) {
        this.service = service;
        this.users = users;
    }

    @PostMapping("/items")
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR')")
    public ApiResponse<ItemResponse> create(@Valid @RequestBody CreateItemRequest req) {
        return ApiResponse.ok("Item created", service.create(req));
    }

    @PostMapping("/items/{uuid}/publish")
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR')")
    public ApiResponse<ItemResponse> publish(@PathVariable String uuid, @RequestParam boolean published) {
        return ApiResponse.ok(service.publish(uuid, published));
    }

    @GetMapping("/items")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PageResponse<ItemResponse>> list(Pageable pageable) {
        return ApiResponse.ok(service.listPublished(pageable));
    }

    @PostMapping("/purchase")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<InvoiceResponse> purchase(@Valid @RequestBody PurchaseRequest req) {
        return ApiResponse.ok("Invoice raised", service.purchase(me(), req));
    }

    private String me() {
        return users.uuidForUsername(CurrentUser.requireUsername());
    }
}
