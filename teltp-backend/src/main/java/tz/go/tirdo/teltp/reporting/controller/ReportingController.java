package tz.go.tirdo.teltp.reporting.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tz.go.tirdo.teltp.common.ApiResponse;
import tz.go.tirdo.teltp.reporting.dto.ReportingDtos.*;
import tz.go.tirdo.teltp.reporting.service.ReportingService;

@RestController
@RequestMapping("/reporting")
public class ReportingController {

    private final ReportingService service;

    public ReportingController(ReportingService service) {
        this.service = service;
    }

    @GetMapping("/kpis")
    @PreAuthorize("hasAnyRole('ADMIN','FINANCE_OFFICER')")
    public ApiResponse<PlatformKpis> kpis() {
        return ApiResponse.ok(service.kpis());
    }

    @GetMapping("/revenue")
    @PreAuthorize("hasAnyRole('ADMIN','FINANCE_OFFICER')")
    public ApiResponse<RevenueDashboard> revenue() {
        return ApiResponse.ok(service.revenue());
    }

    @GetMapping("/completion")
    @PreAuthorize("hasAnyRole('ADMIN','FINANCE_OFFICER','INSTRUCTOR')")
    public ApiResponse<CompletionDashboard> completion() {
        return ApiResponse.ok(service.completion());
    }

    @GetMapping("/trainer")
    @PreAuthorize("hasAnyRole('ADMIN','FINANCE_OFFICER')")
    public ApiResponse<TrainerDashboard> trainer() {
        return ApiResponse.ok(service.trainer());
    }
}
