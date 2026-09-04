package com.sunrise.dental.web;

import com.sunrise.dental.config.ClinicProperties;
import com.sunrise.dental.domain.Appointment;
import com.sunrise.dental.dto.AppointmentRequest;
import com.sunrise.dental.exception.DoubleBookingException;
import com.sunrise.dental.exception.NotFoundException;
import com.sunrise.dental.exception.OutsideOpeningHoursException;
import com.sunrise.dental.service.AppointmentService;
import com.sunrise.dental.service.BillingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * The staff-facing appointment screens, rendered as JSP pages.
 *
 * <p>Shares its entire business tier with {@link com.sunrise.dental.api.AppointmentApiController},
 * so a booking made in the browser is subject to exactly the same validation and double-booking
 * checks as one made through the web service. Duplicating those rules per channel is how the two
 * would silently drift apart.</p>
 */
@Controller
public class AppointmentWebController {

    private final AppointmentService appointmentService;
    private final BillingService billingService;
    private final ClinicProperties clinic;

    public AppointmentWebController(AppointmentService appointmentService,
                                    BillingService billingService,
                                    ClinicProperties clinic) {
        this.appointmentService = appointmentService;
        this.billingService = billingService;
        this.clinic = clinic;
    }

    /** Puts the signed-in user and clinic details on every page this controller renders. */
    @ModelAttribute("currentUser")
    public SessionUser currentUser(HttpServletRequest request) {
        return CurrentUser.require(request);
    }

    @ModelAttribute("clinic")
    public ClinicProperties clinic() {
        return clinic;
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/appointments";
    }

    /** The day's diary, defaulting to today. */
    @GetMapping("/appointments")
    public String list(@RequestParam(required = false)
                       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                       Model model) {
        LocalDate target = date != null ? date : LocalDate.now();
        model.addAttribute("date", target);
        model.addAttribute("appointments", appointmentService.findByDate(target));
        return "appointments/list";
    }

    /** Requirement 2 - the registration form. */
    @GetMapping("/appointments/new")
    public String showRegisterForm(Model model) {
        if (!model.containsAttribute("appointmentRequest")) {
            model.addAttribute("appointmentRequest", emptyRequest());
        }
        addFormReferenceData(model);
        return "appointments/form";
    }

    @PostMapping("/appointments")
    public String register(@Valid @ModelAttribute("appointmentRequest") AppointmentRequest form,
                           BindingResult binding,
                           HttpServletRequest httpRequest,
                           Model model,
                           RedirectAttributes flash) {

        // Field-level validation failures: redisplay the form with the messages inline rather
        // than losing what the receptionist already typed.
        if (binding.hasErrors()) {
            addFormReferenceData(model);
            return "appointments/form";
        }

        try {
            Appointment saved = appointmentService.register(form, CurrentUser.username(httpRequest));
            flash.addFlashAttribute("message",
                    "Appointment " + saved.getAppointmentNumber() + " registered for "
                            + saved.getPatient().getName() + ".");
            return "redirect:/appointments/" + saved.getAppointmentNumber();

        } catch (DoubleBookingException | OutsideOpeningHoursException e) {
            // Business-rule rejections are not field errors: they depend on the state of the
            // diary, so they are shown as a page-level message with the form still populated.
            model.addAttribute("error", e.getMessage());
            addFormReferenceData(model);
            return "appointments/form";
        }
    }

    /** Requirement 3 - display appointment details, searched by appointment number. */
    @GetMapping("/appointments/{appointmentNumber}")
    public String details(@PathVariable String appointmentNumber, Model model) {
        Appointment appointment = appointmentService.findByNumber(appointmentNumber);
        model.addAttribute("appointment", appointment);
        model.addAttribute("hasBill", billingService.hasBill(appointmentNumber));
        model.addAttribute("preview", billingService.calculate(appointment));
        return "appointments/details";
    }

    /** Search box on the diary page: by appointment number if it looks like one, else by name. */
    @GetMapping("/appointments/search")
    public String search(@RequestParam String query, Model model, RedirectAttributes flash) {
        String trimmed = query == null ? "" : query.trim();

        if (trimmed.toUpperCase().startsWith("APT-")) {
            try {
                appointmentService.findByNumber(trimmed.toUpperCase());
                return "redirect:/appointments/" + trimmed.toUpperCase();
            } catch (NotFoundException e) {
                flash.addFlashAttribute("error", e.getMessage());
                return "redirect:/appointments";
            }
        }

        model.addAttribute("query", trimmed);
        model.addAttribute("results", appointmentService.searchByPatientName(trimmed));
        return "appointments/search";
    }

    @PostMapping("/appointments/{appointmentNumber}/attended")
    public String markAttended(@PathVariable String appointmentNumber, RedirectAttributes flash) {
        appointmentService.markAttended(appointmentNumber);
        flash.addFlashAttribute("message", appointmentNumber + " marked as attended.");
        return "redirect:/appointments/" + appointmentNumber;
    }

    @PostMapping("/appointments/{appointmentNumber}/cancel")
    public String cancel(@PathVariable String appointmentNumber, RedirectAttributes flash) {
        appointmentService.cancel(appointmentNumber);
        flash.addFlashAttribute("message", appointmentNumber + " cancelled. The slot is free again.");
        return "redirect:/appointments/" + appointmentNumber;
    }

    @PostMapping("/appointments/{appointmentNumber}/no-show")
    public String markNoShow(@PathVariable String appointmentNumber, RedirectAttributes flash) {
        appointmentService.markNoShow(appointmentNumber);
        flash.addFlashAttribute("message", appointmentNumber + " recorded as a no-show.");
        return "redirect:/appointments/" + appointmentNumber;
    }

    private void addFormReferenceData(Model model) {
        model.addAttribute("dentists", appointmentService.activeDentists());
        model.addAttribute("treatments", appointmentService.treatments());
    }

    private AppointmentRequest emptyRequest() {
        return new AppointmentRequest(null, null, null, null, null, null,
                LocalDate.now(), null, 1, null);
    }
}
