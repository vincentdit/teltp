package tz.go.tirdo.teltp.corporate.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tz.go.tirdo.teltp.common.ApiResponse;
import tz.go.tirdo.teltp.common.PageResponse;
import tz.go.tirdo.teltp.corporate.dto.CorporateDtos.*;
import tz.go.tirdo.teltp.corporate.service.CorporateService;

@RestController
@RequestMapping("/corporate/contracts")
public class CorporateController {

    private final CorporateService service;

    public CorporateController(CorporateService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','FINANCE_OFFICER','CORPORATE_CLIENT')")
    public ApiResponse<ContractResponse> create(@Valid @RequestBody CreateContractRequest req) {
        return ApiResponse.ok("Contract created", service.create(req));
    }

    @PostMapping("/{uuid}/quote")
    @PreAuthorize("hasAnyRole('ADMIN','FINANCE_OFFICER')")
    public ApiResponse<ContractResponse> quote(@PathVariable String uuid, @Valid @RequestBody QuoteRequest req) {
        return ApiResponse.ok(service.quote(uuid, req));
    }

    @PostMapping("/{uuid}/transition")
    @PreAuthorize("hasAnyRole('ADMIN','FINANCE_OFFICER')")
    public ApiResponse<ContractResponse> transition(@PathVariable String uuid, @Valid @RequestBody TransitionRequest req) {
        return ApiResponse.ok(service.transition(uuid, req));
    }

    @GetMapping("/organization/{organizationUuid}")
    @PreAuthorize("hasAnyRole('ADMIN','FINANCE_OFFICER','CORPORATE_CLIENT')")
    public ApiResponse<PageResponse<ContractResponse>> forOrganization(@PathVariable String organizationUuid,
                                                                       Pageable pageable) {
        return ApiResponse.ok(service.forOrganization(organizationUuid, pageable));
    }
}
