package org.eclipse.microprofile.problemdetails.tckapp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
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
import java.util.function.Function;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;

@Slf4j
@Path("/bridge")
public class BridgeBoundary {
    @Data @NoArgsConstructor @AllArgsConstructor
    public static class Reply {
        private String value;
    }

    @Status(ResponseStatus.FORBIDDEN)
    public static class ApiException extends RuntimeException {}

    @RegisterRestClient
    public interface API {
        @Path("/bridge/target/{state}")
        @GET Reply request(@PathParam("state") String state) throws ApiException;
    }

    @Inject @ConfigProperty(name = "org.eclipse.microprofile.problemdetails.tckapp.BridgeBoundary$API/mp-rest/url")
    URI baseUri;

    @Inject @RestClient API target;

    private final Client rest = ClientBuilder.newClient();

    @Path("/indirect/{state}")
    @GET public Reply indirect(@PathParam("state") String state, @NotNull @QueryParam("mode") String mode) {
        log.info("call indirect {} :: {}", state, mode);

        Function<String, Reply> target = by(mode);

        try {
            Reply reply = target.apply(state);
            log.info("indirect call reply {}", reply);
            return reply;
        } catch (RuntimeException e) {
            log.info("indirect call exception", e);
            throw e;
        }
    }

    private Function<String, Reply> by(String mode) {
        switch (mode) {
            case "webTarget": // JAX-RS WebTarget
                return this::jaxRsCall;
            case "mpm": // Manually build a Microprofile Rest Client
                return RestClientBuilder.newBuilder().baseUri(baseUri)
                    .build(API.class)::request;
            case "mpi": // Injected Microprofile Rest Client
                return target::request;
        }
        throw new UnsupportedOperationException("unknown mode " + mode);
    }

    private Reply jaxRsCall(String state) {
        return rest.target(baseUri)
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
