package org.eclipse.microprofile.problemdetails.tckapp;

import lombok.extern.java.Log;

import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

import static org.eclipse.microprofile.problemdetails.Constants.EXCEPTION_MESSAGE_AS_DETAIL;

@Log
@Path("/backdoors")
public class BackdoorBoundary {
    @Path("/message-as-detail")
    @POST public void exceptionMessageAsDetail(@FormParam("enabled") boolean enabled) {
        log.warning("set " + EXCEPTION_MESSAGE_AS_DETAIL + " to " + enabled);
        System.setProperty(EXCEPTION_MESSAGE_AS_DETAIL, Boolean.toString(enabled));
    }
}
