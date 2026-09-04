package com.sunrise.dental.service.pricing;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * FACTORY pattern.
 *
 * <p>Returns the {@link PricingStrategy} that applies to a given treatment code. Spring injects
 * every {@code PricingStrategy} bean on the classpath, which this factory indexes once at
 * startup, so registering a new pricing rule means adding one {@code @Component} - this class
 * never changes.</p>
 *
 * <p>This is also a SINGLETON: Spring beans are singleton-scoped by default, so the lookup table
 * is built once per application rather than per request. That is a deliberate contrast with the
 * classic {@code getInstance()} idiom, which would have to hand-roll thread safety and would be
 * far harder to substitute in a unit test - here the constructor takes its collaborators, so a
 * test can construct the factory directly with just the strategies it cares about.</p>
 */
@Component
public class TreatmentPricingFactory {

    private final Map<String, PricingStrategy> strategiesByCode;
    private final PricingStrategy fallback;

    public TreatmentPricingFactory(List<PricingStrategy> strategies) {
        this.strategiesByCode = strategies.stream()
                .collect(Collectors.toMap(PricingStrategy::supportedTreatmentCode, Function.identity()));
        this.fallback = strategiesByCode.getOrDefault(StandardPricing.CODE, new StandardPricing());
    }

    /**
     * @param treatmentCode a {@code TreatmentType.code}
     * @return the matching strategy, or the standard no-adjustment rule when the treatment has
     *         no rule of its own. Returning a working default rather than throwing means a newly
     *         added treatment still bills correctly at its base cost.
     */
    public PricingStrategy strategyFor(String treatmentCode) {
        if (treatmentCode == null) {
            return fallback;
        }
        return strategiesByCode.getOrDefault(treatmentCode, fallback);
    }
}
