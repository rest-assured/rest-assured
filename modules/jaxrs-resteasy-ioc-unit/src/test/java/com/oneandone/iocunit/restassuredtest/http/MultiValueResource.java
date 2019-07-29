package com.oneandone.iocunit.restassuredtest.http;

import java.util.List;

import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang3.StringUtils;

/**
 * @author aschoerk
 */
@Path("")
public class MultiValueResource {
    @GET
    @Path("/multiValueParam")
    @Produces(MediaType.APPLICATION_JSON)
    public String multiValueParam(@QueryParam("list") List<String> listValues) {
        return "{ \"list\" : \"" + StringUtils.join(listValues, ",") + "\" }";
    }

    @POST
    @Path("/multiValueParam")
    @Produces(MediaType.APPLICATION_JSON)
    public String multiValueParamPost(@FormParam("list") List<String> listValues) {
        return "{ \"list\" : \"" + StringUtils.join(listValues, ",") + "\" }";
    }

    @GET
    @Path("/threeMultiValueParam")
    @Produces(MediaType.APPLICATION_JSON)
    public
    String getThreeMultiValueParam(@QueryParam("list") List<String> list1Values,
                                @QueryParam("list2") List<String> list2Values,
                                @FormParam("list3") List<String> list3Values) {
        return "{ \"list\" : \"" + StringUtils.join(list1Values, ",") + "\"," +
               " \"list2\" : \"" + StringUtils.join(list2Values, ",") + "\", " +
               " \"list3\" : \"" + StringUtils.join(list3Values, ",") + "\" }";
    }

    @POST
    @Path("/threeMultiValueParam")
    @Produces(MediaType.APPLICATION_JSON)
    public
    String postThreeMultiValueParam(@FormParam("list") List<String> list1Values,
                                @QueryParam("list2") List<String> list2Values,
                                @FormParam("list3") List<String> list3Values) {
        return "{ \"list\" : \"" + StringUtils.join(list1Values, ",") + "\"," +
               " \"list2\" : \"" + StringUtils.join(list2Values, ",") + "\", " +
               " \"list3\" : \"" + StringUtils.join(list3Values, ",") + "\" }";
    }


}
