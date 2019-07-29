package com.oneandone.iocunit.restassuredtest.http;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;


/**
 * @author aschoerk
 */
@Path("/")
public class BasePathResource {
    private static final String template = "Hello, %s!";

    @GET
    @Path("/my-path/greetingPath")
    @Produces(MediaType.APPLICATION_JSON)
    public Greeting greeting(
            @QueryParam("name") @DefaultValue("World") String name) {
        return new Greeting(0, String.format(template, name));
    }

    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    public Greeting greeting2(
            @QueryParam("name") @DefaultValue("World") String name) {
        return new Greeting(1, String.format(template, name));
    }
}
