package org.eclipse.microprofile.problemdetails;

import java.util.stream.Stream;

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
        return Stream.of(values())
            .filter(status -> status.code == code)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("illegal status code " + code));
    }
}
