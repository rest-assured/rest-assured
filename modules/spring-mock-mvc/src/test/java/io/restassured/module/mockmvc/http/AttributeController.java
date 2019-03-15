/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.restassured.module.mockmvc.http;

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
