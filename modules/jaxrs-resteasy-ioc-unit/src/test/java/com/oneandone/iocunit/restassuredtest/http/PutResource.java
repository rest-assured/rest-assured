package com.oneandone.iocunit.restassuredtest.http;

import java.util.concurrent.atomic.AtomicLong;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * @author aschoerk
 */
@Path("")
public class PutResource {
    private static final String template = "Hello, %s!";
    private final AtomicLong counter = new AtomicLong();

    @PUT
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/greetingPut")
    public Greeting greeting(@FormParam("name") String name) {
        return new Greeting(counter.incrementAndGet(), String.format(template, name));
    }

    @PUT
    @Path("/stringBody")
    public String stringBody(String body) {
        return body;
    }

    @PUT
    @Path("/jsonReflect")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String jsonReflect(String body) {
        return body;
    }

}
