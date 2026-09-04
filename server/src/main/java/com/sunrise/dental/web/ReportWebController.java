package com.sunrise.dental.web;

import com.sunrise.dental.config.ClinicProperties;
import com.sunrise.dental.service.ReportService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Management reports - the brief's "suitable set of reports, which you think add more value".
 *
 * <p>Admin-only, enforced by {@link AuthInterceptor}: a receptionist books and bills, but
 * clinic-wide revenue is not theirs to read.</p>
 */
@Controller
public class ReportWebController {

    private final ReportService reportService;
    private final ClinicProperties clinic;

    public ReportWebController(ReportService reportService, ClinicProperties clinic) {
        this.reportService = reportService;
        this.clinic = clinic;
    }

    @ModelAttribute("currentUser")
    public SessionUser currentUser(HttpServletRequest request) {
        return CurrentUser.require(request);
    }

    @ModelAttribute("clinic")
    public ClinicProperties clinic() {
        return clinic;
    }

    @GetMapping("/reports")
    public String reports(@RequestParam(required = false)
                          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                          @RequestParam(required = false)
                          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                          Model model) {

        // Resolve the range through the service rather than defaulting again here: two copies
        // of the same defaulting rule is how the page and the API drift apart.
        ReportService.DateRange range = ReportService.DateRange.of(from, to);

        model.addAttribute("from", range.from());
        model.addAttribute("to", range.to());
        model.addAttribute("revenue", reportService.revenueByTreatment(range.from(), range.to()));
        model.addAttribute("workload", reportService.dentistWorkload(range.from(), range.to()));
        return "reports/index";
    }
}
