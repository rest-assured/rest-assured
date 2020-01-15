package com.oneandone.iocunit.restassuredtest.http;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @author aschoerk
 */
@Path("/header")
public class HeaderResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response header(@HeaderParam("headerName") String headerValue, @HeaderParam("User-Agent") String userAgent) {
        final String entity = "{\"headerName\" : \"" + headerValue + "\", \"user-agent\" : \"" + userAgent + "\"}";
        return Response
                .ok()
                .entity(entity)
                .header("Content-Length", entity.length())
                .build();
    }
}
