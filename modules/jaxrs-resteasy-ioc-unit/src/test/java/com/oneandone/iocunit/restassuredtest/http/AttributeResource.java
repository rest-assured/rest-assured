package com.oneandone.iocunit.restassuredtest.http;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;

/**
 * @author aschoerk
 */
@Path("/attribute")
public class AttributeResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response attribute(@Context HttpServletRequest request) {
        Collection<String> attributes = new ArrayList<String>();
        for (String attributeName : Collections.list(request.getAttributeNames())) {
            attributes.add("\"" + attributeName + "\": \"" + request.getAttribute(attributeName) + "\"");
        }

        return Response.ok().entity("{" + StringUtils.join(attributes, ", ") + "}").build();
    }


}
