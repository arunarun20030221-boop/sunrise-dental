package com.sunrise.dental.api;

import com.sunrise.dental.dto.DentistWorkloadRow;
import com.sunrise.dental.dto.RevenueRow;
import com.sunrise.dental.service.ReportService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Management reports over the web service channel. Restricted to ADMIN in
 * {@link com.sunrise.dental.web.AuthInterceptor} - a receptionist books and bills, but
 * clinic-wide revenue is not theirs to read.
 */
@RestController
@RequestMapping("/api/reports")
public class ReportApiController {

    private final ReportService reportService;

    public ReportApiController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/revenue")
    public List<RevenueRow> revenue(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reportService.revenueByTreatment(from, to);
    }

    @GetMapping("/workload")
    public List<DentistWorkloadRow> workload(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reportService.dentistWorkload(from, to);
    }
}
