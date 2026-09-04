package com.sunrise.dental.config;

import java.math.BigDecimal;
import java.time.LocalTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Clinic settings, bound from {@code application.properties} by {@code @Value}.
 *
 * <p>Opening hours and the consultation fee are configuration rather than constants in code, so
 * the clinic can change them without a rebuild - and so a test can construct this class
 * directly with its own values, which is why the constructor is public and takes every setting.</p>
 *
 * <p>Accessors follow the JavaBean {@code getX()} convention rather than the shorter
 * record-style {@code x()}: JSP expression language resolves {@code ${clinic.name}} by
 * looking for {@code getName()}, and fails outright if only {@code name()} exists.</p>
 */
@Component
public class ClinicProperties {

    private final String name;
    private final String address;
    private final String phone;
    private final String currency;
    private final BigDecimal consultationFee;
    private final LocalTime openingTime;
    private final LocalTime closingTime;

    /**
     * Marked {@code @Autowired} because this class has two constructors: without the
     * annotation Spring cannot tell which one to inject through and falls back to looking for
     * a no-argument constructor that does not exist.
     */
    @Autowired
    public ClinicProperties(
            @Value("${clinic.name}") String name,
            @Value("${clinic.address}") String address,
            @Value("${clinic.phone}") String phone,
            @Value("${clinic.currency}") String currency,
            @Value("${clinic.consultation-fee}") BigDecimal consultationFee,
            @Value("${clinic.opening-time}") String openingTime,
            @Value("${clinic.closing-time}") String closingTime) {
        this(name, address, phone, currency, consultationFee,
                LocalTime.parse(openingTime), LocalTime.parse(closingTime));
    }

    /** Direct constructor, used by tests. */
    public ClinicProperties(String name,
                            String address,
                            String phone,
                            String currency,
                            BigDecimal consultationFee,
                            LocalTime openingTime,
                            LocalTime closingTime) {
        this.name = name;
        this.address = address;
        this.phone = phone;
        this.currency = currency;
        this.consultationFee = consultationFee;
        this.openingTime = openingTime;
        this.closingTime = closingTime;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public String getPhone() {
        return phone;
    }

    public String getCurrency() {
        return currency;
    }

    public BigDecimal getConsultationFee() {
        return consultationFee;
    }

    public LocalTime getOpeningTime() {
        return openingTime;
    }

    public LocalTime getClosingTime() {
        return closingTime;
    }
}
