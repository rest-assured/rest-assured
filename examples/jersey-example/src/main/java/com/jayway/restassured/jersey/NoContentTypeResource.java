package com.jayway.restassured.jersey;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

@Path("/noContentType")
public class NoContentTypeResource {

    @GET
    @Path("/json")
    public Response jsonWithoutContentType() {
        return Response.ok("{ \"message\" : \"It works\" }").build();
    }
}
