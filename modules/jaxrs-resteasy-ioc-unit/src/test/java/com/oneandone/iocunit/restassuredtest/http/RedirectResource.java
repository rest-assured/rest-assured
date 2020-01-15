package com.oneandone.iocunit.restassuredtest.http;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @author aschoerk
 */
@Path("/redirect")
public class RedirectResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response redirect() {
        return Response.status(Response.Status.MOVED_PERMANENTLY)
                .header("Location", "http://localhost:8080/redirect/1")
                .entity("{ \"id\" : 1 }").build();
    }
}
