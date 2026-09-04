package com.sunrise.dental.api;

import com.sunrise.dental.domain.Appointment;
import com.sunrise.dental.dto.BillResponse;
import com.sunrise.dental.service.AppointmentService;
import com.sunrise.dental.service.BillCalculation;
import com.sunrise.dental.service.BillingService;
import com.sunrise.dental.web.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * The billing web service: preview a total, issue a bill, retrieve an issued bill.
 *
 * <p>Reaches the data tier only through {@link BillingService} - a controller that queried a
 * DAO directly would collapse the three tiers into two and let presentation concerns leak
 * into persistence.</p>
 */
@RestController
@RequestMapping("/api/bills")
public class BillApiController {

    private final BillingService billingService;
    private final AppointmentService appointmentService;

    public BillApiController(BillingService billingService,
                             AppointmentService appointmentService) {
        this.billingService = billingService;
        this.appointmentService = appointmentService;
    }

    /**
     * Prices an appointment without issuing anything, so staff can quote a figure to the
     * patient before committing it.
     */
    @GetMapping("/preview/{appointmentNumber}")
    public BillCalculation preview(@PathVariable String appointmentNumber) {
        Appointment appointment = appointmentService.findByNumber(appointmentNumber);
        return billingService.calculate(appointment);
    }

    @PostMapping("/{appointmentNumber}")
    @ResponseStatus(HttpStatus.CREATED)
    public BillResponse issue(@PathVariable String appointmentNumber, HttpServletRequest request) {
        Appointment appointment = appointmentService.findByNumber(appointmentNumber);
        return BillResponse.from(billingService.issue(appointment, CurrentUser.username(request)));
    }

    @GetMapping("/{appointmentNumber}")
    public BillResponse byAppointment(@PathVariable String appointmentNumber) {
        return BillResponse.from(billingService.findByAppointmentNumber(appointmentNumber));
    }
}
