package com.sunrise.dental.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Builds the JSON mapper used by the web service.
 *
 * <p>Exists so that there is exactly one definition of how the API serialises JSON, shared by
 * {@link WebConfig} at runtime and by the controller tests. When the test built its own mapper
 * instead, the two drifted: the test passed dates as {@code [2026,9,10]} while production
 * returned {@code "2026-09-10"}, so the test was verifying a format the application never
 * actually produced. A single factory makes that class of bug impossible.</p>
 */
public final class JsonMapperFactory {

    private JsonMapperFactory() {
    }

    /**
     * @return a mapper that writes {@code java.time} values as ISO-8601 strings
     *         ("2026-09-10", "10:00:00") rather than as numeric arrays, which is what the
     *         console client's parser expects
     */
    public static ObjectMapper create() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
}
