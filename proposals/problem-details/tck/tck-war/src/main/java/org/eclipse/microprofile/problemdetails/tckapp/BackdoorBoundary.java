package org.eclipse.microprofile.problemdetails.tckapp;

import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

import static org.eclipse.microprofile.problemdetails.Constants.EXCEPTION_MESSAGE_AS_DETAIL;

@Path("/backdoors")
public class BackdoorBoundary {
    @Path("/message-as-detail")
    @POST public void exceptionMessageAsDetail(@FormParam("enabled") boolean enabled) {
        if (enabled) {
            System.clearProperty(EXCEPTION_MESSAGE_AS_DETAIL);
        } else {
            System.setProperty(EXCEPTION_MESSAGE_AS_DETAIL, "false");
        }
    }
}
