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

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Spring Framework 7.0 has no RequestMethod.QUERY, so the mapping is not restricted to a method. It echoes the
 * method, path variable, query string, content type and body of the request.
 */
@RestController
public class QueryController {

    @RequestMapping("/search/{collection}")
    public String search(HttpServletRequest request, @PathVariable("collection") String collection,
                         @RequestBody(required = false) String body) {
        String queryString = request.getQueryString() == null ? "" : "?" + request.getQueryString();
        return request.getMethod() + " " + collection + queryString + " " + body;
    }
}
