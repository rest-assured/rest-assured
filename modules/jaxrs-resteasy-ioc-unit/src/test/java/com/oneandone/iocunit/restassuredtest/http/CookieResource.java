package com.oneandone.iocunit.restassuredtest.http;

import javax.ws.rs.CookieParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;

/**
 * @author aschoerk
 */
@Path("/")
public class CookieResource {
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/cookie")
    public String cookie(@CookieParam("cookieName1") String cookieValue1, @CookieParam(value = "cookieName2") String cookieValue2) {
        return "{\"cookieValue1\" : \"" + cookieValue1 + "\", \"cookieValue2\" : \"" + cookieValue2 + "\"}";
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/setCookies")
    public Response setCookies(@QueryParam("cookieName1") String cookieName1, @QueryParam("cookieValue1") String cookieValue1,
                               @QueryParam("cookieName2") String cookieName2, @QueryParam("cookieValue2") String cookieValue2) {
        return Response.ok()
                .cookie(new NewCookie(cookieName1, cookieValue1))
                .cookie(new NewCookie(cookieName2, cookieValue2))
                .build();

    }
}
