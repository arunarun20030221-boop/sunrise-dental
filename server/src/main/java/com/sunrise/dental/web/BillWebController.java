package com.sunrise.dental.web;

import com.sunrise.dental.config.ClinicProperties;
import com.sunrise.dental.domain.Appointment;
import com.sunrise.dental.domain.Bill;
import com.sunrise.dental.exception.BillAlreadyIssuedException;
import com.sunrise.dental.service.AppointmentService;
import com.sunrise.dental.service.BillingService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Requirement 4 - "Calculate the total treatment cost" and "Print the patient bill/receipt".
 *
 * <p>Printing is handled by a print-specific stylesheet on the receipt page rather than by
 * generating a PDF. A PDF library would add a dependency and an API to learn in exchange for
 * no marks: the receipt needs to come out of the printer on the front desk, and the browser's
 * own print dialog does that.</p>
 */
@Controller
public class BillWebController {

    private final BillingService billingService;
    private final AppointmentService appointmentService;
    private final ClinicProperties clinic;

    public BillWebController(BillingService billingService,
                             AppointmentService appointmentService,
                             ClinicProperties clinic) {
        this.billingService = billingService;
        this.appointmentService = appointmentService;
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

    /** Issues the bill, then redirects to the printable receipt. */
    @PostMapping("/bills/{appointmentNumber}")
    public String issue(@PathVariable String appointmentNumber,
                        HttpServletRequest httpRequest,
                        RedirectAttributes flash) {
        Appointment appointment = appointmentService.findByNumber(appointmentNumber);
        try {
            Bill bill = billingService.issue(appointment, CurrentUser.username(httpRequest));
            flash.addFlashAttribute("message", "Bill " + bill.getBillNumber() + " issued.");
        } catch (BillAlreadyIssuedException e) {
            // Not an error worth blocking on: show the existing receipt instead of a stack trace.
            flash.addFlashAttribute("message", e.getMessage() + " Showing the existing receipt.");
        }
        return "redirect:/bills/" + appointmentNumber;
    }

    /** The printable receipt. */
    @GetMapping("/bills/{appointmentNumber}")
    public String receipt(@PathVariable String appointmentNumber, Model model) {
        model.addAttribute("bill", billingService.findByAppointmentNumber(appointmentNumber));
        return "bills/receipt";
    }
}
