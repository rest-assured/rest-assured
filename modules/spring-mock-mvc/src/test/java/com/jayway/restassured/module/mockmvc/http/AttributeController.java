package com.jayway.restassured.module.mockmvc.http;

import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;

public class AttributeController {

    @RequestMapping(value = "/attribute", method = GET, produces = APPLICATION_JSON_VALUE)
    public @ResponseBody String attribute(HttpServletRequest request) {
        Collection<String> attributes = new ArrayList<String>();
        for (String attributeName : Collections.list(request.getAttributeNames())) {
            attributes.add("\"" + attributeName + "\": \"" + request.getAttribute(attributeName) + "\"");
        }

        return "{" + StringUtils.join(attributes, ", ") + "}";
    }
}
