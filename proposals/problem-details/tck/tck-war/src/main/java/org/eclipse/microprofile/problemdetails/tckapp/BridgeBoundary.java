package org.eclipse.microprofile.problemdetails.tckapp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.problemdetails.ResponseStatus;
import org.eclipse.microprofile.problemdetails.Status;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import java.net.URI;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;

@Slf4j
@Path("/bridge")
public class BridgeBoundary {
    private static final String BASE_URI = "http://localhost:8080/problem-details.tck-war";

    /** how to call the target -- duplicated in MicroprofileRestClientBridgeIT */
    @SuppressWarnings("unused") public enum Mode {
        /** JAX-RS WebTarget */
        webTarget {
            @Override public API api(BridgeBoundary context) {
                return context::jaxRsCall;
            }
        },

        /** Manually build a Microprofile Rest Client */
        mpm {
            @Override public API api(BridgeBoundary context) {
                return RestClientBuilder.newBuilder().baseUri(URI.create(BASE_URI)).build(API.class);
            }
        },

        /** Injected Microprofile Rest Client */
        mpi {
            @Override public API api(BridgeBoundary context) {
                return context.target;
            }
        };

        public abstract API api(BridgeBoundary context);
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class Reply {
        private String value;
    }

    @Status(ResponseStatus.FORBIDDEN)
    public static class ApiException extends RuntimeException {}

    @RegisterRestClient(baseUri = BASE_URI)
    public interface API {
        @Path("/bridge/target/{state}")
        @GET Reply request(@PathParam("state") String state) throws ApiException;
    }

    @Inject @RestClient API target;

    private final Client rest = ClientBuilder.newClient();

    @Path("/indirect/{state}")
    @GET public Reply indirect(@PathParam("state") String state, @NotNull @QueryParam("mode") Mode mode) {
        log.info("call indirect {} :: {}", state, mode);

        API target = mode.api(this);

        try {
            Reply reply = target.request(state);
            log.info("indirect call reply {}", reply);
            return reply;
        } catch (RuntimeException e) {
            log.info("indirect call exception", e);
            throw e;
        }
    }

    private Reply jaxRsCall(String state) {
        return rest.target(BASE_URI)
            .path("/bridge/target")
            .path(state)
            .request(APPLICATION_JSON_TYPE)
            .get(Reply.class);
    }

    @Path("/target/{state}")
    @GET public Response target(@PathParam("state") String state) {
        log.info("target {}", state);
        switch (state) {
            case "ok":
                return Response.ok(new Reply("okay")).build();
            case "raw":
                return Response.status(FORBIDDEN).build();
            case "fails":
                throw new ApiException();
        }
        throw new UnsupportedOperationException();
    }
}
