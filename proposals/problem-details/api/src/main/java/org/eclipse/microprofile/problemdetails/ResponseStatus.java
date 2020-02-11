package org.eclipse.microprofile.problemdetails;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * The http status codes that are allowed for Problem Details.
 */
public enum ResponseStatus {
    BAD_REQUEST(400),
    FORBIDDEN(403),
    NOT_FOUND(404),
    CONFLICT(409),

    INTERNAL_SERVER_ERROR(500),
    NOT_IMPLEMENTED(501),
    BAD_GATEWAY(502),
    SERVICE_UNAVAILABLE(503),
    GATEWAY_TIMEOUT(504);

    public final int code;

    ResponseStatus(int code) { this.code = code; }

    @Override public String toString() {
        return code + " " + name();
    }

    public static ResponseStatus valueOf(int code) {
        return find(code).orElseThrow(() -> new IllegalArgumentException("status code " + code + " not allowed for problem details"));
    }

    public static boolean allowed(int code) { return find(code).isPresent(); }

    private static Optional<ResponseStatus> find(int code) {
        return Stream.of(values())
            .filter(status -> status.code == code)
            .findFirst();
    }
}
