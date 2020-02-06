package org.eclipse.microprofile.problemdetails.tck;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.OK;
import static org.assertj.core.api.BDDAssertions.then;
import static org.eclipse.microprofile.problemdetails.Constants.PROBLEM_DETAIL_JSON;
import static org.eclipse.microprofile.problemdetails.tck.MicroprofileRestClientBridgeIT.Mode.mpi;
import static org.eclipse.microprofile.problemdetails.tck.MicroprofileRestClientBridgeIT.Mode.mpm;
import static org.eclipse.microprofile.problemdetails.tck.MicroprofileRestClientBridgeIT.Mode.webTarget;

@Slf4j
@ExtendWith(ContainerLaunchingExtension.class)
class MicroprofileRestClientBridgeIT {

    /** how to call the target -- duplicated in BridgeBoundary */
    @SuppressWarnings("unused") public enum Mode {
        /** JAX-RS WebTarget */
        webTarget,

        /** Manually build a Microprofile Rest Client */
        mpm,

        /** Injected Microprofile Rest Client */
        mpi
    }

    @Test void shouldFailValidationWithoutMode() {
        Response response = get("/bridge/indirect/ok", null);

        ContainerLaunchingExtension.thenProblemDetail(response).hasType("urn:problem-type:validation-failed");
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
        // the fallback behavior of the servers is very diverse, some send html, some an empty entity, some none at all.
        // so it doesn't make much sense to check for the entity
    }


    // we don't use @ParameterizedTest here, so it's easier to disable single cases
    // I didn't manage to make surefire parameterized exclusion work with JUnit 5:
    // https://maven.apache.org/surefire/maven-surefire-plugin/examples/single-test.html
    @Test void shouldMapBridgedFail_webTarget() { shouldMapBridgedFail(webTarget); }

    @Test void shouldMapBridgedFail_mpm() { shouldMapBridgedFail(mpm); }

    @Test void shouldMapBridgedFail_mpi() { shouldMapBridgedFail(mpi); }

    private void shouldMapBridgedFail(Mode mode) {
        Response response = get("/bridge/indirect/fails", mode);

        ContainerLaunchingExtension.thenProblemDetail(response)
            .hasStatus(FORBIDDEN)
            .hasContentType(PROBLEM_DETAIL_JSON)
            .hasTitle("Api")
            .hasType("urn:problem-type:api")
            .hasUuidInstance();
    }

    private Response get(String path, Mode mode) {
        WebTarget target = ContainerLaunchingExtension.target(path).queryParam("mode", mode);
        log.info("GET {}", target.getUri());
        return target
            .request(APPLICATION_JSON_TYPE)
            .get();
    }
}
