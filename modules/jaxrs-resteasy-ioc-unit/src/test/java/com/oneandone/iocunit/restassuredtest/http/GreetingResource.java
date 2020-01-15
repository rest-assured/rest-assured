package com.oneandone.iocunit.restassuredtest.http;

import java.util.concurrent.atomic.AtomicLong;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

/**
 * @author aschoerk
 */
@Path("/")
public class GreetingResource {
    private static final String template = "Hello, %s!";
    private final AtomicLong counter = new AtomicLong();

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/greeting")
    public Greeting greeting(
            @QueryParam("name") @DefaultValue("World") String name) {
        return new Greeting(counter.incrementAndGet(), String.format(template, name));
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/greeting")
    public Greeting greetingWithRequiredContentType(
            @QueryParam("name") @DefaultValue("World") String name) {
        return greeting(name);
    }
}
