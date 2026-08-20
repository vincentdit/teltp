package tz.go.tirdo.teltp.organization.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tz.go.tirdo.teltp.common.ApiResponse;
import tz.go.tirdo.teltp.common.PageResponse;
import tz.go.tirdo.teltp.organization.dto.OrganizationDtos.*;
import tz.go.tirdo.teltp.organization.service.OrganizationService;

@RestController
@RequestMapping("/organizations")
public class OrganizationController {

    private final OrganizationService service;

    public OrganizationController(OrganizationService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<OrganizationResponse> create(@Valid @RequestBody CreateRequest req) {
        return ApiResponse.ok("Organization created", service.create(req));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','FINANCE_OFFICER')")
    public ApiResponse<PageResponse<OrganizationResponse>> list(Pageable pageable) {
        return ApiResponse.ok(service.list(pageable));
    }

    @GetMapping("/{uuid}")
    @PreAuthorize("hasAnyRole('ADMIN','FINANCE_OFFICER','CORPORATE_CLIENT')")
    public ApiResponse<OrganizationResponse> get(@PathVariable String uuid) {
        return ApiResponse.ok(service.get(uuid));
    }
}
