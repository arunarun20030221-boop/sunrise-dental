package com.sunrise.dental.api;

import com.sunrise.dental.domain.Appointment;
import com.sunrise.dental.dto.AppointmentRequest;
import com.sunrise.dental.dto.AppointmentResponse;
import com.sunrise.dental.service.AppointmentService;
import com.sunrise.dental.web.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * The appointment web service.
 *
 * <p>This is the interface that makes the application distributed: the console client is a
 * separate operating-system process that reaches these endpoints over HTTP and shares no code
 * and no database connection with the server. The browser UI calls the same business tier
 * through its own controllers, so both channels enforce identical rules.</p>
 */
@RestController
@RequestMapping("/api/appointments")
public class AppointmentApiController {

    private final AppointmentService appointmentService;

    public AppointmentApiController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    /** Register a new appointment. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AppointmentResponse register(@Valid @RequestBody AppointmentRequest appointmentRequest,
                                        HttpServletRequest httpRequest) {
        Appointment appointment =
                appointmentService.register(appointmentRequest, CurrentUser.username(httpRequest));
        return AppointmentResponse.from(appointment);
    }

    /** Look up one appointment by its number - the brief's "Display Appointment Details". */
    @GetMapping("/{appointmentNumber}")
    public AppointmentResponse byNumber(@PathVariable String appointmentNumber) {
        return AppointmentResponse.from(appointmentService.findByNumber(appointmentNumber));
    }

    /** The day's diary, defaulting to today. */
    @GetMapping
    public List<AppointmentResponse> byDate(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate target = date != null ? date : LocalDate.now();
        return appointmentService.findByDate(target).stream()
                .map(AppointmentResponse::from)
                .toList();
    }

    @GetMapping("/search")
    public List<AppointmentResponse> searchByPatient(@RequestParam String patientName) {
        return appointmentService.searchByPatientName(patientName).stream()
                .map(AppointmentResponse::from)
                .toList();
    }

    @PostMapping("/{appointmentNumber}/cancel")
    public AppointmentResponse cancel(@PathVariable String appointmentNumber) {
        return AppointmentResponse.from(appointmentService.cancel(appointmentNumber));
    }

    @PostMapping("/{appointmentNumber}/attended")
    public AppointmentResponse markAttended(@PathVariable String appointmentNumber) {
        return AppointmentResponse.from(appointmentService.markAttended(appointmentNumber));
    }

    @PostMapping("/{appointmentNumber}/no-show")
    public AppointmentResponse markNoShow(@PathVariable String appointmentNumber) {
        return AppointmentResponse.from(appointmentService.markNoShow(appointmentNumber));
    }
}
