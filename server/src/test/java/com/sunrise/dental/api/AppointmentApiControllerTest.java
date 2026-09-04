package com.sunrise.dental.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sunrise.dental.config.JsonMapperFactory;
import com.sunrise.dental.domain.Appointment;
import com.sunrise.dental.domain.Dentist;
import com.sunrise.dental.domain.Patient;
import com.sunrise.dental.domain.Staff;
import com.sunrise.dental.domain.StaffRole;
import com.sunrise.dental.domain.TreatmentType;
import com.sunrise.dental.exception.DoubleBookingException;
import com.sunrise.dental.exception.NotFoundException;
import com.sunrise.dental.service.AppointmentService;
import com.sunrise.dental.web.SessionUser;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Tests the appointment web service through {@link MockMvc}, without starting Tomcat or
 * touching PostgreSQL.
 *
 * <p>These are the tests that cover the HTTP contract the console client depends on: the status
 * codes and the JSON shape. The unit tests in {@code BillingServiceTest} and
 * {@code AppointmentSchedulingRulesTest} prove the rules are right; these prove the rules are
 * reported correctly over the wire, which is a separate failure mode - a correct rule returned
 * as HTTP 500 is still a broken client.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AppointmentApiController")
class AppointmentApiControllerTest {

    @Mock
    private AppointmentService appointmentService;

    private MockMvc mockMvc;
    private ObjectMapper json;

    @BeforeEach
    void setUp() {
        // The very same mapper the application uses, not a lookalike.
        json = JsonMapperFactory.create();
        mockMvc = MockMvcBuilders
                .standaloneSetup(new AppointmentApiController(appointmentService))
                .setControllerAdvice(new ApiExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(json))
                .build();
    }

    @Test
    @DisplayName("returns 201 and the appointment number when a booking succeeds")
    void registerReturnsCreated() throws Exception {
        when(appointmentService.register(any(), any())).thenReturn(sampleAppointment());

        mockMvc.perform(post("/api/appointments")
                        .contentType("application/json")
                        .content(validRequestJson())
                        .requestAttr(SessionUser.SESSION_KEY, sessionUser()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.appointmentNumber").value("APT-2026-000001"))
                .andExpect(jsonPath("$.patientName").value("Nimal Perera"))
                .andExpect(jsonPath("$.status").value("BOOKED"));
    }

    @Test
    @DisplayName("returns 400 with per-field messages when validation fails")
    void invalidRequestReturnsFieldErrors() throws Exception {
        String badRequest = """
                {"patientName":"","address":"12 Galle Road","contactNumber":"12345",
                 "dentistId":1,"treatmentTypeId":1,"appointmentDate":"2026-12-01",
                 "appointmentTime":"10:00:00","sessionCount":1}
                """;

        mockMvc.perform(post("/api/appointments")
                        .contentType("application/json")
                        .content(badRequest)
                        .requestAttr(SessionUser.SESSION_KEY, sessionUser()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.patientName").exists())
                .andExpect(jsonPath("$.fieldErrors.contactNumber").exists());
    }

    @Test
    @DisplayName("maps a double booking to 409 Conflict, not 500")
    void doubleBookingReturnsConflict() throws Exception {
        when(appointmentService.register(any(), any()))
                .thenThrow(new DoubleBookingException(
                        "Dr. Silva is already booked from 10:00 to 10:45 by appointment APT-2026-000001."));

        mockMvc.perform(post("/api/appointments")
                        .contentType("application/json")
                        .content(validRequestJson())
                        .requestAttr(SessionUser.SESSION_KEY, sessionUser()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("already booked")));
    }

    @Test
    @DisplayName("maps an unknown appointment number to 404")
    void unknownAppointmentReturnsNotFound() throws Exception {
        when(appointmentService.findByNumber(eq("APT-9999-999999")))
                .thenThrow(NotFoundException.appointment("APT-9999-999999"));

        mockMvc.perform(get("/api/appointments/APT-9999-999999")
                        .requestAttr(SessionUser.SESSION_KEY, sessionUser()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("serialises dates and times as ISO strings the console client can parse")
    void datesSerialiseAsIsoStrings() throws Exception {
        when(appointmentService.findByNumber(any())).thenReturn(sampleAppointment());

        mockMvc.perform(get("/api/appointments/APT-2026-000001")
                        .requestAttr(SessionUser.SESSION_KEY, sessionUser()))
                .andExpect(status().isOk())
                // Without the JavaTimeModule these would serialise as numeric arrays.
                .andExpect(jsonPath("$.appointmentDate").value("2026-09-10"))
                .andExpect(jsonPath("$.appointmentTime").value("10:00:00"));
    }

    private String validRequestJson() {
        return """
                {"patientName":"Nimal Perera","address":"12 Galle Road, Colombo 03",
                 "contactNumber":"0771234567","dentistId":1,"treatmentTypeId":2,
                 "appointmentDate":"2026-09-10","appointmentTime":"10:00:00","sessionCount":1}
                """;
    }

    private SessionUser sessionUser() {
        Staff staff = new Staff("reception1", "irrelevant-hash", "Front Desk", StaffRole.RECEPTIONIST);
        return new SessionUser(staff);
    }

    private Appointment sampleAppointment() {
        return new Appointment(
                "APT-2026-000001",
                new Patient("Nimal Perera", "12 Galle Road, Colombo 03", "0771234567", null),
                new Dentist("Dr. Silva", "General Dentistry"),
                new TreatmentType("EXTRACTION", "Tooth Extraction", new BigDecimal("6000.00"), 45),
                LocalDate.of(2026, 9, 10),
                LocalTime.of(10, 0),
                1,
                "reception1");
    }
}
