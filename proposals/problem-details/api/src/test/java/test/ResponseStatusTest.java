package test;

import org.eclipse.microprofile.problemdetails.ResponseStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ResponseStatusTest {
    @EnumSource(ResponseStatus.class)
    @ParameterizedTest void shouldFindAllowedStatus(ResponseStatus status) {
        assertTrue(ResponseStatus.allowed(status.code));
        assertEquals(status, ResponseStatus.valueOf(status.code));
        assertEquals(status, ResponseStatus.valueOf(status.name()));
    }

    @Test void shouldNotFindUnmappedStatus() {
        assertFalse(ResponseStatus.allowed(405));
        assertThrows(IllegalArgumentException.class, () -> ResponseStatus.valueOf(405));
    }
}
