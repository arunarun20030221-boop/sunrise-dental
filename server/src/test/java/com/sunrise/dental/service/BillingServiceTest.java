package com.sunrise.dental.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.sunrise.dental.domain.Appointment;
import com.sunrise.dental.domain.Bill;
import com.sunrise.dental.domain.Dentist;
import com.sunrise.dental.domain.Patient;
import com.sunrise.dental.domain.TreatmentType;
import com.sunrise.dental.exception.BillAlreadyIssuedException;
import com.sunrise.dental.dao.BillDao;
import com.sunrise.dental.service.pricing.CosmeticPricing;
import com.sunrise.dental.service.pricing.ExtractionPricing;
import com.sunrise.dental.service.pricing.RootCanalPricing;
import com.sunrise.dental.service.pricing.StandardPricing;
import com.sunrise.dental.service.pricing.TreatmentPricingFactory;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for bill calculation - the "Calculate and Print Bill" requirement.
 *
 * <p>Written before {@code BillingService} existed, as the first step of the test-driven cycle
 * documented in the test plan. The repository is mocked so that these tests exercise the
 * arithmetic and the pricing rules only, with no database involved: they run in milliseconds,
 * which is what makes it practical to run them on every commit.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BillingService")
class BillingServiceTest {

    private static final BigDecimal CONSULTATION_FEE = new BigDecimal("2000.00");

    @Mock
    private BillDao billDao;

    private BillingService billingService;

    @BeforeEach
    void setUp() {
        TreatmentPricingFactory factory = new TreatmentPricingFactory(List.of(
                new StandardPricing(),
                new ExtractionPricing(),
                new RootCanalPricing(),
                new CosmeticPricing()));
        billingService = new BillingService(billDao, factory, CONSULTATION_FEE);
    }

    @Test
    @DisplayName("charges consultation fee plus base cost for a standard treatment")
    void standardTreatmentTotalsFeePlusBaseCost() {
        Appointment appointment = appointmentFor(StandardPricing.CODE, new BigDecimal("3500.00"), 1);

        BillCalculation calculation = billingService.calculate(appointment);

        assertThat(calculation.consultationFee()).isEqualByComparingTo("2000.00");
        assertThat(calculation.treatmentCost()).isEqualByComparingTo("3500.00");
        assertThat(calculation.adjustment()).isEqualByComparingTo("0.00");
        assertThat(calculation.total()).isEqualByComparingTo("5500.00");
    }

    @Test
    @DisplayName("adds a flat anaesthesia fee for an extraction")
    void extractionAddsAnaesthesiaFee() {
        Appointment appointment = appointmentFor(ExtractionPricing.CODE, new BigDecimal("6000.00"), 1);

        BillCalculation calculation = billingService.calculate(appointment);

        // 2000 consultation + 6000 treatment + 1500 anaesthesia
        assertThat(calculation.adjustment()).isEqualByComparingTo("1500.00");
        assertThat(calculation.adjustmentReason()).isEqualTo("Local anaesthesia");
        assertThat(calculation.total()).isEqualByComparingTo("9500.00");
    }

    @Test
    @DisplayName("charges each additional root canal session again")
    void rootCanalChargesPerSession() {
        Appointment appointment = appointmentFor(RootCanalPricing.CODE, new BigDecimal("15000.00"), 3);

        BillCalculation calculation = billingService.calculate(appointment);

        // 2000 consultation + 15000 first session + 2 x 15000 additional
        assertThat(calculation.adjustment()).isEqualByComparingTo("30000.00");
        assertThat(calculation.total()).isEqualByComparingTo("47000.00");
    }

    @Test
    @DisplayName("adds a 15% materials surcharge for cosmetic work, rounded to two decimals")
    void cosmeticAddsMaterialsSurcharge() {
        Appointment appointment = appointmentFor(CosmeticPricing.CODE, new BigDecimal("12345.00"), 1);

        BillCalculation calculation = billingService.calculate(appointment);

        // 15% of 12345.00 = 1851.75
        assertThat(calculation.adjustment()).isEqualByComparingTo("1851.75");
        assertThat(calculation.total()).isEqualByComparingTo("16196.75");
    }

    @Test
    @DisplayName("falls back to the standard rule when a treatment has no pricing strategy")
    void unknownTreatmentCodeFallsBackToStandard() {
        Appointment appointment = appointmentFor("IMPLANT_SURGERY", new BigDecimal("80000.00"), 1);

        BillCalculation calculation = billingService.calculate(appointment);

        assertThat(calculation.adjustment()).isEqualByComparingTo("0.00");
        assertThat(calculation.total()).isEqualByComparingTo("82000.00");
    }

    @Test
    @DisplayName("every monetary amount is returned with exactly two decimal places")
    void amountsAreScaledToTwoDecimals() {
        Appointment appointment = appointmentFor(CosmeticPricing.CODE, new BigDecimal("999.99"), 1);

        BillCalculation calculation = billingService.calculate(appointment);

        assertThat(calculation.total().scale()).isEqualTo(2);
        assertThat(calculation.adjustment().scale()).isEqualTo(2);
    }

    @Test
    @DisplayName("refuses to issue a second bill for the same appointment")
    void doesNotIssueDuplicateBill() {
        Appointment appointment = appointmentFor(StandardPricing.CODE, new BigDecimal("3500.00"), 1);
        when(billDao.existsForAppointment("APT-2026-000001")).thenReturn(true);

        assertThatThrownBy(() -> billingService.issue(appointment, "reception1"))
                .isInstanceOf(BillAlreadyIssuedException.class)
                .hasMessageContaining("APT-2026-000001");
    }

    @Test
    @DisplayName("persists the calculated amounts when issuing a bill")
    void issuePersistsCalculatedAmounts() {
        Appointment appointment = appointmentFor(ExtractionPricing.CODE, new BigDecimal("6000.00"), 1);
        when(billDao.existsForAppointment("APT-2026-000001")).thenReturn(false);
        when(billDao.findByBillNumber(any())).thenReturn(Optional.empty());
        when(billDao.insert(any(Bill.class))).thenAnswer(call -> call.getArgument(0));

        Bill bill = billingService.issue(appointment, "reception1");

        assertThat(bill.getTotal()).isEqualByComparingTo("9500.00");
        assertThat(bill.getConsultationFee()).isEqualByComparingTo("2000.00");
        assertThat(bill.getTreatmentCost()).isEqualByComparingTo("6000.00");
        assertThat(bill.getAdjustment()).isEqualByComparingTo("1500.00");
        assertThat(bill.getIssuedBy()).isEqualTo("reception1");
        assertThat(bill.getBillNumber()).startsWith("BILL-");
    }

    /** Builds an appointment with just enough detail for a billing calculation. */
    private Appointment appointmentFor(String treatmentCode, BigDecimal baseCost, int sessions) {
        Patient patient = new Patient("Nimal Perera", "12 Galle Road, Colombo 03", "0771234567", "nimal@example.lk");
        Dentist dentist = new Dentist("Dr. Silva", "General Dentistry");
        TreatmentType treatment = new TreatmentType(treatmentCode, treatmentCode, baseCost, 30);
        return new Appointment(
                "APT-2026-000001",
                patient,
                dentist,
                treatment,
                LocalDate.of(2026, 9, 10),
                LocalTime.of(10, 0),
                sessions,
                "reception1");
    }
}
