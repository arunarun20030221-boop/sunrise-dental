package com.sunrise.dental.client;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * The outcome of one web-service call: either parsed JSON, or a message fit to show a user.
 *
 * <p>Modelled as a result object rather than by throwing, because on this client almost every
 * failure is something the receptionist can act on - a clashing slot, a mistyped phone number -
 * and those are ordinary outcomes of the menu rather than exceptional conditions.</p>
 */
public record ApiResult(boolean ok, int status, JsonNode body, String errorMessage) {

    public static ApiResult success(int status, JsonNode body) {
        return new ApiResult(true, status, body, null);
    }

    public static ApiResult failure(int status, String errorMessage) {
        return new ApiResult(false, status, null, errorMessage);
    }

    /** Reads a text field from the response, or "-" when it is absent or null. */
    public String text(String field) {
        if (body == null || !body.hasNonNull(field)) {
            return "-";
        }
        return body.get(field).asText();
    }
}
