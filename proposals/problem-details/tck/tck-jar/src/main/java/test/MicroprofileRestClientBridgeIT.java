package test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import javax.ws.rs.core.Response;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.OK;
import static org.assertj.core.api.BDDAssertions.then;
import static org.eclipse.microprofile.problemdetails.Constants.PROBLEM_DETAIL_JSON;
import static test.ContainerLaunchingExtension.target;
import static test.ContainerLaunchingExtension.thenProblemDetail;

@ExtendWith(ContainerLaunchingExtension.class)
class MicroprofileRestClientBridgeIT {

    /** how to call the target -- duplicated in BridgeBoundary */
    public enum Mode {
        /** JAX-RS WebTarget */
        jaxRs,

        /** Manually build a Microprofile Rest Client */
        mMpRest,

        /** Injected Microprofile Rest Client */
        iMpRest
    }

    @Test void shouldFailValidationWithoutMode() {
        Response response = get("/bridge/indirect/ok", null);

        thenProblemDetail(response).hasType("urn:problem-type:validation-failed");
    }

    @EnumSource(Mode.class)
    @ParameterizedTest void shouldFailWithUnknownState(Mode mode) {
        Response response = get("/bridge/indirect/unknown", mode);

        thenProblemDetail(response).hasStatus(NOT_FOUND).hasType("urn:problem-type:not-found");
    }

    @EnumSource(Mode.class)
    @ParameterizedTest void shouldMapBridgedOkay(Mode mode) {
        Response response = get("/bridge/indirect/ok", mode);

        then(response.getStatusInfo()).isEqualTo(OK);
        then(response.getMediaType()).isEqualTo(APPLICATION_JSON_TYPE);
        then(response.readEntity(String.class)).isEqualTo("{\"value\":\"okay\"}");
    }

    @EnumSource(Mode.class)
    @ParameterizedTest void shouldMapBridgedRaw(Mode mode) {
        Response response = get("/bridge/indirect/raw", mode);

        then(response.getStatusInfo()).isEqualTo(FORBIDDEN);
        if (response.hasEntity()) {
            then(response.readEntity(String.class)).isEmpty();
        }
    }

    @EnumSource(Mode.class)
    @ParameterizedTest void shouldMapBridgedFail(Mode mode) {
        Response response = get("/bridge/indirect/fails", mode);

        thenProblemDetail(response)
            .hasStatus(BAD_REQUEST)
            .hasContentType(PROBLEM_DETAIL_JSON)
            .hasTitle("Api")
            .hasType("urn:problem-type:api")
            .hasUuidInstance();
    }

    private Response get(String path, Mode mode) {
        return target(path)
            .queryParam("mode", mode)
            .request(APPLICATION_JSON_TYPE)
            .get();
    }
}
