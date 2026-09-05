package com.sunrise.dental.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.format.support.DefaultFormattingConversionService;

/**
 * Guards the format in which the appointment date and time are <em>rendered</em> back into the
 * registration form.
 *
 * <p>This covers a defect that no other test could see. Parsing was always correct, so every
 * booking test passed; the fault appeared only when the form was redisplayed after a rejection.
 * Spring converted the bound {@link LocalDate} using the JVM's locale style, producing
 * {@code 11/12/26} and {@code 2:00 PM}. An {@code <input type="date">} accepts only
 * {@code yyyy-MM-dd} and an {@code <input type="time">} only {@code HH:mm}; given anything else
 * the browser silently renders an <em>empty</em> field rather than reporting an error. The
 * receptionist therefore lost the date and time from a form they had just filled in, every time
 * a slot clashed - which is precisely the situation in which the values matter most.</p>
 *
 * <p>The assertion is on the rendering direction because that is the direction that broke, and
 * because a browser's silence makes the failure invisible to any test that only round-trips a
 * value through the parser.</p>
 */
@DisplayName("AppointmentRequest form formatting")
class AppointmentRequestFormatTest {

    private final DefaultFormattingConversionService conversion =
            new DefaultFormattingConversionService();

    /** Describes the record component, so the {@code @DateTimeFormat} on it is honoured. */
    private TypeDescriptor componentOf(String name) throws NoSuchFieldException {
        Field field = AppointmentRequest.class.getDeclaredField(name);
        return new TypeDescriptor(field);
    }

    private String render(String component, Object value) throws NoSuchFieldException {
        return (String) conversion.convert(
                value, componentOf(component), TypeDescriptor.valueOf(String.class));
    }

    @Test
    @DisplayName("renders the date as yyyy-MM-dd, which is the only form <input type=date> accepts")
    void datesRenderAsIsoForTheDateInput() throws Exception {
        assertThat(render("appointmentDate", LocalDate.of(2026, 11, 12)))
                .isEqualTo("2026-11-12");
    }

    @Test
    @DisplayName("renders the time as HH:mm, which is the only form <input type=time> accepts")
    void timesRenderAsIsoForTheTimeInput() throws Exception {
        assertThat(render("appointmentTime", LocalTime.of(14, 0)))
                .isEqualTo("14:00");
    }

    @Test
    @DisplayName("an afternoon time keeps the 24-hour clock rather than reverting to AM/PM")
    void afternoonTimesStayOnThe24HourClock() throws Exception {
        assertThat(render("appointmentTime", LocalTime.of(17, 30)))
                .isEqualTo("17:30")
                .doesNotContain("PM");
    }

    @Test
    @DisplayName("parsing still accepts what the browser submits")
    void parsingStillAcceptsBrowserInput() throws Exception {
        assertThat(conversion.convert("2026-11-12", TypeDescriptor.valueOf(String.class),
                componentOf("appointmentDate")))
                .isEqualTo(LocalDate.of(2026, 11, 12));
        assertThat(conversion.convert("14:00", TypeDescriptor.valueOf(String.class),
                componentOf("appointmentTime")))
                .isEqualTo(LocalTime.of(14, 0));
    }
}
